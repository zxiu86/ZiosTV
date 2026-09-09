package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionState
import com.example.data.model.TvBrand
import com.example.data.model.TvDevice
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.FaintBlueGrid
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanDark
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DeviceScannerView(
  devices: List<TvDevice>,
  selectedDevice: TvDevice?,
  connectionState: ConnectionState,
  isScanning: Boolean,
  onScanClicked: () -> Unit,
  onConnectClicked: (TvDevice) -> Unit,
  onDisconnectClicked: () -> Unit,
  onAddManualDevice: (String, String, Int, TvBrand) -> Unit,
  modifier: Modifier = Modifier
) {
  var showManualDialog by remember { mutableStateOf(false) }

  val infiniteTransition = rememberInfiniteTransition(label = "spin")
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotation"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Top Network Status & Scan Header Card
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurface),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkSurfaceBorder, FaintBlueGrid))),
      modifier = Modifier.fillMaxWidth().testTag("scanner_network_header")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(NeonCyan.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(imageVector = Icons.Default.Router, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
          }

          Column {
            Text(text = "تقنية الاتصال الذكي (Smart Connect)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
              text = if (isScanning) "جارٍ البحث التلقائي عن أجهزة التلفاز..." else "تم العثور على ${devices.size} جهاز على الشبكة",
              color = if (isScanning) NeonCyan else TextMuted,
              fontSize = 11.sp
            )
          }
        }

        // Scan Action Button
        ElevatedButton(
          onClick = onScanClicked,
          enabled = !isScanning,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.elevatedButtonColors(
            containerColor = DarkSurfaceElevated,
            contentColor = NeonCyan
          ),
          border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(listOf(DarkSurfaceBorder, NeonCyan.copy(alpha = 0.5f)))
          ),
          modifier = Modifier.testTag("btn_scan_network")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Scan",
              tint = NeonCyan,
              modifier = Modifier
                .size(16.dp)
                .then(if (isScanning) Modifier.rotate(rotation) else Modifier)
            )
            Text(text = if (isScanning) "بحث..." else "بحث", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Sub-header: Discovered TV Devices count & Add Manual Button
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(imageVector = Icons.Default.Tv, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
        Text(text = "شاشات التلفزيون على الشبكة", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
      }

      TextButton(
        onClick = { showManualDialog = true },
        modifier = Modifier.testTag("btn_add_manual_tv")
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
          Text(text = "إضافة يدوية (IP)", color = NeonCyan, fontSize = 11.sp)
        }
      }
    }

    // List of Discovered Smart TVs
    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .testTag("discovered_tvs_list"),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(devices, key = { it.id }) { tv ->
        val isCurrentSelected = selectedDevice?.id == tv.id && connectionState == ConnectionState.CONNECTED
        val isConnectingThis = selectedDevice?.id == tv.id && connectionState == ConnectionState.CONNECTING

        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isCurrentSelected) DarkSurfaceElevated else DarkSurface
          ),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
              listOf(
                if (isCurrentSelected) NeonCyan else DarkSurfaceBorder,
                if (isCurrentSelected) NeonCyan.copy(alpha = 0.3f) else FaintBlueGrid
              )
            )
          ),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnectClicked(tv) }
            .testTag("tv_card_${tv.id}")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              // TV Brand Icon with badge
              Box(
                modifier = Modifier
                  .size(50.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(if (isCurrentSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceElevated)
                  .border(
                    1.dp,
                    if (isCurrentSelected) NeonCyan else DarkSurfaceBorder,
                    RoundedCornerShape(14.dp)
                  ),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Tv,
                  contentDescription = null,
                  tint = if (isCurrentSelected) NeonCyan else TextSecondary,
                  modifier = Modifier.size(26.dp)
                )
              }

              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = tv.name,
                  color = TextPrimary,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
                Text(
                  text = "${tv.brand.displayName} • ${tv.ipAddress}:${tv.port}",
                  color = TextMuted,
                  fontSize = 10.sp
                )

                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(top = 2.dp)
                ) {
                  // Signal strength badge
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(imageVector = Icons.Default.SignalWifi4Bar, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(12.dp))
                    Text(text = "${tv.signalStrength}%", color = StatusGreen, fontSize = 10.sp)
                  }

                  // Mouse Pointer Support badge
                  if (tv.isSupportsMouse) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(2.dp),
                      modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                      Icon(imageVector = Icons.Default.Mouse, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(10.dp))
                      Text(text = "يدعم الماوس", color = NeonCyan, fontSize = 9.sp)
                    }
                  }
                }
              }
            }

            // Connection State Pill / Action Button
            if (isCurrentSelected) {
              ElevatedButton(
                onClick = onDisconnectClicked,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                  containerColor = StatusGreen.copy(alpha = 0.2f),
                  contentColor = StatusGreen
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                  brush = Brush.linearGradient(listOf(StatusGreen, StatusGreen.copy(alpha = 0.4f)))
                ),
                modifier = Modifier.testTag("btn_disconnect_${tv.id}")
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                  Text(text = "متصل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            } else if (isConnectingThis) {
              CircularProgressIndicator(
                color = NeonCyan,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(24.dp)
              )
            } else {
              OutlinedButton(
                onClick = { onConnectClicked(tv) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                  brush = Brush.linearGradient(listOf(DarkSurfaceBorder, NeonCyan.copy(alpha = 0.4f)))
                ),
                modifier = Modifier.testTag("btn_connect_${tv.id}")
              ) {
                Text(text = "اتصال", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      }
    }
  }

  // Manual TV IP Entry Dialog
  if (showManualDialog) {
    var manualName by remember { mutableStateOf("") }
    var manualIp by remember { mutableStateOf("192.168.1.") }
    var manualPort by remember { mutableStateOf("8002") }
    var selectedBrand by remember { mutableStateOf(TvBrand.SAMSUNG) }

    AlertDialog(
      onDismissRequest = { showManualDialog = false },
      containerColor = DarkSurfaceElevated,
      title = {
        Text(text = "إضافة تلفزيون يدوياً عبر IP", color = TextPrimary, fontWeight = FontWeight.Bold)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "أدخل عنوان IP الخاص بالتلفاز الذكي الموجود على شبكتك المحلية:",
            color = TextMuted,
            fontSize = 12.sp
          )

          OutlinedTextField(
            value = manualName,
            onValueChange = { manualName = it },
            label = { Text("اسم الشاشة (مثال: شاشة الصالة)") },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = NeonCyan,
              unfocusedBorderColor = DarkSurfaceBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = manualIp,
            onValueChange = { manualIp = it },
            label = { Text("عنوان IP") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = NeonCyan,
              unfocusedBorderColor = DarkSurfaceBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = manualPort,
            onValueChange = { manualPort = it },
            label = { Text("المنفذ (Port)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = NeonCyan,
              unfocusedBorderColor = DarkSurfaceBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        ElevatedButton(
          onClick = {
            val portNum = manualPort.toIntOrNull() ?: 8002
            onAddManualDevice(manualName, manualIp, portNum, selectedBrand)
            showManualDialog = false
          },
          colors = ButtonDefaults.elevatedButtonColors(containerColor = NeonCyan, contentColor = Color.Black)
        ) {
          Text("اتصال ذكي", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showManualDialog = false }) {
          Text("إلغاء", color = TextMuted)
        }
      }
    )
  }
}
