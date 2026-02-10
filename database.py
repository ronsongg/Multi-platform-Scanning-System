"""
全能扫描系统 - 数据库操作模块
SQLite数据库初始化与所有CRUD操作
"""

import sqlite3
import os

DB_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'data', 'scanner.db')


def get_db():
    """获取数据库连接（启用外键、Row工厂）"""
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute('PRAGMA foreign_keys = ON')
    conn.execute('PRAGMA journal_mode = WAL')
    return conn


def init_db():
    """初始化数据库（创建表和索引）"""
    conn = get_db()
    cursor = conn.cursor()

    cursor.executescript('''
        CREATE TABLE IF NOT EXISTS datasets (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            file_name TEXT NOT NULL,
            total_count INTEGER NOT NULL,
            scanned_count INTEGER DEFAULT 0,
            is_current INTEGER DEFAULT 0,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

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

        CREATE TABLE IF NOT EXISTS scan_records (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            dataset_id INTEGER NOT NULL,
            box_number TEXT NOT NULL,
            zone TEXT NOT NULL,
            store_address TEXT DEFAULT '',
            status TEXT DEFAULT 'normal',
            device_id TEXT,
            scanned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (dataset_id) REFERENCES datasets(id) ON DELETE CASCADE
        );

        CREATE INDEX IF NOT EXISTS idx_boxes_lookup
            ON boxes(dataset_id, box_number);

        CREATE INDEX IF NOT EXISTS idx_scan_records_time
            ON scan_records(scanned_at DESC);

        CREATE INDEX IF NOT EXISTS idx_datasets_current
            ON datasets(is_current);
    ''')

    conn.commit()
    conn.close()


# ========== Dataset Operations ==========

def create_dataset(name, file_name, total_count):
    """创建新数据集记录，返回ID"""
    conn = get_db()
    cursor = conn.execute(
        'INSERT INTO datasets (name, file_name, total_count) VALUES (?, ?, ?)',
        (name, file_name, total_count)
    )
    dataset_id = cursor.lastrowid
    conn.commit()
    conn.close()
    return dataset_id


def insert_boxes(dataset_id, boxes_data):
    """
    批量插入箱号数据
    boxes_data: [(box_number, store_address, zone), ...]
    """
    conn = get_db()
    conn.executemany(
        'INSERT INTO boxes (dataset_id, box_number, store_address, zone) VALUES (?, ?, ?, ?)',
        [(dataset_id, b[0], b[1], b[2]) for b in boxes_data]
    )
    conn.commit()
    conn.close()


def get_all_datasets():
    """获取所有数据集列表"""
    conn = get_db()
    rows = conn.execute(
        'SELECT * FROM datasets ORDER BY created_at DESC'
    ).fetchall()
    conn.close()
    return [dict(row) for row in rows]


def get_dataset(dataset_id):
    """获取单个数据集信息"""
    conn = get_db()
    row = conn.execute(
        'SELECT * FROM datasets WHERE id = ?', (dataset_id,)
    ).fetchone()
    conn.close()
    return dict(row) if row else None


def get_current_dataset():
    """获取当前扫描数据集"""
    conn = get_db()
    row = conn.execute(
        'SELECT * FROM datasets WHERE is_current = 1'
    ).fetchone()
    conn.close()
    return dict(row) if row else None


def set_current_dataset(dataset_id):
    """设置当前扫描数据集"""
    conn = get_db()
    # 先检查数据集是否存在
    row = conn.execute(
        'SELECT id FROM datasets WHERE id = ?', (dataset_id,)
    ).fetchone()
    if not row:
        conn.close()
        return False

    # 清除所有当前标记
    conn.execute('UPDATE datasets SET is_current = 0')
    # 设置新的当前数据集
    conn.execute(
        'UPDATE datasets SET is_current = 1 WHERE id = ?', (dataset_id,)
    )
    conn.commit()
    conn.close()
    return True


def delete_dataset(dataset_id):
    """删除数据集（级联删除箱号和扫描记录）"""
    conn = get_db()
    row = conn.execute(
        'SELECT id FROM datasets WHERE id = ?', (dataset_id,)
    ).fetchone()
    if not row:
        conn.close()
        return False

    conn.execute('DELETE FROM datasets WHERE id = ?', (dataset_id,))
    conn.commit()
    conn.close()
    return True


# ========== Box Operations ==========

def find_box(dataset_id, box_number):
    """查询箱号信息"""
    conn = get_db()
    row = conn.execute(
        'SELECT * FROM boxes WHERE dataset_id = ? AND box_number = ?',
        (dataset_id, box_number)
    ).fetchone()
    conn.close()
    return dict(row) if row else None


def mark_scanned(box_id, dataset_id, device_id=None):
    """标记箱号为已扫描，更新数据集计数"""
    conn = get_db()
    conn.execute(
        'UPDATE boxes SET is_scanned = 1, scanned_at = CURRENT_TIMESTAMP, device_id = ? WHERE id = ?',
        (device_id, box_id)
    )
    conn.execute(
        'UPDATE datasets SET scanned_count = scanned_count + 1 WHERE id = ?',
        (dataset_id,)
    )
    conn.commit()
    conn.close()


# ========== Scan Record Operations ==========

def add_scan_record(dataset_id, box_number, zone, store_address='', status='normal', device_id=None):
    """添加扫描记录"""
    conn = get_db()
    conn.execute(
        'INSERT INTO scan_records (dataset_id, box_number, zone, store_address, status, device_id) VALUES (?, ?, ?, ?, ?, ?)',
        (dataset_id, box_number, zone, store_address, status, device_id)
    )
    conn.commit()
    conn.close()


def get_recent_scan_records(limit=20):
    """获取最近N条扫描记录"""
    conn = get_db()
    rows = conn.execute(
        'SELECT * FROM scan_records ORDER BY scanned_at DESC LIMIT ?',
        (min(limit, 50),)
    ).fetchall()
    conn.close()
    return [dict(row) for row in rows]


# ========== Progress ==========

def get_progress(dataset_id):
    """获取扫描进度"""
    conn = get_db()
    row = conn.execute(
        'SELECT total_count, scanned_count FROM datasets WHERE id = ?',
        (dataset_id,)
    ).fetchone()
    conn.close()

    if not row:
        return {'scanned': 0, 'total': 0, 'percentage': 0}

    total = row['total_count']
    scanned = row['scanned_count']
    percentage = round((scanned / total) * 100) if total > 0 else 0

    return {
        'scanned': scanned,
        'total': total,
        'percentage': percentage
    }


# ========== Export ==========

def get_all_boxes(dataset_id):
    """获取数据集全部箱号（用于导出）"""
    conn = get_db()
    rows = conn.execute(
        'SELECT * FROM boxes WHERE dataset_id = ? ORDER BY id',
        (dataset_id,)
    ).fetchall()
    conn.close()
    return [dict(row) for row in rows]
