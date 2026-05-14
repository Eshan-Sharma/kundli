package com.kundli.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Calendar

class MuhuratWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) update(ctx, mgr, id)
    }
    companion object {
        private const val LAT = 28.6139; private const val LON = 77.2090
        fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val p = PanchangCalc.computeFor(Calendar.getInstance(), LAT, LON)
            val v = RemoteViews(ctx.packageName, R.layout.widget_muhurat)
            v.setTextViewText(R.id.m_date,    PanchangCalc.dateLabel(p.date))
            v.setTextViewText(R.id.m_abhijit, "${PanchangCalc.fmtTime(p.abhijit.startHour)} – ${PanchangCalc.fmtTime(p.abhijit.endHour)}")
            v.setTextViewText(R.id.m_brahma,  "${PanchangCalc.fmtTime(p.brahma.startHour)} – ${PanchangCalc.fmtTime(p.brahma.endHour)}")
            v.setTextViewText(R.id.m_rahu,    "${PanchangCalc.fmtTime(p.rahu.startHour)} – ${PanchangCalc.fmtTime(p.rahu.endHour)}")
            v.setTextViewText(R.id.m_yama,    "${PanchangCalc.fmtTime(p.yama.startHour)} – ${PanchangCalc.fmtTime(p.yama.endHour)}")
            v.setTextViewText(R.id.m_gulika,  "${PanchangCalc.fmtTime(p.gulika.startHour)} – ${PanchangCalc.fmtTime(p.gulika.endHour)}")
            val pi = PendingIntent.getActivity(ctx, id, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            v.setOnClickPendingIntent(R.id.m_root, pi)
            mgr.updateAppWidget(id, v)
        }
        fun refreshAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            for (id in mgr.getAppWidgetIds(ComponentName(ctx, MuhuratWidget::class.java))) update(ctx, mgr, id)
        }
    }
}
