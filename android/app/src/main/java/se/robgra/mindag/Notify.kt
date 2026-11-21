package se.robgra.mindag

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notify {
    const val ID_ROUTINE = 10
    const val ID_TIMER = 11
    const val ID_NIGHT = 12
    const val ID_NIGHT_SERVICE = 13
    const val CH_SERVICE = "nattlage_tyst"

    private fun soundUri(ctx: Context): Uri =
        Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${ctx.packageName}/${R.raw.mjuk_ton}")

    /** Kanaler kan inte ändra ljud i efterhand, så det finns en kanal per kombination av ljud/vibration. */
    fun channel(ctx: Context, s: Settings, high: Boolean = false): String {
        val id = "paminnelse_${if (s.sound) "ljud" else "tyst"}_${if (s.vibration) "vib" else "novib"}${if (high) "_natt" else ""}"
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(id) == null) {
            val name = buildString {
                append(if (high) "Nattpåminnelse" else "Mjuka påminnelser")
                append(if (s.sound) " (ljud" else " (tyst"); append(if (s.vibration) ", vibration)" else ")")
            }
            val ch = NotificationChannel(id, name, if (high) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT)
            if (s.sound) ch.setSound(soundUri(ctx), AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            else ch.setSound(null, null)
            ch.enableVibration(s.vibration)
            if (s.vibration) ch.vibrationPattern = longArrayOf(0, 180, 220, 180)
            ch.enableLights(false)
            nm.createNotificationChannel(ch)
            // Ta bort gamla kanaler med andra ljudval så listan i systeminställningar blir begriplig.
            nm.notificationChannels.filter { it.id.startsWith("paminnelse_") && it.id.endsWith("_natt") == high && it.id != id }
                .forEach { nm.deleteNotificationChannel(it.id) }
        }
        return id
    }

    fun serviceChannel(ctx: Context): String {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CH_SERVICE) == null) {
            nm.createNotificationChannel(NotificationChannel(CH_SERVICE, "Nattläge är på (tyst)", NotificationManager.IMPORTANCE_MIN).apply {
                setSound(null, null); enableVibration(false); setShowBadge(false)
            })
        }
        return CH_SERVICE
    }

    fun openApp(ctx: Context, req: Int = 0): PendingIntent = PendingIntent.getActivity(
        ctx, req, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun canPost(ctx: Context) = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    fun show(ctx: Context, id: Int, title: String, text: String) {
        if (!canPost(ctx)) return
        val s = Store(ctx).settings
        val n = NotificationCompat.Builder(ctx, channel(ctx, s))
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(0xFF8FB8A8.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(ctx, id))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (_: SecurityException) {}
    }

    fun night(ctx: Context, wakeText: String) {
        if (!canPost(ctx)) return
        val s = Store(ctx).settings
        val n = NotificationCompat.Builder(ctx, channel(ctx, s, high = true))
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(0xFF2E3A59.toInt())
            .setContentTitle("🌙 Nu är det natt")
            .setContentText("Dags att sova. Du kan vakna $wakeText.")
            .setContentIntent(openApp(ctx, ID_NIGHT))
            .setFullScreenIntent(openApp(ctx, ID_NIGHT + 100), true)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(ID_NIGHT, n) } catch (_: SecurityException) {}
    }

    fun cancel(ctx: Context, id: Int) = NotificationManagerCompat.from(ctx).cancel(id)

    fun fullScreenAllowed(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 34) ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent() else true
}
