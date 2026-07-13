#!/bin/bash
set -e

echo "=========================================="
echo "Starting Azure App Service Container"
echo "=========================================="

# SSH 서버 시작
echo "[INFO] Starting SSH server on port 2222..."
service ssh start

# SSH 상태 확인
if service ssh status > /dev/null 2>&1; then
    echo "[OK] SSH server is running"
else
    echo "[WARN] SSH server may not be running properly"
fi

# 애플리케이션 시작
echo "[INFO] Starting Uvicorn application on port ${PORT:-9100}..."
exec uvicorn main:app --host 0.0.0.0 --port ${PORT:-9100}