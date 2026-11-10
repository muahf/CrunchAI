#!/usr/bin/env bash
# Запускает сервер, ждёт готовности, гонит коллекцию Postman, останавливает сервер.
set -u
python -m uvicorn main:app --host 127.0.0.1 --port 5000 > uvicorn.log 2>&1 &
SERVER_PID=$!

for i in $(seq 1 30); do
  if curl -fs http://127.0.0.1:5000/health > /dev/null; then break; fi
  sleep 1
done

COLLECTION=$(find .. -iname "*postman_collection*.json" -not -path "*/node_modules/*" | head -n 1)
if [ -z "$COLLECTION" ]; then
  echo "ОШИБКА: файл коллекции Postman не найден в репозитории. Содержимое server/:"
  ls -R .. | head -n 50
  kill $SERVER_PID 2>/dev/null
  exit 1
fi
echo "Коллекция: $COLLECTION"

newman run "$COLLECTION" \
  --env-var "base_url=http://127.0.0.1:5000" \
  --reporters cli
RESULT=$?

kill $SERVER_PID 2>/dev/null
if [ $RESULT -ne 0 ]; then
  echo "--- лог сервера ---"
  cat uvicorn.log
fi
exit $RESULT
