package com.aya.keep4.data

import kotlinx.coroutines.flow.MutableStateFlow

/** 全局运行状态，供 UI 观察展示。 */
object AppState {
    /** 保活前台服务是否运行中 */
    val serviceRunning = MutableStateFlow(false)

    /** 四扬触发是否生效（录音中 / 通信模式已切入） */
    val triggerActive = MutableStateFlow(false)

    /** 无障碍保活服务是否连接 */
    val accessibilityActive = MutableStateFlow(false)

    /** 系统当前是否有媒体在播放 */
    val playbackActive = MutableStateFlow(false)
}
