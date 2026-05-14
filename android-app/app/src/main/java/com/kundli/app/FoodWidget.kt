package com.kundli.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Calendar

class FoodWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) update(ctx, mgr, id)
    }
    companion object {
        private const val LAT = 28.6139; private const val LON = 77.2090
        fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val p = PanchangCalc.computeFor(Calendar.getInstance(), LAT, LON)
            val d = PanchangCalc.dietaryFor(p)
            val v = RemoteViews(ctx.packageName, R.layout.widget_food)
            v.setTextViewText(R.id.fo_date, "${PanchangCalc.WEEKDAY_SHORT[p.weekday]} • ${p.paksha} ${p.tithiName} • ${p.hinduMonth}")
            v.setTextViewText(R.id.fo_tithi_avoid, "🚫 Tithi: ${d.tithiAvoid}")
            v.setTextViewText(R.id.fo_vaar_avoid,  "🚫 ${PanchangCalc.WEEKDAY_FULL[p.weekday]} (${d.vaarGraha}): ${d.vaarAvoid}")
            v.setTextViewText(R.id.fo_maas_avoid,  "🚫 ${p.hinduMonth}: ${d.maasAvoid}")
            v.setTextViewText(R.id.fo_maas_prefer, "✓ Prefer: ${d.maasPrefer}")
            v.setTextViewText(R.id.fo_tree, "🌳 ${p.nakshatra.name} tree: ${d.nakTree}")
            val pi = PendingIntent.getActivity(ctx, id, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            v.setOnClickPendingIntent(R.id.fo_root, pi)
            mgr.updateAppWidget(id, v)
        }
        fun refreshAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            for (id in mgr.getAppWidgetIds(ComponentName(ctx, FoodWidget::class.java))) update(ctx, mgr, id)
        }
    }
}
