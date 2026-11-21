@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package se.robgra.mindag

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

private val dayShort = mapOf(Calendar.MONDAY to "mån", Calendar.TUESDAY to "tis", Calendar.WEDNESDAY to "ons",
    Calendar.THURSDAY to "tor", Calendar.FRIDAY to "fre", Calendar.SATURDAY to "lör", Calendar.SUNDAY to "sön")

fun whenText(now: Long, t: Long): String {
    if (t <= 0) return "–"
    val d = when (Planner.dateKey(t)) {
        Planner.dateKey(now) -> "i dag"
        Planner.dateKey(now + 86_400_000L) -> "i morgon"
        Planner.dateKey(now - 86_400_000L) -> "i går"
        else -> dayShort[Planner.cal(t).get(Calendar.DAY_OF_WEEK)] ?: ""
    }
    return "$d ${Planner.fmt(t)}"
}

@Composable
fun RecoveryShowScreen(state: AppState) {
    val code = state.shownCode ?: "–"
    var saved by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(C.bg).systemBarsPadding().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(30.dp))
        Text("🔑", fontSize = 60.sp)
        Spacer(Modifier.height(10.dp))
        Text("Din återställningskod", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = C.text)
        Spacer(Modifier.height(10.dp))
        Text("Om du glömmer PIN-koden kan du använda den här koden. Då väljer du en ny PIN och alla rutiner och inställningar finns kvar.",
            fontSize = 17.sp, color = C.soft, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.clip(RoundedCornerShape(18.dp)).background(C.card).padding(horizontal = 28.dp, vertical = 18.dp)) {
            Text(code, fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = C.text, letterSpacing = 3.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text("Skriv ner den eller fota den med DIN telefon och spara den där barnet inte hittar den. Koden visas bara nu.",
            fontSize = 16.sp, color = C.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { saved = !saved }.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(saved, { saved = it }); Text("Jag har sparat koden", fontSize = 18.sp, color = C.text)
        }
        Spacer(Modifier.height(18.dp))
        BigButton("Fortsätt", color = if (saved) C.sageDark else C.sand, textColor = if (saved) androidx.compose.ui.graphics.Color.White else C.soft) {
            if (saved) { state.shownCode = null; state.route = state.afterCode }
        }
    }
}

@Composable
fun RecoveryEnterScreen(state: AppState) {
    var code by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    val has = state.settings.recoveryHash.isNotEmpty()
    ParentFrame("Glömt PIN-koden", onBack = { state.route = Route.PIN }) {
        Spacer(Modifier.height(10.dp))
        if (has) {
            Text("Skriv återställningskoden du fick när PIN-koden valdes (8 tecken, t.ex. K7QM-4XPA). Rutiner och inställningar finns kvar.",
                fontSize = 17.sp, color = C.soft)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(code, { code = it.take(12); err = null }, label = { Text("Återställningskod") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontFamily = FontFamily.Monospace))
            Text(err ?: " ", color = C.wedge, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            BigButton("Återställ PIN") {
                if (Recovery.matches(code, state.settings.recoveryHash)) { state.recovering = true; state.route = Route.PIN_SETUP }
                else { err = "Koden stämmer inte"; Thread.sleep(400) }
            }
        } else {
            Text("Ingen återställningskod har skapats än (den kom i version 2.1).", fontSize = 17.sp, color = C.text)
            Spacer(Modifier.height(10.dp))
            Text("Då återstår: Inställningar → Appar → Min dag → Lagring → Rensa data. Det tar bort PIN och egna rutiner.\n\nSkapa en återställningskod under Inställningar → PIN-kod nästa gång du är inne, så slipper du det.",
                fontSize = 16.sp, color = C.soft)
        }
    }
}

@Composable
fun ReminderStatusSection(state: AppState) {
    val st = state.store
    val ctx = LocalContext.current
    Section("Påminnelser just nu") {
        Text("Nästa: ${if (st.nextAt > 0) "${st.nextWhat} · ${whenText(state.now, st.nextAt)}" else "ingen planerad"}", fontSize = 17.sp, color = C.text)
        Text("Senaste larm som kom fram: ${whenText(state.now, st.lastFired)}", fontSize = 15.sp, color = C.soft)
        Text("Skyddsnät kördes senast: ${whenText(state.now, st.lastSafety)}", fontSize = 15.sp, color = C.soft)
        if (!Notify.canPost(ctx) || !Alarms.exactAllowed(ctx))
            Text("⚠️ Något saknas. Tryck \"Kontrollera behörigheter\".", fontSize = 15.sp, color = C.wedge)
        Spacer(Modifier.height(4.dp))
        ToggleRow("Extra säkra larm", state.settings.strongAlarms, "Rutinstart och läggdags som väckarklockelarm. Säkrast, men en liten larmikon syns.") { v ->
            state.updateSettings { it.copy(strongAlarms = v) }
        }
    }
}

@Composable
fun FamilySection(state: AppState) {
    val ctx = LocalContext.current
    val installed = remember { FamilyBridge.familyInstalled(ctx) }
    val s = state.settings
    Section("Familjeappen") {
        if (!installed && !s.followFamily) {
            Text("Familjeappen finns inte på telefonen. Min dag fungerar helt själv.", fontSize = 15.sp, color = C.soft)
        } else {
            ToggleRow("Låt Familjeappen styra läggdags", s.followFamily,
                "Läggdags och vakna hämtas från Familjeappens regler. Bara Familjeappen signerad med din nyckel får läsa och ändra.") { v ->
                state.updateSettings { it.copy(followFamily = v) }
            }
            if (s.followFamily) {
                if (s.familyNights.isEmpty()) Text("Väntar på tider från Familjeappen. Tills dess gäller tiderna under Nattläge.", fontSize = 14.sp, color = C.soft)
                else listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY).forEach { d ->
                    s.familyNights[d]?.let { n -> Text("${dayShort[d]}kväll: ${Planner.fmt(n.bedH, n.bedM)} → ${Planner.fmt(n.wakeH, n.wakeM)}", fontSize = 15.sp, color = C.text) }
                }
            }
        }
    }
}

@Composable
fun OemCard(brandOverride: String? = null) {
    val ctx = LocalContext.current
    val g = Oem.guide(ctx, brandOverride ?: Oem.brand())
    Spacer(Modifier.height(12.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.card).padding(14.dp)) {
        if (g != null) {
            Text("📱 ${g.brand}: extra batterispärr", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = C.text)
            Spacer(Modifier.height(4.dp))
            Text(g.steps, fontSize = 15.sp, color = C.soft)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { Oem.open(ctx, g) }, shape = RoundedCornerShape(14.dp)) {
                Text(when { "Xiaomi" in g.brand -> "Öppna Autostart"; "Huawei" in g.brand -> "Öppna Appstart"; "Samsung" in g.brand -> "Öppna Batteri"; else -> "Öppna inställningar" }, fontSize = 16.sp)
            }
        } else {
            Text("📱 Telefonens batteriinställningar", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = C.text)
            Text("Appinfo → Batteri → \"Obegränsad\" (eller \"Ej optimerad\").", fontSize = 15.sp, color = C.soft)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { Oem.open(ctx, null) }) { Text("Öppna appinfo", fontSize = 16.sp) }
        }
        Spacer(Modifier.height(8.dp))
        Text("Ärligt: om någon trycker \"Tvinga stopp\" på Min dag stänger Android av alla larm tills appen öppnas igen. Det går inte att komma runt. Öppna Min dag en gång så är allt igång igen.",
            fontSize = 14.sp, color = C.soft)
    }
}

fun batteryIntent(ctx: android.content.Context): Intent =
    if (BuildConfig.FLAVOR == "sideload")
        Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, android.net.Uri.parse("package:${ctx.packageName}"))
    else Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
