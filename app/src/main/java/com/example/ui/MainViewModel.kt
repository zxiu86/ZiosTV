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
  val networkInfo: StateFlow<String> = discoveryManager.networkInfo

  private val _selectedDevice = MutableStateFlow<TvDevice?>(null)
  val selectedDevice: StateFlow<TvDevice?> = _selectedDevice.asStateFlow()

  private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
  val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

  private val _currentLatencyMs = MutableStateFlow<Long?>(null)
  val currentLatencyMs: StateFlow<Long?> = _currentLatencyMs.asStateFlow()

  // Remote state
  private val _isTvPoweredOn = MutableStateFlow(true)
  val isTvPoweredOn: StateFlow<Boolean> = _isTvPoweredOn.asStateFlow()

  private val _volumeLevel = MutableStateFlow(32)
  val volumeLevel: StateFlow<Int> = _volumeLevel.asStateFlow()

  private val _isMuted = MutableStateFlow(false)
  val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

  private val _currentChannel = MutableStateFlow(1)
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
  private val _lastActionFeedback = MutableStateFlow<String?>("البحث التلقائي جاهز على شبكة Wi-Fi")
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
    // Initial real network scan on Wi-Fi
    viewModelScope.launch {
      discoveryManager.scanNetwork()
    }

    // If a device is discovered and none is selected, auto-select it
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
      triggerHaptic(20)
      _lastActionFeedback.value = "جارٍ فحص شبكة Wi-Fi ومنافذ التلفاز..."
      discoveryManager.scanNetwork()
      val count = discoveryManager.discoveredDevices.value.size
      _lastActionFeedback.value = if (count > 0) "تم العثور على $count شاشة متصلة" else "لم يتم العثور على أجهزة جديدة (تأكد من تشغيل الشاشة)"
    }
  }

  fun connectToDevice(device: TvDevice) {
    viewModelScope.launch {
      _connectionState.value = ConnectionState.CONNECTING
      _selectedDevice.value = device
      _lastActionFeedback.value = "جارٍ الاتصال وتجهيز قنوات التحكم مع ${device.name}..."
      triggerHaptic(25)

      // Initiate protocol pairing & handshake
      val pairingResult = protocolClient.initiatePairing(device)
      val ping = protocolClient.pingDevice(device)

      if (ping >= 0) {
        _currentLatencyMs.value = ping
        _connectionState.value = ConnectionState.CONNECTED
        if (pairingResult.requiresOnScreenApproval) {
          _lastActionFeedback.value = pairingResult.message
        } else {
          _lastActionFeedback.value = "متصل بنجاح: ${device.name} ($ping ms)"
        }
        triggerHaptic(50)
      } else {
        // Still allow connection via direct IP mode
        _currentLatencyMs.value = null
        _connectionState.value = ConnectionState.CONNECTED
        _lastActionFeedback.value = if (pairingResult.requiresOnScreenApproval) {
          pairingResult.message
        } else {
          "متصل عبر IP: ${device.ipAddress} (تأكد من تشغيل التلفاز)"
        }
        triggerHaptic(30)
      }
    }
  }

  fun disconnect() {
    _connectionState.value = ConnectionState.DISCONNECTED
    _selectedDevice.value = null
    _currentLatencyMs.value = null
    _lastActionFeedback.value = "تم قطع الاتصال"
    triggerHaptic(30)
  }

  fun addCustomDevice(name: String, ip: String, port: Int, brand: TvBrand) {
    viewModelScope.launch {
      _lastActionFeedback.value = "جارٍ التحقق من عنوان IP: $ip..."
      val newDev = discoveryManager.addCustomDevice(name, ip, port, brand)
      connectToDevice(newDev)
    }
  }

  fun onRemoteKeyPressed(key: RemoteKey) {
    triggerHaptic(20)
    val dev = _selectedDevice.value

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

    if (dev != null) {
      viewModelScope.launch {
        val result = protocolClient.sendRemoteKey(dev, key)
        _currentLatencyMs.value = result.latencyMs
        showFeedback("${result.message} (${result.latencyMs} ms)")
      }
    } else {
      showFeedback("أمر ${key.labelArabic} (يرجى الاتصال بتلفاز أولاً)")
    }
  }

  fun onMouseMoved(deltaX: Float, deltaY: Float) {
    val current = _mousePointerState.value
    val sensitivity = current.sensitivity
    // Smooth, precise non-linear acceleration
    val accel = if (Math.abs(deltaX) + Math.abs(deltaY) > 20) 1.25f else 1.0f
    val newX = (current.x + (deltaX * 0.0028f * sensitivity * accel)).coerceIn(0.01f, 0.99f)
    val newY = (current.y + (deltaY * 0.0028f * sensitivity * accel)).coerceIn(0.01f, 0.99f)

    _mousePointerState.value = current.copy(x = newX, y = newY)

    val dev = _selectedDevice.value
    if (dev != null) {
      viewModelScope.launch {
        protocolClient.sendMouseMovement(dev, deltaX, deltaY)
      }
    }
  }

  fun onMouseClick(isLeft: Boolean) {
    triggerHaptic(30)
    val dev = _selectedDevice.value
    if (dev != null) {
      viewModelScope.launch {
        val result = protocolClient.sendMouseClick(dev, isLeft)
        _currentLatencyMs.value = result.latencyMs
        showFeedback(result.message)
      }
    } else {
      showFeedback(if (isLeft) "نقر أيسر" else "نقر أيمن")
    }
  }

  fun toggleMouseMode(enabled: Boolean) {
    _isMouseModeEnabled.value = enabled
    _mousePointerState.value = _mousePointerState.value.copy(isVisible = enabled)
    triggerHaptic(25)
    showFeedback(if (enabled) "تم تفعيل مؤشر الماوس على التلفاز" else "تم إخفاء مؤشر الماوس")
  }

  fun setMouseSensitivity(level: Float) {
    _mousePointerState.value = _mousePointerState.value.copy(sensitivity = level)
    triggerHaptic(15)
  }

  fun launchTvApp(appName: String) {
    triggerHaptic(30)
    val dev = _selectedDevice.value
    if (dev != null) {
      viewModelScope.launch {
        val res = protocolClient.launchApp(dev, appName)
        _currentLatencyMs.value = res.latencyMs
        showFeedback(res.message)
      }
    } else {
      showFeedback("تشغيل تطبيق $appName (يرجى الاتصال بالشاشة)")
    }
  }

  // Screen mirroring actions
  fun toggleScreenMirroring() {
    triggerHaptic(40)
    _isMirroring.value = !_isMirroring.value
    val status = if (_isMirroring.value) "بدأ بث شاشة الهاتف للتلفاز" else "تم إيقاف بث الشاشة"
    showFeedback(status)
  }

  fun setMirrorResolution(res: String) {
    _mirrorResolution.value = res
    triggerHaptic(15)
  }

  fun setMirrorLatencyMode(mode: String) {
    _mirrorLatencyMode.value = mode
    triggerHaptic(15)
  }

  fun toggleMirrorAudio() {
    _mirrorAudioStream.value = !_mirrorAudioStream.value
    triggerHaptic(15)
  }

  private fun showFeedback(msg: String) {
    _lastActionFeedback.value = msg
    feedbackClearJob?.cancel()
    feedbackClearJob = viewModelScope.launch {
      delay(3500)
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
