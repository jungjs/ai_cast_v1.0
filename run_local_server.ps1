# 로컬 웹 서버 구동을 위한 환경 변수 세팅 (자격 증명 노출 방지 및 포터블 설정)
$env:AZURE_SPEECH_KEY="YOUR_AZURE_SPEECH_KEY_HERE"
$env:AZURE_SPEECH_REGION="koreacentral"

$env:AZURE_OPENAI_KEY="YOUR_AZURE_OPENAI_KEY_HERE"
$env:AZURE_OPENAI_ENDPOINT="https://aoai-aicast-dev.openai.azure.com/"
$env:AZURE_OPENAI_DEPLOYMENT_NAME="gpt-4o"

$env:AZURE_TRANSLATOR_KEY="YOUR_AZURE_TRANSLATOR_KEY_HERE"
$env:AZURE_TRANSLATOR_REGION="koreacentral"

$env:AZURE_VISION_KEY="YOUR_AZURE_VISION_KEY_HERE"
$env:AZURE_VISION_ENDPOINT="https://aivision-aicast.cognitiveservices.azure.com/"

$env:AZURE_STORAGE_CONNECTION_STRING="YOUR_AZURE_STORAGE_CONNECTION_STRING_HERE"
$env:AZURE_STORAGE_CONTAINER_NAME="uploads"

# Spring AI 호환용 환경 변수
$env:SPRING_AI_AZURE_OPENAI_API_KEY="YOUR_SPRING_AI_AZURE_OPENAI_API_KEY_HERE"
$env:SPRING_AI_AZURE_OPENAI_ENDPOINT="https://aoai-aicast-dev.openai.azure.com/"
$env:SPRING_AI_AZURE_OPENAI_CHAT_OPTIONS_MODEL="gpt-4o"

# 현재 스크립트 파일이 위치한 폴더 절대 경로 추출 (한글 경로 깨짐 방지)
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
if ([string]::IsNullOrEmpty($scriptPath)) { $scriptPath = Get-Location }

# Java & Maven 경로 설정 (내장된 포터블 경로로 매핑)
$env:JAVA_HOME="$scriptPath\jdk17\jdk-17.0.10+7"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"

Write-Host "==========================================================" -ForegroundColor Green
Write-Host " 로컬 AI Cast 웹 서버를 기동합니다." -ForegroundColor Green
Write-Host " - Port: 8080" -ForegroundColor Green
Write-Host " - 대시보드 URL: http://localhost:8080/dashboard" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Green

# Spring Boot 서버 구동
& "$scriptPath\maven\apache-maven-3.8.8\bin\mvn.cmd" spring-boot:run
