"""Register the SEC adapter through FastAPI and ingest the five configured companies once."""

import hashlib
import json
import time
from pathlib import Path
from urllib.parse import unquote, urlencode, urlsplit
from uuid import uuid4

from app.config import Settings
from services.ingestion_worker import Transport, Worker, now
from services.sec_live_prepare import prepare


class API:
    """Small client for explicit Java/Python contracts, without direct database writes."""

    def __init__(self, base, transport):
        self.base, self.transport = base.rstrip("/"), transport

    def call(self, path, body=None):
        _, _, raw = self.transport.request(self.base + path, body)
        return json.loads(raw)

    def records(self, resource, **filters):
        page = 0
        while True:
            result = self.call(
                f"/api/v1/company-data/{resource}?"
                + urlencode({**filters, "page": page, "size": 100})
            )
            yield from result["items"]
            if not result["hasNext"]:
                break
            page += 1

    def create(self, resource, **fields):
        return self.call("/api/v1/company-data/" + resource, fields)["id"]


def verify_overview(core, overview, config):
    """Check saved names, provider identity, evidence hash and normalization provenance."""
    profiles = overview["sections"]["profile"]
    names = [
        r["attributes"]["profileValue"]
        for r in profiles
        if r["recordType"] == "profile-observations" and r["attributes"]["field"] == "legal_name"
    ]
    numbers = [
        r["attributes"]["registrationNumber"]
        for r in profiles
        if r["recordType"] == "registrations"
    ]
    if names != [config["expectedName"]] or numbers != [config["expectedSourceIdentifier"]]:
        raise ValueError("Frozen profile differs from independently checked identity")
    attempt = overview["sections"]["ingestion"][0]["attributes"]
    artifact = core.call("/api/v1/company-data/raw-artifacts/" + attempt["artifactId"])[
        "attributes"
    ]
    raw = Path(unquote(urlsplit(artifact["storageUri"]).path)).read_bytes()
    if hashlib.sha256(raw).hexdigest() != artifact["sha256"] or len(raw) != artifact["byteLength"]:
        raise ValueError("Retained response does not match persisted artifact metadata")
    if attempt["httpStatus"] != 200 or attempt["requestUrlRedacted"] != config["fetchUrl"]:
        raise ValueError("Unexpected persisted source response")
    return {"rawHashVerified": True, "rawByteLength": len(raw)}


def ingest(settings, python_url="http://127.0.0.1:8000"):
    """Verify live samples first, then provision exact CIK identities and ingest bounded jobs."""
    root = Path(__file__).resolve().parents[3]
    bundle = root / "finecore-ai-server/adapters/sec-submissions-profile"
    output = root / "temp/sec-live-profile" / "runs" / uuid4().hex
    transport = Transport(settings)
    core = API(settings.core_api_url, transport)
    python = API(python_url, transport)
    core.call("/actuator/health")
    python.call("/health")
    prepare(settings, bundle, output)
    candidate = json.loads((output / "candidate.json").read_text())
    python.call("/adapters/validate", candidate)
    adapter_id = python.call("/adapters/register", candidate)["adapter_id"]
    # Worker and API must use the same registry. This also checks the actual HTTP normalize route.
    replay = python.call(f"/adapters/{adapter_id}/normalize", {"sample": candidate["samples"][0]})
    if replay["adapter_id"] != adapter_id:
        raise ValueError("Python registry replay mismatch")
    provider = next(
        (r["id"] for r in core.records("providers") if r["attributes"]["code"] == "sec"), None
    )
    if provider is None:
        provider = core.create(
            "providers", evidenceIds=[], code="sec", name="SEC EDGAR", kind="regulator"
        )
    endpoint = next(
        (
            r["id"]
            for r in core.records("source-endpoints", providerId=provider)
            if r["attributes"]["code"] == "submissions-identity"
            and r["attributes"]["contractVersion"] == "1"
        ),
        None,
    )
    if endpoint is None:
        endpoint = core.create(
            "source-endpoints",
            evidenceIds=[],
            providerId=provider,
            code="submissions-identity",
            contractVersion="1",
            authScheme="none",
            supportedDomains=["company_identity"],
            paginationKind="none",
            accessStatus="available",
            checkedAt=now(),
            documentationUrl="https://www.sec.gov/search-filings/edgar-application-programming-interfaces",
        )
    report = {
        "startedAt": now(),
        "adapterId": adapter_id,
        "runDirectory": str(output),
        "companies": [],
    }
    report_path = root / "temp/sec-live-profile/ingestion-report.json"

    def checkpoint():
        report_path.write_text(json.dumps(report, indent=2) + "\n")

    checkpoint()
    settings.ingestion_allowed_origins = ["https://data.sec.gov"]
    worker = Worker(settings)
    for config, sample in zip(
        json.loads((bundle / "companies.json").read_text()), candidate["samples"], strict=True
    ):
        cik = config["expectedSourceIdentifier"]
        # Search is broad; verify exact CIK ownership through the identifiers endpoint.
        hits = core.call("/api/v1/companies?" + urlencode({"query": cik, "size": 100}))["items"]
        matches = []
        for hit in hits:
            identifiers = core.records("company-identifiers", companyId=hit["id"])
            if any(
                r["attributes"]["scheme"] == "cik" and r["attributes"]["value"] == cik
                for r in identifiers
            ):
                matches.append(hit["id"])
        if len(matches) > 1:
            raise ValueError(f"Ambiguous existing CIK identity for {config['ticker']}")
        # Use retained live-response evidence to establish the initial binding.
        fetch = next(
            r
            for r in json.loads((output / "fetch-report.json").read_text())
            if r["ticker"] == config["ticker"]
        )
        artifact = core.create(
            "raw-artifacts",
            evidenceIds=[],
            storageUri=sample["raw_reference"],
            sha256=fetch["sha256"],
            hashBasis="response_bytes",
            contentType=fetch["contentType"],
            byteLength=fetch["byteLength"],
            format="json",
            storedAt=fetch["retrievedAt"],
            originalResponseSha256=fetch["sha256"],
        )
        evidence = core.create(
            "source-references",
            evidenceIds=[],
            artifactId=artifact,
            sourceUrl=config["fetchUrl"],
            locator="$.cik / $.name",
            extractionMethod="api",
            extractorVersion=adapter_id,
            observedAt=fetch["retrievedAt"],
            availabilityPrecision="unknown",
        )
        company = (
            matches[0]
            if matches
            else core.call(
                "/api/v1/companies",
                {
                    "company": {
                        "evidenceIds": [evidence],
                        "slug": "sec-" + cik,
                        "displayName": config["expectedName"],
                        "entityKind": "issuer",
                    },
                    "identifiers": [
                        {
                            "evidenceIds": [evidence],
                            "scheme": "cik",
                            "authority": "SEC",
                            "value": cik,
                        }
                    ],
                },
            )["id"]
        )
        binding = next(
            (
                r["id"]
                for r in core.records("provider-bindings", providerId=provider, companyId=company)
                if r["attributes"]["sourceIdentifier"] == cik
                and r["attributes"]["verificationStatus"] == "verified"
            ),
            None,
        )
        if binding is None:
            binding = core.create(
                "provider-bindings",
                evidenceIds=[evidence],
                providerId=provider,
                companyId=company,
                sourceIdentifier=cik,
                identifierKind="cik",
                verificationStatus="verified",
                verifiedAt=now(),
            )
        target = core.call(
            "/api/v1/ingestion/targets",
            {
                "bindingId": binding,
                "endpointId": endpoint,
                "fetchUrl": config["fetchUrl"],
                "sourceId": "sec_submissions_profile",
                "adapterId": adapter_id,
                "registry": "SEC EDGAR filer directory",
                "registryJurisdiction": "US",
                "registrationKind": "unknown",
            },
        )["id"]
        job = core.call(f"/api/v1/ingestion/targets/{target}/refresh", {})
        entry = {
            "ticker": config["ticker"],
            "companyId": company,
            "targetId": target,
            "jobId": job["id"],
            "status": "queued",
        }
        report["companies"].append(entry)
        checkpoint()
        worker.run_once(job_id=job["id"])
        result = core.call(f"/api/v1/ingestion/jobs/{job['id']}")
        entry.update(
            status=result["status"], snapshotId=result["snapshotId"], errorCode=result["errorCode"]
        )
        checkpoint()
        if result["status"] != "succeeded":
            raise RuntimeError(f"{config['ticker']} job did not succeed; inspect {report_path}")
        overview = core.call(
            f"/api/v1/companies/{company}/overview?snapshotId={result['snapshotId']}"
        )
        (output / f"overview-{config['ticker']}.json").write_text(
            json.dumps(overview, indent=2) + "\n"
        )
        entry.update(verify_overview(core, overview, config))
        entry["overviewVerified"] = True
        checkpoint()
        print(config["ticker"], "succeeded", result["snapshotId"], flush=True)
        time.sleep(1)
    report["finishedAt"] = now()
    checkpoint()
    print("Report:", report_path)


def main():
    ingest(Settings())


if __name__ == "__main__":
    main()
