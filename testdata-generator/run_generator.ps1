# Java 17 및 환경변수 설정
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
if ([string]::IsNullOrEmpty($scriptPath)) { $scriptPath = Get-Location }

$env:JAVA_HOME="$scriptPath\..\jdk17\jdk-17.0.10+7"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"

# AI_Cast에서 실제 Azure Speech Key 추출
$envConfigPath = "$scriptPath\..\docs\80_RawData\aicast_env_config.txt"
if (Test-Path $envConfigPath) {
    $configContent = Get-Content $envConfigPath
    $speechKeyLine = $configContent | Where-Object { $_ -match "^AZURE_SPEECH_KEY=" }
    if ($speechKeyLine) {
        $azureSpeechKey = $speechKeyLine.Split("=")[1].Trim()
    }
}

if ([string]::IsNullOrEmpty($azureSpeechKey)) {
    Write-Error "Azure Speech Key를 찾을 수 없습니다."
    exit 1
}

# 마을방송테스트문구.md 파일을 로컬 scenario.md로 복사하여 경로 문자 인코딩 에러 방지
Copy-Item "$scriptPath\..\docs\80_RawData\마을방송테스트문구.md" "$scriptPath\scenario.md" -Force

# Maven 빌드 및 의존성 복사
& "$scriptPath\..\maven\apache-maven-3.8.8\bin\mvn.cmd" compile dependency:copy-dependencies

# 실행 (경로 매개변수에 한글이 포함되지 않도록 상대 경로 사용)
& java -cp "$scriptPath\target\classes;$scriptPath\target\dependency\*" com.aicast.tool.TestDataGenerator `
    --azure.speech.key $azureSpeechKey `
    --azure.speech.region koreacentral `
    --markdown.file "scenario.md" `
    --output.dir "../docs/80_RawData/testdata"
