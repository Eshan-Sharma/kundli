package com.kundli.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Calendar

/**
 * 1×1 minimal widget — shows a bell icon with the number of reminders firing today.
 * Tap to open the app.
 */
class ReminderMiniWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) update(ctx, mgr, id)
    }
    companion object {
        private const val LAT = 28.6139; private const val LON = 77.2090
        fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val cal = Calendar.getInstance()
            val pan = PanchangCalc.computeFor(cal, LAT, LON)
            val rules = ReminderStore.load(ctx)
            val count = ReminderStore.matchesToday(rules, pan, cal).size
            val v = RemoteViews(ctx.packageName, R.layout.widget_reminder_mini)
            v.setTextViewText(R.id.rm_count, if (count > 0) count.toString() else "")
            v.setTextViewText(R.id.rm_icon, "🔔")
            v.setTextViewText(R.id.rm_label, if (count == 0) "—" else (if (count == 1) "1 today" else "$count today"))
            val tap = PendingIntent.getActivity(
                ctx, id, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.rm_root, tap)
            mgr.updateAppWidget(id, v)
        }
        fun refreshAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            for (id in mgr.getAppWidgetIds(ComponentName(ctx, ReminderMiniWidget::class.java))) update(ctx, mgr, id)
        }
    }
}
