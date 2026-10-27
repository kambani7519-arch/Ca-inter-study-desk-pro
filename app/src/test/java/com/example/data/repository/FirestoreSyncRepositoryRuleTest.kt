package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firestore.FirestoreMistakeVaultItem
import com.example.data.firestore.FirestoreSyncRepository
import com.example.data.firestore.FirestoreTestRecord
import com.example.data.firestore.FirestoreTopicProgress
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FirestoreSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveUserProfile_authenticatedUser_success() {
    runBlocking {
      val uid = signInTestUser(ALICE_EMAIL)
      val repo = FirestoreSyncRepository(firestore)

      val result = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.saveUserProfile(
          email = ALICE_EMAIL,
          fullName = "Alice CA Aspirant",
          attempt = "Jan 2027",
          targetGroup = "Both Groups"
        )
      }
      assertTrue(result.isSuccess)

      val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.getUserProfile().getOrThrow()
      }
      assertNotNull(profile)
      assertEquals("Alice CA Aspirant", profile?.fullName)
      assertEquals(uid, profile?.userId)
    }
  }

  @Test
  fun saveTopicProgress_authenticatedOwner_canSaveAndObserve() {
    runBlocking {
      val uid = signInTestUser(ALICE_EMAIL)
      val repo = FirestoreSyncRepository(firestore)
      val dynamicTopicId = "topic_" + UUID.randomUUID().toString().take(8)

      val topic = FirestoreTopicProgress(
        userId = uid,
        topicId = dynamicTopicId,
        paperId = 1,
        chapterName = "AS 10 Property Plant Equipment",
        topicTitle = "Revaluation Model",
        icaiRef = "AS 10",
        status = "DONE",
        notes = "Formula practiced"
      )

      val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.saveTopicProgress(topic)
      }
      assertTrue(saveResult.isSuccess)

      val topics = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeTopicsProgress().first { list ->
          list.any { it.topicId == dynamicTopicId }
        }
      }
      assertTrue(topics.any { it.topicId == dynamicTopicId && it.status == "DONE" })
    }
  }

  @Test
  fun saveMistake_authenticatedOwner_canSaveAndUpdateReview() {
    runBlocking {
      val uid = signInTestUser(ALICE_EMAIL)
      val repo = FirestoreSyncRepository(firestore)
      val dynamicMistakeId = "mistake_" + UUID.randomUUID().toString().take(8)

      val mistake = FirestoreMistakeVaultItem(
        userId = uid,
        mistakeId = dynamicMistakeId,
        paperId = 2,
        subjectName = "Corporate and Other Laws",
        topicOrStandard = "Sec 135",
        question = "Explain composition of CSR Committee.",
        studentAttempt = "3 directors always needed",
        marksLost = 2.0,
        totalMarks = 5.0,
        missingKeywords = "Unlisted public company, Private company 2 directors",
        toppersSolution = "Under Sec 135(1), every company shall constitute CSR committee...",
        reviewIntervalDays = 1,
        isMastered = false
      )

      val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.saveMistake(mistake)
      }
      assertTrue(saveResult.isSuccess)

      val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.updateMistakeReview(
          mistakeId = dynamicMistakeId,
          newInterval = 3,
          nextDate = com.google.firebase.Timestamp.now(),
          isMastered = false
        )
      }
      assertTrue(updateResult.isSuccess)
    }
  }

  @Test
  fun recordTest_authenticatedOwner_createsImmutableRecord() {
    runBlocking {
      val uid = signInTestUser(ALICE_EMAIL)
      val repo = FirestoreSyncRepository(firestore)
      val dynamicTestId = "test_" + UUID.randomUUID().toString().take(8)

      val testRecord = FirestoreTestRecord(
        userId = uid,
        testId = dynamicTestId,
        paperId = 4,
        paperName = "Cost and Management Accounting",
        chapterOrTopic = "Standard Costing",
        totalMarks = 30,
        marksScored = 24.5,
        mcqScore = 9.0,
        descriptiveScore = 15.5,
        summaryNotes = "Variance formulas applied correctly"
      )

      val recordResult = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.recordTest(testRecord)
      }
      assertTrue(recordResult.isSuccess)
    }
  }

  @Test
  fun crossUserAccess_bobCannotAccessAliceSubcollection_failsWithPermissionDenied() {
    runBlocking {
      val aliceUid = signInTestUser(ALICE_EMAIL)
      val aliceRepo = FirestoreSyncRepository(firestore)
      val aliceTopicId = "topic_secret_" + UUID.randomUUID().toString().take(8)

      val topic = FirestoreTopicProgress(
        userId = aliceUid,
        topicId = aliceTopicId,
        paperId = 1,
        chapterName = "AS 2 Valuation of Inventories",
        topicTitle = "NRV Testing",
        status = "DONE"
      )
      withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.saveTopicProgress(topic).getOrThrow() }

      // Bob signs in
      val bobUid = signInTestUser(BOB_EMAIL)
      assertTrue(bobUid != aliceUid)

      // Bob attempts to directly read Alice's topic document
      try {
        withTimeout(DEFAULT_TIMEOUT_MS) {
          firestore.collection("users").document(aliceUid).collection("topics_progress").document(aliceTopicId).get().await()
        }
        fail("Expected PERMISSION_DENIED for cross-user read")
      } catch (e: Exception) {
        val firestoreEx = e.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
      }
    }
  }

  @Test
  fun unauthenticatedUser_cannotAccessProtectedCollections_failsWithPermissionDenied() {
    runBlocking {
      auth.signOut()

      try {
        withTimeout(DEFAULT_TIMEOUT_MS) {
          firestore.collection("users").document("any_user").collection("topics_progress").get().await()
        }
        fail("Expected PERMISSION_DENIED for unauthenticated collection read")
      } catch (e: Exception) {
        val firestoreEx = e.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
      }
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice_student@cainter.pro"
    const val BOB_EMAIL = "bob_student@cainter.pro"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
