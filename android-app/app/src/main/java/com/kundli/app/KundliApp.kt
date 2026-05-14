package com.kundli.app

import android.app.Application

class KundliApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Schedule the daily worker at first launch.
        DailyWorker.schedule(this)
    }
}
