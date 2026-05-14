package com.kundli.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import java.util.Calendar

class MiniWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) update(ctx, mgr, id)
    }
    companion object {
        private const val LAT = 28.6139; private const val LON = 77.2090
        private val DIR_ARROW = mapOf("N" to "↑", "E" to "→", "S" to "↓", "W" to "←")
        fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val p = PanchangCalc.computeFor(Calendar.getInstance(), LAT, LON)
            val v = RemoteViews(ctx.packageName, R.layout.widget_mini)
            val isPrimaryOK = p.primary != p.blocked
            val dir = if (isPrimaryOK) p.primary else (if (p.secondary != p.blocked) p.secondary else "—")
            v.setTextViewText(R.id.mi_arrow, DIR_ARROW[dir] ?: "·")
            v.setTextColor(R.id.mi_arrow, if (isPrimaryOK) Color.parseColor("#6AB04C") else Color.parseColor("#D4A017"))
            v.setTextViewText(R.id.mi_dir, dir)
            v.setTextViewText(R.id.mi_tithi, "${p.paksha[0]}.${(p.tithiNum % 15) + 1} ${p.tithiName}")
            v.setTextViewText(R.id.mi_nak, p.nakshatra.name)
            val pi = PendingIntent.getActivity(ctx, id, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            v.setOnClickPendingIntent(R.id.mi_root, pi)
            mgr.updateAppWidget(id, v)
        }
        fun refreshAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            for (id in mgr.getAppWidgetIds(ComponentName(ctx, MiniWidget::class.java))) update(ctx, mgr, id)
        }
    }
}
