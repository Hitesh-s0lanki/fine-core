# finecore-ai-server

Python 3.12 and FastAPI starter for FinCore's financial analytics and future
filing research service. This currently provides application configuration,
process health, and generated API documentation. Company ingestion, financial
calculations, storage, and model-provider integrations are not implemented yet.

## Run

Install Python 3.12 and [uv](https://docs.astral.sh/uv/getting-started/installation/),
then run from this directory:

```sh
uv sync --frozen
cp .env.example .env
uv run uvicorn finecore_ai_server.main:app --reload --host 127.0.0.1 --port 8000
```

- Health: http://127.0.0.1:8000/health returns `{"status":"UP"}`.
- Interactive API documentation: http://127.0.0.1:8000/docs.
- OpenAPI schema: http://127.0.0.1:8000/openapi.json.

Port 8000 allows this service to run alongside the Java API on port 8080.
For a non-reloading process, omit `--reload`. Set `--host 0.0.0.0` when the
process must accept connections from outside localhost.

## Configuration

Settings use the `FINECORE_` prefix and load from environment variables or a
local `.env` file. Environment variables take precedence. `.env` is ignored by Git.

| Variable | Default | Accepted values |
| --- | --- | --- |
| `FINECORE_APP_NAME` | `finecore-ai-server` | Nonempty string |
| `FINECORE_ENVIRONMENT` | `development` | `development`, `test`, `production` |

Configuration is validated when the application starts. The environment label
does not enable authentication or change the deployment topology.

## Development checks

```sh
uv run ruff check .
uv run ruff format --check .
uv run pytest
uv build
```

Use `uv run ruff format .` to format sources. Commit `uv.lock` with dependency
changes so local environments and CI use the same resolved versions.

The repository's Git hooks run Ruff on staged snapshots and add tests on push.
They require `uv` when this module is present. CI also runs checks and builds
the Python distribution.

The `src/finecore_ai_server` package separates application setup, configuration,
and API routes. The structure follows FastAPI's guidance for
[multiple-file applications](https://fastapi.tiangolo.com/tutorial/bigger-applications/)
and [environment settings](https://fastapi.tiangolo.com/advanced/settings/).
Add domain features as their contracts are defined; financial calculations
must remain deterministic, with source tracking for reported values.
