"""ASGI application entry point."""

from importlib.metadata import version

from fastapi import FastAPI

from app.config import Settings
from app.router import router


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings if settings is not None else Settings()
    application = FastAPI(
        title=settings.app_name,
        version=version("finecore-ai-server"),
        description="FinCore financial analytics and declarative adapter onboarding.",
    )
    application.state.settings = settings
    application.include_router(router)
    return application


app = create_app()
