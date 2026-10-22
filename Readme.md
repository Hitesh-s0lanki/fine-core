# FinCore

**Investment Banking Learning + Project**

FinCore is a planned research, valuation, and deal analytics platform for learning investment banking through a practical software project. The aim is to turn public-company disclosures into traceable financial statements, reproducible valuation models, scenario comparisons, and exportable analyst reports.

This repository contains project documentation and a Spring Boot API starter in `fincore-java-server`. The financial workflows, integrations, calculations, and exports are not implemented yet.

## Project idea

The core workflow is:

**Search a company → review sourced financials → set assumptions → run a valuation → compare scenarios → export a report.**

Start with US-listed non-financial companies and a discounted cash flow (DCF) valuation workflow. Later phases introduce M&A simulation, credit analysis, financing scenarios, and source-grounded filing research.

The project serves two goals:

- Learn financial statement analysis, corporate valuation, capital structure, and deal mechanics.
- Build a portfolio project that demonstrates financial reasoning, data engineering, backend development, and an auditable user experience.

## Planned MVP

- Search by company name or ticker and resolve the SEC company identifier (CIK).
- Retrieve SEC submissions and Company Facts, targeting five annual reporting periods where available.
- Display financial statements with fiscal periods, units, filing links, and data-quality flags.
- Calculate forecast free cash flow, WACC, terminal value, enterprise value, and equity value.
- Save and compare base, bull, and bear scenarios.
- Export an Excel workbook with assumptions and formulas, plus a PDF report with sources and limitations.
- Reproduce saved results from versioned inputs and a fixed financial-data snapshot.

## Learning through implementation

| Learning area | Practical project work |
| --- | --- |
| Financial statements | Read filings; normalize income statement, balance sheet, and cash flow data. |
| Financial modeling | Build forecasts from explicit growth, margin, reinvestment, and tax assumptions. |
| Valuation | Implement and validate DCF calculations, discount rates, and the enterprise-to-equity bridge. |
| Analyst judgment | Explain scenario assumptions, sensitivities, data gaps, and model limitations. |
| M&A and financing | Extend the platform with purchase price, funding mix, synergies, dilution, and accretion analysis. |
| Credit analysis | Add leverage, liquidity, debt-service analysis, and stress scenarios. |
| Research communication | Produce a report that a reviewer can trace back to filings and reproduce. |

## Proposed technology stack

| Layer | Proposed tools |
| --- | --- |
| Web application | Next.js, TypeScript, Tailwind CSS, shadcn/ui |
| Core API | Java 21, Spring Boot, Spring Security |
| Financial service | Python, FastAPI, Pandas, NumPy, SciPy |
| Storage | PostgreSQL and object storage compatible with S3 |
| Development and delivery | Docker Compose, GitHub Actions; AWS as a potential deployment target |
| Later additions | Redis/workers when needed; LangGraph and pgvector for filing research |

The Java API starter uses Java 21, Maven, Spring Web, and Spring Boot Actuator. The remaining tools are design proposals.

## Run the Java server

Install JDK 21 and set `JAVA_HOME` to its installation directory, then run:

```sh
cd fincore-java-server
./mvnw spring-boot:run
```

The server listens on `http://localhost:8080`. Check its health at `http://localhost:8080/actuator/health`.

Run the tests and build an executable JAR with `./mvnw verify`. See the [server README](fincore-java-server/README.md) for more details.

## Local Git checks

Enable the shared Git hooks once after cloning, from the repository root:

```sh
sh scripts/install-git-hooks.sh
```

With JDK 21 and `JAVA_HOME` configured, commits check staged whitespace across the repository and run Java formatting and Checkstyle checks. Pushes run a clean Java build with all tests for each distinct commit tip being pushed. Newly added Java sources and tests are included automatically. Hooks check isolated snapshots without changing your files or staging area.

Hook activation is local to each clone. GitHub Actions also runs the Java checks after pushing. When another application or language is added, extend `scripts/run-git-checks.sh` and CI with its checks.

Run the hook regression tests with `python3 -m unittest discover -s tests -v`. They use temporary repositories to cover partial staging, check failures, whitespace, multiple pushed refs, and deletions.

## Roadmap

1. **Data foundation:** company search, SEC ingestion, financial normalization, and source tracking.
2. **Valuation MVP:** statement views, deterministic DCF calculations, saved scenarios, and Excel/PDF exports.
3. **Deal analytics:** M&A and financing models after the valuation workflow is validated.
4. **Research extensions:** credit analysis, cited filing Q&A, and demo polish.

## Documentation

- [Project idea and requirements](docs/idea.md): scope, learning plan, architecture, data rules, acceptance criteria, and open decisions.

```text
fine-core/
├── Readme.md
├── docs/
│   └── idea.md
└── fincore-java-server/
    ├── pom.xml
    ├── mvnw
    └── src/
```

## Modeling principles

Financial calculations must run in deterministic code and be checked against reference calculations. Every reported value needs provenance; every forecast needs explicit assumptions. Missing disclosures must remain visible, and insufficient inputs must prevent misleading valuations.

FinCore is an educational and portfolio project. Its planned outputs are analytical exercises and do not constitute investment advice. Live trading and brokerage integrations are outside the MVP scope.
