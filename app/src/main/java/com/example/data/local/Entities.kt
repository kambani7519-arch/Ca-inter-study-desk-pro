package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "users",
  indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val email: String,
  val passwordHash: String,
  val salt: String,
  val fullName: String,
  val attempt: String = "Jan 2027",
  val targetGroup: String = "Both Groups",
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "topics_progress",
  indices = [Index(value = ["userId", "paperId"])]
)
data class TopicProgressEntity(
  @PrimaryKey val uid: String, // "${userId}_${topicId}"
  val userId: Int,
  val topicId: String,
  val paperId: Int,
  val chapterName: String,
  val topicTitle: String,
  val icaiRef: String,
  val isCustom: Boolean,
  val status: String, // PENDING, DONE, REVISE
  val notes: String = "",
  val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "mistake_vault",
  indices = [Index(value = ["userId", "paperId"])]
)
data class MistakeVaultEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val userId: Int,
  val paperId: Int,
  val subjectName: String,
  val topicOrStandard: String,
  val question: String,
  val studentAttempt: String,
  val marksLost: Float,
  val totalMarks: Float,
  val missingKeywords: String,
  val toppersSolution: String,
  val reviewIntervalDays: Int = 1, // 1 -> 3 -> 7 -> 14 -> 30
  val nextReviewDate: Long = System.currentTimeMillis() + (1L * 24 * 60 * 60 * 1000),
  val isMastered: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "test_history",
  indices = [Index(value = ["userId", "dateTaken"])]
)
data class TestHistoryEntity(
  @PrimaryKey val testId: String,
  val userId: Int,
  val paperId: Int,
  val paperName: String,
  val chapterOrTopic: String,
  val totalMarks: Int,
  val marksScored: Float,
  val mcqScore: Float,
  val descriptiveScore: Float,
  val dateTaken: Long = System.currentTimeMillis(),
  val summaryNotes: String = ""
)

@Entity(
  tableName = "study_schedule",
  indices = [Index(value = ["userId", "scheduledDate"])]
)
data class StudyScheduleEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val userId: Int,
  val topicId: String,
  val paperId: Int,
  val chapterName: String,
  val topicTitle: String,
  val icaiRef: String,
  val scheduledDate: String, // YYYY-MM-DD
  val timeSlot: String = "Morning", // "Morning", "Afternoon", "Evening"
  val isCompleted: Boolean = false,
  val orderIndex: Int = 0,
  val createdAt: Long = System.currentTimeMillis()
)

