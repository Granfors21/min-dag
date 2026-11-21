package se.robgra.mindag

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Skyddsnät: var tredje timme planeras alla larm om. Om ett larm har tappats (t.ex. av en
 * aggressiv batterisparare) kommer kedjan tillbaka av sig själv. Hjälper inte mot "Tvinga stopp".
 */
class SafetyWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        Store(applicationContext).lastSafety = System.currentTimeMillis()
        Alarms.scheduleNext(applicationContext)
        val st = Store(applicationContext)
        st.timer?.let { if (it.endsAt > System.currentTimeMillis()) Alarms.scheduleTimer(applicationContext, it) }
        NightService.sync(applicationContext)
        return Result.success()
    }

    companion object {
        fun ensure(ctx: Context) {
            try {
                WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                    "mindag-skyddsnat", ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<SafetyWorker>(3, TimeUnit.HOURS).build(),
                )
            } catch (_: Exception) { }
        }
    }
}
