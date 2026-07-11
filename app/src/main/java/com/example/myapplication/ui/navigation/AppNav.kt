// Author: PKQ32
// Created: 2026-07-11
// Description: 应用 Compose 导航图：列表、编辑、运行、权限占位页

package com.example.myapplication.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.editor.MiniAppEditorScreen
import com.example.myapplication.ui.list.MiniAppListScreen

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
            PlaceholderScreen(title = "运行页（待实现）", subtitle = "id=$id")
        }
        composable(Routes.PERMISSIONS) {
            PlaceholderScreen(title = "权限引导（待实现）")
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String? = null,
) {
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (subtitle != null) "$title\n$subtitle" else title,
            )
        }
    }
}
