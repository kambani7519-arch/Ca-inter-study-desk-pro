package com.example

import com.example.data.ai.IcaiExaminerEvaluator
import com.example.data.questionbank.CuratedQuestionBank
import com.example.data.syllabus.IcaiSyllabusData
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testSyllabusPapers_allSixPapersExist() {
    val papers = IcaiSyllabusData.papers
    assertEquals(6, papers.size)
    assertEquals(1, papers[0].id)
    assertEquals("Advanced Accounting", papers[0].name)
    assertEquals(2, papers[1].id)
    assertEquals("Corporate & Other Laws", papers[1].name)
    assertEquals(3, papers[2].id)
    assertEquals("Taxation (DT & GST)", papers[2].name)
    assertEquals(4, papers[3].id)
    assertEquals("Cost & Management Accounting", papers[3].name)
    assertEquals(5, papers[4].id)
    assertEquals("Auditing & Ethics", papers[4].name)
    assertEquals(6, papers[5].id)
    assertEquals("FM & Strategic Management", papers[5].name)

    val topics = IcaiSyllabusData.getInitialTopics()
    assertTrue(topics.isNotEmpty())
    assertTrue(topics.any { it.icaiRef == "AS 10" })
    assertTrue(topics.any { it.icaiRef == "SA 500" })
  }

  @Test
  fun testQuestionBank_30to70RatioGeneration() {
    val testPaper30 = CuratedQuestionBank.generateTest(
      paperId = 2,
      chapterOrStandard = "Corporate Law",
      marks = 30
    )
    assertEquals(30, testPaper30.totalMarks)
    assertTrue(testPaper30.mcqs.isNotEmpty())
    assertTrue(testPaper30.descriptiveQuestions.isNotEmpty())

    val testPaper100 = CuratedQuestionBank.generateTest(
      paperId = 1,
      chapterOrStandard = "Full Mock",
      marks = 100
    )
    assertEquals(100, testPaper100.totalMarks)
    assertEquals(30, testPaper100.mcqMarks)
    assertEquals(70, testPaper100.descriptiveMarks)
  }

  @Test
  fun testOfflineExaminerEvaluation_calculatesStepMarks() {
    val keyPhrases = listOf("Section 123", "Rule 3", "Accumulated Profits", "Free Reserves")
    val studentAnswer = "Under Section 123 and Rule 3, the company can declare dividend out of accumulated profits."

    val eval = IcaiExaminerEvaluator.computeOfflineEvaluation(
      questionId = "test_q",
      studentAnswer = studentAnswer,
      maxMarks = 7f,
      keyPhrases = keyPhrases,
      modelAnswer = "Model Answer"
    )

    assertTrue(eval.marksAwarded > 0f)
    assertEquals(7f, eval.totalMarks, 0.01f)
    assertEquals(4, eval.stepBreakdown.size)
  }
}
