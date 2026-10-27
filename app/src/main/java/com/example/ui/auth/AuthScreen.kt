package com.example.ui.auth

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.auth.GoogleAuthManager
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
  authRepository: AuthRepository,
  onAuthSuccess: () -> Unit
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val credentialManager = remember { CredentialManager.create(context) }

  var isLoginMode by remember { mutableStateOf(true) }
  var fullName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var selectedAttempt by remember { mutableStateOf("Jan 2027") }
  var selectedGroup by remember { mutableStateOf("Both Groups") }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  // Attempt auto sign in on launch
  LaunchedEffect(Unit) {
    GoogleAuthManager.attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = { firebaseUser ->
        scope.launch {
          val userEmail = firebaseUser.email ?: "aspirant@cainter.pro"
          val userName = firebaseUser.displayName ?: "CA Aspirant"
          authRepository.loginWithGoogle(userEmail, userName, firebaseUser.uid)
          onAuthSuccess()
        }
      },
      onUnauthenticated = {},
      scope = scope
    )
  }

  val gradientBrush = Brush.verticalGradient(
    colors = listOf(
      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
      MaterialTheme.colorScheme.background
    )
  )

  Scaffold { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(gradientBrush)
        .padding(innerPadding)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(24.dp)
      ) {
        Spacer(modifier = Modifier.height(24.dp))

        // App Branding Badge
        Surface(
          color = MaterialTheme.colorScheme.primary,
          shape = RoundedCornerShape(16.dp),
          shadowElevation = 6.dp,
          modifier = Modifier.size(72.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.School,
              contentDescription = "Logo",
              tint = Color.White,
              modifier = Modifier.size(40.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "CA Inter Study Desk Pro",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = "Strict ICAI New Scheme Suite • Jan 2027",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Google Sign-In Card
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Cloud Database Sync & Backup",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Sync syllabus, mistake vault, and tests to Firebase Firestore across devices.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            FilledTonalButton(
              onClick = {
                if (activity != null) {
                  isLoading = true
                  errorMessage = null
                  GoogleAuthManager.onGoogleSignInClicked(
                    activity = activity,
                    credentialManager = credentialManager,
                    onAuthSuccess = { firebaseUser ->
                      scope.launch {
                        val userEmail = firebaseUser.email ?: "aspirant@cainter.pro"
                        val userName = firebaseUser.displayName ?: "CA Aspirant"
                        authRepository.loginWithGoogle(userEmail, userName, firebaseUser.uid)
                        isLoading = false
                        onAuthSuccess()
                      }
                    },
                    onAuthError = { err ->
                      isLoading = false
                      errorMessage = err
                    },
                    scope = scope,
                    onAuthCancelled = { isLoading = false }
                  )
                }
              },
              enabled = !isLoading,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("google_signin_button")
            ) {
              Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Sign in with Google", fontWeight = FontWeight.Bold)
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
          HorizontalDivider(modifier = Modifier.weight(1f))
          Text(
            text = " OR LOCAL ACCOUNT ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
          )
          HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Switcher (Login vs Sign Up)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          SegmentedButton(
            selected = isLoginMode,
            onClick = {
              isLoginMode = true
              errorMessage = null
            },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
          ) {
            Text("Sign In")
          }
          SegmentedButton(
            selected = !isLoginMode,
            onClick = {
              isLoginMode = false
              errorMessage = null
            },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
          ) {
            Text("Sign Up")
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp)
          ) {
            if (!isLoginMode) {
              OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Student Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("auth_fullname_input")
              )
              Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
              value = email,
              onValueChange = { email = it },
              label = { Text("Email Address") },
              leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_email_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
              value = password,
              onValueChange = { password = it },
              label = { Text("Password") },
              leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
              trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                  Icon(
                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle password visibility"
                  )
                }
              },
              visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_password_input")
            )

            if (!isLoginMode) {
              Spacer(modifier = Modifier.height(14.dp))
              Text(
                text = "Target Attempt",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                listOf("Jan 2027", "May 2027", "Sept 2027").forEach { att ->
                  FilterChip(
                    selected = selectedAttempt == att,
                    onClick = { selectedAttempt = att },
                    label = { Text(att, fontSize = 12.sp) }
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "Group Enrollment",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                listOf("Both Groups", "Group 1", "Group 2").forEach { grp ->
                  FilterChip(
                    selected = selectedGroup == grp,
                    onClick = { selectedGroup = grp },
                    label = { Text(grp, fontSize = 12.sp) }
                  )
                }
              }
            }

            if (errorMessage != null) {
              Spacer(modifier = Modifier.height(12.dp))
              Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = errorMessage!!,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.padding(10.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
              onClick = {
                scope.launch {
                  isLoading = true
                  errorMessage = null
                  val result = if (isLoginMode) {
                    authRepository.login(email, password)
                  } else {
                    authRepository.register(fullName, email, password, selectedAttempt, selectedGroup)
                  }
                  isLoading = false
                  if (result.isSuccess) {
                    onAuthSuccess()
                  } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Authentication failed."
                  }
                }
              },
              enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("auth_submit_button")
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  color = Color.White,
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp
                )
              } else {
                Text(if (isLoginMode) "Sign In Locally" else "Create Aspirant Account")
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Demo Access button
            OutlinedButton(
              onClick = {
                scope.launch {
                  isLoading = true
                  authRepository.checkExistingSession()
                  isLoading = false
                  onAuthSuccess()
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("demo_login_button")
            ) {
              Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Quick Demo Student Access (Jan 2027)")
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Dual Persistence: Local Room SQLite with Firebase Firestore Cloud Synchronization.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}
