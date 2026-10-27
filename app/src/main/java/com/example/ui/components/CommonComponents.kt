package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TopicStatus
import com.example.ui.theme.StatusDone
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusRevise

@Composable
fun StatusBadge(status: TopicStatus, modifier: Modifier = Modifier) {
  val (bgColor, textColor, text, icon) = when (status) {
    TopicStatus.PENDING -> Quad(
      MaterialTheme.colorScheme.surfaceVariant,
      MaterialTheme.colorScheme.onSurfaceVariant,
      "Pending",
      Icons.Default.HourglassEmpty
    )
    TopicStatus.DONE -> Quad(
      StatusDone.copy(alpha = 0.15f),
      StatusDone,
      "Done",
      Icons.Default.CheckCircle
    )
    TopicStatus.REVISE -> Quad(
      StatusRevise.copy(alpha = 0.15f),
      StatusRevise,
      "Revise",
      Icons.Default.Repeat
    )
  }

  Surface(
    color = bgColor,
    shape = RoundedCornerShape(12.dp),
    modifier = modifier
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = text,
        tint = textColor,
        modifier = Modifier.size(14.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = text,
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@Composable
fun PaperTag(paperId: Int, modifier: Modifier = Modifier) {
  val code = "P$paperId"
  val color = when (paperId) {
    1 -> Color(0xFF2563EB)
    2 -> Color(0xFF7C3AED)
    3 -> Color(0xFFDC2626)
    4 -> Color(0xFFD97706)
    5 -> Color(0xFF059669)
    else -> Color(0xFF0891B2)
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(color.copy(alpha = 0.12f))
      .padding(horizontal = 6.dp, vertical = 2.dp)
  ) {
    Text(
      text = code,
      color = color,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
fun ProgressBarWithLabel(
  label: String,
  progress: Float,
  color: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = "${(progress * 100).toInt()}%",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    LinearProgressIndicator(
      progress = { progress.coerceIn(0f, 1f) },
      color = color,
      trackColor = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(3.dp))
    )
  }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
