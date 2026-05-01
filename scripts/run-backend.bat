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
    gradlew.bat bootRun
)
