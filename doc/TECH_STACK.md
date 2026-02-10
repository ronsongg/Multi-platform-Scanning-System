# 全能扫描系统 - 技术栈文档 (TECH_STACK)

## 1. 技术栈总览

| 层级 | 技术 | 版本要求 | 用途 |
|------|------|---------|------|
| 运行时 | Python | 3.8+ | 服务端运行环境 |
| Web框架 | Flask | 3.x | HTTP服务、路由、API |
| 数据库 | SQLite | 内置 | 数据持久化存储 |
| Excel处理 | openpyxl | 3.x | .xlsx文件读写 |
| 二维码生成 | qrcode | 7.x | 生成连接二维码 |
| 图像处理 | Pillow | 10.x | 二维码图片处理 |
| PC端前端 | HTML/CSS/JS | 原生 | PC端管理界面 |
| 打包工具 | PyInstaller | 6.x | 打包为可执行文件 |
| 部署脚本 | Windows Batch | - | 一键安装/启动脚本 |
| Android开发 | Android SDK | API 21+ (5.0+) | 手持终端应用 |
| Android构建 | Gradle | 7.x+ | Android应用构建 |
| Android扫描 | ZXing | 4.x | 二维码扫描 |
| Android语音 | TTS API | 系统内置 | 中文语音播报 |

---

## 2. PC端技术详情

### 2.1 Python

**选择理由：**
- Windows上安装简单，官网下载安装包一键安装
- 生态丰富，Excel处理库成熟
- 语法简洁，维护成本低
- 内置SQLite支持，无需额外安装数据库

**版本要求：** Python 3.8 及以上

### 2.2 Flask

**选择理由：**
- 轻量级Web框架，核心代码少，适合单用户小型应用
- 学习曲线平缓，代码直观
- 内置开发服务器，无需额外配置Nginx/Apache
- 单文件即可实现完整Web服务

**使用方式：**
```python
from flask import Flask, request, jsonify, render_template, send_file
app = Flask(__name__)
app.run(host='0.0.0.0', port=5000)
```

**关键配置：**
- `host='0.0.0.0'`：监听所有网络接口，允许局域网内其他设备访问
- `port=5000`：默认端口
- `debug=False`：生产环境关闭调试模式

### 2.3 SQLite

**选择理由：**
- Python内置支持（`sqlite3`模块），无需额外安装
- 无需数据库服务进程，数据以单个文件存储
- 对单用户场景完全足够，读写性能优秀
- 数据文件可直接备份复制

**数据库文件位置：** `data/scanner.db`（自动创建）

**关键设置：**
```python
# 启用外键约束（SQLite默认不启用）
PRAGMA foreign_keys = ON;

# WAL模式提升并发性能（可选）
PRAGMA journal_mode = WAL;
```

### 2.4 openpyxl

**选择理由：**
- Python处理.xlsx格式的标准库
- 支持读取和创建Excel文件
- 纯Python实现，无需C扩展，安装简单
- API直观，代码可读性好

**使用场景：**
1. **导入**：读取用户上传的.xlsx文件，解析箱号/门店/分区数据
2. **导出**：生成包含扫描结果的.xlsx文件供下载

### 2.5 qrcode

**选择理由：**
- Python生成二维码的标准库
- 纯Python实现，无需外部依赖
- 支持多种二维码格式和纠错级别
- API简单易用

**使用场景：**
- 生成包含服务器地址的二维码
- 手持终端扫描后自动连接

**使用方式：**
```python
import qrcode

def generate_qr_code(server_url):
    qr = qrcode.QRCode(version=1, box_size=10, border=5)
    qr.add_data(server_url)
    qr.make(fit=True)
    img = qr.make_image(fill_color="black", back_color="white")
    return img
```

### 2.6 PC端前端技术

**原生HTML/CSS/JavaScript**

**选择理由：**
- 无需构建工具（无Webpack/Vite/npm），开发部署极简
- 无外部CDN依赖，完全内网可用，不受网络限制
- 页面轻量（总大小 < 50KB），加载速度快
- 无框架版本升级、安全补丁等维护负担

**不使用框架的理由：**
- React/Vue等框架需要构建工具链，增加部署复杂度
- PC端仅用于数据管理，逻辑不复杂，原生JS完全胜任

---

## 3. Android手持终端技术详情

### 3.1 Android SDK

**选择理由：**
- 原生Android应用，性能最优
- 完整的硬件访问能力（扫描头、TTS、震动等）
- 离线缓存能力，网络中断不影响扫描
- 更好的用户体验和响应速度

**版本要求：** Android 5.0 (API 21) 及以上

### 3.2 开发语言

**选择：Kotlin**

**选择理由：**
- Google官方推荐语言
- 语法简洁，代码量比Java少约40%
- 空指针安全，减少运行时错误
- 与Java 100%互操作，可使用现有Java库
- 现代化特性（协程、扩展函数等）

**备选：Java**
- 如团队更熟悉Java，也可使用Java开发
- 功能完全相同，仅语法差异

### 3.3 二维码扫描 - ZXing

**选择理由：**
- Android平台最成熟的二维码扫描库
- 支持多种二维码格式
- 扫描速度快，识别准确率高
- 开源免费，社区活跃

**使用方式：**
```kotlin
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.DecoratedBarcodeView

val barcodeView: DecoratedBarcodeView = findViewById(R.id.barcode_scanner)

val callback = BarcodeCallback { result ->
    val qrContent = result.text
    // 解析服务器地址并连接
}

barcodeView.decodeContinuous(callback)
```

### 3.4 语音播报 - 预录制音频文件

**选择理由：**
- 播放速度更快（无需TTS合成）
- 语音质量一致（预录制的高质量音频）
- 无需依赖系统TTS引擎
- 支持所有分区号（1-20的组合）

**音频文件命名规则：**
- 分区"1-2" → `zone_1_2.mp3`
- 分区"10-5" → `zone_10_5.mp3`
- 分区"20-9" → `zone_20_9.mp3`

**使用方式：**
```kotlin
import android.media.MediaPlayer
import android.content.Context
import com.scanner.app.R

class AudioHelper(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    fun playZone(zone: String) {
        try {
            releaseMediaPlayer()
            val resourceId = getZoneResourceId(zone)
            if (resourceId != 0) {
                mediaPlayer = MediaPlayer.create(context, resourceId)
                mediaPlayer?.setOnCompletionListener {
                    releaseMediaPlayer()
                }
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            releaseMediaPlayer()
        }
    }

    private fun getZoneResourceId(zone: String): Int {
        val parts = zone.split("-")
        if (parts.size == 2) {
            val firstPart = parts[0].trim()
            val secondPart = parts[1].trim()
            val resourceName = "zone_${firstPart}_${secondPart}"
            return context.resources.getIdentifier(resourceName, "raw", context.packageName)
        }
        return 0
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
        }
    }

    fun release() {
        releaseMediaPlayer()
    }
}
```

### 3.5 网络请求 - OkHttp

**选择理由：**
- Android最流行的HTTP客户端
- 性能优异，连接池复用
- 支持异步请求，不阻塞UI线程
- 自动重连，网络中断后自动恢复

**使用方式：**
```kotlin
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

val client = OkHttpClient()

val json = JSONObject().apply {
    put("dataset_id", 1)
    put("box_number", "ABC123")
}

val body = RequestBody.create("application/json".toMediaType(), json.toString())
val request = Request.Builder()
    .url("http://192.168.1.100:5000/api/scan")
    .post(body)
    .build()

client.newCall(request).enqueue(object : Callback {
    override fun onResponse(call: Call, response: Response) {
        val responseData = response.body?.string()
        // 处理响应
    }

    override fun onFailure(call: Call, e: IOException) {
        // 处理错误
    }
})
```

### 3.6 网络请求 - Retrofit（备选）

**选择理由：**
- 类型安全的HTTP客户端
- 自动JSON序列化/反序列化
- 支持协程，代码更简洁
- 与OkHttp无缝集成

**使用方式：**
```kotlin
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ScanApi {
    @POST("/api/scan")
    suspend fun scanBox(@Body request: ScanRequest): ScanResponse

    @GET("/api/scan/progress/{datasetId}")
    suspend fun getProgress(@Path("datasetId") datasetId: Int): ProgressResponse
}

val retrofit = Retrofit.Builder()
    .baseUrl("http://192.168.1.100:5000")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val api = retrofit.create(ScanApi::class.java)
```

### 3.7 本地数据存储 - Room Database

**选择理由：**
- Android官方推荐的数据库框架
- SQLite的抽象层，使用简单
- 支持LiveData，数据变化自动更新UI
- 支持协程，异步操作不阻塞UI

**使用场景：**
- 缓存扫描记录，网络中断后自动上传
- 存储服务器地址和连接配置

**使用方式：**
```kotlin
@Entity
data class ScanRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val boxNumber: String,
    val zone: String,
    val scannedAt: Long,
    val uploaded: Boolean = false
)

@Dao
interface ScanRecordDao {
    @Query("SELECT * FROM scan_record WHERE uploaded = 0")
    suspend fun getUnuploadedRecords(): List<ScanRecord>

    @Insert
    suspend fun insert(record: ScanRecord)

    @Update
    suspend fun update(record: ScanRecord)
}

@Database(entities = [ScanRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanRecordDao(): ScanRecordDao
}
```

### 3.8 扫描头集成

**选择方式：**
- 使用Android系统的Broadcast机制接收扫描结果
- 大多数手持终端扫描头会通过Broadcast发送扫描结果

**使用方式：**
```kotlin
private val scanReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val scanResult = intent?.getStringExtra("SCAN_RESULT")
        scanResult?.let { handleScan(it) }
    }
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val filter = IntentFilter("com.android.server.scannerservice.broadcast")
    registerReceiver(scanReceiver, filter)
}
```

---

## 4. 通信协议

### 4.1 前后端通信

| 项目 | 规格 |
|------|------|
| 协议 | HTTP（内网环境，无需HTTPS） |
| 数据格式 | JSON（API请求/响应）、multipart/form-data（文件上传） |
| PC端请求方式 | 原生 `fetch()` API |
| Android请求方式 | OkHttp 或 Retrofit |

### 4.2 API请求示例

**扫描查询请求：**
```
POST /api/scan
Content-Type: application/json

{
    "dataset_id": 1,
    "box_number": "ABC123"
}
```

**扫描查询响应（正常）：**
```json
{
    "status": "found",
    "zone": "1-2",
    "store_address": "XX路XX号XX店",
    "first_scan": true,
    "progress": {
        "scanned": 5,
        "total": 100,
        "percentage": 5
    }
}
```

**获取当前数据集请求：**
```
GET /api/current-dataset
```

**获取当前数据集响应：**
```json
{
    "id": 1,
    "name": "批次A",
    "total_count": 100
}
```

---

## 5. 开发与运行环境

### 5.1 PC端开发环境要求

| 项目 | 要求 |
|------|------|
| 操作系统 | Windows 10/11 |
| Python | 3.8+ |
| 代码编辑器 | 任意（VSCode推荐） |
| 浏览器（测试） | Chrome/Edge |

### 5.2 Android开发环境要求

| 项目 | 要求 |
|------|------|
| 操作系统 | Windows 10/11 / macOS / Linux |
| Android Studio | Arctic Fox (2020.3.1) 或更高 |
| JDK | 11 或更高 |
| Gradle | 7.0 或更高 |
| Android SDK | API 21+ (Android 5.0+) |
| 测试设备 | Android 5.0+ 真机或模拟器 |

### 5.3 生产环境要求

| 项目 | 要求 |
|------|------|
| PC端操作系统 | Windows 7+ |
| Python | 3.8+（批处理方式需加入PATH，可执行文件方式无需） |
| 网络 | 内网WiFi，电脑与手持终端同网段 |
| 防火墙 | 端口5000需放通入站规则 |
| 手持终端 | Android 5.0+，安装APK |

### 5.4 Python依赖清单

```
# requirements.txt
flask>=3.0.0
openpyxl>=3.1.0
qrcode>=7.4.0
Pillow>=10.0.0
```

### 5.5 Android依赖清单

```gradle
// app/build.gradle

dependencies {
    // 网络请求
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

    // 二维码扫描
    implementation 'com.journeyapps:zxing-android-embedded:4.3.0'
    implementation 'com.google.zxing:core:3.5.2'

    // 数据库
    implementation 'androidx.room:room-runtime:2.6.1'
    implementation 'androidx.room:room-ktx:2.6.1'
    kapt 'androidx.room:room-compiler:2.6.1'

    // 协程
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

    // JSON解析
    implementation 'com.google.code.gson:gson:2.10.1'

    // 生命周期
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
}
```

---

## 6. 部署方式

### 6.1 PC端安装流程

**方式1：批处理脚本（需要Python环境）**
```
1. 将项目文件夹复制到目标Windows电脑
2. 双击 install.bat
   → 自动检测Python是否已安装
   → 自动执行 pip install -r requirements.txt
3. 安装完成
```

**方式2：可执行文件（无需Python环境）**
```
1. 将 dist 文件夹复制到目标Windows电脑
2. 双击 全能扫描系统.exe
3. 系统自动启动，无需安装
```

### 6.2 PC端启动流程

**方式1：批处理脚本**
```
1. 双击 启动系统.bat
   → 检查Python环境和依赖
   → 启动Flask服务（host=0.0.0.0, port=5000）
   → 控制台显示服务器IP地址
   → 自动在默认浏览器中打开管理页面
   → 显示连接二维码
2. 手持终端扫描二维码，自动连接
```

**方式2：可执行文件**
```
1. 双击 全能扫描系统.exe
   → 解压临时文件
   → 启动Flask服务（host=0.0.0.0, port=5000）
   → 控制台显示服务器IP地址
   → 自动在默认浏览器中打开管理页面
   → 显示连接二维码
2. 手持终端扫描二维码，自动连接
```

### 6.3 PC端打包流程（开发环境）
```
1. 确保已安装Python和所有依赖
2. 双击 build.bat
   → 自动检查PyInstaller是否安装
   → 执行打包命令
   → 生成 dist/全能扫描系统.exe
3. 将 dist 文件夹复制到目标电脑
```

### 6.4 Android应用安装流程
```
1. 将APK文件传输到手持终端
2. 在手持终端上点击APK文件进行安装
3. 安装完成后打开应用
4. 扫描PC端显示的二维码进行连接
```

### 6.4 无需的组件
- PC端无需 Node.js / npm
- PC端无需 MySQL / PostgreSQL / MongoDB
- PC端无需 Nginx / Apache
- PC端无需 Docker
- PC端无需任何云服务
- PC端无需 SSL 证书
- Android端无需 WebView（原生应用）

---

## 7. 技术风险与应对

| 风险 | 影响 | 应对措施 |
|------|------|---------|
| 手持终端TTS不支持中文 | 无法语音播报 | 语音功能作为增强体验，静默降级不影响核心扫描功能 |
| 网络中断导致扫描记录丢失 | 数据丢失 | Android应用本地缓存扫描记录，网络恢复后自动上传 |
| 多手持终端同时扫描冲突 | 数据不一致 | 服务器端使用数据库事务和乐观锁保证数据一致性 |
| Windows防火墙阻止5000端口 | 手持终端无法访问 | start.bat中提示用户放通端口，或提供防火墙配置指引 |
| Python未加入PATH | install.bat/start.bat执行失败 | 脚本中检测并给出安装指引 |
| Android应用兼容性问题 | 部分设备无法运行 | 最低支持Android 5.0，覆盖99%+设备 |
| 扫描头Broadcast不统一 | 无法接收扫描结果 | 提供多种扫描头集成方案，支持常见设备 |

---

## 8. 性能优化

### 8.1 PC端优化
- **数据库索引**：为箱号查询创建复合索引
- **批量插入**：上传Excel时使用批量插入
- **连接池**：使用数据库连接池减少连接开销
- **缓存**：缓存当前数据集信息，减少数据库查询

### 8.2 Android端优化
- **网络请求优化**：使用OkHttp连接池，复用TCP连接
- **异步处理**：使用协程处理网络请求，不阻塞UI
- **本地缓存**：使用Room数据库缓存扫描记录
- **图片优化**：二维码图片使用WebP格式，减少传输大小
- **内存优化**：及时释放资源，避免内存泄漏

---

## 9. 安全考虑

### 9.1 PC端安全
- 文件上传大小限制（16MB）
- 文件类型验证（仅允许.xlsx）
- SQL注入防护（使用参数化查询）
- 输入验证和清理

### 9.2 Android端安全
- HTTPS证书验证（如使用HTTPS）
- 敏感数据加密存储
- 权限最小化原则
- 网络请求超时设置
