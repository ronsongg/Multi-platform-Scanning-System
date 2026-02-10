@echo off
chcp 65001 >nul
echo ========================================
echo       全能扫描系统 安装依赖
echo ========================================

where python >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [错误] 未检测到Python，请先安装Python 3.8以上版本
    echo 下载地址：https://www.python.org/downloads/
    pause
    exit /b 1
)

echo 正在安装依赖...
pip install -r requirements.txt

if %ERRORLEVEL% equ 0 (
    echo.
    echo [成功] 依赖安装完成！
    echo 请运行 启动系统.bat 启动系统
) else (
    echo.
    echo [错误] 安装失败，请检查网络连接
)

pause
