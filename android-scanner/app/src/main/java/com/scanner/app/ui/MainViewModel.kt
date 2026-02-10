package com.scanner.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.scanner.app.ScannerApp
import com.scanner.app.data.local.ServerConfig
import com.scanner.app.data.remote.models.DatasetInfo
import com.scanner.app.data.remote.models.ProgressInfo
import com.scanner.app.data.repository.ConnectionRepository
import com.scanner.app.data.repository.ScanRepository
import com.scanner.app.util.Constants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as ScannerApp).database
    val connectionRepo = ConnectionRepository(db)
    val scanRepo = ScanRepository(db)

    // Connection State
    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private val _serverUrl = MutableLiveData<String>()
    val serverUrl: LiveData<String> = _serverUrl

    // Active Dataset
    private val _activeDataset = MutableLiveData<DatasetInfo?>()
    val activeDataset: LiveData<DatasetInfo?> = _activeDataset

    // Progress
    private val _progress = MutableLiveData<ProgressInfo?>()
    val progress: LiveData<ProgressInfo?> = _progress

    // Pending uploads
    private val _pendingUploads = MutableLiveData(0)
    val pendingUploads: LiveData<Int> = _pendingUploads

    private var progressPollingJob: Job? = null
    private var reconnectJob: Job? = null
    private var syncJob: Job? = null

    init {
        // Load saved config
        viewModelScope.launch {
            val config = connectionRepo.getSavedConfig()
            if (config != null) {
                _serverUrl.value = config.serverUrl
                tryConnect(config.serverUrl)
            }
        }

        // Observe pending uploads
        viewModelScope.launch {
            scanRepo.getPendingUploadCount().collectLatest { count ->
                _pendingUploads.postValue(count)
            }
        }
    }

    fun tryConnect(url: String) {
        viewModelScope.launch {
            _serverUrl.value = url
            val result = connectionRepo.testConnection(url)
            if (result.isSuccess) {
                _isConnected.value = true
                _activeDataset.value = result.getOrNull()
                startProgressPolling()
                startPendingSync()
            } else {
                _isConnected.value = false
                startReconnect(url)
            }
        }
    }

    fun setActiveDataset(dataset: DatasetInfo) {
        val url = _serverUrl.value ?: return
        viewModelScope.launch {
            connectionRepo.activateDataset(url, dataset.id, dataset.name)
            _activeDataset.value = dataset
            // Reset progress for new dataset
            _progress.value = ProgressInfo(dataset.scannedCount, dataset.totalCount,
                if (dataset.totalCount > 0) (dataset.scannedCount * 100 / dataset.totalCount) else 0)
        }
    }

    fun updateProgress(progressInfo: ProgressInfo) {
        _progress.value = progressInfo
    }

    private fun startProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = viewModelScope.launch {
            while (true) {
                delay(Constants.POLL_INTERVAL_MS)
                val url = _serverUrl.value ?: continue
                val datasetId = _activeDataset.value?.id ?: continue
                val result = connectionRepo.getProgress(url, datasetId)
                if (result.isSuccess) {
                    _progress.postValue(result.getOrNull())
                    _isConnected.postValue(true)
                } else {
                    _isConnected.postValue(false)
                    startReconnect(url)
                    break
                }
            }
        }
    }

    private fun startReconnect(url: String) {
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            var attempts = 0
            while (attempts < Constants.MAX_RECONNECT_ATTEMPTS) {
                delay(Constants.RECONNECT_INTERVAL_MS)
                val result = connectionRepo.testConnection(url)
                if (result.isSuccess) {
                    _isConnected.postValue(true)
                    _activeDataset.postValue(result.getOrNull())
                    startProgressPolling()
                    startPendingSync()
                    return@launch
                }
                attempts++
            }
        }
    }

    private fun startPendingSync() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            val url = _serverUrl.value ?: return@launch
            val datasetId = _activeDataset.value?.id ?: return@launch
            scanRepo.uploadPendingRecords(url, datasetId)
        }
    }

    fun refreshConnection() {
        val url = _serverUrl.value ?: return
        tryConnect(url)
    }

    override fun onCleared() {
        super.onCleared()
        progressPollingJob?.cancel()
        reconnectJob?.cancel()
        syncJob?.cancel()
    }
}
