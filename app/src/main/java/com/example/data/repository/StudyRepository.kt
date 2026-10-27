package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.firestore.FirestoreMistakeVaultItem
import com.example.data.firestore.FirestoreSyncRepository
import com.example.data.firestore.FirestoreTestRecord
import com.example.data.firestore.FirestoreTopicProgress
import com.example.data.local.AppDatabase
import com.example.data.local.MistakeVaultEntity
import com.example.data.local.StudyScheduleEntity
import com.example.data.local.TestHistoryEntity
import com.example.data.local.TopicProgressEntity
import com.example.data.model.TopicItem
import com.example.data.model.TopicStatus
import com.example.data.syllabus.IcaiSyllabusData
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

class StudyRepository(context: Context) {

  private val db = AppDatabase.getDatabase(context)
  private val studyDao = db.studyDao()
  val firestoreSync = FirestoreSyncRepository(context)
  private val syncScope = CoroutineScope(Dispatchers.IO)

  suspend fun ensureTopicsSeeded(userId: Int) = withContext(Dispatchers.IO) {
    val count = studyDao.getUserTopicCount(userId)
    if (count == 0) {
      val initial = IcaiSyllabusData.getInitialTopics()
      val entities = initial.map { topic ->
        TopicProgressEntity(
          uid = "${userId}_${topic.id}",
          userId = userId,
          topicId = topic.id,
          paperId = topic.paperId,
          chapterName = topic.chapterName,
          topicTitle = topic.topicTitle,
          icaiRef = topic.icaiRef,
          isCustom = false,
          status = topic.status.name,
          notes = ""
        )
      }
      studyDao.insertAllTopics(entities)
    }
  }

  fun getTopicsForUser(userId: Int): Flow<List<TopicItem>> {
    return studyDao.getAllUserTopics(userId).map { list ->
      list.map { it.toDomain() }
    }
  }

  fun getTopicsForUserAndPaper(userId: Int, paperId: Int): Flow<List<TopicItem>> {
    return studyDao.getUserTopicsByPaper(userId, paperId).map { list ->
      list.map { it.toDomain() }
    }
  }

  suspend fun updateTopicStatus(userId: Int, topicId: String, newStatus: TopicStatus) = withContext(Dispatchers.IO) {
    val uid = "${userId}_${topicId}"
    studyDao.updateTopicStatus(uid, newStatus.name)

    // Cloud Firestore dual-sync
    if (firestoreSync.isUserSignedIn()) {
      syncScope.launch {
        try {
          val topicEntity = studyDao.getUserTopicsByPaper(userId, 1).first().firstOrNull { it.topicId == topicId }
            ?: studyDao.getAllUserTopics(userId).first().firstOrNull { it.topicId == topicId }
          if (topicEntity != null) {
            val cloudTopic = FirestoreTopicProgress(
              userId = firestoreSync.requireUserId(),
              topicId = topicEntity.topicId,
              paperId = topicEntity.paperId,
              chapterName = topicEntity.chapterName,
              topicTitle = topicEntity.topicTitle,
              icaiRef = topicEntity.icaiRef,
              isCustom = topicEntity.isCustom,
              status = newStatus.name,
              notes = topicEntity.notes
            )
            firestoreSync.saveTopicProgress(cloudTopic)
          }
        } catch (e: Exception) {
          Log.w("StudyRepository", "Cloud sync topic status failed: ${e.message}")
        }
      }
    }
  }

  suspend fun addCustomTopic(
    userId: Int,
    paperId: Int,
    chapterName: String,
    topicTitle: String,
    icaiRef: String
  ) = withContext(Dispatchers.IO) {
    val customId = "custom_" + UUID.randomUUID().toString().take(8)
    val entity = TopicProgressEntity(
      uid = "${userId}_${customId}",
      userId = userId,
      topicId = customId,
      paperId = paperId,
      chapterName = chapterName.ifBlank { "Custom Chapters" },
      topicTitle = topicTitle,
      icaiRef = icaiRef,
      isCustom = true,
      status = TopicStatus.PENDING.name,
      notes = ""
    )
    studyDao.insertOrUpdateTopic(entity)

    // Cloud Firestore dual-sync
    if (firestoreSync.isUserSignedIn()) {
      syncScope.launch {
        try {
          val cloudTopic = FirestoreTopicProgress(
            userId = firestoreSync.requireUserId(),
            topicId = customId,
            paperId = paperId,
            chapterName = entity.chapterName,
            topicTitle = topicTitle,
            icaiRef = icaiRef,
            isCustom = true,
            status = TopicStatus.PENDING.name,
            notes = ""
          )
          firestoreSync.saveTopicProgress(cloudTopic)
        } catch (e: Exception) {
          Log.w("StudyRepository", "Cloud sync custom topic failed: ${e.message}")
        }
      }
    }
  }

  // Mistake Vault
  fun getUserMistakes(userId: Int): Flow<List<MistakeVaultEntity>> {
    return studyDao.getUserMistakes(userId)
  }

  suspend fun addMistake(
    userId: Int,
    paperId: Int,
    subjectName: String,
    topicOrStandard: String,
    question: String,
    studentAttempt: String,
    marksLost: Float,
    totalMarks: Float,
    missingKeywords: List<String>,
    toppersSolution: String
  ): Long = withContext(Dispatchers.IO) {
    val entity = MistakeVaultEntity(
      userId = userId,
      paperId = paperId,
      subjectName = subjectName,
      topicOrStandard = topicOrStandard,
      question = question,
      studentAttempt = studentAttempt,
      marksLost = marksLost,
      totalMarks = totalMarks,
      missingKeywords = missingKeywords.joinToString(", "),
      toppersSolution = toppersSolution,
      reviewIntervalDays = 1,
      nextReviewDate = System.currentTimeMillis() + (1L * 24 * 60 * 60 * 1000),
      isMastered = false
    )
    val insertedId = studyDao.insertMistake(entity)

    // Cloud Firestore dual-sync
    if (firestoreSync.isUserSignedIn()) {
      syncScope.launch {
        try {
          val cloudMistake = FirestoreMistakeVaultItem(
            userId = firestoreSync.requireUserId(),
            mistakeId = "m_$insertedId",
            paperId = paperId,
            subjectName = subjectName,
            topicOrStandard = topicOrStandard,
            question = question,
            studentAttempt = studentAttempt,
            marksLost = marksLost.toDouble(),
            totalMarks = totalMarks.toDouble(),
            missingKeywords = missingKeywords.joinToString(", "),
            toppersSolution = toppersSolution,
            reviewIntervalDays = 1,
            nextReviewDate = Timestamp(Date(entity.nextReviewDate)),
            isMastered = false
          )
          firestoreSync.saveMistake(cloudMistake)
        } catch (e: Exception) {
          Log.w("StudyRepository", "Cloud sync mistake failed: ${e.message}")
        }
      }
    }
    insertedId
  }

  suspend fun advanceSpacedRepetition(mistakeId: Int, currentInterval: Int, remembered: Boolean) = withContext(Dispatchers.IO) {
    val intervals = listOf(1, 3, 7, 14, 30)
    val nextInterval: Int
    val nextDate: Long
    val isMastered: Boolean

    if (!remembered) {
      nextInterval = 1
      nextDate = System.currentTimeMillis() + (1L * 24 * 60 * 60 * 1000)
      isMastered = false
      studyDao.updateSpacedRepetition(mistakeId, 1, nextDate, false)
    } else {
      val currentIndex = intervals.indexOf(currentInterval)
      if (currentIndex == -1 || currentIndex >= intervals.size - 1) {
        nextInterval = 30
        nextDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
        isMastered = true
        studyDao.updateSpacedRepetition(mistakeId, 30, nextDate, true)
      } else {
        nextInterval = intervals[currentIndex + 1]
        nextDate = System.currentTimeMillis() + (nextInterval.toLong() * 24 * 60 * 60 * 1000)
        isMastered = false
        studyDao.updateSpacedRepetition(mistakeId, nextInterval, nextDate, false)
      }
    }

    // Cloud Firestore dual-sync
    if (firestoreSync.isUserSignedIn()) {
      syncScope.launch {
        try {
          firestoreSync.updateMistakeReview(
            mistakeId = "m_$mistakeId",
            newInterval = nextInterval,
            nextDate = Timestamp(Date(nextDate)),
            isMastered = isMastered
          )
        } catch (e: Exception) {
          Log.w("StudyRepository", "Cloud sync review update failed: ${e.message}")
        }
      }
    }
  }

  suspend fun deleteMistake(mistakeId: Int) = withContext(Dispatchers.IO) {
    studyDao.deleteMistake(mistakeId)
    if (firestoreSync.isUserSignedIn()) {
      syncScope.launch {
        try {
          firestoreSync.deleteMistake("m_$mistakeId")
        } catch (e: Exception) {
          Log.w("StudyRepository", "Cloud sync delete mistake failed: ${e.message}")
        }
      }
    }
  }

  // Test History
  fun getUserTestHistory(userId: Int): Flow<List<TestHistoryEntity>> {
    return studyDao.getUserTestHistory(userId)
  }

  suspend fun recordTestAttempt(record: TestHistoryEntity) = withContext(Dispatchers.IO) {
    studyDao.insertTestHistory(record)
    if (firestoreSync.isUserSignedIn()) {
      syncScope.launch {
        try {
          val cloudTest = FirestoreTestRecord(
            userId = firestoreSync.requireUserId(),
            testId = record.testId,
            paperId = record.paperId,
            paperName = record.paperName,
            chapterOrTopic = record.chapterOrTopic,
            totalMarks = record.totalMarks,
            marksScored = record.marksScored.toDouble(),
            mcqScore = record.mcqScore.toDouble(),
            descriptiveScore = record.descriptiveScore.toDouble(),
            summaryNotes = record.summaryNotes
          )
          firestoreSync.recordTest(cloudTest)
        } catch (e: Exception) {
          Log.w("StudyRepository", "Cloud sync test record failed: ${e.message}")
        }
      }
    }
  }

  suspend fun syncAllWithCloud(userId: Int): Result<Int> = withContext(Dispatchers.IO) {
    if (!firestoreSync.isUserSignedIn()) {
      return@withContext Result.failure(IllegalStateException("Please sign in with Google to sync."))
    }
    try {
      val uid = firestoreSync.requireUserId()
      val localTopics = studyDao.getAllUserTopics(userId).first()
      val cloudTopics = localTopics.map {
        FirestoreTopicProgress(
          userId = uid,
          topicId = it.topicId,
          paperId = it.paperId,
          chapterName = it.chapterName,
          topicTitle = it.topicTitle,
          icaiRef = it.icaiRef,
          isCustom = it.isCustom,
          status = it.status,
          notes = it.notes
        )
      }
      firestoreSync.syncBatchTopics(cloudTopics)

      val localMistakes = studyDao.getUserMistakes(userId).first()
      for (m in localMistakes) {
        val cloudMistake = FirestoreMistakeVaultItem(
          userId = uid,
          mistakeId = "m_${m.id}",
          paperId = m.paperId,
          subjectName = m.subjectName,
          topicOrStandard = m.topicOrStandard,
          question = m.question,
          studentAttempt = m.studentAttempt,
          marksLost = m.marksLost.toDouble(),
          totalMarks = m.totalMarks.toDouble(),
          missingKeywords = m.missingKeywords,
          toppersSolution = m.toppersSolution,
          reviewIntervalDays = m.reviewIntervalDays,
          nextReviewDate = Timestamp(Date(m.nextReviewDate)),
          isMastered = m.isMastered
        )
        firestoreSync.saveMistake(cloudMistake)
      }
      Result.success(localTopics.size + localMistakes.size)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun TopicProgressEntity.toDomain(): TopicItem {
    val statusEnum = try {
      TopicStatus.valueOf(this.status)
    } catch (e: Exception) {
      TopicStatus.PENDING
    }
    return TopicItem(
      id = this.topicId,
      paperId = this.paperId,
      chapterName = this.chapterName,
      topicTitle = this.topicTitle,
      icaiRef = this.icaiRef,
      isCustom = this.isCustom,
      status = statusEnum,
      notes = this.notes
    )
  }

  // Weekly Study Planner Schedule
  fun getWeeklySchedule(userId: Int, startDate: String, endDate: String): Flow<List<StudyScheduleEntity>> {
    return studyDao.getSchedulesForDateRange(userId, startDate, endDate)
  }

  fun getAllSchedules(userId: Int): Flow<List<StudyScheduleEntity>> {
    return studyDao.getAllUserSchedules(userId)
  }

  suspend fun scheduleTopic(
    userId: Int,
    topic: TopicItem,
    date: String,
    timeSlot: String = "Morning"
  ): Long = withContext(Dispatchers.IO) {
    val schedule = StudyScheduleEntity(
      userId = userId,
      topicId = topic.id,
      paperId = topic.paperId,
      chapterName = topic.chapterName,
      topicTitle = topic.topicTitle,
      icaiRef = topic.icaiRef,
      scheduledDate = date,
      timeSlot = timeSlot,
      isCompleted = false
    )
    studyDao.insertSchedule(schedule)
  }

  suspend fun moveScheduleSlot(id: Long, newDate: String, newSlot: String) = withContext(Dispatchers.IO) {
    studyDao.updateScheduleSlot(id, newDate, newSlot)
  }

  suspend fun toggleScheduleCompleted(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
    studyDao.toggleScheduleCompleted(id, completed)
  }

  suspend fun removeScheduleItem(id: Long) = withContext(Dispatchers.IO) {
    studyDao.deleteSchedule(id)
  }

  suspend fun clearWeeklySchedule(userId: Int, startDate: String, endDate: String) = withContext(Dispatchers.IO) {
    studyDao.clearScheduleRange(userId, startDate, endDate)
  }

  suspend fun autoPlanWeek(userId: Int, weekDates: List<String>) = withContext(Dispatchers.IO) {
    if (weekDates.isEmpty()) return@withContext
    val topics = studyDao.getAllUserTopics(userId).first()
      .filter { it.status != TopicStatus.DONE.name }
      .take(weekDates.size * 2)

    val schedules = mutableListOf<StudyScheduleEntity>()
    topics.forEachIndexed { index, topic ->
      val targetDate = weekDates[index % weekDates.size]
      val slot = if ((index / weekDates.size) % 2 == 0) "Morning" else "Evening"
      schedules.add(
        StudyScheduleEntity(
          userId = userId,
          topicId = topic.topicId,
          paperId = topic.paperId,
          chapterName = topic.chapterName,
          topicTitle = topic.topicTitle,
          icaiRef = topic.icaiRef,
          scheduledDate = targetDate,
          timeSlot = slot,
          isCompleted = false,
          orderIndex = index
        )
      )
    }
    studyDao.insertScheduleList(schedules)
  }
}

