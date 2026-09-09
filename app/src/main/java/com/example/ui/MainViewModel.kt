package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ConnectionState
import com.example.data.model.MousePointerState
import com.example.data.model.RemoteKey
import com.example.data.model.TvBrand
import com.example.data.model.TvDevice
import com.example.data.network.TvDiscoveryManager
import com.example.data.network.TvProtocolClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val discoveryManager = TvDiscoveryManager(application.applicationContext)
  private val protocolClient = TvProtocolClient()

  val discoveredDevices: StateFlow<List<TvDevice>> = discoveryManager.discoveredDevices
  val isScanning: StateFlow<Boolean> = discoveryManager.isScanning

  private val _selectedDevice = MutableStateFlow<TvDevice?>(null)
  val selectedDevice: StateFlow<TvDevice?> = _selectedDevice.asStateFlow()

  private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
  val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

  // Remote state
  private val _isTvPoweredOn = MutableStateFlow(true)
  val isTvPoweredOn: StateFlow<Boolean> = _isTvPoweredOn.asStateFlow()

  private val _volumeLevel = MutableStateFlow(32)
  val volumeLevel: StateFlow<Int> = _volumeLevel.asStateFlow()

  private val _isMuted = MutableStateFlow(false)
  val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

  private val _currentChannel = MutableStateFlow(12)
  val currentChannel: StateFlow<Int> = _currentChannel.asStateFlow()

  // Trackpad / Mouse state
  private val _mousePointerState = MutableStateFlow(MousePointerState())
  val mousePointerState: StateFlow<MousePointerState> = _mousePointerState.asStateFlow()

  private val _isMouseModeEnabled = MutableStateFlow(true)
  val isMouseModeEnabled: StateFlow<Boolean> = _isMouseModeEnabled.asStateFlow()

  // Screen mirroring state
  private val _isMirroring = MutableStateFlow(false)
  val isMirroring: StateFlow<Boolean> = _isMirroring.asStateFlow()

  private val _mirrorResolution = MutableStateFlow("1080p FHD (60 FPS)")
  val mirrorResolution: StateFlow<String> = _mirrorResolution.asStateFlow()

  private val _mirrorLatencyMode = MutableStateFlow("فائق السرعة (ألعاب - 12ms)")
  val mirrorLatencyMode: StateFlow<String> = _mirrorLatencyMode.asStateFlow()

  private val _mirrorAudioStream = MutableStateFlow(true)
  val mirrorAudioStream: StateFlow<Boolean> = _mirrorAudioStream.asStateFlow()

  // Last action feedback banner
  private val _lastActionFeedback = MutableStateFlow<String?>("جاهز للاتصال الذكي")
  val lastActionFeedback: StateFlow<String?> = _lastActionFeedback.asStateFlow()

  private var feedbackClearJob: Job? = null

  private val vibrator: Vibrator? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      vibratorManager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }

  init {
    // Auto select first device if available
    viewModelScope.launch {
      discoveryManager.discoveredDevices.collect { list ->
        if (_selectedDevice.value == null && list.isNotEmpty()) {
          connectToDevice(list.first())
        }
      }
    }
  }

  fun scanForTvs() {
    viewModelScope.launch {
      discoveryManager.scanNetwork()
    }
  }

  fun connectToDevice(device: TvDevice) {
    viewModelScope.launch {
      _connectionState.value = ConnectionState.CONNECTING
      _selectedDevice.value = device
      _lastActionFeedback.value = "جارٍ الاتصال الذكي بـ ${device.name}..."

      // Simulate handshake & authorization protocol
      delay(700)
      _connectionState.value = ConnectionState.CONNECTED
      _lastActionFeedback.value = "متصل بنجاح مع ${device.name} عبر ${device.ipAddress}"
      triggerHaptic(50)
    }
  }

  fun disconnect() {
    _connectionState.value = ConnectionState.DISCONNECTED
    _selectedDevice.value = null
    _lastActionFeedback.value = "تم قطع الاتصال"
    triggerHaptic(30)
  }

  fun addCustomDevice(name: String, ip: String, port: Int, brand: TvBrand) {
    val newDev = discoveryManager.addCustomDevice(name, ip, port, brand)
    connectToDevice(newDev)
  }

  fun onRemoteKeyPressed(key: RemoteKey) {
    triggerHaptic(35)
    val dev = _selectedDevice.value ?: return

    when (key) {
      RemoteKey.POWER -> _isTvPoweredOn.value = !_isTvPoweredOn.value
      RemoteKey.MUTE -> _isMuted.value = !_isMuted.value
      RemoteKey.VOL_UP -> {
        _isMuted.value = false
        _volumeLevel.value = (_volumeLevel.value + 1).coerceAtMost(100)
      }
      RemoteKey.VOL_DOWN -> {
        _volumeLevel.value = (_volumeLevel.value - 1).coerceAtLeast(0)
      }
      RemoteKey.CH_UP -> _currentChannel.value = _currentChannel.value + 1
      RemoteKey.CH_DOWN -> _currentChannel.value = (_currentChannel.value - 1).coerceAtLeast(1)
      else -> {}
    }

    viewModelScope.launch {
      val result = protocolClient.sendRemoteKey(dev, key)
      showFeedback("${result.message} (${result.latencyMs} ms)")
    }
  }

  fun onMouseMoved(deltaX: Float, deltaY: Float) {
    val current = _mousePointerState.value
    val sensitivity = current.sensitivity
    val newX = (current.x + (deltaX * 0.0025f * sensitivity)).coerceIn(0.02f, 0.98f)
    val newY = (current.y + (deltaY * 0.0025f * sensitivity)).coerceIn(0.02f, 0.98f)

    _mousePointerState.value = current.copy(x = newX, y = newY)

    val dev = _selectedDevice.value
    if (dev != null) {
      viewModelScope.launch {
        protocolClient.sendMouseMovement(dev, deltaX, deltaY)
      }
    }
  }

  fun onMouseClick(isLeft: Boolean) {
    triggerHaptic(45)
    val dev = _selectedDevice.value
    if (dev != null) {
      viewModelScope.launch {
        val result = protocolClient.sendMouseClick(dev, isLeft)
        showFeedback(result.message)
      }
    }
  }

  fun toggleMouseMode(enabled: Boolean) {
    _isMouseModeEnabled.value = enabled
    _mousePointerState.value = _mousePointerState.value.copy(isVisible = enabled)
    triggerHaptic(40)
    showFeedback(if (enabled) "تم تفعيل مؤشر الماوس على التلفاز" else "تم إخفاء مؤشر الماوس")
  }

  fun setMouseSensitivity(level: Float) {
    _mousePointerState.value = _mousePointerState.value.copy(sensitivity = level)
  }

  fun launchTvApp(appName: String) {
    triggerHaptic(40)
    val dev = _selectedDevice.value ?: return
    viewModelScope.launch {
      val res = protocolClient.launchApp(dev, appName)
      showFeedback(res.message)
    }
  }

  // Screen mirroring actions
  fun toggleScreenMirroring() {
    triggerHaptic(60)
    _isMirroring.value = !_isMirroring.value
    val status = if (_isMirroring.value) "بدأ بث شاشة الهاتف للتلفاز" else "تم إيقاف بث الشاشة"
    showFeedback(status)
  }

  fun setMirrorResolution(res: String) {
    _mirrorResolution.value = res
  }

  fun setMirrorLatencyMode(mode: String) {
    _mirrorLatencyMode.value = mode
  }

  fun toggleMirrorAudio() {
    _mirrorAudioStream.value = !_mirrorAudioStream.value
  }

  private fun showFeedback(msg: String) {
    _lastActionFeedback.value = msg
    feedbackClearJob?.cancel()
    feedbackClearJob = viewModelScope.launch {
      delay(3000)
      // don't clear completely, keep subtle ready text
    }
  }

  fun triggerHaptic(durationMs: Long) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(durationMs)
      }
    } catch (_: Exception) {}
  }
}
