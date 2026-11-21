package se.robgra.mindag

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val store = Store(ctx)
        val now = System.currentTimeMillis()
        when (intent.action) {
            Alarms.ACTION_EVENT -> {
                store.lastFired = now
                val firedFor = intent.getLongExtra("at", now)
                val s = store.settings
                // Hantera alla händelser från den planerade tiden fram till nu (om flera sammanfaller eller larmet blev sent).
                val due = Planner.events(s, firedFor - 60_000).filter { it.at in (firedFor - 1_000)..(now + 10_000) }
                for (e in due) handle(ctx, s, e, now)
                Alarms.scheduleNext(ctx, maxOf(now, firedFor) + 10_000)
                NightService.sync(ctx)
            }
            Alarms.ACTION_TIMER_SOON -> store.timer?.let {
                Notify.show(ctx, Notify.ID_TIMER, "${it.icon} Snart slut", "${it.name}: 5 minuter kvar.")
            }
            Alarms.ACTION_TIMER_END -> store.timer?.let {
                store.timerDone = it
                store.timer = null
                Notify.show(ctx, Notify.ID_TIMER, "${it.icon} Tiden är slut", "${it.name} är klart nu. Dags att sluta.")
            }
        }
    }

    private fun handle(ctx: Context, s: Settings, e: Event, now: Long) {
        // För sena larm (t.ex. telefonen var avstängd) visas inte gamla påminnelser.
        if (now - e.at > 20 * 60_000L) return
        val r = s.routines.firstOrNull { it.id == e.routineId }
        when (e.type) {
            // Ingen förvarning mitt i natten (den skulle bara väcka barnet), bara när det är dags.
            EventType.ROUTINE_SOON -> if (r != null && Planner.night(s, e.at) == null) Notify.show(ctx, Notify.ID_ROUTINE,
                "${r.symbol} Snart ${r.title.lowercase()}", "Om ${Planner.SOON_MIN} minuter börjar ${r.title.lowercase()}srutinen.")
            EventType.ROUTINE_START -> if (r != null) Notify.show(ctx, Notify.ID_ROUTINE,
                "${r.symbol} Nu är det ${r.title.lowercase()}", "Första steget: ${r.steps.first().text}. Tryck för att öppna.")
            EventType.BED_SOON -> Notify.show(ctx, Notify.ID_ROUTINE, "🌙 Snart läggdags", "Om ${Planner.BED_SOON_MIN} minuter är det dags att sova.")
            EventType.NIGHT_START -> Planner.night(s, now + 1_000)?.let { Notify.night(ctx, Planner.fmt(it.wakeAt)) }
            EventType.NIGHT_END -> Notify.cancel(ctx, Notify.ID_NIGHT)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Alarms.syncAll(ctx)
    }
}
