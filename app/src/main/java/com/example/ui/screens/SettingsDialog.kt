package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.FeedbackManager
import com.example.ui.theme.BuzzerGold
import com.example.ui.theme.GameSurface
import com.example.ui.theme.GameSurfaceBorder
import com.example.ui.theme.GameSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsDialog(
    feedbackManager: FeedbackManager,
    onDismiss: () -> Unit
) {
    var soundEnabled by remember { mutableStateOf(feedbackManager.isSoundEnabled) }
    var vibrationEnabled by remember { mutableStateOf(feedbackManager.isVibrationEnabled) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.verticalGradient(listOf(BuzzerGold.copy(alpha = 0.5f), GameSurfaceBorder))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
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
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = BuzzerGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "إعدادات المؤثرات",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                // Sound Effects Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GameSurfaceElevated, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = BuzzerGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "المؤثرات الصوتية",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "أصوات البوزير والتحكيم والعجلة",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            feedbackManager.isSoundEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = BuzzerGold,
                            uncheckedTrackColor = Color(0xFF222834)
                        ),
                        modifier = Modifier.testTag("sound_toggle")
                    )
                }

                // Vibration Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GameSurfaceElevated, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = BuzzerGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "الاهتزاز اللمسي",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "اهتزاز فوز البوزير ونقل الإجابة",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = {
                            vibrationEnabled = it
                            feedbackManager.isVibrationEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = BuzzerGold,
                            uncheckedTrackColor = Color(0xFF222834)
                        ),
                        modifier = Modifier.testTag("vibration_toggle")
                    )
                }

                Text(
                    text = "يراعي التطبيق وضع الصامت للجهاز تلقائياً عند تفعيل الصوت",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
