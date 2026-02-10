/**
 * 全能扫描系统 - PC管理页面交互逻辑
 */

// ========== API Helpers ==========

async function apiCall(url, options = {}) {
    try {
        const response = await fetch(url, options);
        if (!response.ok) {
            const data = await response.json().catch(() => ({}));
            throw new Error(data.message || `HTTP ${response.status}`);
        }
        return response;
    } catch (error) {
        console.error('API调用失败:', error);
        throw error;
    }
}

async function apiJson(url, options = {}) {
    const response = await apiCall(url, options);
    return await response.json();
}

// ========== Toast Notifications ==========

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => {
        if (toast.parentNode) {
            toast.parentNode.removeChild(toast);
        }
    }, 3000);
}

// ========== Confirm Dialog ==========

function showConfirm(title, message) {
    return new Promise((resolve) => {
        const overlay = document.createElement('div');
        overlay.className = 'dialog-overlay';
        overlay.innerHTML = `
            <div class="dialog-box">
                <div class="dialog-title">${title}</div>
                <div class="dialog-message">${message}</div>
                <div class="dialog-actions">
                    <button class="btn btn-secondary" id="dialog-cancel">取消</button>
                    <button class="btn btn-primary" style="background-color: #dc3545; box-shadow: 0 4px 12px rgba(220,53,69,0.2);" id="dialog-confirm">确定</button>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);

        overlay.querySelector('#dialog-cancel').addEventListener('click', () => {
            document.body.removeChild(overlay);
            resolve(false);
        });
        overlay.querySelector('#dialog-confirm').addEventListener('click', () => {
            document.body.removeChild(overlay);
            resolve(true);
        });
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) {
                document.body.removeChild(overlay);
                resolve(false);
            }
        });
    });
}

// ========== QR Code Modal ==========

function showQrcodeModal() {
    const modal = document.getElementById('qrcode-modal');
    const img = document.getElementById('qrcode-image');
    const placeholder = document.getElementById('qrcode-placeholder');
    const serverUrl = document.getElementById('server-url');

    // Reset state
    img.style.display = 'none';
    placeholder.style.display = 'flex';
    placeholder.textContent = '加载中...';
    modal.style.display = 'flex';

    // Load QR code
    img.src = '/api/qrcode?' + Date.now();
    img.onload = () => {
        img.style.display = 'block';
        placeholder.style.display = 'none';
    };
    img.onerror = () => {
        placeholder.textContent = '二维码加载失败';
    };

    serverUrl.textContent = window.location.origin;
}

function closeQrcodeModal(event) {
    if (event && event.target !== event.currentTarget) return;
    document.getElementById('qrcode-modal').style.display = 'none';
}

// ========== Dataset List ==========

function getDateKey(datetimeStr) {
    if (!datetimeStr) return '';
    return datetimeStr.split(' ')[0]; // "2025-01-15 14:30:00" -> "2025-01-15"
}

function formatDateLabel(dateKey) {
    const today = new Date();
    const todayStr = today.getFullYear() + '-'
        + String(today.getMonth() + 1).padStart(2, '0') + '-'
        + String(today.getDate()).padStart(2, '0');

    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayStr = yesterday.getFullYear() + '-'
        + String(yesterday.getMonth() + 1).padStart(2, '0') + '-'
        + String(yesterday.getDate()).padStart(2, '0');

    if (dateKey === todayStr) return '今天';
    if (dateKey === yesterdayStr) return '昨天';
    return dateKey;
}

function renderDatasetCard(ds) {
    const percentage = ds.total_count > 0
        ? Math.round((ds.scanned_count / ds.total_count) * 100)
        : 0;
    const isComplete = percentage >= 100;
    const isCurrent = ds.is_current;

    return `
        <div class="dataset-card ${isCurrent ? 'is-current' : ''}">
            <div class="dataset-card-header">
                <div class="dataset-info">
                    <h3>${escapeHtml(ds.name)}${isCurrent ? '<span class="current-badge">当前</span>' : ''}</h3>
                    <p class="file-name">${escapeHtml(ds.file_name)}</p>
                </div>
                <div class="dataset-actions">
                    <button class="btn-action btn-success" onclick="setCurrentDataset(${ds.id})" ${isCurrent ? 'disabled' : ''}>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="5 3 19 12 5 21 5 3"/></svg>
                        设为当前
                    </button>
                    <button class="btn-action btn-export" onclick="exportDataset(${ds.id})">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                        导出
                    </button>
                    <button class="btn-action btn-danger" onclick="deleteDataset(${ds.id}, '${escapeHtml(ds.name)}')">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                        删除
                    </button>
                </div>
            </div>
            <div class="progress-section">
                <div class="progress-info">
                    <span>处理进度</span>
                    <span>${percentage}% (${ds.scanned_count}/${ds.total_count} 条)</span>
                </div>
                <div class="progress-bar-bg">
                    <div class="progress-bar-fill ${isComplete ? 'complete' : ''}" style="width: ${percentage}%"></div>
                </div>
            </div>
        </div>
    `;
}

function toggleDateGroup(dateKey) {
    const body = document.getElementById('date-group-' + dateKey);
    const arrow = document.getElementById('date-arrow-' + dateKey);
    if (!body) return;
    const collapsed = body.classList.toggle('collapsed');
    if (arrow) arrow.classList.toggle('rotated', !collapsed);
}

async function loadDatasets() {
    const container = document.getElementById('dataset-list');

    try {
        const data = await apiJson('/api/datasets');
        const datasets = data.datasets || [];

        if (datasets.length === 0) {
            container.innerHTML = '<div class="dataset-empty">暂无数据集，请上传Excel文件</div>';
            return;
        }

        // Group by date (datasets already sorted by created_at DESC)
        const groups = [];
        const groupMap = {};
        for (const ds of datasets) {
            const key = getDateKey(ds.created_at);
            if (!groupMap[key]) {
                groupMap[key] = [];
                groups.push(key);
            }
            groupMap[key].push(ds);
        }

        // Today's date key
        const today = new Date();
        const todayKey = today.getFullYear() + '-'
            + String(today.getMonth() + 1).padStart(2, '0') + '-'
            + String(today.getDate()).padStart(2, '0');

        container.innerHTML = groups.map(dateKey => {
            const isToday = dateKey === todayKey;
            const label = formatDateLabel(dateKey);
            const count = groupMap[dateKey].length;
            const cards = groupMap[dateKey].map(renderDatasetCard).join('');

            return `
                <div class="date-group">
                    <div class="date-group-header" onclick="toggleDateGroup('${dateKey}')">
                        <div class="date-group-left">
                            <svg id="date-arrow-${dateKey}" class="date-group-arrow ${isToday ? 'rotated' : ''}" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                <polyline points="9 18 15 12 9 6"/>
                            </svg>
                            <span class="date-group-label">${escapeHtml(label)}</span>
                            <span class="date-group-count">${count}</span>
                        </div>
                        <span class="date-group-date">${dateKey !== label ? dateKey : ''}</span>
                    </div>
                    <div id="date-group-${dateKey}" class="date-group-body ${isToday ? '' : 'collapsed'}">
                        ${cards}
                    </div>
                </div>
            `;
        }).join('');
    } catch (error) {
        container.innerHTML = '<div class="dataset-empty">加载失败，请刷新页面</div>';
    }
}

// ========== Upload ==========

let pendingFile = null;

function startUpload() {
    const fileInput = document.getElementById('file-input');
    fileInput.value = '';
    fileInput.click();
}

// File selected -> show name input dialog
document.getElementById('file-input').addEventListener('change', function() {
    if (!this.files.length) return;
    const file = this.files[0];

    if (!file.name.endsWith('.xlsx')) {
        showToast('仅支持.xlsx格式文件', 'error');
        this.value = '';
        return;
    }

    pendingFile = file;
    showUploadDialog(file.name);
});

function showUploadDialog(fileName) {
    const overlay = document.createElement('div');
    overlay.className = 'dialog-overlay';
    overlay.innerHTML = `
        <div class="dialog-box upload-dialog">
            <div class="dialog-title">输入车次</div>
            <p class="upload-dialog-file">${escapeHtml(fileName)}</p>
            <input type="text" id="upload-name-input" class="upload-dialog-input" placeholder="请输入车次名称..." autofocus>
            <div class="dialog-actions">
                <button class="btn btn-secondary" id="upload-dialog-cancel">取消</button>
                <button class="btn btn-primary" id="upload-dialog-confirm">确定上传</button>
            </div>
        </div>
    `;
    document.body.appendChild(overlay);

    const input = overlay.querySelector('#upload-name-input');
    const confirmBtn = overlay.querySelector('#upload-dialog-confirm');
    const cancelBtn = overlay.querySelector('#upload-dialog-cancel');

    setTimeout(() => input.focus(), 50);

    const close = () => {
        document.body.removeChild(overlay);
    };

    const confirm = () => {
        const name = input.value.trim();
        if (!name) {
            input.style.borderColor = '#dc3545';
            input.setAttribute('placeholder', '名称不能为空');
            input.focus();
            return;
        }
        close();
        doUpload(name);
    };

    cancelBtn.addEventListener('click', () => { pendingFile = null; close(); });
    confirmBtn.addEventListener('click', confirm);
    input.addEventListener('keydown', (e) => { if (e.key === 'Enter') confirm(); });
    overlay.addEventListener('click', (e) => { if (e.target === overlay) { pendingFile = null; close(); } });
}

async function doUpload(name) {
    if (!pendingFile) return;

    const uploadBtn = document.getElementById('upload-btn');
    uploadBtn.disabled = true;
    uploadBtn.innerHTML = '<span class="spinner"></span> 上传中...';

    try {
        const formData = new FormData();
        formData.append('file', pendingFile);
        formData.append('name', name);

        const data = await apiJson('/api/upload', {
            method: 'POST',
            body: formData
        });

        showToast(`导入成功: ${data.dataset.name} (${data.dataset.total_count} 条)`, 'success');
        loadDatasets();
    } catch (error) {
        showToast(error.message || '上传失败', 'error');
    } finally {
        pendingFile = null;
        document.getElementById('file-input').value = '';
        uploadBtn.disabled = false;
        uploadBtn.innerHTML = `
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
            上传文件
        `;
    }
}

// ========== Dataset Actions ==========

async function setCurrentDataset(id) {
    try {
        await apiJson('/api/set-current-dataset', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ dataset_id: id })
        });
        showToast('设置成功', 'success');
        loadDatasets();
    } catch (error) {
        showToast(error.message || '设置失败', 'error');
    }
}

function exportDataset(id) {
    window.location.href = `/api/export/${id}`;
}

async function deleteDataset(id, name) {
    const confirmed = await showConfirm(
        '确认删除',
        `确定要删除数据集 "${name}" 吗？\n所有扫描记录将被清除，此操作不可撤销。`
    );

    if (!confirmed) return;

    try {
        await apiJson(`/api/datasets/${id}`, {
            method: 'DELETE'
        });
        showToast('删除成功', 'success');
        loadDatasets();
    } catch (error) {
        showToast(error.message || '删除失败', 'error');
    }
}

// ========== Scan Records ==========

async function loadRecentRecords(limit = 20) {
    const tbody = document.getElementById('records-body');

    try {
        const data = await apiJson(`/api/scan/recent?limit=${limit}`);
        const records = data.records || [];

        if (records.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="records-empty">暂无扫描记录</td></tr>';
            return;
        }

        tbody.innerHTML = records.map(record => {
            // Determine status class based on record data
            let statusClass = 'status-normal';
            if (record.status === 'duplicate') {
                statusClass = 'status-duplicate';
            } else if (record.status === 'not_found') {
                statusClass = 'status-not-found';
            }

            // Format time (extract HH:MM:SS from datetime)
            const time = record.scanned_at
                ? record.scanned_at.split(' ').pop() || record.scanned_at
                : '';

            return `
                <tr class="${statusClass}">
                    <td class="cell-boxid">#${escapeHtml(record.box_number)}</td>
                    <td class="cell-secondary">${escapeHtml(record.zone || '-')}</td>
                    <td class="cell-secondary">${escapeHtml(record.store_address || record.device_id || '-')}</td>
                    <td class="cell-time">${escapeHtml(time)}</td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        // Silently fail on polling errors - don't clear existing data
        console.error('加载扫描记录失败:', error);
    }
}

// ========== Auto Refresh ==========

let refreshTimer = null;

function startAutoRefresh() {
    if (refreshTimer) clearInterval(refreshTimer);
    refreshTimer = setInterval(() => {
        loadRecentRecords();
        loadDatasets();
    }, 5000);
}

function stopAutoRefresh() {
    if (refreshTimer) {
        clearInterval(refreshTimer);
        refreshTimer = null;
    }
}

// ========== Utilities ==========

function escapeHtml(text) {
    if (!text) return '';
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return String(text).replace(/[&<>"']/g, (m) => map[m]);
}

// ========== Page Initialization ==========

function initPage() {
    loadDatasets();
    loadRecentRecords();
    startAutoRefresh();
}

// Start when DOM is ready
document.addEventListener('DOMContentLoaded', initPage);

// Pause refresh when page is hidden
document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
        stopAutoRefresh();
    } else {
        loadDatasets();
        loadRecentRecords();
        startAutoRefresh();
    }
});
