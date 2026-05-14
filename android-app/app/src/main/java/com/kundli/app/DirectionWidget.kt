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

class DirectionWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) updateWidget(context, appWidgetManager, id)
    }

    companion object {
        /** Push a fresh render of today's direction into the given widget instance. */
        fun updateWidget(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val info = DirectionCalc.computeFor(Calendar.getInstance())

            val views = RemoteViews(context.packageName, R.layout.widget_4x2)
            views.setTextViewText(R.id.w_date, "${info.weekdayShort} • ${info.dateLabel}")
            views.setTextViewText(R.id.w_moon, "Moon in ${info.moonSign}")

            // Render four cardinal arrows N E S W with colour coding.
            views.setTextViewText(R.id.w_arrow_n, "↑")
            views.setTextViewText(R.id.w_arrow_e, "→")
            views.setTextViewText(R.id.w_arrow_s, "↓")
            views.setTextViewText(R.id.w_arrow_w, "←")

            views.setTextViewText(R.id.w_label_n, "N")
            views.setTextViewText(R.id.w_label_e, "E")
            views.setTextViewText(R.id.w_label_s, "S")
            views.setTextViewText(R.id.w_label_w, "W")

            val GOOD = Color.parseColor("#6AB04C")
            val GOLD = Color.parseColor("#D4A017")
            val BAD  = Color.parseColor("#E74C3C")
            val DIM  = Color.parseColor("#5A4A2F")

            fun colorFor(dir: String): Int {
                if (dir == info.blocked) return BAD
                if (dir == info.primary && !info.primaryBlocked) return GOOD
                if (dir == info.secondary && !info.secondaryBlocked) return GOLD
                return DIM
            }

            views.setTextColor(R.id.w_arrow_n, colorFor("N"))
            views.setTextColor(R.id.w_label_n, colorFor("N"))
            views.setTextColor(R.id.w_arrow_e, colorFor("E"))
            views.setTextColor(R.id.w_label_e, colorFor("E"))
            views.setTextColor(R.id.w_arrow_s, colorFor("S"))
            views.setTextColor(R.id.w_label_s, colorFor("S"))
            views.setTextColor(R.id.w_arrow_w, colorFor("W"))
            views.setTextColor(R.id.w_label_w, colorFor("W"))

            // Summary line
            val goodText = if (info.good.isNotEmpty())
                "Go ${info.good.joinToString(", ") { DirectionCalc.DIR_NAME[it]!! }}"
            else "All recommended directions blocked"
            views.setTextViewText(R.id.w_good, goodText)
            views.setTextViewText(
                R.id.w_blocked,
                "Avoid: ${DirectionCalc.DIR_NAME[info.blocked]} (Disha Shool)"
            )

            // Tap → open app
            val tapIntent = Intent(context, MainActivity::class.java)
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
            val pi = PendingIntent.getActivity(context, widgetId, tapIntent, flags)
            views.setOnClickPendingIntent(R.id.w_root, pi)

            mgr.updateAppWidget(widgetId, views)
        }

        /** Convenience: refresh ALL instances of this widget on the home screen. */
        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val cn = ComponentName(context, DirectionWidget::class.java)
            val ids = mgr.getAppWidgetIds(cn)
            for (id in ids) updateWidget(context, mgr, id)
        }
    }
}
