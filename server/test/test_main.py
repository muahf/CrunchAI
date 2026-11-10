import pytest
import uuid
import datetime
from unittest.mock import patch
from fastapi.testclient import TestClient

import os
os.environ["USE_EMULATOR"] = "1"
os.environ["FIRESTORE_EMULATOR_HOST"] = "127.0.0.1:8080"

from main import app, make_token, hash_password, calm_index, encode_cursor, decode_cursor, db

client = TestClient(app)

# Test IDs and Tokens
TEST_USER_ID = "test-user-uuid-123"
TEST_STATION_ID = "test-station-device-456"
TEST_OBSERVER_ID = "test-observer-device-789"
TEST_CONN_ID = "test-conn-uuid-999"

STATION_TOKEN = make_token(TEST_STATION_ID, TEST_USER_ID, "station")
OBSERVER_TOKEN = make_token(TEST_OBSERVER_ID, TEST_USER_ID, "observer")


@pytest.fixture(autouse=True)
def setup_mock_db():
    """Seed minimal test data into Firestore emulator for testing."""
    try:
        db.collection("users").document(TEST_USER_ID).set({
            "email": "owner@example.com",
            "password_hash": hash_password("password123"),
            "created_at": datetime.datetime.now(datetime.timezone.utc)
        })
        db.collection("devices").document(TEST_STATION_ID).set({
            "user_id": TEST_USER_ID, "role": "station", "name": "Station Phone", "fcm_token": None
        })
        db.collection("devices").document(TEST_OBSERVER_ID).set({
            "user_id": TEST_USER_ID, "role": "observer", "name": "Observer Phone", "fcm_token": None
        })
        db.collection("connections").document(TEST_CONN_ID).set({
            "station_id": TEST_STATION_ID, "observer_id": TEST_OBSERVER_ID, "key": "TST-999",
            "time_key": datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=1)
        })
    except Exception:
        pass


# ======================================================================
# UNIT TESTS (Isolated Functions)
# ======================================================================
def test_unit_calm_index():
    assert calm_index(0) == 10
    assert calm_index(2) == 8
    assert calm_index(15) == 1  # Minimum bound is 1


def test_unit_cursor_encoding():
    doc_id = "sample_doc_123"
    encoded = encode_cursor(doc_id)
    decoded = decode_cursor(encoded)
    assert decoded == doc_id


# ======================================================================
# INTEGRATION TESTS (TC-01 to TC-16 Table Matrix)
# ======================================================================

# TC-01: Signup Station Success (201)
def test_tc01_signup_station_success():
    payload = {
        "email": f"test_station_{uuid.uuid4().hex[:6]}@crunchai.example",
        "password": "Password123!",
        "role": "station",
        "device_name": "Test Station Phone"
    }
    res = client.post("/api/v1/auth/signup", json=payload)
    assert res.status_code == 201
    assert "token" in res.json()
    assert res.json()["device"]["role"] == "station"


# TC-02: Signup Validation Error (422)
def test_tc02_signup_validation_error():
    payload = {"email": "bad", "password": "123", "role": "invalid"}
    res = client.post("/api/v1/auth/signup", json=payload)
    assert res.status_code == 422
    data = res.json()
    assert data["error"]["code"] == "validation_error"


# TC-03: Login Invalid Credentials (401)
def test_tc03_login_invalid_credentials():
    payload = {
        "email": "owner@example.com",
        "password": "WrongPassword!",
        "role": "observer",
        "device_name": "Observer Phone"
    }
    res = client.post("/api/v1/auth/login", json=payload)
    assert res.status_code == 401
    assert res.json()["error"]["code"] == "invalid_credentials"


# TC-04: Unauthorized Request (401)
def test_tc04_get_pet_unauthorized():
    res = client.get("/api/v1/pet")
    assert res.status_code == 401
    assert res.json()["error"]["code"] == "token_invalid"


# TC-05: Forbidden Role Access (403)
def test_tc05_create_pairing_session_forbidden_for_observer():
    headers = {"Authorization": f"Bearer {OBSERVER_TOKEN}"}
    res = client.post("/api/v1/pairing/sessions", headers=headers)
    assert res.status_code == 403
    assert res.json()["error"]["code"] == "wrong_role"


# TC-06: Create Pairing Session Station Success (201)
def test_tc06_create_pairing_session_success():
    headers = {"Authorization": f"Bearer {STATION_TOKEN}"}
    res = client.post("/api/v1/pairing/sessions", headers=headers)
    assert res.status_code == 201
    assert "key" in res.json()


# TC-07: Confirm Pairing Non-existent Key (404)
def test_tc07_confirm_pairing_invalid_key():
    headers = {"Authorization": f"Bearer {OBSERVER_TOKEN}"}
    res = client.post("/api/v1/pairing/confirm", json={"key": "WRONG-999"}, headers=headers)
    assert res.status_code == 404
    assert res.json()["error"]["code"] == "not_found"


# TC-08: Update Pet Profile Success (200)
def test_tc08_update_pet_profile_success():
    headers = {"Authorization": f"Bearer {OBSERVER_TOKEN}"}
    payload = {
        "name": "Рекс",
        "species": "dog",
        "age_month": 36,
        "weight_kg": 18.5,
        "body_score": 5
    }
    res = client.put("/api/v1/pet", json=payload, headers=headers)
    assert res.status_code == 200
    assert res.json()["name"] == "Рекс"


# TC-09: Receive Events Batch Success (200)
def test_tc09_receive_events_batch_success():
    headers = {"Authorization": f"Bearer {STATION_TOKEN}"}
    payload = {
        "events": [
            {
                "id": str(uuid.uuid4()),
                "type": "cough",
                "confidence": 0.95,
                "started_at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
                "duration": 4
            },
            {
                "id": str(uuid.uuid4()),
                "type": "chew",
                "confidence": 0.88,
                "started_at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
                "duration": 10
            }
        ]
    }
    res = client.post("/api/v1/events/batch", json=payload, headers=headers)
    assert res.status_code == 200
    assert res.json()["accepted"] == 2


# TC-10: Batch Too Large (413)
def test_tc10_receive_events_batch_too_large():
    headers = {"Authorization": f"Bearer {STATION_TOKEN}"}
    events = [
        {
            "id": str(uuid.uuid4()),
            "type": "chew",
            "confidence": 0.9,
            "started_at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
            "duration": 5
        }
        for _ in range(101)
    ]
    res = client.post("/api/v1/events/batch", json={"events": events}, headers=headers)
    assert res.status_code == 413
    assert res.json()["error"]["code"] == "batch_too_large"


# TC-11: Invalid Audio File Format (422)
def test_tc11_receive_audio_invalid_mime():
    headers = {"Authorization": f"Bearer {STATION_TOKEN}"}
    files = {"file": ("test.txt", b"Hello text file content", "text/plain")}
    res = client.post("/api/v1/events/audio", files=files, headers=headers)
    assert res.status_code == 422
    assert res.json()["error"]["code"] == "validation_error"


# TC-12: Audio AI Gemini Mock Success (201)
@patch("gemini_service.analyze_audio")
def test_tc12_receive_audio_ai_success(mock_gemini):
    mock_gemini.return_value = {
        "type": "cough",
        "confidence": 0.94,
        "duration": 4,
        "description": "Распознан влажный кашель"
    }
    headers = {"Authorization": f"Bearer {STATION_TOKEN}"}
    files = {"file": ("cough.mp3", b"mock_binary_mp3", "audio/mp3")}
    res = client.post("/api/v1/events/audio", files=files, headers=headers)
    assert res.status_code == 201
    assert res.json()["type"] == "cough"


# TC-13: Gemini AI Unavailable (502)
@patch("gemini_service.analyze_audio")
def test_tc13_receive_audio_ai_failure(mock_gemini):
    mock_gemini.side_effect = Exception("Gemini API connection error")
    headers = {"Authorization": f"Bearer {STATION_TOKEN}"}
    files = {"file": ("cough.mp3", b"mock_binary_mp3", "audio/mp3")}
    res = client.post("/api/v1/events/audio", files=files, headers=headers)
    assert res.status_code == 502
    assert res.json()["error"]["code"] == "ai_unavailable"


# TC-14: Live Sessions Not Implemented (501)
def test_tc14_start_live_session_not_implemented():
    headers = {"Authorization": f"Bearer {OBSERVER_TOKEN}"}
    res = client.post("/api/v1/live/sessions", headers=headers)
    assert res.status_code == 501
    assert res.json()["error"]["code"] == "not_implemented"


# TC-15: Get Meals List Pagination (200)
def test_tc15_get_meals_success():
    headers = {"Authorization": f"Bearer {OBSERVER_TOKEN}"}
    res = client.get("/api/v1/meals?limit=5", headers=headers)
    assert res.status_code == 200
    assert "items" in res.json()


# TC-16: Get Diary Summary Success (200)
def test_tc16_get_diary_summary_success():
    headers = {"Authorization": f"Bearer {OBSERVER_TOKEN}"}
    res = client.get("/api/v1/diary/summary?period=week", headers=headers)
    assert res.status_code == 200
    assert res.json()["period"] == "week"