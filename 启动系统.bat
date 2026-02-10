@echo off
chcp 65001 >nul
echo ========================================
echo         全能扫描系统 启动中...
echo ========================================

where python >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [错误] 未检测到Python，请先安装Python 3.8以上版本
    pause
    exit /b 1
)

echo.
echo 按 Ctrl+C 停止服务
echo ========================================

REM 延迟后打开浏览器
start http://localhost:5000

REM 启动Flask
python app.py

pause
