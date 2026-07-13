# E2E 테스트를 위한 환경 변수 세팅 (템플릿 파일 - 자격 증명을 채워 사용해 주십시오)
$env:AZURE_SPEECH_KEY="YOUR_AZURE_SPEECH_KEY"
$env:AZURE_SPEECH_REGION="koreacentral"

$env:AZURE_OPENAI_KEY="YOUR_AZURE_OPENAI_KEY"
$env:AZURE_OPENAI_ENDPOINT="https://aoai-aicast-dev.openai.azure.com/"
$env:AZURE_OPENAI_DEPLOYMENT_NAME="gpt-4o"

$env:AZURE_TRANSLATOR_KEY="YOUR_AZURE_TRANSLATOR_KEY"
$env:AZURE_TRANSLATOR_REGION="koreacentral"

$env:AZURE_VISION_KEY="YOUR_AZURE_VISION_KEY"
$env:AZURE_VISION_ENDPOINT="https://aivision-aicast.cognitiveservices.azure.com/"

$env:AZURE_STORAGE_CONNECTION_STRING="DefaultEndpointsProtocol=https;AccountName=aicaststoragedev1;AccountKey=YOUR_AZURE_STORAGE_ACCOUNT_KEY;EndpointSuffix=core.windows.net"
$env:AZURE_STORAGE_CONTAINER_NAME="uploads"

# Spring AI 호환용 환경 변수
$env:SPRING_AI_AZURE_OPENAI_API_KEY="YOUR_AZURE_OPENAI_KEY"
$env:SPRING_AI_AZURE_OPENAI_ENDPOINT="https://aoai-aicast-dev.openai.azure.com/"
$env:SPRING_AI_AZURE_OPENAI_CHAT_OPTIONS_MODEL="gpt-4o"

# 현재 스크립트 파일이 위치한 폴더 절대 경로 추출 (한글 경로 깨짐 방지)
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
if ([string]::IsNullOrEmpty($scriptPath)) { $scriptPath = Get-Location }

# Java & Maven 경로 설정 (상대 경로 결합)
$env:JAVA_HOME="$scriptPath\jdk17\jdk-17.0.10+7"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"

Write-Host "Azure E2E 환경 변수 설정 완료. E2E 통합 테스트를 시작합니다..." -ForegroundColor Green

# Maven E2E 테스트 실행 (상대 경로 결합)
& "$scriptPath\maven\apache-maven-3.8.8\bin\mvn.cmd" test -Dtest=E2eIntegrationTest
