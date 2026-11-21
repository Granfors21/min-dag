package se.robgra.mindag

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

enum class Route { CHILD, CALM, TIMER_SETUP, PIN, PIN_SETUP, PARENT, ROUTINE_EDIT, WELCOME, CHECKS, RECOVERY_SHOW, RECOVERY_ENTER }

class AppState(val ctx: Context, val clock: () -> Long, private val sideEffects: Boolean = true) {
    val store = Store(ctx)
    var settings by mutableStateOf(store.settings)
    var progress by mutableStateOf(store.progress)
    var timer by mutableStateOf(store.timer)
    var timerDone by mutableStateOf(store.timerDone)
    var now by mutableLongStateOf(clock())
    var route by mutableStateOf(if (settings.pin.isEmpty()) Route.WELCOME else Route.CHILD)
    var editing by mutableStateOf<String?>(null)
    var pinTarget by mutableStateOf(Route.PARENT)
    /** Nyss skapad återställningskod som visas en gång. */
    var shownCode by mutableStateOf<String?>(null)
    var afterCode by mutableStateOf(Route.PARENT)
    var recovering by mutableStateOf(false)

    fun newRecoveryCode(next: Route) {
        val c = Recovery.newCode()
        updateSettings { it.copy(recoveryHash = Recovery.hash(c)) }
        shownCode = c; afterCode = next; route = Route.RECOVERY_SHOW
    }

    fun reload() {
        settings = store.settings; progress = store.progress; timer = store.timer; timerDone = store.timerDone
        now = clock()
        if (timer != null && now >= timer!!.endsAt) { store.timerDone = timer; store.timer = null; timer = null; timerDone = store.timerDone }
    }

    fun tick() { now = clock(); val t = timer; if (t != null && now >= t.endsAt) reload() }

    fun updateSettings(f: (Settings) -> Settings) {
        settings = f(settings); store.settings = settings
        Photos.cleanup(ctx, settings)
        if (sideEffects) Alarms.syncAll(ctx)
    }

    fun screen() = Planner.screen(settings, progress, timer, timerDone, now)

    private var lastCheck = 0L
    var debounceMs = 1_000L
    fun checkOff(r: Routine, p: Progress) {
        val n = clock()
        // Skydd mot dubbeltryck: ett tryck räknas bara en gång per sekund.
        val wall = System.nanoTime() / 1_000_000
        if (wall - lastCheck < debounceMs) return
        lastCheck = wall
        val next = p.index + 1
        progress = if (next >= r.steps.size) p.copy(index = r.steps.size, finishedAt = n) else p.copy(index = next, stepStartedAt = n)
        store.progress = progress; now = n
    }

    fun undo(r: Routine, p: Progress) {
        if (p.index <= 0) return
        progress = p.copy(index = p.index - 1, stepStartedAt = clock(), finishedAt = 0)
        store.progress = progress
    }

    fun startEarly(r: Routine) {
        progress = Progress(r.id, Planner.dateKey(clock()), 0, clock(), startedEarly = true)
        store.progress = progress
    }

    fun dismissDone() {
        // Flytta fram tiden så att klar-skärmen försvinner direkt.
        progress = progress?.copy(finishedAt = clock() - Planner.DONE_SHOW_MIN * 60_000L - 1)
        store.progress = progress
    }

    fun startTimer(a: Activity, minutes: Int) {
        val n = clock()
        val t = TimerState(a.name, a.icon, n, n + minutes * 60_000L)
        timer = t; timerDone = null; store.timer = t; store.timerDone = null
        if (sideEffects) Alarms.scheduleTimer(ctx, t)
        route = Route.CHILD
    }

    fun stopTimer() {
        timer = null; timerDone = null; store.timer = null; store.timerDone = null
        if (sideEffects) { Alarms.scheduleTimer(ctx, null); Notify.cancel(ctx, Notify.ID_TIMER) }
    }

    fun dismissTimerDone() { timerDone = null; store.timerDone = null }
}

@Composable
fun MinDagApp(state: AppState, ticking: Boolean = true, animate: Boolean = true) {
    if (ticking) LaunchedEffect(Unit) { while (true) { state.tick(); delay(1_000) } }
    MinDagTheme {
        when (state.route) {
            Route.WELCOME -> WelcomeScreen(state)
            Route.PIN_SETUP -> PinSetupScreen(state)
            Route.PIN -> PinEntryScreen(state)
            Route.PARENT -> ParentScreen(state)
            Route.ROUTINE_EDIT -> RoutineEditScreen(state)
            Route.CHECKS -> ChecksScreen(state, firstRun = !state.settings.onboarded)
            Route.CALM -> CalmScreen(state, animate)
            Route.TIMER_SETUP -> TimerSetupScreen(state)
            Route.CHILD -> ChildScreen(state)
            Route.RECOVERY_SHOW -> RecoveryShowScreen(state)
            Route.RECOVERY_ENTER -> RecoveryEnterScreen(state)
        }
    }
}
