package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object GoogleAuthManager {

  private const val TAG = "GoogleAuth"

  val currentUser: FirebaseUser?
    get() = Firebase.auth.currentUser

  val isUserSignedIn: Boolean
    get() = Firebase.auth.currentUser != null

  fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: (FirebaseUser) -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
  ) {
    val existingUser = Firebase.auth.currentUser
    if (existingUser != null) {
      onAuthSuccess(existingUser)
      return
    }

    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      Log.w(TAG, "default_web_client_id not available: ${e.message}")
      onUnauthenticated()
      return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    scope.launch {
      try {
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          val authResult = Firebase.auth.signInWithCredential(authCredential).await()
          authResult.user?.let(onAuthSuccess) ?: onUnauthenticated()
        } else {
          onUnauthenticated()
        }
      } catch (e: Exception) {
        onUnauthenticated()
      }
    }
  }

  fun onGoogleSignInClicked(
    activity: Activity,
    credentialManager: CredentialManager,
    onAuthSuccess: (FirebaseUser) -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
  ) {
    val clientId = try {
      activity.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
      return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    scope.launch {
      try {
        val result = credentialManager.getCredential(activity, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          val authResult = Firebase.auth.signInWithCredential(authCredential).await()
          val user = authResult.user
          if (user != null) {
            onAuthSuccess(user)
          } else {
            onAuthError("Firebase authentication failed: User is null.")
          }
        } else {
          onAuthError("Unexpected credential type returned.")
        }
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
        onAuthCancelled()
      } catch (e: Exception) {
        Log.e(TAG, "Google Sign-In failed", e)
        onAuthError(e.localizedMessage ?: "Google Sign-In failed")
      }
    }
  }

  fun signOut(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
  ) {
    Firebase.auth.signOut()
    scope.launch {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.e(TAG, "Failed to clear credential state", e)
      } finally {
        onSignOutComplete()
      }
    }
  }
}
