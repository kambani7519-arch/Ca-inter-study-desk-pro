package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

object SpacedRepetitionNotificationManager {

  const val CHANNEL_ID = "ca_spaced_repetition_channel"
  private const val NOTIFICATION_ID = 1001
  private const val PREFS_NAME = "ca_notification_prefs"
  private const val KEY_NOTIFICATIONS_ENABLED = "spaced_notifications_enabled"

  fun isNotificationsEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
  }

  fun setNotificationsEnabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    if (enabled) {
      scheduleDailyReviewAlarm(context)
    } else {
      cancelDailyReviewAlarm(context)
    }
  }

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "CA Inter Spaced Repetition Reminders"
      val descriptionText = "Daily nudges for topics due on the 1, 3, 7, 14, and 30-day review schedule"
      val importance = NotificationManager.IMPORTANCE_HIGH
      val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
        description = descriptionText
        enableVibration(true)
      }
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }

  fun showDueReviewNotification(
    context: Context,
    dueCount: Int,
    sampleTopicOrPaper: String = "Corporate Law & Auditing"
  ) {
    createNotificationChannel(context)

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      putExtra("EXTRA_NAVIGATE_TO", "vault")
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val title = if (dueCount > 0) {
      "CA Inter Revision Due! ($dueCount ${if (dueCount == 1) "Topic" else "Topics"})"
    } else {
      "CA Inter Study Desk • Daily Spaced Retention"
    }

    val contentText = if (dueCount > 0) {
      "Time for your 1-3-7-14-30d review. Don't lose marks on $sampleTopicOrPaper for Jan 2027!"
    } else {
      "All caught up! Quick 5-minute rapid-fire audio revision is ready on your desk."
    }

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(contentText)
      .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)

    try {
      val notificationManager = NotificationManagerCompat.from(context)
      notificationManager.notify(NOTIFICATION_ID, builder.build())
    } catch (e: SecurityException) {
      // Permission not granted on Android 13+
      e.printStackTrace()
    }
  }

  fun scheduleDailyReviewAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, SpacedRepetitionReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      2001,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Schedule for 8:00 AM daily
    val calendar = Calendar.getInstance().apply {
      timeInMillis = System.currentTimeMillis()
      set(Calendar.HOUR_OF_DAY, 8)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      if (timeInMillis <= System.currentTimeMillis()) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }

    alarmManager.setInexactRepeating(
      AlarmManager.RTC_WAKEUP,
      calendar.timeInMillis,
      AlarmManager.INTERVAL_DAY,
      pendingIntent
    )
  }

  fun cancelDailyReviewAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, SpacedRepetitionReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      2001,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
  }
}
