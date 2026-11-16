"""Version-pinned extraction of selected raw SEC Company Facts, without metric selection.

All periods/units/filings for the explicitly selected concepts are retained. No
annual selection, restatement precedence, currency conversion or canonical metric
mapping happens here. Original numeric JSON tokens are preserved alongside exact
decimal strings; the complete HTTP bytes remain the primary source artifact.
"""

import hashlib
import json
import re
from datetime import date
from decimal import Decimal
from pathlib import Path

PARSER_VERSION = "sec-companyfacts-v1"
CONTRACT_BYTES = Path(__file__).with_name("sec_company_facts_contract.json").read_bytes()
PARSER_ID = hashlib.sha256(CONTRACT_BYTES).hexdigest()
SOURCE_ID = "sec_company_facts"
MAX_FACTS = 5_000
TAXONOMIES = {"us-gaap", "ifrs-full", "dei", "srt"}
CONCEPT = re.compile(r"[A-Za-z_][A-Za-z0-9_.-]{0,199}\Z")


class NumberToken(str):
    """A JSON number's original source spelling, distinct from a quoted string."""


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("Duplicate JSON object key")
        result[key] = value
    return result


def invalid_constant(value):
    raise ValueError("Non-finite source number")


def text(value, limit, required=True):
    if value is None and not required:
        return None
    if type(value) is not str or (required and not value.strip()) or len(value) > limit:
        raise ValueError("Invalid SEC text field")
    return value


def iso_date(value):
    value = text(value, 10)
    if date.fromisoformat(value).isoformat() != value:
        raise ValueError("Expected ISO source date")
    return value


def pointer(value):
    return value.replace("~", "~0").replace("/", "~1")


def selection(value):
    """Validate the explicit, bounded raw taxonomy/concept selection."""
    if not isinstance(value, dict) or set(value) != {"taxonomy", "concepts"}:
        raise ValueError("Expected taxonomy and concepts selection")
    taxonomy, concepts = value["taxonomy"], value["concepts"]
    if (
        type(taxonomy) is not str
        or taxonomy not in TAXONOMIES
        or not isinstance(concepts, list)
        or not 1 <= len(concepts) <= 20
        or any(type(tag) is not str or not CONCEPT.fullmatch(tag) for tag in concepts)
        or len(set(concepts)) != len(concepts)
    ):
        raise ValueError("Invalid SEC concept selection")
    return taxonomy, concepts


def parse_company_facts(raw, expected_cik, selected):
    """Return exact selected raw observations or reject the whole response."""
    taxonomy, concepts = selection(selected)
    payload = json.loads(
        raw,
        object_pairs_hook=unique_object,
        parse_int=NumberToken,
        parse_float=NumberToken,
        parse_constant=invalid_constant,
    )
    if not isinstance(payload, dict):
        raise ValueError("Expected Company Facts object")
    cik = payload.get("cik")
    if not isinstance(cik, NumberToken) or not re.fullmatch(r"[1-9][0-9]{0,9}", cik):
        raise ValueError("Invalid Company Facts CIK")
    if not re.fullmatch(r"(?!0000000000)[0-9]{10}", expected_cik):
        raise ValueError("Expected verified ten-digit CIK")
    if cik.zfill(10) != expected_cik:
        raise ValueError("Company Facts CIK differs from binding")
    facts = payload.get("facts")
    if not isinstance(facts, dict) or not isinstance(facts.get(taxonomy), dict):
        raise ValueError("Selected taxonomy is unavailable")
    records = []
    for tag in sorted(concepts):
        concept = facts[taxonomy].get(tag)
        if not isinstance(concept, dict) or not isinstance(concept.get("units"), dict):
            raise ValueError("Selected concept is unavailable or malformed")
        label = text(concept.get("label"), 40000, required=False)
        description = text(concept.get("description"), 40000, required=False)
        before = len(records)
        for unit, observations in sorted(concept["units"].items()):
            text(unit, 200)
            if not isinstance(observations, list):
                raise ValueError("Expected fact array")
            for index, fact in enumerate(observations):
                if not isinstance(fact, dict):
                    raise ValueError("Expected fact object")
                token = fact.get("val")
                if not isinstance(token, NumberToken) or len(token) > 200:
                    raise ValueError("Expected a source JSON number")
                value = Decimal(token)
                # Check magnitude before formatting to avoid huge exponent expansion.
                if (
                    not value.is_finite()
                    or value.adjusted() > 41
                    or value.as_tuple().exponent < -18
                ):
                    raise ValueError("Source value exceeds supported decimal precision")
                exact = format(value, "f")
                start = iso_date(fact["start"]) if "start" in fact else None
                end = iso_date(fact.get("end"))
                filed = iso_date(fact.get("filed"))
                if start is not None and start > end:
                    raise ValueError("Invalid source period")
                accession = text(fact.get("accn"), 20)
                if not re.fullmatch(r"[0-9]{10}-[0-9]{2}-[0-9]{6}", accession):
                    raise ValueError("Invalid accession number")
                fiscal_year = fact.get("fy")
                if fiscal_year is not None:
                    if not isinstance(fiscal_year, NumberToken) or not re.fullmatch(
                        r"[0-9]{4}", fiscal_year
                    ):
                        raise ValueError("Invalid fiscal year")
                    fiscal_year = int(fiscal_year)
                records.append(
                    {
                        "taxonomy": taxonomy,
                        "concept": tag,
                        "label": label,
                        "description": description,
                        "unit": unit,
                        "value": exact,
                        "reportedValueRaw": str(token),
                        "periodStart": start,
                        "periodEnd": end,
                        "fiscalYear": fiscal_year,
                        "fiscalPeriod": text(fact.get("fp"), 20, required=False),
                        "frame": text(fact.get("frame"), 100, required=False),
                        "accession": accession,
                        "form": text(fact.get("form"), 40),
                        "filedOn": filed,
                        "locator": f"/facts/{pointer(taxonomy)}/{pointer(tag)}/units/"
                        f"{pointer(unit)}/{index}",
                    }
                )
                if len(records) > MAX_FACTS:
                    raise ValueError("Selected response exceeds fact limit; narrow selection")
        if len(records) == before:
            raise ValueError("Selected concept contains no observations")
    if not records:
        raise ValueError("Selected response contains no observations")
    return records
