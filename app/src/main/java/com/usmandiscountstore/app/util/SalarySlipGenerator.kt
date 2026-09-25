package com.usmandiscountstore.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.usmandiscountstore.app.data.local.entity.AdvanceEntity
import com.usmandiscountstore.app.data.local.entity.AttendanceEntity
import com.usmandiscountstore.app.data.local.entity.StaffEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SalaryBreakdown(
    val staff: StaffEntity,
    val monthYear: String,
    val presentDays: Int,
    val halfDays: Int,
    val leaveDays: Int,
    val absentDays: Int,
    val dailyWage: Double,
    val presentAmount: Double,
    val halfDayAmount: Double,
    val grossSalary: Double,
    val totalAdvance: Double,
    val netPayable: Double,
    val attendanceList: List<AttendanceEntity>,
    val advanceList: List<AdvanceEntity>
)

object SalarySlipGenerator {

    fun calculate(
        staff: StaffEntity,
        monthYear: String,
        attendance: List<AttendanceEntity>,
        advances: List<AdvanceEntity>
    ): SalaryBreakdown {

        val monthAtt = attendance.filter {
            it.staffId == staff.id && it.date.startsWith(monthYear)
        }
        val present = monthAtt.count { it.status == "PRESENT" }
        val half = monthAtt.count { it.status == "HALF_DAY" }
        val leave = monthAtt.count { it.status == "LEAVE" }
        val absent = monthAtt.count { it.status == "ABSENT" }

        val wage = staff.dailyWage
        val presentAmt = present * wage
        val halfAmt = half * wage * 0.5
        val gross = presentAmt + halfAmt

        val monthAdv = advances.filter {
            it.staffId == staff.id && it.date.startsWith(monthYear)
        }
        val advTotal = monthAdv.sumOf { it.amount }
        val net = (gross - advTotal).coerceAtLeast(0.0)

        return SalaryBreakdown(
            staff = staff,
            monthYear = monthYear,
            presentDays = present,
            halfDays = half,
            leaveDays = leave,
            absentDays = absent,
            dailyWage = wage,
            presentAmount = presentAmt,
            halfDayAmount = halfAmt,
            grossSalary = gross,
            totalAdvance = advTotal,
            netPayable = net,
            attendanceList = monthAtt,
            advanceList = monthAdv
        )
    }

    fun generatePdf(context: Context, breakdown: SalaryBreakdown): File {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val green = Color.parseColor("#046A38")
        val dark = Color.parseColor("#1F2937")
        val gray = Color.parseColor("#6B7280")
        val lightGray = Color.parseColor("#F3F4F6")
        val red = Color.parseColor("#DC2626")

        // ===== HEADER =====
        paint.color = green
        canvas.drawRect(0f, 0f, 595f, 110f, paint)

        paint.color = Color.WHITE
        paint.textSize = 26f
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        canvas.drawText("USMAN DISCOUNT STORE", 40f, 50f, paint)

        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Vehari Road, Old Hasilpur | Staff Salary Slip", 40f, 72f, paint)
        canvas.drawText("Contact: 0300-XXXXXXX  |  Terminal: UDS-HSL-01", 40f, 90f, paint)

        // ===== MONTH BANNER =====
        paint.color = lightGray
        canvas.drawRect(0f, 110f, 595f, 140f, paint)
        paint.color = dark
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        canvas.drawText("SALARY MONTH: ${breakdown.monthYear}", 40f, 130f, paint)

        // ===== STAFF DETAILS BOX =====
        var y = 170f
        paint.color = lightGray
        canvas.drawRoundRect(40f, y, 555f, y + 105f, 8f, 8f, paint)

        paint.color = gray
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("STAFF NAME", 55f, y + 22f, paint)
        canvas.drawText("DESIGNATION", 300f, y + 22f, paint)

        paint.color = dark
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        canvas.drawText(breakdown.staff.name, 55f, y + 42f, paint)
        canvas.drawText(breakdown.staff.designation.ifEmpty { breakdown.staff.role }, 300f, y + 42f, paint)

        paint.color = gray
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("PHONE", 55f, y + 65f, paint)
        canvas.drawText("JOIN DATE", 300f, y + 65f, paint)

        paint.color = dark
        paint.textSize = 12f
        canvas.drawText(breakdown.staff.phone.ifEmpty { "—" }, 55f, y + 85f, paint)
        canvas.drawText(breakdown.staff.joinDate.ifEmpty { "—" }, 300f, y + 85f, paint)

        // ===== ATTENDANCE SUMMARY =====
        y += 130f
        paint.color = dark
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        canvas.drawText("ATTENDANCE SUMMARY", 40f, y, paint)

        y += 15f
        val colW = 128f
        val boxes = listOf(
            Triple("Hazir", "${breakdown.presentDays}", green),
            Triple("Half Day", "${breakdown.halfDays}", Color.parseColor("#7C3AED")),
            Triple("Chutti", "${breakdown.leaveDays}", Color.parseColor("#FF9800")),
            Triple("Gair Hazir", "${breakdown.absentDays}", red)
        )
        boxes.forEachIndexed { i, (label, value, color) ->
            val x = 40f + i * colW
            paint.color = lightGray
            canvas.drawRoundRect(x, y, x + colW - 8f, y + 60f, 6f, 6f, paint)

            paint.color = gray
            paint.textSize = 10f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(label, x + 10f, y + 20f, paint)

            paint.color = color
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            canvas.drawText(value, x + 10f, y + 48f, paint)
        }

        // ===== EARNINGS =====
        y += 90f
        paint.color = green
        canvas.drawRect(40f, y, 555f, y + 28f, paint)
        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        canvas.drawText("EARNINGS (Aamdani)", 55f, y + 19f, paint)
        canvas.drawText("AMOUNT", 470f, y + 19f, paint)

        y += 35f
        paint.color = dark
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Present Days (${breakdown.presentDays} × Rs. ${breakdown.dailyWage.toInt()})",
            55f, y, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        canvas.drawText("Rs. ${breakdown.presentAmount.toInt()}", 470f, y, paint)

        y += 22f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Half Days (${breakdown.halfDays} × Rs. ${(breakdown.dailyWage * 0.5).toInt()})",
            55f, y, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        canvas.drawText("Rs. ${breakdown.halfDayAmount.toInt()}", 470f, y, paint)

        y += 25f
        paint.color = lightGray
        canvas.drawRect(40f, y - 12f, 555f, y + 8f, paint)
        paint.color = dark
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        canvas.drawText("GROSS SALARY", 55f, y, paint)
        canvas.drawText("Rs. ${breakdown.grossSalary.toInt()}", 470f, y, paint)

        // ===== DEDUCTIONS =====
        y += 40f
        paint.color = red
        canvas.drawRect(40f, y, 555f, y + 28f, paint)
        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        canvas.drawText("DEDUCTIONS (Katoti)", 55f, y + 19f, paint)
        canvas.drawText("AMOUNT", 470f, y + 19f, paint)

        y += 35f
        paint.color = dark
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Advance / Peshgi", 55f, y, paint)
        paint.color = red
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        canvas.drawText("- Rs. ${breakdown.totalAdvance.toInt()}", 470f, y, paint)

        // ===== NET PAYABLE =====
        y += 45f
        paint.color = green
        canvas.drawRoundRect(40f, y, 555f, y + 60f, 10f, 10f, paint)
        paint.color = Color.WHITE
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        canvas.drawText("NET PAYABLE (Saafi Tankhwah)", 60f, y + 38f, paint)
        paint.textSize = 22f
        canvas.drawText("Rs. ${breakdown.netPayable.toInt()}", 400f, y + 42f, paint)

        // ===== SIGNATURES =====
        y += 130f
        paint.color = gray
        paint.strokeWidth = 1f
        canvas.drawLine(60f, y, 220f, y, paint)
        canvas.drawLine(360f, y, 520f, y, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Staff Signature", 100f, y + 15f, paint)
        canvas.drawText("M. Usman Nawaz (Owner)", 380f, y + 15f, paint)

        // ===== FOOTER =====
        paint.color = gray
        paint.textSize = 9f
        val dateStr = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.US).format(Date())
        canvas.drawText("Generated: $dateStr  |  Computer generated slip — no manual signature required.", 40f, 810f, paint)
        paint.color = green
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        canvas.drawText("© 2026 Mr.DHooM 4K", 450f, 810f, paint)

        pdf.finishPage(page)

        // Save to Downloads
        val dir = File(context.getExternalFilesDir(null), "salary_slips")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Salary_${breakdown.staff.name.replace(" ", "_")}_${breakdown.monthYear}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return file
    }
}
