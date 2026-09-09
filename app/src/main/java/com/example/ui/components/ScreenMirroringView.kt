package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TvDevice
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.FaintBlueGrid
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanDark
import com.example.ui.theme.PowerRed
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ScreenMirroringView(
  device: TvDevice?,
  isMirroring: Boolean,
  mirrorResolution: String,
  mirrorLatencyMode: String,
  mirrorAudioStream: Boolean,
  onToggleMirroring: () -> Unit,
  onSetResolution: (String) -> Unit,
  onSetLatencyMode: (String) -> Unit,
  onToggleAudioStream: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseGlow by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 0.8f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Hero Mirroring Status Card
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurface),
      border = CardDefaults.outlinedCardBorder().copy(
        brush = Brush.radialGradient(
          colors = listOf(
            if (isMirroring) NeonCyan.copy(alpha = 0.4f) else DarkSurfaceBorder,
            DarkSurfaceBorder
          )
        )
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("screen_mirror_hero_card")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Glowing Icon Beacon
        Box(
          modifier = Modifier
            .size(90.dp)
            .clip(CircleShape)
            .background(
              if (isMirroring) NeonCyan.copy(alpha = 0.15f * pulseGlow) else DarkSurfaceElevated
            )
            .border(
              width = 2.dp,
              color = if (isMirroring) NeonCyan else DarkSurfaceBorder,
              shape = CircleShape
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (isMirroring) Icons.Default.CastConnected else Icons.Default.Cast,
            contentDescription = "Cast Status",
            tint = if (isMirroring) NeonCyan else TextSecondary,
            modifier = Modifier.size(44.dp)
          )
        }

        Text(
          text = if (isMirroring) "بث الشاشة قيد التشغيل الآن" else "بث شاشة الهاتف للتلفاز",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = TextPrimary
        )

        Text(
          text = if (device != null) {
            "الهدف: ${device.name} (${device.ipAddress})"
          } else {
            "يرجى تحديد تلفزيون من الشبكة لبدء البث"
          },
          style = MaterialTheme.typography.bodySmall,
          color = if (isMirroring) NeonCyan else TextMuted
        )

        // Primary Action: Start / Stop Mirroring
        ElevatedButton(
          onClick = onToggleMirroring,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.elevatedButtonColors(
            containerColor = if (isMirroring) PowerRed.copy(alpha = 0.2f) else NeonCyan,
            contentColor = if (isMirroring) PowerRed else Color.Black
          ),
          border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(
              listOf(
                if (isMirroring) PowerRed else NeonCyan,
                if (isMirroring) PowerRed.copy(alpha = 0.4f) else NeonCyanDark
              )
            )
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("btn_toggle_mirroring")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = if (isMirroring) Icons.Default.CastConnected else Icons.Default.Cast,
              contentDescription = null,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = if (isMirroring) "إيقاف بث الشاشة" else "بدء بث الشاشة للتلفاز",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }
        }

        // Native System Cast Launcher button
        OutlinedButton(
          onClick = { openSystemCastSettings(context) },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
          border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(DarkSurfaceBorder, FaintBlueGrid))),
          modifier = Modifier.fillMaxWidth().testTag("btn_system_cast_settings")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
            Text(text = "فتح إعدادات البث اللاسلكي للنظام (Smart View / Cast)", fontSize = 11.sp)
          }
        }
      }
    }

    // Live Stream Telemetry
    AnimatedVisibility(visible = isMirroring) {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkSurfaceBorder, FaintBlueGrid))),
        modifier = Modifier.fillMaxWidth().testTag("stream_telemetry_card")
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = "مؤشرات البث المباشر (Telemetry)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StatusGreen))
              Text(text = "مستقر", color = StatusGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            TelemetryItem(title = "معدل الإطارات", value = "60 FPS")
            TelemetryItem(title = "سرعة التدفق", value = "16.2 Mbps")
            TelemetryItem(title = "زمن الاستجابة", value = "14 ms")
            TelemetryItem(title = "البروتوكول", value = "Miracast / DLNA")
          }
        }
      }
    }

    // Quality / Resolution Selector
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurface),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkSurfaceBorder, FaintBlueGrid))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(imageVector = Icons.Default.HighQuality, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
          Text(text = "دقة وضوح البث:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
        }

        val resolutions = listOf(
          "1080p FHD (60 FPS)",
          "720p Gaming (Ultra Low Latency)",
          "4K UHD (30 FPS)"
        )
        resolutions.forEach { res ->
          val isSelected = mirrorResolution == res
          FilterChip(
            selected = isSelected,
            onClick = { onSetResolution(res) },
            label = { Text(res, fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
              selectedLabelColor = NeonCyan,
              containerColor = DarkSurfaceElevated,
              labelColor = TextSecondary
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = isSelected,
              borderColor = if (isSelected) NeonCyan else DarkSurfaceBorder
            ),
            modifier = Modifier.fillMaxWidth().testTag("res_${res.take(5)}")
          )
        }
      }
    }

    // Latency & Audio Stream Toggles
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurface),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkSurfaceBorder, FaintBlueGrid))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
            Column {
              Text(text = "مزامنة وبث الصوت مع الصورة", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text(text = "تشغيل صوت الهاتف عبر مكبرات التلفزيون", color = TextMuted, fontSize = 10.sp)
            }
          }
          Switch(
            checked = mirrorAudioStream,
            onCheckedChange = { onToggleAudioStream() },
            colors = SwitchDefaults.colors(
              checkedThumbColor = NeonCyan,
              checkedTrackColor = NeonCyanDark.copy(alpha = 0.4f),
              uncheckedThumbColor = TextMuted,
              uncheckedTrackColor = DarkSurfaceElevated
            )
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
            Column {
              Text(text = "وضع الألعاب الفائق (Ultra Low Latency)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text(text = "تقليل تأخير الإطارات حتى 12ms لمشاهدة بدون تقطيع", color = TextMuted, fontSize = 10.sp)
            }
          }
          Switch(
            checked = mirrorLatencyMode.contains("ألعاب"),
            onCheckedChange = { isGame ->
              onSetLatencyMode(if (isGame) "فائق السرعة (ألعاب - 12ms)" else "وضع السينما المتزن")
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = NeonCyan,
              checkedTrackColor = NeonCyanDark.copy(alpha = 0.4f),
              uncheckedThumbColor = TextMuted,
              uncheckedTrackColor = DarkSurfaceElevated
            )
          )
        }
      }
    }

    // Info Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkSurfaceBorder, FaintBlueGrid))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
      ) {
        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
        Text(
          text = "تأكد من اتصال الهاتف والتلفزيون بنفس شبكة Wi-Fi. يدعم تطبيق zaidTV تقنيات البث اللاسلكي: Google Cast، Samsung Smart View، LG Screen Share، وتقنية Miracast القياسية.",
          color = TextSecondary,
          fontSize = 11.sp,
          lineHeight = 17.sp
        )
      }
    }
  }
}

@Composable
private fun TelemetryItem(title: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = value, color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Text(text = title, color = TextMuted, fontSize = 10.sp)
  }
}

private fun openSystemCastSettings(context: Context) {
  try {
    val intent = Intent(Settings.ACTION_CAST_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
  } catch (_: Exception) {
    try {
      val intent = Intent("android.settings.WIFI_DISPLAY_SETTINGS")
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
    } catch (_: Exception) {
      try {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
      } catch (_: Exception) {}
    }
  }
}
