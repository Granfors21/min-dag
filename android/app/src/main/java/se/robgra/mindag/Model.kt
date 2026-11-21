package se.robgra.mindag

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

enum class Kind { MORNING, EVENING }
enum class DayType { SCHOOL, FREE }

/** image = filnamn på ett eget foto i appens lagring (null = visa emoji). */
data class Step(val text: String, val icon: String, val minutes: Int = 0, val image: String? = null) {
    fun toJson() = JSONObject().put("t", text).put("i", icon).put("m", minutes).apply { if (image != null) put("img", image) }
    companion object {
        fun from(o: JSONObject) = Step(o.optString("t"), o.optString("i", "⭐"), o.optInt("m", 0), o.optString("img", "").ifEmpty { null })
    }
}

data class Routine(
    val kind: Kind,
    val dayType: DayType,
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val steps: List<Step>,
) {
    val id get() = "${kind.name}_${dayType.name}"
    val title get() = if (kind == Kind.MORNING) "Morgon" else "Kväll"
    val symbol get() = if (kind == Kind.MORNING) "☀️" else "🌆"
    val totalMinutes get() = steps.sumOf { it.minutes }
    fun toJson() = JSONObject().put("k", kind.name).put("d", dayType.name).put("e", enabled)
        .put("h", hour).put("min", minute)
        .put("s", JSONArray().apply { steps.forEach { put(it.toJson()) } })
    companion object {
        fun from(o: JSONObject): Routine {
            val arr = o.optJSONArray("s") ?: JSONArray()
            return Routine(
                Kind.valueOf(o.getString("k")), DayType.valueOf(o.getString("d")), o.optBoolean("e", true),
                o.optInt("h"), o.optInt("min"), (0 until arr.length()).map { Step.from(arr.getJSONObject(it)) },
            )
        }
    }
}

/** Natt: läggdags och vakna för nätter som leder fram till en viss dagtyp. */
data class Night(val bedH: Int, val bedM: Int, val wakeH: Int, val wakeM: Int) {
    fun toJson() = JSONObject().put("bh", bedH).put("bm", bedM).put("wh", wakeH).put("wm", wakeM)
    companion object {
        fun from(o: JSONObject?, def: Night) = if (o == null) def else
            Night(o.optInt("bh", def.bedH), o.optInt("bm", def.bedM), o.optInt("wh", def.wakeH), o.optInt("wm", def.wakeM))
    }
}

data class Settings(
    val pin: String = "",
    val sound: Boolean = true,
    val vibration: Boolean = true,
    /** Calendar.DAY_OF_WEEK-värden som räknas som skoldag. */
    val schoolDays: Set<Int> = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
    val routines: List<Routine> = Defaults.routines,
    val nightEnabled: Boolean = true,
    val nightSchool: Night = Night(20, 30, 6, 45),
    val nightFree: Night = Night(21, 30, 8, 0),
    val nightReminderOnUnlock: Boolean = true,
    val onboarded: Boolean = false,
    /** SHA-256 av återställningskoden (aldrig koden i klartext). */
    val recoveryHash: String = "",
    /** setAlarmClock för rutinstart och läggdags: säkrast, men visar en liten larmikon. */
    val strongAlarms: Boolean = true,
    /** Låt Familjeappen (se.robgra.app) styra läggdags och vakna. */
    val followFamily: Boolean = false,
    /** Kvällens natt per veckodag (Calendar.DAY_OF_WEEK för kvällen) från Familjeappen. */
    val familyNights: Map<Int, Night> = emptyMap(),
) {
    /** Natten som börjar på kvällen den här dagen. */
    fun nightForEvening(evening: Calendar): Night {
        if (followFamily) familyNights[evening.get(Calendar.DAY_OF_WEEK)]?.let { return it }
        val tomorrow = (evening.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        return night(dayType(tomorrow))
    }
    fun routine(kind: Kind, type: DayType) = routines.first { it.kind == kind && it.dayType == type }
    fun dayType(cal: Calendar) = if (cal.get(Calendar.DAY_OF_WEEK) in schoolDays) DayType.SCHOOL else DayType.FREE
    fun night(type: DayType) = if (type == DayType.SCHOOL) nightSchool else nightFree

    fun toJson(): String = JSONObject().put("pin", pin).put("sound", sound).put("vib", vibration)
        .put("school", JSONArray().apply { schoolDays.sorted().forEach { put(it) } })
        .put("routines", JSONArray().apply { routines.forEach { put(it.toJson()) } })
        .put("nightOn", nightEnabled).put("nightS", nightSchool.toJson()).put("nightF", nightFree.toJson())
        .put("nightUnlock", nightReminderOnUnlock).put("onb", onboarded)
        .put("rec", recoveryHash).put("strong", strongAlarms).put("follow", followFamily)
        .put("famN", JSONObject().apply { familyNights.forEach { (d, n) -> put(d.toString(), n.toJson()) } })
        .toString()

    companion object {
        fun from(s: String?): Settings {
            if (s.isNullOrBlank()) return Settings()
            return try {
                val o = JSONObject(s)
                val d = Settings()
                val sch = o.optJSONArray("school")
                val ra = o.optJSONArray("routines")
                val parsed = if (ra == null) d.routines else (0 until ra.length()).map { Routine.from(ra.getJSONObject(it)) }
                // Se till att alla fyra rutiner finns
                val full = d.routines.map { def -> parsed.firstOrNull { it.id == def.id } ?: def }
                Settings(
                    pin = o.optString("pin", ""),
                    sound = o.optBoolean("sound", true),
                    vibration = o.optBoolean("vib", true),
                    schoolDays = if (sch == null) d.schoolDays else (0 until sch.length()).map { sch.getInt(it) }.toSet(),
                    routines = full,
                    nightEnabled = o.optBoolean("nightOn", true),
                    nightSchool = Night.from(o.optJSONObject("nightS"), d.nightSchool),
                    nightFree = Night.from(o.optJSONObject("nightF"), d.nightFree),
                    nightReminderOnUnlock = o.optBoolean("nightUnlock", true),
                    onboarded = o.optBoolean("onb", false),
                    recoveryHash = o.optString("rec", ""),
                    strongAlarms = o.optBoolean("strong", true),
                    followFamily = o.optBoolean("follow", false),
                    familyNights = o.optJSONObject("famN")?.let { fo ->
                        fo.keys().asSequence().mapNotNull { k -> k.toIntOrNull()?.let { d -> d to Night.from(fo.optJSONObject(k), Night(20, 0, 7, 0)) } }.toMap()
                    } ?: emptyMap(),
                )
            } catch (e: Exception) { Settings() }
        }
    }
}

object Defaults {
    private val morningSchool = listOf(
        Step("Gå upp ur sängen", "🛏️", 0),
        Step("Gå på toaletten", "🚽", 0),
        Step("Klä på dig", "👕", 10),
        Step("Äta frukost", "🥣", 15),
        Step("Borsta tänderna", "🦷", 3),
        Step("Kamma håret", "💇", 0),
        Step("Packa väskan", "🎒", 5),
        Step("Ytterkläder och skor på", "🧥", 5),
    )
    private val morningFree = listOf(
        Step("Gå på toaletten", "🚽", 0),
        Step("Klä på dig", "👕", 10),
        Step("Äta frukost", "🥣", 20),
        Step("Borsta tänderna", "🦷", 3),
    )
    private val evening = listOf(
        Step("Städa undan det du lekt med", "🧸", 5),
        Step("Duscha eller tvätta dig", "🚿", 10),
        Step("Pyjamas på", "👚", 5),
        Step("Borsta tänderna", "🦷", 3),
        Step("Gå på toaletten", "🚽", 0),
        Step("Lägg fram kläder till i morgon", "👖", 5),
        Step("Gå och lägg dig", "🛏️", 0),
    )
    val routines = listOf(
        Routine(Kind.MORNING, DayType.SCHOOL, true, 6, 45, morningSchool),
        Routine(Kind.MORNING, DayType.FREE, true, 8, 0, morningFree),
        Routine(Kind.EVENING, DayType.SCHOOL, true, 19, 30, evening),
        Routine(Kind.EVENING, DayType.FREE, true, 20, 0, evening),
    )
    val icons = listOf(
        "🛏️", "🚽", "👕", "👖", "🧦", "🥣", "🍎", "🥪", "🥛", "🦷", "💇", "🧼", "🚿", "🛁", "💊",
        "🎒", "🧥", "👟", "🧢", "📚", "✏️", "🧸", "🎮", "📺", "📖", "🖍️", "🎵", "🐶", "🌙", "☀️", "⭐",
        "🚌", "🚲", "🏫", "🍽️", "💧", "🧹", "🌱", "❤️",
    )
}

data class Activity(val name: String, val icon: String)
val defaultActivities = listOf(
    Activity("Spel", "🎮"), Activity("Rita", "🖍️"), Activity("Vila", "🛋️"),
    Activity("Läsa", "📖"), Activity("TV", "📺"),
)

/** Pågående framsteg i en rutin, för ett visst datum. */
data class Progress(val routineId: String, val dateKey: String, val index: Int, val stepStartedAt: Long, val startedEarly: Boolean = false, val finishedAt: Long = 0) {
    fun toJson(): String = JSONObject().put("r", routineId).put("d", dateKey).put("i", index).put("t", stepStartedAt)
        .put("e", startedEarly).put("f", finishedAt).toString()
    companion object {
        fun from(s: String?): Progress? = if (s.isNullOrBlank()) null else try {
            val o = JSONObject(s)
            Progress(o.getString("r"), o.getString("d"), o.getInt("i"), o.getLong("t"), o.optBoolean("e"), o.optLong("f"))
        } catch (e: Exception) { null }
    }
}

data class TimerState(val name: String, val icon: String, val startedAt: Long, val endsAt: Long) {
    fun toJson(): String = JSONObject().put("n", name).put("i", icon).put("s", startedAt).put("e", endsAt).toString()
    companion object {
        fun from(s: String?): TimerState? = if (s.isNullOrBlank()) null else try {
            val o = JSONObject(s)
            TimerState(o.getString("n"), o.getString("i"), o.getLong("s"), o.getLong("e"))
        } catch (e: Exception) { null }
    }
}
