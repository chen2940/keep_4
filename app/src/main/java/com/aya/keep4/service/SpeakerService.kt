package com.aya.keep4.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.aya.keep4.R
import com.aya.keep4.audio.PlaybackMonitor
import com.aya.keep4.audio.QuadSpeakerTrigger
import com.aya.keep4.data.AppState
import com.aya.keep4.data.SettingsStore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 四扬保活前台服务。
 *
 * 事件驱动：注册 AudioPlaybackCallback，检测到媒体播放时启动触发器（AudioRecord 丢弃 /
 * 通信模式），播放停止时停止触发器。以前台服务 + 常驻通知保活，替代原来只依赖
 * Activity 存活 + 1s 轮询的做法。
 */
class SpeakerService : Service() {

    private var monitor: PlaybackMonitor? = null
    // Created lazily after Context attach (onCreate); getSystemService would NPE otherwise
    private val trigger by lazy { QuadSpeakerTrigger(this) }
    private val settings by lazy { SettingsStore(this) }

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
        } catch (e: Exception) {
            Log.e(TAG, "createNotificationChannel failed", e)
        }
        startAsForeground() // 内部自带 fallback，不抛出

        try {
            val m = PlaybackMonitor(this) { active ->
                if (active) {
                    startTriggerIfNeeded()
                } else {
                    trigger.stop()
                    updateNotification()
                }
            }
            monitor = m
            m.start()
            AppState.serviceRunning.value = true
            instance = this
            if (m.isPlaybackActive) {
                startTriggerIfNeeded()
            }
        } catch (e: Exception) {
            Log.e(TAG, "monitor init failed", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        // 初始化在 onCreate 完成；START_STICKY 保证进程被杀后系统会重建服务
        return START_STICKY
    }

    private fun startTriggerIfNeeded() {
        if (AppState.triggerActive.value) return
        val strategy = if (settings.communicationMode) {
            QuadSpeakerTrigger.Strategy.COMMUNICATION_MODE
        } else {
            QuadSpeakerTrigger.Strategy.AUDIO_RECORD
        }
        trigger.start(strategy)
        updateNotification()
    }

    /** 切换触发方式后立即用新策略重启触发器 */
    private fun restartTrigger() {
        try {
            if (AppState.triggerActive.value) {
                trigger.stop()
                startTriggerIfNeeded()
            }
        } catch (e: Exception) {
            Log.e(TAG, "restartTrigger failed", e)
        }
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            // 部分模拟器/系统对 microphone 类型 FGS 有限制，退回无类型启动，避免整个应用崩溃
            Log.e(TAG, "startForeground(type=microphone) failed, fallback to plain", e)
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (e2: Exception) {
                Log.e(TAG, "startForeground(plain) failed", e2)
                throw e2
            }
        }
    }

    private fun buildNotification(): Notification {
        val triggering = AppState.triggerActive.value
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(
                getString(
                    if (triggering) R.string.notification_triggering
                    else R.string.notification_waiting
                )
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        monitor?.stop()
        monitor = null
        trigger.stop()
        instance = null
        AppState.serviceRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        @Volatile
        private var instance: SpeakerService? = null

        /** 供界面在切换免录音模式后调用，立即生效 */
        fun restartTriggerIfNeeded() {
            instance?.restartTrigger()
        }

        const val ACTION_START = "com.aya.keep4.action.START"
        const val ACTION_STOP = "com.aya.keep4.action.STOP"
        private const val CHANNEL_ID = "keep4_service"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "SpeakerService"
    }
}
