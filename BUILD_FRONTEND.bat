@echo off
setlocal
cd /d "%~dp0"
where node >nul 2>nul
if errorlevel 1 goto :missing
where npm >nul 2>nul
if errorlevel 1 goto :missing
pushd frontend
if not exist node_modules (
    call npm install --no-audit --no-fund
    if errorlevel 1 goto :failed
)
call npm run build
if errorlevel 1 goto :failed
popd
echo.
echo Frontend build complete: frontend\dist
pause
exit /b 0
:failed
popd
echo.
echo Frontend build failed. Review the npm output above.
pause
exit /b 1
:missing
echo Node.js 22 LTS and npm are required. Install them from https://nodejs.org/ and retry.
pause
exit /b 1
