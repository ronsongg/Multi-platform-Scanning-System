package com.scanner.app.ui.scan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.scanner.app.ScannerApp
import com.scanner.app.data.local.CachedScanRecord
import com.scanner.app.data.remote.models.ScanResponse
import com.scanner.app.data.repository.ScanRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class ScanState {
    WAITING,
    FOUND,
    DUPLICATE,
    NOT_FOUND,
    ERROR
}

enum class FilterMode {
    ALL, SUCCESS, ERROR
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as ScannerApp).database
    private val scanRepo = ScanRepository(db)

    // Scan state
    private val _scanState = MutableLiveData(ScanState.WAITING)
    val scanState: LiveData<ScanState> = _scanState

    // Last scan result
    private val _lastResult = MutableLiveData<ScanResponse?>()
    val lastResult: LiveData<ScanResponse?> = _lastResult

    // Records list
    private val _records = MutableLiveData<List<CachedScanRecord>>(emptyList())
    val records: LiveData<List<CachedScanRecord>> = _records

    // Filter mode
    private val _filterMode = MutableLiveData(FilterMode.ALL)
    val filterMode: LiveData<FilterMode> = _filterMode

    // Loading state
    private val _isScanning = MutableLiveData(false)
    val isScanning: LiveData<Boolean> = _isScanning

    private var currentDatasetId: Int = -1
    private var currentServerUrl: String = ""

    fun setup(serverUrl: String, datasetId: Int) {
        currentServerUrl = serverUrl
        currentDatasetId = datasetId
        loadRecords()
    }

    fun submitScan(boxNumber: String) {
        if (boxNumber.isBlank() || currentDatasetId < 0 || currentServerUrl.isBlank()) return

        _isScanning.value = true
        viewModelScope.launch {
            val result = scanRepo.submitScan(currentServerUrl, currentDatasetId, boxNumber)
            _isScanning.value = false

            result.onSuccess { response ->
                _lastResult.value = response
                _scanState.value = when {
                    response.isNotFound -> ScanState.NOT_FOUND
                    response.isDuplicate -> ScanState.DUPLICATE
                    response.isFound -> ScanState.FOUND
                    else -> ScanState.ERROR
                }
            }.onFailure {
                _scanState.value = ScanState.ERROR
                _lastResult.value = null
            }

            // Refresh records
            loadRecords()
        }
    }

    fun setFilter(mode: FilterMode) {
        _filterMode.value = mode
        loadRecords()
    }

    private fun loadRecords() {
        if (currentDatasetId < 0) return
        viewModelScope.launch {
            val flow = when (_filterMode.value) {
                FilterMode.SUCCESS -> scanRepo.getSuccessRecords(currentDatasetId)
                FilterMode.ERROR -> scanRepo.getErrorRecords(currentDatasetId)
                else -> scanRepo.getRecentRecords(currentDatasetId)
            }
            flow.collectLatest { list ->
                _records.postValue(list)
            }
        }
    }

    fun resetState() {
        _scanState.value = ScanState.WAITING
        _lastResult.value = null
    }
}
