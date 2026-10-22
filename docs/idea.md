# FinCore: Project Idea and Requirements

**Investment Banking Learning + Project**

- **Document version:** 1.0
- **Date:** 22 October 2026
- **Status:** Planning; documentation only
- **Initial market:** US-listed non-financial companies
- **Source:** Adapted from the original FinCore MVP v1.0 product requirements document. This Markdown document replaces `FinCore_PRD_v1.docx`.

## 1. Idea and purpose

FinCore is a proposed web application for public-company research, corporate valuation, and deal analytics. A user selects a company, reviews financial information from official disclosures, builds a valuation, compares scenarios, and exports an auditable report.

The project combines investment banking learning with software implementation. Each module should produce both a working capability and an explanation of the underlying financial concepts, assumptions, and limitations.

**Core promise:** select a company → fetch official disclosures → review normalized financials → adjust assumptions → calculate valuation → export a reproducible report.

### Problem

Company information is scattered across filings, PDFs, market-data sources, and spreadsheets. Preparing comparable financial histories and defensible valuations requires repetitive data collection and careful interpretation. A model is difficult to review when source values, assumptions, or calculation methods are unclear.

### Objectives

1. Complete a reliable end-to-end public-company valuation workflow.
2. Learn financial statement analysis, forecasting, valuation, and transaction mechanics through implementation.
3. Demonstrate financial domain knowledge and engineering depth in a portfolio suitable for financial-services recruiting.
4. Make correctness, provenance, and reproducibility the measures of success.

## 2. Intended users

| User | Core job |
| --- | --- |
| Student or aspiring investment banking analyst | Understand a business, analyze financial statements, and prepare a defensible valuation. |
| Financial analyst | Build and compare models with visible assumptions and source data. |
| Reviewer or hiring manager | Inspect the methodology and reproduce the analysis. |
| Administrator, in a later phase | Manage accounts, data-source settings, and audit controls. |

## 3. Learning plan

The following learning deliverables extend the original product scope into a practical study plan. They are proposed work, not completed modules.

| Stage | Concepts to learn | Practical deliverable |
| --- | --- | --- |
| Company research | Business model, industry context, annual reports, and filing structure | A sourced company overview and a short business summary. |
| Financial statements | Income statement, balance sheet, cash flow, fiscal periods, and accounting relationships | Five annual periods of normalized financials where available, with reconciliation notes. |
| Forecasting | Revenue drivers, operating margins, taxes, capex, depreciation, and working capital | An explicit assumption set and a forecast schedule. |
| DCF valuation | Unlevered free cash flow, WACC, terminal value, discounting, and enterprise versus equity value | A deterministic valuation checked against a reference workbook. |
| Scenario analysis | Base, bull, and bear assumptions; sensitivity to discount rates and terminal growth | Saved scenarios and an explanation of what drives differences. |
| Research communication | Evidence, methodology, valuation limitations, and clear presentation | An Excel model and a PDF analyst report. |
| M&A, after MVP | Purchase price, synergies, funding mix, share issuance, and accretion/dilution | A transaction simulation with transparent assumptions. |
| Credit and financing, later | Leverage, liquidity, interest coverage, capital structure, and dilution | Stress tests and financing scenario comparisons. |

Use one well-covered non-financial issuer for the first complete walkthrough, then validate the workflow across at least five issuers. Keep learning notes and reference calculations alongside each implemented module when development begins.

## 4. MVP scope

| ID | Capability | Required behavior |
| --- | --- | --- |
| F01 | Company search | Search by company name or ticker, resolve the SEC CIK, and select one issuer. |
| F02 | Company overview | Show identity, sector when sourced, reporting currency, recent filings, and key metrics. |
| F03 | Financial ingestion | Fetch SEC Company Facts and submissions; collect the latest five annual periods where available; retain raw JSON and metadata. |
| F04 | Statement views | Show revenue, net income, assets, liabilities, and cash flow information with units, periods, and source links. |
| F05 | Valuation model | Calculate forecast free cash flows, WACC, terminal value, enterprise value, and equity value. |
| F06 | Scenarios | Edit growth, margins, capex, working capital, tax, WACC, and terminal growth; save and compare base, bull, and bear scenarios. |
| F07 | Exports | Generate an Excel workbook containing assumptions and calculations, and a PDF report containing source links and caveats. |
| F08 | Data quality | Flag missing or inconsistent values and prevent misleading valuations when required inputs are insufficient. |

### Later phases

- **M&A:** purchase-price scenarios, synergy assumptions, funding mix, accretion/dilution, and deal reports.
- **Credit analysis:** leverage and liquidity ratios, transparent basic borrower scoring, and revenue/interest-rate stress tests.
- **Filing research:** document ingestion, grounded Q&A with page or section citations, and document comparison.
- **IPO and financing:** primary equity issuance, dilution, capital structure, and proceeds scenarios.
- **Collaboration:** projects, comments, approval workflows, role-based access, and model revisions.

### Outside the MVP

Live trading, trade execution, brokerage accounts, proprietary Bloomberg/Refinitiv feeds, comprehensive private-company coverage, investment advice, guaranteed price targets, and full bank-specific modeling are excluded.

Investment banking describes the learning domain. The initial companies being valued are non-financial issuers; banks and other financial institutions require separate sector-specific valuation methods.

## 5. Primary user journey

1. Search for a company, such as Microsoft, and resolve its official identifier.
2. Inspect five years of financial data where available, including filing links and missing-data flags.
3. Create a valuation model using a selected filing-date snapshot.
4. Review and enter forecast and financing assumptions.
5. Run deterministic DCF calculations.
6. Compare base, upside, and downside results.
7. Export the workbook and report.
8. Reopen the saved model and reproduce its results from the same snapshot, assumptions, and method version.

## 6. Data sources and provenance

| Source | Purpose | Planned approach |
| --- | --- | --- |
| SEC EDGAR | US filings, XBRL fundamentals, reporting periods, and company identifiers | Backend integration with documented JSON APIs. |
| Company investor relations | Annual reports and presentations | Store document links; optionally ingest authorized documents. |
| FRED | US macroeconomic data and yields | Optional API integration with an API key. |
| Damodaran datasets | Industry comparisons and valuation assumptions | Import versioned downloads and record the dataset date. |
| NSE/BSE, future | Indian listed-company disclosures | Evaluate parsing, access, and usage rights before integration or redistribution. |

### Data rules

- Retain original values, units/currency, fiscal-period start and end, form type, accession number, filing date, and source URL.
- Preserve raw responses and the metadata needed to reconstruct a normalized snapshot.
- Distinguish reported facts, calculated metrics, and user forecasts in storage and presentation.
- Avoid double counting overlapping quarterly and annual observations.
- Handle amended filings and restatements explicitly; new data must not silently change saved model snapshots.
- Use a metric-mapping layer for differing XBRL concepts, with company-specific overrides when justified.
- Flag absent or unreconciled disclosures; never invent values to complete a model.
- Record sources and dates for user-supplied prices, market capitalization, peer multiples, or valuation inputs.

## 7. Financial modeling rules

Calculations must be implemented in deterministic code. An LLM may support later document research, but it must not be the financial calculation engine.

The proposed initial method is an unlevered DCF. Its calculation specification should define:

1. Forecast operating performance using explicit revenue-growth and margin assumptions.
2. Calculate free cash flow to the firm from after-tax operating profit, depreciation/amortization, capital expenditure, and changes in operating working capital.
3. Calculate WACC from explicit cost-of-equity, cost-of-debt, tax, and capital-weight assumptions; retain any user override.
4. Discount forecast cash flows and terminal value using a documented timing convention.
5. Bridge enterprise value to equity value using clearly sourced debt, cash, and any applicable adjustments.
6. Calculate a per-share result only when the share-count basis is available and explicitly identified.

The exact forecast horizon, discounting convention, terminal method, and input policies remain design decisions. For a perpetual-growth terminal method, enforce valid discount-rate/growth relationships and document the validation rules before implementation.

### Integrity requirements

- Use suitable precision for money and shares; display currencies and units.
- Record snapshot ID, scenario assumptions, method version, and computation timestamp for each analysis.
- Distinguish enterprise value from equity value and disclose terminal and financing assumptions.
- Keep scenarios independently versioned and reproducible.
- Block calculation when essential inputs are insufficient or invalid, and explain the corrective action.
- Validate results against reference workbooks and boundary cases before releasing the valuation MVP.

## 8. Proposed architecture

```text
Next.js web application
         |
Spring Boot core API
         |
Python financial service
         |
PostgreSQL / object storage
```

This is the original proposed service flow, not a finalized deployment topology. Define data ownership and service contracts during implementation. Keep service boundaries limited to justified responsibilities.

| Layer | Proposed stack | Responsibility |
| --- | --- | --- |
| Web | Next.js, TypeScript, Tailwind CSS, shadcn/ui | Search, financial tables, assumption entry, scenario comparison, export UI. |
| Core API | Java 21, Spring Boot, Spring Security | Company/project APIs, access control if enabled, and versioned models. |
| Financial service | Python, FastAPI, Pandas, NumPy, SciPy | Ingestion, normalization, and deterministic financial calculations. |
| Data | PostgreSQL and S3-compatible object storage | Structured facts, snapshots, models, raw responses, and documents. |
| Async/cache, when needed | Redis and workers | Background ingestion and report-generation jobs. |
| AI, later | LangGraph and pgvector | Filing retrieval and cited research answers. |
| Infrastructure | Docker, GitHub Actions, potential AWS deployment | Local Compose environment, automated checks, deployment, and logging. |

## 9. Conceptual data model

These entities describe domain needs rather than a finalized database schema.

| Entity | Main information |
| --- | --- |
| Company | ID, ticker, CIK, name, jurisdiction. |
| Filing | ID, company ID, form, filing date, accession number, source URL. |
| FinancialFact | Company, concept, original value, unit/currency, reporting period, and filing reference. |
| FinancialSnapshot | Company, as-of date, facts version, and a reproducible reference to the included facts. |
| ValuationModel | Company, snapshot, model version, scenario assumptions, and method version. |
| ValuationResult | Model/scenario reference, computation timestamp, enterprise value, equity value, and sensitivities. |
| Export | Model/version reference, storage location, and output format. |

Scenario storage and snapshot membership need explicit schema designs before implementation so later ingestion cannot change historical results.

## 10. MVP acceptance criteria

| Capability | Definition of done |
| --- | --- |
| Company retrieval | Search and load at least five well-covered US non-financial issuers. |
| Traceability | Every displayed statement number links to its originating filing/fact; calculated metrics expose their inputs and method. |
| Historical coverage | Display five annual reporting periods when available and visibly identify gaps. |
| Model integrity | Pass reference-workbook comparisons and boundary-case tests. |
| Scenarios | Recalculate and save each scenario independently when assumptions change. |
| Reproducibility | Reopening a saved model reproduces results using the same snapshot, assumptions, and method version. |
| Exports | Workbook includes assumptions/formulas; report includes sources, methodology, and educational-use limitations. |
| Reliability | Provide clear, recoverable states for network errors, source throttling, and missing tags. |

## 11. Non-functional requirements

- **Security:** keep secrets server-side, validate inputs, and enforce role-based controls if accounts are enabled. Confidential customer data is outside the project scope.
- **Performance:** target cached company overviews below two seconds under documented development test conditions; use background ingestion for cold requests.
- **Observability:** record structured logs, ingestion status, errors, and financial calculation traces.
- **Source access:** honor source access limits and applicable licensing restrictions; retain source attribution.
- **Accessibility and UX:** provide a responsive interface, readable tables, and explicit fiscal-year, unit, and currency labels.
- **Educational positioning:** label analytical outputs as educational and non-advisory.

## 12. Indicative delivery plan

The original PRD proposes the following sequence. These are effort windows from development kickoff, not committed calendar dates.

| Window | Focus | Deliverable |
| --- | --- | --- |
| Weeks 1–2 | Data foundation | Company search, SEC ingestion, normalization, and source records. |
| Weeks 3–4 | Valuation MVP | Statements, DCF, scenarios, Excel/PDF exports, and financial validation. |
| Weeks 5–6 | Deal analytics | M&A and financing scenarios, after the MVP passes acceptance criteria. |
| Weeks 7–8 | Extensions | Credit ratios, cited filing Q&A, and demonstration polish, subject to core-model readiness. |

Release the core valuation workflow when calculations and provenance are validated. Later modules depend on that foundation and may require additional time.

## 13. Risks and open decisions

| Issue | Decision or mitigation needed |
| --- | --- |
| Issuer-specific XBRL tags | Define canonical metrics, mapping rules, override review, and reconciliation checks. |
| Restatements and amendments | Define filing precedence and immutable snapshot behavior. |
| Market data availability | Select permitted sources or require explicit user inputs with provenance. |
| Valuation methodology | Finalize forecast horizon, WACC inputs, timing convention, terminal method, equity bridge, and share-count basis. |
| Authentication | Choose local/single-user operation or authenticated multiuser projects; the original PRD favors a simple single-user start. |
| Service complexity | Confirm the proposed Java/Python boundary and data ownership before scaffolding services. |
| Demo coverage | Select the first non-financial issuer, then define the five-company validation set. |
| Indian-market expansion | Evaluate distinct data formats, access conditions, and redistribution rights. |

## 14. Source references

The following links are carried forward from the original PRD as implementation references. Their current API details and access requirements should be checked when integrating each source.

- [SEC EDGAR APIs](https://www.sec.gov/search-filings/edgar-application-programming-interfaces)
- [Example SEC Company Facts: Microsoft, CIK 0000789019](https://data.sec.gov/api/xbrl/companyfacts/CIK0000789019.json)
- [FRED API documentation](https://fred.stlouisfed.org/docs/api/fred/)
- [Damodaran datasets](https://pages.stern.nyu.edu/~adamodar/New_Home_Page/data.html)
- [NSE](https://www.nseindia.com/)
- [BSE](https://www.bseindia.com/)
