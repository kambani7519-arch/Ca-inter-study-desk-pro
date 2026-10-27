package com.example.data.firestore

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreSyncRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  fun isUserSignedIn(): Boolean = auth.currentUser != null

  fun currentUserId(): String? = auth.currentUser?.uid

  // --- USER PROFILE ---

  suspend fun saveUserProfile(
    email: String,
    fullName: String,
    attempt: String = "Jan 2027",
    targetGroup: String = "Both Groups"
  ): Result<Unit> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val path = "users/$uid"
    val docRef = db.collection("users").document(uid)

    return try {
      val existing = docRef.get().await()
      if (!existing.exists()) {
        val data = mapOf(
          "userId" to uid,
          "email" to email.take(150),
          "fullName" to fullName.take(100),
          "attempt" to attempt.take(50),
          "targetGroup" to targetGroup.take(50),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
      } else {
        val data = mapOf(
          "email" to email.take(150),
          "fullName" to fullName.take(100),
          "attempt" to attempt.take(50),
          "targetGroup" to targetGroup.take(50),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(data).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun getUserProfile(): Result<FirestoreUserProfile?> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val path = "users/$uid"
    return try {
      val snapshot = db.collection("users").document(uid).get().await()
      Result.success(snapshot.toObject(FirestoreUserProfile::class.java))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  // --- TOPIC PROGRESS ---

  fun observeTopicsProgress(): Flow<List<FirestoreTopicProgress>> = flow {
    val uid = requireUserId()
    val path = "users/$uid/topics_progress"
    emitAll(
      db.collection("users").document(uid).collection("topics_progress")
        .snapshots()
        .map { snapshot -> snapshot.toObjects(FirestoreTopicProgress::class.java) }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
          throw error
        }
    )
  }

  suspend fun saveTopicProgress(topic: FirestoreTopicProgress): Result<Unit> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val path = "users/$uid/topics_progress/${topic.topicId}"
    val docRef = db.collection("users").document(uid).collection("topics_progress").document(topic.topicId)

    return try {
      val data = mutableMapOf<String, Any>(
        "userId" to uid,
        "topicId" to topic.topicId,
        "paperId" to topic.paperId,
        "chapterName" to topic.chapterName.take(200),
        "topicTitle" to topic.topicTitle.take(300),
        "icaiRef" to topic.icaiRef.take(100),
        "isCustom" to topic.isCustom,
        "status" to topic.status,
        "notes" to topic.notes.take(1000),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(data).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun syncBatchTopics(topics: List<FirestoreTopicProgress>): Result<Unit> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val path = "users/$uid/topics_progress"

    return try {
      // Write in batches of up to 400
      topics.chunked(400).forEach { chunk ->
        val batch = db.batch()
        for (topic in chunk) {
          val docRef = db.collection("users").document(uid).collection("topics_progress").document(topic.topicId)
          val data = mapOf(
            "userId" to uid,
            "topicId" to topic.topicId,
            "paperId" to topic.paperId,
            "chapterName" to topic.chapterName.take(200),
            "topicTitle" to topic.topicTitle.take(300),
            "icaiRef" to topic.icaiRef.take(100),
            "isCustom" to topic.isCustom,
            "status" to topic.status,
            "notes" to topic.notes.take(1000),
            "updatedAt" to FieldValue.serverTimestamp()
          )
          batch.set(docRef, data)
        }
        batch.commit().await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  // --- MISTAKE VAULT ---

  fun observeMistakes(): Flow<List<FirestoreMistakeVaultItem>> = flow {
    val uid = requireUserId()
    val path = "users/$uid/mistake_vault"
    emitAll(
      db.collection("users").document(uid).collection("mistake_vault")
        .snapshots()
        .map { snapshot -> snapshot.toObjects(FirestoreMistakeVaultItem::class.java) }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
          throw error
        }
    )
  }

  suspend fun saveMistake(mistake: FirestoreMistakeVaultItem): Result<Unit> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val path = "users/$uid/mistake_vault/${mistake.mistakeId}"
    val docRef = db.collection("users").document(uid).collection("mistake_vault").document(mistake.mistakeId)

    return try {
      val data = mapOf(
        "userId" to uid,
        "mistakeId" to mistake.mistakeId,
        "paperId" to mistake.paperId,
        "subjectName" to mistake.subjectName.take(150),
        "topicOrStandard" to mistake.topicOrStandard.take(200),
        "question" to mistake.question.take(2000),
        "studentAttempt" to mistake.studentAttempt.take(4000),
        "marksLost" to mistake.marksLost,
        "totalMarks" to mistake.totalMarks,
        "missingKeywords" to mistake.missingKeywords.take(1000),
        "toppersSolution" to mistake.toppersSolution.take(4000),
        "reviewIntervalDays" to mistake.reviewIntervalDays,
        "nextReviewDate" to (mistake.nextReviewDate ?: Timestamp.now()),
        "isMastered" to mistake.isMastered,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(data).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun updateMistakeReview(
    mistakeId: String,
    newInterval: Int,
    nextDate: Timestamp,
    isMastered: Boolean
  ): Result<Unit> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val path = "users/$uid/mistake_vault/$mistakeId"
    val docRef = db.collection("users").document(uid).collection("mistake_vault").document(mistakeId)

    return try {
      docRef.update(
        mapOf(
          "reviewIntervalDays" to newInterval,
          "nextReviewDate" to nextDate,
          "isMastered" to isMastered,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteMistake(mistakeId: String): Result<Unit> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val path = "users/$uid/mistake_vault/$mistakeId"
    return try {
      db.collection("users").document(uid).collection("mistake_vault").document(mistakeId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  // --- TEST HISTORY ---

  fun observeTestHistory(): Flow<List<FirestoreTestRecord>> = flow {
    val uid = requireUserId()
    val path = "users/$uid/test_history"
    emitAll(
      db.collection("users").document(uid).collection("test_history")
        .snapshots()
        .map { snapshot -> snapshot.toObjects(FirestoreTestRecord::class.java) }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
          throw error
        }
    )
  }

  suspend fun recordTest(test: FirestoreTestRecord): Result<Unit> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val path = "users/$uid/test_history/${test.testId}"
    val docRef = db.collection("users").document(uid).collection("test_history").document(test.testId)

    return try {
      val data = mapOf(
        "userId" to uid,
        "testId" to test.testId,
        "paperId" to test.paperId,
        "paperName" to test.paperName.take(150),
        "chapterOrTopic" to test.chapterOrTopic.take(200),
        "totalMarks" to test.totalMarks,
        "marksScored" to test.marksScored,
        "mcqScore" to test.mcqScore,
        "descriptiveScore" to test.descriptiveScore,
        "dateTaken" to FieldValue.serverTimestamp(),
        "summaryNotes" to test.summaryNotes.take(2000)
      )
      docRef.set(data).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }
}
