package se.robgra.mindag

import java.util.Calendar

enum class EventType { ROUTINE_SOON, ROUTINE_START, BED_SOON, NIGHT_START, NIGHT_END }
data class Event(val at: Long, val type: EventType, val routineId: String = "")

data class NightInfo(val wakeAt: Long, val bedAt: Long)

sealed class Screen {
    data class InRoutine(val routine: Routine, val progress: Progress) : Screen()
    data class RoutineDone(val routine: Routine) : Screen()
    data class NightTime(val info: NightInfo) : Screen()
    data class TimerRunning(val timer: TimerState) : Screen()
    data class TimerFinished(val timer: TimerState) : Screen()
    data class Free(val next: Pair<Routine, Long>?) : Screen()
}

object Planner {
    const val SOON_MIN = 5
    const val BED_SOON_MIN = 15
    const val DONE_SHOW_MIN = 20

    fun cal(t: Long): Calendar = Calendar.getInstance().apply { timeInMillis = t }
    fun at(day: Calendar, h: Int, m: Int): Long = (day.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    fun plusDays(c: Calendar, n: Int): Calendar = (c.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, n) }
    fun dateKey(t: Long): String { val c = cal(t); return "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)) }

    fun startOf(r: Routine, day: Calendar) = at(day, r.hour, r.minute)
    /** Hur länge en rutin räknas som pågående efter start om den inte bockats av. */
    fun windowMin(r: Routine) = maxOf(r.totalMinutes + 60, 120)

    fun todaysRoutines(s: Settings, now: Long): List<Routine> {
        val type = s.dayType(cal(now))
        return s.routines.filter { it.enabled && it.dayType == type && it.steps.isNotEmpty() }
    }

    fun night(s: Settings, now: Long): NightInfo? {
        if (!s.nightEnabled) return null
        val today = cal(now)
        val tomorrow = plusDays(today, 1)
        val tn = s.nightForEvening(today)
        val bedTonight = at(today, tn.bedH, tn.bedM)
        if (now >= bedTonight) return NightInfo(at(tomorrow, tn.wakeH, tn.wakeM), bedTonight)
        val yesterday = plusDays(today, -1)
        val n = s.nightForEvening(yesterday)
        val wakeToday = at(today, n.wakeH, n.wakeM)
        if (now < wakeToday) return NightInfo(wakeToday, at(yesterday, n.bedH, n.bedM))
        return null
    }

    fun nextRoutine(s: Settings, now: Long): Pair<Routine, Long>? {
        val today = cal(now)
        for (d in 0..7) {
            val day = plusDays(today, d)
            val type = s.dayType(day)
            s.routines.filter { it.enabled && it.dayType == type && it.steps.isNotEmpty() }
                .map { it to startOf(it, day) }.filter { it.second > now }.minByOrNull { it.second }
                ?.let { return it }
        }
        return null
    }

    fun screen(s: Settings, progress: Progress?, timer: TimerState?, timerDone: TimerState?, now: Long): Screen {
        val today = cal(now)
        val key = dateKey(now)
        for (r in todaysRoutines(s, now)) {
            val start = startOf(r, today)
            val p = progress?.takeIf { it.routineId == r.id && it.dateKey == key }
            if (p != null && p.finishedAt > 0) {
                if (now - p.finishedAt < DONE_SHOW_MIN * 60_000L) return Screen.RoutineDone(r)
                continue
            }
            val inWindow = now >= start && now < start + windowMin(r) * 60_000L
            if (p != null && (p.startedEarly || inWindow) && now < start + windowMin(r) * 60_000L) return Screen.InRoutine(r, p)
            if (inWindow) return Screen.InRoutine(r, Progress(r.id, key, 0, maxOf(start, now - 1)))
        }
        night(s, now)?.let { return Screen.NightTime(it) }
        if (timer != null && now < timer.endsAt) return Screen.TimerRunning(timer)
        if (timerDone != null && now - timerDone.endsAt < 10 * 60_000L) return Screen.TimerFinished(timerDone)
        return Screen.Free(nextRoutine(s, now))
    }

    fun events(s: Settings, from: Long, days: Int = 8): List<Event> {
        val out = ArrayList<Event>()
        val c0 = cal(from)
        for (d in -1..days) {
            val day = plusDays(c0, d)
            val type = s.dayType(day)
            for (r in s.routines) if (r.enabled && r.dayType == type && r.steps.isNotEmpty()) {
                val st = startOf(r, day)
                out += Event(st - SOON_MIN * 60_000L, EventType.ROUTINE_SOON, r.id)
                out += Event(st, EventType.ROUTINE_START, r.id)
            }
            if (s.nightEnabled) {
                val tn = s.nightForEvening(day)
                val bed = at(day, tn.bedH, tn.bedM)
                out += Event(bed - BED_SOON_MIN * 60_000L, EventType.BED_SOON)
                out += Event(bed, EventType.NIGHT_START)
                out += Event(at(plusDays(day, 1), tn.wakeH, tn.wakeM), EventType.NIGHT_END)
            }
        }
        return out.sortedBy { it.at }
    }

    fun nextEvents(s: Settings, after: Long): List<Event> {
        val all = events(s, after).filter { it.at > after }
        val first = all.firstOrNull() ?: return emptyList()
        return all.filter { it.at == first.at }
    }

    fun fmt(t: Long): String { val c = cal(t); return "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)) }
    fun fmt(h: Int, m: Int) = "%02d:%02d".format(h, m)
}
