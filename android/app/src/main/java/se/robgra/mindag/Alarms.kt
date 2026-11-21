package se.robgra.mindag

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager

object Alarms {
    const val ACTION_EVENT = "se.robgra.mindag.EVENT"
    const val ACTION_TIMER_SOON = "se.robgra.mindag.TIMER_SOON"
    const val ACTION_TIMER_END = "se.robgra.mindag.TIMER_END"
    private const val RC_EVENT = 1
    private const val RC_TIMER_SOON = 2
    private const val RC_TIMER_END = 3

    fun exactAllowed(ctx: Context): Boolean {
        val am = ctx.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
    }

    fun batteryUnrestricted(ctx: Context): Boolean =
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

    private fun pi(ctx: Context, action: String, rc: Int, extra: Long = 0L): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, rc, Intent(ctx, AlarmReceiver::class.java).setAction(action).putExtra("at", extra),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun set(ctx: Context, at: Long, p: PendingIntent, strong: Boolean = false) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        try {
            // setAlarmClock är det Android prioriterar högst (går igenom Doze och de flesta batterispärrar).
            if (strong) { am.setAlarmClock(AlarmManager.AlarmClockInfo(at, Notify.openApp(ctx, 7)), p); return }
            if (exactAllowed(ctx)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
        }
    }

    /** Planerar nästa rutin-/nattpåminnelse. Kedjan fortsätter i AlarmReceiver. */
    fun scheduleNext(ctx: Context, now: Long = System.currentTimeMillis()) {
        val store = Store(ctx)
        val s = store.settings
        val next = Planner.nextEvents(s, now + 5_000).firstOrNull()
        val am = ctx.getSystemService(AlarmManager::class.java)
        if (next == null) { am.cancel(pi(ctx, ACTION_EVENT, RC_EVENT)); store.nextAt = 0; store.nextWhat = ""; return }
        val strong = s.strongAlarms && next.type in setOf(EventType.ROUTINE_START, EventType.NIGHT_START)
        set(ctx, next.at, pi(ctx, ACTION_EVENT, RC_EVENT, next.at), strong)
        store.nextAt = next.at
        store.nextWhat = describe(s, next)
    }

    fun scheduleTimer(ctx: Context, t: TimerState?) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(pi(ctx, ACTION_TIMER_SOON, RC_TIMER_SOON))
        am.cancel(pi(ctx, ACTION_TIMER_END, RC_TIMER_END))
        if (t == null) return
        val soon = t.endsAt - 5 * 60_000L
        if (soon > System.currentTimeMillis() + 30_000) set(ctx, soon, pi(ctx, ACTION_TIMER_SOON, RC_TIMER_SOON, soon))
        set(ctx, t.endsAt, pi(ctx, ACTION_TIMER_END, RC_TIMER_END, t.endsAt))
    }

    fun describe(s: Settings, e: Event): String {
        val r = s.routines.firstOrNull { it.id == e.routineId }
        return when (e.type) {
            EventType.ROUTINE_SOON -> "Förvarning: ${r?.title?.lowercase() ?: "rutin"}"
            EventType.ROUTINE_START -> "${r?.title ?: "Rutin"} börjar"
            EventType.BED_SOON -> "Snart läggdags"
            EventType.NIGHT_START -> "Läggdags"
            EventType.NIGHT_END -> "Natten slutar"
        }
    }

    /** Allt som ska vara igång efter omstart, uppdatering eller ändrade inställningar. */
    fun syncAll(ctx: Context) {
        scheduleNext(ctx)
        val st = Store(ctx)
        val t = st.timer
        if (t != null && t.endsAt > System.currentTimeMillis()) scheduleTimer(ctx, t) else scheduleTimer(ctx, null)
        NightService.sync(ctx)
        SafetyWorker.ensure(ctx)
    }
}
