package se.robgra.mindag

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PlannerTest {
    @Before fun tz() { TimeZone.setDefault(TimeZone.getTimeZone("Europe/Stockholm")) }

    private fun t(y: Int, mo: Int, d: Int, h: Int, mi: Int) = Calendar.getInstance().apply { clear(); set(y, mo - 1, d, h, mi, 0) }.timeInMillis
    private val s = Settings(pin = "2580", onboarded = true)

    @Test fun nightBeforeSchoolDay() {
        // Söndag 22 nov 2026 21:00 -> natt före måndag (skoldag): läggdags 20:30, vakna 06:45
        val n = Planner.night(s, t(2026, 11, 22, 21, 0))!!
        assertEquals(t(2026, 11, 23, 6, 45), n.wakeAt)
        assertNull(Planner.night(s, t(2026, 11, 22, 20, 29)))
    }

    @Test fun nightBeforeFreeDay() {
        // Fredag 20:45 är inte natt (ledig dag i morgon, läggdags 21:30)
        assertNull(Planner.night(s, t(2026, 11, 27, 20, 45)))
        val n = Planner.night(s, t(2026, 11, 27, 22, 0))!!
        assertEquals(t(2026, 11, 28, 8, 0), n.wakeAt)
        // Lördag 07:30 är fortfarande natt
        assertNotNull(Planner.night(s, t(2026, 11, 28, 7, 30)))
        assertNull(Planner.night(s, t(2026, 11, 28, 8, 0)))
    }

    @Test fun morningRoutineActiveOnSchoolDay() {
        val sc = Planner.screen(s, null, null, null, t(2026, 11, 23, 6, 50))
        assertTrue(sc is Screen.InRoutine)
        assertEquals("MORNING_SCHOOL", (sc as Screen.InRoutine).routine.id)
        // Helg: morgonrutinen för ledig dag startar 08:00
        assertTrue(Planner.screen(s, null, null, null, t(2026, 11, 28, 8, 5)).let { it is Screen.InRoutine && it.routine.id == "MORNING_FREE" })
    }

    @Test fun finishedRoutineShowsDoneThenFree() {
        val now = t(2026, 11, 23, 7, 30)
        val p = Progress("MORNING_SCHOOL", "2026-11-23", 8, now, finishedAt = now)
        assertTrue(Planner.screen(s, p, null, null, now + 60_000) is Screen.RoutineDone)
        assertTrue(Planner.screen(s, p, null, null, now + 30 * 60_000) is Screen.Free)
    }

    @Test fun eveningRoutineBeatsNightUntilDone() {
        val sLate = s.copy(nightSchool = Night(19, 45, 6, 45))
        assertTrue(Planner.screen(sLate, null, null, null, t(2026, 11, 23, 19, 50)) is Screen.InRoutine)
        val p = Progress("EVENING_SCHOOL", "2026-11-23", 7, 0, finishedAt = t(2026, 11, 23, 19, 40))
        assertTrue(Planner.screen(sLate, p, null, null, t(2026, 11, 23, 20, 10)) is Screen.NightTime)
    }

    @Test fun timerScreens() {
        val now = t(2026, 11, 23, 16, 0)
        val tm = TimerState("Spel", "🎮", now, now + 20 * 60_000)
        assertTrue(Planner.screen(s, null, tm, null, now + 60_000) is Screen.TimerRunning)
        assertTrue(Planner.screen(s, null, null, tm, now + 21 * 60_000) is Screen.TimerFinished)
    }

    @Test fun eventsIncludeWarningsAndNight() {
        val from = t(2026, 11, 23, 12, 0)
        val next = Planner.nextEvents(s, from)
        assertEquals(EventType.ROUTINE_SOON, next.first().type)
        assertEquals(t(2026, 11, 23, 19, 25), next.first().at)
        val ev = Planner.events(s, from).filter { it.at > from }
        assertTrue(ev.any { it.type == EventType.BED_SOON && it.at == t(2026, 11, 23, 20, 15) })
        assertTrue(ev.any { it.type == EventType.NIGHT_START && it.at == t(2026, 11, 23, 20, 30) })
        assertTrue(ev.any { it.type == EventType.NIGHT_END && it.at == t(2026, 11, 24, 6, 45) })
    }

    @Test fun disabledRoutineSkipped() {
        val s2 = s.copy(routines = s.routines.map { if (it.id == "EVENING_SCHOOL") it.copy(enabled = false) else it })
        val n = Planner.nextRoutine(s2, t(2026, 11, 23, 12, 0))!!
        assertEquals("MORNING_SCHOOL", n.first.id)
    }

    @Test fun settingsRoundTrip() {
        val s2 = s.copy(sound = false, schoolDays = setOf(Calendar.MONDAY), nightFree = Night(22, 0, 9, 0))
        assertEquals(s2, Settings.from(s2.toJson()))
        assertEquals(Settings(), Settings.from("trasig json"))
    }

    @Test fun breathTiming() {
        assertTrue(breath(0).second); assertTrue(breath(3_999).second); assertFalse(breath(4_000).second)
        assertEquals(1.0f, breath(4_000).first, 0.01f); assertEquals(0.55f, breath(9_999).first, 0.01f)
    }
}
