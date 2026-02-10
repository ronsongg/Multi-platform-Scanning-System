package com.scanner.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.scanner.app.ScannerApp
import com.scanner.app.data.repository.ConnectionRepository
import kotlinx.coroutines.launch

enum class ConnectionState {
    IDLE,
    TESTING,
    SUCCESS,
    FAILED
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as ScannerApp).database
    private val connectionRepo = ConnectionRepository(db)

    private val _connectionState = MutableLiveData(ConnectionState.IDLE)
    val connectionState: LiveData<ConnectionState> = _connectionState

    private val _connectionMessage = MutableLiveData<String>()
    val connectionMessage: LiveData<String> = _connectionMessage

    private val _savedUrl = MutableLiveData<String>()
    val savedUrl: LiveData<String> = _savedUrl

    private val prefs = application.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)

    private val _playbackSpeed = MutableLiveData(1.0f)
    val playbackSpeed: LiveData<Float> = _playbackSpeed

    init {
        viewModelScope.launch {
            val config = connectionRepo.getSavedConfig()
            config?.let {
                _savedUrl.value = it.serverUrl
            }
        }
        _playbackSpeed.value = prefs.getFloat("playback_speed", 1.0f)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        prefs.edit().putFloat("playback_speed", speed).apply()
    }

    fun testConnection(url: String) {
        _connectionState.value = ConnectionState.TESTING
        viewModelScope.launch {
            val result = connectionRepo.testConnection(url)
            if (result.isSuccess) {
                _connectionState.value = ConnectionState.SUCCESS
                _connectionMessage.value = "连接成功"
            } else {
                _connectionState.value = ConnectionState.FAILED
                _connectionMessage.value = result.exceptionOrNull()?.message ?: "连接失败"
            }
        }
    }

    fun connect(url: String) {
        testConnection(url)
    }
}
