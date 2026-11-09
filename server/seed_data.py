import os
import datetime
from google.cloud import firestore
from google.auth.credentials import AnonymousCredentials

# Подключение к локальному Firestore Emulator
os.environ["FIRESTORE_EMULATOR_HOST"] = "127.0.0.1:8080"

db = firestore.Client(
    project="crunchai-c91e2",
    credentials=AnonymousCredentials()
)

def seed_database():
    print("🚀 Начинаем заполнение базы данных Firestore тестовыми данными...")

    # --- 1. Пользователь (Users) ---
    user_id = "user-001"
    db.collection("users").document(user_id).set({
        "email": "anna@example.com",
        "password_hash": "$2b$12$eImiTXuWVxfM37uY4JANjO5E.fake_hash",
        "created_at": firestore.SERVER_TIMESTAMP
    })

    # --- 2. Устройства (Devices) ---
    station_uid = "station-uid-001"
    observer_uid = "observer-uid-002"

    db.collection("devices").document(station_uid).set({
        "user_id": user_id,
        "role": "station", 
        "name": "Старый телефон у миски (Станция)",
        "fcm_token": "fcm_token_station_sample_123"
    })

    db.collection("devices").document(observer_uid).set({
        "user_id": user_id,
        "role": "observer",
        "name": "Телефон Анны (Наблюдатель)",
        "fcm_token": "fcm_token_observer_sample_456"
    })

    # --- 3. Подключение (Connections) ---
    pair_id = "pair-test-123"
    db.collection("connections").document(pair_id).set({
        "station_id": station_uid,
        "observer_id": observer_uid,
        "key": "4F7-A9C",
        "time_key": datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(minutes=10),
        "created_at": firestore.SERVER_TIMESTAMP
    })

    # --- 4. Профиль питомца (Pets) ---
    pet_id = "pet-rex-001"
    db.collection("pets").document(pet_id).set({
        "connection_id": pair_id,
        "name": "Рекс",
        "species": "dog",
        "age_month": 36,
        "weight_kg": 18.4,
        "body_score": 5,
        "updated_at": firestore.SERVER_TIMESTAMP
    })

    # --- 5. Звуки (Sounds) ---
    sounds_ref = db.collection("sounds")
    sounds_ref.document("11111111-1111-4111-8111-111111111111").set({
        "pets_id": pet_id, 
        "connection_id": pair_id,
        "type": "cough",
        "confidence": 0.91,
        "started_at": datetime.datetime.now(datetime.timezone.utc),
        "duration": 6
    })
    sounds_ref.document("22222222-2222-4222-8222-222222222222").set({
        "pets_id": pet_id,
        "connection_id": pair_id,
        "type": "chew",
        "confidence": 0.83,
        "started_at": datetime.datetime.now(datetime.timezone.utc),
        "duration": 2
    })

    # --- 6. Прием пищи (Meals) ---
    db.collection("meals").document("meal-001").set({
        "connection_id": pair_id,
        "started_at": datetime.datetime.now(datetime.timezone.utc),
        "duration": 240,
        "calm_index": 8
    })

    # --- 7. Отклонения (Anomalies) ---
    db.collection("anomalies").document("anomaly-001").set({
        "connection_id": pair_id,
        "kind": "cough",
        "duration": 6,
        "started_at": datetime.datetime.now(datetime.timezone.utc),
        "seen": False
    })

    # --- 8. Анализ корма/миски (Food Analyses) ---
    db.collection("food_analyses").document("food-001").set({
        "connection_id": pair_id,
        "kind": "bowl_after",
        "photo_url": "https://storage.googleapis.com/crunchai/bowl-photo.jpg",
        "status": "done",
        "eaten_percent": 70,
        "created_at": firestore.SERVER_TIMESTAMP
    })

    print("✅ База Firestore успешно заполнена тестовыми данными!")

if __name__ == "__main__":
    seed_database()