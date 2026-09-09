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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.style.TextAlign
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
  networkInfo: String,
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
      animation = tween(1200, easing = LinearEasing),
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
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(NeonCyan.copy(alpha = 0.15f))
              .border(1.dp, NeonCyan.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Router,
              contentDescription = null,
              tint = NeonCyan,
              modifier = Modifier.size(24.dp)
            )
          }

          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "فحص شبكة Wi-Fi الحقيقية",
              color = TextPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
            Text(
              text = networkInfo,
              color = if (isScanning) NeonCyan else TextMuted,
              fontSize = 10.sp,
              maxLines = 2
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

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
            Text(
              text = if (isScanning) "فحص..." else "بحث حقيقي",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // Scanning Radar Indicator when active
    AnimatedVisibility(visible = isScanning) {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NeonCyan.copy(alpha = 0.08f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NeonCyanDark, NeonCyan.copy(alpha = 0.5f)))),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          CircularProgressIndicator(
            color = NeonCyan,
            strokeWidth = 2.dp,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = "جارٍ إرسال حزم SSDP/UPnP وفحص منافذ التلفزيون (8060, 8001, 8008, 3000)...",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
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
        Text(
          text = if (devices.isEmpty()) "شاشات التلفزيون" else "تم العثور على (${devices.size}) شاشة حقيقية",
          color = TextSecondary,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold
        )
      }

      TextButton(
        onClick = { showManualDialog = true },
        modifier = Modifier.testTag("btn_add_manual_tv")
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
          Text(text = "إضافة يدوية (IP)", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Empty State vs Device List
    if (devices.isEmpty()) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkSurfaceBorder, FaintBlueGrid))),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .testTag("empty_scanner_card")
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .background(DarkSurfaceElevated)
              .border(1.dp, NeonCyan.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Tv,
              contentDescription = null,
              tint = if (isScanning) NeonCyan else TextMuted,
              modifier = Modifier.size(36.dp)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = if (isScanning) "جارٍ البحث عن شاشتك على الشبكة..." else "لم يتم العثور على أجهزة بعد",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "تأكد من أن هاتفك وشاشة التلفاز متصلان بنفس شبكة Wi-Fi المحلية وأن الشاشة قيد التشغيل، ثم اضغط على زر البحث أو أضف IP التلفزيون مباشرة.",
            color = TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
          )

          Spacer(modifier = Modifier.height(20.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ElevatedButton(
              onClick = onScanClicked,
              enabled = !isScanning,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.elevatedButtonColors(
                containerColor = NeonCyan,
                contentColor = Color.Black
              ),
              modifier = Modifier.testTag("btn_empty_start_scan")
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(text = "بدء الفحص الآن", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }

            OutlinedButton(
              onClick = { showManualDialog = true },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
              border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.linearGradient(listOf(DarkSurfaceBorder, NeonCyan.copy(alpha = 0.6f)))
              )
            ) {
              Text(text = "إدخال IP يدوياً", fontSize = 12.sp)
            }
          }
        }
      }
    } else {
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
                  if (isCurrentSelected) NeonCyan.copy(alpha = 0.4f) else FaintBlueGrid
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
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
                    text = "${tv.brand.displayName} • IP: ${tv.ipAddress}:${tv.port}",
                    color = TextMuted,
                    fontSize = 11.sp
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
                          .padding(horizontal = 5.dp, vertical = 1.dp)
                      ) {
                        Icon(imageVector = Icons.Default.Mouse, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(10.dp))
                        Text(text = "يدعم الماوس", color = NeonCyan, fontSize = 9.sp)
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

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
                    brush = Brush.linearGradient(listOf(StatusGreen, StatusGreen.copy(alpha = 0.5f)))
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
                    brush = Brush.linearGradient(listOf(DarkSurfaceBorder, NeonCyan.copy(alpha = 0.5f)))
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
  }

  // Manual TV IP Entry Dialog
  if (showManualDialog) {
    var manualName by remember { mutableStateOf("") }
    var manualIp by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf("8002") }
    var selectedBrand by remember { mutableStateOf(TvBrand.SAMSUNG) }

    AlertDialog(
      onDismissRequest = { showManualDialog = false },
      containerColor = DarkSurfaceElevated,
      title = {
        Text(text = "إضافة شاشة تلفزيون عبر IP", color = TextPrimary, fontWeight = FontWeight.Bold)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "أدخل عنوان IP التلفزيون الموجود في إعدادات الشبكة بالشاشة:",
            color = TextMuted,
            fontSize = 12.sp
          )

          // Brand selection chips
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(
              TvBrand.SAMSUNG,
              TvBrand.LG,
              TvBrand.ROKU,
              TvBrand.ANDROID_TV,
              TvBrand.SONY,
              TvBrand.GENERIC_DLNA
            ).forEach { brand ->
              FilterChip(
                selected = selectedBrand == brand,
                onClick = {
                  selectedBrand = brand
                  manualPort = brand.defaultPort.toString()
                },
                label = { Text(brand.displayName.take(12), fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                  selectedLabelColor = NeonCyan
                )
              )
            }
          }

          OutlinedTextField(
            value = manualName,
            onValueChange = { manualName = it },
            label = { Text("اسم الشاشة (اختياري)") },
            placeholder = { Text("مثال: شاشة الصالة") },
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
            label = { Text("عنوان IP التلفزيون") },
            placeholder = { Text("مثال: 192.168.1.100") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = NeonCyan,
              unfocusedBorderColor = DarkSurfaceBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth().testTag("input_manual_ip")
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
            if (manualIp.isNotBlank()) {
              val portNum = manualPort.toIntOrNull() ?: selectedBrand.defaultPort
              onAddManualDevice(manualName, manualIp.trim(), portNum, selectedBrand)
              showManualDialog = false
            }
          },
          colors = ButtonDefaults.elevatedButtonColors(containerColor = NeonCyan, contentColor = Color.Black),
          modifier = Modifier.testTag("btn_confirm_manual_ip")
        ) {
          Text("اتصال مباشر", fontWeight = FontWeight.Bold)
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
