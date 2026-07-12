// Author: PKQ32
// Created: 2026-07-11
// Description: 应用 Compose 导航：列表、独立入口、编辑、运行、权限中心

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
import com.example.myapplication.ui.standalone.MiniAppStandaloneScreen

object Routes {
    const val LIST = "list"
    const val STANDALONE = "standalone/{id}"
    const val EDITOR = "editor/{id}"
    const val RUN = "run/{id}"
    const val PERMISSIONS = "permissions"

    fun standalone(id: String) = "standalone/$id"
    fun editor(id: String) = "editor/$id"
    fun run(id: String) = "run/$id"
}

/**
 * 根导航。桌面快捷方式 / deep link 进入 [standalone] 独立控制台（可配置、可运行）。
 */
@Composable
fun AppNav(deepLinkMiniAppId: String? = null) {
    val navController = rememberNavController()

    LaunchedEffect(deepLinkMiniAppId) {
        val id = deepLinkMiniAppId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        navController.navigate(Routes.standalone(id)) {
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
                onOpenStandalone = { id -> navController.navigate(Routes.standalone(id)) },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
            )
        }
        composable(
            route = Routes.STANDALONE,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            MiniAppStandaloneScreen(
                miniAppId = id,
                onRun = { navController.navigate(Routes.run(id)) },
                onConfigure = { navController.navigate(Routes.editor(id)) },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                onOpenFactory = {
                    navController.navigate(Routes.LIST) {
                        popUpTo(Routes.LIST) { inclusive = true }
                        launchSingleTop = true
                    }
                },
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
