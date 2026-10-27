package com.example.data.ai

import com.example.data.model.EvaluationResult
import com.example.data.model.StepMarkingItem
import org.json.JSONObject

object IcaiExaminerEvaluator {

  fun buildExaminerPrompt(
    paperName: String,
    questionText: String,
    maxMarks: Int,
    icaiReference: String,
    modelAnswer: String,
    keyPhrases: List<String>,
    studentTypedAnswer: String,
    hasHandwrittenImage: Boolean
  ): String {
    return """
      EVALUATE_ANSWER
      You are a Strict Senior Examiner for the Institute of Chartered Accountants of India (ICAI) evaluating an exam answer for CA Intermediate (New Scheme - January 2027 attempt).
      
      SUBJECT: $paperName
      QUESTION (${maxMarks} MARKS):
      $questionText
      
      ICAI REFERENCE STANDARD / SECTION: $icaiReference
      OFFICIAL MODEL ANSWER:
      $modelAnswer
      
      MANDATORY ICAI KEY PHRASES: ${keyPhrases.joinToString(", ")}
      
      STUDENT ANSWER SUBMISSION:
      ${if (hasHandwrittenImage) "[User has attached a handwritten notebook photo. Transcribe handwriting and strictly evaluate it according to step marking rules below]" else ""}
      $studentTypedAnswer
      
      STRICT ICAI EVALUATION GUIDELINES:
      1. For LAW / AUDIT: Strictly enforce the ICAI 4-Step Framework:
         - Step 1: Relevant Legal Provision & Exact Section/SA Citation
         - Step 2: Facts of the Case
         - Step 3: Analysis, Application & Correlation of Law to Facts
         - Step 4: Final Conclusion
      2. For ACCOUNTING / COST / FM:
         - Working Notes carry 40% of descriptive marks. Penalize severely if working notes are missing or unclear.
         - Check final figures and intermediate formulas (e.g. AS 10, AS 14, P/V ratio, WACC, NPV).
      3. Penalize missing ICAI technical keywords (-0.5 to -1.0 mark per omitted core term).
      4. Provide a "Topper's Suggested Rewrite" showing how a Rank 1 student would format the solution.
      
      Return ONLY a valid JSON object matching this schema:
      {
        "marksAwarded": Float,
        "totalMarks": $maxMarks,
        "stepBreakdown": [
          {"step": String, "max": Float, "awarded": Float, "remark": String}
        ],
        "missingKeywords": [String],
        "deductions": [String],
        "toppersRewrite": String,
        "examinerSummary": String
      }
    """.trimIndent()
  }

  fun parseEvaluationResponse(
    responseString: String,
    questionId: String,
    defaultMaxMarks: Float,
    studentAnswer: String,
    keyPhrases: List<String>,
    modelAnswer: String
  ): EvaluationResult {
    try {
      val cleanJson = extractJson(responseString)
      val obj = JSONObject(cleanJson)

      val awarded = obj.optDouble("marksAwarded", defaultMaxMarks * 0.6).toFloat()
      val total = obj.optDouble("totalMarks", defaultMaxMarks.toDouble()).toFloat()
      val summary = obj.optString("examinerSummary", "Evaluation completed as per ICAI guidelines.")
      val toppersRewrite = obj.optString("toppersRewrite", modelAnswer)

      val stepBreakdownList = mutableListOf<StepMarkingItem>()
      val stepsArr = obj.optJSONArray("stepBreakdown")
      if (stepsArr != null) {
        for (i in 0 until stepsArr.length()) {
          val sObj = stepsArr.getJSONObject(i)
          stepBreakdownList.add(
            StepMarkingItem(
              stepName = sObj.optString("step", "Step ${i + 1}"),
              maxMarks = sObj.optDouble("max", 1.0).toFloat(),
              awardedMarks = sObj.optDouble("awarded", 0.5).toFloat(),
              remark = sObj.optString("remark", "")
            )
          )
        }
      }

      val missingKeywordsList = mutableListOf<String>()
      val missingArr = obj.optJSONArray("missingKeywords")
      if (missingArr != null) {
        for (i in 0 until missingArr.length()) {
          missingKeywordsList.add(missingArr.getString(i))
        }
      }

      val deductionsList = mutableListOf<String>()
      val dedArr = obj.optJSONArray("deductions")
      if (dedArr != null) {
        for (i in 0 until dedArr.length()) {
          deductionsList.add(dedArr.getString(i))
        }
      }

      if (stepBreakdownList.isEmpty()) {
        stepBreakdownList.addAll(getDefaultStepBreakdown(total, awarded))
      }

      return EvaluationResult(
        questionId = questionId,
        marksAwarded = awarded,
        totalMarks = total,
        stepBreakdown = stepBreakdownList,
        missingKeywords = missingKeywordsList,
        deductions = deductionsList,
        toppersRewrite = toppersRewrite,
        examinerSummary = summary
      )
    } catch (e: Exception) {
      // Offline fallback assessment
      return computeOfflineEvaluation(
        questionId = questionId,
        studentAnswer = studentAnswer,
        maxMarks = defaultMaxMarks,
        keyPhrases = keyPhrases,
        modelAnswer = modelAnswer
      )
    }
  }

  private fun extractJson(raw: String): String {
    val startIndex = raw.indexOf('{')
    val endIndex = raw.lastIndexOf('}')
    return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
      raw.substring(startIndex, endIndex + 1)
    } else {
      raw
    }
  }

  fun computeOfflineEvaluation(
    questionId: String,
    studentAnswer: String,
    maxMarks: Float,
    keyPhrases: List<String>,
    modelAnswer: String
  ): EvaluationResult {
    val lowerAnswer = studentAnswer.lowercase()
    val presentKeywords = mutableListOf<String>()
    val missingKeywords = mutableListOf<String>()

    for (phrase in keyPhrases) {
      if (lowerAnswer.contains(phrase.lowercase())) {
        presentKeywords.add(phrase)
      } else {
        missingKeywords.add(phrase)
      }
    }

    val keywordRatio = if (keyPhrases.isNotEmpty()) {
      presentKeywords.size.toFloat() / keyPhrases.size.toFloat()
    } else {
      0.75f
    }

    // Checking 4-step framework presence
    val hasProvision = lowerAnswer.contains("provision") || lowerAnswer.contains("section") || lowerAnswer.contains("standard") || lowerAnswer.contains("as ") || lowerAnswer.contains("sa ")
    val hasFacts = lowerAnswer.contains("fact") || lowerAnswer.contains("case") || lowerAnswer.contains("given")
    val hasAnalysis = lowerAnswer.contains("analys") || lowerAnswer.contains("correlat") || lowerAnswer.contains("accord") || lowerAnswer.contains("applic")
    val hasConclusion = lowerAnswer.contains("conclusion") || lowerAnswer.contains("therefore") || lowerAnswer.contains("hence") || lowerAnswer.contains("liable")

    var awarded = maxMarks * 0.4f * keywordRatio
    val deductions = mutableListOf<String>()

    val s1Max = maxMarks * 0.25f
    val s1Awarded = if (hasProvision) s1Max * 0.9f else s1Max * 0.3f
    if (!hasProvision) deductions.add("-${String.format("%.1f", s1Max * 0.7f)}: Legal provision or section citation omitted")

    val s2Max = maxMarks * 0.15f
    val s2Awarded = if (hasFacts) s2Max else s2Max * 0.4f

    val s3Max = maxMarks * 0.40f
    val s3Awarded = (s3Max * keywordRatio).coerceAtMost(s3Max)
    if (missingKeywords.isNotEmpty()) {
      deductions.add("Omitted essential ICAI technical terms: ${missingKeywords.take(3).joinToString(", ")}")
    }

    val s4Max = maxMarks * 0.20f
    val s4Awarded = if (hasConclusion) s4Max else s4Max * 0.4f
    if (!hasConclusion) deductions.add("-${String.format("%.1f", s4Max * 0.6f)}: Clear final conclusion paragraph missing")

    val totalAwarded = (s1Awarded + s2Awarded + s3Awarded + s4Awarded).coerceIn(0.5f, maxMarks)

    val steps = listOf(
      StepMarkingItem("1. Provision & Section/Standard", s1Max, s1Awarded, if (hasProvision) "Standard/Section reference identified" else "Needs specific citation"),
      StepMarkingItem("2. Facts of the Case", s2Max, s2Awarded, if (hasFacts) "Facts neatly outlined" else "Summary of facts omitted"),
      StepMarkingItem("3. Analysis & Key Arguments", s3Max, s3Awarded, "Keyword match: ${presentKeywords.size}/${keyPhrases.size}"),
      StepMarkingItem("4. Final Conclusion", s4Max, s4Awarded, if (hasConclusion) "Definite opinion delivered" else "Ambiguous conclusion")
    )

    return EvaluationResult(
      questionId = questionId,
      marksAwarded = (Math.round(totalAwarded * 2) / 2.0).toFloat(), // round to nearest 0.5 mark like ICAI
      totalMarks = maxMarks,
      stepBreakdown = steps,
      missingKeywords = missingKeywords,
      deductions = deductions,
      toppersRewrite = modelAnswer,
      examinerSummary = "Evaluated via ICAI step-marking criteria. Score reflects 4-step framework adoption and technical keyword coverage."
    )
  }

  private fun getDefaultStepBreakdown(total: Float, awarded: Float): List<StepMarkingItem> {
    return listOf(
      StepMarkingItem("1. Relevant Provision / Standard", total * 0.25f, awarded * 0.25f, "Citation of Law / Standard"),
      StepMarkingItem("2. Analysis & Working Notes", total * 0.50f, awarded * 0.50f, "Mathematical computation & reasoning"),
      StepMarkingItem("3. Final Conclusion & Disclosures", total * 0.25f, awarded * 0.25f, "Closing result and formal presentation")
    )
  }
}
