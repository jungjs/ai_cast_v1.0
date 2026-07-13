# ?�� Docker Compose Ollama 가?�드 (?�택?�항 - 배포??

> **?�로?�트:** [PROJECT_NAME]  
> **?�도:** ?� ?��???& CI/CD 배포  
> **?�제조건:** Docker Desktop ?�치 & Ollama 로컬 개발 ?�료  
> **?�상 ?�간:** 20�?

---

## ?�� 개요

??가?�드??**?� ?�체가 ?�일???�경?�로 Ollama�??�행**?�는 방법?�니??

### ?�제 ?�요?��??

```
로컬 개발: Ollama 직접 ?�치 (OLLAMA_DIRECT_INSTALL_TEMPLATE.md)
   ??(?�요????
?� ?�업/CI/CD: Docker Compose (??가?�드)
   ??
?�라?�드 배포: Kubernetes (별도)
```

### ?�징
- ???� ?�체 ?�일 ?�경
- ??git??버전 관�?가??
- ??CI/CD ?�이?�라???�합
- ???�현 가?�한 배포
- ?�️ Apple Silicon?�서 CPU 모드�?지??(개발???�님)

---

## ?? 빠른 ?�작 (3?�계)

### Step 1: docker-compose.yml ?�성

?�로?�트 루트???�음 ?�일???�성?�세??

```yaml
# docker-compose.yml

version: '3.8'

services:
  ollama:
    image: ollama/ollama:latest
    
    # GPU 지??(NVIDIA�?
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
    
    # 볼륨 (모델 ?�??
    volumes:
      - ollama_data:/root/.ollama
    
    # ?�트
    ports:
      - "11434:11434"
    
    # ?�경 변??
    environment:
      OLLAMA_MODELS: /root/.ollama/models
    
    # ??�� ?�시??
    restart: unless-stopped

volumes:
  ollama_data:
    driver: local
```

### Step 2: 모델 ?�운로드 & ?�행

```bash
# ?��?지 ?�운로드
docker-compose pull

# ?�비???�작
docker-compose up -d

# 모델 ?�운로드 (컨테?�너 ?�에??
docker-compose exec ollama ollama pull llama2:7b

# ?�인
docker-compose exec ollama ollama list
```

### Step 3: ?�스??

```bash
# 방법 1: Docker?�서 직접 ?�스??
docker-compose exec ollama ollama run llama2:7b "Hello"

# 방법 2: API�??�스??
curl http://localhost:11434/api/generate \
  -d '{"model":"llama2:7b","prompt":"Hello"}'

# 방법 3: Python?�서
import requests
response = requests.post('http://localhost:11434/api/generate',
  json={'model':'llama2:7b','prompt':'Hello'})
```

---

## ?�� ?�체 ?�정 (?�세)

### 기본 구성

```yaml
version: '3.8'

services:
  ollama:
    image: ollama/ollama:latest
    container_name: ollama_[PROJECT_NAME]
    
    ports:
      - "11434:11434"
    
    volumes:
      - ollama_data:/root/.ollama
    
    restart: unless-stopped
    
volumes:
  ollama_data:
```

### NVIDIA GPU 지??(Linux)

```yaml
services:
  ollama:
    image: ollama/ollama:latest
    
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
    
    volumes:
      - ollama_data:/root/.ollama
    
    ports:
      - "11434:11434"
    
    restart: unless-stopped
```

### AMD GPU 지??(ROCm)

```yaml
services:
  ollama:
    image: ollama/ollama:rocm  # rocm ?��?지 ?�용
    
    environment:
      OLLAMA_DEBUG: 1
    
    volumes:
      - ollama_data:/root/.ollama
    
    ports:
      - "11434:11434"
    
    restart: unless-stopped
```

### ?�중 모델 (고급)

```yaml
version: '3.8'

services:
  ollama-fast:
    image: ollama/ollama:latest
    container_name: ollama_fast
    ports:
      - "11434:11434"
    volumes:
      - ollama_fast:/root/.ollama
    restart: unless-stopped
  
  ollama-powerful:
    image: ollama/ollama:latest
    container_name: ollama_powerful
    ports:
      - "11435:11434"
    volumes:
      - ollama_powerful:/root/.ollama
    restart: unless-stopped

volumes:
  ollama_fast:
  ollama_powerful:
```

---

## ?�� ?� ?�업 ?�정

### Step 1: git??추�?

```bash
# ?�로?�트 루트??docker-compose.yml ?�??
git add docker-compose.yml
git commit -m "infra: Add Docker Compose for Ollama"
git push
```

### Step 2: ?�???�정 (모두 ?�일)

```bash
# ?�?�소 ?�론 (?��? ?�어 ?�음)
git clone [REPO_URL]
cd [PROJECT_NAME]

# ?�비???�작
docker-compose up -d

# 모델 ?�운로드
docker-compose exec ollama ollama pull llama2:7b

# ?�료
docker-compose ps
```

### Step 3: ?�경 변??관�?(.env)

```bash
# .env ?�일 ?�성
OLLAMA_MODEL=llama2:7b
OLLAMA_PORT=11434
OLLAMA_DEBUG=0

# docker-compose.yml?�서 ?�용
environment:
  OLLAMA_MODEL: ${OLLAMA_MODEL:-llama2:7b}
  OLLAMA_PORT: ${OLLAMA_PORT:-11434}
```

---

## ?�� CI/CD ?�합 (GitHub Actions)

### ?�동 ?�스??

```yaml
# .github/workflows/test.yml

name: Test Ollama Models

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      ollama:
        image: ollama/ollama:latest
        options: >-
          --gpus all
        ports:
          - 11434:11434
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Pull model
        run: |
          docker exec $(docker ps -q) ollama pull llama2:7b
      
      - name: Test generate
        run: |
          curl http://localhost:11434/api/generate \
            -d '{"model":"llama2:7b","prompt":"test"}'
      
      - name: Run tests
        run: |
          python -m pytest tests/
```

---

## ?�� 명령??모음

### 기본 명령

```bash
# ?�작
docker-compose up -d

# 중�?
docker-compose down

# 로그 ?�인
docker-compose logs -f

# ?�태 ?�인
docker-compose ps

# 컨테?�너 진입
docker-compose exec ollama bash
```

### 모델 관�?

```bash
# 모델 ?�운로드
docker-compose exec ollama ollama pull [MODEL_NAME]

# 모델 목록
docker-compose exec ollama ollama list

# 모델 ??��
docker-compose exec ollama ollama rm [MODEL_NAME]

# 모델 ?�행
docker-compose exec ollama ollama run [MODEL_NAME] "[PROMPT]"
```

### API ?�출

```bash
# ?�스???�성
curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama2:7b",
    "prompt": "Why is the sky blue?",
    "stream": false
  }'

# ?�트리밍 ?�답
curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama2:7b",
    "prompt": "Hello",
    "stream": true
  }' | jq .
```

---

## ?�� ?�러블슈??

### Q1: NVIDIA GPU가 ?�식 ????

```bash
# ?�인: NVIDIA Container Toolkit 미설�?

# ?�치 (Ubuntu/Debian)
distribution=$(. /etc/os-release;echo $ID$VERSION_ID)
curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | sudo apt-key add -
curl -s -L https://nvidia.github.io/nvidia-docker/$distribution/nvidia-docker.list | \
  sudo tee /etc/apt/sources.list.d/nvidia-docker.list

sudo apt-get update && sudo apt-get install -y nvidia-container-toolkit
sudo systemctl restart docker

# ?�인
nvidia-smi
docker run --rm --gpus all nvidia/cuda:11.6.2-base-ubuntu20.04 nvidia-smi
```

### Q2: "Cannot connect to Docker daemon"

```bash
# ?�인: Docker Desktop ?�는 Docker ?�몬???�행 ????

# ?�결 1: Docker Desktop ?�작 (Mac/Windows)
# ?�용 ?�로그램 ??Docker ?�행

# ?�결 2: Docker ?�비???�작 (Linux)
sudo systemctl start docker
sudo usermod -aG docker $USER

# ?�인
docker ps
```

### Q3: ?�트 11434가 ?��? ?�용 �?

```bash
# ?�인: 로컬 Ollama가 ?��? ?�행 �?

# ?�결 1: 로컬 Ollama 중�?
ollama stop  # ?�을 ???�음

# ?�결 2: ?�트 변�?
# docker-compose.yml ?�정
ports:
  - "11435:11434"  # 11435�?변�?

# ?�결 3: ?�행 중인 ?�로?�스 ?�인
lsof -i :11434  # macOS/Linux
netstat -ano | findstr :11434  # Windows
```

### Q4: 모델 ?�운로드가 ?�려??

```bash
# ?�결 1: ???��? 모델 ?�용
docker-compose exec ollama ollama pull mistral

# ?�결 2: ?�운로드 ?�태 ?�인
docker-compose logs -f

# ?�결 3: ?�터???�??�� ?�인
# 모델 ?�운로드???�간???�요?????�음
```

---

## ?�� ?�능 비교

### 직접 ?�치 vs Docker

| ??�� | 직접 ?�치 | Docker |
|------|---------|--------|
| **?�답 ?�간** | 1-2�?(GPU) | 3-5�?(컨테?�너 ?�버?�드) |
| **메모�?* | 최소 | +100-200MB |
| **?�정** | 간단 | 중간 |
| **?� ?�업** | 개별 관�?| ?�일 ?�경 |
| **추천 ?�도** | 개발 | 배포/CI |

---

## ?�� ?� ?�업 가?�드

### 개발?� 권장 ?�크?�로??

```
로컬 개발 (개발??PC):
1. Ollama 직접 ?�치 (OLLAMA_DIRECT_INSTALL_TEMPLATE.md)
2. 빠른 반복 개발 & ?�스??

?� ?�합 ?�스??
1. docker-compose up -d�??��? ?�경 구성
2. 모든 ?�?�이 ?�일???��?지 ?�용
3. 문제 발생???�현 가??

배포:
1. CI/CD?�서 ?�동?�로 Docker ?��?지 빌드
2. ?�로?�션 ?�경??배포
```

---

## ?�� ?� 체크리스??

```bash
??docker-compose.yml ?�성 ?�료
  ???�일 ?�치: [PROJECT_ROOT]/docker-compose.yml
  ??git??추�???

??Docker Compose ?�행 ?�스??
  ??docker-compose up -d ?�공
  ??docker-compose ps?�서 ollama ?�행 �?
  ???�트 11434 ?�상 ?�동

??모델 ?�운로드 & ?�스??
  ??docker-compose exec ollama ollama pull llama2:7b ?�료
  ??API ?�스???�공
  
???�??모두 ?�일???�경 ?�인
  ???�??A: ?�료
  ???�??B: ?�료
  ???�??C: ?�료

??CI/CD ?�이?�라??(?�택)
  ??GitHub Actions ?�는 GitLab CI ?�정
  ???�동 ?�스???�행
```

---

## ?�� 참고 ?�료

- [Docker Compose 공식 문서](https://docs.docker.com/compose/)
- [Ollama Docker 가?�드](https://github.com/jmorganca/ollama/blob/main/docs/docker.md)
- [NVIDIA Container Toolkit](https://github.com/NVIDIA/nvidia-docker)

---

## ?�� 주의?�항

### ?�️ Apple Silicon (Mac) ?�용??

```
Docker?�서 Ollama??GPU 미�???
??CPU 모드�?가??(5-6�??�림)

권장:
??개발: 로컬 Ollama 직접 ?�치 (Metal GPU)
??배포: Docker Compose (?�요??
```

### ?�️ ?�트 충돌

```
로컬 Ollama?� Docker ?�시 ?�행 불�?
???�트 변�??�는 ?�나�??�행
```

---

**?�음 ?�계:**
- 로컬 개발: [OLLAMA_DIRECT_INSTALL_TEMPLATE.md](./OLLAMA_DIRECT_INSTALL_TEMPLATE.md)
- ?�동?? [ZEN_A4_SETUP_TEMPLATE.md](./ZEN_A4_SETUP_TEMPLATE.md)

**?�성??** [TEAM_LEADER]  
**?�성??** 2026-04-XX  
**버전:** 1.0

