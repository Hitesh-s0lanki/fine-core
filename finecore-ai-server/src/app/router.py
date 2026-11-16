"""Central API router registration."""

from fastapi import APIRouter

from controllers.adapter_controller import router as adapter_router
from controllers.health_controller import router as health_router

router = APIRouter()
router.include_router(health_router)
router.include_router(adapter_router)
