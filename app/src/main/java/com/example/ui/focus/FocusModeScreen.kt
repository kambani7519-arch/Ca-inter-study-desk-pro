package com.example.ui.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen(
  modifier: Modifier = Modifier
) {
  var selectedWorkMinutes by remember { mutableStateOf(25) }
  var selectedBreakMinutes by remember { mutableStateOf(5) }
  var isBreakMode by remember { mutableStateOf(false) }

  var remainingSeconds by remember { mutableStateOf(selectedWorkMinutes * 60) }
  var isTimerRunning by remember { mutableStateOf(false) }
  var completedPomodoros by remember { mutableStateOf(0) }
  var strictFocusLockActive by remember { mutableStateOf(false) }

  val quotes = remember {
    listOf(
      "Every hour of discipline today adds to your rank in January 2027.",
      "Work hard until the prefix 'CA' is officially earned before your name.",
      "The pain of discipline is far less than the pain of regret.",
      "Balance sheets don't tally on their own — consistency and precision create rank-holders.",
      "Master the standards today, sign the audit reports tomorrow."
    )
  }
  var currentQuoteIndex by remember { mutableStateOf(0) }

  // Countdown timer coroutine
  LaunchedEffect(isTimerRunning, remainingSeconds) {
    if (isTimerRunning && remainingSeconds > 0) {
      delay(1000)
      remainingSeconds--
      if (remainingSeconds % 300 == 0) {
        currentQuoteIndex = (currentQuoteIndex + 1) % quotes.size
      }
    } else if (isTimerRunning && remainingSeconds == 0) {
      isTimerRunning = false
      if (!isBreakMode) {
        completedPomodoros++
        isBreakMode = true
        remainingSeconds = selectedBreakMinutes * 60
      } else {
        isBreakMode = false
        remainingSeconds = selectedWorkMinutes * 60
      }
    }
  }

  val minutes = remainingSeconds / 60
  val seconds = remainingSeconds % 60
  val formattedTime = remember(remainingSeconds) {
    "%02d:%02d".format(minutes, seconds)
  }

  val totalSecondsForCurrent = if (isBreakMode) selectedBreakMinutes * 60 else selectedWorkMinutes * 60
  val progress = if (totalSecondsForCurrent > 0) 1f - (remainingSeconds.toFloat() / totalSecondsForCurrent) else 0f

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Focus Mode & Pomodoro Desk", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Distraction-Free Deep Study", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
          }
        }
      )
    },
    modifier = modifier
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Mode & Completed counter
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = if (isBreakMode) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(20.dp)
          ) {
            Text(
              text = if (isBreakMode) "☕ Break Time" else "📚 Deep Study Sprint",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = if (isBreakMode) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
          }

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "$completedPomodoros Sprints Done",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        // Circular Timer Display
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(260.dp)
            .padding(16.dp)
        ) {
          CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 10.dp,
            color = if (isBreakMode) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = formattedTime,
              style = MaterialTheme.typography.displayLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground
            )
            Text(
              text = if (isBreakMode) "Rest & recharge" else "Strict CA Concentration",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Inspirational quote card
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(14.dp)
          ) {
            Text(
              text = "\"${quotes[currentQuoteIndex]}\"",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        // Controls
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedButton(
              onClick = {
                isTimerRunning = false
                remainingSeconds = (if (isBreakMode) selectedBreakMinutes else selectedWorkMinutes) * 60
              },
              shape = CircleShape,
              modifier = Modifier.size(54.dp)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Reset")
            }

            Button(
              onClick = { isTimerRunning = !isTimerRunning },
              shape = CircleShape,
              modifier = Modifier
                .size(72.dp)
                .testTag("pomodoro_toggle_btn")
            ) {
              Icon(
                imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isTimerRunning) "Pause" else "Start",
                modifier = Modifier.size(32.dp)
              )
            }

            OutlinedButton(
              onClick = {
                isBreakMode = !isBreakMode
                isTimerRunning = false
                remainingSeconds = (if (isBreakMode) selectedBreakMinutes else selectedWorkMinutes) * 60
              },
              shape = CircleShape,
              modifier = Modifier.size(54.dp)
            ) {
              Icon(Icons.Default.SkipNext, contentDescription = "Skip mode")
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          // Duration preset chips
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            listOf(25, 45, 60).forEach { mins ->
              FilterChip(
                selected = selectedWorkMinutes == mins && !isBreakMode,
                onClick = {
                  selectedWorkMinutes = mins
                  if (!isBreakMode) {
                    isTimerRunning = false
                    remainingSeconds = mins * 60
                  }
                },
                label = { Text("${mins}m Study", fontSize = 11.sp) }
              )
            }
            listOf(5, 15).forEach { bMins ->
              FilterChip(
                selected = selectedBreakMinutes == bMins && isBreakMode,
                onClick = {
                  selectedBreakMinutes = bMins
                  if (isBreakMode) {
                    isTimerRunning = false
                    remainingSeconds = bMins * 60
                  }
                },
                label = { Text("${bMins}m Break", fontSize = 11.sp) }
              )
            }
          }
        }
      }
    }
  }
}
