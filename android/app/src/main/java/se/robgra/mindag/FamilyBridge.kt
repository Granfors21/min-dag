package se.robgra.mindag

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Valfri lokal koppling till Familjeappen (se.robgra.app) på samma telefon.
 * Bara Familjeappen, signerad med Robins nyckel, får anropa. Inget går över nätet.
 *
 * call("status")   -> rutin, steg, klar, natt, vakna (JSON i "json")
 * call("bedtimes", extras "json" = {"1":{"bh":20,"bm":0,"wh":7,"wm":0}, ...}) nyckel = Calendar.DAY_OF_WEEK för kvällen.
 *     Används bara om föräldern slagit på "Låt Familjeappen styra läggdags" i Min dag.
 */
class FamilyBridge : ContentProvider() {
    companion object {
        const val AUTHORITY = "se.robgra.mindag.family"
        const val FAMILY_PKG = "se.robgra.app"
        /** SHA-256 av Familjeappens signeringscertifikat. */
        const val FAMILY_CERT = "81fc3893cfc17a31449b1503bcf53f4b08a0bfe5c2d32ab2fc7a53c5e30f74dd"

        fun certOf(ctx: Context, pkg: String): Set<String> = try {
            val pm = ctx.packageManager
            val sigs = if (Build.VERSION.SDK_INT >= 28) {
                val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
                if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
            } else @Suppress("DEPRECATION") pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures
            sigs.map { s -> MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) } }.toSet()
        } catch (_: Exception) { emptySet() }

        fun familyInstalled(ctx: Context) = FAMILY_CERT in certOf(ctx, FAMILY_PKG)

        fun statusJson(ctx: Context, now: Long = System.currentTimeMillis()): JSONObject {
            val st = Store(ctx)
            val s = st.settings
            val o = JSONObject().put("app", "Min dag").put("version", 3)
            when (val sc = Planner.screen(s, st.progress, st.timer, st.timerDone, now)) {
                is Screen.InRoutine -> o.put("state", "routine").put("routine", sc.routine.id)
                    .put("step", sc.progress.index + 1).put("steps", sc.routine.steps.size)
                    .put("stepText", sc.routine.steps.getOrNull(sc.progress.index)?.text ?: "")
                is Screen.RoutineDone -> o.put("state", "done").put("routine", sc.routine.id)
                is Screen.NightTime -> o.put("state", "night").put("wakeAt", sc.info.wakeAt)
                is Screen.TimerRunning -> o.put("state", "timer").put("activity", sc.timer.name).put("endsAt", sc.timer.endsAt)
                is Screen.TimerFinished -> o.put("state", "timerDone").put("activity", sc.timer.name)
                is Screen.Free -> o.put("state", "free").apply { sc.next?.let { put("next", it.first.id).put("nextAt", it.second) } }
            }
            val key = Planner.dateKey(now)
            o.put("doneToday", org.json.JSONArray().apply {
                st.progress?.takeIf { it.dateKey == key && it.finishedAt > 0 }?.let { put(it.routineId) }
            })
            o.put("followFamily", s.followFamily)
            return o
        }

        fun applyBedtimes(ctx: Context, json: String): Boolean {
            val st = Store(ctx)
            val s = st.settings
            if (!s.followFamily) return false
            val o = JSONObject(json)
            val map = HashMap<Int, Night>()
            for (k in o.keys()) {
                val d = k.toIntOrNull() ?: continue
                if (d !in 1..7) continue
                val n = Night.from(o.optJSONObject(k), Night(20, 0, 7, 0))
                // Bara rimliga tider: läggdags 17–23, vakna 4–11 (annars ignoreras dagen).
                if (n.bedH in 17..23 && n.wakeH in 4..11 && n.bedM in 0..59 && n.wakeM in 0..59) map[d] = n
            }
            st.settings = s.copy(familyNights = map)
            return true
        }
    }

    private fun callerAllowed(): Boolean {
        val ctx = context ?: return false
        val uid = Binder.getCallingUid()
        val pkgs = ctx.packageManager.getPackagesForUid(uid) ?: return false
        return FAMILY_PKG in pkgs && FAMILY_CERT in certOf(ctx, FAMILY_PKG)
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val ctx = context ?: return null
        if (!callerAllowed()) throw SecurityException("Endast Familjeappen")
        val id = Binder.clearCallingIdentity()
        try {
            return when (method) {
                "status" -> Bundle().apply { putString("json", statusJson(ctx).toString()) }
                "bedtimes" -> {
                    val ok = extras?.getString("json")?.let { runCatching { applyBedtimes(ctx, it) }.getOrDefault(false) } ?: false
                    if (ok) Alarms.syncAll(ctx)
                    Bundle().apply { putBoolean("applied", ok) }
                }
                else -> null
            }
        } finally { Binder.restoreCallingIdentity(id) }
    }

    override fun onCreate() = true
    override fun query(u: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}
