package com.example.data.model

enum class TvBrand(val displayName: String, val defaultPort: Int, val supportsMousePointer: Boolean) {
  SAMSUNG("Samsung Smart TV", 8002, true),
  LG("LG webOS TV", 3000, true),
  ANDROID_TV("Android TV / Google TV", 6466, true),
  ROKU("Roku TV", 8060, false),
  SONY("Sony Bravia (Android)", 80, true),
  GENERIC_DLNA("Smart TV (DLNA / UPnP)", 8080, false);

  companion object {
    fun fromName(name: String): TvBrand {
      val lower = name.lowercase()
      return when {
        lower.contains("samsung") || lower.contains("tizen") -> SAMSUNG
        lower.contains("lg") || lower.contains("webos") -> LG
        lower.contains("android") || lower.contains("google") || lower.contains("chromecast") -> ANDROID_TV
        lower.contains("roku") -> ROKU
        lower.contains("sony") || lower.contains("bravia") -> SONY
        else -> GENERIC_DLNA
      }
    }
  }
}

enum class ConnectionState {
  DISCONNECTED,
  SCANNING,
  CONNECTING,
  AUTHENTICATING,
  CONNECTED,
  ERROR
}

data class TvDevice(
  val id: String,
  val name: String,
  val ipAddress: String,
  val port: Int,
  val brand: TvBrand,
  val signalStrength: Int = 90,
  val isSupportsMouse: Boolean = brand.supportsMousePointer,
  val isPaired: Boolean = false,
  val macAddress: String = "",
  val model: String = "Smart TV UHD",
  val isCustomAdded: Boolean = false
)

enum class RemoteKey(val code: String, val labelArabic: String, val labelEnglish: String) {
  POWER("power", "تشغيل/إيقاف", "Power"),
  MUTE("mute", "كتم الصوت", "Mute"),
  VOL_UP("vol_up", "رفع الصوت", "Vol +"),
  VOL_DOWN("vol_down", "خفض الصوت", "Vol -"),
  CH_UP("ch_up", "القناة التالية", "CH +"),
  CH_DOWN("ch_down", "القناة السابقة", "CH -"),
  DPAD_UP("up", "أعلى", "Up"),
  DPAD_DOWN("down", "أسفل", "Down"),
  DPAD_LEFT("left", "يسار", "Left"),
  DPAD_RIGHT("right", "يمين", "Right"),
  DPAD_OK("ok", "موافق", "OK"),
  HOME("home", "الرئيسية", "Home"),
  BACK("back", "رجوع", "Back"),
  MENU("menu", "القائمة", "Menu"),
  SETTINGS("settings", "الإعدادات", "Settings"),
  INPUT("source", "المصدر HDMI", "Input"),
  PLAY_PAUSE("play_pause", "تشغيل/إيقاف مؤقت", "Play/Pause"),
  FAST_FORWARD("ff", "تقديم", "Forward"),
  REWIND("rw", "ترجيع", "Rewind"),
  STOP("stop", "إيقاف", "Stop"),
  NUM_0("0", "0", "0"),
  NUM_1("1", "1", "1"),
  NUM_2("2", "2", "2"),
  NUM_3("3", "3", "3"),
  NUM_4("4", "4", "4"),
  NUM_5("5", "5", "5"),
  NUM_6("6", "6", "6"),
  NUM_7("7", "7", "7"),
  NUM_8("8", "8", "8"),
  NUM_9("9", "9", "9")
}

data class MousePointerState(
  val x: Float = 0.5f,
  val y: Float = 0.5f,
  val isVisible: Boolean = true,
  val isLeftDown: Boolean = false,
  val sensitivity: Float = 1.0f
)
