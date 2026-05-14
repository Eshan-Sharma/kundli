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
 * 4×2 widget showing the user's reminders that match today.
 * Resizable from 2×1 up to 4×3.
 */
class ReminderListWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) update(ctx, mgr, id)
    }
    companion object {
        private const val LAT = 28.6139; private const val LON = 77.2090
        fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val cal = Calendar.getInstance()
            val pan = PanchangCalc.computeFor(cal, LAT, LON)
            val rules = ReminderStore.load(ctx)
            val matching = ReminderStore.matchesToday(rules, pan, cal)

            val v = RemoteViews(ctx.packageName, R.layout.widget_reminder_list)
            v.setTextViewText(R.id.rl_date, "${PanchangCalc.WEEKDAY_SHORT[pan.weekday]} • ${PanchangCalc.dateLabel(cal)}")
            v.setTextViewText(R.id.rl_header, if (matching.isEmpty()) "No reminders today" else "${matching.size} reminder${if (matching.size==1) "" else "s"} today")
            // Up to 4 lines (the layout has 4 slots — extras truncated)
            val slots = listOf(R.id.rl_item1, R.id.rl_item2, R.id.rl_item3, R.id.rl_item4)
            for (i in slots.indices) {
                if (i < matching.size) {
                    val r = matching[i]
                    val line = if (r.message.isNotEmpty()) "${r.name} — ${r.message}" else r.name
                    v.setTextViewText(slots[i], "• $line")
                    v.setViewVisibility(slots[i], android.view.View.VISIBLE)
                } else {
                    v.setViewVisibility(slots[i], android.view.View.GONE)
                }
            }
            if (matching.isEmpty() && rules.isEmpty()) {
                v.setTextViewText(R.id.rl_item1, "Tap to add your first reminder")
                v.setViewVisibility(R.id.rl_item1, android.view.View.VISIBLE)
            }
            val tap = PendingIntent.getActivity(
                ctx, id, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.rl_root, tap)
            mgr.updateAppWidget(id, v)
        }
        fun refreshAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            for (id in mgr.getAppWidgetIds(ComponentName(ctx, ReminderListWidget::class.java))) update(ctx, mgr, id)
        }
    }
}
