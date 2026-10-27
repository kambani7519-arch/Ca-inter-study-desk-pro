package com.example.ui.desk

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.GeminiApiClient
import com.example.data.local.UserEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.StudyRepository
import com.example.ui.focus.FocusModeScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.rapidfire.RapidFireScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyDeskScreen(
  user: UserEntity,
  apiClient: GeminiApiClient,
  studyRepository: StudyRepository,
  authRepository: AuthRepository,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedDeskTab by remember { mutableStateOf(0) } // 0 = Audio Rapid-Fire, 1 = Focus Timer, 2 = Aspirant Profile

  Scaffold(
    topBar = {
      Column {
        TopAppBar(
          title = {
            Text(
              text = when (selectedDeskTab) {
                0 -> "Commute Voice Rapid-Fire"
                1 -> "Focus Mode & Pomodoro Desk"
                else -> "Aspirant Profile & Keys"
              },
              style = MaterialTheme.typography.titleMedium
            )
          }
        )
        PrimaryTabRow(
          selectedTabIndex = selectedDeskTab,
          modifier = Modifier.fillMaxWidth()
        ) {
          Tab(
            selected = selectedDeskTab == 0,
            onClick = { selectedDeskTab = 0 },
            text = { Text("Audio Drill", fontSize = 12.sp) },
            icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp)) }
          )
          Tab(
            selected = selectedDeskTab == 1,
            onClick = { selectedDeskTab = 1 },
            text = { Text("Focus Timer", fontSize = 12.sp) },
            icon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp)) }
          )
          Tab(
            selected = selectedDeskTab == 2,
            onClick = { selectedDeskTab = 2 },
            text = { Text("Profile & Keys", fontSize = 12.sp) },
            icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
          )
        }
      }
    },
    modifier = modifier
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (selectedDeskTab) {
        0 -> RapidFireScreen(userId = user.id, studyRepository = studyRepository)
        1 -> FocusModeScreen()
        2 -> ProfileScreen(
          user = user,
          apiClient = apiClient,
          studyRepository = studyRepository,
          authRepository = authRepository,
          onLogout = onLogout
        )
      }
    }
  }
}
