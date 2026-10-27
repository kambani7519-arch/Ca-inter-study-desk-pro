package com.example.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.MistakeVaultEntity
import com.example.data.local.UserEntity
import com.example.data.model.GroupType
import com.example.data.model.TopicItem
import com.example.data.model.TopicStatus
import com.example.data.syllabus.IcaiSyllabusData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

  suspend fun generateStudyReport(
    context: Context,
    user: UserEntity?,
    allTopics: List<TopicItem>,
    mistakes: List<MistakeVaultEntity>
  ): File = withContext(Dispatchers.IO) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (595x842 pt)
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    val paint = Paint().apply { isAntiAlias = true }
    val titlePaint = Paint().apply {
      isAntiAlias = true
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = AndroidColor.rgb(15, 23, 42) // Oxford Dark Navy
      textSize = 15f
    }
    val subtitlePaint = Paint().apply {
      isAntiAlias = true
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = AndroidColor.rgb(30, 58, 138) // Royal Navy
      textSize = 10f
    }
    val bodyPaint = Paint().apply {
      isAntiAlias = true
      color = AndroidColor.rgb(30, 41, 59)
      textSize = 9f
    }
    val boldBodyPaint = Paint().apply {
      isAntiAlias = true
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = AndroidColor.rgb(15, 23, 42)
      textSize = 9f
    }
    val smallPaint = Paint().apply {
      isAntiAlias = true
      color = AndroidColor.rgb(100, 116, 139)
      textSize = 8f
    }

    var currentY = 35f

    // 1. Top Decorative Brand Bar
    paint.color = AndroidColor.rgb(30, 58, 138) // Primary Blue
    canvas.drawRect(30f, currentY, 565f, currentY + 6f, paint)
    currentY += 22f

    // 2. Header
    canvas.drawText("CA INTER STUDY DESK PRO — PERFORMANCE & MISTAKE VAULT AUDIT", 30f, currentY, titlePaint)
    currentY += 14f
    canvas.drawText("Strictly Tailored for ICAI CA Intermediate (New Scheme) • January 2027 Examination", 30f, currentY, subtitlePaint)
    currentY += 16f

    // 3. Student & Attempt Meta Info Box
    paint.color = AndroidColor.rgb(241, 245, 249) // Slate Light
    canvas.drawRoundRect(30f, currentY, 565f, currentY + 45f, 6f, 6f, paint)

    val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
    val dateStr = sdf.format(Date())

    val metaY = currentY + 16f
    canvas.drawText("Aspirant: ${user?.fullName ?: "CA Aspirant"}", 40f, metaY, boldBodyPaint)
    canvas.drawText("Email: ${user?.email ?: "student@cainter.pro"}", 40f, metaY + 14f, bodyPaint)

    canvas.drawText("Target: ${user?.attempt ?: "Jan 2027"} Attempt", 230f, metaY, boldBodyPaint)
    canvas.drawText("Group: ${user?.targetGroup ?: "Both Groups"}", 230f, metaY + 14f, bodyPaint)

    canvas.drawText("Report Generated:", 410f, metaY, smallPaint)
    canvas.drawText(dateStr, 410f, metaY + 14f, smallPaint)

    currentY += 60f

    // 4. Executive Summary KPI Boxes
    val totalTopics = allTopics.size.coerceAtLeast(1)
    val doneTopics = allTopics.count { it.status == TopicStatus.DONE }
    val overallPct = (doneTopics * 100 / totalTopics)

    val g1Topics = allTopics.filter { it.paperId in 1..3 }
    val g1Done = g1Topics.count { it.status == TopicStatus.DONE }
    val g1Pct = if (g1Topics.isNotEmpty()) (g1Done * 100 / g1Topics.size) else 0

    val g2Topics = allTopics.filter { it.paperId in 4..6 }
    val g2Done = g2Topics.count { it.status == TopicStatus.DONE }
    val g2Pct = if (g2Topics.isNotEmpty()) (g2Done * 100 / g2Topics.size) else 0

    val masteredCount = mistakes.count { it.isMastered }
    val activeMistakes = mistakes.size - masteredCount

    // Draw 3 KPI tiles
    val tileW = 165f
    val tileH = 42f

    // Tile 1: Overall
    paint.color = AndroidColor.rgb(239, 246, 255)
    canvas.drawRoundRect(30f, currentY, 30f + tileW, currentY + tileH, 6f, 6f, paint)
    canvas.drawText("OVERALL SYLLABUS", 40f, currentY + 14f, smallPaint)
    paint.color = AndroidColor.rgb(37, 99, 235)
    canvas.drawText("$overallPct% Cleared ($doneTopics/$totalTopics)", 40f, currentY + 30f, boldBodyPaint)

    // Tile 2: Groups Split
    paint.color = AndroidColor.rgb(245, 243, 255)
    canvas.drawRoundRect(205f, currentY, 205f + tileW, currentY + tileH, 6f, 6f, paint)
    canvas.drawText("GROUP PROGRESS", 215f, currentY + 14f, smallPaint)
    paint.color = AndroidColor.rgb(124, 58, 237)
    canvas.drawText("G1: $g1Pct%  |  G2: $g2Pct%", 215f, currentY + 30f, boldBodyPaint)

    // Tile 3: Mistake Vault
    paint.color = AndroidColor.rgb(254, 243, 199)
    canvas.drawRoundRect(380f, currentY, 380f + tileW, currentY + tileH, 6f, 6f, paint)
    canvas.drawText("MISTAKE VAULT RETENTION", 390f, currentY + 14f, smallPaint)
    paint.color = AndroidColor.rgb(180, 83, 9)
    canvas.drawText("$masteredCount Mastered / $activeMistakes Active", 390f, currentY + 30f, boldBodyPaint)

    currentY += 56f

    // 5. Section: Paper-wise Syllabus Audit Table
    canvas.drawText("1. OFFICIAL ICAI SYLLABUS READINESS STATUS", 30f, currentY, boldBodyPaint)
    currentY += 12f

    // Table Header
    paint.color = AndroidColor.rgb(226, 232, 240)
    canvas.drawRect(30f, currentY, 565f, currentY + 16f, paint)
    canvas.drawText("Paper", 35f, currentY + 12f, boldBodyPaint)
    canvas.drawText("Subject Name", 75f, currentY + 12f, boldBodyPaint)
    canvas.drawText("Group", 270f, currentY + 12f, boldBodyPaint)
    canvas.drawText("Done", 330f, currentY + 12f, boldBodyPaint)
    canvas.drawText("Pending", 380f, currentY + 12f, boldBodyPaint)
    canvas.drawText("Revise", 440f, currentY + 12f, boldBodyPaint)
    canvas.drawText("Progress", 500f, currentY + 12f, boldBodyPaint)

    currentY += 16f

    IcaiSyllabusData.papers.forEachIndexed { idx, p ->
      val pTopics = allTopics.filter { it.paperId == p.id }
      val pTotal = pTopics.size.coerceAtLeast(1)
      val pDone = pTopics.count { it.status == TopicStatus.DONE }
      val pPending = pTopics.count { it.status == TopicStatus.PENDING }
      val pRevise = pTopics.count { it.status == TopicStatus.REVISE }
      val pPct = (pDone * 100 / pTotal)

      // Alternate row shading
      if (idx % 2 == 1) {
        paint.color = AndroidColor.rgb(248, 250, 252)
        canvas.drawRect(30f, currentY, 565f, currentY + 15f, paint)
      }

      canvas.drawText("P${p.id}", 35f, currentY + 11f, boldBodyPaint)
      canvas.drawText(p.name.take(30), 75f, currentY + 11f, bodyPaint)
      canvas.drawText(if (p.group == GroupType.GROUP_1) "Group 1" else "Group 2", 270f, currentY + 11f, smallPaint)
      canvas.drawText("$pDone", 330f, currentY + 11f, bodyPaint)
      canvas.drawText("$pPending", 380f, currentY + 11f, bodyPaint)
      canvas.drawText("$pRevise", 440f, currentY + 11f, bodyPaint)
      canvas.drawText("$pPct%", 500f, currentY + 11f, boldBodyPaint)

      currentY += 15f
    }

    currentY += 18f

    // 6. Section: Mistake Vault & Spaced Repetition Weak-Spot Audit
    canvas.drawText("2. MISTAKE VAULT & 4-STEP FRAMEWORK DEFICIENCY AUDIT", 30f, currentY, boldBodyPaint)
    currentY += 12f

    if (mistakes.isEmpty()) {
      paint.color = AndroidColor.rgb(241, 245, 249)
      canvas.drawRoundRect(30f, currentY, 565f, currentY + 30f, 6f, 6f, paint)
      canvas.drawText("No deficiencies currently recorded in Mistake Vault. All test attempts met ICAI 4-step marking criteria.", 40f, currentY + 18f, bodyPaint)
      currentY += 40f
    } else {
      // Table Header for Mistakes
      paint.color = AndroidColor.rgb(226, 232, 240)
      canvas.drawRect(30f, currentY, 565f, currentY + 16f, paint)
      canvas.drawText("Standard / Law", 35f, currentY + 12f, boldBodyPaint)
      canvas.drawText("Question Description", 125f, currentY + 12f, boldBodyPaint)
      canvas.drawText("Omitted Technical Keywords", 340f, currentY + 12f, boldBodyPaint)
      canvas.drawText("Schedule", 495f, currentY + 12f, boldBodyPaint)
      currentY += 16f

      val dateFmt = SimpleDateFormat("dd MMM", Locale.getDefault())

      mistakes.take(8).forEachIndexed { mIdx, m ->
        if (mIdx % 2 == 1) {
          paint.color = AndroidColor.rgb(248, 250, 252)
          canvas.drawRect(30f, currentY, 565f, currentY + 22f, paint)
        }

        canvas.drawText(m.topicOrStandard.take(15), 35f, currentY + 14f, boldBodyPaint)
        canvas.drawText(m.question.take(38) + if (m.question.length > 38) "..." else "", 125f, currentY + 14f, bodyPaint)
        canvas.drawText(m.missingKeywords.take(28) + if (m.missingKeywords.length > 28) "..." else "", 340f, currentY + 14f, smallPaint)

        val schedText = if (m.isMastered) "Mastered" else "${m.reviewIntervalDays}d (${dateFmt.format(Date(m.nextReviewDate))})"
        canvas.drawText(schedText, 495f, currentY + 14f, boldBodyPaint)

        currentY += 22f
      }
    }

    currentY += 10f

    // 7. Footer
    paint.color = AndroidColor.rgb(203, 213, 225)
    canvas.drawLine(30f, 810f, 565f, 810f, paint)
    canvas.drawText("Confidential CA Student Study Dossier • Auto-generated by CA Inter Study Desk Pro • Offline Database Sync Active", 30f, 824f, smallPaint)
    canvas.drawText("Page 1 of 1", 520f, 824f, smallPaint)

    pdfDocument.finishPage(page)

    // Save PDF file in cache
    val reportsDir = File(context.cacheDir, "pdf_reports")
    if (!reportsDir.exists()) reportsDir.mkdirs()

    val file = File(reportsDir, "CA_Inter_Study_Report_${System.currentTimeMillis()}.pdf")
    val fos = FileOutputStream(file)
    pdfDocument.writeTo(fos)
    fos.close()
    pdfDocument.close()

    return@withContext file
  }

  fun shareOrViewPdf(context: Context, file: File) {
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
      type = "application/pdf"
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, "CA Inter Jan 2027 Study & Mistake Vault Report")
      putExtra(Intent.EXTRA_TEXT, "Here is my official CA Inter Study Desk Pro performance audit and mistake vault review dossier.")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(sendIntent, "Download / Share Study Report PDF").apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
  }
}
