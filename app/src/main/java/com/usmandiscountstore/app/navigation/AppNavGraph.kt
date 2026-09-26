package com.usmandiscountstore.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.usmandiscountstore.app.ui.screens.*

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val STAFF_LIST = "staff_list"
    const val ATTENDANCE = "attendance"
    const val HISTORY = "history/{staffId}"
    const val ADMIN_SETTINGS = "admin_settings"
    const val ADVANCE = "advance"
    const val SALARY = "salary"
    const val SALARY_SHEET = "salary_sheet"
    const val LEAVE = "leave/{isAdmin}"
    fun historyRoute(staffId: Long = 0L) = "history/$staffId"
    fun leaveRoute(isAdmin: Boolean) = "leave/$isAdmin"
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) {
            LoginScreen(onLoginSuccess = { _, _ ->
                navController.navigate(Routes.DASHBOARD) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateStaff = { navController.navigate(Routes.STAFF_LIST) },
                onNavigateAttendance = { navController.navigate(Routes.ATTENDANCE) },
                onNavigateHistory = { navController.navigate(Routes.historyRoute(0L)) },
                onNavigateAdvance = { navController.navigate(Routes.ADVANCE) },
                onNavigateSalary = { navController.navigate(Routes.SALARY) },
                onNavigateSalarySheet = { navController.navigate(Routes.SALARY_SHEET) },
                onNavigateLeave = { navController.navigate(Routes.leaveRoute(true)) },
                onNavigateSettings = { navController.navigate(Routes.ADMIN_SETTINGS) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable(Routes.STAFF_LIST) { StaffListScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.ATTENDANCE) { AttendanceScreen(onBack = { navController.popBackStack() }) }
        composable(
            route = Routes.HISTORY,
            arguments = listOf(navArgument("staffId") { type = NavType.LongType; defaultValue = 0L })
        ) { entry ->
            val sid = entry.arguments?.getLong("staffId") ?: 0L
            AttendanceHistoryScreen(staffId = sid, onBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN_SETTINGS) { AdminSettingsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.ADVANCE) { AdvanceKhataScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SALARY) { SalarySlipScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SALARY_SHEET) { SalarySheetScreen(onBack = { navController.popBackStack() }) }
        composable(
            route = Routes.LEAVE,
            arguments = listOf(navArgument("isAdmin") { type = NavType.BoolType; defaultValue = true })
        ) { entry ->
            val isAdmin = entry.arguments?.getBoolean("isAdmin") ?: true
            LeaveRequestScreen(isAdminView = isAdmin, onBack = { navController.popBackStack() })
        }
    }
}
