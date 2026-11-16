"""Fetch five SEC identity samples and certify the supplied adapter, without Java writes.

From the Python module directory:
FINECORE_INGESTION_USER_AGENT='FinCore Your Name contact@your-domain.tld' \
uv run python -m services.sec_live_prepare
"""

import hashlib
import json
import time
from pathlib import Path

from app.config import Settings
from schemas.adapters import Candidate, Sample
from services.adapter_service import normalize, register, validate
from services.ingestion_worker import Transport, now, reject_constant, unique_object


def prepare(settings, bundle_dir, output_dir):
    """Use independently specified names/CIKs; retain full original responses for replay."""
    agent = settings.ingestion_user_agent
    if "@" not in agent or "example.com" in agent or "your-domain" in agent:
        raise ValueError("Configure a real identifying User-Agent with your contact email")
    adapter = json.loads((bundle_dir / "adapter.json").read_text())
    companies = json.loads((bundle_dir / "companies.json").read_text())
    output_dir.mkdir(parents=True, exist_ok=True)
    transport = Transport(settings)
    samples = []
    checks = []
    for company in companies:
        status, content_type, raw = transport.request(company["fetchUrl"], source=True)
        path = output_dir / f"{company['ticker']}.json"
        path.write_bytes(raw)
        checks.append(
            {
                "ticker": company["ticker"],
                "url": company["fetchUrl"],
                "retrievedAt": now(),
                "httpStatus": status,
                "contentType": content_type,
                "sha256": hashlib.sha256(raw).hexdigest(),
                "byteLength": len(raw),
            }
        )
        (output_dir / "fetch-report.json").write_text(json.dumps(checks, indent=2) + "\n")
        if status != 200:
            raise ValueError(f"SEC fetch failed for {company['ticker']}: HTTP {status}")
        payload = json.loads(raw, object_pairs_hook=unique_object, parse_constant=reject_constant)
        samples.append(
            {
                # Certification labels only; production requires real Java company UUIDs.
                "company_id": f"certification:{company['ticker']}",
                "expected_source_identifier": company["expectedSourceIdentifier"],
                "raw_reference": path.resolve().as_uri(),
                "payload": payload,
                "expected_records": [
                    {
                        "record_type": "company_profile",
                        "registration_number": company["expectedSourceIdentifier"],
                        "name": company["expectedName"],
                    }
                ],
            }
        )
        # Conservative single-process test pace; this is not a distributed rate limiter.
        time.sleep(1)
    candidate = Candidate(adapter=adapter, samples=samples)
    validate(candidate)
    (output_dir / "candidate.json").write_text(candidate.model_dump_json(indent=2) + "\n")
    registration = register(candidate, settings.adapter_registry_dir)
    results = [
        normalize(
            registration["adapter_id"], Sample.model_validate(sample), settings.adapter_registry_dir
        )
        for sample in samples
    ]
    (output_dir / "normalized.json").write_text(json.dumps(results, indent=2) + "\n")
    (output_dir / "registration.json").write_text(json.dumps(registration, indent=2) + "\n")
    print(
        json.dumps(
            {**registration, "companiesPassed": len(samples), "outputDir": str(output_dir)},
            indent=2,
        )
    )


def main():
    root = Path(__file__).resolve().parents[3]
    settings = Settings()
    prepare(
        settings,
        root / "finecore-ai-server" / "adapters" / "sec-submissions-profile",
        root / "temp" / "sec-live-profile",
    )


if __name__ == "__main__":
    main()
