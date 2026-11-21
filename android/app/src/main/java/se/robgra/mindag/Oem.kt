package se.robgra.mindag

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings

/** Tillverkarnas egna batterispärrar och hur man tar sig förbi dem. */
object Oem {
    data class Guide(val brand: String, val steps: String, val intents: List<Intent>)

    private fun cmp(pkg: String, cls: String) = Intent().setComponent(ComponentName(pkg, cls))

    fun brand(): String = Build.MANUFACTURER.lowercase()

    fun guide(ctx: Context, brand: String = brand()): Guide? = when {
        "xiaomi" in brand || "redmi" in brand || "poco" in brand -> Guide(
            "Xiaomi / Redmi / POCO",
            "1. Tryck \"Öppna Autostart\" och slå på Min dag.\n2. Inställningar → Appar → Min dag → Batterisparläge → \"Inga begränsningar\".\n3. Lås appen i Senaste appar: dra ner på kortet så att ett hänglås syns.",
            listOf(
                cmp("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                cmp("com.miui.securitycenter", "com.miui.powercenter.PowerSettings"),
            ),
        )
        "huawei" in brand || "honor" in brand -> Guide(
            "Huawei / Honor",
            "1. Tryck \"Öppna Appstart\".\n2. Stäng av \"Hantera automatiskt\" för Min dag och slå på alla tre: Autostart, Sekundär start och Kör i bakgrunden.",
            listOf(
                cmp("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                cmp("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
                cmp("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            ),
        )
        "samsung" in brand -> Guide(
            "Samsung",
            "1. Tryck \"Öppna Batteri\".\n2. Bakgrundsanvändning → \"Appar som aldrig sover\" → lägg till Min dag.\n3. Kontrollera att Min dag INTE står under \"Sovande appar\" eller \"Appar i djup sömn\".",
            listOf(
                cmp("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
                cmp("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
            ),
        )
        "oppo" in brand || "realme" in brand || "oneplus" in brand -> Guide(
            "OPPO / realme / OnePlus",
            "1. Tryck \"Öppna inställningar\".\n2. Batteri → \"Tillåt bakgrundsaktivitet\" och \"Tillåt autostart\" för Min dag.",
            listOf(
                cmp("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                cmp("com.oplus.battery", "com.oplus.powermanager.fuelgaue.PowerUsageModelActivity"),
                cmp("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
            ),
        )
        "vivo" in brand -> Guide(
            "vivo",
            "1. Tryck \"Öppna inställningar\".\n2. Batteri → Hög bakgrundsförbrukning → tillåt Min dag. Slå även på Autostart.",
            listOf(cmp("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
        )
        else -> null
    }

    fun appDetails(ctx: Context) = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))

    /** Försök öppna tillverkarens sida; annars appens egen informationssida. */
    fun open(ctx: Context, g: Guide?): Boolean {
        val list = (g?.intents ?: emptyList()) + appDetails(ctx)
        for (i in list) {
            try { ctx.startActivity(Intent(i).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return true } catch (_: Exception) { }
        }
        return false
    }
}
