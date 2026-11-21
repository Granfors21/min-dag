package se.robgra.mindag

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Körs bara under natten. Lyssnar på när telefonen låses upp (ACTION_USER_PRESENT kan inte
 * registreras i manifestet sedan Android 8) och visar då en lugn påminnelse. Ingen låsning.
 */
class NightService : Service() {
    private var lastShown = 0L
    private val unlock = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val s = Store(c).settings
            val info = Planner.night(s, System.currentTimeMillis())
            if (info == null) { stopSelf(); return }
            if (!s.nightReminderOnUnlock) return
            val now = System.currentTimeMillis()
            if (now - lastShown < 2 * 60_000L) return
            lastShown = now
            Notify.night(c, Planner.fmt(info.wakeAt))
        }
    }

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(this, unlock, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_EXPORTED) // systemsändning, skyddad av Android
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val s = Store(this).settings
        val info = Planner.night(s, System.currentTimeMillis())
        val n = NotificationCompat.Builder(this, Notify.serviceChannel(this))
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("🌙 Nattläge")
            .setContentText(if (info != null) "Vakna ${Planner.fmt(info.wakeAt)}" else "Nattläge")
            .setOngoing(true).setSilent(true)
            .setContentIntent(Notify.openApp(this, 99))
            .build()
        try {
            ServiceCompat.startForeground(this, Notify.ID_NIGHT_SERVICE, n,
                if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
        } catch (e: Exception) { stopSelf(); return START_NOT_STICKY }
        if (info == null || !s.nightReminderOnUnlock) { stopSelf(); return START_NOT_STICKY }
        return START_STICKY
    }

    override fun onDestroy() {
        try { unregisterReceiver(unlock) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun sync(ctx: Context) {
            val s = Store(ctx).settings
            val night = Planner.night(s, System.currentTimeMillis()) != null
            val i = Intent(ctx, NightService::class.java)
            if (night && s.nightReminderOnUnlock) {
                try { ContextCompat.startForegroundService(ctx, i) } catch (_: Exception) { }
            } else {
                ctx.stopService(i)
            }
        }
    }
}
