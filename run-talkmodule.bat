@echo off
title Heronix-TalkModule Application
echo ============================================
echo    Heronix-TalkModule Application Launcher
echo ============================================
echo.

cd /d "%~dp0"

echo [1/3] Cleaning project...
call mvn clean -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Clean failed!
    pause
    exit /b 1
)
echo [OK] Clean completed.
echo.

echo [2/3] Compiling project...
call mvn compile -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)
echo [OK] Compilation completed.
echo.

echo [3/3] Starting Heronix-TalkModule...
echo.
call mvn javafx:run
