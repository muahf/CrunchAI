import json
import logging
import os
import time

from google import genai
from google.genai import types

logger = logging.getLogger("crunchai")

# Модель берётся из переменной окружения: устареет — меняете в .env, код не трогаете.
MODEL = os.getenv("GEMINI_MODEL", "gemini-3.8-flash")
FALLBACK_MODELS = ["gemini-3.6-flash"]


def get_client() -> genai.Client:
    key = os.getenv("GEMINI_API_KEY")
    if not key:
        raise RuntimeError("GEMINI_API_KEY не задан в переменных окружения")
    return genai.Client(api_key=key, http_options=types.HttpOptions(timeout=30_000))  # 30 секунд


def _ask(data: bytes, mime: str, prompt: str) -> dict:
    """Отправляет файл + вопрос в Gemini, пробует модели по очереди, возвращает dict из JSON."""
    client = get_client()
    last = None
    for model in [MODEL] + FALLBACK_MODELS:
        t0 = time.monotonic()
        try:
            resp = client.models.generate_content(
                model=model,
                contents=[types.Part.from_bytes(data=data, mime_type=mime), prompt],
                config=types.GenerateContentConfig(response_mime_type="application/json"),
            )
            logger.info("Gemini %s ответил за %.1f с", model, time.monotonic() - t0)
            return json.loads(resp.text)
        except json.JSONDecodeError as e:
            last = e              # ответ не JSON: честная ошибка, а не выдуманный результат
            break
        except Exception as e:
            last = e
            logger.warning("модель %s не сработала за %.1f с: %s", model, time.monotonic() - t0, e)
    raise RuntimeError(f"Gemini не ответил: {last}")


def analyze_audio(audio: bytes, mime: str = "audio/mpeg") -> dict:
    return _ask(audio, mime,
        "Определи, какой звук издаёт домашнее животное. Верни JSON: "
        '{"type": "cough|chew|lap|bark|other", "confidence": 0.0-1.0, '
        '"duration": длительность звука в секундах (целое), "description": "коротко по-русски"}')


def analyze_photo(image: bytes, mime: str, kind: str) -> dict:
    if kind == "bowl_after":
        prompt = ('Оцени, сколько корма съедено, по фото миски. Верни JSON: '
                  '{"eaten_percent": 0-100, "comment": "коротко по-русски"}')
    else:  # feed_label
        prompt = ('Это фото этикетки корма. Прочитай состав и оцени его для питомца. Верни JSON: '
                  '{"eaten_percent": null, "comment": "коротко по-русски: состав и вывод"}')
    return _ask(image, mime, prompt)