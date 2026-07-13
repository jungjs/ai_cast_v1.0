# ?�� Ollama 직접 ?�치 가?�드 (Local Development)

> **?�로?�트:** [PROJECT_NAME]  
> **?�도:** 로컬 개발 ?�경 (개발??각각 ?�치)  
> **?�상 ?�간:** 15�?+ 모델 ?�운로드 (5-20�?

---

## ?�� 개요

??가?�드??**개발 ?� �?멤버가 ?�신??PC??Ollama�?직접 ?�치**?�는 방법?�니??

### ?�징
- ??빠른 ?�치 & ?�정
- ??즉시 ?�용 가??
- ??[TECH_STACK]�?직접 ?�합
- ??Mac?�서 최고 ?�능 (Apple Silicon GPU ?�용)
- ???�버�?& 개발 최적??

### ?�이브리??구조
```
로컬 개발 (??가?�드):
?��? Ollama 직접 ?�치 ??빠른 반복 개발
  
배포/CI ?�경 (별도):
?��? Docker (?�요?? ???� ?��???
```

---

## ?���??�랫?�별 ?�치

### macOS (권장 - Apple Silicon 최적??

#### Step 1: ?�치

**Option A: 공식 ?�치 ?�로그램 (가???��?)**

```bash
# ?�운로드
# https://ollama.ai/download/mac

# ?�는 Homebrew
brew install ollama
```

**Option B: Docker Desktop ?�??직접 ?�치 (권장)**

```bash
# ?��? Docker Desktop???�치?�어 ?�다�?
# Ollama??별도 ?�치 (CPU vs GPU ?�능 차이 중요)
```

#### Step 2: ?�행

```bash
# 백그?�운?�에???�행
ollama serve

# ?�인 (?�른 ?��???
ollama list
```

#### Step 3: 모델 ?�운로드

```bash
# 권장 모델: llama2 (7B, 3.8GB, ?�답 빠름)
ollama pull llama2:7b

# ?�는 최신 모델: llama2 13B
ollama pull llama2:13b

# ?�는 가벼운 모델: mistral
ollama pull mistral

# ?�운로드 ?�인
ollama list
```

#### Step 4: ?�스??

```bash
# 모델 ?�행
ollama run llama2:7b

# ?�롬?�트?�서 ?�스??
>>> Hello, what's your name?

# 종료: /bye
```

**?�징:**
- ??Metal GPU ?�동 ?�용 (M1/M2/M3 최적??
- ???�답 ?�도: 1-2�?(GPU 가??
- ???�정 ?�일: `~/.ollama/models`

---

### Windows

#### Step 1: ?�치

```bash
# 공식 ?�치 ?�로그램 ?�운로드
# https://ollama.ai/download/windows

# ?�는 Windows Package Manager
winget install ollama
```

#### Step 2: ?�행

```bash
# ?�치 ???�동 ?�행 (백그?�운???�비??
# ?�는 ?�동 ?�작
ollama serve
```

#### Step 3: 모델 ?�운로드

```bash
# PowerShell?�서
ollama pull llama2:7b
```

#### Step 4: ?�스??

```bash
ollama run llama2:7b
```

**?�징:**
- NVIDIA GPU: CUDA ?�동 지??
- AMD GPU: ?�한??지??
- ?�정 ?�일: `%USERPROFILE%\.ollama\models`

---

### Linux (Ubuntu/Debian)

#### Step 1: ?�치

```bash
# 공식 ?�치 ?�크립트
curl -fsSL https://ollama.ai/install.sh | sh

# ?�는 ?�키지 매니?�
# Ubuntu
apt-get install ollama

# Fedora
dnf install ollama
```

#### Step 2: ?�비???�작

```bash
# systemd ?�비?�로 ?�록
sudo systemctl enable ollama
sudo systemctl start ollama

# ?�인
sudo systemctl status ollama
```

#### Step 3: 모델 ?�운로드

```bash
ollama pull llama2:7b
```

#### Step 4: ?�스??

```bash
ollama run llama2:7b
```

**?�징:**
- NVIDIA GPU: CUDA 지??
- AMD GPU: ROCm 지??
- ?�정 ?�일: `~/.ollama/models`

---

## ?�� 개발?�용 ?�정

### VS Code ?�합

**Extension ?�치:**

```
Marketplace?�서 "Ollama" 검??
?�는 "Continue.dev" ?�치
```

**?�정:**

```json
// .vscode/settings.json ?�는 VS Code settings.json
{
  "continue.serverUrl": "http://localhost:11434",
  "continue.models": [
    {
      "title": "Llama 2",
      "model": "llama2:7b",
      "provider": "ollama"
    }
  ]
}
```

**?�용:**
```
VS Code?�서 Cmd+K,C (Mac) ?�는 Ctrl+K,C (Windows)
??Ollama?�서 ?�동?�성 ?�안
```

---

### 개발?� ?�크?�로??

#### 1?�계: 로컬 ?�치 ?�료 ?�인

```bash
# 모든 ?�?�이 ?�행
ollama pull llama2:7b

# ?�답 ?�인
ollama run llama2:7b -e "test"
```

#### 2?�계: 개발 �??�용

```bash
# ?��???1: Ollama ?�비???�행
ollama serve

# ?��???2: 코딩
# - VS Code?�서 ?�동?�성 ?�용
# - ?�는 CLI?�서 ollama run llama2:7b
# - ?�는 API ?�출: curl http://localhost:11434/api/generate
```

#### 3?�계: ?�능 최적??

```bash
# GPU ?�용 ?�인
ollama run llama2:7b --verbose

# 로그 ?�인
tail -f ~/.ollama/logs/ollama.log (macOS/Linux)
# Windows: %APPDATA%\ollama\logs
```

---

## ?�� ?�러블슈??

### Q1: "ollama: command not found"

```bash
# macOS
# ?�스??PATH??추�?
export PATH="/Applications/Ollama.app/Contents/MacOS:$PATH"

# ?�는 ~/.zshrc??추�?
echo 'export PATH="/Applications/Ollama.app/Contents/MacOS:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

### Q2: 모델 ?�운로드가 ?�려??

```bash
# 문제: ?�터???�도 ?�림 ?�는 ?�스??부�?

# ?�결 1: ???��? 모델 ?�용
ollama pull mistral  # 4.1GB (??빠름)

# ?�결 2: ?�스??공간 ?�인
df -h ~/.ollama

# ?�결 3: ?�운로드 ?�개
ollama pull llama2:7b  # 중단?�었?�면 ?�시??
```

### Q3: ?�답???�무 ?�려??(3�??�상)

```bash
# 문제: GPU 미사???�는 CPU 모드

# 1?�계: GPU ?�인
ollama run llama2:7b --verbose

# macOS: "Metal Acceleration Enabled" ?�인
# Windows: "CUDA" ?�는 "GPU" ?�인
# Linux: "GPU" ?�인

# 2?�계: GPU 미사?????�결

# macOS (Apple Silicon):
# ??기본 지?? ?�동 ?�용
# ??Metal GPU ?�인: system_profiler SPDisplaysDataType

# Windows (NVIDIA):
# ??nvidia-smi 명령?�로 GPU ?�인
# ??NVIDIA Container Toolkit ?�치 ?�요?????�음

# Linux (NVIDIA):
# ??nvidia-smi 명령?�로 GPU ?�인
# ??NVIDIA CUDA Toolkit ?�치 ?�요

# 3?�계: ?�시 CPU 모드 (?�스?�용)
ollama run llama2:7b --cpu-only
```

### Q4: 모델 ??��?�고 ?�어??

```bash
# 모델 ?�인
ollama list

# ?�정 모델 ??��
ollama rm llama2:7b

# ?�?�소 ?�리
rm -rf ~/.ollama/models
```

### Q5: ?�트 변경하�??�어??(11434???��? ?�용 �?

```bash
# macOS/Linux
OLLAMA_PORT=11435 ollama serve

# Windows (PowerShell)
$env:OLLAMA_PORT = "11435"; ollama serve

# ?�는 ?�경변???�정 (.env ?�일)
OLLAMA_PORT=11435
```

---

## ?�� ?�능 비교

### 모델�??�능 (Apple Silicon Mac ?�시)

| 모델 | ?�기 | ?�운로드 | ?�답 ?�간 | 추천 |
|------|------|---------|---------|------|
| mistral | 4.1GB | 5�?| 1�?| ⭐⭐�?(빠름) |
| llama2:7b | 3.8GB | 5�?| 1-2�?| ⭐⭐⭐⭐ (균형) |
| llama2:13b | 7.4GB | 10�?| 3-4�?| ⭐⭐ (?�림) |

### 개발?� 권장 ?�정

```bash
# Step 1: 빠른 ?�스??(처음)
ollama pull mistral

# Step 2: 메인 모델 (개발 �?
ollama pull llama2:7b

# Step 3: 고급 기능 (?�요??
ollama pull llama2:13b  # ?�는 ?�른 모델
```

---

## ?�� ?� 공유 체크리스??

```bash
??Ollama ?�치 ?�료
  ??ollama --version ?�행??
  
??모델 ?�운로드 ?�료
  ??ollama list?�서 [MODEL_NAME] ?�인
  
???�스???�료
  ??ollama run [MODEL_NAME]?�서 ?�답 ?�인
  ???�답 ?�간: ___ �?(기록)
  
??VS Code ?�합 (?�택)
  ??Continue.dev ?�는 Ollama Extension ?�치
  ??http://localhost:11434 ?�결 ?�인
  
???�능 기록
  ???�용 모델: [MODEL_NAME]
  ???�답 ?�간: ___ �?
  ??GPU ?�용: [ ] Yes [ ] No
  ??OS: [macOS/Windows/Linux]
  ??CPU/GPU: _______________
```

---

## ?�� 개발?� ?��? ?�정 (권장)

```bash
# [PROJECT_NAME] ?� 공식 ?�정

모델: llama2:7b
?�트: 11434
?�?�소: ~/.ollama/models

?�치 명령:
  ollama pull llama2:7b

?�스??명령:
  ollama run llama2:7b "Hello, test."

?�답 ?�간 목표: 1-2�?(M1 Mac 기�?)
```

> **TODO**: ?� ?�의 ?????�정???�로?�트??CLAUDE.md??추�?

---

## ?�� 참고 ?�료

- [Ollama 공식 ?�이??(https://ollama.ai)
- [Ollama GitHub](https://github.com/jmorganca/ollama)
- [Ollama 모델 ?�이브러�?(https://ollama.ai/library)
- [Apple Silicon 최적??가?�드](https://github.com/jmorganca/ollama/discussions/1051)

---

## ?�� ?��?�?

문제가 발생?�면:

1. ?�의 "?�러블슈?? 참고
2. [TROUBLESHOOTING_TEMPLATE.md](../030_TROUBLESHOOTING_TEMPLATE.md) ?�인
3. ?� 리더?�게 문의
4. [Ollama GitHub Issues](https://github.com/jmorganca/ollama/issues) 검??

---

**?�음 ?�계:**
- ?�치 ?�료 ??[ZEN_A4_SETUP_TEMPLATE.md](./ZEN_A4_SETUP_TEMPLATE.md)�??�동
- ?�는 [DOCKER_COMPOSE_OLLAMA_TEMPLATE.md](./DOCKER_COMPOSE_OLLAMA_TEMPLATE.md) (배포 ?�경)

**?�치??** [YOUR_NAME]  
**?�료??** 2026-04-XX  
**버전:** 1.0

