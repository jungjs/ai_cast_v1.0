# syntax=docker/dockerfile:1

FROM python:3.11-slim-bullseye AS base

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PORT=9100

WORKDIR /app

# 시스템 패키지 (ffmpeg는 pydub에서 사용)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    openssh-server \
    ffmpeg \
    libasound2 \
    libcurl4 \
    libssl1.1 \
    ca-certificates 
RUN rm -rf /var/lib/apt/lists/*

# SSH 서버 설정 (Azure App Service용)
RUN mkdir -p /var/run/sshd \
    && ssh-keygen -A \
    && echo "root:Docker!" | chpasswd

# SSH 설정 파일 복사
COPY sshd_config /etc/ssh/

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

RUN python -m compileall -b . && \
    find . -name "__pycache__" -type d -prune -exec rm -rf {} + && \
    find . -name "*.py" -not -name "__init__.py" -delete

COPY init.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/init.sh

EXPOSE 2222 9100
CMD ["/usr/local/bin/init.sh"]
# CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "9100"]
