# 全能扫描系统 - 后端结构文档 (BACKEND_STRUCTURE)

## 1. 项目文件结构

```
全能扫描系统/
├── app.py                  # Flask主应用（路由、API、启动入口）
├── database.py             # 数据库初始化与所有数据库操作函数
├── requirements.txt        # Python依赖清单
├── start.bat               # 一键启动脚本
├── install.bat             # 一键安装依赖脚本
├── data/
│   └── scanner.db          # SQLite数据库文件（运行时自动创建）
├── static/
│   ├── css/
│   │   └── style.css
│   └── js/
│       └── main.js
└── templates/
    └── index.html
```

**说明：**
- PC端仅保留一个管理页面（index.html）
- 移除扫描页面（scan.html），扫描功能由Android原生应用实现
- 移除 scan.js，扫描逻辑由Android应用实现
- 移除 audio 目录，警告音效由Android应用实现

---

## 2. 数据库设计

### 2.1 数据库文件
- **路径**：`data/scanner.db`
- **引擎**：SQLite3
- **字符编码**：UTF-8
- **自动创建**：应用启动时自动检查并创建数据库文件和表结构

### 2.2 表结构

#### datasets 表（数据集）

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | 数据集ID |
| name | TEXT | NOT NULL | 用户自定义名称 |
| file_name | TEXT | NOT NULL | 原始上传文件名 |
| total_count | INTEGER | NOT NULL | 该数据集的总箱号数量 |
| scanned_count | INTEGER | DEFAULT 0 | 已扫描的不重复箱号数 |
| is_current | INTEGER | DEFAULT 0 | 是否为当前扫描数据集（0=否，1=是） |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

```sql
CREATE TABLE IF NOT EXISTS datasets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    file_name TEXT NOT NULL,
    total_count INTEGER NOT NULL,
    scanned_count INTEGER DEFAULT 0,
    is_current INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

#### boxes 表（箱号数据）

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | 记录ID |
| dataset_id | INTEGER | NOT NULL, FOREIGN KEY | 所属数据集ID |
| box_number | TEXT | NOT NULL | 箱号（字母+数字混合） |
| store_address | TEXT | NOT NULL | 门店地址 |
| zone | TEXT | NOT NULL | 分区号（如"1-2"） |
| is_scanned | INTEGER | DEFAULT 0 | 是否已扫描（0=未扫描，1=已扫描） |
| scanned_at | DATETIME | NULL | 扫描时间（未扫描时为NULL） |
| device_id | TEXT | NULL | 扫描设备ID（可选，用于区分不同设备） |

```sql
CREATE TABLE IF NOT EXISTS boxes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dataset_id INTEGER NOT NULL,
    box_number TEXT NOT NULL,
    store_address TEXT NOT NULL,
    zone TEXT NOT NULL,
    is_scanned INTEGER DEFAULT 0,
    scanned_at DATETIME,
    device_id TEXT,
    FOREIGN KEY (dataset_id) REFERENCES datasets(id) ON DELETE CASCADE
);
```

#### scan_records 表（扫描记录）

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | 记录ID |
| dataset_id | INTEGER | NOT NULL, FOREIGN KEY | 所属数据集ID |
| box_number | TEXT | NOT NULL | 箱号 |
| zone | TEXT | NOT NULL | 分区号 |
| device_id | TEXT | NULL | 扫描设备ID |
| scanned_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 扫描时间 |

```sql
CREATE TABLE IF NOT EXISTS scan_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dataset_id INTEGER NOT NULL,
    box_number TEXT NOT NULL,
    zone TEXT NOT NULL,
    device_id TEXT,
    scanned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (dataset_id) REFERENCES datasets(id) ON DELETE CASCADE
);
```

#### 索引

```sql
-- 箱号查询索引：加速扫描查询（核心性能优化）
CREATE INDEX IF NOT EXISTS idx_boxes_lookup ON boxes(dataset_id, box_number);

-- 扫描记录索引：加速查询最近记录
CREATE INDEX IF NOT EXISTS idx_scan_records_time ON scan_records(scanned_at DESC);

-- 当前数据集索引
CREATE INDEX IF NOT EXISTS idx_datasets_current ON datasets(is_current);
```

### 2.3 外键级联删除
- 删除datasets表中的记录时，自动级联删除boxes表和scan_records表中对应的所有数据
- 需要在每次连接时启用外键支持：`PRAGMA foreign_keys = ON`

---

## 3. database.py 模块设计

### 3.1 职责
- 数据库连接管理
- 表结构初始化
- 提供所有数据库CRUD操作函数

### 3.2 函数清单

| 函数名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| `get_db()` | 无 | sqlite3.Connection | 获取数据库连接（启用外键、Row工厂） |
| `init_db()` | 无 | 无 | 初始化数据库（创建表和索引） |
| `create_dataset(name, file_name, total_count)` | name: str, file_name: str, total_count: int | int (dataset_id) | 创建新数据集记录，返回ID |
| `insert_boxes(dataset_id, boxes_data)` | dataset_id: int, boxes_data: list[tuple] | 无 | 批量插入箱号数据 |
| `get_all_datasets()` | 无 | list[dict] | 获取所有数据集列表 |
| `get_current_dataset()` | 无 | dict 或 None | 获取当前扫描数据集 |
| `set_current_dataset(dataset_id)` | dataset_id: int | bool | 设置当前扫描数据集 |
| `get_dataset(dataset_id)` | dataset_id: int | dict 或 None | 获取单个数据集信息 |
| `delete_dataset(dataset_id)` | dataset_id: int | bool | 删除数据集（级联删除箱号） |
| `find_box(dataset_id, box_number)` | dataset_id: int, box_number: str | dict 或 None | 查询箱号信息 |
| `mark_scanned(box_id, dataset_id, device_id)` | box_id: int, dataset_id: int, device_id: str | 无 | 标记箱号为已扫描，更新扫描时间和数据集计数 |
| `add_scan_record(dataset_id, box_number, zone, device_id)` | dataset_id: int, box_number: str, zone: str, device_id: str | 无 | 添加扫描记录 |
| `get_progress(dataset_id)` | dataset_id: int | dict | 获取扫描进度 {scanned, total, percentage} |
| `get_recent_scan_records(limit=10)` | limit: int | list[dict] | 获取最近N条扫描记录 |
| `get_all_boxes(dataset_id)` | dataset_id: int | list[dict] | 获取数据集全部箱号（用于导出） |

### 3.3 数据库连接管理

```python
import sqlite3
import os

DB_PATH = os.path.join(os.path.dirname(__file__), 'data', 'scanner.db')

def get_db():
    """获取数据库连接"""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row  # 结果以字典形式访问
    conn.execute('PRAGMA foreign_keys = ON')  # 启用外键约束
    return conn
```

### 3.4 批量插入优化

```python
def insert_boxes(dataset_id, boxes_data):
    """
    批量插入箱号数据
    boxes_data: [(box_number, store_address, zone), ...]
    使用executemany提高插入性能
    """
    conn = get_db()
    conn.executemany(
        'INSERT INTO boxes (dataset_id, box_number, store_address, zone) VALUES (?, ?, ?, ?)',
        [(dataset_id, b[0], b[1], b[2]) for b in boxes_data]
    )
    conn.commit()
    conn.close()
```

---

## 4. app.py 路由与API设计

### 4.1 应用配置

```python
from flask import Flask
app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 上传文件限制16MB
```

### 4.2 页面路由

| 方法 | 路径 | 函数名 | 返回 | 说明 |
|------|------|--------|------|------|
| GET | `/` | `index()` | render_template('index.html') | 管理页面 |
| GET | `/api/qrcode` | `get_qrcode()` | image/png | 生成连接二维码 |

### 4.3 API路由

#### GET /api/qrcode — 生成连接二维码

**响应：**
- Content-Type: image/png
- 二维码图片，包含服务器地址

**实现：**
```python
import qrcode
from io import BytesIO

@app.route('/api/qrcode')
def get_qrcode():
    server_url = f"http://{get_local_ip()}:{app.config['SERVER_PORT']}"
    qr = qrcode.QRCode(version=1, box_size=10, border=5)
    qr.add_data(server_url)
    qr.make(fit=True)
    img = qr.make_image(fill_color="black", back_color="white")

    img_io = BytesIO()
    img.save(img_io, 'PNG')
    img_io.seek(0)

    return send_file(img_io, mimetype='image/png')
```

---

#### POST /api/upload — 上传Excel

**请求格式**：multipart/form-data
**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | .xlsx文件 |
| name | String | 是 | 数据集自定义名称 |

**处理逻辑：**
1. 验证文件存在且扩展名为.xlsx
2. 验证name非空
3. 使用openpyxl读取工作簿的第一个Sheet
4. 跳过第一行（表头行）
5. 遍历后续行，提取第1列（箱号）、第2列（门店地址）、第3列（分区）
6. 验证至少有1行数据
7. 在datasets表创建记录
8. 批量插入boxes表
9. 返回成功响应

**成功响应** (200):
```json
{
    "success": true,
    "message": "导入成功",
    "dataset": {
        "id": 1,
        "name": "批次A",
        "total_count": 100
    }
}
```

**失败响应** (400):
```json
{
    "success": false,
    "message": "错误描述信息"
}
```

**可能的错误信息：**
- "请选择文件"
- "仅支持.xlsx格式文件"
- "请输入数据集名称"
- "文件中没有数据"
- "文件格式不正确，每行需要至少3列数据"

---

#### GET /api/datasets — 获取数据集列表

**请求参数**：无

**响应** (200):
```json
{
    "datasets": [
        {
            "id": 1,
            "name": "批次A",
            "file_name": "data_a.xlsx",
            "total_count": 100,
            "scanned_count": 5,
            "is_current": true,
            "created_at": "2025-01-15 14:30:00"
        },
        {
            "id": 2,
            "name": "批次B",
            "file_name": "data_b.xlsx",
            "total_count": 50,
            "scanned_count": 0,
            "is_current": false,
            "created_at": "2025-01-16 09:00:00"
        }
    ]
}
```

---

#### GET /api/current-dataset — 获取当前数据集

**请求参数**：无

**响应** (200):
```json
{
    "id": 1,
    "name": "批次A",
    "total_count": 100,
    "scanned_count": 5
}
```

**响应** (404):
```json
{
    "success": false,
    "message": "未设置当前数据集"
}
```

---

#### POST /api/set-current-dataset — 设置当前数据集

**请求格式**：application/json

**请求参数**：
```json
{
    "dataset_id": 1
}
```

**处理逻辑：**
1. 验证dataset_id存在
2. 将所有数据集的is_current设置为0
3. 将指定数据集的is_current设置为1

**成功响应** (200):
```json
{
    "success": true,
    "message": "设置成功"
}
```

**失败响应** (404):
```json
{
    "success": false,
    "message": "数据集不存在"
}
```

---

#### DELETE /api/datasets/\<int:id\> — 删除数据集

**请求参数**：URL路径中的id

**处理逻辑：**
1. 删除datasets表中对应记录
2. 通过外键级联，自动删除boxes表和scan_records表中关联的所有数据

**成功响应** (200):
```json
{
    "success": true,
    "message": "删除成功"
}
```

**失败响应** (404):
```json
{
    "success": false,
    "message": "数据集不存在"
}
```

---

#### POST /api/scan — 扫描箱号查询

**请求格式**：application/json

**请求参数**：
```json
{
    "dataset_id": 1,
    "box_number": "ABC123",
    "device_id": "device_001"
}
```

**处理逻辑：**
1. 在boxes表中查询 `WHERE dataset_id = ? AND box_number = ?`
2. 如果找到：
   - 检查 `is_scanned` 字段
   - 如果 `is_scanned = 0`（首次扫描）：
     - 更新 `is_scanned = 1`，`scanned_at = 当前时间`，`device_id = 设备ID`
     - 更新 datasets 表的 `scanned_count += 1`
     - 返回 `first_scan: true`
   - 如果 `is_scanned = 1`（重复扫描）：
     - 不修改boxes表数据
     - 返回 `first_scan: false`
   - 添加扫描记录到scan_records表
3. 如果未找到：返回 `{"status": "not_found"}`

**成功响应 - 找到** (200):
```json
{
    "status": "found",
    "box_number": "ABC123",
    "zone": "1-2",
    "store_address": "XX路XX号XX店",
    "first_scan": true,
    "progress": {
        "scanned": 6,
        "total": 100,
        "percentage": 6
    }
}
```

**成功响应 - 未找到** (200):
```json
{
    "status": "not_found",
    "box_number": "XYZ999"
}
```

---

#### GET /api/scan/recent — 获取最近扫描记录

**请求参数**：
- limit: 可选，默认10，最大50

**响应** (200):
```json
{
    "records": [
        {
            "box_number": "ABC123",
            "zone": "1-2",
            "device_id": "device_001",
            "scanned_at": "2025-01-15 14:30:05"
        },
        {
            "box_number": "DEF456",
            "zone": "3-1",
            "device_id": "device_002",
            "scanned_at": "2025-01-15 14:29:58"
        }
    ]
}
```

---

#### GET /api/scan/progress/\<int:dataset_id\> — 获取扫描进度

**响应** (200):
```json
{
    "scanned": 5,
    "total": 100,
    "percentage": 5
}
```

---

#### GET /api/export/\<int:dataset_id\> — 导出Excel

**处理逻辑：**
1. 查询dataset信息获取名称
2. 查询该dataset_id下所有boxes记录
3. 使用openpyxl创建新工作簿
4. 写入表头行：箱号、门店地址、分区、扫描状态、扫描时间
5. 逐行写入数据：
   - 扫描状态：`is_scanned == 1` → "已扫描"，否则 → "未扫描"
   - 扫描时间：`scanned_at` 值或空字符串
6. 将工作簿保存到BytesIO内存流
7. 返回文件流，设置Content-Disposition触发浏览器下载

**响应：**
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Content-Disposition: `attachment; filename={数据集名称}_导出.xlsx`

---

## 5. Excel处理规范

### 5.1 导入解析规则

```python
from openpyxl import load_workbook

def parse_excel(file_stream):
    """
    解析上传的Excel文件
    返回: list of tuples [(box_number, store_address, zone), ...]
    """
    wb = load_workbook(file_stream, read_only=True)
    ws = wb.active  # 使用第一个Sheet

    boxes = []
    for i, row in enumerate(ws.iter_rows(values_only=True)):
        if i == 0:  # 跳过表头行
            continue
        if len(row) < 3:
            continue  # 跳过列数不足的行

        box_number = str(row[0]).strip() if row[0] else ''
        store_address = str(row[1]).strip() if row[1] else ''
        zone = str(row[2]).strip() if row[2] else ''

        if box_number:  # 箱号非空才添加
            boxes.append((box_number, store_address, zone))

    wb.close()
    return boxes
```

### 5.2 导出生成规则

```python
from openpyxl import Workbook
from io import BytesIO

def generate_export(boxes):
    """
    生成导出Excel
    boxes: list of dict with keys: box_number, store_address, zone, is_scanned, scanned_at
    返回: BytesIO 对象
    """
    wb = Workbook()
    ws = wb.active
    ws.title = '扫描结果'

    # 表头
    ws.append(['箱号', '门店地址', '分区', '扫描状态', '扫描时间'])

    # 数据行
    for box in boxes:
        ws.append([
            box['box_number'],
            box['store_address'],
            box['zone'],
            '已扫描' if box['is_scanned'] else '未扫描',
            box['scanned_at'] or ''
        ])

    output = BytesIO()
    wb.save(output)
    output.seek(0)
    return output
```

---

## 6. 错误处理

### 6.1 HTTP错误码使用

| 状态码 | 使用场景 |
|--------|---------|
| 200 | 成功请求（含扫描未找到的情况，通过JSON中status字段区分） |
| 400 | 请求参数错误（文件格式错误、缺少必填字段等） |
| 404 | 资源不存在（数据集ID不存在） |
| 500 | 服务器内部错误 |

### 6.2 全局异常处理

```python
@app.errorhandler(Exception)
def handle_error(error):
    return jsonify({
        'success': False,
        'message': '服务器内部错误'
    }), 500
```

---

## 7. 启动配置

### 7.1 app.py 入口

```python
import socket

def get_local_ip():
    """获取本机IP地址"""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except:
        return "127.0.0.1"

if __name__ == '__main__':
    # 确保data目录存在
    os.makedirs('data', exist_ok=True)

    # 初始化数据库
    init_db()

    # 设置服务器端口
    app.config['SERVER_PORT'] = 5000

    # 启动Flask服务
    print(f"服务器地址：http://{get_local_ip()}:{app.config['SERVER_PORT']}")
    print("手持终端请扫描二维码或输入以上地址进行连接")
    app.run(host='0.0.0.0', port=app.config['SERVER_PORT'], debug=False)
```

### 7.2 start.bat

```batch
@echo off
chcp 65001 >nul
echo ========================================
echo         全能扫描系统 启动中...
echo ========================================

REM 获取本机IP地址
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /c:"IPv4"') do (
    set IP=%%a
)
set IP=%IP: =%

echo.
echo 服务地址：http://%IP%:5000
echo 手持终端请扫描二维码或输入以上地址进行连接
echo.
echo 按 Ctrl+C 停止服务
echo ========================================

REM 延迟后打开浏览器
start http://localhost:5000

REM 启动Flask
python app.py
```

### 7.3 install.bat

```batch
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
    echo 请运行 start.bat 启动系统
) else (
    echo.
    echo [错误] 安装失败，请检查网络连接
)

pause
```

---

## 8. 性能优化要点

### 8.1 数据库性能
- **索引**：`idx_boxes_lookup` 索引覆盖 `(dataset_id, box_number)` 组合查询，确保扫描查询 O(log n) 复杂度
- **批量插入**：上传Excel时使用 `executemany()` 批量插入，而非逐条INSERT
- **连接管理**：每次请求获取新连接并在请求结束后关闭，避免连接泄漏
- **WAL模式**：启用WAL模式提升并发性能

### 8.2 API性能
- **单次查询返回完整信息**：扫描API一次返回分区、门店、进度等全部数据，Android端无需多次请求
- **进度内嵌**：扫描响应中直接包含最新进度信息，避免额外的进度查询请求
- **二维码缓存**：生成的二维码图片缓存5分钟，减少重复生成

### 8.3 文件处理
- **read_only模式**：使用openpyxl的read_only模式读取上传文件，减少内存占用
- **内存流导出**：导出Excel使用BytesIO内存流，不写入临时文件

### 8.4 并发处理
- **多设备支持**：SQLite使用WAL模式，支持多设备同时读取
- **事务处理**：扫描操作使用事务，保证数据一致性
- **乐观锁**：使用is_scanned字段实现乐观锁，避免并发冲突
