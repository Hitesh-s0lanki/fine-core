"""Offline regression checks for the checked-in SEC identity mapping."""

import json
from pathlib import Path

import pytest

from schemas.adapters import Adapter, Sample
from services.adapter_service import execute

BUNDLE = Path(__file__).resolve().parents[1] / "adapters" / "sec-submissions-profile"


@pytest.mark.parametrize("company", json.loads((BUNDLE / "companies.json").read_text()))
def test_identity_mapping_preserves_cik_and_does_not_invent_registration_details(company):
    adapter = Adapter.model_validate_json((BUNDLE / "adapter.json").read_text())
    sample = Sample(
        company_id="fixture",
        expected_source_identifier=company["expectedSourceIdentifier"],
        raw_reference="synthetic:sec-shape",
        payload={
            "cik": company["expectedSourceIdentifier"],
            "name": company["expectedName"],
            "stateOfIncorporation": "DE",
            "filings": {"files": [{"name": "additional-history.json"}]},
        },
    )
    assert execute(adapter, sample) == [
        {
            "record_type": "company_profile",
            "registration_number": company["expectedSourceIdentifier"],
            "name": company["expectedName"],
            "status": None,
            "jurisdiction": None,
        }
    ]
    sample.expected_source_identifier = "WRONG"
    with pytest.raises(ValueError, match="identifier mismatch"):
        execute(adapter, sample)
