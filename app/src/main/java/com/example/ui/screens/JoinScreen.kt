package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.ClientConnectionStatus
import com.example.network.DiscoveredRoom
import com.example.ui.theme.BuzzerGold
import com.example.ui.theme.GameBackground
import com.example.ui.theme.GameSurface
import com.example.ui.theme.GameSurfaceBorder
import com.example.ui.theme.GameSurfaceElevated
import com.example.ui.theme.GlowRed
import com.example.ui.theme.QuizBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun JoinScreen(
    discoveredRooms: List<DiscoveredRoom>,
    connectionStatus: ClientConnectionStatus,
    rejectionReason: String?,
    playerName: String,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onJoinRoom: (hostIp: String, port: Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var manualIp by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onStartScan()
        onDispose {
            onStopScan()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GameBackground)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GameSurfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Text(
                text = "الانضمام إلى غرفة",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = onStartScan,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GameSurfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rescan",
                    tint = BuzzerGold
                )
            }
        }

        // Connection Error / Rejection Banner
        if (!rejectionReason.isNullOrBlank() || connectionStatus == ClientConnectionStatus.REJECTED) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GlowRed.copy(alpha = 0.15f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GlowRed, GlowRed.copy(alpha = 0.5f)))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = GlowRed)
                    Text(
                        text = rejectionReason ?: "فشل الاتصال بالغرفة، يرجى المحاولة مرة أخرى",
                        color = GlowRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Discovered Rooms List
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GameSurfaceBorder, Color.Transparent))),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiFind,
                            contentDescription = null,
                            tint = QuizBlue
                        )
                        Text(
                            text = "الغرف المكتشفة تلقائياً",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = QuizBlue.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (discoveredRooms.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "جارٍ البحث عن غرف في الشبكة المحلية…",
                                color = TextMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "تأكد من أن هاتف المضيف قام بإنشاء الغرفة وأنه متصل بنفس شبكة Wi-Fi",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp, start = 16.dp, end = 16.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(discoveredRooms) { room ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(GameSurfaceElevated)
                                    .border(1.dp, QuizBlue.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                                    .testTag("discovered_room_${room.roomCode}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MeetingRoom,
                                        contentDescription = null,
                                        tint = BuzzerGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "غرفة #${room.roomCode}",
                                            color = TextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "IP: ${room.hostIp}",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onJoinRoom(room.hostIp, room.hostPort) },
                                    enabled = connectionStatus != ClientConnectionStatus.CONNECTING,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = QuizBlue,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("join_discovered_button")
                                ) {
                                    Text(
                                        text = "انضمام",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Manual IP Connection Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GameSurfaceBorder, Color.Transparent))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showManualInput = !showManualInput },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (showManualInput) "إخفاء الاتصال اليدوي" else "الاتصال اليدوي عبر IP (في حال تعذر الاكتشاف)",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (showManualInput) {
                    OutlinedTextField(
                        value = manualIp,
                        onValueChange = { manualIp = it },
                        singleLine = true,
                        placeholder = { Text("مثال: 192.168.1.5", color = TextMuted) },
                        label = { Text("عنوان IP للمضيف", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BuzzerGold,
                            unfocusedBorderColor = GameSurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = GameSurfaceElevated,
                            unfocusedContainerColor = GameSurfaceElevated
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_ip_input")
                    )

                    Button(
                        onClick = { onJoinRoom(manualIp, 8080) },
                        enabled = manualIp.isNotBlank() && connectionStatus != ClientConnectionStatus.CONNECTING,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BuzzerGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("connect_manual_button")
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اتصال مباشر", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
