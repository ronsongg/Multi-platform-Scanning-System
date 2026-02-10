@echo off
chcp 65001 >nul
title 编译Android APK

echo ========================================
echo        编译全能扫描系统 APK
echo ========================================
echo.

REM 检查是否在正确的目录
if not exist "android-scanner" (
    echo [错误] 未找到 android-scanner 目录！
    echo 请确保此批处理文件位于项目根目录
    echo.
    pause
    exit /b 1
)

REM 进入Android项目目录
cd android-scanner

REM 检查Gradle Wrapper是否存在
if not exist "gradlew.bat" (
    echo [错误] 未找到 gradlew.bat！
    echo 请确保Android项目结构完整
    echo.
    pause
    exit /b 1
)

echo [信息] 开始编译Debug版本APK...
echo.

REM 清理之前的构建
echo [1/3] 清理之前的构建...
call gradlew.bat clean
if %errorlevel% neq 0 (
    echo [错误] 清理失败！
    pause
    exit /b 1
)

REM 编译Debug版本
echo [2/3] 编译Debug版本APK...
call gradlew.bat assembleDebug
if %errorlevel% neq 0 (
    echo [错误] 编译失败！
    pause
    exit /b 1
)

REM 检查APK是否生成
if not exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo [错误] APK文件未生成！
    echo 请检查编译日志
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo        编译成功！
echo ========================================
echo.
echo APK文件位置:
echo   %CD%\app\build\outputs\apk\debug\app-debug.apk
echo.
echo 文件大小:
for %%A in ("app\build\outputs\apk\debug\app-debug.apk") do (
    set size=%%~zA
)
set /a sizeMB=%size% / 1048576
echo   %sizeMB% MB
echo.
echo ========================================
echo        下一步操作
echo ========================================
echo.
echo 1. 将APK文件传输到Android设备
echo 2. 在Android设备上安装APK
echo 3. 打开应用并扫描PC端二维码连接
echo.
echo ========================================
echo.

REM 询问是否打开输出目录
set /p openFolder=是否打开APK所在目录？(Y/N): 
if /i "%openFolder%"=="Y" (
    explorer "app\build\outputs\apk\debug"
)

REM 返回根目录
cd ..

echo.
echo 按任意键退出...
pause >nul
