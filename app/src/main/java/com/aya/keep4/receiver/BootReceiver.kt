package com.aya.keep4.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.aya.keep4.data.SettingsStore
import com.aya.keep4.service.SpeakerService

/** 开机后若开关已开启，自动拉起保活服务。 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!SettingsStore(context).enabled) return
        try {
            ContextCompat.startForegroundService(context,
                Intent(context, SpeakerService::class.java)
                    .setAction(SpeakerService.ACTION_START)
            )
        } catch (e: Exception) {
            Log.w(TAG, "start service on boot failed", e)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
