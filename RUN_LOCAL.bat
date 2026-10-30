@echo off
setlocal
cd /d "%~dp0"
title NIFTY LAB - Trading Research Terminal

echo ================================================
echo   NIFTY LAB - Trading Research Terminal
echo ================================================
echo.

where py >nul 2>nul
if errorlevel 1 goto :no_py

py -3.12 --version >nul 2>nul
if errorlevel 1 (
    echo ERROR: Python 3.12 was not found by the Python Launcher.
    echo Install Python 3.12 x64 from:
    echo https://www.python.org/downloads/release/python-31210/
    echo During installation, enable the Python Launcher option if offered.
    echo Then close this window and run RUN_LOCAL.bat again.
    pause
    exit /b 1
)

if exist ".venv\Scripts\python.exe" (
    .venv\Scripts\python.exe -c "import sys; raise SystemExit(0 if sys.version_info[:2] == (3,12) else 1)" >nul 2>nul
    if errorlevel 1 (
        echo Removing the existing virtual environment because it uses a different Python version...
        rmdir /s /q .venv
    )
)

if not exist ".venv\Scripts\python.exe" (
    echo [1/4] Creating a Python 3.12 virtual environment...
    py -3.12 -m venv .venv
    if errorlevel 1 goto :failed
) else (
    echo [1/4] Python 3.12 virtual environment already exists.
)

echo.
echo [2/4] Installing/checking Python dependencies. Internet is required the first time...
.venv\Scripts\python.exe -m pip install --upgrade pip
if errorlevel 1 goto :failed
.venv\Scripts\python.exe -m pip install --only-binary=:all: -r requirements.txt
if errorlevel 1 goto :failed

echo.
echo [3/4] Building the React + TypeScript terminal UI...
where node >nul 2>nul
if errorlevel 1 goto :frontend_missing
where npm >nul 2>nul
if errorlevel 1 goto :frontend_missing
pushd frontend
if not exist node_modules (
    call npm install --no-audit --no-fund
    if errorlevel 1 goto :frontend_failed
)
call npm run build
if errorlevel 1 goto :frontend_failed
popd
goto :start_server

:frontend_failed
popd
echo.
echo ERROR: The React frontend could not be built. Review the npm error above.
echo The backend and original calculation files have not been changed by this build step.
pause
exit /b 1

:frontend_missing
echo WARNING: Node.js/npm is not installed or not on PATH.
echo The original NIFTY LAB workspace will launch as a fallback, but the new React terminal shell will not be available.
echo Install Node.js 22 LTS (including npm), then run this file again to build the premium UI.

goto :start_server

:start_server
echo.
echo [4/4] Starting NIFTY LAB...
echo Keep this command window open while using the application.
echo Open http://127.0.0.1:8000 in your browser.
echo Press CTRL+C here to stop the server.
echo.
.venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8000
if errorlevel 1 goto :failed
exit /b 0

:no_py
echo ERROR: The Windows Python Launcher (py) was not found.
echo Install Python 3.12 x64 from https://www.python.org/downloads/release/python-31210/
echo Be sure the Python Launcher is included in the installation.
pause
exit /b 1

:failed
echo.
echo Setup or startup failed. Read the error above.
pause
exit /b 1
