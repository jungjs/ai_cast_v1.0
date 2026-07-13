# ?�� VS Code?�서 Ollama 구성?�기

> **?�도:** VS Code?�서 Ollama(로컬 LLM)�??�용??개발  
> **?�상 ?�간:** 10-15�? 
> **?�전 ?�구:** Ollama ?�치 ?�료 + Gemma4-9B ?�운로드

---

## ?�� 개요

VS Code?�서 Ollama�??�용?�면:

- ??로컬?�서 ?�동?�성 기능
- ???�프?�인 개발 가??
- ???�라?�버??보호 (?��? ?�송 ?�음)
- ??빠른 ?�답 (GPU ?�용)
- ??무료 (Subscription 불필??

### 권장 ?�션

| ?�션 | 기능 | ?�정?�이??| 추천??|
| --- | --- | --- | --- |
| **Continue.dev** | Chat + ?�동?�성 | ?��? | ⭐⭐⭐⭐�?|
| **Ollama API** | 기본 API ?�용 | 중간 | ⭐⭐�?|
| **기�? ?�장** | ?�한??| ?�려?� | ⭐⭐ |

**추천: Continue.dev** (가???�고 강력??

---

## ?? ?�션 1: Continue - Open-source AI Code Agent (추천)

### ?�개

**Continue**???�픈?�스 AI 코드 ?�이?�트�?

- 로컬 LLM (Ollama) ?�벽 지??
- Chat + ?�동?�성 + 코드 분석
- ?�전???�픈?�스 & ?�라?�빗
- VS Code ?�합

### Step 1: Ollama ?�인

Ollama가 로컬?�서 ?�행 중인지 ?�인:

```bash
# ?��??�에??Ollama ?�행 (백그?�운??
ollama serve

# ?�른 ?��??�에???�인
curl http://localhost:11434/api/tags

# ?�답 ?�시:
# {"models":[{"name":"gemma4:9b","modified_time":"..."}]}
```

**?�인 ?�항:**
- ??Ollama ?�버 ?�행 �?(기본 ?�트: 11434)
- ??모델 ?�운로드 ?�료 (gemma4:9b ??
- ???��??�에??curl ?�답 ?�인

### Step 2: VS Code ?�장 ?�치

VS Code�??�고 ?�장 마켓?�레?�스 검??

1. **?�장 ?�기**: `Ctrl+Shift+X` (Windows/Linux) ?�는 `Cmd+Shift+X` (Mac)

2. **검??*: "Continue" ?�력

3. **?�치**: "Continue - Open-source AI Code Agent" (공식 ?�장)
   - 개발?? Continue Dev
   - ?�명: Open-source AI Code Agent
   - ?�운로드: 100�? (?�뢰???�음)

```
?�장 ?�보:
?�름: Continue - Open-source AI Code Agent
ID: Continue.continue
출판?? Continue Dev
버전: 최신
?�명: AI-powered coding assistant with Ollama support
```

### Step 3: ?�정 ?�일 ?�성

1. **?�정 ?�더 ?�기**
   - Mac: `~/.continue/config.yaml`
   - Windows: `%USERPROFILE%\.continue\config.yaml`
   - Linux: `~/.continue/config.yaml`

2. **?�으�??�성**:
   ```bash
   # Mac/Linux
   mkdir -p ~/.continue
   touch ~/.continue/config.yaml
   
   # Windows (PowerShell)
   New-Item -Path "$env:USERPROFILE\.continue" -ItemType Directory -Force
   New-Item -Path "$env:USERPROFILE\.continue\config.yaml" -ItemType File -Force
   ```

3. **?�정 ?�일 ?�성**: `config.yaml`
   ```yaml
   models:
     - title: "Ollama - Gemma4"
       provider: "ollama"
       model: "gemma4:9b"
       apiBase: "http://localhost:11434"
   
   tabAutocompleteModel:
     title: "Ollama - Gemma4"
     provider: "ollama"
     model: "gemma4:9b"
     apiBase: "http://localhost:11434"
   
   embeddingsProvider:
     provider: "ollama"
     model: "nomic-embed-text"
     apiBase: "http://localhost:11434"
   ```

### Step 4: VS Code ?�정 ?�데?�트

VS Code ?�정 ?�일 (`settings.json`)??추�?:

1. **?�정 ?�기**: `Ctrl+,` (Windows/Linux) ?�는 `Cmd+,` (Mac)

2. **JSON ?�집 ?�기**: ?�측 ?�단??`{}` ?�이�??�릭

3. **?�음 추�?**:
   ```json
   {
     "continue.enableAutoScroll": true,
     "continue.devMode": false,
     "[python]": {
       "editor.defaultFormatter": "ms-python.python",
       "editor.formatOnSave": true,
       "editor.codeActionsOnSave": {
         "source.organizeImports": "explicit"
       }
     }
   }
   ```

### Step 5: ?�스??

1. **VS Code ?�시??* (?�는 `Ctrl+Shift+P` ??"Developer: Reload Window")

2. **Continue ?�널 ?�기**:
   - 좌측 ?�이?�바??"Continue" ?�이�??�인
   - ?�는 `Ctrl+L` (Chat ?�널)

3. **Chat ?�스??*:
   ```
   질문: "Hello, what's your name?"
   
   ?�답: Gemma4?�서 ?�답 (로컬?�서 처리)
   ```

4. **?�동?�성 ?�스??*:
   ```python
   # Python ?�일?�서 ?�수 ?�성 ??
   def my_function(
   # Tab ???�르�??�동 ?�성 ?�안
   ```

**?�인 ?�항:**
- ??Chat 창에???�답 ?�인
- ???�동?�성 ?�안 ?�성??
- ???�답 ?�간 1-3�?(GPU ?�용)

---

## ?�� ?�션 2: Ollama API 직접 ?�용

??가볍거??맞춤 ?�정???�요??경우:

### Ollama API ?�드?�인??

```bash
# 기본 ?�트
http://localhost:11434

# 주요 ?�드?�인??
POST   /api/generate      # ?�스???�성
POST   /api/chat          # ?�?�형 모드
GET    /api/tags          # ?�치??모델 ?�인
POST   /api/embeddings    # ?�베???�성
```

### curl�??�스??

```bash
# 기본 ?�성
curl -X POST http://localhost:11434/api/generate -d '{
  "model": "gemma4:9b",
  "prompt": "Hello, what is your name?",
  "stream": false
}'

# ?�?�형 (chat)
curl -X POST http://localhost:11434/api/chat -d '{
  "model": "gemma4:9b",
  "messages": [
    {
      "role": "user",
      "content": "Hello"
    }
  ],
  "stream": false
}'
```

### VS Code ?�장?�서 ?�용

커스?� ?�장 개발 ??

```javascript
// VS Code ?�장 코드 ?�시
const response = await fetch('http://localhost:11434/api/generate', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    model: 'gemma4:9b',
    prompt: userInput,
    stream: false
  })
});

const data = await response.json();
console.log(data.response);
```

---

## ?�️ 고급 ?�정

### ?�능 최적??

**Ollama ?�경 변??* (`~/.zshrc` ?�는 `~/.bashrc`):

```bash
# ?�레?????�정
export OLLAMA_NUM_THREAD=8

# GPU 메모�??�당
export OLLAMA_GPU=1

# 모델 캐시 경로
export OLLAMA_MODELS=~/.ollama/models
```

### 모델 ?�환

`config.yaml`?�서 모델 변�?

```json
{
  "models": [
    {
      "title": "Ollama - Gemma4 (빠름)",
      "provider": "ollama",
      "model": "gemma4:9b"
    },
    {
      "title": "Ollama - Llama2 (?�확??",
      "provider": "ollama",
      "model": "llama2:13b"
    },
    {
      "title": "Ollama - Mistral (가벼�?)",
      "provider": "ollama",
      "model": "mistral:7b"
    }
  ]
}
```

### ?�스???�롬?�트 커스?�마?�징

```json
{
  "models": [
    {
      "title": "Ollama - Gemma4",
      "provider": "ollama",
      "model": "gemma4:9b",
      "systemPrompt": "You are a helpful coding assistant. Provide code examples in the user's language. Keep responses concise."
    }
  ]
}
```

---

## ?�� 문제 ?�결

### 문제: "Cannot connect to localhost:11434"

**?�결:**
```bash
# 1. Ollama ?�버 ?�행 ?�인
ollama serve

# 2. ?�트 ?�인
curl http://localhost:11434/api/tags

# 3. Windows: 방화�??�인
# Settings > Privacy & Security > Windows Defender Firewall
# > Allow an app through firewall > ollama 추�?
```

### 문제: ?�동?�성???�동?��? ?�음

**?�결:**
```bash
# 1. config.yaml 경로 ?�인
# Mac: ~/.continue/config.yaml 존재?

# 2. VS Code ?�시??
# Ctrl+Shift+P > "Developer: Reload Window"

# 3. Continue ?�장 ?�데?�트
# ?�장 마켓?�레?�스 > Continue > Update
```

### 문제: ?�답???�무 ?�림

**?�결:**
```bash
# 1. ??가벼운 모델 ?�용
ollama pull mistral:7b

# 2. GPU ?�용 ?�인
ollama ps  # ?�행 중인 모델 ?�인

# 3. ?�스??리소???�인
# 메모�?CPU ?�용�?모니?�링
```

### 문제: 모델??찾을 ???�음

**?�결:**
```bash
# 1. 모델 ?�운로드
ollama pull gemma4:9b

# 2. ?�치??모델 ?�인
ollama list

# 3. config.yaml?�서 모델�??�확???�인
# "model": "gemma4:9b" (공백 주의)
```

---

## ?�� ?�능 비교

| 모델 | ?�기 | ?�답?�간 | ?�확??| 추천?�도 |
| --- | --- | --- | --- | --- |
| **mistral:7b** | 4.1GB | 1-2�?| ⭐⭐�?| 빠른 ?�성 |
| **gemma4:9b** | 5.2GB | 2-3�?| ⭐⭐⭐⭐ | ?�반 개발 |
| **llama2:13b** | 7.4GB | 3-5�?| ⭐⭐⭐⭐�?| ?�확??분석 |

**추천:** 개발 중에??**gemma4:9b** (?�도?� ?�확??균형)

---

## ?�� 참고 ?�료

- [Continue.dev 공식 문서](https://continue.dev)
- [Ollama GitHub](https://github.com/ollama/ollama)
- [Ollama 모델 ?�이브러�?(https://ollama.ai/library)
- [VS Code ?�장 마켓?�레?�스](https://marketplace.visualstudio.com)

---

## ??체크리스??

```bash
??Ollama ?�치 ?�료 (ollama --version)
??모델 ?�운로드 ?�료 (ollama list)
??Ollama ?�버 ?�행 �?(ollama serve)
???�트 ?�인 (curl http://localhost:11434/api/tags)
??Continue ?�장 ?�치
??config.yaml ?�일 ?�성 �??�정
??VS Code ?�시??
??Chat 창에???�스???�공
???�동?�성 기능 ?�동 ?�인
```

?�성?�면 로컬?�서 ?�전???�프?�인 개발 가?? ??

---

**최종 ?�데?�트**: 2026-04-08  
**버전**: v1.0

