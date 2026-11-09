"""Тестовые данные для эмулятора. Запуск: python seed_data.py  (эмулятор уже запущен)."""
import uuid
from datetime import datetime, timedelta, timezone

from google.cloud import firestore

from main import db, hash_password, make_token

now = datetime.now(timezone.utc).replace(microsecond=0)

# очищаем старые тестовые данные
for name in ["users", "devices", "connections", "pets", "sounds", "meals", "anomalies", "food_analyses"]:
    for d in db.collection(name).stream():
        d.reference.delete()

user_id, station_id, observer_id, conn_id, pet_id = (str(uuid.uuid4()) for _ in range(5))
db.collection("users").document(user_id).set(
    {"email": "owner@example.com", "password_hash": hash_password("password123"),
     "created_at": firestore.SERVER_TIMESTAMP})
db.collection("devices").document(station_id).set(
    {"user_id": user_id, "role": "station", "name": "Телефон у миски", "fcm_token": None})
db.collection("devices").document(observer_id).set(
    {"user_id": user_id, "role": "observer", "name": "Телефон владельца", "fcm_token": None})
db.collection("connections").document(conn_id).set(
    {"station_id": station_id, "observer_id": observer_id, "key": "TST-001",
     "time_key": now + timedelta(minutes=10), "created_at": firestore.SERVER_TIMESTAMP})
db.collection("pets").document(pet_id).set(
    {"connection_id": conn_id, "name": "Рекс", "species": "dog", "age_month": 36,
     "weight_kg": 18.4, "body_score": 5})

for i in range(25):
    db.collection("meals").document(str(uuid.uuid4())).set(
        {"connection_id": conn_id, "started_at": now - timedelta(hours=8 * i),
         "duration": 240 + i, "calm_index": 10 - i % 4})
for i in range(3):
    db.collection("anomalies").document(str(uuid.uuid4())).set(
        {"connection_id": conn_id, "kind": "cough", "duration": 6,
         "started_at": now - timedelta(days=i), "seen": False})

print("Готово. Скопируйте токены в Swagger (кнопка Authorize):")
print("\nСТАНЦИЯ:\n", make_token(station_id, user_id, "station"))
print("\nНАБЛЮДАТЕЛЬ:\n", make_token(observer_id, user_id, "observer"))
print("\nВход: owner@example.com / password123")
