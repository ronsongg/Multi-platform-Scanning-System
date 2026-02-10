# PC端启动与打包方案 (PC_LAUNCH_GUIDE)

## 概述

本文档详细说明全能扫描系统PC端的启动方式和打包方案，提供两种启动方式：
1. **批处理脚本方式**：需要Python环境，适合开发和测试
2. **可执行文件方式**：无需Python环境，适合分发和部署

---

## 方式1：批处理脚本启动

### 1.1 文件清单

| 文件名 | 说明 |
|--------|------|
| `install.bat` | 一键安装依赖脚本 |
| `启动系统.bat` | 一键启动系统脚本 |
| `requirements.txt` | Python依赖清单 |

### 1.2 使用步骤

#### 首次使用
1. 双击 `install.bat` 安装Python依赖
2. 等待安装完成（约1-2分钟）
3. 双击 `启动系统.bat` 启动系统
4. 浏览器自动打开管理页面

#### 日常使用
1. 双击 `启动系统.bat` 启动系统
2. 浏览器自动打开管理页面

### 1.3 install.bat 详细说明

**功能：**
- 检测Python环境
- 安装所有依赖包
- 显示安装结果

**完整代码：**
```batch
@echo off
chcp 65001 >nul
title 安装全能扫描系统

echo ========================================
echo        全能扫描系统 - 依赖安装
echo ========================================
echo.

REM 检查Python是否安装
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到Python环境！
    echo 请先安装Python 3.8或更高版本
    echo 下载地址: https://www.python.org/downloads/
    echo.
    echo 注意：安装时请勾选 "Add Python to PATH"
    echo.
    pause
    exit /b 1
)

echo [信息] Python版本:
python --version
echo.

echo [信息] 正在安装依赖包...
pip install -r requirements.txt

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo        安装完成！
    echo ========================================
    echo.
    echo 现在可以双击 "启动系统.bat" 启动系统
    echo.
) else (
    echo.
    echo [错误] 依赖安装失败！
    echo 请检查网络连接或手动运行: pip install -r requirements.txt
    echo.
)

pause
```

### 1.4 启动系统.bat 详细说明

**功能：**
- 检查Python环境
- 检查依赖是否安装
- 创建数据目录
- 获取本机IP地址
- 启动Flask Web服务
- 自动打开浏览器
- 显示访问地址

**完整代码：**
```batch
@echo off
chcp 65001 >nul
title 全能扫描系统

echo ========================================
echo        全能扫描系统 - 启动中...
echo ========================================
echo.

REM 检查Python是否安装
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到Python环境！
    echo 请先安装Python 3.8或更高版本
    echo 下载地址: https://www.python.org/downloads/
    echo.
    pause
    exit /b 1
)

echo [信息] Python环境检测通过
python --version
echo.

REM 检查依赖是否安装
echo [信息] 检查依赖包...
python -c "import flask" >nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] Flask未安装，正在安装依赖...
    pip install -r requirements.txt
    if %errorlevel% neq 0 (
        echo [错误] 依赖安装失败！
        pause
        exit /b 1
    )
    echo [信息] 依赖安装完成
) else (
    echo [信息] 依赖包检测通过
)
echo.

REM 创建数据目录
if not exist "data" mkdir data
echo [信息] 数据目录已准备
echo.

REM 获取本机IP地址
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr "IPv4"') do (
    set LOCAL_IP=%%a
    goto :found_ip
)
:found_ip
set LOCAL_IP=%LOCAL_IP: =%
echo [信息] 本机IP地址: %LOCAL_IP%
echo.

REM 启动Flask服务
echo [信息] 正在启动Web服务...
echo.
echo ========================================
echo        系统启动成功！
echo ========================================
echo.
echo 访问地址:
echo   本地访问: http://localhost:5000
echo   局域网访问: http://%LOCAL_IP%:5000
echo.
echo 使用Android扫描应用扫描页面上的二维码即可连接
echo.
echo 按 Ctrl+C 可停止服务
echo ========================================
echo.

REM 延迟3秒后打开浏览器
start /min cmd /c timeout /t 3 >nul && start http://localhost:5000

REM 启动Flask应用
python app.py

pause
```

### 1.5 requirements.txt 内容

```text
flask>=3.0.0
openpyxl>=3.1.0
qrcode>=7.4.0
pillow>=10.0.0
```

---

## 方式2：可执行文件打包

### 2.1 文件清单

| 文件名 | 说明 |
|--------|------|
| `build.spec` | PyInstaller打包配置文件 |
| `build.bat` | 一键打包脚本 |
| `dist/全能扫描系统.exe` | 打包后的可执行文件 |

### 2.2 使用步骤

#### 打包步骤
1. 确保已安装Python和所有依赖
2. 双击 `build.bat` 执行打包
3. 等待打包完成（约2-5分钟）
4. 在 `dist` 文件夹中找到 `全能扫描系统.exe`

#### 部署步骤
1. 将 `dist` 文件夹复制到目标电脑
2. 双击 `全能扫描系统.exe` 启动系统
3. 浏览器自动打开管理页面

### 2.3 build.spec 详细说明

**功能：**
- 配置PyInstaller打包参数
- 包含所有必要的资源文件
- 生成独立可执行文件

**完整代码：**
```python
# -*- mode: python ; coding: utf-8 -*-

block_cipher = None

a = Analysis(
    ['app.py'],
    pathex=[],
    binaries=[],
    datas=[
        ('static', 'static'),
        ('templates', 'templates'),
        ('data', 'data'),
    ],
    hiddenimports=[
        'flask',
        'openpyxl',
        'qrcode',
        'PIL',
    ],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)

pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.zipfiles,
    a.datas,
    [],
    name='全能扫描系统',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    console=True,
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
    icon=None,
)
```

### 2.4 build.bat 详细说明

**功能：**
- 检查PyInstaller是否安装
- 执行打包命令
- 显示打包结果

**完整代码：**
```batch
@echo off
chcp 65001 >nul
title 打包全能扫描系统

echo ========================================
echo        开始打包全能扫描系统
echo ========================================
echo.

REM 检查PyInstaller是否安装
python -c "import PyInstaller" >nul 2>&1
if %errorlevel% neq 0 (
    echo [信息] 正在安装PyInstaller...
    pip install pyinstaller
    if %errorlevel% neq 0 (
        echo [错误] PyInstaller安装失败！
        pause
        exit /b 1
    )
)

echo [信息] 开始打包...
pyinstaller --clean build.spec

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo        打包成功！
    echo ========================================
    echo.
    echo 可执行文件位置: dist\全能扫描系统.exe
    echo.
    echo 可以将 dist 文件夹复制到其他电脑上直接运行
    echo.
) else (
    echo.
    echo [错误] 打包失败！
    echo.
)

pause
```

---

## 启动流程图

### 批处理方式启动流程

```
双击 "启动系统.bat"
        ↓
    检查Python环境
        ↓
    检查依赖包
        ↓
    创建数据目录
        ↓
    获取本机IP
        ↓
    启动Flask服务
        ↓
    延迟3秒
        ↓
    自动打开浏览器
        ↓
    显示管理页面
```

### 可执行文件启动流程

```
双击 "全能扫描系统.exe"
        ↓
    解压临时文件
        ↓
    启动Flask服务
        ↓
    获取本机IP
        ↓
    延迟3秒
        ↓
    自动打开浏览器
        ↓
    显示管理页面
```

---

## 系统要求

### 批处理方式
- **操作系统**：Windows 7 或更高版本
- **Python版本**：3.8 或更高版本
- **网络**：需要网络连接（用于安装依赖和Android设备连接）
- **磁盘空间**：至少 100MB

### 可执行文件方式
- **操作系统**：Windows 7 或更高版本
- **网络**：需要网络连接（用于Android设备连接）
- **磁盘空间**：至少 150MB（包含可执行文件）

---

## 常见问题

### Q1: 双击 `install.bat` 提示"未检测到Python环境"
**A:** 请先安装Python 3.8或更高版本，下载地址：https://www.python.org/downloads/  
安装时请务必勾选 "Add Python to PATH" 选项

### Q2: 依赖安装失败
**A:** 请检查网络连接，或手动运行以下命令：
```bash
pip install -r requirements.txt
```

### Q3: 启动后浏览器没有自动打开
**A:** 请手动在浏览器中访问 http://localhost:5000

### Q4: Android设备无法连接
**A:** 请检查：
1. PC和Android设备是否在同一局域网
2. 防火墙是否允许Python程序访问网络
3. 端口5000是否被其他程序占用

### Q5: 打包失败
**A:** 请检查：
1. 是否已安装所有依赖
2. 是否已安装PyInstaller
3. app.py文件是否存在
4. static和templates文件夹是否存在

### Q6: 可执行文件无法启动
**A:** 请检查：
1. Windows版本是否满足要求（Windows 7+）
2. 杀毒软件是否拦截了可执行文件
3. 是否有足够的磁盘空间

---

## 停止服务

### 批处理方式
在批处理窗口中按 `Ctrl+C` 停止服务

### 可执行文件方式
在命令行窗口中按 `Ctrl+C` 停止服务

---

## 文件目录结构

### 开发环境（批处理方式）
```
全能扫描系统/
├── app.py
├── database.py
├── requirements.txt
├── install.bat
├── 启动系统.bat
├── data/
│   └── scanner.db
├── static/
│   ├── css/
│   │   └── style.css
│   └── js/
│       └── main.js
└── templates/
    └── index.html
```

### 生产环境（可执行文件方式）
```
dist/
├── 全能扫描系统.exe
├── _internal/
│   ├── app.py
│   ├── database.py
│   ├── static/
│   ├── templates/
│   └── ...
└── data/
    └── scanner.db
```

---

## 性能指标

| 指标 | 批处理方式 | 可执行文件方式 |
|------|-----------|--------------|
| 首次启动时间 | 3-5秒 | 5-8秒 |
| 后续启动时间 | 2-3秒 | 3-5秒 |
| 内存占用 | ~50MB | ~80MB |
| 磁盘占用 | ~20MB | ~80MB |
| 需要Python | 是 | 否 |

---

## 推荐使用场景

### 批处理方式
- 开发和测试环境
- 需要频繁修改代码
- 有Python环境
- 对启动速度要求不高

### 可执行文件方式
- 生产环境
- 分发给最终用户
- 无Python环境
- 需要独立运行
- 对启动速度要求较高

---

## 安全建议

1. **防火墙设置**：确保防火墙允许Python程序或可执行文件访问网络
2. **端口安全**：默认使用5000端口，如需修改请确保端口未被占用
3. **数据备份**：定期备份 `data/scanner.db` 数据库文件
4. **访问控制**：确保只有授权设备可以访问管理页面
5. **网络隔离**：建议在内网环境中使用，避免暴露到公网

---

## 更新日志

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2026-02-10 | 初始版本，支持批处理和可执行文件两种启动方式 |

---

## 技术支持

如有问题，请联系技术支持人员或查阅以下资源：
- Python官方文档：https://docs.python.org/
- Flask官方文档：https://flask.palletsprojects.com/
- PyInstaller文档：https://pyinstaller.org/
