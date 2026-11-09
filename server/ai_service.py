import os
import json
import logging
from google import genai
from google.genai import types

logger = logging.getLogger("crunchai-backend")

def get_gemini_client():
    """Извлекает ключ только из переменных окружения (.env)."""
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        logger.error("ОШИБКА: GEMINI_API_KEY отсутствует в .env!")
        raise ValueError("GEMINI_API_KEY не найден в переменных окружения")
    return genai.Client(api_key=api_key)

async def analyze_audio_with_gemini(audio_bytes: bytes, mime_type: str = "audio/wav") -> dict:
    """
    Отправляет байты аудиозаписи в Gemini AI для распознавания типа звука питомца.
    """
    try:
        client = get_gemini_client()
        prompt = (
            "Проанализируй этот аудиофрагмент. Определи, какой звук издает домашнее животное. "
            "Верни ответ строго в формате JSON без разметки markdown:\n"
            '{"type": "cough" | "chew" | "bark" | "meow" | "unknown", '
            '"confidence": 0.95, "description": "короткое описание"}'
        )

        response = client.models.generate_content(
            model="gemini-2.5-flash",
            contents=[
                types.Part.from_bytes(data=audio_bytes, mime_type=mime_type),
                prompt
            ]
        )
        return {"status": "success", "data": json.loads(response.text)}
    except json.JSONDecodeError:
        return {"status": "success", "data": {"type": "unknown", "confidence": 0.5, "raw": response.text}}
    except Exception as e:
        logger.error(f"Сбой обращения к Gemini API (Audio): {str(e)}")
        return {"status": "error", "message": str(e)}

async def analyze_bowl_photo_with_gemini(image_bytes: bytes, mime_type: str = "image/jpeg") -> dict:
    """
    Отправляет фото миски в Gemini AI для расчёта процента съеденного корма.
    """
    try:
        client = get_gemini_client()
        prompt = (
            "Оцени процент съеденного корма по фото миски. "
            'Верни ответ строго в формате JSON: {"eaten_percent": 70, "comment": "описание"}'
        )

        response = client.models.generate_content(
            model="gemini-2.5-flash",
            contents=[
                types.Part.from_bytes(data=image_bytes, mime_type=mime_type),
                prompt
            ]
        )
        return {"status": "success", "data": json.loads(response.text)}
    except Exception as e:
        logger.error(f"Сбой обращения к Gemini API (Photo): {str(e)}")
        return {"status": "error", "message": str(e)}