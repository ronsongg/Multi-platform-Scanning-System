# Multi-platform Scanning System

仓库/物流分拣多平台扫描系统，由 PC 端 Flask 后端 + Web 管理界面 和 Android 手持终端原生应用组成，通过局域网通信实现扫码查询、语音播报、离线扫描、进度跟踪等功能。

## 系统架构

```
┌──────────────────────────────┐
│     Windows PC (服务端)       │
│  Flask + SQLite + Web 前端   │
└──────────┬───────────────────┘
           │ HTTP/JSON (局域网 :5000)
┌──────────▼───────────────────┐
│   Android 手持终端 (客户端)    │
│  Kotlin + Retrofit + Room    │
└──────────────────────────────┘
```

## 功能特性

### PC 端

- **数据导入** — 上传 Excel (.xlsx) 文件，自动解析箱号、门店地址、分区信息
- **实时监控** — 扫描进度跟踪（已扫/总数/百分比）、最近扫描记录展示
- **数据导出** — 将扫描结果导出为 Excel 文件，包含扫描结果和异常记录（未找到箱号）两个工作表
- **多数据集管理** — 支持多个数据集并发管理、切换与删除
- **设备连接** — 自动检测本机 IP，生成二维码供手持终端快速连接

### Android 端

- **硬件扫码集成** — 通过 BroadcastReceiver 接收手持终端内置扫码头数据，兼容 Symbol/Zebra、Honeywell、Newland 等主流品牌
- **扫描结果展示** — 大字号分区显示，颜色编码（绿色=正常，黄色=重复，红色=未找到）
- **语音播报** — 预录音频文件播报分区号和状态提示，支持 0.5x ~ 2.0x 变速播放
- **离线扫描** — 支持下载数据集到本地，断网时本地缓存扫描（Room），恢复连接后自动同步上传
- **自动重连** — 连接断开后自动尝试重新连接服务器
- **记录筛选** — 支持按全部/成功/异常筛选扫描记录
- **深色/浅色主题** — Material Design 3 风格，支持深色与浅色主题切换
- **二维码连接** — 扫描 PC 端二维码自动配置服务器地址

## 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| PC 后端 | Python / Flask | Web 框架与 REST API |
| PC 数据库 | SQLite (WAL 模式) | 支持多设备并发读写 |
| PC 前端 | HTML / CSS / JavaScript | 原生实现，零构建依赖 |
| Excel 处理 | openpyxl | .xlsx 文件读写 |
| Android 语言 | Kotlin | 协程异步处理 |
| Android 架构 | MVVM | ViewModel + LiveData |
| Android 网络 | Retrofit2 + OkHttp3 | REST 客户端 |
| Android 本地存储 | Room | 离线缓存与本地数据集 |
| Android UI | Material Design 3 | 深色/浅色主题 |
| 二维码 | qrcode (PC) / ZXing (Android) | 生成与扫描 |
| TTS 语音生成 | edge-tts | 微软 Edge 超自然语音（中文） |

## 项目结构

```
Multi-platform Scanning System/
├── app.py                  # Flask 主应用（路由 + API）
├── database.py             # SQLite 数据库操作层
├── requirements.txt        # Python 依赖
├── install.bat             # 一键安装依赖
├── 启动系统.bat             # 一键启动服务
├── templates/              # HTML 模板
│   └── index.html
├── static/                 # Web 前端资源
│   ├── css/style.css
│   └── js/main.js
├── sounds/                 # 预录语音文件（分区播报、错误提示等）
├── data/                   # SQLite 数据库存储目录
├── tools/                  # 开发工具
│   └── generate_tts.py     # TTS 语音文件批量生成脚本
├── android-scanner/        # Android 原生应用
│   └── app/src/main/
│       ├── java/com/scanner/app/
│       │   ├── ui/             # Activity + Fragment (MVVM)
│       │   ├── data/           # Repository + Retrofit + Room
│       │   └── util/           # 音频播放、扫码接收工具类
│       └── res/
│           ├── layout/         # 界面布局
│           ├── drawable/       # 背景图形
│           ├── raw/            # 预录语音文件 (mp3)
│           └── values/         # 颜色、字符串、主题
├── doc/                    # 开发文档
│   ├── PRD.md              # 产品需求文档
│   ├── TECH_STACK.md       # 技术栈说明
│   ├── APP_FLOW.md         # 用户流程图
│   ├── BACKEND_STRUCTURE.md# 后端架构
│   ├── FRONTEND_GUIDELINES.md # 前端规范
│   └── IMPLEMENTATION_PLAN.md # 实施计划
└── PC_Package/             # PC 端打包发行版
```

## 快速开始

### PC 端

**方式一：Python 环境运行**

```bash
# 1. 安装依赖
pip install -r requirements.txt

# 2. 启动服务
python app.py
```

或直接运行批处理脚本：

```bash
install.bat       # 安装依赖
启动系统.bat       # 启动服务并打开浏览器
```

**方式二：免安装运行**

使用 `PC_Package/` 目录下的打包版本，双击 `一键启动.bat` 即可运行（内含 Python 运行时）。

服务启动后自动打开浏览器访问 `http://localhost:5000`。

### Android 端

```bash
# 构建 APK
cd android-scanner
./gradlew assembleDebug
```

或在 Windows 上运行：

```bash
build_apk.bat
```

生成的 APK 位于 `android-scanner/app/build/outputs/apk/debug/`。

### 连接使用

1. 确保 PC 和手持终端在同一局域网内
2. 在 PC 端 Web 界面点击显示二维码
3. 在 Android 端设置页扫描二维码，自动完成连接配置
4. 上传 Excel 数据文件，选择数据集，开始扫描

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ping` | 连接测试 |
| GET | `/api/qrcode` | 获取连接二维码（PNG 图片） |
| POST | `/api/upload` | 上传 Excel 文件（multipart/form-data） |
| GET | `/api/datasets` | 获取数据集列表 |
| DELETE | `/api/datasets/<id>` | 删除指定数据集 |
| POST | `/api/set-current-dataset` | 设置当前活跃数据集 |
| GET | `/api/current-dataset` | 获取当前活跃数据集 |
| POST | `/api/scan` | 提交扫描记录（JSON: dataset_id, box_number, device_id） |
| GET | `/api/scan/recent` | 获取最近扫描记录 |
| GET | `/api/scan/progress/<id>` | 查询扫描进度 |
| GET | `/api/datasets/<id>/boxes` | 获取数据集所有箱号（离线同步用） |
| GET | `/api/export/<id>` | 导出数据为 Excel |

## 数据库结构

### PC 端（SQLite）

| 表 | 说明 |
|------|------|
| `datasets` | 数据集（名称、文件名、总数、已扫描数、是否当前） |
| `boxes` | 箱号（箱号、门店地址、分区、扫描状态、扫描时间、设备ID） |
| `scan_records` | 扫描记录（箱号、分区、状态[normal/duplicate/not_found]、设备ID） |

### Android 端（Room）

| 表 | 说明 |
|------|------|
| `cached_scan_records` | 本地缓存扫描记录（含上传状态） |
| `server_config` | 服务器连接配置 |
| `local_boxes` | 离线箱号数据（数据集下载缓存） |

## 离线扫描机制

系统采用**本地优先**策略：

1. **数据集下载** — 在 Android 端点击「下载」按钮，将当前数据集所有箱号缓存到本地 Room 数据库
2. **本地优先查询** — 扫描时优先查询本地缓存，命中则立即返回结果；未命中再请求服务器
3. **记录缓存** — 所有扫描记录写入本地，标记 `uploaded=false`
4. **自动上传** — 网络恢复后，后台自动将未上传记录同步到服务器

## 语音文件生成

使用 `tools/generate_tts.py` 批量生成中文语音提示文件：

```bash
pip install edge-tts
python tools/generate_tts.py
```

生成的 MP3 文件输出到 `android-scanner/app/src/main/res/raw/`，涵盖错误提示（网络失败、超时、空箱号等）和状态提示（已连接、断开、下载中等）。

## 系统要求

- **PC 端**: Windows 7/10/11，Python 3.8+（使用打包版则无需 Python）
- **Android 端**: Android 7.0 (API 24) 及以上
- **网络**: 局域网环境，端口 5000 需可访问（注意防火墙设置）

## 许可证

本项目仅供内部使用。
