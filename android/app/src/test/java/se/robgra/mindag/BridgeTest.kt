package se.robgra.mindag

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BridgeTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    @Before fun tz() { TimeZone.setDefault(TimeZone.getTimeZone("Europe/Stockholm")) }

    @Test fun bedtimesIgnoredUnlessParentAllows() {
        Store(ctx).settings = Settings(pin = "2580", onboarded = true)
        assertFalse(FamilyBridge.applyBedtimes(ctx, """{"2":{"bh":20,"bm":0,"wh":7,"wm":0}}"""))
        assertTrue(Store(ctx).settings.familyNights.isEmpty())
    }

    @Test fun bedtimesValidated() {
        Store(ctx).settings = Settings(pin = "2580", onboarded = true, followFamily = true)
        assertTrue(FamilyBridge.applyBedtimes(ctx, """{"2":{"bh":20,"bm":15,"wh":6,"wm":50},"3":{"bh":2,"bm":0,"wh":7,"wm":0},"9":{"bh":20,"bm":0,"wh":7,"wm":0},"x":{}}"""))
        val n = Store(ctx).settings.familyNights
        assertEquals(setOf(Calendar.MONDAY), n.keys)
        assertEquals(Night(20, 15, 6, 50), n[Calendar.MONDAY])
    }

    @Test fun statusJson() {
        Store(ctx).settings = Settings(pin = "2580", onboarded = true)
        val c = Calendar.getInstance().apply { clear(); set(2026, 10, 23, 6, 50) }
        val o = FamilyBridge.statusJson(ctx, c.timeInMillis)
        assertEquals("routine", o.getString("state"))
        assertEquals("MORNING_SCHOOL", o.getString("routine"))
        assertEquals(1, o.getInt("step"))
    }

    @Test fun strangersRejected() {
        val b = FamilyBridge()
        b.attachInfo(ctx, android.content.pm.ProviderInfo().apply { authority = FamilyBridge.AUTHORITY })
        try { b.call("status", null, null); fail("borde nekas") } catch (e: SecurityException) { }
    }

    @Test fun familyNotInstalled() { assertFalse(FamilyBridge.familyInstalled(ctx)) }

    @Test fun oemGuides() {
        assertTrue(Oem.guide(ctx, "xiaomi")!!.intents.isNotEmpty())
        assertTrue(Oem.guide(ctx, "huawei")!!.steps.contains("Autostart"))
        assertTrue(Oem.guide(ctx, "samsung")!!.steps.contains("aldrig sover"))
        assertNull(Oem.guide(ctx, "google"))
    }

    @Test fun safetyWorkerReArms() {
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(ctx)
        Store(ctx).settings = Settings(pin = "2580", onboarded = true)
        val w = androidx.work.testing.TestWorkerBuilder.from(ctx, SafetyWorker::class.java, java.util.concurrent.Executors.newSingleThreadExecutor()).build()
        assertTrue(w.doWork() is androidx.work.ListenableWorker.Result.Success)
        assertTrue(Store(ctx).nextAt > System.currentTimeMillis())
        assertTrue(Store(ctx).lastSafety > 0)
    }
}
