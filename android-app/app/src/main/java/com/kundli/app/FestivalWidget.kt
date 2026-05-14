package com.kundli.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Calendar

class FestivalWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) update(ctx, mgr, id)
    }
    companion object {
        private const val LAT = 28.6139; private const val LON = 77.2090
        fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val today = Calendar.getInstance()
            val p = PanchangCalc.computeFor(today, LAT, LON)
            val todays = PanchangCalc.festivalsFor(p)
            val v = RemoteViews(ctx.packageName, R.layout.widget_festival)
            if (todays.isNotEmpty()) {
                v.setTextViewText(R.id.f_label, "TODAY")
                v.setTextViewText(R.id.f_name, todays.joinToString(" • ") { it.en })
                v.setTextViewText(R.id.f_date, PanchangCalc.dateLabel(today))
            } else {
                val next = PanchangCalc.nextFestival(today, LAT, LON, 90)
                if (next != null) {
                    val (cal, f) = next
                    val daysAway = ((cal.timeInMillis - today.timeInMillis) / (1000L * 60 * 60 * 24)).toInt() + 1
                    v.setTextViewText(R.id.f_label, "NEXT in $daysAway day${if (daysAway == 1) "" else "s"}")
                    v.setTextViewText(R.id.f_name, f.en)
                    v.setTextViewText(R.id.f_date, PanchangCalc.dateLabel(cal))
                } else {
                    v.setTextViewText(R.id.f_label, "NO FESTIVAL")
                    v.setTextViewText(R.id.f_name, "Nothing within 90 days")
                    v.setTextViewText(R.id.f_date, PanchangCalc.dateLabel(today))
                }
            }
            val pi = PendingIntent.getActivity(ctx, id, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            v.setOnClickPendingIntent(R.id.f_root, pi)
            mgr.updateAppWidget(id, v)
        }
        fun refreshAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            for (id in mgr.getAppWidgetIds(ComponentName(ctx, FestivalWidget::class.java))) update(ctx, mgr, id)
        }
    }
}
