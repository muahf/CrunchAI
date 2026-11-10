import pytest
from fastapi import FastAPI, Depends
from fastapi.testclient import TestClient
from auth import verify_token, require_role

# Приложение для изолированного тестирования авторизации
app = FastAPI()

@app.get("/test/protected")
def protected_route(token: str = Depends(verify_token)):
    return {"status": "ok", "token": token}

@app.get("/test/station-only")
def station_route(token: str = Depends(require_role("station"))):
    return {"status": "station_granted"}

@app.get("/test/observer-only")
def observer_route(token: str = Depends(require_role("observer"))):
    return {"status": "observer_granted"}

client = TestClient(app)

def test_missing_token():
    """1. Запрос без токена возвращает 401 token_invalid"""
    res = client.get("/test/protected")
    assert res.status_code == 401
    assert res.json()["detail"]["error"]["code"] == "token_invalid"

def test_valid_token_success():
    """2. Запрос с верным токеном проходит успешно"""
    headers = {"Authorization": "Bearer jwt_station_123"}
    res = client.get("/test/protected", headers=headers)
    assert res.status_code == 200
    assert res.json()["status"] == "ok"

def test_role_access_granted():
    """3. Доступ разрешен при совпадении роли"""
    headers = {"Authorization": "Bearer jwt_station_123"}
    res = client.get("/test/station-only", headers=headers)
    assert res.status_code == 200

def test_role_access_forbidden():
    """4. Доступ запрещен (403 wrong_role) при неверной роли"""
    headers = {"Authorization": "Bearer jwt_station_123"}
    res = client.get("/test/observer-only", headers=headers)
    assert res.status_code == 403
    assert res.json()["detail"]["error"]["code"] == "wrong_role"