package se.robgra.mindag

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private var now = 0L
    private lateinit var state: AppState

    @Before fun tz() { TimeZone.setDefault(TimeZone.getTimeZone("Europe/Stockholm")) }
    private fun t(d: Int, h: Int, mi: Int, s: Int = 0) = Calendar.getInstance().apply { clear(); set(2026, 10, d, h, mi, s) }.timeInMillis

    private fun store() = Store(ApplicationProvider.getApplicationContext())

    private fun start(at: Long, onboarded: Boolean = true, setup: (Store) -> Unit = {}) {
        now = at
        val st = store()
        st.settings = if (onboarded) Settings(pin = "2580", onboarded = true) else Settings()
        st.progress = null; st.timer = null; st.timerDone = null
        setup(st)
        state = AppState(ApplicationProvider.getApplicationContext(), { now }, sideEffects = false).also { it.debounceMs = 0 }
        rule.setContent { MinDagApp(state, ticking = false, animate = false) }
    }

    private fun shot(name: String) {
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(Canvas(bmp)) }
        val dir = File(System.getProperty("shots.dir") ?: "build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun firstRun() {
        start(t(23, 16, 10), onboarded = false)
        shot("01_valkommen")
        rule.onNodeWithText("Välj PIN-kod").performClick()
        listOf("2", "5", "8", "0").forEach { rule.onNodeWithText(it).performClick() }
        listOf("2", "5", "8").forEach { rule.onNodeWithText(it).performClick() }
        shot("02_pin_upprepa")
        rule.onNodeWithText("0").performClick()
        shot("03_kontroll_behorigheter")
        rule.onNodeWithText("Klart, visa barnets vy").performScrollTo().performClick()
        assertTrue(store().settings.pin == "2580")
    }

    @Test fun morningRoutine() {
        start(t(23, 6, 47))
        shot("10_steg_utan_tid")
        rule.onNodeWithText("Klar ✓").performClick()
        now = t(23, 6, 49); rule.onNodeWithText("Klar ✓").performClick() // toalett klar -> klä på dig 10 min
        now = t(23, 6, 55, 30); state.tick()
        shot("11_steg_med_tartbitsklocka")
        rule.onNodeWithText("↩ Ångra").assertExists()
        repeat(6) { rule.onNodeWithText("Klar ✓").performClick() }
        shot("12_bra_jobbat")
        rule.onNodeWithText("Okej").performClick()
        shot("13_fri_tid_efter_rutin")
    }

    @Test fun freeAndTimer() {
        start(t(23, 18, 52))
        shot("20_fri_tid_snart_kvall")
        rule.onNodeWithText("⏱ Starta tid").performClick()
        rule.onNodeWithText("Spel").performClick()
        rule.onNodeWithText("30 min").performClick()
        shot("21_starta_tid")
        rule.onNodeWithText("Starta 30 min").performClick()
        now = t(23, 18, 52) + 12 * 60_000 + 20_000; state.tick()
        shot("22_timer_pagar")
        now = t(23, 18, 52) + 31 * 60_000; state.tick()
        shot("23_timer_slut")
    }

    @Test fun calm() {
        start(t(23, 16, 10))
        rule.onNodeWithText("💙 Lugn").performClick()
        shot("30_lugn_lage")
        rule.onNodeWithText("Jag behöver en paus").performClick()
        shot("31_lugn_paus")
    }

    @Test fun night() {
        start(t(23, 21, 40))
        shot("40_natt")
    }

    @Test fun parent() {
        start(t(23, 16, 10))
        rule.onNodeWithText("⚙").performClick()
        shot("50_pin_vuxen")
        listOf("2", "5", "8", "0").forEach { rule.onNodeWithText(it).performClick() }
        shot("51_installningar")
        rule.onNodeWithText("Byt PIN-kod").performScrollTo()
        shot("52_installningar_natt")
        rule.onAllNodesWithText("Ändra ›")[0].performScrollTo().performClick()
        shot("53_redigera_morgon")
        rule.onAllNodesWithText("👕")[0].performClick()
        shot("54_valj_bild")
    }

    @Test fun doubleTapCountsOnce() {
        start(t(23, 6, 47))
        state.debounceMs = 1_000
        rule.onNodeWithText("Klar ✓").performClick()
        rule.onNodeWithText("Klar ✓").performClick()
        assertTrue(store().progress!!.index == 1)
    }

    @Test fun wrongPin() {
        start(t(23, 16, 10))
        rule.onNodeWithText("⚙").performClick()
        listOf("1", "1", "1", "1").forEach { rule.onNodeWithText(it).performClick() }
        rule.onNodeWithText("Fel kod").assertExists()
    }
}
