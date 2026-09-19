@echo off
setlocal EnableExtensions
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..\") do set "BACKEND_DIR=%%~fI"

cd /d "%BACKEND_DIR%"
if exist "build\libs\Pharmacy-Management-Backend.jar" (
    echo [INFO] Backend: starting from JAR...
    java -jar "build\libs\Pharmacy-Management-Backend.jar"
) else (
    echo [INFO] Backend: JAR not found, starting with Gradle bootRun...
    REM Try gradlew first, if it fails use system gradle
    call gradlew.bat bootRun
    if errorlevel 1 (
        echo [INFO] Gradle wrapper failed, trying system Gradle...
        gradle bootRun
    )
)
