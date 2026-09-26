package com.usmandiscountstore.app.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Simple i18n for 3 languages:
 *  - en : English
 *  - ur : Roman Urdu
 *  - pa : Punjabi (Shahmukhi)
 */
object Lang {

    const val EN = "en"
    const val UR = "ur"
    const val PA = "pa"

    private const val PREFS = "uds_lang"
    private const val KEY = "current_lang"

    var current: String by mutableStateOf(EN)
        private set

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        current = p.getString(KEY, EN) ?: EN
    }

    fun set(context: Context, lang: String) {
        current = lang
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, lang).apply()
    }

    fun displayName(lang: String): String = when (lang) {
        EN -> "English"
        UR -> "Roman Urdu"
        PA -> "پنجابی"
        else -> "English"
    }

    fun t(key: String): String {
        return when (current) {
            UR -> ur[key] ?: en[key] ?: key
            PA -> pa[key] ?: en[key] ?: key
            else -> en[key] ?: key
        }
    }

    // ============================================================
    // ENGLISH
    // ============================================================
    private val en = mapOf(
        // Common
        "app_name" to "Usman Discount Store",
        "app_address" to "Vehari Road, Old Hasilpur",
        "app_tagline" to "Staff Hazri & Khata Management",
        // Login
        "login_title" to "Login",
        "admin" to "Admin",
        "moderator" to "Moderator",
        "username" to "Username",
        "password" to "Password",
        "login_button" to "Login",
        "wrong_password" to "Wrong password",
        "username_empty" to "Please enter username",
        "hide" to "Hide",
        "show" to "Show",
        // Dashboard
        "welcome" to "Welcome",
        "admin_access" to "ADMIN ACCESS",
        "moderator_access" to "MODERATOR ACCESS",
        "geofence_active" to "Hasilpur Geofence Active",
        "geofence_sub" to "Vehari Road Counter • UDS-HSL-01",
        "store_modules" to "Store Modules",
        "mod_hazri" to "Mark Attendance",
        "mod_hazri_sub" to "GPS + face verify",
        "mod_staff_admin" to "Staff Management",
        "mod_staff_admin_sub" to "Add / edit / delete",
        "mod_staff_mod" to "Staff List",
        "mod_staff_mod_sub" to "View staff details",
        "mod_history" to "Attendance History",
        "mod_history_sub" to "Monthly report + edit",
        "mod_advance" to "Advance & Peshgi Khata",
        "mod_advance_sub" to "Loans and cash records",
        "mod_leave" to "Leave Requests",
        "mod_leave_pending" to "pending approval",
        "mod_leave_none" to "No pending requests",
        "mod_sheet" to "Salary Sheet (All Staff)",
        "mod_sheet_sub" to "Monthly + CSV export",
        "mod_slip" to "Salary Slips & Payroll",
        "mod_slip_sub" to "PDF slip per staff",
        "mod_settings" to "Admin Settings",
        "mod_settings_sub" to "Store + security",
        "logout" to "Logout",
        "language" to "Language",
        "select_language" to "Select Language",
        // Common words
        "yes" to "Yes",
        "no" to "No",
        "cancel" to "Cancel",
        "save" to "Save",
        "close" to "Close",
        "delete" to "Delete",
        "edit" to "Edit",
        "add" to "Add",
        "ok" to "OK",
        "loading" to "Loading...",
        "success" to "Success",
        "error" to "Error",
        "copyright" to "© 2026 Mr.DHooM 4K — All Rights Reserved"
    )

    // ============================================================
    // ROMAN URDU
    // ============================================================
    private val ur = mapOf(
        "app_name" to "Usman Discount Store",
        "app_address" to "Vehari Road, Old Hasilpur",
        "app_tagline" to "Staff Hazri aur Khata Management",
        "login_title" to "Login",
        "admin" to "Admin",
        "moderator" to "Moderator",
        "username" to "Username",
        "password" to "Password",
        "login_button" to "Login Karein",
        "wrong_password" to "Ghalat password",
        "username_empty" to "Username darj karein",
        "hide" to "Chupayein",
        "show" to "Dekhein",
        "welcome" to "Khush Amdeed",
        "admin_access" to "ADMIN ACCESS",
        "moderator_access" to "MODERATOR ACCESS",
        "geofence_active" to "Hasilpur Geofence Active",
        "geofence_sub" to "Vehari Road Counter • UDS-HSL-01",
        "store_modules" to "Store Ke Kaam",
        "mod_hazri" to "Hazri Lagao",
        "mod_hazri_sub" to "GPS aur face verify",
        "mod_staff_admin" to "Mulazimeen Management",
        "mod_staff_admin_sub" to "Staff add / edit / delete",
        "mod_staff_mod" to "Mulazimeen List",
        "mod_staff_mod_sub" to "Staff ki tafseelat dekho",
        "mod_history" to "Hazri History",
        "mod_history_sub" to "Mahine ki report aur edit",
        "mod_advance" to "Advance aur Peshgi Khata",
        "mod_advance_sub" to "Udhaar aur cash ka record",
        "mod_leave" to "Chutti Ki Requests",
        "mod_leave_pending" to "manzoori ka intezaar",
        "mod_leave_none" to "Koi pending request nahi",
        "mod_sheet" to "Salary Sheet (Sab Staff)",
        "mod_sheet_sub" to "Mahine ki tankhwah aur CSV",
        "mod_slip" to "Salary Slips aur Payroll",
        "mod_slip_sub" to "Har staff ki PDF slip",
        "mod_settings" to "Admin Settings",
        "mod_settings_sub" to "Store aur security",
        "logout" to "Logout",
        "language" to "Zubaan",
        "select_language" to "Zubaan Chuno",
        "yes" to "Haan",
        "no" to "Nahi",
        "cancel" to "Cancel",
        "save" to "Save",
        "close" to "Close",
        "delete" to "Delete",
        "edit" to "Edit",
        "add" to "Add",
        "ok" to "Theek",
        "loading" to "Intezaar karein...",
        "success" to "Kamyabi",
        "error" to "Masla",
        "copyright" to "© 2026 Mr.DHooM 4K — Tamam Haqooq Mehfooz"
    )

    // ============================================================
    // PUNJABI (SHAHMUKHI)
    // ============================================================
    private val pa = mapOf(
        "app_name" to "عثمان ڈسکاؤنٹ سٹور",
        "app_address" to "وہاڑی روڈ، پرانا حاصل پور",
        "app_tagline" to "عملے دی حاضری تے کھاتہ",
        "login_title" to "لاگ ان",
        "admin" to "ایڈمن",
        "moderator" to "موڈریٹر",
        "username" to "صارف دا ناں",
        "password" to "پاس ورڈ",
        "login_button" to "لاگ ان کرو",
        "wrong_password" to "غلط پاس ورڈ",
        "username_empty" to "صارف دا ناں لکھو",
        "hide" to "لکاؤ",
        "show" to "دکھاؤ",
        "welcome" to "جی آیاں نوں",
        "admin_access" to "ایڈمن رسائی",
        "moderator_access" to "موڈریٹر رسائی",
        "geofence_active" to "حاصل پور جیو فینس چالو",
        "geofence_sub" to "وہاڑی روڈ کاؤنٹر • UDS-HSL-01",
        "store_modules" to "سٹور دے کم",
        "mod_hazri" to "حاضری لاؤ",
        "mod_hazri_sub" to "جی پی ایس تے چہرہ تصدیق",
        "mod_staff_admin" to "عملے دا انتظام",
        "mod_staff_admin_sub" to "شامل / تبدیل / مٹاؤ",
        "mod_staff_mod" to "عملے دی لسٹ",
        "mod_staff_mod_sub" to "عملے دی تفصیل ویکھو",
        "mod_history" to "حاضری دی تریخ",
        "mod_history_sub" to "مہینے دی رپورٹ تے تبدیلی",
        "mod_advance" to "ایڈوانس تے پیسگی کھاتہ",
        "mod_advance_sub" to "قرضہ تے نقد دا ریکارڈ",
        "mod_leave" to "چھٹی دی درخواست",
        "mod_leave_pending" to "منظوری دی اڈیک",
        "mod_leave_none" to "کوئی درخواست نہیں",
        "mod_sheet" to "تنخواہ شیٹ (سارا عملہ)",
        "mod_sheet_sub" to "مہینے دی تنخواہ تے CSV",
        "mod_slip" to "تنخواہ سلپ تے پے رول",
        "mod_slip_sub" to "ہر بندے دی PDF سلپ",
        "mod_settings" to "ایڈمن ترتیبات",
        "mod_settings_sub" to "سٹور تے سیکیورٹی",
        "logout" to "باہر نکلو",
        "language" to "بولی",
        "select_language" to "بولی چنو",
        "yes" to "ہاں",
        "no" to "نہیں",
        "cancel" to "منسوخ",
        "save" to "محفوظ کرو",
        "close" to "بند کرو",
        "delete" to "مٹاؤ",
        "edit" to "تبدیل کرو",
        "add" to "شامل کرو",
        "ok" to "ٹھیک اے",
        "loading" to "اڈیک کرو...",
        "success" to "کامیابی",
        "error" to "مسئلہ",
        "copyright" to "© 2026 Mr.DHooM 4K — سارے حق محفوظ نیں"
    )
}
