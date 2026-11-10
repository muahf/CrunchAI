"""
Сервер CrunchAI: FastAPI + Cloud Firestore (эмулятор).  Практика 9.

Как читать файл (сверху вниз):
  1. Настройки и подключение к базе
  2. Ошибки: единый формат {"error": {...}}
  3. Схемы: какие поля принимает сервер
  4. Пароли и токены
  5. Проверка токена и роли (авторизация)
  6. Вспомогательные функции для базы
  7. Запросы (адреса API), по группам
"""
import base64
import hashlib
import hmac
import json
import logging
import os
import secrets
import time
import uuid
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Literal, Optional

import jwt
from fastapi import Depends, FastAPI, File, Form, Query, Request, UploadFile
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse, Response
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from google.auth.credentials import AnonymousCredentials
from google.cloud import firestore
from google.cloud.firestore_v1.base_query import FieldFilter
from pydantic import BaseModel, Field
from starlette.exceptions import HTTPException as StarletteHTTPException

try:
    from dotenv import load_dotenv
    load_dotenv()          # читает ключи из файла .env (его нет в git)
except ImportError:
    pass

import gemini_service

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("crunchai")

# ----------------------------------------------------------------------
# 1. НАСТРОЙКИ И ПОДКЛЮЧЕНИЕ К БАЗЕ
# ----------------------------------------------------------------------
PROJECT_ID = os.getenv("FIREBASE_PROJECT", "crunchai-c91e2")
JWT_SECRET = os.getenv("JWT_SECRET", "dev-secret-change-me-dev-secret-change-me")  # в .env задайте свой
TOKEN_DAYS = 90        # срок жизни токена
KEY_MINUTES = 10       # срок жизни ключа привязки
MAX_BATCH = 100        # звуков в одной пачке
MAX_PHOTO = 8 * 1024 * 1024   # 8 МБ
MAX_AUDIO = 10 * 1024 * 1024  # 10 МБ
RATE_LIMIT = int(os.getenv("RATE_LIMIT", "120"))   # запросов в минуту с одного адреса
MEAL_GAP_S = 300       # пауза до 5 минут: ещё тот же приём пищи
DATA_COLLECTIONS = ["pets", "sounds", "meals", "anomalies", "food_analyses"]

if os.getenv("USE_EMULATOR", "1") == "1":
    os.environ.setdefault("FIRESTORE_EMULATOR_HOST", "127.0.0.1:8080")
    db = firestore.Client(project=PROJECT_ID, credentials=AnonymousCredentials())
else:
    db = firestore.Client(project=PROJECT_ID)

app = FastAPI(title="CrunchAI REST API", version="1.0.0")


hits: dict = {}   # адрес -> времена последних запросов (счётчик для ограничения частоты)


@app.middleware("http")
async def request_guard(request: Request, call_next):
    """Для каждого запроса: номер, ограничение частоты (429) и запись в лог."""
    request.state.request_id = "req_" + uuid.uuid4().hex[:8]
    started = time.monotonic()
    ip = request.client.host if request.client else "unknown"
    now = time.monotonic()
    recent = [t for t in hits.get(ip, []) if now - t < 60]
    if len(recent) >= RATE_LIMIT:
        hits[ip] = recent
        response = error_response(request, 429, "rate_limited", "Слишком много запросов, подождите минуту")
        response.headers["Retry-After"] = "60"
    else:
        recent.append(now)
        hits[ip] = recent
        response = await call_next(request)
    response.headers["X-Request-ID"] = request.state.request_id
    log.info("%s %s -> %s (%d мс) %s", request.method, request.url.path, response.status_code,
             (time.monotonic() - started) * 1000, request.state.request_id)
    return response


# ----------------------------------------------------------------------
# 2. ЕДИНЫЙ ФОРМАТ ОШИБОК
# ----------------------------------------------------------------------
class ApiError(Exception):
    def __init__(self, status: int, code: str, message: str, details=None):
        self.status, self.code, self.message, self.details = status, code, message, details


def error_response(request: Request, status: int, code: str, message: str, details=None):
    error = {"code": code, "message": message}
    if details:
        error["details"] = details
    error["request_id"] = getattr(request.state, "request_id", "req_unknown")
    return JSONResponse(status_code=status, content={"error": error})


@app.exception_handler(ApiError)
async def api_error_handler(request: Request, exc: ApiError):
    return error_response(request, exc.status, exc.code, exc.message, exc.details)


@app.exception_handler(RequestValidationError)
async def validation_handler(request: Request, exc: RequestValidationError):
    details = [
        {"field": ".".join(str(x) for x in e["loc"][1:]) or str(e["loc"][0]), "issue": e["msg"]}
        for e in exc.errors()
    ]
    return error_response(request, 422, "validation_error", "Проверьте поля запроса", details)


@app.exception_handler(StarletteHTTPException)
async def http_handler(request: Request, exc: StarletteHTTPException):
    code = "not_found" if exc.status_code == 404 else "http_error"
    return error_response(request, exc.status_code, code, str(exc.detail))


@app.exception_handler(Exception)
async def unknown_handler(request: Request, exc: Exception):
    log.exception("необработанная ошибка")
    return error_response(request, 500, "internal_error", "Сбой на сервере")


# ----------------------------------------------------------------------
# 3. СХЕМЫ: КАКИЕ ПОЛЯ ПРИНИМАЕТ СЕРВЕР (числа совпадают с openapi.yaml)
# ----------------------------------------------------------------------
Role = Literal["station", "observer"]
EMAIL = r"^[^@\s]+@[^@\s]+\.[^@\s]+$"


class AuthRequest(BaseModel):
    email: str = Field(pattern=EMAIL, max_length=254)
    password: str = Field(min_length=8, max_length=72)
    role: Role
    device_name: str = Field(min_length=1, max_length=60)


class PushTokenRequest(BaseModel):
    fcm_token: str = Field(min_length=1, max_length=4096)


class PairConfirmRequest(BaseModel):
    key: str = Field(min_length=3, max_length=20)


class SoundEventDTO(BaseModel):
    id: uuid.UUID
    type: Literal["cough", "chew", "lap", "bark", "other"]
    confidence: float = Field(ge=0.0, le=1.0)
    started_at: datetime
    duration: int = Field(ge=0, le=600)


class EventBatchRequest(BaseModel):
    # длину (не больше 100) проверяет сам запрос: нужна ошибка 413, а не 422
    events: list[SoundEventDTO] = Field(min_length=1)


class PetProfileDTO(BaseModel):
    name: str = Field(min_length=1, max_length=40)
    species: Literal["cat", "dog", "other"]
    age_month: Optional[int] = Field(default=None, ge=0, le=480)
    weight_kg: Optional[float] = Field(default=None, ge=0.1, le=120)
    body_score: Optional[int] = Field(default=None, ge=1, le=9)


# ----------------------------------------------------------------------
# 4. ПАРОЛИ И ТОКЕНЫ
# ----------------------------------------------------------------------
def hash_password(password: str) -> str:
    """Пароль хранится только в виде хеша (scrypt + случайная соль)."""
    salt = os.urandom(16)
    digest = hashlib.scrypt(password.encode(), salt=salt, n=2**14, r=8, p=1, dklen=32)
    return f"scrypt${salt.hex()}${digest.hex()}"


def verify_password(password: str, stored: str) -> bool:
    try:
        _, salt_hex, digest_hex = stored.split("$")
        digest = hashlib.scrypt(password.encode(), salt=bytes.fromhex(salt_hex), n=2**14, r=8, p=1, dklen=32)
        return hmac.compare_digest(digest.hex(), digest_hex)
    except ValueError:
        return False


def make_token(device_id: str, user_id: str, role: str) -> str:
    now = datetime.now(timezone.utc)
    payload = {"sub": device_id, "uid": user_id, "role": role, "iat": now, "exp": now + timedelta(days=TOKEN_DAYS)}
    return jwt.encode(payload, JWT_SECRET, algorithm="HS256")


def decode_token(token: str) -> dict:
    try:
        return jwt.decode(token, JWT_SECRET, algorithms=["HS256"])
    except jwt.PyJWTError:
        raise ApiError(401, "token_invalid", "Токен недействителен или истёк")


def iso(dt: datetime) -> str:
    return dt.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


# ----------------------------------------------------------------------
# 5. АВТОРИЗАЦИЯ: КТО ОБРАТИЛСЯ И МОЖНО ЛИ ЕМУ ЭТО ДЕЛАТЬ
# ----------------------------------------------------------------------
bearer = HTTPBearer(auto_error=False)


@dataclass
class Auth:
    device_id: str
    user_id: str
    role: str


def current_auth(creds: Optional[HTTPAuthorizationCredentials] = Depends(bearer)) -> Auth:
    """Проверяет токен и находит телефон. Без токена: ошибка 401."""
    if creds is None:
        raise ApiError(401, "token_invalid", "Нужен токен: заголовок Authorization: Bearer")
    data = decode_token(creds.credentials)
    device = db.collection("devices").document(str(data.get("sub", ""))).get()
    if not device.exists:
        raise ApiError(401, "token_invalid", "Телефон не найден, войдите заново")
    d = device.to_dict()
    return Auth(device_id=device.id, user_id=d["user_id"], role=d["role"])


def require_role(role: str):
    """Запрос только для Станции или только для Наблюдателя. Иначе ошибка 403."""
    def check(auth: Auth = Depends(current_auth)) -> Auth:
        if auth.role != role:
            who = "Станции" if role == "station" else "Наблюдателю"
            raise ApiError(403, "wrong_role", f"Операция доступна только {who}")
        return auth
    return check


# ----------------------------------------------------------------------
# 6. ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ДЛЯ БАЗЫ
# ----------------------------------------------------------------------
def where(collection: str, field: str, op: str, value):
    return db.collection(collection).where(filter=FieldFilter(field, op, value))


def current_connection(auth: Auth):
    """Подключение этого телефона. Все данные дальше берутся только по его номеру:
    так телефон видит только своё (правило доступа к строкам на стороне сервера)."""
    field = "station_id" if auth.role == "station" else "observer_id"
    paired = [c for c in where("connections", field, "==", auth.device_id).stream() if c.to_dict().get("observer_id")]
    if not paired:
        raise ApiError(404, "not_found", "Телефоны ещё не связаны: выполните привязку по QR-коду")
    return max(paired, key=lambda c: c.to_dict()["time_key"])


def find_pet(connection_id: str):
    return next(iter(where("pets", "connection_id", "==", connection_id).limit(1).stream()), None)


def delete_connection(connection_id: str) -> None:
    """Удаляет подключение и все его данные: питомца, звуки, приёмы пищи, аномалии, анализы."""
    for name in DATA_COLLECTIONS:
        docs = list(where(name, "connection_id", "==", connection_id).stream())
        for i in range(0, len(docs), 400):
            batch = db.batch()
            for d in docs[i:i + 400]:
                batch.delete(d.reference)
            batch.commit()
    db.collection("connections").document(connection_id).delete()


def new_pairing_key() -> str:
    alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"   # без похожих символов 0/O и 1/I
    chars = "".join(secrets.choice(alphabet) for _ in range(6))
    return f"{chars[:3]}-{chars[3:]}"


def encode_cursor(doc_id: str) -> str:
    return base64.urlsafe_b64encode(json.dumps({"i": doc_id}).encode()).decode().rstrip("=")


def decode_cursor(cursor: str) -> Optional[str]:
    try:
        return json.loads(base64.urlsafe_b64decode(cursor + "=" * (-len(cursor) % 4)))["i"]
    except (ValueError, KeyError, TypeError):
        return None


def list_page(collection: str, conn_id: str, limit: int, cursor: Optional[str], to_dict) -> dict:
    """Страница списка: сначала новые записи; курсор запоминает, на какой записи остановились."""
    q = where(collection, "connection_id", "==", conn_id).order_by("started_at", direction=firestore.Query.DESCENDING)
    if cursor:
        last_id = decode_cursor(cursor)
        last = db.collection(collection).document(last_id).get() if last_id else None
        if not last or not last.exists or last.to_dict().get("connection_id") != conn_id:
            raise ApiError(422, "validation_error", "Проверьте поля запроса",
                           [{"field": "cursor", "issue": "курсор повреждён"}])
        q = q.start_after(last)
    docs = list(q.limit(limit + 1).stream())
    has_more = len(docs) > limit
    docs = docs[:limit]
    return {
        "items": [to_dict(d) for d in docs],
        "next_cursor": encode_cursor(docs[-1].id) if has_more else None,
        "has_more": has_more,
    }


def meal_intervals(sounds, gap_seconds: int = MEAL_GAP_S):
    """Склеивает звуки еды в приёмы пищи. sounds: [(начало, длительность в секундах)]."""
    items = sorted((s, s + timedelta(seconds=d)) for s, d in sounds)
    result = []
    for start, end in items:
        if result and (start - result[-1][1]).total_seconds() <= gap_seconds:
            result[-1] = (result[-1][0], max(result[-1][1], end))
        else:
            result.append((start, end))
    return result


def calm_index(cough_count: int) -> int:
    """Индекс спокойствия от 1 до 10: чем больше кашля во время еды, тем ниже."""
    return max(1, 10 - cough_count)


# ----------------------------------------------------------------------
# 7. ЗАПРОСЫ
# ----------------------------------------------------------------------
@app.get("/health", tags=["service"])
def health():
    return {"status": "ok"}


# --- Аккаунт и вход ---------------------------------------------------
def auth_response(user_id: str, email: str, device_id: str, role: str, name: str) -> dict:
    return {
        "user": {"id": user_id, "email": email},
        "device": {"id": device_id, "role": role, "name": name},
        "token": make_token(device_id, user_id, role),
        "expires_in": TOKEN_DAYS * 24 * 3600,
    }


@app.post("/api/v1/auth/signup", status_code=201, tags=["auth"], summary="Создать аккаунт и войти")
def signup(data: AuthRequest):
    email = data.email.strip().lower()
    if next(iter(where("users", "email", "==", email).limit(1).stream()), None):
        raise ApiError(409, "email_taken", "Аккаунт с таким email уже есть")
    user_id, device_id = str(uuid.uuid4()), str(uuid.uuid4())
    db.collection("users").document(user_id).set(
        {"email": email, "password_hash": hash_password(data.password), "created_at": firestore.SERVER_TIMESTAMP})
    db.collection("devices").document(device_id).set(
        {"user_id": user_id, "role": data.role, "name": data.device_name, "fcm_token": None})
    return auth_response(user_id, email, device_id, data.role, data.device_name)


@app.post("/api/v1/auth/login", tags=["auth"], summary="Войти в аккаунт")
def login(data: AuthRequest):
    email = data.email.strip().lower()
    found = next(iter(where("users", "email", "==", email).limit(1).stream()), None)
    if not found or not verify_password(data.password, found.to_dict()["password_hash"]):
        raise ApiError(401, "invalid_credentials", "Неверный email или пароль")
    same = [d for d in where("devices", "user_id", "==", found.id).stream()
            if d.to_dict()["role"] == data.role and d.to_dict()["name"] == data.device_name]
    if same:
        device_id = same[0].id
    else:   # новый телефон добавляется в аккаунт
        device_id = str(uuid.uuid4())
        db.collection("devices").document(device_id).set(
            {"user_id": found.id, "role": data.role, "name": data.device_name, "fcm_token": None})
    return auth_response(found.id, email, device_id, data.role, data.device_name)


@app.put("/api/v1/devices/me/push-token", status_code=204, tags=["auth"],
         summary="Сохранить токен для push-уведомлений (FCM)")
def update_push_token(data: PushTokenRequest, auth: Auth = Depends(current_auth)):
    db.collection("devices").document(auth.device_id).update({"fcm_token": data.fcm_token})
    return Response(status_code=204)


@app.delete("/api/v1/account", status_code=204, tags=["auth"], summary="Удалить аккаунт со всеми данными")
def delete_account(auth: Auth = Depends(current_auth)):
    devices = [d.id for d in where("devices", "user_id", "==", auth.user_id).stream()]
    for device_id in devices:
        for field in ("station_id", "observer_id"):
            for c in where("connections", field, "==", device_id).stream():
                delete_connection(c.id)
        db.collection("devices").document(device_id).delete()
    db.collection("users").document(auth.user_id).delete()
    return Response(status_code=204)


# --- Привязка телефонов -----------------------------------------------
@app.post("/api/v1/pairing/sessions", status_code=201, tags=["pairing"], summary="Создать ключ привязки (Станция)")
def create_pairing_session(auth: Auth = Depends(require_role("station"))):
    key = new_pairing_key()
    while next(iter(where("connections", "key", "==", key).limit(1).stream()), None):
        key = new_pairing_key()
    expires = datetime.now(timezone.utc) + timedelta(minutes=KEY_MINUTES)
    db.collection("connections").document(str(uuid.uuid4())).set({
        "station_id": auth.device_id, "observer_id": None, "key": key,
        "time_key": expires, "created_at": firestore.SERVER_TIMESTAMP})
    return {"key": key, "expires_at": iso(expires)}


@app.post("/api/v1/pairing/confirm", tags=["pairing"], summary="Подтвердить привязку по ключу (Наблюдатель)")
def confirm_pairing(data: PairConfirmRequest, auth: Auth = Depends(require_role("observer"))):
    found = next(iter(where("connections", "key", "==", data.key.strip().upper()).limit(1).stream()), None)
    if not found:
        raise ApiError(404, "not_found", "Ключ не найден")
    c = found.to_dict()
    if c.get("observer_id"):
        raise ApiError(409, "pairing_key_used", "Ключ уже использован")
    if c["time_key"] < datetime.now(timezone.utc):
        raise ApiError(410, "pairing_key_expired", "Ключ истёк. Создайте новый на Станции")
    found.reference.update({"observer_id": auth.device_id})
    station = db.collection("devices").document(c["station_id"]).get()
    return {"connection_id": found.id, "station_name": station.to_dict()["name"] if station.exists else "Станция"}


@app.delete("/api/v1/pairing", status_code=204, tags=["pairing"],
            summary="Отвязать телефоны и удалить данные пары")
def delete_pairing(auth: Auth = Depends(current_auth)):
    delete_connection(current_connection(auth).id)
    return Response(status_code=204)


# --- Профиль питомца --------------------------------------------------
def pet_out(pet_id: str, p: dict) -> dict:
    return {"id": pet_id, "name": p["name"], "species": p["species"],
            "age_month": p.get("age_month"), "weight_kg": p.get("weight_kg"), "body_score": p.get("body_score")}


@app.get("/api/v1/pet", tags=["pet"], summary="Профиль питомца")
def get_pet(auth: Auth = Depends(current_auth)):
    pet = find_pet(current_connection(auth).id)
    if not pet:
        raise ApiError(404, "not_found", "Профиль питомца ещё не создан")
    return pet_out(pet.id, pet.to_dict())


@app.put("/api/v1/pet", tags=["pet"], summary="Создать или изменить профиль питомца (Наблюдатель)")
def save_pet(data: PetProfileDTO, auth: Auth = Depends(require_role("observer"))):
    conn = current_connection(auth)
    pet = find_pet(conn.id)
    pet_id = pet.id if pet else str(uuid.uuid4())
    body = {"connection_id": conn.id, **data.model_dump()}
    db.collection("pets").document(pet_id).set(body)
    return pet_out(pet_id, body)


@app.delete("/api/v1/pet", status_code=204, tags=["pet"], summary="Удалить профиль питомца (Наблюдатель)")
def delete_pet(auth: Auth = Depends(require_role("observer"))):
    pet = find_pet(current_connection(auth).id)
    if not pet:
        raise ApiError(404, "not_found", "Профиль питомца ещё не создан")
    pet.reference.delete()
    return Response(status_code=204)


# --- Звуки, приёмы пищи, аномалии -------------------------------------
def save_meals(conn_id: str, batch, food_sounds, cough_times):
    """Звуки жевания: продлевают уже записанный приём пищи или создают новый."""
    if not food_sounds:
        return
    intervals = meal_intervals([(s.started_at, s.duration) for s in food_sounds])
    gap = timedelta(seconds=MEAL_GAP_S)
    lo = min(i[0] for i in intervals) - timedelta(days=1)
    hi = max(i[1] for i in intervals) + gap
    q = where("meals", "connection_id", "==", conn_id).where(filter=FieldFilter("started_at", ">=", lo))
    meals = [{"ref": d.reference, "start": d.to_dict()["started_at"],
              "end": d.to_dict()["started_at"] + timedelta(seconds=d.to_dict()["duration"])}
             for d in q.where(filter=FieldFilter("started_at", "<=", hi)).stream()]
    for start, end in intervals:
        near = next((m for m in meals if m["end"] + gap >= start and m["start"] - gap <= end), None)
        if near:
            near["start"], near["end"] = min(near["start"], start), max(near["end"], end)
        else:
            near = {"ref": db.collection("meals").document(str(uuid.uuid4())), "start": start, "end": end}
            meals.append(near)
        near["changed"] = True
    for m in (x for x in meals if x.get("changed")):
        coughs = sum(1 for t in cough_times if m["start"] <= t <= m["end"])
        batch.set(m["ref"], {"connection_id": conn_id, "started_at": m["start"],
                             "duration": int((m["end"] - m["start"]).total_seconds()),
                             "calm_index": calm_index(coughs)})


def notify_cough(observer_device_id: Optional[str], seconds: int) -> None:
    """Push «Обнаружен кашель в течение n секунд». Настоящая отправка (FCM) — позже."""
    log.info("push Наблюдателю %s: Обнаружен кашель в течение %s секунд", observer_device_id, seconds)


@app.post("/api/v1/events/batch", tags=["events"], summary="Отправить пачку звуков (Станция)")
def receive_events_batch(data: EventBatchRequest, auth: Auth = Depends(require_role("station"))):
    if len(data.events) > MAX_BATCH:
        raise ApiError(413, "batch_too_large", f"В пачке не больше {MAX_BATCH} звуков")
    conn = current_connection(auth)
    conn_id = conn.id

    # Номер звука создаёт телефон и он же служит номером документа:
    # если такой документ уже есть, звук получен раньше (дубль).
    unique = list({str(e.id): e for e in data.events}.values())
    refs = [db.collection("sounds").document(str(e.id)) for e in unique]
    known = {s.id for s in db.get_all(refs) if s.exists}
    seen, duplicates, fresh = set(), [], []
    for e in data.events:
        eid = str(e.id)
        if eid in known or eid in seen:
            duplicates.append(eid)
        else:
            seen.add(eid)
            fresh.append(e)

    pet = find_pet(conn_id)
    batch = db.batch()
    for e in fresh:
        batch.set(db.collection("sounds").document(str(e.id)), {
            "pets_id": pet.id if pet else None, "connection_id": conn_id, "type": e.type,
            "confidence": e.confidence, "started_at": e.started_at, "duration": e.duration})
    coughs = [e for e in fresh if e.type == "cough"]
    for c in coughs:   # номер аномалии равен номеру звука: повторная отправка не создаст дубль
        batch.set(db.collection("anomalies").document(str(c.id)), {
            "connection_id": conn_id, "kind": "cough", "duration": c.duration,
            "started_at": c.started_at, "seen": False})

    food = [e for e in fresh if e.type in ("chew", "lap")]
    if food:
        lo = min(e.started_at for e in food) - timedelta(days=1)
        old_coughs = [d.to_dict()["started_at"] for d in
                      where("anomalies", "connection_id", "==", conn_id)
                      .where(filter=FieldFilter("started_at", ">=", lo)).stream()
                      if d.to_dict()["kind"] == "cough"]
        save_meals(conn_id, batch, food, old_coughs + [c.started_at for c in coughs])
    batch.commit()

    if coughs:
        notify_cough(conn.to_dict().get("observer_id"), max(c.duration for c in coughs))
    return {"accepted": len(fresh), "duplicates": duplicates}


@app.post("/api/v1/events/audio", status_code=201, tags=["events"],
          summary="Отправить аудиофайл: Gemini определит звук (Станция)")
def receive_audio(audio: UploadFile = File(..., alias="file"), auth: Auth = Depends(require_role("station"))):
    mime = audio.content_type or ""
    if not mime.startswith("audio/"):
        raise ApiError(422, "validation_error", "Проверьте поля запроса",
                       [{"field": "file", "issue": "нужен аудиофайл (mp3, wav, ogg, m4a)"}])
    data = audio.file.read(MAX_AUDIO + 1)
    if len(data) > MAX_AUDIO:
        raise ApiError(413, "audio_too_large", "Аудио больше 10 МБ")
    try:
        result = gemini_service.analyze_audio(data, "audio/mpeg" if mime == "audio/mp3" else mime)
    except Exception as e:
        log.error("Gemini недоступен: %s", e)
        raise ApiError(502, "ai_unavailable", "ИИ-сервис не ответил, попробуйте позже")
    sound_type = result.get("type") if result.get("type") in ("cough", "chew", "lap", "bark") else "other"
    try:
        confidence = max(0.0, min(1.0, float(result.get("confidence", 0.5))))
        duration = max(0, min(600, int(result.get("duration", 5))))
    except (TypeError, ValueError):
        confidence, duration = 0.5, 5
    event = SoundEventDTO(id=uuid.uuid4(), type=sound_type, confidence=confidence,
                          started_at=datetime.now(timezone.utc), duration=duration)
    receive_events_batch(EventBatchRequest(events=[event]), auth)   # сохраняем как обычный звук
    return {"id": str(event.id), "type": sound_type, "confidence": confidence,
            "duration": duration, "description": str(result.get("description", ""))[:300]}


def meal_dict(d) -> dict:
    m = d.to_dict()
    return {"id": d.id, "started_at": iso(m["started_at"]), "duration": m["duration"], "calm_index": m["calm_index"]}


def anomaly_dict(d) -> dict:
    a = d.to_dict()
    return {"id": d.id, "type": a["kind"], "started_at": iso(a["started_at"]), "duration": a["duration"]}


@app.get("/api/v1/meals", tags=["diary"], summary="Приёмы пищи (список по страницам)")
def get_meals(limit: int = Query(20, ge=1, le=100), cursor: Optional[str] = None,
              auth: Auth = Depends(current_auth)):
    return list_page("meals", current_connection(auth).id, limit, cursor, meal_dict)


@app.get("/api/v1/anomalies", tags=["diary"], summary="Аномалии (кашель, долгая пауза)")
def get_anomalies(limit: int = Query(20, ge=1, le=100), cursor: Optional[str] = None,
                  auth: Auth = Depends(current_auth)):
    return list_page("anomalies", current_connection(auth).id, limit, cursor, anomaly_dict)


@app.get("/api/v1/diary/summary", tags=["diary"], summary="Сводка для графиков Дневника")
def get_diary_summary(period: Literal["week", "month"], auth: Auth = Depends(current_auth)):
    conn_id = current_connection(auth).id
    days_count = 7 if period == "week" else 30
    first = datetime.now(timezone.utc).date() - timedelta(days=days_count - 1)
    start = datetime(first.year, first.month, first.day, tzinfo=timezone.utc)

    def since(name):
        return [d.to_dict() for d in where(name, "connection_id", "==", conn_id)
                .where(filter=FieldFilter("started_at", ">=", start)).stream()]

    meals, anomalies = since("meals"), since("anomalies")
    days = []
    for i in range(days_count):
        day = first + timedelta(days=i)
        days.append({"date": day.isoformat(),
                     "meals": sum(1 for m in meals if m["started_at"].date() == day),
                     "anomalies": sum(1 for a in anomalies if a["started_at"].date() == day)})
    avg = round(sum(m["calm_index"] for m in meals) / len(meals), 1) if meals else 0
    return {"period": period, "days": days, "avg_calm_index": avg}


# --- Практика 10: ИИ-анализ фото (Gemini) и Радионяня ------------------
def analysis_out(doc_id: str, a: dict) -> dict:
    created = a["created_at"]
    return {"id": doc_id, "kind": a["kind"], "eaten_percent": a.get("eaten_percent"),
            "comment": a.get("comment"), "created_at": iso(created) if hasattr(created, "astimezone") else created}


@app.post("/api/v1/food-analyses", status_code=201, tags=["food"],
          summary="Отправить фото миски или корма на анализ (Наблюдатель)")
def create_food_analysis(kind: Literal["bowl_after", "feed_label"] = Form(...),
                         photo: UploadFile = File(...),
                         auth: Auth = Depends(require_role("observer"))):
    conn = current_connection(auth)
    mime = photo.content_type or ""
    if mime not in ("image/jpeg", "image/png", "image/webp"):
        raise ApiError(422, "validation_error", "Проверьте поля запроса",
                       [{"field": "photo", "issue": "нужен файл JPEG, PNG или WebP"}])
    data = photo.file.read(MAX_PHOTO + 1)
    if len(data) > MAX_PHOTO:
        raise ApiError(413, "photo_too_large", "Фото больше 8 МБ")
    try:
        result = gemini_service.analyze_photo(data, mime, kind)
    except Exception as e:   # ключ и подробности ошибки пользователю не показываем
        log.error("Gemini недоступен: %s", e)
        raise ApiError(502, "ai_unavailable", "ИИ-сервис не ответил, попробуйте позже")
    percent = result.get("eaten_percent")
    if percent is not None:
        try:
            percent = max(0, min(100, int(percent)))
        except (TypeError, ValueError):
            percent = None
    body = {"connection_id": conn.id, "kind": kind, "eaten_percent": percent,
            "comment": str(result.get("comment", ""))[:500], "created_at": datetime.now(timezone.utc)}
    doc_id = str(uuid.uuid4())
    db.collection("food_analyses").document(doc_id).set(body)
    return analysis_out(doc_id, body)


@app.get("/api/v1/food-analyses/{analysis_id}", tags=["food"], summary="Результат анализа фото")
def get_food_analysis(analysis_id: str, auth: Auth = Depends(current_auth)):
    doc = db.collection("food_analyses").document(analysis_id).get()
    # чужой анализ выглядит как несуществующий
    if not doc.exists or doc.to_dict().get("connection_id") != current_connection(auth).id:
        raise ApiError(404, "not_found", "Анализ не найден")
    return analysis_out(doc.id, doc.to_dict())


@app.post("/api/v1/live/sessions", tags=["live"], summary="Начать Радионяню (Наблюдатель)")
def start_live_session(auth: Auth = Depends(require_role("observer"))):
    raise ApiError(501, "not_implemented", "Радионяня будет добавлена позже")
