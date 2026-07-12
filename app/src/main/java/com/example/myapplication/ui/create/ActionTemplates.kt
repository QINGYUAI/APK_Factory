// Author: PKQ32
// Created: 2026-07-12
// Description: 新建小应用模板（动作链预设）

package com.example.myapplication.ui.create

import com.example.myapplication.domain.model.ActionStep
import com.example.myapplication.domain.model.ActionTypes
import java.util.UUID

data class ActionTemplate(
    val key: String,
    val titleZh: String,
    val subtitle: String,
    val buildSteps: () -> List<ActionStep>,
)

object ActionTemplates {

    val all: List<ActionTemplate> = listOf(
        ActionTemplate(
            key = "blank",
            titleZh = "空白",
            subtitle = "稍后自行添加动作",
        ) {
            emptyList()
        },
        ActionTemplate(
            key = "acceptance",
            titleZh = "验收模板",
            subtitle = "震动 → 延时 → 音量 → 通知",
        ) {
            listOf(
                step(ActionTypes.VIBRATE, mapOf("millis" to "200"), 0),
                step(ActionTypes.DELAY, mapOf("millis" to "1000"), 1),
                step(ActionTypes.SET_VOLUME, mapOf("stream" to "music", "percent" to "50"), 2),
                step(ActionTypes.NOTIFY, mapOf("title" to "小应用", "text" to "执行完成"), 3),
            )
        },
        ActionTemplate(
            key = "music",
            titleZh = "音乐提醒",
            subtitle = "延时 → 震动 → 播放音乐",
        ) {
            listOf(
                step(ActionTypes.DELAY, mapOf("millis" to "500"), 0),
                step(ActionTypes.VIBRATE, mapOf("millis" to "150"), 1),
                step(ActionTypes.PLAY_MUSIC, mapOf("uri" to "", "path" to "", "displayName" to ""), 2),
            )
        },
        ActionTemplate(
            key = "quiet",
            titleZh = "勿扰准备",
            subtitle = "音量 0 → 震动模式 → 通知",
        ) {
            listOf(
                step(ActionTypes.SET_VOLUME, mapOf("stream" to "music", "percent" to "0"), 0),
                step(ActionTypes.SET_RINGER_MODE, mapOf("mode" to "vibrate"), 1),
                step(ActionTypes.NOTIFY, mapOf("title" to "勿扰", "text" to "已切换震动"), 2),
            )
        },
    )

    private fun step(type: String, params: Map<String, String>, order: Int) = ActionStep(
        id = UUID.randomUUID().toString(),
        type = type,
        params = params,
        order = order,
    )
}
