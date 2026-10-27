package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.data.ai.GeminiApiClient
import com.example.data.repository.AuthRepository
import com.example.data.repository.StudyRepository
import com.example.ui.MainAppScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.theme.CAInterDeskTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val apiClient = GeminiApiClient(this)
    val authRepository = AuthRepository(this)
    val studyRepository = StudyRepository(this)

    com.example.notifications.SpacedRepetitionNotificationManager.createNotificationChannel(this)
    if (com.example.notifications.SpacedRepetitionNotificationManager.isNotificationsEnabled(this)) {
      com.example.notifications.SpacedRepetitionNotificationManager.scheduleDailyReviewAlarm(this)
    }

    setContent {
      CAInterDeskTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val currentUser by authRepository.currentUser.collectAsState()
          var isCheckingSession by remember { mutableStateOf(true) }
          val scope = rememberCoroutineScope()

          LaunchedEffect(Unit) {
            authRepository.checkExistingSession()
            if (currentUser != null) {
              studyRepository.ensureTopicsSeeded(currentUser!!.id)
            }
            isCheckingSession = false
          }

          LaunchedEffect(currentUser) {
            if (currentUser != null) {
              studyRepository.ensureTopicsSeeded(currentUser!!.id)
            }
          }

          if (isCheckingSession) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.fillMaxSize()
            ) {
              CircularProgressIndicator()
            }
          } else {
            if (currentUser != null) {
              MainAppScreen(
                user = currentUser!!,
                apiClient = apiClient,
                studyRepository = studyRepository,
                authRepository = authRepository,
                onLogout = {
                  scope.launch {
                    val credentialManager = androidx.credentials.CredentialManager.create(this@MainActivity)
                    com.example.data.auth.GoogleAuthManager.signOut(this@MainActivity, credentialManager, {}, scope)
                    authRepository.logout()
                  }
                }
              )
            } else {
              AuthScreen(
                authRepository = authRepository,
                onAuthSuccess = {
                  // Session updated in StateFlow
                }
              )
            }
          }
        }
      }
    }
  }
}
