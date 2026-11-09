import os
import time
import uuid
import datetime
import logging
from typing import List, Optional

# ----------------------------------------------------------------------
# 1. ЗАГРУЗКА ПЕРЕМЕННЫХ ОКРУЖЕНИЯ ИЗ .ENV
# ----------------------------------------------------------------------
from dotenv import load_dotenv
load_dotenv()

from fastapi import FastAPI, HTTPException, Header, Depends, status, File, UploadFile, Form, Query, Request
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from pydantic import BaseModel, Field

from slowapi import Limiter
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded

from google.cloud import firestore
from google.auth.credentials import AnonymousCredentials

# Импортируем модули авторизации и ИИ (убедитесь, что они лежат в папке server)
try:
    from auth import verify_token, require_role
    from ai_service import analyze_audio_with_gemini, analyze_bowl_photo_with_gemini
except ImportError:
    # Запасной импорт при запуске из корня проекта
    from server.auth import verify_token, require_role
    from server.ai_service import analyze_audio_with_gemini, analyze_bowl_photo_with_gemini

# ----------------------------------------------------------------------
# 2. ИНИЦИАЛИЗАЦИЯ И НАСТРОЙКА ЛОГИРОВАНИЯ И RATE LIMITER
# ----------------------------------------------------------------------
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("crunchai-backend")

os.environ["FIRESTORE_EMULATOR_HOST"] = os.getenv("FIRESTORE_EMULATOR_HOST", "127.0.0.1:8080")

db = firestore.Client(
    project="crunchai-c91e2",
    credentials=AnonymousCredentials()
)

limiter = Limiter(key_func=get_remote_address)
app = FastAPI(
    title="CrunchAI REST API",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)
app.state.limiter = limiter

# ----------------------------------------------------------------------
# 3. МИДЛВАРЫ И ЕДИНЫЙ ФОРМАТ ОШИБОК
# ----------------------------------------------------------------------
@app.middleware("http")
async def log_requests(request: Request, call_next):
    start_time = time.time()
    req_id = f"req_{uuid.uuid4().hex[:6]}"
    request.state.req_id = req_id
    
    response = await call_next(request)
    duration = round((time.time() - start_time) * 1000, 2)
    
    logger.info(
        f"REQ_ID={req_id} METHOD={request.method} PATH={request.url.path} "
        f"STATUS={response.status_code} TIME={duration}ms"
    )
    return response

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    details = []
    for err in exc.errors():
        field_name = ".".join(str(x) for x in err.get("loc", []))
        details.append({
            "field": field_name,
            "issue": err.get("msg", "Неверное значение")
        })
    
    return JSONResponse(
        status_code=422,
        content={
            "error": {
                "code": "validation_error",
                "message": "Проверьте поля запроса",
                "details": details,
                "request_id": getattr(request.state, "req_id", f"req_{uuid.uuid4().hex[:6]}")
            }
        }
    )

@app.exception_handler(RateLimitExceeded)
async def custom_rate_limit_handler(request: Request, exc: RateLimitExceeded):
    req_id = getattr(request.state, "req_id", f"req_{uuid.uuid4().hex[:6]}")
    logger.warning(f"Rate Limit Exceeded for IP={request.client.host} PATH={request.url.path}")
    return JSONResponse(
        status_code=status.HTTP_429_TOO_MANY_REQUESTS,
        content={
            "error": {
                "code": "rate_limited",
                "message": "Слишком много запросов. Подождите и повторите снова.",
                "request_id": req_id
            }
        }
    )

# ----------------------------------------------------------------------
# 4. PYDANTIC СХЕМЫ
# ----------------------------------------------------------------------
class SignupRequest(BaseModel):
    email: str
    password: str
    role: str = Field(..., description="observer или station")
    device_name: str

class RefreshTokenRequest(BaseModel):
    refresh_token: Optional[str] = None

class PushTokenRequest(BaseModel):
    fcm_token: str

class PairConfirmRequest(BaseModel):
    key: str

class SoundEventDTO(BaseModel):
    id: str
    type: str
    confidence: float = Field(..., ge=0.0, le=1.0)
    started_at: str
    duration: int

class EventBatchRequest(BaseModel):
    events: List[SoundEventDTO]

class PetProfileDTO(BaseModel):
    name: str
    species: str
    age_month: int = Field(..., ge=0, le=600)
    weight_kg: float = Field(..., gt=0.1, lt=120.0)
    body_score: int = Field(..., ge=1, le=9)

# ----------------------------------------------------------------------
# 5. МАРШРУТЫ API (ENDPOINTS)
# ----------------------------------------------------------------------

# --- Авторизация и Устройства ---
@app.post("/api/v1/auth/signup", status_code=201, summary="Зарегистрировать телефон")
async def signup(data: SignupRequest):
    user_id = str(uuid.uuid4())
    device_id = str(uuid.uuid4())
    mock_token = f"jwt_mock_{data.role}_{user_id[:8]}"

    db.collection("users").document(user_id).set({
        "email": data.email,
        "password_hash": "hash_" + data.password,
        "created_at": firestore.SERVER_TIMESTAMP
    })

    db.collection("devices").document(device_id).set({
        "user_id": user_id,
        "role": data.role,
        "name": data.device_name,
        "fcm_token": ""
    })

    return {
        "user": {"id": user_id, "email": data.email},
        "device": {"id": device_id, "role": data.role, "name": data.device_name},
        "token": mock_token,
        "expires_in": 7776000
    }

@app.post("/api/v1/auth/refresh", summary="Обновить токен")
async def refresh_token(data: Optional[RefreshTokenRequest] = None, token: str = Depends(verify_token)):
    return {
        "token": f"jwt_refreshed_{uuid.uuid4().hex[:8]}",
        "expires_in": 7776000
    }

@app.put("/api/v1/devices/me/push-token", summary="Сохранить токен для push-уведомлений (FCM)")
async def update_push_token(data: PushTokenRequest, token: str = Depends(verify_token)):
    return {"status": "ok", "message": "FCM токен обновлён"}

# --- Привязка устройств (Pairing) ---
@app.post("/api/v1/pairing/sessions", summary="Создать ключ привязки (Станция)")
async def create_pairing_session(