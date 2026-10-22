"""ASGI application entry point."""

from importlib.metadata import version

from fastapi import FastAPI

from finecore_ai_server.api.health import router as health_router
from finecore_ai_server.config import Settings


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings if settings is not None else Settings()
    application = FastAPI(
        title=settings.app_name,
        version=version("finecore-ai-server"),
        description="FinCore financial analytics and filing research service starter.",
    )
    application.state.settings = settings
    application.include_router(health_router)
    return application


app = create_app()
