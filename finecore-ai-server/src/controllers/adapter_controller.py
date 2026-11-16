"""HTTP request handling for adapter onboarding and normalization."""

from decimal import InvalidOperation

from fastapi import APIRouter, HTTPException, Request

from schemas.adapters import Candidate, Normalize, SourceBundle
from services import adapter_service

router = APIRouter(tags=["adapters"])


@router.post("/adapters/prepare")
def prepare(bundle: SourceBundle):
    return adapter_service.prepare(bundle)


@router.post("/adapters/validate")
def validate_adapter(candidate: Candidate):
    try:
        return {"status": "passed", "results": adapter_service.validate(candidate)}
    except (ValueError, TypeError, InvalidOperation) as exc:
        raise HTTPException(422, str(exc)) from exc


@router.post("/adapters/register")
def register(candidate: Candidate, request: Request):
    try:
        return adapter_service.register(candidate, request.app.state.settings.adapter_registry_dir)
    except (ValueError, TypeError, InvalidOperation) as exc:
        raise HTTPException(422, str(exc)) from exc


@router.post("/adapters/{adapter_id}/normalize")
def normalize(adapter_id: str, body: Normalize, request: Request):
    try:
        return adapter_service.normalize(
            adapter_id, body.sample, request.app.state.settings.adapter_registry_dir
        )
    except adapter_service.AdapterNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc
    except adapter_service.AdapterIntegrityError as exc:
        raise HTTPException(409, str(exc)) from exc
    except (ValueError, TypeError, InvalidOperation) as exc:
        raise HTTPException(422, str(exc)) from exc
