# 全能扫描系统 - 实施计划 (IMPLEMENTATION_PLAN)

## 总览

本文档定义了全能扫描系统的分步实施计划。共分9个阶段，每个阶段包含具体的文件操作、代码要求和验收标准。

**架构说明：**
- PC端：简单的Web服务，用于数据管理和显示连接二维码
- Android端：原生应用，用于扫描、语音播报和显示分区信息

**PC端启动方式：**
- 方式1：双击 `启动系统.bat` 批处理脚本
- 方式2：双击打包后的 `全能扫描系统.exe` 可执行文件
- 启动后自动启动Web服务和后端数据处理服务，并自动打开浏览器访问管理页面

---

## 第1步：项目初始化

### 目标
创建完整的项目目录结构和基础配置文件。

### 操作清单

#### 1.1 创建PC端目录结构
```
全能扫描系统/
├── data/               （空目录，运行时存放scanner.db）
├── static/
│   ├── css/
│   │   └── style.css   # 全局样式表
│   └── js/
│       └── main.js     # 管理页面交互逻辑
└── templates/
    └── index.html      # 管理页面模板
```

#### 1.2 创建Android项目结构
```
android-scanner/
├── app/
│   ├── src/main/
│   │   ├── java/com/scanner/
│   │   │   ├── data/
│   │   │   │   ├── AppDatabase.java
│   │   │   │   ├── ScanRecord.java
│   │   │   │   ├── ScanRecordDao.java
│   │   │   │   └── ServerConfig.java
│   │   │   ├── network/
│   │   │   │   ├── ApiClient.java
│   │   │   │   ├── ScanApi.java
│   │   │   │   └── models/
│   │   │   │       ├── ScanRequest.java
│   │   │   │       ├── ScanResponse.java
│   │   │   │       ├── Dataset.java
│   │   │   │       └── Progress.java
│   │   │   ├── ui/
│   │   │   │   ├── ConnectionActivity.java
│   │   │   │   ├── ScanActivity.java
│   │   │   │   └── adapters/
│   │   │   │       └── HistoryAdapter.java
│   │   │   ├── utils/
│   │   │   │   ├── TTSHelper.java
│   │   │   │   ├── SoundHelper.java
│   │   │   │   └── ScanReceiver.java
│   │   │   └── MainActivity.java
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_connection.xml
│   │   │   │   └── activity_scan.xml
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── drawable/
│   │   │       ├── input_bg.xml
│   │   │       └── status_bg.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle
└── build.gradle
```

#### 1.3 创建PC端 requirements.txt
```
flask>=3.0.0
openpyxl>=3.1.0
qrcode>=7.4.0
```

#### 1.4 创建 database.py
- 定义 `DB_PATH` 常量，指向 `data/scanner.db`
- 实现 `get_db()` 函数：返回SQLite连接，设置 `row_factory=sqlite3.Row`，启用外键
- 实现 `init_db()` 函数：创建 datasets、boxes、scan_records、current_dataset 表和索引（IF NOT EXISTS）
- 实现所有数据库操作函数（见 BACKEND_STRUCTURE.md 第3节）

### 验收标准
- [ ] PC端所有目录已创建
- [ ] Android项目结构已创建
- [ ] requirements.txt 包含 flask、openpyxl 和 qrcode
- [ ] database.py 可独立导入运行 `init_db()` 不报错
- [ ] 运行后 `data/scanner.db` 文件被创建且包含正确的表结构

---

## 第2步：PC端后端核心API

### 目标
实现 app.py 的所有路由和API端点。

### 操作清单

#### 2.1 创建 app.py 基础结构
```python
from flask import Flask, request, jsonify, render_template, send_file
from database import init_db, ...
import os

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024
app.config['SERVER_PORT'] = 5000
```

#### 2.2 实现页面路由
- `GET /` → 渲染 index.html，显示管理页面和二维码

#### 2.3 实现 GET /api/qrcode
**处理流程：**
1. 获取本机IP地址
2. 构建服务器URL：`http://{ip}:{port}`
3. 使用qrcode库生成二维码图片
4. 返回PNG图片

#### 2.4 实现 POST /api/upload
**处理流程：**
1. 从 `request.files` 获取文件，从 `request.form` 获取名称
2. 验证文件存在、扩展名为.xlsx、名称非空
3. 使用 openpyxl `load_workbook(file, read_only=True)` 读取
4. 读取第一个Sheet（`wb.active`）
5. 跳过第一行（表头），遍历后续行
6. 提取每行的前3列：箱号(str)、门店地址(str)、分区(str)
7. 跳过箱号为空的行
8. 验证至少有1条有效数据
9. 调用 `create_dataset()` 创建数据集记录
10. 调用 `insert_boxes()` 批量插入箱号数据
11. 返回成功JSON

#### 2.5 实现 GET /api/datasets
- 调用 `get_all_datasets()`
- 返回数据集列表JSON

#### 2.6 实现 DELETE /api/datasets/\<int:id\>
- 调用 `delete_dataset(id)`
- 返回成功/失败JSON

#### 2.7 实现 POST /api/set-current-dataset
- 从JSON body获取 `dataset_id`
- 调用 `set_current_dataset(dataset_id)`
- 返回成功JSON

#### 2.8 实现 GET /api/current-dataset
- 调用 `get_current_dataset()`
- 返回当前数据集JSON

#### 2.9 实现 POST /api/scan
**处理流程：**
1. 从JSON body获取 `dataset_id`、`box_number` 和 `device_id`
2. 调用 `find_box(dataset_id, box_number.strip())`
3. 如果找到且 `is_scanned == 0`：
   - 调用 `mark_scanned(box_id, dataset_id)`
   - 调用 `insert_scan_record(dataset_id, box_number, zone, device_id)`
   - `first_scan = True`
4. 如果找到且 `is_scanned == 1`：
   - `first_scan = False`
5. 获取最新进度 `get_progress(dataset_id)`
6. 返回响应JSON（包含status、zone、store_address、first_scan、progress）
7. 如果未找到：返回 `{"status": "not_found"}`

#### 2.10 实现 GET /api/scan/recent
- 调用 `get_recent_scan_records(limit=10)`
- 返回最近扫描记录JSON

#### 2.11 实现 GET /api/scan/progress/\<int:dataset_id\>
- 调用 `get_progress(dataset_id)`
- 返回进度JSON

#### 2.12 实现 GET /api/export/\<int:dataset_id\>
**处理流程：**
1. 获取数据集信息（用于文件名）
2. 获取该数据集所有箱号数据
3. 使用openpyxl创建工作簿
4. 写入表头：箱号、门店地址、分区、扫描状态、扫描时间
5. 写入数据行
6. 保存到BytesIO
7. 使用 `send_file()` 返回文件下载

#### 2.13 实现应用入口
```python
if __name__ == '__main__':
    os.makedirs('data', exist_ok=True)
    init_db()
    app.run(host='0.0.0.0', port=5000, debug=False)
```

### 验收标准
- [ ] `python app.py` 可成功启动服务器
- [ ] 所有API端点可通过curl/Postman测试
- [ ] GET /api/qrcode 返回有效的二维码图片
- [ ] 上传有效.xlsx文件返回成功
- [ ] 上传无效文件返回正确错误信息
- [ ] 扫描查询返回正确的分区信息
- [ ] 重复扫描返回 `first_scan: false`
- [ ] 删除数据集后箱号数据同步删除
- [ ] 导出下载的Excel包含正确内容
- [ ] 设置当前数据集后 GET /api/current-dataset 返回正确数据

---

## 第3步：PC端管理页面（index.html + main.js）

### 目标
实现完整的数据管理页面。

### 操作清单

#### 3.1 创建 templates/index.html
**页面结构：**
```html
<body>
    <div class="container">
        <!-- 标题 -->
        <h1>全能扫描系统</h1>

        <!-- 二维码区域 -->
        <div class="qrcode-section">
            <h2>连接二维码</h2>
            <img id="qrcode-img" src="/api/qrcode" alt="连接二维码">
            <p>使用Android扫描应用扫描此二维码连接</p>
        </div>

        <!-- 上传区域 -->
        <div class="upload-section">
            <h2>上传新数据集</h2>
            <form id="upload-form">
                <input type="file" accept=".xlsx" id="file-input">
                <input type="text" placeholder="输入数据集名称" id="name-input">
                <button type="submit">上传</button>
            </form>
            <div id="upload-status"></div>
        </div>

        <!-- 数据集列表 -->
        <div class="dataset-list">
            <h2>数据集列表</h2>
            <div id="datasets-container"></div>
        </div>

        <!-- 扫描记录 -->
        <div class="scan-records">
            <h2>最近扫描记录</h2>
            <div id="records-container"></div>
        </div>
    </div>
    <script src="/static/js/main.js"></script>
</body>
```

#### 3.2 创建 static/js/main.js
**实现功能：**
1. 页面加载时调用 `initPage()`
2. `initPage()`: 加载二维码、加载数据集列表、加载扫描记录、启动定时刷新
3. `loadQrcode()`: 加载并显示连接二维码
4. `loadDatasets()`: GET /api/datasets → 渲染卡片列表
5. 每个数据集卡片显示：名称、文件名、进度（已扫/总数）、创建时间
6. 每个卡片包含3个操作按钮：设置当前、导出、删除
7. 上传表单提交处理：FormData包装file和name → POST /api/upload
8. 删除确认：`confirm()` 对话框 → DELETE /api/datasets/{id}
9. 设置当前数据集：POST /api/set-current-dataset
10. 导出：触发 `window.location = /api/export/{id}` 下载
11. `loadRecentRecords()`: GET /api/scan/recent → 渲染扫描记录列表
12. `startAutoRefresh()`: 启动定时刷新（每5秒刷新扫描记录）

### 验收标准
- [ ] 页面正常加载显示
- [ ] 二维码图片正确显示
- [ ] 上传Excel文件成功后列表自动刷新
- [ ] 上传失败显示错误信息
- [ ] 数据集列表正确显示所有字段
- [ ] 删除确认对话框正常工作
- [ ] 删除成功后列表自动刷新
- [ ] 设置当前数据集成功
- [ ] 导出按钮触发文件下载
- [ ] 扫描记录列表每5秒自动刷新

---

## 第4步：Android端基础框架

### 目标
创建Android应用的基础框架和配置。

### 操作清单

#### 4.1 配置 build.gradle (Module: app)
```gradle
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.recyclerview:recyclerview:1.3.0'
    implementation 'androidx.room:room-runtime:2.5.0'
    implementation 'androidx.room:room-ktx:2.5.0'
    kapt 'androidx.room:room-compiler:2.5.0'
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.google.zxing:core:3.5.1'
    implementation 'com.journeyapps:zxing-android-embedded:4.3.0'
}
```

#### 4.2 配置 AndroidManifest.xml
添加必要的权限：
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.VIBRATE" />
```

#### 4.3 创建 colors.xml
```xml
<resources>
    <color name="bg_primary">#1a1a2e</color>
    <color name="bg_card">#16213e</color>
    <color name="text_primary">#ffffff</color>
    <color name="text_secondary">#cccccc</color>
    <color name="primary">#4a90d9</color>
    <color name="success">#00ff88</color>
    <color name="warning">#ffdd00</color>
    <color name="error">#dc3545</color>
</resources>
```

#### 4.4 创建 strings.xml
```xml
<resources>
    <string name="app_name">全能扫描</string>
    <string name="scan_qr_connect">扫描二维码连接</string>
    <string name="server_url_hint">输入服务器地址</string>
    <string name="connect">连接</string>
    <string name="not_connected">未连接</string>
    <string name="connected">已连接</string>
    <string name="back">返回</string>
    <string name="dataset_name">数据集名称</string>
    <string name="progress_text">已扫 0 / 0 (0%)</string>
    <string name="waiting_scan">等待扫描...</string>
    <string name="please_scan">请扫描箱号</string>
    <string name="store_address">门店地址</string>
    <string name="scanned">已扫描</string>
    <string name="recent_scans">最近扫描</string>
</resources>
```

#### 4.5 创建 drawable 资源
- `input_bg.xml`: 输入框背景
- `status_bg.xml`: 状态标签背景

### 验收标准
- [ ] Android项目可成功编译
- [ ] 所有依赖正确添加
- [ ] 权限配置正确
- [ ] 颜色和字符串资源正确配置

---

## 第5步：Android端数据层

### 目标
实现Android应用的数据存储和网络请求。

### 操作清单

#### 5.1 创建 AppDatabase.java
```java
@Database(entities = {ScanRecord.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ScanRecordDao scanRecordDao();

    private static AppDatabase INSTANCE;

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                "scanner.db"
            ).build();
        }
        return INSTANCE;
    }
}
```

#### 5.2 创建 ScanRecord.java
```java
@Entity
public class ScanRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String boxNumber;
    public String zone;
    public long scannedAt;
    public boolean uploaded;
}
```

#### 5.3 创建 ScanRecordDao.java
```java
@Dao
public interface ScanRecordDao {
    @Query("SELECT * FROM scan_record WHERE uploaded = 0")
    List<ScanRecord> getUnuploadedRecords();

    @Insert
    void insert(ScanRecord record);

    @Update
    void update(ScanRecord record);

    @Query("DELETE FROM scan_record WHERE uploaded = 1")
    void deleteUploadedRecords();
}
```

#### 5.4 创建网络模型类
- `ScanRequest.java`: 扫描请求模型
- `ScanResponse.java`: 扫描响应模型
- `Dataset.java`: 数据集模型
- `Progress.java`: 进度模型

#### 5.5 创建 ScanApi.java
```java
public interface ScanApi {
    @POST("/api/scan")
    Call<ScanResponse> scanBox(@Body ScanRequest request);

    @GET("/api/current-dataset")
    Call<Dataset> getCurrentDataset();

    @GET("/api/scan/progress/{datasetId}")
    Call<Progress> getProgress(@Path("datasetId") int datasetId);

    @GET("/api/scan/recent")
    Call<ScanRecordsResponse> getRecentRecords(@Query("limit") int limit);
}
```

#### 5.6 创建 ApiClient.java
```java
public class ApiClient {
    private static final String BASE_URL = "http://%s:5000";
    private static Retrofit retrofit = null;

    public static Retrofit getClient(String serverUrl) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return retrofit;
    }
}
```

### 验收标准
- [ ] Room数据库可正常创建和操作
- [ ] Retrofit客户端可正常初始化
- [ ] 所有模型类正确配置

---

## 第6步：Android端UI层

### 目标
实现Android应用的连接页面和扫描页面。

### 操作清单

#### 6.1 创建 activity_connection.xml
- 实现连接页面布局（见 FRONTEND_GUIDELINES.md 第6.3.1节）

#### 6.2 创建 ConnectionActivity.java
**核心功能：**
1. 扫码连接：使用ZXing库扫描二维码
2. 手动连接：输入服务器地址并连接
3. 连接状态显示：实时显示连接状态
4. 断线重连：网络中断后自动重连
5. 保存服务器地址：连接成功后保存到本地

#### 6.3 创建 activity_scan.xml
- 实现扫描页面布局（见 FRONTEND_GUIDELINES.md 第6.4.1节）

#### 6.4 创建 ScanActivity.java
**核心功能：**
1. 页面加载时自动聚焦输入框
2. 监听扫描头Broadcast接收扫描结果
3. 根据查询结果更新UI（分区号、门店、状态样式）
4. 触发语音播报或警告音
5. 更新进度条和统计数字
6. 维护最近5条历史记录
7. 网络中断时本地缓存扫描记录

#### 6.5 创建 HistoryAdapter.java
- 实现历史记录列表适配器

### 验收标准
- [ ] 连接页面正常显示
- [ ] 扫描二维码可成功连接服务器
- [ ] 手动输入地址可成功连接
- [ ] 扫描页面正常显示
- [ ] 输入框自动聚焦
- [ ] 扫描结果正确显示

---

## 第7步：Android端工具类

### 目标
实现语音播报、音效播放和扫描头集成。

### 操作清单

#### 7.1 创建 TTSHelper.java
- 实现中文语音播报（见 FRONTEND_GUIDELINES.md 第6.5节）

#### 7.2 创建 SoundHelper.java
- 实现警告音效播放（见 FRONTEND_GUIDELINES.md 第6.6节）

#### 7.3 创建 ScanReceiver.java
- 实现扫描头Broadcast接收器（见 FRONTEND_GUIDELINES.md 第6.7节）

#### 7.4 创建 warning.mp3
- 将警告音效文件放入 `res/raw/` 目录

### 验收标准
- [ ] 语音播报正常工作
- [ ] 警告音效正常播放
- [ ] 扫描头可正常接收扫描结果

---

## 第8步：PC端样式表与部署脚本

### 目标
完善CSS样式和创建部署脚本。

### 操作清单

#### 8.1 创建 static/css/style.css
**需包含的样式模块：**

1. **全局重置与基础样式**
   - `* { box-sizing: border-box; margin: 0; padding: 0; }`
   - `body { font-family: ... }`
   - 深色背景主题

2. **管理页面样式**
   - `.container` 容器（max-width: 1200px, 居中）
   - `.qrcode-section` 二维码区域
   - `.upload-section` 上传区域
   - `.dataset-card` 数据集卡片
   - `.scan-records` 扫描记录区域
   - 按钮样式（蓝色主按钮、红色危险按钮、绿色设置按钮）

#### 8.2 创建 install.bat
- 检测Python安装
- 执行 pip install -r requirements.txt
- 显示安装结果

#### 8.3 创建 start.bat
- 显示启动信息
- 获取本机IP地址
- 显示访问地址
- 启动Flask服务
- 自动打开浏览器

### 验收标准
- [ ] 管理页面在PC浏览器中美观显示
- [ ] 二维码图片正确显示
- [ ] 数据集列表正确显示
- [ ] install.bat 可正确安装依赖
- [ ] start.bat 可正确启动服务并显示IP地址

---

## 第9步：PC端打包与启动脚本

### 目标
创建一键启动的批处理脚本和打包成独立可执行文件，实现双击即可启动整个系统。

### 操作清单

#### 9.1 创建 启动系统.bat

**功能要求：**
1. 检查Python环境
2. 检查依赖是否安装
3. 启动Flask Web服务
4. 获取本机IP地址
5. 自动打开浏览器访问管理页面
6. 显示访问地址和二维码说明

**完整脚本内容：**

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

#### 9.2 创建 打包配置文件 (build.spec)

使用PyInstaller打包为独立可执行文件：

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

#### 9.3 创建 打包脚本 (build.bat)

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

#### 9.4 创建 一键安装脚本 (install.bat)

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

#### 9.5 创建 requirements.txt

```text
flask>=3.0.0
openpyxl>=3.1.0
qrcode>=7.4.0
pillow>=10.0.0
```

#### 9.6 创建 README.md

```markdown
# 全能扫描系统

## 快速启动

### 方式1：使用批处理脚本（推荐）

1. 双击 `install.bat` 安装依赖（首次使用）
2. 双击 `启动系统.bat` 启动系统
3. 浏览器会自动打开管理页面

### 方式2：使用可执行文件

1. 双击 `dist/全能扫描系统.exe` 直接运行
2. 浏览器会自动打开管理页面

## 系统要求

- Windows 7 或更高版本
- Python 3.8 或更高版本（批处理方式）
- 网络连接（用于Android设备连接）

## 使用说明

1. 启动系统后，在浏览器中打开管理页面
2. 上传Excel数据集文件
3. 使用Android扫描应用扫描页面上的二维码连接
4. 开始扫描箱号，系统会自动显示分区信息

## 停止服务

在批处理窗口中按 `Ctrl+C` 停止服务

## 技术支持

如有问题，请联系技术支持人员
```

### 验收标准
- [ ] `install.bat` 可正确安装所有依赖
- [ ] `启动系统.bat` 可正确启动Web服务
- [ ] 启动后自动打开浏览器访问管理页面
- [ ] 正确显示本机IP地址和访问地址
- [ ] `build.bat` 可正确打包为可执行文件
- [ ] 打包后的 `全能扫描系统.exe` 可独立运行
- [ ] 可执行文件无需Python环境即可运行
- [ ] 可执行文件启动后自动打开浏览器

---

## 第10步：测试与优化

### 目标
全面测试所有功能，修复问题，优化性能。

### 测试用例

#### 10.1 PC端导入测试
| # | 操作 | 预期结果 |
|---|------|---------|
| T1 | 上传有效.xlsx文件（含表头+数据行） | 成功导入，列表显示新数据集 |
| T2 | 上传空文件名（不输入名称） | 提示"请输入数据集名称" |
| T3 | 不选择文件直接上传 | 提示"请选择文件" |
| T4 | 上传.csv文件 | 提示"仅支持.xlsx格式" |
| T5 | 上传只有表头无数据的Excel | 提示"文件中没有数据" |
| T6 | 上传多个数据集 | 列表显示所有数据集 |
| T7 | 设置当前数据集 | 成功设置，Android端可获取 |

#### 10.2 Android端连接测试
| # | 操作 | 预期结果 |
|---|------|---------|
| T8 | 扫描二维码连接 | 成功连接到服务器 |
| T9 | 手动输入服务器地址连接 | 成功连接到服务器 |
| T10 | 输入错误的服务器地址 | 提示连接失败 |
| T11 | 断网后重连 | 自动重连成功 |

#### 10.3 Android端扫描测试
| # | 操作 | 预期结果 |
|---|------|---------|
| T12 | 输入存在的箱号+回车 | 绿色显示分区号，语音播报，进度+1 |
| T13 | 再次输入同一箱号+回车 | 黄色显示分区号，"已扫描"标签，语音播报，进度不变 |
| T14 | 输入不存在的箱号+回车 | 红色背景，"未找到该箱号"，警告音，进度不变 |
| T15 | 连续快速扫描多个箱号 | 每次结果正确更新，语音不重叠 |
| T16 | 扫描所有箱号 | 进度显示100% |

#### 10.4 进度与历史测试
| # | 操作 | 预期结果 |
|---|------|---------|
| T17 | 扫描3个箱号后查看进度 | 显示"已扫 3 / N (X%)" |
| T18 | 查看历史记录 | 显示最近扫描的记录，最新在上 |
| T19 | 扫描超过5个箱号 | 历史记录始终保持最近5条 |
| T20 | PC端查看扫描记录 | 显示Android端上传的扫描记录 |

#### 10.5 持久化测试
| # | 操作 | 预期结果 |
|---|------|---------|
| T21 | 扫描几个箱号后关闭Android应用 | - |
| T22 | 重新打开Android应用 | 进度保留，已扫描的箱号不会再次计数 |
| T23 | 重启Flask服务后打开Android应用 | 所有数据保留 |

#### 10.6 网络中断测试
| # | 操作 | 预期结果 |
|---|------|---------|
| T24 | 扫描时断网 | 本地缓存扫描记录 |
| T25 | 恢复网络后 | 自动上传缓存的扫描记录 |
| T26 | 断网时扫描多个箱号 | 所有记录本地缓存，恢复后全部上传 |

#### 10.7 导出测试
| # | 操作 | 预期结果 |
|---|------|---------|
| T27 | 导出未扫描的数据集 | Excel中所有箱号状态为"未扫描" |
| T28 | 导出部分扫描的数据集 | Excel中正确标注已扫/未扫和时间 |
| T29 | 导出全部扫描的数据集 | 所有箱号状态为"已扫描"且有时间 |

#### 10.8 删除测试
| # | 操作 | 预期结果 |
|---|------|---------|
| T30 | 点击删除→取消 | 数据集保留不变 |
| T31 | 点击删除→确认 | 数据集从列表消失，Android端不可访问 |

### 优化检查项
- [ ] 扫描查询响应 < 1秒
- [ ] 10000条数据的Excel导入 < 10秒
- [ ] PC端页面首次加载 < 2秒
- [ ] Android端页面首次加载 < 1秒
- [ ] 无JavaScript控制台报错
- [ ] 无网络请求404错误
- [ ] Android端无崩溃
- [ ] `install.bat` 安装依赖时间 < 2分钟
- [ ] `启动系统.bat` 启动时间 < 5秒
- [ ] 打包后的可执行文件启动时间 < 5秒
- [ ] 可执行文件大小 < 100MB

---

## 实施顺序依赖关系

```
第1步：项目初始化
  │
  ├──────────────┐
  ▼              ▼
第2步：PC端后端   第4步：Android端基础框架
  │              │
  ▼              ▼
第3步：PC端管理   第5步：Android端数据层
  │              │
  └──────┬───────┘
         ▼
第6步：Android端UI层
         │
         ▼
第7步：Android端工具类
         │
         ▼
第8步：PC端样式与部署
         │
         ▼
第9步：PC端打包与启动脚本
         │
         ▼
第10步：测试与优化
```

---

## 文件创建顺序（推荐）

### PC端文件
1. `requirements.txt`
2. `database.py`
3. `app.py`（基础结构 + API）
4. `templates/index.html`
5. `static/js/main.js`
6. `static/css/style.css`
7. `install.bat`
8. `启动系统.bat`
9. `build.spec`
10. `build.bat`
11. `README.md`

### Android端文件
1. `build.gradle` (Module: app)
2. `AndroidManifest.xml`
3. `colors.xml`
4. `strings.xml`
5. `drawable/*.xml`
6. `AppDatabase.java`
7. `ScanRecord.java`
8. `ScanRecordDao.java`
9. `ScanRequest.java`
10. `ScanResponse.java`
11. `Dataset.java`
12. `Progress.java`
13. `ScanApi.java`
14. `ApiClient.java`
15. `activity_connection.xml`
16. `ConnectionActivity.java`
17. `activity_scan.xml`
18. `ScanActivity.java`
19. `HistoryAdapter.java`
20. `TTSHelper.java`
21. `SoundHelper.java`
22. `ScanReceiver.java`
23. `MainActivity.java`
24. `res/raw/warning.mp3`

---

## 交付物清单

### PC端文件
| 文件 | 说明 | 大小估计 |
|------|------|---------|
| app.py | Flask主应用 | ~250行 |
| database.py | 数据库操作 | ~180行 |
| requirements.txt | 依赖清单 | 4行 |
| templates/index.html | 管理页面 | ~100行 |
| static/css/style.css | 样式表 | ~350行 |
| static/js/main.js | 管理页面逻辑 | ~150行 |
| install.bat | 安装脚本 | ~30行 |
| 启动系统.bat | 启动脚本 | ~80行 |
| build.spec | PyInstaller配置 | ~40行 |
| build.bat | 打包脚本 | ~30行 |
| README.md | 使用说明 | ~30行 |
| **PC端总计** | **11个文件** | **~1240行** |

### Android端文件
| 文件 | 说明 | 大小估计 |
|------|------|---------|
| AppDatabase.java | Room数据库 | ~30行 |
| ScanRecord.java | 扫描记录实体 | ~15行 |
| ScanRecordDao.java | 数据访问对象 | ~20行 |
| ScanRequest.java | 扫描请求模型 | ~10行 |
| ScanResponse.java | 扫描响应模型 | ~15行 |
| Dataset.java | 数据集模型 | ~15行 |
| Progress.java | 进度模型 | ~10行 |
| ScanApi.java | API接口定义 | ~20行 |
| ApiClient.java | Retrofit客户端 | ~25行 |
| ConnectionActivity.java | 连接页面 | ~150行 |
| ScanActivity.java | 扫描页面 | ~250行 |
| HistoryAdapter.java | 历史记录适配器 | ~50行 |
| TTSHelper.java | 语音播报工具 | ~40行 |
| SoundHelper.java | 音效播放工具 | ~30行 |
| ScanReceiver.java | 扫描头接收器 | ~25行 |
| MainActivity.java | 主Activity | ~20行 |
| activity_connection.xml | 连接页面布局 | ~60行 |
| activity_scan.xml | 扫描页面布局 | ~140行 |
| colors.xml | 颜色资源 | ~10行 |
| strings.xml | 字符串资源 | ~20行 |
| drawable/*.xml | 绘图资源 | ~30行 |
| AndroidManifest.xml | 清单文件 | ~30行 |
| build.gradle | 构建配置 | ~40行 |
| warning.mp3 | 警告音效 | ~50KB |
| **Android端总计** | **23个文件** | **~970行** |

### 总计
| 类别 | 文件数 | 代码行数 |
|------|--------|---------|
| PC端 | 11 | ~1240行 |
| Android端 | 23 | ~970行 |
| **总计** | **34个文件** | **~2210行** |
