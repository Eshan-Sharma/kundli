package com.kundli.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-schedules the daily worker after device reboot, and refreshes the widget. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            DailyWorker.schedule(context)
            DirectionWidget.refreshAll(context)
        }
    }
}
