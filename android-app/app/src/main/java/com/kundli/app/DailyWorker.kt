package com.kundli.app

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Runs once a day:
 *   1. Computes today's good directions / blocked direction.
 *   2. Posts a notification.
 *   3. Forces every home-screen widget instance to refresh.
 */
class DailyWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val info = DirectionCalc.computeFor(Calendar.getInstance())

        val good = if (info.good.isNotEmpty())
            info.good.joinToString(", ") { DirectionCalc.DIR_NAME[it]!! }
        else "—"
        val blocked = "${DirectionCalc.DIR_NAME[info.blocked]} (Disha Shool)"

        val title = "${info.weekdayFull} — Moon in ${info.moonSign}"
        val body = "Good: $good   •   Avoid: $blocked"

        val openIntent = Intent(ctx, MainActivity::class.java)
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getActivity(ctx, 0, openIntent, flags)

        val notif = NotificationCompat.Builder(ctx, "kundli_daily")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1001, notif)

        // Refresh all widget instances
        DirectionWidget.refreshAll(ctx)
        PanchangWidget.refreshAll(ctx)
        MuhuratWidget.refreshAll(ctx)
        FestivalWidget.refreshAll(ctx)
        MiniWidget.refreshAll(ctx)
        FoodWidget.refreshAll(ctx)

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "kundli_daily_direction"

        /** Schedule the daily worker to fire at the next 07:00 local. */
        fun schedule(ctx: Context) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 7)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_MONTH, 1)
            }
            val initialDelayMs = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<DailyWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
