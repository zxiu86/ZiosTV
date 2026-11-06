package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BuzzerGold
import com.example.ui.theme.BuzzerGoldBright
import com.example.ui.theme.GameBackground
import com.example.ui.theme.GameSurface
import com.example.ui.theme.GameSurfaceBorder
import com.example.ui.theme.GameSurfaceElevated
import com.example.ui.theme.QuizBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    playerName: String,
    onPlayerNameChange: (String) -> Unit,
    onCreateRoomClick: () -> Unit,
    onJoinRoomClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GameBackground)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GameSurfaceElevated)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary
                )
            }
        }

        // Hero Emblem & Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(BuzzerGold.copy(alpha = 0.25f), Color.Transparent)))
                    .border(2.dp, BuzzerGold.copy(alpha = 0.6f), CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.soual_jawab_icon_1793986916885),
                    contentDescription = "سؤال جواب Logo",
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "سؤال جواب",
                color = BuzzerGoldBright,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Text(
                text = "لعبة المسابقات والتحدي السريع • 4 لاعبين",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Name & Action Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.verticalGradient(listOf(GameSurfaceBorder, Color.Transparent))
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "اسم اللاعب",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = playerName,
                    onValueChange = onPlayerNameChange,
                    singleLine = true,
                    placeholder = { Text("أدخل اسمك هنا", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = BuzzerGold)
                    },
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
                        .testTag("player_name_input")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onCreateRoomClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BuzzerGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("create_room_button")
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إنشاء غرفة محلية (المضيف)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onJoinRoomClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = QuizBlue
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(QuizBlue.copy(alpha = 0.5f), QuizBlue))
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("join_room_button")
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الانضمام إلى غرفة موجودة",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Footer Note
        Text(
            text = "تتطلب اللعبة اتصال جميع الهواتف بشبكة Wi-Fi نفسها • لا حاجة للإنترنت",
            color = TextMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
