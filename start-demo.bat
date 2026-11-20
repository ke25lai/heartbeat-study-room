@echo off
chcp 65001 >nul
title Heartbeat Study Room - H2 Demo (Zero Config)

set "ROOT=%~dp0"

echo ============================================
echo   Heartbeat Study Room - QUICK DEMO (H2)
echo   No MySQL / No Redis required
echo ============================================
echo.

rem ---- backend (port 8090, H2 in-memory profile) ----
netstat -ano | findstr ":8090 " | findstr "LISTENING" >nul
if %errorlevel%==0 (
  echo [SKIP] Backend already running on 8090.
) else (
  echo [1/2] Building backend jar if needed ...
  cd /d "%ROOT%backend"
  if not exist "target\heartbeat-backend-1.0.0.jar" (
    call mvnw.cmd -q -DskipTests package
  )
  echo [1/2] Starting backend with H2 profile on 8090 ...
  start "Heartbeat-Backend-H2" cmd /k "java -jar target\heartbeat-backend-1.0.0.jar --spring.profiles.active=h2"
)

rem ---- frontend (port 5173) ----
netstat -ano | findstr ":5173 " | findstr "LISTENING" >nul
if %errorlevel%==0 (
  echo [SKIP] Frontend already running on 5173.
) else (
  echo [2/2] Preparing frontend ...
  cd /d "%ROOT%frontend"
  if not exist "node_modules" (
    echo Installing frontend dependencies (first time, ~1-2 min) ...
    call npm install
  )
  echo [2/2] Starting frontend on 5173 ...
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
echo   H2 console: http://localhost:8090/h2-console
echo --------------------------------------------
echo   DEMO ACCOUNTS (password: demo123)
echo     demo_zhang / demo_alin / demo_ming
echo     demo_lily  / demo_wang / demo_chen
echo   ADMIN (password: admin123)
echo     admin
echo --------------------------------------------
echo   Data is in-memory: restarting resets it.
echo   Close the two windows to stop services.
echo ============================================
echo.
pause
