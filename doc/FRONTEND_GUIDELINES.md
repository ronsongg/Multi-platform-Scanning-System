# 全能扫描系统 - 前端开发规范 (FRONTEND_GUIDELINES)

## 1. 总体原则

### 1.1 PC端前端
- **原生技术**：仅使用HTML5、CSS3、原生JavaScript（ES6+），不使用任何前端框架或构建工具
- **零外部依赖**：不引用任何CDN资源，所有资源本地化
- **极简交互**：数据管理页面，操作简单直观

### 1.2 Android端前端
- **原生Android应用**：使用Kotlin或Java开发，不使用WebView
- **Material Design**：遵循Material Design设计规范
- **竖屏优化**：针对手持终端竖屏手持优化设计
- **极简交互**：扫描流程全自动化，无需手动点击

---

## 2. PC端文件结构

```
static/
├── css/
│   └── style.css           # 全局样式表
└── js/
    └── main.js             # 管理页面（index.html）的交互逻辑

templates/
└── index.html              # 管理页面模板
```

---

## 3. PC端HTML规范

### 3.1 模板引擎
- 使用Flask Jinja2模板引擎
- 仅在需要服务端注入数据时使用模板变量
- 页面交互逻辑全部由JavaScript处理

### 3.2 基础结构
```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>全能扫描系统</title>
    <link rel="stylesheet" href="/static/css/style.css">
</head>
<body>
    <!-- 页面内容 -->
    <script src="/static/js/main.js"></script>
</body>
</html>
```

---

## 4. PC端CSS规范

### 4.1 设计规范

#### 配色方案

| 用途 | 颜色 | 色值 |
|------|------|------|
| 页面背景 | 深灰 | `#1a1a2e` |
| 卡片/容器背景 | 深蓝灰 | `#16213e` |
| 普通文字 | 白色/浅灰 | `#ffffff` / `#cccccc` |
| 主要按钮 | 蓝色 | `#4a90d9` |
| 危险按钮（删除） | 红色 | `#dc3545` |
| 成功按钮（设置当前） | 绿色 | `#28a745` |
| 进度条填充 | 蓝色 | `#4a90d9` |
| 进度条背景 | 深灰 | `#333333` |

#### 字体大小

| 元素 | 大小 | 场景 |
|------|------|------|
| 页面标题 | 24-28px | 管理页面 |
| 表格/列表内容 | 14-16px | 管理页面 |
| 按钮文字 | 14-16px | 所有按钮 |

### 4.2 布局规范

#### 管理页面（index.html）
- 最大宽度：1200px，居中显示
- 适配PC和移动设备
- 使用Flexbox布局
- 数据集列表使用表格或卡片式布局

### 4.3 响应式设计
- 使用 `box-sizing: border-box` 全局设置
- 使用相对单位（%, vw, vh）配合固定最小/最大值
- 管理页面允许纵向滚动（数据集列表可能较长）

---

## 5. PC端JavaScript规范

### 5.1 编码规范
- 使用ES6+语法（const/let、箭头函数、模板字符串、async/await）
- 使用原生 `fetch()` API发送HTTP请求
- 使用 `addEventListener` 绑定事件
- 不使用jQuery或任何JS库

### 5.2 API调用封装

```javascript
// 统一的API调用方式
async function apiCall(url, options = {}) {
    try {
        const response = await fetch(url, options);
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        return await response.json();
    } catch (error) {
        console.error('API调用失败:', error);
        throw error;
    }
}
```

### 5.3 main.js（管理页面逻辑）

**职责：**
- 页面加载时获取并渲染数据集列表
- 页面加载时显示连接二维码
- 实时更新扫描记录列表
- 处理Excel文件上传（表单提交）
- 处理数据集删除（确认对话框 + API调用）
- 处理设置当前数据集
- 处理Excel导出下载

**核心函数：**

| 函数名 | 功能 |
|--------|------|
| `initPage()` | 初始化：加载二维码、加载数据集列表、启动定时刷新 |
| `loadQrcode()` | 加载并显示连接二维码 |
| `loadDatasets()` | 调用 GET /api/datasets，渲染数据集列表 |
| `uploadFile()` | 调用 POST /api/upload，上传Excel |
| `deleteDataset(id, name)` | 确认后调用 DELETE /api/datasets/{id} |
| `setCurrentDataset(id)` | 调用 POST /api/set-current-dataset |
| `exportDataset(id)` | 触发 GET /api/export/{id} 文件下载 |
| `loadRecentRecords()` | 调用 GET /api/scan/recent，渲染扫描记录列表 |
| `startAutoRefresh()` | 启动定时刷新（每5秒刷新扫描记录） |

---

## 6. Android端开发规范

### 6.1 项目结构

```
app/
├── src/main/
│   ├── java/com/scanner/
│   │   ├── data/
│   │   │   ├── AppDatabase.java          # Room数据库
│   │   │   ├── ScanRecord.java          # 扫描记录实体
│   │   │   ├── ScanRecordDao.java       # 数据访问对象
│   │   │   └── ServerConfig.java        # 服务器配置
│   │   ├── network/
│   │   │   ├── ApiClient.java           # Retrofit客户端
│   │   │   ├── ScanApi.java             # API接口定义
│   │   │   └── models/
│   │   │       ├── ScanRequest.java     # 扫描请求模型
│   │   │       ├── ScanResponse.java    # 扫描响应模型
│   │   │       ├── Dataset.java         # 数据集模型
│   │   │       └── Progress.java        # 进度模型
│   │   ├── ui/
│   │   │   ├── ConnectionActivity.java  # 连接页面
│   │   │   ├── ScanActivity.java       # 扫描页面
│   │   │   └── adapters/
│   │   │       └── HistoryAdapter.java # 历史记录适配器
│   │   ├── utils/
│   │   │   ├── TTSHelper.java         # 语音播报工具
│   │   │   ├── SoundHelper.java       # 音效播放工具
│   │   │   └── ScanReceiver.java      # 扫描头接收器
│   │   └── MainActivity.java
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_connection.xml
│   │   │   └── activity_scan.xml
│   │   ├── values/
│   │   │   ├── colors.xml
│   │   │   ├── strings.xml
│   │   │   └── themes.xml
│   │   └── drawable/
│   └── AndroidManifest.xml
└── build.gradle
```

### 6.2 UI设计规范

#### 配色方案

| 用途 | 颜色 | 色值 |
|------|------|------|
| 页面背景 | 深灰 | `#1a1a2e` |
| 卡片/容器背景 | 深蓝灰 | `#16213e` |
| 正常扫描-分区文字 | 亮绿色 | `#00ff88` |
| 重复扫描-分区文字 | 亮黄色 | `#ffdd00` |
| 未找到-背景 | 红色 | `#dc3545` |
| 普通文字 | 白色/浅灰 | `#ffffff` / `#cccccc` |
| 主要按钮 | 蓝色 | `#4a90d9` |
| 进度条填充 | 蓝色 | `#4a90d9` |
| 进度条背景 | 深灰 | `#333333` |

#### 字体大小（sp单位）

| 元素 | 大小 | 场景 |
|------|------|------|
| 分区号（扫描结果） | 96sp, bold | 扫描页面核心区域 |
| 门店地址 | 18sp | 扫描页面辅助信息 |
| 状态标签（已扫描） | 16sp | 重复扫描提示 |
| 进度文字 | 16sp | 进度区 |
| 历史记录 | 13-14sp | 扫描页面底部 |
| 页面标题 | 24sp | 两个页面 |
| 按钮文字 | 16sp | 所有按钮 |

### 6.3 连接页面（ConnectionActivity）

#### 6.3.1 页面布局

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/bg_primary"
    android:padding="24dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/app_name"
        android:textSize="24sp"
        android:textColor="@color/text_primary"
        android:layout_gravity="center"
        android:layout_marginTop="48dp" />

    <Button
        android:id="@+id/btn_scan_qr"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:text="@string/scan_qr_connect"
        android:textSize="16sp"
        android:layout_marginTop="48dp" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/or"
        android:textSize="16sp"
        android:textColor="@color/text_secondary"
        android:layout_gravity="center"
        android:layout_marginTop="24dp" />

    <EditText
        android:id="@+id/et_server_url"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:hint="@string/server_url_hint"
        android:textColor="@color/text_primary"
        android:textColorHint="@color/text_secondary"
        android:background="@drawable/input_bg"
        android:padding="16dp"
        android:layout_marginTop="24dp" />

    <Button
        android:id="@+id/btn_connect"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:text="@string/connect"
        android:textSize="16sp"
        android:layout_marginTop="16dp" />

    <TextView
        android:id="@+id/tv_connection_status"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/not_connected"
        android:textSize="14sp"
        android:textColor="@color/text_secondary"
        android:layout_gravity="center"
        android:layout_marginTop="24dp" />

</LinearLayout>
```

#### 6.3.2 功能实现

**核心功能：**
1. 扫码连接：使用ZXing库扫描二维码
2. 手动连接：输入服务器地址并连接
3. 连接状态显示：实时显示连接状态
4. 断线重连：网络中断后自动重连
5. 保存服务器地址：连接成功后保存到本地

### 6.4 扫描页面（ScanActivity）

#### 6.4.1 页面布局

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/bg_primary">

    <!-- 顶部栏 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:padding="16dp"
        android:background="@color/bg_card">

        <Button
            android:id="@+id/btn_back"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/back"
            android:textSize="14sp"
            android:background="?attr/selectableItemBackgroundBorderless" />

        <TextView
            android:id="@+id/tv_dataset_name"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/dataset_name"
            android:textSize="18sp"
            android:textColor="@color/text_primary"
            android:gravity="center" />

    </LinearLayout>

    <!-- 进度区 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp"
        android:background="@color/bg_card">

        <ProgressBar
            android:id="@+id/progress_bar"
            style="@style/Widget.AppCompat.ProgressBar.Horizontal"
            android:layout_width="match_parent"
            android:layout_height="8dp"
            android:progress="5"
            android:max="100"
            android:progressTint="@color/primary" />

        <TextView
            android:id="@+id/tv_progress"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/progress_text"
            android:textSize="16sp"
            android:textColor="@color/text_primary"
            android:layout_marginTop="8dp" />

    </LinearLayout>

    <!-- 输入区 -->
    <EditText
        android:id="@+id/et_scan_input"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:hint="@string/waiting_scan"
        android:textColor="@color/text_primary"
        android:textColorHint="@color/text_secondary"
        android:background="@color/bg_card"
        android:padding="16dp"
        android:layout_margin="16dp" />

    <!-- 结果区（核心） -->
    <LinearLayout
        android:id="@+id/layout_result"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:orientation="vertical"
        android:gravity="center"
        android:padding="16dp">

        <TextView
            android:id="@+id/tv_zone"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/please_scan"
            android:textSize="96sp"
            android:textStyle="bold"
            android:textColor="@color/text_secondary" />

        <TextView
            android:id="@+id/tv_store"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/store_address"
            android:textSize="18sp"
            android:textColor="@color/text_primary"
            android:layout_marginTop="16dp"
            android:visibility="gone" />

        <TextView
            android:id="@+id/tv_status"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/scanned"
            android:textSize="16sp"
            android:textColor="@color/warning"
            android:layout_marginTop="16dp"
            android:padding="8dp"
            android:background="@drawable/status_bg"
            android:visibility="gone" />

    </LinearLayout>

    <!-- 历史区 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp"
        android:background="@color/bg_card">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/recent_scans"
            android:textSize="14sp"
            android:textColor="@color/text_secondary"
            android:layout_marginBottom="8dp" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rv_history"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />

    </LinearLayout>

</LinearLayout>
```

#### 6.4.2 功能实现

**核心功能：**
1. 页面加载时自动聚焦输入框
2. 监听扫描头Broadcast接收扫描结果
3. 根据查询结果更新UI（分区号、门店、状态样式）
4. 触发语音播报或警告音
5. 更新进度条和统计数字
6. 维护最近5条历史记录
7. 网络中断时本地缓存扫描记录

### 6.5 语音播报实现

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

    fun playSuccess() {
        try {
            releaseMediaPlayer()
            mediaPlayer = MediaPlayer.create(context, R.raw.success_scanned)
            mediaPlayer?.setOnCompletionListener {
                releaseMediaPlayer()
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            releaseMediaPlayer()
        }
    }

    fun playError() {
        try {
            releaseMediaPlayer()
            mediaPlayer = MediaPlayer.create(context, R.raw.error_not_found)
            mediaPlayer?.setOnCompletionListener {
                releaseMediaPlayer()
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            releaseMediaPlayer()
        }
    }

    fun playDuplicate() {
        try {
            releaseMediaPlayer()
            mediaPlayer = MediaPlayer.create(context, R.raw.error_duplicate_scan)
            mediaPlayer?.setOnCompletionListener {
                releaseMediaPlayer()
            }
            mediaPlayer?.start()
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

### 6.6 警告音效播放

```kotlin
import android.media.SoundPool
import android.content.Context
import android.media.AudioAttributes

class SoundHelper(context: Context) {

    private val soundPool: SoundPool
    private var warningSoundId: Int = 0

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        // 加载警告音效（需要将音频文件放在res/raw目录）
        warningSoundId = soundPool.load(context, R.raw.warning, 1)
    }

    fun playWarning() {
        soundPool.play(warningSoundId, 1f, 1f, 0, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
```

### 6.7 扫描头集成

```kotlin
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class ScanReceiver(private val onScanResult: (String) -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val scanResult = intent?.getStringExtra("SCAN_RESULT")
        scanResult?.let { onScanResult(it) }
    }

    companion object {
        fun register(context: Context, receiver: ScanReceiver) {
            val filter = IntentFilter().apply {
                addAction("com.android.server.scannerservice.broadcast")
                // 添加其他可能的扫描头Broadcast
                addAction("android.intent.ACTION_DECODE_DATA")
                addAction("com.symbol.datawedge.api.RESULT_ACTION")
            }
            context.registerReceiver(receiver, filter)
        }

        fun unregister(context: Context, receiver: ScanReceiver) {
            context.unregisterReceiver(receiver)
        }
    }
}
```

### 6.8 网络请求实现

```kotlin
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ScanApi {

    @POST("/api/scan")
    suspend fun scanBox(@Body request: ScanRequest): ScanResponse

    @GET("/api/current-dataset")
    suspend fun getCurrentDataset(): Dataset?

    @GET("/api/scan/progress/{datasetId}")
    suspend fun getProgress(@Path("datasetId") datasetId: Int): Progress

    @GET("/api/scan/recent")
    suspend fun getRecentRecords(@Query("limit") limit: Int = 10): ScanRecordsResponse
}

class ApiClient(private val baseUrl: String) {

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ScanApi by lazy {
        retrofit.create(ScanApi::class.java)
    }
}
```

### 6.9 本地数据存储

```kotlin
import androidx.room.*

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

    @Query("DELETE FROM scan_record WHERE uploaded = 1")
    suspend fun deleteUploadedRecords()
}

@Database(entities = [ScanRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanRecordDao(): ScanRecordDao
}
```

---

## 7. Android端UI状态定义

### 7.1 三种状态的UI规格

#### 状态1：正常扫描（首次）
```
结果区背景色：默认深色 (@color/bg_primary)
分区号文字色：亮绿色 (@color/success)
分区号字体大小：96sp, bold
门店地址：显示，白色文字，18sp
状态标签：不显示
语音：播报"分区 X X"
```

#### 状态2：重复扫描
```
结果区背景色：默认深色 (@color/bg_primary)
分区号文字色：亮黄色 (@color/warning)
分区号字体大小：96sp, bold
门店地址：显示，白色文字，18sp
状态标签：显示"已扫描"标签，黄色背景
语音：播报"分区 X X"
```

#### 状态3：未找到
```
结果区背景色：红色 (@color/error)
分区号：不显示
显示文字："未找到该箱号"，白色，36sp
门店地址：不显示
状态标签：不显示
声音：播放警告音效
```

#### 状态0：初始/待扫描
```
结果区背景色：默认深色 (@color/bg_primary)
显示文字："请扫描箱号"，灰色，24sp
门店地址：不显示
状态标签：不显示
语音/声音：无
```

---

## 8. 浏览器兼容性

### 8.1 PC端目标浏览器
| 浏览器 | 版本 | 优先级 |
|--------|------|--------|
| Chrome (PC) | 80+ | 高（管理端） |
| Edge (PC) | 80+ | 高（管理端） |
| Firefox (PC) | 70+ | 中（管理端备选） |

### 8.2 PC端需要兼容的API
- `fetch()` — 所有目标浏览器均支持
- `Flexbox` — 所有目标浏览器均支持
- `ES6+ (const/let/arrow/async-await/template literals)` — 所有目标浏览器均支持

### 8.3 PC端降级策略
- 不使用任何需要polyfill的特性
- 所有功能在所有目标浏览器上均可正常使用

---

## 9. Android端兼容性

### 9.1 目标设备
| 设备类型 | 版本 | 优先级 |
|---------|------|--------|
| Android手持终端 | 5.0+ (API 21+) | 最高 |

### 9.2 需要兼容的API
- `TextToSpeech` — Android 1.0+ 完全支持
- `BroadcastReceiver` — Android 1.0+ 完全支持
- `Retrofit` — Android 4.0+ 完全支持
- `Room Database` — Android 4.1+ 完全支持
- `RecyclerView` — Android 5.0+ 完全支持

### 9.3 Android端降级策略
- 如果 `TextToSpeech` 不支持中文，语音功能静默跳过，不影响扫描主流程
- 如果扫描头Broadcast不统一，提供多种集成方案
- 不使用任何需要高版本API的特性
