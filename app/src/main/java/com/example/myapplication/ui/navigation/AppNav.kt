// Author: PKQ32
// Created: 2026-07-11
// Description: 应用 Compose 导航图：列表、编辑、运行状态、权限中心

package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.editor.MiniAppEditorScreen
import com.example.myapplication.ui.list.MiniAppListScreen
import com.example.myapplication.ui.permissions.PermissionCenterScreen
import com.example.myapplication.ui.run.RunStatusScreen

object Routes {
    const val LIST = "list"
    const val EDITOR = "editor/{id}"
    const val RUN = "run/{id}"
    const val PERMISSIONS = "permissions"

    fun editor(id: String) = "editor/$id"
    fun run(id: String) = "run/$id"
}

/**
 * 根导航。若 [deepLinkMiniAppId] 非空，启动后跳转到运行页（桌面快捷方式 / deep link）。
 */
@Composable
fun AppNav(deepLinkMiniAppId: String? = null) {
    val navController = rememberNavController()

    LaunchedEffect(deepLinkMiniAppId) {
        val id = deepLinkMiniAppId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        navController.navigate(Routes.run(id)) {
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.LIST,
    ) {
        composable(Routes.LIST) {
            MiniAppListScreen(
                onOpenEditor = { id -> navController.navigate(Routes.editor(id)) },
                onRun = { id -> navController.navigate(Routes.run(id)) },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
            )
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            MiniAppEditorScreen(
                miniAppId = id,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.RUN,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            RunStatusScreen(
                miniAppId = id,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PERMISSIONS) {
            PermissionCenterScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
