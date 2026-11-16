// Author: PKQ32
// Created: 2026-11-16
// Description: 权限中心状态层：系统能力授权快照与恢复前台时刷新

package com.example.myapplication.ui.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.platform.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PermissionSnapshot(
    val notifications: Boolean = false,
    val usageAccess: Boolean = false,
    val writeSettings: Boolean = false,
    val dndAccess: Boolean = false,
    val exactAlarm: Boolean = false,
)

class PermissionCenterViewModel : ViewModel() {

    private val _state = MutableStateFlow(PermissionSnapshot())
    val state: StateFlow<PermissionSnapshot> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val context = ServiceLocator.application
        _state.value = PermissionSnapshot(
            notifications = PermissionHelper.hasNotificationsPermission(context),
            usageAccess = PermissionHelper.hasUsageAccess(context),
            writeSettings = PermissionHelper.canWriteSettings(context),
            dndAccess = PermissionHelper.hasNotificationPolicyAccess(context),
            exactAlarm = PermissionHelper.canScheduleExactAlarms(context),
        )
        // 授权可能改变 appOpened / 精确定时等触发器能力
        viewModelScope.launch {
            runCatching { ServiceLocator.triggerManager.resync() }
        }
    }
}
