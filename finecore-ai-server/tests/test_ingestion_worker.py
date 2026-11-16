"""Worker boundaries: exact raw retention, pinned adapter reuse and failure reporting."""

import hashlib
import json
from pathlib import Path
from urllib.error import URLError

import pytest

from app.config import Settings
from schemas.adapters import Candidate
from services.adapter_service import register
from services.ingestion_worker import Worker


class Core:
    def __init__(self, job):
        self.job = job
        self.calls = []

    def post(self, path, body):
        self.calls.append((path, body))
        if path == "/jobs/claim":
            return self.job
        return {"status": "succeeded"}


class Source:
    def __init__(self, raw, status=200, error=None):
        self.raw, self.status, self.error = raw, status, error
        self.calls = []

    def request(self, url, source=False):
        self.calls.append(url)
        if self.error:
            raise self.error
        return self.status, "application/json", self.raw


@pytest.fixture
def setup(tmp_path):
    adapter = {
        "source_id": "test_profile",
        "identity_path": ["number"],
        "semantic_notes": ["number is the registry registration number"],
        "fields": {
            "record_type": {"constant": "company_profile"},
            "name": {"path": ["name"]},
            "registration_number": {"path": ["number"]},
        },
    }
    samples = [
        {
            "company_id": str(i),
            "expected_source_identifier": f"FC00{i}",
            "raw_reference": "fixture",
            "payload": {"number": f"FC00{i}", "name": f"Issuer {i}"},
            "expected_records": [
                {
                    "record_type": "company_profile",
                    "name": f"Issuer {i}",
                    "registration_number": f"FC00{i}",
                }
            ],
        }
        for i in (1, 2)
    ]
    settings = Settings(
        _env_file=None,
        adapter_registry_dir=tmp_path / "adapters",
        ingestion_raw_dir=tmp_path / "raw",
        ingestion_allowed_origins=["http://127.0.0.1:9999"],
    )
    adapter_id = register(
        Candidate(adapter=adapter, samples=samples), settings.adapter_registry_dir
    )["adapter_id"]
    job = {
        "id": "11111111-1111-1111-1111-111111111111",
        "attempt": 1,
        "leaseToken": "lease",
        "companyId": "company-1",
        "sourceIdentifier": "FC001",
        "target": {
            "fetchUrl": "http://127.0.0.1:9999/profile",
            "sourceId": "test_profile",
            "adapterId": adapter_id,
        },
    }
    return settings, Core(job)


def test_fetch_retains_exact_bytes_and_commits_profile(setup):
    settings, core = setup
    raw = b'{ "number": "FC001", "name": "Issuer One" }\n'
    assert Worker(settings, core, Source(raw)).run_once()
    path, body = core.calls[-1]
    assert path.endswith("/complete")
    artifact = body["artifact"]
    assert artifact["sha256"] == hashlib.sha256(raw).hexdigest()
    assert artifact["byteLength"] == len(raw)
    assert Path(artifact["storageUri"].removeprefix("file://")).read_bytes() == raw
    assert body["records"][0]["registrationNumber"] == "FC001"
    assert body["records"][0]["name"] == "Issuer One"
    assert body["adapterId"] == core.job["target"]["adapterId"]


@pytest.mark.parametrize(
    "raw,status,error,expected",
    [
        (b"{}", 429, None, "rate_limited"),
        (b"{}", 503, None, "provider_error"),
        (b"{}", 403, None, "configuration"),
        (b"<html>blocked</html>", 200, None, "invalid_payload"),
        (b'{"number":"FC001","number":"OTHER"}', 200, None, "invalid_payload"),
        (b'{"number":"OTHER","name":"Other"}', 200, None, "normalization"),
        (b'{"number":"FC001"}', 200, None, "normalization"),
        (b"", 200, URLError("offline"), "network"),
        (b"", 200, TimeoutError(), "timeout"),
    ],
)
def test_failure_classification_and_raw_retention(setup, raw, status, error, expected):
    settings, core = setup
    Worker(settings, core, Source(raw, status, error)).run_once()
    path, body = core.calls[-1]
    assert path.endswith("/fail")
    assert body["errorCode"] == expected
    assert (body["artifact"] is not None) == (error is None)
    assert not any(path.endswith("/complete") for path, _ in core.calls)


def test_unapproved_origin_is_never_fetched(setup):
    settings, core = setup
    core.job["target"]["fetchUrl"] = "http://169.254.169.254/profile"
    source = Source(b"{}")
    Worker(settings, core, source).run_once()
    assert not source.calls
    assert core.calls[-1][1]["errorCode"] == "configuration"


def test_no_work_has_no_source_side_effects(setup):
    settings, core = setup
    core.job = None
    source = Source(b"{}")
    assert not Worker(settings, core, source).run_once()
    assert not source.calls


def test_ambiguous_completion_failure_does_not_report_a_false_failure(setup):
    settings, core = setup
    original = core.post

    def post(path, body):
        if path.endswith("/complete"):
            raise URLError("acknowledgement lost")
        return original(path, body)

    core.post = post
    with pytest.raises(URLError):
        Worker(
            settings, core, Source(json.dumps({"number": "FC001", "name": "One"}).encode())
        ).run_once()
    assert not any(path.endswith("/fail") for path, _ in core.calls)


def test_storage_failure_leaves_lease_for_recovery(setup, monkeypatch):
    settings, core = setup

    def broken_store(*args):
        raise OSError("disk unavailable")

    monkeypatch.setattr("services.ingestion_worker.store_raw", broken_store)
    with pytest.raises(OSError):
        Worker(settings, core, Source(b"{}")).run_once()
    assert len(core.calls) == 1


def test_financial_output_is_explicitly_rejected(setup, monkeypatch):
    settings, core = setup
    monkeypatch.setattr(
        "services.ingestion_worker.normalize",
        lambda *args: {"source_id": "test_profile", "records": [{"record_type": "financial_fact"}]},
    )
    Worker(settings, core, Source(b"{}")).run_once()
    assert core.calls[-1][1]["errorCode"] == "unsupported_record"


def test_specific_job_claim_does_not_poll_general_queue(setup):
    settings, _ = setup
    calls = []

    class TargetedCore:
        def post(self, path, body):
            calls.append(path)
            return None

    source = Source(b"{}")
    assert not Worker(settings, TargetedCore(), source).run_once(
        job_id="11111111-1111-1111-1111-111111111111"
    )
    assert calls == ["/jobs/claim?jobId=11111111-1111-1111-1111-111111111111"]
    assert not source.calls
