package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionState
import com.example.ui.MainViewModel
import com.example.ui.components.DeviceScannerView
import com.example.ui.components.ScreenMirroringView
import com.example.ui.components.TvMouseTrackpad
import com.example.ui.components.TvRemoteControl
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.FaintBlueGrid
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanDark
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class ZaidTvTab(val title: String, val icon: ImageVector, val tag: String) {
  REMOTE("الريموت", Icons.Default.SettingsRemote, "tab_remote"),
  TOUCHPAD("لوحة اللمس", Icons.Default.TouchApp, "tab_touchpad"),
  MIRROR("بث الشاشة", Icons.Default.Cast, "tab_mirror"),
  SMART_CONNECT("الاتصال الذكي", Icons.Default.WifiTethering, "tab_connect")
}

class MainActivity : ComponentActivity() {
  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(darkTheme = true) {
        ZaidTvApp(viewModel)
      }
    }
  }
}

@Composable
fun ZaidTvApp(viewModel: MainViewModel) {
  var selectedTab by remember { mutableIntStateOf(0) }

  val selectedDevice by viewModel.selectedDevice.collectAsState()
  val connectionState by viewModel.connectionState.collectAsState()
  val discoveredDevices by viewModel.discoveredDevices.collectAsState()
  val isScanning by viewModel.isScanning.collectAsState()

  val isTvPoweredOn by viewModel.isTvPoweredOn.collectAsState()
  val volumeLevel by viewModel.volumeLevel.collectAsState()
  val isMuted by viewModel.isMuted.collectAsState()
  val currentChannel by viewModel.currentChannel.collectAsState()

  val mousePointerState by viewModel.mousePointerState.collectAsState()
  val isMouseModeEnabled by viewModel.isMouseModeEnabled.collectAsState()

  val isMirroring by viewModel.isMirroring.collectAsState()
  val mirrorResolution by viewModel.mirrorResolution.collectAsState()
  val mirrorLatencyMode by viewModel.mirrorLatencyMode.collectAsState()
  val mirrorAudioStream by viewModel.mirrorAudioStream.collectAsState()

  val feedbackMessage by viewModel.lastActionFeedback.collectAsState()

  Scaffold(
    containerColor = DarkBackground,
    topBar = {
      ZaidTvTopAppBar(
        activeDeviceName = selectedDevice?.name,
        isConnected = connectionState == ConnectionState.CONNECTED,
        onDeviceHeaderClick = { selectedTab = 3 }
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = DarkSurface,
        tonalElevation = 6.dp,
        modifier = Modifier
          .border(
            width = 1.dp,
            brush = Brush.horizontalGradient(listOf(DarkSurfaceBorder, FaintBlueGrid, DarkSurfaceBorder)),
            shape = RoundedCornerShape(0.dp)
          )
          .testTag("zaidtv_bottom_nav")
      ) {
        ZaidTvTab.values().forEachIndexed { index, tab ->
          val isSelected = selectedTab == index
          NavigationBarItem(
            selected = isSelected,
            onClick = {
              viewModel.triggerHaptic(25)
              selectedTab = index
            },
            icon = {
              Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                tint = if (isSelected) NeonCyan else TextMuted,
                modifier = Modifier.size(24.dp)
              )
            },
            label = {
              Text(
                text = tab.title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) NeonCyan else TextMuted
              )
            },
            colors = NavigationBarItemDefaults.colors(
              indicatorColor = NeonCyan.copy(alpha = 0.16f),
              selectedIconColor = NeonCyan,
              unselectedIconColor = TextMuted
            ),
            modifier = Modifier.testTag(tab.tag)
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Main Content Switching by selected tab
      when (selectedTab) {
        0 -> TvRemoteControl(
          isPoweredOn = isTvPoweredOn,
          volumeLevel = volumeLevel,
          isMuted = isMuted,
          currentChannel = currentChannel,
          onKeyPressed = { viewModel.onRemoteKeyPressed(it) },
          onLaunchApp = { viewModel.launchTvApp(it) },
          modifier = Modifier.fillMaxSize()
        )
        1 -> TvMouseTrackpad(
          device = selectedDevice,
          mousePointerState = mousePointerState,
          isMouseModeEnabled = isMouseModeEnabled,
          onMouseMoved = { dx, dy -> viewModel.onMouseMoved(dx, dy) },
          onMouseClick = { isLeft -> viewModel.onMouseClick(isLeft) },
          onToggleMouseMode = { viewModel.toggleMouseMode(it) },
          onSensitivityChanged = { viewModel.setMouseSensitivity(it) },
          modifier = Modifier.fillMaxSize()
        )
        2 -> ScreenMirroringView(
          device = selectedDevice,
          isMirroring = isMirroring,
          mirrorResolution = mirrorResolution,
          mirrorLatencyMode = mirrorLatencyMode,
          mirrorAudioStream = mirrorAudioStream,
          onToggleMirroring = { viewModel.toggleScreenMirroring() },
          onSetResolution = { viewModel.setMirrorResolution(it) },
          onSetLatencyMode = { viewModel.setMirrorLatencyMode(it) },
          onToggleAudioStream = { viewModel.toggleMirrorAudio() },
          modifier = Modifier.fillMaxSize()
        )
        3 -> DeviceScannerView(
          devices = discoveredDevices,
          selectedDevice = selectedDevice,
          connectionState = connectionState,
          isScanning = isScanning,
          onScanClicked = { viewModel.scanForTvs() },
          onConnectClicked = { viewModel.connectToDevice(it) },
          onDisconnectClicked = { viewModel.disconnect() },
          onAddManualDevice = { name, ip, port, brand ->
            viewModel.addCustomDevice(name, ip, port, brand)
          },
          modifier = Modifier.fillMaxSize()
        )
      }

      // Subtle Action Feedback HUD Toast at Top
      AnimatedVisibility(
        visible = !feedbackMessage.isNullOrBlank(),
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 8.dp)
      ) {
        feedbackMessage?.let { msg ->
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated.copy(alpha = 0.95f)),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(FaintBlueGrid, NeonCyan.copy(alpha = 0.4f)))),
            modifier = Modifier.padding(horizontal = 24.dp).testTag("feedback_hud_toast")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NeonCyan))
              Text(
                text = msg,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun ZaidTvTopAppBar(
  activeDeviceName: String?,
  isConnected: Boolean,
  onDeviceHeaderClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(DarkBackground)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // App Brand Logo & Name
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(
            Brush.linearGradient(listOf(NeonCyan, NeonCyanDark))
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Tv,
          contentDescription = "zaidTV Logo",
          tint = Color.Black,
          modifier = Modifier.size(22.dp)
        )
      }

      Column {
        Text(
          text = "zaidTV",
          fontSize = 18.sp,
          fontWeight = FontWeight.Black,
          color = TextPrimary,
          letterSpacing = 0.5.sp
        )
        Text(
          text = "اتصال ذكي • تحكم وبث",
          fontSize = 10.sp,
          color = NeonCyan.copy(alpha = 0.8f)
        )
      }
    }

    // Active Device Status Pill
    Row(
      modifier = Modifier
        .clip(RoundedCornerShape(20.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, if (isConnected) StatusGreen.copy(alpha = 0.5f) else DarkSurfaceBorder, RoundedCornerShape(20.dp))
        .clickable { onDeviceHeaderClick() }
        .padding(horizontal = 12.dp, vertical = 6.dp)
        .testTag("top_device_status_pill"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(if (isConnected) StatusGreen else TextMuted)
      )
      Text(
        text = if (isConnected) (activeDeviceName?.take(18) ?: "تلفاز متصل") else "غير متصل",
        color = if (isConnected) TextPrimary else TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}
