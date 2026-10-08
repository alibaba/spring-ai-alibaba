@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"

set "OUT_DIR=%~dp0out"
set "TARGET_DIR=%~dp0..\src\main\resources\META-INF\resources\chatui"

echo === Start build and deploy ===

echo Step 1: Cleaning target directory...
if exist "%TARGET_DIR%" (
    rd /s /q "%TARGET_DIR%"
    mkdir "%TARGET_DIR%"
    echo   Target directory cleaned
) else (
    mkdir "%TARGET_DIR%"
    echo   Created target directory
)

echo Step 2: Building static files...
echo   Cleaning .next and out directories...
if exist ".next" rd /s /q ".next"
if exist "out" rd /s /q "out"

set STATIC_EXPORT=true
echo   Running npx next build ...
call npx next build
if %ERRORLEVEL% neq 0 (
    echo ERROR: Build failed
    exit /b 1
)

if not exist "%OUT_DIR%" (
    echo ERROR: Build output directory not found
    exit /b 1
)

echo   Build complete

echo Step 3: Copying files to target directory...
xcopy /e /i /q /y "%OUT_DIR%\*" "%TARGET_DIR%"

echo.
echo === Deploy complete ===
echo   Target: %TARGET_DIR%
