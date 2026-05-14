# Kundli — Android App

Personal-use Android wrapper around `kundli.html` with a home-screen widget
and a daily notification showing travel directions (Moon's direction + Disha Shool).

## What's inside

- **MainActivity** — full-screen WebView that loads `kundli.html` from the APK's
  `assets/` folder. Your entire web app works untouched on the phone.
- **DirectionCalc.kt** — Kotlin port of the moon-longitude + sign + Disha-Shool
  pipeline, so the widget and notification do not need the WebView running.
- **DirectionWidget** (4×2 home-screen widget) — shows today's date, weekday,
  moon sign, four cardinal arrows colour-coded (green = primary / Moon's
  direction, gold = secondary / right-of-Moon, red = Disha Shool / avoid),
  plus a "Go N, E" summary line. Tap to open the app. Auto-updates every hour.
- **DailyWorker** — WorkManager job that fires once a day at 07:00 local,
  posts a notification with today's good and blocked directions, and refreshes
  every widget instance.
- **BootReceiver** — re-schedules the daily worker after a reboot.

## Build & install (one-time setup)

You need:

1. **Android Studio** — download from <https://developer.android.com/studio>.
   It bundles the Android SDK and a recent JDK.
2. **A USB cable** + your Android phone with **Developer Options** and
   **USB Debugging** enabled (Settings → About → tap "Build Number" 7 times →
   then Settings → Developer Options → USB Debugging ON).

Then:

1. Open Android Studio → "Open" → pick the `android-app/` folder.
2. Wait for Gradle to sync (it will download the SDK pieces it needs, ~5 min
   on first run). Accept any "install missing components" prompts.
3. Plug your phone in, accept the USB-debugging prompt on the phone.
4. Top toolbar: select your phone from the device dropdown, click the green
   ▶ Run button. The APK builds and installs onto the phone in ~1 minute.

That's it — the app icon appears on your phone. Open it once to grant the
notification permission.

## Adding the widget

Long-press an empty spot on your home screen → Widgets → search "Kundli" →
drag the 4×2 widget onto the home screen.

## Updating the embedded `kundli.html`

The widget calculations are duplicated in Kotlin and won't drift, but the
in-app UI lives in the HTML. To update it:

```
cp ~/Desktop/Developer/Kundli/kundli.html \
   ~/Desktop/Developer/Kundli/android-app/app/src/main/assets/kundli.html
```

…and re-run the app from Android Studio.

## Changing the notification time

Open `app/src/main/java/com/kundli/app/DailyWorker.kt` → look for the
`set(Calendar.HOUR_OF_DAY, 7)` line in `schedule()` and change `7` to your
preferred hour (24-h clock). Re-run.

## Notes

- The widget uses `updatePeriodMillis="3600000"` (1 hour) which is the
  minimum Android allows for AppWidget self-updates. The daily WorkManager
  job also kicks an explicit refresh at 07:00 so the widget never goes
  stale across a day boundary.
- Calculations use Lahiri ayanamsa and a top-terms Brown's-lunar-theory
  longitude (~0.1° accuracy, plenty for sign determination).
- Manifest declares `RECEIVE_BOOT_COMPLETED` so the daily worker survives
  a reboot. No internet permission is actually used for calculations —
  it's only listed because the WebView shell expects it.
