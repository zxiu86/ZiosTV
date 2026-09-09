package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MousePointerState
import com.example.data.model.TvDevice
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.FaintBlueGrid
import com.example.ui.theme.FaintGridLine
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TvMouseTrackpad(
  device: TvDevice?,
  mousePointerState: MousePointerState,
  isMouseModeEnabled: Boolean,
  onMouseMoved: (Float, Float) -> Unit,
  onMouseClick: (Boolean) -> Unit,
  onToggleMouseMode: (Boolean) -> Unit,
  onSensitivityChanged: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  var activeTouchOffset by remember { mutableStateOf<Offset?>(null) }

  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseAlpha"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Top Control Bar: Mouse Mode Switch & TV Support Badge
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurface),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkSurfaceBorder, FaintBlueGrid))),
      modifier = Modifier.fillMaxWidth().testTag("trackpad_header_card")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(if (isMouseModeEnabled) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceElevated),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isMouseModeEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription = "Mouse Visibility",
              tint = if (isMouseModeEnabled) NeonCyan else TextMuted,
              modifier = Modifier.size(20.dp)
            )
          }

          Column {
            Text(
              text = "إظهار الماوس على التلفاز",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary
            )
            Text(
              text = if (device?.isSupportsMouse == true) "مدعوم: ${device.brand.displayName}" else "يدعم التلفزيونات الذكية النشطة",
              style = MaterialTheme.typography.bodySmall,
              color = if (isMouseModeEnabled) NeonCyan.copy(alpha = 0.85f) else TextMuted
            )
          }
        }

        Switch(
          checked = isMouseModeEnabled,
          onCheckedChange = onToggleMouseMode,
          colors = SwitchDefaults.colors(
            checkedThumbColor = NeonCyan,
            checkedTrackColor = NeonCyanDark.copy(alpha = 0.4f),
            uncheckedThumbColor = TextMuted,
            uncheckedTrackColor = DarkSurfaceElevated
          ),
          modifier = Modifier.testTag("toggle_mouse_switch")
        )
      }
    }

    // Interactive Virtual TV Screen Mini-Preview (Simulates TV cursor sync in real-time)
    AnimatedVisibility(visible = isMouseModeEnabled) {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBackground),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(FaintBlueGrid, DarkSurfaceBorder))),
        modifier = Modifier
          .fillMaxWidth()
          .height(105.dp)
          .testTag("tv_screen_preview")
      ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          val previewWidth = maxWidth
          val previewHeight = maxHeight

          // Subtle TV screen scanlines & bezel
          Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color(0xFF070B14))
            // Outer TV display frame
            drawRoundRect(
              color = Color(0x3338BDF8),
              size = size,
              style = Stroke(width = 2f)
            )
          }

          // TV Screen Status Text
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "شاشة التلفاز المدعوم (${device?.name?.take(22) ?: "Smart TV"})",
              color = TextMuted,
              fontSize = 11.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(NeonCyan.copy(alpha = pulseAlpha))
              )
              Text(
                text = "مؤشر نشط",
                color = NeonCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }

          // Cursor Dot on Virtual TV
          val cursorX = previewWidth * mousePointerState.x
          val cursorY = previewHeight * mousePointerState.y

          Box(
            modifier = Modifier
              .offset(x = cursorX - 10.dp, y = cursorY - 10.dp)
              .size(20.dp),
            contentAlignment = Alignment.Center
          ) {
            // Glowing Halo
            Box(
              modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(NeonCyan.copy(alpha = 0.35f * pulseAlpha))
            )
            // Cursor Center Dot
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(NeonCyan)
            )
          }
        }
      }
    }

    // Sensitivity Selector Chips
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
        Text(text = "سرعة المؤشر:", color = TextSecondary, fontSize = 12.sp)
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val sensitivities = listOf(
          Triple("دقيق", 0.6f, "sens_precision"),
          Triple("عادي", 1.0f, "sens_normal"),
          Triple("سريع", 1.8f, "sens_fast")
        )
        sensitivities.forEach { (label, value, tag) ->
          val isSelected = (mousePointerState.sensitivity - value).let { kotlin.math.abs(it) < 0.2f }
          FilterChip(
            selected = isSelected,
            onClick = { onSensitivityChanged(value) },
            label = { Text(label, fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
              selectedLabelColor = NeonCyan,
              containerColor = DarkSurface,
              labelColor = TextMuted
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = isSelected,
              borderColor = if (isSelected) NeonCyan else DarkSurfaceBorder
            ),
            modifier = Modifier.testTag(tag)
          )
        }
      }
    }

    // PRIMARY TRACKPAD SURFACE (مربع فارغ بمربعات بلون أزرق خافت)
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(DarkBackground)
        .border(
          width = 1.5.dp,
          brush = Brush.radialGradient(
            colors = listOf(NeonCyan.copy(alpha = 0.3f), FaintBlueGrid, DarkSurfaceBorder)
          ),
          shape = RoundedCornerShape(24.dp)
        )
        .testTag("tv_trackpad_surface")
        .pointerInput(Unit) {
          detectTapGestures(
            onTap = { offset ->
              activeTouchOffset = offset
              onMouseClick(true) // Left click on tap
            },
            onLongPress = { offset ->
              activeTouchOffset = offset
              onMouseClick(false) // Right click on long press
            }
          )
        }
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = { offset ->
              activeTouchOffset = offset
            },
            onDragEnd = {
              activeTouchOffset = null
            },
            onDragCancel = {
              activeTouchOffset = null
            },
            onDrag = { change, dragAmount ->
              change.consume()
              activeTouchOffset = change.position
              onMouseMoved(dragAmount.x, dragAmount.y)
            }
          )
        }
    ) {
      // Draw the faint blue square grid lines ("مربعات بلون ازرق خافت")
      Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 32.dp.toPx()
        val numCols = (size.width / gridSize).toInt() + 1
        val numRows = (size.height / gridSize).toInt() + 1

        // Vertical faint blue lines
        for (i in 0..numCols) {
          val x = i * gridSize
          drawLine(
            color = FaintGridLine,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
          )
        }

        // Horizontal faint blue lines
        for (j in 0..numRows) {
          val y = j * gridSize
          drawLine(
            color = FaintGridLine,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
          )
        }

        // Subtle center crosshair guide
        val centerX = size.width / 2
        val centerY = size.height / 2
        drawLine(
          color = NeonCyan.copy(alpha = 0.2f),
          start = Offset(centerX - 16.dp.toPx(), centerY),
          end = Offset(centerX + 16.dp.toPx(), centerY),
          strokeWidth = 1.5f
        )
        drawLine(
          color = NeonCyan.copy(alpha = 0.2f),
          start = Offset(centerX, centerY - 16.dp.toPx()),
          end = Offset(centerX, centerY + 16.dp.toPx()),
          strokeWidth = 1.5f
        )
      }

      // Active user finger touch ripple effect
      activeTouchOffset?.let { touchPos ->
        Box(
          modifier = Modifier
            .offset(x = (touchPos.x / 2.7f).dp - 24.dp, y = (touchPos.y / 2.7f).dp - 24.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                colors = listOf(NeonCyan.copy(alpha = 0.45f), Color.Transparent)
              )
            )
        )
      }

      // Central Instructional Watermark
      Column(
        modifier = Modifier
          .align(Alignment.Center)
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(
          imageVector = Icons.Default.TouchApp,
          contentDescription = null,
          tint = NeonCyan.copy(alpha = 0.25f),
          modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "اسحب للتحكم بالماوس بسلاسة وسرعة\nانقر للنقر الأيسر • ضغطة مطولة للنقر الأيمن",
          color = TextMuted.copy(alpha = 0.6f),
          fontSize = 11.sp,
          textAlign = TextAlign.Center,
          lineHeight = 16.sp
        )
      }
    }

    // Bottom Physical Mouse Buttons (Left Click, Scroll Strip, Right Click)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Left Click Button
      ElevatedButton(
        onClick = { onMouseClick(true) },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.elevatedButtonColors(
          containerColor = DarkSurfaceElevated,
          contentColor = NeonCyan
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
          brush = Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.4f), DarkSurfaceBorder))
        ),
        modifier = Modifier
          .weight(1.3f)
          .fillMaxSize()
          .testTag("left_click_button")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(imageVector = Icons.Default.AdsClick, contentDescription = null, modifier = Modifier.size(18.dp))
          Text(text = "نقر أيسر", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }

      // Scroll Up/Down Quick Action
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkSurfaceBorder, FaintBlueGrid))),
        modifier = Modifier
          .weight(0.9f)
          .fillMaxSize()
      ) {
        Row(
          modifier = Modifier.fillMaxSize(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = { onMouseMoved(0f, -45f) },
            modifier = Modifier.testTag("scroll_up_btn")
          ) {
            Icon(
              imageVector = Icons.Default.UnfoldMore,
              contentDescription = "Scroll",
              tint = NeonCyan,
              modifier = Modifier.size(20.dp)
            )
          }
          Text(text = "تمرير", color = TextSecondary, fontSize = 11.sp)
        }
      }

      // Right Click Button
      ElevatedButton(
        onClick = { onMouseClick(false) },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.elevatedButtonColors(
          containerColor = DarkSurfaceElevated,
          contentColor = TextPrimary
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
          brush = Brush.linearGradient(listOf(DarkSurfaceBorder, FaintBlueGrid))
        ),
        modifier = Modifier
          .weight(1.3f)
          .fillMaxSize()
          .testTag("right_click_button")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(imageVector = Icons.Default.Mouse, contentDescription = null, modifier = Modifier.size(18.dp))
          Text(text = "نقر أيمن", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
      }
    }
  }
}
