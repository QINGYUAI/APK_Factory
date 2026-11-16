// Author: PKQ32
// Created: 2026-07-11
// Description: 应用 Compose 导航：列表、独立入口、编辑、运行、历史、权限中心

package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.ui.create.CreateMiniAppWizardScreen
import com.example.myapplication.ui.editor.MiniAppEditorScreen
import com.example.myapplication.ui.list.MiniAppListScreen
import com.example.myapplication.ui.permissions.PermissionCenterScreen
import com.example.myapplication.ui.run.RunHistoryScreen
import com.example.myapplication.ui.run.RunStatusScreen
import com.example.myapplication.ui.standalone.MiniAppStandaloneScreen

object Routes {
    const val LIST = "list"
    const val CREATE = "create"
    const val STANDALONE = "standalone/{id}"
    const val EDITOR = "editor/{id}"
    const val RUN = "run/{id}"
    const val HISTORY = "history/{id}"
    const val PERMISSIONS = "permissions"

    fun standalone(id: String) = "standalone/$id"
    fun editor(id: String) = "editor/$id"
    fun run(id: String) = "run/$id"
    fun history(id: String) = "history/$id"
}

/**
 * deep link 目标。token 用于区分「同一个 id 的再次打开」，
 * 避免 LaunchedEffect 因参数相等而不重新导航。
 */
data class DeepLinkTarget(
    val miniAppId: String,
    val token: Long,
)

/**
 * 根导航。桌面快捷方式 / deep link 进入 [standalone] 独立控制台（可配置、可运行）。
 */
@Composable
fun AppNav(deepLinkTarget: DeepLinkTarget? = null) {
    val navController = rememberNavController()

    LaunchedEffect(deepLinkTarget) {
        val id = deepLinkTarget?.miniAppId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
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
                onCreate = { navController.navigate(Routes.CREATE) },
                onRun = { id -> navController.navigate(Routes.run(id)) },
                onOpenStandalone = { id -> navController.navigate(Routes.standalone(id)) },
                onOpenHistory = { id -> navController.navigate(Routes.history(id)) },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
            )
        }
        composable(Routes.CREATE) {
            CreateMiniAppWizardScreen(
                onBack = { navController.popBackStack() },
                onFinished = { id, runNow ->
                    navController.popBackStack()
                    if (runNow) {
                        navController.navigate(Routes.run(id))
                    } else {
                        navController.navigate(Routes.editor(id))
                    }
                },
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
                onOpenHistory = { navController.navigate(Routes.history(id)) },
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
        composable(
            route = Routes.HISTORY,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            var appName by remember(id) { mutableStateOf("") }
            LaunchedEffect(id) {
                appName = ServiceLocator.miniApps.get(id)?.name.orEmpty()
            }
            RunHistoryScreen(
                miniAppId = id,
                appName = appName,
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
