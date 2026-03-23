package com.terminallauncher.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first

class WallpaperUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = PreferencesStore(applicationContext)
        val birthDate = prefs.birthDate.first() ?: return Result.failure()

        val tracker = UsageTracker(applicationContext)
        val cal = java.util.Calendar.getInstance()
        val lived = ((cal.get(java.util.Calendar.YEAR) - birthDate.year) * 12 +
            (cal.get(java.util.Calendar.MONTH) + 1 - birthDate.month)).coerceIn(0, 960)
        val screenTimeMonths = tracker.screenTimeMonths(960 - lived)

        val generator = WallpaperGenerator(applicationContext)
        generator.generateAndSetWallpaper(birthDate.year, birthDate.month, screenTimeMonths)

        return Result.success()
    }
}
