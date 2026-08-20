package com.aya.keep4.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.aya.keep4.data.AppState
import com.aya.keep4.data.SettingsStore

/**
 * 无障碍保活服务：系统对无障碍服务有强守护（被清理后会自动重启），
 * 连接后作为 watchdog 自动拉起四扬保活服务，实现强力保活。
 */
class KeepAliveAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "connected")
        AppState.accessibilityActive.value = true
        try {
            val settings = SettingsStore(this)
            if (settings.enabled) {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, SpeakerService::class.java)
                        .setAction(SpeakerService.ACTION_START)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "start SpeakerService failed", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 无需处理事件，仅用于保活
    }

    override fun onInterrupt() {
        // 无需处理
    }

    override fun onDestroy() {
        AppState.accessibilityActive.value = false
        super.onDestroy()
    }

    companion object {
        private const val TAG = "KeepAliveAccessibility"
    }
}