package com.kundli.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Calendar

class PanchangWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) update(ctx, mgr, id)
    }
    companion object {
        private const val LAT = 28.6139; private const val LON = 77.2090
        fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val p = PanchangCalc.computeFor(Calendar.getInstance(), LAT, LON)
            val v = RemoteViews(ctx.packageName, R.layout.widget_panchang)
            v.setTextViewText(R.id.p_date, "${PanchangCalc.WEEKDAY_SHORT[p.weekday]} • ${PanchangCalc.dateLabel(p.date)}")
            v.setTextViewText(R.id.p_tithi, "${p.paksha} ${p.tithiName}")
            v.setTextViewText(R.id.p_nak,  "${p.nakshatra.name} P${p.nakshatra.pada}")
            v.setTextViewText(R.id.p_yoga, "${p.yogaName}  •  ${p.karanaName}")
            v.setTextViewText(R.id.p_sun,  "☀ ${PanchangCalc.fmtTime(p.sun.sunrise)} – ${PanchangCalc.fmtTime(p.sun.sunset)}")
            v.setTextViewText(R.id.p_maas, "${p.hinduMonth} • ${p.ritu}")
            val pi = PendingIntent.getActivity(ctx, id, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            v.setOnClickPendingIntent(R.id.p_root, pi)
            mgr.updateAppWidget(id, v)
        }
        fun refreshAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            for (id in mgr.getAppWidgetIds(ComponentName(ctx, PanchangWidget::class.java))) update(ctx, mgr, id)
        }
    }
}
