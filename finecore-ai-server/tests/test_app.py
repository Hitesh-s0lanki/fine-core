"""Exercise the API contract and environment configuration."""

import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError

from finecore_ai_server.config import Settings
from finecore_ai_server.main import create_app


def test_health_and_openapi():
    with TestClient(create_app(Settings(_env_file=None, environment="test"))) as client:
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json() == {"status": "UP"}
        schema = client.get("/openapi.json").json()
        assert "/health" in schema["paths"]
        assert client.get("/docs").status_code == 200


def test_settings_from_environment(monkeypatch):
    monkeypatch.setenv("FINECORE_APP_NAME", "custom-service")
    monkeypatch.setenv("FINECORE_ENVIRONMENT", "production")
    application = create_app(Settings(_env_file=None))
    assert application.title == "custom-service"
    assert application.state.settings.environment == "production"


def test_invalid_environment_fails_at_startup(monkeypatch):
    monkeypatch.setenv("FINECORE_ENVIRONMENT", "invalid")
    with pytest.raises(ValidationError):
        create_app()
