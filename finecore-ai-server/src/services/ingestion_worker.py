"""Durable Java-coordinated worker for unpaged JSON company profiles.

Run separately from FastAPI: uv run python -m services.ingestion_worker [--once].
Only source origins explicitly allowed by the operator may be fetched. Adapters
run locally through the existing deterministic engine; no model is invoked.
"""

import argparse
import hashlib
import json
import logging
import os
import time
from datetime import UTC, datetime
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener

from app.config import Settings
from schemas.adapters import Sample
from services.adapter_service import normalize

LOG = logging.getLogger(__name__)


class NoRedirects(HTTPRedirectHandler):
    """Require explicit configuration for every source origin, including redirects."""

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def now():
    return datetime.now(UTC).isoformat()


def origin(url):
    parsed = urlsplit(url)
    if (
        parsed.scheme not in {"http", "https"}
        or not parsed.hostname
        or parsed.username is not None
        or parsed.password is not None
        or parsed.query
        or parsed.fragment
    ):
        raise ValueError("Expected a credential-free HTTP URL without query or fragment")
    port = parsed.port or (443 if parsed.scheme == "https" else 80)
    return parsed.scheme, parsed.hostname.lower(), port


class Transport:
    """Bounded HTTP transport; redirects are never followed implicitly."""

    def __init__(self, settings):
        self.settings = settings
        self.opener = build_opener(NoRedirects())

    def request(self, url, body=None, source=False):
        headers = {"Accept": "application/json", "User-Agent": self.settings.ingestion_user_agent}
        encoded = None
        if body is not None:
            encoded = json.dumps(body).encode()
            headers["Content-Type"] = "application/json"
        request = Request(url, data=encoded, headers=headers)
        try:
            response = self.opener.open(request, timeout=30)
        except HTTPError as exc:
            if not source:
                exc.close()
                raise
            response = exc
        with response:
            data = response.read(self.settings.ingestion_max_bytes + 1)
            if len(data) > self.settings.ingestion_max_bytes:
                raise ValueError("Response exceeds configured byte limit")
            return (
                response.status,
                response.headers.get("Content-Type", "application/octet-stream"),
                data,
            )


class CoreClient:
    """Internal Java API client. A failed completion call is left for lease recovery."""

    def __init__(self, settings, transport):
        origin(settings.core_api_url)
        self.base = settings.core_api_url.rstrip("/") + "/api/v1/ingestion"
        self.transport = transport

    def post(self, path, body):
        status, _, data = self.transport.request(self.base + path, body)
        return None if status == 204 else json.loads(data)


def store_raw(directory: Path, job, content, content_type):
    """Write original bytes once per job attempt, atomically, before parsing them."""
    folder = directory.resolve() / str(job["id"]) / str(job["attempt"])
    folder.mkdir(parents=True, exist_ok=True)
    path = folder / "response.bin"
    temporary = folder / "response.pending"
    with temporary.open("wb") as file:
        file.write(content)
        file.flush()
        os.fsync(file.fileno())
    temporary.replace(path)
    return {
        "storageUri": path.as_uri(),
        "sha256": hashlib.sha256(content).hexdigest(),
        "byteLength": len(content),
        "contentType": content_type[:200],
        "storedAt": now(),
    }


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("Duplicate JSON object key")
        result[key] = value
    return result


def reject_constant(value):
    raise ValueError("Non-finite JSON number")


class Worker:
    """Claims one job at a time with bounded retries owned by Java."""

    def __init__(self, settings, core=None, transport=None):
        self.settings = settings
        self.transport = transport or Transport(settings)
        self.core = core or CoreClient(settings, self.transport)

    def run_once(self, job_id=None):
        path = "/jobs/claim" if job_id is None else f"/jobs/claim?jobId={job_id}"
        job = self.core.post(path, {})
        if job is None:
            return False
        requested = now()
        artifact = None
        status = None
        target = job["target"]
        code = "configuration"
        try:
            allowed = {origin(url) for url in self.settings.ingestion_allowed_origins}
            if origin(target["fetchUrl"]) not in allowed:
                raise ValueError("Source origin is not allowed")
            code = "network"
            status, content_type, raw = self.transport.request(target["fetchUrl"], source=True)
            retrieved = now()
            artifact = store_raw(self.settings.ingestion_raw_dir, job, raw, content_type)
            if status != 200:
                code = (
                    "rate_limited"
                    if status == 429
                    else "provider_error"
                    if status >= 500
                    else "configuration"
                )
                raise ValueError("Source request failed")
            code = "invalid_payload"
            payload = json.loads(
                raw, object_pairs_hook=unique_object, parse_constant=reject_constant
            )
            code = "normalization"
            result = normalize(
                target["adapterId"],
                Sample(
                    company_id=job["companyId"],
                    expected_source_identifier=job["sourceIdentifier"],
                    raw_reference=artifact["storageUri"],
                    payload=payload,
                ),
                self.settings.adapter_registry_dir,
            )
            if result["source_id"] != target["sourceId"]:
                raise ValueError("Adapter source mismatch")
            code = "unsupported_record"
            records = result["records"]
            if len(records) != 1 or records[0]["record_type"] != "company_profile":
                raise ValueError("The first bridge accepts exactly one company profile")
            profile = records[0]
            if profile["registration_number"] != job["sourceIdentifier"]:
                raise ValueError("Registration and verified binding differ")
        except Exception as exc:
            # Do not put provider bodies, URLs or exception messages in logs or the job record.
            if isinstance(exc, OSError) and not isinstance(exc, (TimeoutError, URLError)):
                raise
            if isinstance(exc, (TimeoutError, URLError)):
                reason = getattr(exc, "reason", exc)
                code = "timeout" if isinstance(reason, TimeoutError) else "network"
            elif code == "network":
                code = "invalid_payload"
            self.core.post(
                f"/jobs/{job['id']}/fail",
                {
                    "leaseToken": job["leaseToken"],
                    "errorCode": code,
                    "requestedAt": requested,
                    "retrievedAt": now(),
                    "httpStatus": status,
                    "artifact": artifact,
                },
            )
            LOG.warning("Ingestion job %s failed: %s", job["id"], code)
            return True
        # A transport failure here may follow a successful server commit. Never
        # report it as failed work: the completion endpoint is idempotent and a
        # crashed/unacknowledged job is recovered through its fenced lease.
        self.core.post(
            f"/jobs/{job['id']}/complete",
            {
                "leaseToken": job["leaseToken"],
                "requestedAt": requested,
                "retrievedAt": retrieved,
                "artifact": artifact,
                "adapterId": result["adapter_id"],
                "sourceId": result["source_id"],
                "records": [
                    {
                        "recordType": profile["record_type"],
                        "name": profile["name"],
                        "registrationNumber": profile["registration_number"],
                        "status": profile["status"],
                        "jurisdiction": profile["jurisdiction"],
                    }
                ],
            },
        )
        LOG.info("Ingestion job %s completed", job["id"])
        return True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--once", action="store_true", help="Claim at most one job, then exit")
    args = parser.parse_args()
    logging.basicConfig(level=logging.INFO)
    settings = Settings()
    worker = Worker(settings)
    while True:
        try:
            worked = worker.run_once()
        except Exception:
            LOG.error("Core API or storage operation failed; lease recovery remains available")
            if args.once:
                raise SystemExit(1) from None
            worked = False
        if args.once:
            return
        if not worked:
            time.sleep(settings.ingestion_poll_seconds)


if __name__ == "__main__":
    main()
