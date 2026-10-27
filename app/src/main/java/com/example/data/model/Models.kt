package com.example.data.model

enum class GroupType(val title: String) {
  GROUP_1("Group 1"),
  GROUP_2("Group 2"),
  BOTH("Both Groups")
}

enum class TopicStatus {
  PENDING,
  DONE,
  REVISE
}

data class PaperInfo(
  val id: Int,
  val code: String,
  val name: String,
  val group: GroupType,
  val iconName: String,
  val description: String,
  val chaptersCount: Int
)

data class TopicItem(
  val id: String,
  val paperId: Int,
  val chapterName: String,
  val topicTitle: String,
  val icaiRef: String = "", // e.g. "AS 10", "Sec 135", "SA 500"
  val isCustom: Boolean = false,
  val status: TopicStatus = TopicStatus.PENDING,
  val notes: String = ""
)

data class CaseScenarioMcq(
  val id: String,
  val scenarioText: String,
  val question: String,
  val options: List<String>,
  val correctOptionIndex: Int,
  val icaiCitation: String,
  val detailedExplanation: String
)

data class DescriptiveQuestion(
  val id: String,
  val questionNumber: String,
  val questionText: String,
  val marks: Int,
  val icaiReference: String, // e.g. "Section 135 & Schedule VII" or "SA 505 External Confirmations"
  val rubricSteps: List<String>,
  val officialModelAnswer: String,
  val keyPhrases: List<String>
)

data class GeneratedTestPaper(
  val testId: String,
  val title: String,
  val paperId: Int,
  val chapterOrStandard: String,
  val totalMarks: Int,
  val mcqMarks: Int,
  val descriptiveMarks: Int,
  val mcqs: List<CaseScenarioMcq>,
  val descriptiveQuestions: List<DescriptiveQuestion>,
  val timestamp: Long = System.currentTimeMillis()
)

data class StepMarkingItem(
  val stepName: String,
  val maxMarks: Float,
  val awardedMarks: Float,
  val remark: String
)

data class EvaluationResult(
  val questionId: String,
  val marksAwarded: Float,
  val totalMarks: Float,
  val stepBreakdown: List<StepMarkingItem>,
  val missingKeywords: List<String>,
  val deductions: List<String>,
  val toppersRewrite: String,
  val examinerSummary: String
)

data class RapidFireCard(
  val id: String,
  val paperId: Int,
  val subjectName: String,
  val question: String,
  val answer: String,
  val keyProvision: String
)
