package com.usmandiscountstore.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.usmandiscountstore.app.data.local.entity.AdvanceEntity
import com.usmandiscountstore.app.data.local.entity.AttendanceEntity
import com.usmandiscountstore.app.data.local.entity.StaffEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    /**
     * Export staff salary sheet as CSV.
     * Columns: Name, Phone, Designation, Present, Half, Leave, Absent, Wage, Gross, Advance, Bonus, Net
     */
    fun exportSalarySheet(
        context: Context,
        monthYear: String,
        rows: List<SalaryRow>
    ): File {
        val sb = StringBuilder()
        // Header
        sb.append("Staff Name,Phone,Designation,Present Days,Half Days,Leave Days,Absent Days,")
        sb.append("Daily Wage,Gross Salary,Total Advance,Total Bonus,Net Payable\n")

        var totalNet = 0.0
        var totalGross = 0.0
        var totalAdv = 0.0
        var totalBonus = 0.0

        rows.forEach { r ->
            sb.append("\"${r.staff.name}\",")
            sb.append("\"${r.staff.phone}\",")
            sb.append("\"${r.staff.designation}\",")
            sb.append("${r.present},${r.half},${r.leave},${r.absent},")
            sb.append("${r.staff.dailyWage},")
            sb.append("${r.gross},")
            sb.append("${r.advance},")
            sb.append("${r.bonus},")
            sb.append("${r.net}\n")
            totalGross += r.gross
            totalAdv += r.advance
            totalBonus += r.bonus
            totalNet += r.net
        }

        sb.append("\nTOTAL,,,,,,,,$totalGross,$totalAdv,$totalBonus,$totalNet\n")
        sb.append("\nMonth: $monthYear\n")
        sb.append("Generated: ${SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.US).format(Date())}\n")
        sb.append("© 2026 Mr.DHooM 4K\n")

        val dir = File(context.getExternalFilesDir(null), "exports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Salary_Sheet_$monthYear.csv")
        file.writeText(sb.toString())
        return file
    }

    fun exportAttendance(
        context: Context,
        staff: StaffEntity,
        monthYear: String,
        attendance: List<AttendanceEntity>
    ): File {
        val sb = StringBuilder()
        sb.append("Date,Status,Check-In Time,Marked By\n")
        attendance.sortedBy { it.date }.forEach { a ->
            sb.append("${a.date},${a.status},${a.checkInTime},\"${a.markedBy}\"\n")
        }
        sb.append("\nStaff: ${staff.name} (${staff.designation})\n")
        sb.append("Month: $monthYear\n")
        sb.append("© 2026 Mr.DHooM 4K\n")

        val dir = File(context.getExternalFilesDir(null), "exports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Attendance_${staff.name.replace(" ","_")}_$monthYear.csv")
        file.writeText(sb.toString())
        return file
    }

    fun shareCsv(context: Context, file: File, title: String = "Share CSV") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    data class SalaryRow(
        val staff: StaffEntity,
        val present: Int,
        val half: Int,
        val leave: Int,
        val absent: Int,
        val gross: Double,
        val advance: Double,
        val bonus: Double,
        val net: Double
    )
}
