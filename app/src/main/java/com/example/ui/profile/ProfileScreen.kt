package com.example.ui.profile

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.ai.GeminiApiClient
import com.example.data.auth.GoogleAuthManager
import com.example.data.local.UserEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.StudyRepository
import com.example.ui.components.ApiKeyPoolDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
  user: UserEntity?,
  apiClient: GeminiApiClient,
  studyRepository: StudyRepository,
  authRepository: AuthRepository,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val credentialManager = remember { CredentialManager.create(context) }

  var showKeyPoolDialog by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  val activeKeyIdx by apiClient.activeKeyIndexFlow.collectAsState()
  val poolSize by apiClient.poolSizeFlow.collectAsState()

  var isSyncingCloud by remember { mutableStateOf(false) }
  var syncStatusMessage by remember { mutableStateOf<String?>(null) }
  var isGoogleConnected by remember { mutableStateOf(GoogleAuthManager.isUserSignedIn) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Aspirant Profile & Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("CA Inter Study Desk Pro", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
          }
        }
      )
    },
    modifier = modifier
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      // User Profile Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            modifier = Modifier.size(64.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = (user?.fullName?.take(1) ?: "C").uppercase(),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = user?.fullName ?: "CA Aspirant",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = user?.email ?: "student@cainter.pro",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          Spacer(modifier = Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("Target Attempt", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Spacer(modifier = Modifier.height(2.dp))
              Text(user?.attempt ?: "Jan 2027", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("Group", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Spacer(modifier = Modifier.height(2.dp))
              Text(user?.targetGroup ?: "Both Groups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Cloud Firestore Synchronization Card
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                tint = if (isGoogleConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text("Firebase Firestore Cloud Sync", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                  text = if (isGoogleConnected) "Real-time sync to cloud database active" else "Connect Google to enable multi-device sync",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Surface(
              color = if (isGoogleConnected) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = if (isGoogleConnected) "CONNECTED" else "LOCAL ONLY",
                color = if (isGoogleConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          if (isGoogleConnected) {
            val googleUser = GoogleAuthManager.currentUser
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = "Google Account: ${googleUser?.email ?: "Signed In"}",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "Cloud DB: ai-studio-android-cainters...",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 10.sp
                )
              }
            }

            if (syncStatusMessage != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = syncStatusMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = {
                if (user != null) {
                  scope.launch {
                    isSyncingCloud = true
                    syncStatusMessage = "Synchronizing with Firebase Firestore..."
                    val result = studyRepository.syncAllWithCloud(user.id)
                    isSyncingCloud = false
                    if (result.isSuccess) {
                      syncStatusMessage = "Synced ${result.getOrNull()} syllabus items & mistakes to Firestore!"
                    } else {
                      syncStatusMessage = "Sync failed: ${result.exceptionOrNull()?.message}"
                    }
                  }
                }
              },
              enabled = !isSyncingCloud,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("sync_to_cloud_button")
            ) {
              if (isSyncingCloud) {
                CircularProgressIndicator(
                  color = Color.White,
                  modifier = Modifier.size(18.dp),
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Syncing to Firestore...")
              } else {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Local Progress to Cloud Now")
              }
            }
          } else {
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(
              onClick = {
                if (activity != null) {
                  GoogleAuthManager.onGoogleSignInClicked(
                    activity = activity,
                    credentialManager = credentialManager,
                    onAuthSuccess = { firebaseUser ->
                      isGoogleConnected = true
                      scope.launch {
                        val email = firebaseUser.email ?: "aspirant@cainter.pro"
                        val name = firebaseUser.displayName ?: "CA Aspirant"
                        authRepository.loginWithGoogle(email, name, firebaseUser.uid)
                        if (user != null) {
                          studyRepository.syncAllWithCloud(user.id)
                        }
                      }
                    },
                    onAuthError = { err ->
                      syncStatusMessage = err
                    },
                    scope = scope
                  )
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("connect_google_account_btn")
            ) {
              Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Connect Google Account for Cloud Sync")
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // API Key Auto-Rotation Pool Card
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text("Gemini API Key Pool", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Auto-rotates upon HTTP 429 quota exhaustion", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
            Surface(
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = "${activeKeyIdx + 1}/$poolSize Active",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = { showKeyPoolDialog = true },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("manage_key_pool_btn")
          ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Manage API Key Pool & Add Keys")
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Spaced Repetition Push Notification Nudge Card
      var notificationsEnabled by remember {
        mutableStateOf(com.example.notifications.SpacedRepetitionNotificationManager.isNotificationsEnabled(context))
      }

      val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
      ) { isGranted ->
        if (isGranted) {
          notificationsEnabled = true
          com.example.notifications.SpacedRepetitionNotificationManager.setNotificationsEnabled(context, true)
        }
      }

      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text("Spaced Repetition Nudges", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("1, 3, 7, 14, 30-day review schedule", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }

            Switch(
              checked = notificationsEnabled,
              onCheckedChange = { checked ->
                if (checked && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                  notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                notificationsEnabled = checked
                com.example.notifications.SpacedRepetitionNotificationManager.setNotificationsEnabled(context, checked)
              },
              modifier = Modifier.testTag("toggle_spaced_notifications")
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Automated daily push alerts notify you when Mistake Vault questions reach their spaced retention milestones so standards & sections remain fresh for Jan 2027.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
          )

          Spacer(modifier = Modifier.height(10.dp))
          OutlinedButton(
            onClick = {
              if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
              }
              com.example.notifications.SpacedRepetitionNotificationManager.showDueReviewNotification(
                context = context,
                dueCount = 3,
                sampleTopicOrPaper = "Companies Act Sec 123 & SA 505"
              )
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("test_spaced_notification_btn")
          ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Send Test Spaced Repetition Nudge Now", fontSize = 12.sp)
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Offline Access & Data Persistence Card
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF10B981))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Offline Engine & Room Database", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Text("All 6 syllabus papers, questions, and mistake vault cached locally", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            color = Color(0xFF10B981).copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(10.dp)
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Full Offline Availability Active", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Logout Button
      OutlinedButton(
        onClick = {
          scope.launch {
            GoogleAuthManager.signOut(
              context = context,
              credentialManager = credentialManager,
              onSignOutComplete = {},
              scope = scope
            )
            authRepository.logout()
            onLogout()
          }
        },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("logout_button")
      ) {
        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Log Out Account")
      }
    }
  }

  if (showKeyPoolDialog) {
    ApiKeyPoolDialog(
      apiClient = apiClient,
      onDismiss = { showKeyPoolDialog = false }
    )
  }
}
