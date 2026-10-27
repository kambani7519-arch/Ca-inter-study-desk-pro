package com.example.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreUserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val email: String = "",
  val fullName: String = "",
  val attempt: String = "Jan 2027",
  val targetGroup: String = "Both Groups",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreTopicProgress(
  @DocumentId val id: String = "",
  val userId: String = "",
  val topicId: String = "",
  val paperId: Int = 1,
  val chapterName: String = "",
  val topicTitle: String = "",
  val icaiRef: String = "",
  val isCustom: Boolean = false,
  val status: String = "PENDING",
  val notes: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreMistakeVaultItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val mistakeId: String = "",
  val paperId: Int = 1,
  val subjectName: String = "",
  val topicOrStandard: String = "",
  val question: String = "",
  val studentAttempt: String = "",
  val marksLost: Double = 0.0,
  val totalMarks: Double = 0.0,
  val missingKeywords: String = "",
  val toppersSolution: String = "",
  val reviewIntervalDays: Int = 1,
  val nextReviewDate: Timestamp? = null,
  val isMastered: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreTestRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val testId: String = "",
  val paperId: Int = 1,
  val paperName: String = "",
  val chapterOrTopic: String = "",
  val totalMarks: Int = 0,
  val marksScored: Double = 0.0,
  val mcqScore: Double = 0.0,
  val descriptiveScore: Double = 0.0,
  @ServerTimestamp val dateTaken: Timestamp? = null,
  val summaryNotes: String = ""
)
