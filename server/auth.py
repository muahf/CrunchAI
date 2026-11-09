import uuid
from typing import Optional
from fastapi import Header, HTTPException, status, Depends

def verify_token(authorization: Optional[str] = Header(None)) -> str:
    """Проверяет наличие Bearer-токена в заголовке Authorization."""
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={
                "error": {
                    "code": "token_invalid",
                    "message": "Нет токена или он недействителен",
                    "request_id": f"req_{uuid.uuid4().hex[:6]}"
                }
            }
        )
    return authorization.split(" ")[1]

def require_role(required_role: str):
    """
    Проверяет, соответствует ли токен (или пользователь) требуемой роли.
    При некорректной роли возвращает HTTP 403 с кодом ошибки 'wrong_role'.
    """
    def role_checker(token: str = Depends(verify_token)) -> str:
        # В данном учебном примере роль извлекается из мок-токена (например: "jwt_station_123" или "jwt_observer_456")
        # В продакшене здесь происходит декодирование JWT (jwt.decode) и чтение payload["role"]
        if required_role not in token:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail={
                    "error": {
                        "code": "wrong_role",
                        "message": f"Доступ разрешён только для роли '{required_role}'",
                        "request_id": f"req_{uuid.uuid4().hex[:6]}"
                    }
                }
            )
        return token
    return role_checker