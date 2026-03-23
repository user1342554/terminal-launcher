package com.terminallauncher.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings

class UsageTracker(private val context: Context) {

    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openPermissionSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Returns map of packageName -> total foreground time in milliseconds over the last 7 days.
     */
    fun getScreenTime(): Map<String, Long> {
        if (!hasPermission()) return emptyMap()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val weekAgo = now - 7 * 24 * 60 * 60 * 1000L

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            weekAgo,
            now
        ) ?: return emptyMap()

        val result = mutableMapOf<String, Long>()
        for (stat in stats) {
            val time = stat.totalTimeInForeground
            if (time > 0) {
                result[stat.packageName] = (result[stat.packageName] ?: 0L) + time
            }
        }
        return result
    }

    /**
     * Returns average daily screen time in hours over the last 7 days.
     */
    fun getDailyScreenTimeHours(): Float {
        val screenTime = getScreenTime()
        if (screenTime.isEmpty()) return 0f
        val totalMs = screenTime.values.sum()
        val totalHours = totalMs / (1000f * 60f * 60f)
        return totalHours / 7f // average per day over 7 days
    }

    /**
     * Given remaining months of life, calculates how many months
     * will be spent on the phone at current usage rate.
     */
    fun screenTimeMonths(remainingMonths: Int): Int {
        val dailyHours = getDailyScreenTimeHours()
        if (dailyHours <= 0f) return 0
        // fraction of each day on phone
        val fraction = dailyHours / 24f
        return (remainingMonths * fraction).toInt()
    }
}
