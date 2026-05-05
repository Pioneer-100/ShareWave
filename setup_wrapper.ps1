$ErrorActionPreference = 'Stop'
$gradleVersion = "8.7"
$gradleZip = "gradle-$gradleVersion-bin.zip"
$downloadUrl = "https://services.gradle.org/distributions/$gradleZip"
$tempDir = Join-Path $env:TEMP "gradle_setup"

if (!(Test-Path $tempDir)) {
    New-Item -ItemType Directory -Path $tempDir | Out-Null
}

$zipPath = Join-Path $tempDir $gradleZip
Write-Host "Downloading Gradle $gradleVersion..."
Invoke-WebRequest -Uri $downloadUrl -OutFile $zipPath

Write-Host "Extracting Gradle..."
Expand-Archive -Path $zipPath -DestinationPath $tempDir -Force

$gradleExe = Join-Path $tempDir "gradle-$gradleVersion\bin\gradle.bat"
Write-Host "Generating Gradle wrapper (gradlew)..."
& $gradleExe wrapper --gradle-version $gradleVersion --distribution-type bin

Write-Host "Cleaning up temporary files..."
Remove-Item -Recurse -Force $tempDir

Write-Host "Done! The wrapper is ready."
