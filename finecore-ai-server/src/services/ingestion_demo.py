"""Run a synthetic local provider through the real Java API and Python worker.

Requires a running Java server and its development database. Creates new demo
records on each run. No external provider or LLM is called.
"""

import json
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from uuid import uuid4

from app.config import Settings
from schemas.adapters import Candidate
from services.adapter_service import register
from services.ingestion_worker import Transport, Worker, now


def main():
    payload = {"number": "DEMO001", "legal_name": "Synthetic Demo Issuer", "status": "Active"}

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            raw = json.dumps(payload).encode()
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(raw)

        def log_message(self, *args):
            pass

    server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        settings = Settings()
        source_origin = f"http://127.0.0.1:{server.server_port}"
        settings.ingestion_allowed_origins = [source_origin]
        transport = Transport(settings)
        base = settings.core_api_url.rstrip("/")

        def post(path, body):
            _, _, raw = transport.request(base + path, body)
            return json.loads(raw)

        def create(resource, **fields):
            return post("/api/v1/company-data/" + resource, fields)["id"]

        evidence = create(
            "source-references", evidenceIds=[], extractionMethod="configured", observedAt=now()
        )
        key = uuid4().hex
        provider = create(
            "providers",
            evidenceIds=[],
            code=f"demo-{key}",
            name="Synthetic local provider",
            kind="manual",
        )
        company = post(
            "/api/v1/companies",
            {
                "company": {
                    "evidenceIds": [evidence],
                    "slug": f"demo-{key}",
                    "displayName": payload["legal_name"],
                    "entityKind": "issuer",
                },
                "identifiers": [
                    {
                        "evidenceIds": [evidence],
                        "scheme": "provider_company_id",
                        "authority": f"demo-{key}",
                        "value": payload["number"],
                    }
                ],
            },
        )["id"]
        endpoint = create(
            "source-endpoints",
            evidenceIds=[],
            providerId=provider,
            code="profile",
            contractVersion="1",
            authScheme="none",
            supportedDomains=["profile"],
            paginationKind="none",
            accessStatus="available",
            checkedAt=now(),
        )
        binding = create(
            "provider-bindings",
            evidenceIds=[evidence],
            providerId=provider,
            companyId=company,
            sourceIdentifier=payload["number"],
            identifierKind="registry_number",
            verificationStatus="verified",
            verifiedAt=now(),
        )
        adapter = {
            "source_id": "synthetic_profile",
            "identity_path": ["number"],
            "semantic_notes": ["Synthetic fixture: number is a registry registration number."],
            "fields": {
                "record_type": {"constant": "company_profile"},
                "name": {"path": ["legal_name"]},
                "registration_number": {"path": ["number"]},
                "status": {"path": ["status"]},
            },
        }
        samples = []
        for number, name in [
            ("DEMO001", "Synthetic Demo Issuer"),
            ("DEMO002", "Second Demo Issuer"),
        ]:
            samples.append(
                {
                    "company_id": number,
                    "expected_source_identifier": number,
                    "raw_reference": "synthetic:certification",
                    "payload": {"number": number, "legal_name": name, "status": "Active"},
                    "expected_records": [
                        {
                            "record_type": "company_profile",
                            "name": name,
                            "registration_number": number,
                            "status": "Active",
                        }
                    ],
                }
            )
        adapter_id = register(
            Candidate(adapter=adapter, samples=samples), settings.adapter_registry_dir
        )["adapter_id"]
        target = post(
            "/api/v1/ingestion/targets",
            {
                "bindingId": binding,
                "endpointId": endpoint,
                "fetchUrl": source_origin + "/profile",
                "sourceId": "synthetic_profile",
                "adapterId": adapter_id,
                "registry": "Synthetic Registry",
                "registryJurisdiction": "GB",
                "registrationKind": "overseas",
            },
        )["id"]
        job = post(f"/api/v1/ingestion/targets/{target}/refresh", {})
        # Use a dedicated development queue with no other pending work.
        Worker(settings).run_once()
        _, _, raw = transport.request(base + f"/api/v1/ingestion/jobs/{job['id']}")
        result = json.loads(raw)
        if result["status"] != "succeeded":
            raise RuntimeError("Demo ingestion did not succeed; inspect the job status")
        overview_url = (
            base + f"/api/v1/companies/{company}/overview?snapshotId={result['snapshotId']}"
        )
        _, _, overview = transport.request(overview_url)
        assert b"Synthetic Demo Issuer" in overview
        print(json.dumps({"job": result, "overviewUrl": overview_url}, indent=2))
    finally:
        server.shutdown()
        server.server_close()
        thread.join()


if __name__ == "__main__":
    main()
