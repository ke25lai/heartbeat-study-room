@echo off
chcp 65001 >nul
title Heartbeat Study Room - Launcher

set "ROOT=%~dp0"

echo ============================================
echo   Heartbeat Online Study Room - START
echo ============================================
echo.

rem ---- backend (port 8090) ----
netstat -ano | findstr ":8090 " | findstr "LISTENING" >nul
if %errorlevel%==0 (
  echo [SKIP] Backend already running on 8090.
) else (
  echo [1/2] Starting backend on 8090 ...
  cd /d "%ROOT%backend"
  if not exist "target\heartbeat-backend-1.0.0.jar" (
    echo Building backend jar first time ...
    call mvnw.cmd -q -DskipTests package
  )
  start "Heartbeat-Backend" cmd /k "java -jar target\heartbeat-backend-1.0.0.jar"
)

rem ---- frontend (port 5173) ----
netstat -ano | findstr ":5173 " | findstr "LISTENING" >nul
if %errorlevel%==0 (
  echo [SKIP] Frontend already running on 5173.
) else (
  echo [2/2] Starting frontend on 5173 ...
  cd /d "%ROOT%frontend"
  start "Heartbeat-Frontend" cmd /k "npm run dev"
)

echo.
echo Waiting for services to warm up (~25s) ...
timeout /t 25 /nobreak >nul

start "" http://localhost:5173

echo.
echo ============================================
echo   Frontend : http://localhost:5173
echo   Backend  : http://localhost:8090
echo   H2       : http://localhost:8090/h2-console
echo ============================================
echo Two windows opened. Close them to stop services.
echo.
pause
