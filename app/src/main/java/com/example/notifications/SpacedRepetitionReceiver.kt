package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SpacedRepetitionReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (!SpacedRepetitionNotificationManager.isNotificationsEnabled(context)) {
      return
    }

    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = AppDatabase.getDatabase(context)
        val studyDao = db.studyDao()
        val now = System.currentTimeMillis()

        val prefs = context.getSharedPreferences("ca_auth_session", Context.MODE_PRIVATE)
        val currentUserId = prefs.getInt("logged_in_user_id", -1)

        val dueMistakes = if (currentUserId != -1) {
          studyDao.getMistakesDueForReview(currentUserId, now).firstOrNull() ?: emptyList()
        } else {
          emptyList()
        }

        val dueCount = dueMistakes.size
        val sampleSubject = dueMistakes.firstOrNull()?.topicOrStandard ?: "ICAI Standards & Sections"

        SpacedRepetitionNotificationManager.showDueReviewNotification(
          context = context,
          dueCount = dueCount,
          sampleTopicOrPaper = sampleSubject
        )
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        pendingResult.finish()
      }
    }
  }
}
