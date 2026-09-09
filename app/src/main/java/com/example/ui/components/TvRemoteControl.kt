package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.data.model.RemoteKey
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.FaintBlueGrid
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PowerRed
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TvRemoteControl(
  isPoweredOn: Boolean,
  volumeLevel: Int,
  isMuted: Boolean,
  currentChannel: Int,
  onKeyPressed: (RemoteKey) -> Unit,
  onLaunchApp: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var showNumpad by remember { mutableStateOf(false) }
  var isPlaying by remember { mutableStateOf(true) }
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Top Power, Input & Mute Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Power Button
      FilledTonalIconButton(
        onClick = { onKeyPressed(RemoteKey.POWER) },
        colors = IconButtonDefaults.filledTonalIconButtonColors(
          containerColor = if (isPoweredOn) PowerRed.copy(alpha = 0.2f) else DarkSurfaceElevated,
          contentColor = if (isPoweredOn) PowerRed else TextMuted
        ),
        modifier = Modifier
          .size(54.dp)
          .border(
            width = 1.dp,
            color = if (isPoweredOn) PowerRed.copy(alpha = 0.6f) else DarkSurfaceBorder,
            shape = CircleShape
          )
          .testTag("remote_power_button")
      ) {
        Icon(
          imageVector = Icons.Default.PowerSettingsNew,
          contentDescription = "Power",
          modifier = Modifier.size(26.dp)
        )
      }

      // Input / HDMI Source Button
      ElevatedButton(
        onClick = { onKeyPressed(RemoteKey.INPUT) },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.elevatedButtonColors(
          containerColor = DarkSurfaceElevated,
          contentColor = TextPrimary
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
          brush = Brush.linearGradient(listOf(DarkSurfaceBorder, FaintBlueGrid))
        ),
        modifier = Modifier.testTag("remote_input_button")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(imageVector = Icons.Default.Input, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
          Text(text = "HDMI / المصدر", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
      }

      // Mute Button
      FilledTonalIconButton(
        onClick = { onKeyPressed(RemoteKey.MUTE) },
        colors = IconButtonDefaults.filledTonalIconButtonColors(
          containerColor = if (isMuted) NeonCyan.copy(alpha = 0.25f) else DarkSurfaceElevated,
          contentColor = if (isMuted) NeonCyan else TextSecondary
        ),
        modifier = Modifier
          .size(54.dp)
          .border(
            width = 1.dp,
            color = if (isMuted) NeonCyan else DarkSurfaceBorder,
            shape = CircleShape
          )
          .testTag("remote_mute_button")
      ) {
        Icon(
          imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
          contentDescription = "Mute",
          modifier = Modifier.size(24.dp)
        )
      }
    }

    // Rockers Row: Volume & Channel
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Volume Rocker
      Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(DarkSurfaceBorder, FaintBlueGrid))),
        modifier = Modifier
          .width(82.dp)
          .height(170.dp)
          .testTag("volume_rocker")
      ) {
        Column(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.SpaceBetween,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          IconButton(
            onClick = { onKeyPressed(RemoteKey.VOL_UP) },
            modifier = Modifier
              .size(52.dp)
              .testTag("vol_up_btn")
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Volume Up", tint = NeonCyan)
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = if (isMuted) "كتم" else "$volumeLevel%",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (isMuted) PowerRed else TextPrimary
            )
            Text(text = "الصوت", fontSize = 10.sp, color = TextMuted)
          }

          IconButton(
            onClick = { onKeyPressed(RemoteKey.VOL_DOWN) },
            modifier = Modifier
              .size(52.dp)
              .testTag("vol_down_btn")
          ) {
            Icon(imageVector = Icons.Default.Remove, contentDescription = "Volume Down", tint = TextSecondary)
          }
        }
      }

      // Middle Status & Numpad Switcher
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Numpad Toggle Button
        FilledTonalIconButton(
          onClick = { showNumpad = !showNumpad },
          colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (showNumpad) NeonCyan.copy(alpha = 0.25f) else DarkSurfaceElevated,
            contentColor = if (showNumpad) NeonCyan else TextSecondary
          ),
          modifier = Modifier
            .size(48.dp)
            .border(width = 1.dp, color = DarkSurfaceBorder, shape = CircleShape)
            .testTag("toggle_numpad_btn")
        ) {
          Icon(imageVector = Icons.Default.Apps, contentDescription = "Numpad", modifier = Modifier.size(22.dp))
        }
        Text(text = "لوحة الأرقام", fontSize = 10.sp, color = TextMuted)
      }

      // Channel Rocker
      Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(DarkSurfaceBorder, FaintBlueGrid))),
        modifier = Modifier
          .width(82.dp)
          .height(170.dp)
          .testTag("channel_rocker")
      ) {
        Column(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.SpaceBetween,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          IconButton(
            onClick = { onKeyPressed(RemoteKey.CH_UP) },
            modifier = Modifier
              .size(52.dp)
              .testTag("ch_up_btn")
          ) {
            Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Channel Up", tint = NeonCyan)
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "CH $currentChannel",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(text = "القناة", fontSize = 10.sp, color = TextMuted)
          }

          IconButton(
            onClick = { onKeyPressed(RemoteKey.CH_DOWN) },
            modifier = Modifier
              .size(52.dp)
              .testTag("ch_down_btn")
          ) {
            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Channel Down", tint = TextSecondary)
          }
        }
      }
    }

    // Expandable Numpad
    AnimatedVisibility(visible = showNumpad) {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkSurfaceBorder, FaintBlueGrid))),
        modifier = Modifier.fillMaxWidth().testTag("numpad_grid")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          val numRows = listOf(
            listOf(RemoteKey.NUM_1, RemoteKey.NUM_2, RemoteKey.NUM_3),
            listOf(RemoteKey.NUM_4, RemoteKey.NUM_5, RemoteKey.NUM_6),
            listOf(RemoteKey.NUM_7, RemoteKey.NUM_8, RemoteKey.NUM_9),
            listOf(RemoteKey.DPAD_LEFT, RemoteKey.NUM_0, RemoteKey.DPAD_OK)
          )

          numRows.forEach { row ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly
            ) {
              row.forEach { key ->
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                    .clickable { onKeyPressed(key) }
                    .testTag("num_${key.code}"),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = key.labelEnglish,
                    color = if (key == RemoteKey.DPAD_OK) NeonCyan else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                  )
                }
              }
            }
          }
        }
      }
    }

    // Directional D-PAD (دائرة التحكم الذكية بالتوجيه)
    Box(
      modifier = Modifier
        .size(230.dp)
        .clip(CircleShape)
        .background(DarkSurface)
        .border(
          width = 2.dp,
          brush = Brush.sweepGradient(listOf(FaintBlueGrid, NeonCyan.copy(alpha = 0.4f), FaintBlueGrid)),
          shape = CircleShape
        )
        .testTag("dpad_controller"),
      contentAlignment = Alignment.Center
    ) {
      // D-Pad Up
      IconButton(
        onClick = { onKeyPressed(RemoteKey.DPAD_UP) },
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 10.dp)
          .size(52.dp)
          .testTag("dpad_up_btn")
      ) {
        Icon(
          imageVector = Icons.Default.KeyboardArrowUp,
          contentDescription = "Up",
          tint = NeonCyan,
          modifier = Modifier.size(34.dp)
        )
      }

      // D-Pad Down
      IconButton(
        onClick = { onKeyPressed(RemoteKey.DPAD_DOWN) },
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 10.dp)
          .size(52.dp)
          .testTag("dpad_down_btn")
      ) {
        Icon(
          imageVector = Icons.Default.KeyboardArrowDown,
          contentDescription = "Down",
          tint = NeonCyan,
          modifier = Modifier.size(34.dp)
        )
      }

      // D-Pad Left
      IconButton(
        onClick = { onKeyPressed(RemoteKey.DPAD_LEFT) },
        modifier = Modifier
          .align(Alignment.CenterStart)
          .padding(start = 10.dp)
          .size(52.dp)
          .testTag("dpad_left_btn")
      ) {
        Icon(
          imageVector = Icons.Default.KeyboardArrowLeft,
          contentDescription = "Left",
          tint = NeonCyan,
          modifier = Modifier.size(34.dp)
        )
      }

      // D-Pad Right
      IconButton(
        onClick = { onKeyPressed(RemoteKey.DPAD_RIGHT) },
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 10.dp)
          .size(52.dp)
          .testTag("dpad_right_btn")
      ) {
        Icon(
          imageVector = Icons.Default.KeyboardArrowRight,
          contentDescription = "Right",
          tint = NeonCyan,
          modifier = Modifier.size(34.dp)
        )
      }

      // Central OK / Select Button
      Box(
        modifier = Modifier
          .size(76.dp)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(NeonCyan.copy(alpha = 0.25f), DarkSurfaceElevated)
            )
          )
          .border(1.5.dp, NeonCyan.copy(alpha = 0.6f), CircleShape)
          .clickable { onKeyPressed(RemoteKey.DPAD_OK) }
          .testTag("dpad_ok_btn"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "OK",
          color = NeonCyan,
          fontWeight = FontWeight.Black,
          fontSize = 18.sp
        )
      }
    }

    // System Nav Row: Back, Home, Menu, Settings
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      SystemNavIconBtn(icon = Icons.AutoMirrored.Filled.ArrowBack, label = "رجوع", tag = "nav_back") {
        onKeyPressed(RemoteKey.BACK)
      }
      SystemNavIconBtn(icon = Icons.Default.Home, label = "الرئيسية", tag = "nav_home") {
        onKeyPressed(RemoteKey.HOME)
      }
      SystemNavIconBtn(icon = Icons.Default.Menu, label = "القائمة", tag = "nav_menu") {
        onKeyPressed(RemoteKey.MENU)
      }
      SystemNavIconBtn(icon = Icons.Default.Settings, label = "الإعدادات", tag = "nav_settings") {
        onKeyPressed(RemoteKey.SETTINGS)
      }
    }

    // Media Playback Bar: Rewind, Play/Pause, Fast Forward
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkSurfaceBorder, FaintBlueGrid))),
      modifier = Modifier.fillMaxWidth().testTag("media_controls_card")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 6.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = { onKeyPressed(RemoteKey.REWIND) }, modifier = Modifier.testTag("btn_rewind")) {
          Icon(imageVector = Icons.Default.FastRewind, contentDescription = "Rewind", tint = TextSecondary)
        }

        FilledTonalIconButton(
          onClick = {
            isPlaying = !isPlaying
            onKeyPressed(RemoteKey.PLAY_PAUSE)
          },
          colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = NeonCyan.copy(alpha = 0.2f),
            contentColor = NeonCyan
          ),
          modifier = Modifier
            .size(48.dp)
            .border(1.dp, NeonCyan, CircleShape)
            .testTag("btn_play_pause")
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = "Play/Pause",
            modifier = Modifier.size(24.dp)
          )
        }

        IconButton(onClick = { onKeyPressed(RemoteKey.FAST_FORWARD) }, modifier = Modifier.testTag("btn_forward")) {
          Icon(imageVector = Icons.Default.FastForward, contentDescription = "Fast Forward", tint = TextSecondary)
        }
      }
    }

    // Quick App Shortcuts (YouTube, Netflix, Prime, Shahid/Browser)
    Text(
      text = "تطبيقات البث السريعة على التلفاز",
      color = TextMuted,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.align(Alignment.Start).padding(start = 6.dp)
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf(
        Pair("YouTube", Color(0xFFFF0000)),
        Pair("Netflix", Color(0xFFE50914)),
        Pair("Prime", Color(0xFF00A8E1)),
        Pair("متصفح TV", NeonCyan)
      ).forEach { (appName, badgeColor) ->
        ElevatedButton(
          onClick = { onLaunchApp(appName) },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.elevatedButtonColors(
            containerColor = DarkSurface,
            contentColor = TextPrimary
          ),
          border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(listOf(DarkSurfaceBorder, badgeColor.copy(alpha = 0.4f)))
          ),
          modifier = Modifier
            .weight(1f)
            .height(42.dp)
            .testTag("app_${appName.take(4)}")
        ) {
          Text(text = appName, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
      }
    }
  }
}

@Composable
private fun SystemNavIconBtn(
  icon: ImageVector,
  label: String,
  tag: String,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    FilledTonalIconButton(
      onClick = onClick,
      colors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = DarkSurfaceElevated,
        contentColor = TextPrimary
      ),
      modifier = Modifier
        .size(46.dp)
        .border(1.dp, DarkSurfaceBorder, CircleShape)
        .testTag(tag)
    ) {
      Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(20.dp))
    }
    Text(text = label, color = TextMuted, fontSize = 10.sp)
  }
}
