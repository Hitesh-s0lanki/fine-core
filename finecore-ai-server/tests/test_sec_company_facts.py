"""Raw SEC extraction boundaries and the durable worker's financial completion contract."""

import hashlib
import json
from pathlib import Path
from urllib.error import URLError

import pytest

from app.config import Settings
from services.ingestion_worker import Worker
from services.sec_company_facts import CONTRACT_BYTES, PARSER_ID, SOURCE_ID, parse_company_facts

CIK = "0000320193"
SELECTED = {"taxonomy": "us-gaap", "concepts": ["Assets"]}


def payload(token="123.4500", **changes):
    observation = {
        "val": "__TOKEN__",
        "end": "2025-09-30",
        "accn": "0001193125-25-000001",
        "form": "10-K",
        "filed": "2025-10-31",
        "fy": 2025,
        "fp": "FY",
        "frame": "CY2025Q3I",
        **changes,
    }
    result = {
        "cik": 320193,
        "entityName": "Test issuer",
        "facts": {
            "us-gaap": {
                "Assets": {
                    "label": "Assets",
                    "description": "Reported assets",
                    "units": {"USD": [observation]},
                }
            }
        },
    }
    return json.dumps(result).replace('"__TOKEN__"', token).encode()


@pytest.mark.parametrize(
    "token,expected",
    [
        ("123.4500", "123.4500"),
        ("1.2345e+5", "123450"),
        ("-0.000", "-0.000"),
        ("0.123456789012345678", "0.123456789012345678"),
        (
            "123456789012345678901234567890123456789012",
            "123456789012345678901234567890123456789012",
        ),
    ],
)
def test_exact_numeric_tokens_metadata_and_source_locator(token, expected):
    records = parse_company_facts(payload(token), CIK, SELECTED)
    assert len(records) == 1
    fact = records[0]
    assert fact["value"] == expected
    assert fact["reportedValueRaw"] == token
    assert fact["accession"] == "0001193125-25-000001"  # Prefix need not match issuer CIK.
    assert fact["filedOn"] == "2025-10-31"
    assert fact["fiscalYear"] == 2025
    assert fact["fiscalPeriod"] == "FY"
    assert fact["frame"] == "CY2025Q3I"
    assert fact["periodStart"] is None
    assert fact["unit"] == "USD"
    assert fact["locator"] == "/facts/us-gaap/Assets/units/USD/0"


def test_every_native_unit_and_reporting_period_is_retained_without_selection():
    data = json.loads(payload("1", start="2025-01-01"))
    units = data["facts"]["us-gaap"]["Assets"]["units"]
    units["USD/shares"] = [dict(units["USD"][0], val=2, end="2025-06-30", fp="Q2")]
    records = parse_company_facts(json.dumps(data).encode(), CIK, SELECTED)
    assert [(r["unit"], r["periodEnd"]) for r in records] == [
        ("USD", "2025-09-30"),
        ("USD/shares", "2025-06-30"),
    ]
    assert records[1]["locator"] == "/facts/us-gaap/Assets/units/USD~1shares/0"
    assert records[0]["periodStart"] == "2025-01-01"


@pytest.mark.parametrize("token", ['"1.2"', "true", "NaN", "1e42", "1e-19", "1e999999"])
def test_invalid_or_unrepresentable_values_reject_the_whole_response(token):
    with pytest.raises(ValueError):
        parse_company_facts(payload(token), CIK, SELECTED)


@pytest.mark.parametrize(
    "changes",
    [
        {"start": "2026-01-01"},
        {"end": "2025-02-30"},
        {"filed": None},
        {"accn": "made-up"},
        {"fy": "2025"},
    ],
)
def test_invalid_period_and_accession_metadata_reject_the_whole_response(changes):
    with pytest.raises(ValueError):
        parse_company_facts(payload(**changes), CIK, SELECTED)


def test_wrong_identity_and_duplicate_keys_are_rejected():
    with pytest.raises(ValueError):
        parse_company_facts(payload(), "0000000001", SELECTED)
    with pytest.raises(ValueError):
        parse_company_facts(b'{"cik":320193,"cik":1}', CIK, SELECTED)


def test_missing_or_empty_selected_concept_is_not_silently_omitted():
    data = json.loads(payload())
    data["facts"]["us-gaap"]["Empty"] = {"units": {"USD": []}}
    selected = {"taxonomy": "us-gaap", "concepts": ["Assets", "Empty"]}
    with pytest.raises(ValueError, match="no observations"):
        parse_company_facts(json.dumps(data).encode(), CIK, selected)
    with pytest.raises(ValueError, match="unavailable"):
        parse_company_facts(payload(), CIK, {"taxonomy": "us-gaap", "concepts": ["Missing"]})


def test_selection_and_total_fact_limits_reject_instead_of_truncating():
    for selected in [
        {"taxonomy": "custom", "concepts": ["Assets"]},
        {"taxonomy": "us-gaap", "concepts": []},
        {"taxonomy": "us-gaap", "concepts": ["Assets", "Assets"]},
        {"taxonomy": "us-gaap", "concepts": ["Assets"] * 21},
    ]:
        with pytest.raises(ValueError):
            parse_company_facts(payload(), CIK, selected)
    data = json.loads(payload())
    units = data["facts"]["us-gaap"]["Assets"]["units"]
    units["USD"] *= 5001
    with pytest.raises(ValueError, match="fact limit"):
        parse_company_facts(json.dumps(data).encode(), CIK, SELECTED)


def test_checked_in_descriptor_digest_matches_the_java_contract_pin():
    assert hashlib.sha256(CONTRACT_BYTES).hexdigest() == PARSER_ID
    java = (
        Path(__file__).resolve().parents[2]
        / "fincore-java-server/src/main/java/com/fincore/server/dto/ingestion"
        / "SecFinancialContracts.java"
    ).read_text()
    assert PARSER_ID in java
    assert json.loads(CONTRACT_BYTES)["digest_scope"].endswith("not executable parser code")


class Core:
    def __init__(self):
        self.calls = []
        self.job = {
            "id": "11111111-1111-1111-1111-111111111111",
            "attempt": 1,
            "leaseToken": "lease",
            "companyId": "company",
            "sourceIdentifier": CIK,
            "target": {
                "fetchUrl": f"https://data.sec.gov/api/xbrl/companyfacts/CIK{CIK}.json",
                "sourceId": SOURCE_ID,
                "adapterId": PARSER_ID,
                "operation": "sec_company_facts",
                "selection": SELECTED,
            },
        }

    def post(self, path, body):
        self.calls.append((path, body))
        return self.job if path.startswith("/jobs/claim") else {"status": "succeeded"}


class Source:
    def __init__(self, raw, status=200):
        self.raw, self.status, self.calls = raw, status, []

    def request(self, url, source=False):
        self.calls.append(url)
        return self.status, "application/json", self.raw


def settings(tmp_path):
    return Settings(
        _env_file=None,
        ingestion_raw_dir=tmp_path / "raw",
        ingestion_allowed_origins=["https://data.sec.gov"],
        ingestion_user_agent="FinCore test test@example.org",
    )


def test_worker_retains_exact_source_and_sends_financial_completion(tmp_path):
    raw, core = payload("1.2345e+5"), Core()
    assert Worker(settings(tmp_path), core, Source(raw)).run_once()
    path, result = core.calls[-1]
    assert path.endswith("/complete-financials")
    assert result["cik"] == CIK
    assert result["adapterId"] == PARSER_ID
    assert result["records"][0]["reportedValueRaw"] == "1.2345e+5"
    artifact = result["artifact"]
    assert Path(artifact["storageUri"].removeprefix("file://")).read_bytes() == raw
    assert artifact["sha256"] == hashlib.sha256(raw).hexdigest()


@pytest.mark.parametrize(
    "field,value", [("adapterId", "0" * 64), ("fetchUrl", "https://data.sec.gov/other")]
)
def test_worker_rejects_unpinned_or_unexpected_sec_operation_before_fetch(tmp_path, field, value):
    core, source = Core(), Source(payload())
    core.job["target"][field] = value
    Worker(settings(tmp_path), core, source).run_once()
    assert not source.calls
    assert core.calls[-1][1]["errorCode"] == "configuration"


@pytest.mark.parametrize("status,code", [(429, "rate_limited"), (503, "provider_error")])
def test_worker_retains_retryable_provider_response(tmp_path, status, code):
    core = Core()
    Worker(settings(tmp_path), core, Source(b"blocked", status)).run_once()
    assert core.calls[-1][1]["errorCode"] == code
    assert core.calls[-1][1]["artifact"] is not None


def test_invalid_selected_financial_payload_is_retained_but_never_completed(tmp_path):
    core = Core()
    Worker(settings(tmp_path), core, Source(payload('"1.2"'))).run_once()
    assert core.calls[-1][0].endswith("/fail")
    assert core.calls[-1][1]["errorCode"] == "invalid_payload"
    assert core.calls[-1][1]["artifact"] is not None


def test_ambiguous_financial_acknowledgement_never_reports_false_failure(tmp_path):
    core = Core()
    original = core.post

    def post(path, body):
        if path.endswith("/complete-financials"):
            raise URLError("acknowledgement lost")
        return original(path, body)

    core.post = post
    with pytest.raises(URLError):
        Worker(settings(tmp_path), core, Source(payload())).run_once()
    assert not any(path.endswith("/fail") for path, _ in core.calls)
