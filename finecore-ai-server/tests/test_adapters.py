"""Certification and replay tests with independently specified expected records."""

from copy import deepcopy
from decimal import Decimal

import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError

from app.config import Settings
from app.main import create_app
from schemas.adapters import Adapter, Sample
from services.adapter_service import execute


@pytest.fixture
def client(tmp_path):
    with TestClient(
        create_app(Settings(_env_file=None, environment="test", adapter_registry_dir=tmp_path))
    ) as value:
        yield value


@pytest.fixture
def candidate():
    samples = []
    for cid, number, name in [("first", "FC001", "First"), ("second", "FC002", "Second")]:
        samples.append(
            {
                "company_id": cid,
                "expected_source_identifier": number,
                "raw_reference": f"fixture:{cid}",
                "payload": {
                    "number": number,
                    "legal_name": name,
                    "fields": [{"label": "Status", "value": "Active"}],
                },
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
    return {
        "adapter": {
            "source_id": "register_test",
            "identity_path": ["number"],
            "semantic_notes": ["The number identifies the overseas registration."],
            "fields": {
                "record_type": {"constant": "company_profile"},
                "name": {"path": ["legal_name"]},
                "registration_number": {"path": ["number"]},
                "status": {"path": ["fields", {"key": "label", "equals": "Status"}, "value"]},
            },
        },
        "samples": samples,
    }


def test_register_replay_and_version(client, candidate):
    assert client.post("/adapters/validate", json=candidate).status_code == 200
    response = client.post("/adapters/register", json=candidate)
    assert response.status_code == 200
    adapter_id = response.json()["adapter_id"]
    assert client.post("/adapters/register", json=candidate).json()["adapter_id"] == adapter_id
    result = client.post(
        f"/adapters/{adapter_id}/normalize", json={"sample": candidate["samples"][0]}
    ).json()
    assert result["records"][0]["name"] == "First"
    assert result["company_id"] == "first"
    assert result["raw_reference"] == "fixture:first"
    assert len(result["raw_sha256"]) == 64
    candidate["adapter"]["semantic_notes"].append("Reviewed revision.")
    assert client.post("/adapters/register", json=candidate).json()["adapter_id"] != adapter_id


@pytest.mark.parametrize("change", ["identity", "missing", "ambiguous", "wrong_expected"])
def test_bad_samples_cannot_register(client, candidate, change):
    sample = candidate["samples"][0]
    if change == "identity":
        sample["payload"]["number"] = "OTHER"
    elif change == "missing":
        del sample["payload"]["legal_name"]
    elif change == "ambiguous":
        sample["payload"]["fields"] *= 2
    else:
        sample["expected_records"][0]["name"] = "Wrong Name"
    assert client.post("/adapters/register", json=candidate).status_code == 422
    assert not list(client.app.state.settings.adapter_registry_dir.glob("*.json"))


def test_missing_expected_and_executable_fields_rejected(client, candidate):
    candidate["samples"][0]["expected_records"] = None
    assert client.post("/adapters/register", json=candidate).status_code == 422
    candidate["adapter"]["script"] = "__import__('os').system('false')"
    assert client.post("/adapters/register", json=candidate).status_code == 422


def test_prepare_excludes_answer_keys(client, candidate):
    response = client.post(
        "/adapters/prepare",
        json={
            "source_id": "register_test",
            "documentation": "Legal name and registration number.",
            "samples": candidate["samples"],
        },
    )
    assert response.status_code == 200
    bundle = response.json()
    assert bundle["status"] == "ready_for_model"
    assert "expected_records" not in bundle["source_bundle"]["samples"][0]
    assert bundle["adapter_json_schema"] and bundle["normalized_record_json_schema"]


def test_provider_extensions_and_reordered_fields(client, candidate):
    sample = candidate["samples"][0]
    sample["payload"]["new_field"] = "provider extension"
    sample["payload"]["fields"].insert(0, {"label": "Other", "value": "Ignore"})
    assert client.post("/adapters/validate", json=candidate).status_code == 200


def test_tampered_and_unknown_adapter(client, candidate):
    adapter_id = client.post("/adapters/register", json=candidate).json()["adapter_id"]
    file = client.app.state.settings.adapter_registry_dir / f"{adapter_id}.json"
    file.write_text(file.read_text().replace("register_test", "tampered"))
    body = {"sample": candidate["samples"][0]}
    assert client.post(f"/adapters/{adapter_id}/normalize", json=body).status_code == 409
    assert client.post(f"/adapters/{'0' * 64}/normalize", json=body).status_code == 404


def test_financial_precision_units_and_periods():
    fields = {
        key: {"constant": value}
        for key, value in {
            "record_type": "financial_fact",
            "metric": "REVENUE",
            "scope": "consolidated",
            "original_concept": "revenue",
            "filing_reference": "annual-2025",
        }.items()
    }
    fields.update(
        {
            "value": {"path": ["amount"], "transform": "decimal"},
            "unit": {"path": ["currency"]},
            "period_start": {"path": ["start"], "transform": "iso_date"},
            "period_end": {"path": ["end"], "transform": "iso_date"},
        }
    )
    adapter = Adapter.model_validate(
        {
            "source_id": "financial_test",
            "identity_path": ["symbol"],
            "fields": fields,
            "semantic_notes": ["Documented USD units, annual consolidated revenue."],
        }
    )
    sample = Sample.model_validate(
        {
            "company_id": "first",
            "expected_source_identifier": "FIRST",
            "raw_reference": "fixture:financial",
            "payload": {
                "symbol": "FIRST",
                "amount": "9007199254740993.01",
                "currency": "USD",
                "start": "2025-01-01",
                "end": "2025-12-31",
            },
        }
    )
    result = execute(adapter, sample)[0]
    assert Decimal(result["value"]) == Decimal("9007199254740993.01")
    assert result["unit"] == "USD"
    for bad in (float("nan"), "NaN", True, 1.1):
        broken = deepcopy(sample)
        broken.payload["amount"] = bad
        with pytest.raises(ValueError):
            execute(adapter, broken)
    sample.payload["end"] = "2024-01-01"
    with pytest.raises(ValidationError):
        execute(adapter, sample)


def test_empty_runtime_response_and_mixed_company_batch(candidate):
    adapter = Adapter.model_validate(candidate["adapter"])
    sample = Sample.model_validate(candidate["samples"][0])
    sample.payload = []
    assert execute(adapter, sample) == []
    row = candidate["samples"][0]["payload"]
    other = deepcopy(row)
    other["number"] = "WRONG-COMPANY"
    sample.payload = [row, other]
    with pytest.raises(ValueError, match="Company identifier mismatch"):
        execute(adapter, sample)


def test_empty_samples_cannot_certify(client, candidate):
    for sample in candidate["samples"]:
        sample["payload"] = []
        sample["expected_records"] = []
    assert client.post("/adapters/register", json=candidate).status_code == 422
