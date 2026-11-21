package se.robgra.mindag

import android.content.Context

class Store(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("mindag", Context.MODE_PRIVATE)
    var settings: Settings
        get() = Settings.from(p.getString("settings", null))
        set(v) { p.edit().putString("settings", v.toJson()).apply() }
    var progress: Progress?
        get() = Progress.from(p.getString("progress", null))
        set(v) { p.edit().putString("progress", v?.toJson()).apply() }
    var timer: TimerState?
        get() = TimerState.from(p.getString("timer", null))
        set(v) { p.edit().putString("timer", v?.toJson()).apply() }
    var timerDone: TimerState?
        get() = TimerState.from(p.getString("timerDone", null))
        set(v) { p.edit().putString("timerDone", v?.toJson()).apply() }
    var customActivity: String
        get() = p.getString("customAct", "") ?: ""
        set(v) { p.edit().putString("customAct", v).apply() }
}
