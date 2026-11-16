"""Declarative adapter contracts and deterministic financial normalization."""

from datetime import date
from decimal import Decimal
from typing import Annotated, Any, Literal

from pydantic import BaseModel, ConfigDict, Field, TypeAdapter, field_validator, model_validator


class Model(BaseModel):
    model_config = ConfigDict(extra="forbid")


class Selector(Model):
    key: str
    equals: str


PathToken = str | int | Selector


class Mapping(Model):
    path: list[PathToken] | None = None
    constant: str | int | None = None
    transform: Literal["identity", "decimal", "iso_date"] = "identity"

    @model_validator(mode="after")
    def one_input(self):
        if (self.path is None) == (self.constant is None):
            raise ValueError("Specify exactly one of path or constant")
        return self


class CompanyProfile(Model):
    record_type: Literal["company_profile"]
    name: str = Field(min_length=1)
    registration_number: str = Field(min_length=1)
    status: str | None = None
    jurisdiction: str | None = None


class FinancialFact(Model):
    record_type: Literal["financial_fact"]
    metric: Literal["REVENUE", "NET_INCOME", "ASSETS", "LIABILITIES", "OPERATING_CASH_FLOW"]
    value: Decimal = Field(allow_inf_nan=False)
    unit: str = Field(min_length=1)
    period_start: date | None = None
    period_end: date
    scope: Literal["consolidated", "standalone", "unspecified"]
    original_concept: str = Field(min_length=1)
    filing_reference: str = Field(min_length=1)

    @field_validator("value", mode="before")
    @classmethod
    def exact_value(cls, value):
        if isinstance(value, bool) or not isinstance(value, (str, int, Decimal)):
            raise ValueError("Financial values require decimal strings or integers")
        return value

    @model_validator(mode="after")
    def periods(self):
        if self.period_start and self.period_start > self.period_end:
            raise ValueError("Period start must precede period end")
        return self


class Announcement(Model):
    record_type: Literal["announcement"]
    title: str = Field(min_length=1)
    published_at_raw: str = Field(min_length=1)
    attachment_url: str | None = None


Record = Annotated[
    CompanyProfile | FinancialFact | Announcement, Field(discriminator="record_type")
]
RECORD = TypeAdapter(Record)


class Adapter(Model):
    source_id: str = Field(pattern=r"^[a-z][a-z0-9_-]{0,63}$")
    schema_version: Literal[1] = 1
    records_path: list[PathToken] = Field(default_factory=list)
    # A single object, or a list of objects, may be selected by records_path.
    fields: dict[str, Mapping] = Field(min_length=1, max_length=30)
    identity_path: list[PathToken] = Field(min_length=1)
    semantic_notes: list[str] = Field(min_length=1)


class Sample(Model):
    company_id: str = Field(min_length=1)
    expected_source_identifier: str = Field(min_length=1)
    raw_reference: str = Field(min_length=1)
    payload: dict[str, Any] | list[Any]
    expected_records: list[Record] | None = None


class SourceBundle(Model):
    source_id: str = Field(pattern=r"^[a-z][a-z0-9_-]{0,63}$")
    documentation: str = Field(min_length=1, max_length=40000)
    documentation_urls: list[str] = Field(default_factory=list)
    source_field_meanings: dict[str, str] = Field(default_factory=dict)
    samples: list[Sample] = Field(min_length=2, max_length=20)


class Candidate(Model):
    adapter: Adapter
    samples: list[Sample] = Field(min_length=2, max_length=20)


class Normalize(Model):
    sample: Sample
