@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package se.robgra.mindag

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

private val weekdays = mapOf(
    Calendar.MONDAY to "Måndag", Calendar.TUESDAY to "Tisdag", Calendar.WEDNESDAY to "Onsdag",
    Calendar.THURSDAY to "Torsdag", Calendar.FRIDAY to "Fredag", Calendar.SATURDAY to "Lördag", Calendar.SUNDAY to "Söndag",
)

fun minutesLeftText(ms: Long): String {
    val m = ((ms + 59_999) / 60_000).toInt()
    return when {
        m <= 1 -> "1 minut kvar"
        m < 60 -> "$m minuter kvar"
        else -> { val h = m / 60; val r = m % 60; if (r == 0) "$h tim kvar" else "$h tim $r min kvar" }
    }
}

@Composable
fun KeepScreenOn() {
    val v = LocalView.current
    DisposableEffect(Unit) { v.keepScreenOn = true; onDispose { v.keepScreenOn = false } }
}

@Composable
fun BigButton(text: String, modifier: Modifier = Modifier, color: Color = C.sageDark, textColor: Color = Color.White, height: Dp = 76.dp, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = textColor),
    ) { Text(text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun SoftButton(text: String, modifier: Modifier = Modifier, color: Color = C.blueLight, textColor: Color = C.text, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = modifier.height(60.dp), shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = textColor),
        elevation = ButtonDefaults.buttonElevation(0.dp),
    ) { Text(text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) }
}

/** Gemensam ram för barnets skärmar: liten klocka och dag överst, Lugn-knappen alltid längst ner. */
@Composable
fun ChildFrame(state: AppState, dark: Boolean = false, extraBottom: (@Composable RowScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val bg = if (dark) C.night else C.bg
    val ink = if (dark) C.nightText else C.text
    Column(Modifier.fillMaxSize().background(bg).systemBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val cal = Planner.cal(state.now)
            Text("${weekdays[cal.get(Calendar.DAY_OF_WEEK)]}  ${Planner.fmt(state.now)}", fontSize = 20.sp, color = ink.copy(alpha = 0.8f))
            Spacer(Modifier.weight(1f))
            Text("⚙", fontSize = 22.sp, color = ink.copy(alpha = 0.35f),
                modifier = Modifier.clip(CircleShape).clickable { state.pinTarget = Route.PARENT; state.route = Route.PIN }.padding(10.dp))
        }
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, content = content)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            extraBottom?.invoke(this)
            SoftButton("💙 Lugn", Modifier.weight(1f), color = if (dark) C.nightCard else C.blueLight, textColor = ink) { state.route = Route.CALM }
        }
    }
}

@Composable
fun ChildScreen(state: AppState) {
    when (val s = state.screen()) {
        is Screen.InRoutine -> StepScreen(state, s.routine, s.progress)
        is Screen.RoutineDone -> DoneScreen(state, s.routine)
        is Screen.NightTime -> NightScreen(state, s.info)
        is Screen.TimerRunning -> TimerRunningScreen(state, s.timer)
        is Screen.TimerFinished -> TimerFinishedScreen(state, s.timer)
        is Screen.Free -> FreeScreen(state, s.next)
    }
}

/** Läser upp en text på svenska, bara när barnet trycker (aldrig automatiskt). */
@Composable
fun rememberSpeaker(): (String) -> Unit {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val holder = remember { arrayOfNulls<android.speech.tts.TextToSpeech>(1) }
    DisposableEffect(Unit) { onDispose { holder[0]?.shutdown() } }
    return { text ->
        val tts = holder[0]
        if (tts == null) {
            holder[0] = android.speech.tts.TextToSpeech(ctx) { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    holder[0]?.language = java.util.Locale("sv", "SE")
                    holder[0]?.setSpeechRate(0.85f)
                    holder[0]?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "steg")
                }
            }
        } else tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "steg")
    }
}

@Composable
fun StepScreen(state: AppState, r: Routine, p: Progress) {
    KeepScreenOn()
    val speak = rememberSpeaker()
    val step = r.steps[p.index.coerceIn(0, r.steps.lastIndex)]
    val next = r.steps.getOrNull(p.index + 1)
    ChildFrame(state) {
        Spacer(Modifier.height(4.dp))
        Text(r.title, fontSize = 22.sp, color = C.soft)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            r.steps.indices.forEach { i ->
                Box(Modifier.size(if (i == p.index) 14.dp else 10.dp).clip(CircleShape)
                    .background(if (i < p.index) C.sage else if (i == p.index) C.sageDark else C.sand))
            }
        }
        Spacer(Modifier.weight(0.5f))
        if (step.minutes > 0) {
            val end = p.stepStartedAt + step.minutes * 60_000L
            Box(contentAlignment = Alignment.Center) {
                WedgeClock(state.now, end, Modifier.size(230.dp))
            }
            Spacer(Modifier.height(8.dp))
            val left = end - state.now
            Text(if (left > 0) minutesLeftText(left) else "Tiden är slut. Gör klart i lugn takt.", fontSize = 20.sp, color = C.soft)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(step.icon, fontSize = 56.sp)
                Spacer(Modifier.width(12.dp))
                Text(step.text, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = C.text, lineHeight = 38.sp)
            }
        } else {
            Box(Modifier.size(210.dp).clip(CircleShape).background(C.sageLight), contentAlignment = Alignment.Center) {
                Text(step.icon, fontSize = 110.sp)
            }
            Spacer(Modifier.height(20.dp))
            Text(step.text, fontSize = 36.sp, fontWeight = FontWeight.SemiBold, color = C.text, textAlign = TextAlign.Center, lineHeight = 42.sp)
        }
        Spacer(Modifier.weight(1f))
        BigButton("Klar ✓") { state.checkOff(r, p) }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (next != null) "Sen: ${next.icon} ${next.text}" else "Sen: färdig!", fontSize = 20.sp, color = C.soft, modifier = Modifier.weight(1f))
            TextButton(onClick = { speak(step.text) }) { Text("🔊", fontSize = 20.sp) }
            if (p.index > 0) TextButton(onClick = { state.undo(r, p) }) { Text("↩ Ångra", fontSize = 18.sp, color = C.soft) }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
fun DoneScreen(state: AppState, r: Routine) {
    val next = Planner.nextRoutine(state.settings, state.now)
    ChildFrame(state) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(180.dp).clip(CircleShape).background(C.sageLight), contentAlignment = Alignment.Center) { Text("💚", fontSize = 90.sp) }
        Spacer(Modifier.height(24.dp))
        Text("Bra jobbat!", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = C.text)
        Spacer(Modifier.height(8.dp))
        Text("${r.title}en är klar.", fontSize = 24.sp, color = C.soft)
        if (r.kind == Kind.EVENING && state.settings.nightEnabled) {
            Spacer(Modifier.height(8.dp)); Text("Nu är det snart dags att sova. 🌙", fontSize = 22.sp, color = C.soft, textAlign = TextAlign.Center)
        } else if (next != null) {
            Spacer(Modifier.height(8.dp)); Text("Nästa: ${next.first.title.lowercase()} kl ${Planner.fmt(next.second)}", fontSize = 20.sp, color = C.soft)
        }
        Spacer(Modifier.weight(1f))
        BigButton("Okej") { state.dismissDone() }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun FreeScreen(state: AppState, next: Pair<Routine, Long>?) {
    val soon = next != null && next.second - state.now <= 60 * 60_000L
    ChildFrame(state, extraBottom = { SoftButton("⏱ Starta tid", Modifier.weight(1f), color = C.sageLight) { state.route = Route.TIMER_SETUP } }) {
        Spacer(Modifier.weight(0.6f))
        WedgeClock(state.now, if (soon) next!!.second else null, Modifier.size(260.dp), wedgeColor = C.blue)
        Spacer(Modifier.height(20.dp))
        if (next != null) {
            val sameDay = Planner.dateKey(next.second) == Planner.dateKey(state.now)
            val day = if (sameDay) "" else if (Planner.dateKey(next.second) == Planner.dateKey(state.now + 86_400_000L)) "i morgon " else ""
            Text("Nästa: ${next.first.symbol} ${next.first.title}", fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = C.text)
            Spacer(Modifier.height(4.dp))
            Text("${day}kl ${Planner.fmt(next.second)}" + if (soon) " · om ${minutesLeftText(next.second - state.now).removeSuffix(" kvar")}" else "",
                fontSize = 22.sp, color = C.soft, textAlign = TextAlign.Center)
            if (soon) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = { state.startEarly(next.first) }, shape = RoundedCornerShape(20.dp), modifier = Modifier.height(56.dp)) {
                    Text("Börja ${next.first.title.lowercase()}en nu", fontSize = 20.sp, color = C.sageDark)
                }
            }
        } else {
            Text("Ingen rutin inlagd", fontSize = 24.sp, color = C.soft)
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun NightScreen(state: AppState, info: NightInfo) {
    ChildFrame(state, dark = true) {
        Spacer(Modifier.weight(0.5f))
        Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stars = listOf(0.12f to 0.20f, 0.85f to 0.15f, 0.78f to 0.70f, 0.18f to 0.80f, 0.50f to 0.05f, 0.95f to 0.45f, 0.05f to 0.50f)
                stars.forEachIndexed { i, (x, y) -> drawCircle(C.moon.copy(alpha = 0.55f), size.minDimension * (if (i % 2 == 0) 0.015f else 0.010f), Offset(size.width * x, size.height * y)) }
                val c = Offset(size.width / 2, size.height / 2); val r = size.minDimension * 0.30f
                drawCircle(C.moon, r, c)
                drawCircle(C.night, r * 0.92f, Offset(c.x + r * 0.45f, c.y - r * 0.25f))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Nu är det natt.", fontSize = 36.sp, fontWeight = FontWeight.SemiBold, color = C.nightText)
        Text("Dags att sova.", fontSize = 36.sp, fontWeight = FontWeight.SemiBold, color = C.nightText)
        Spacer(Modifier.height(18.dp))
        Text("Du kan vakna kl ${Planner.fmt(info.wakeAt)}", fontSize = 24.sp, color = C.nightText.copy(alpha = 0.75f))
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun TimerRunningScreen(state: AppState, t: TimerState) {
    KeepScreenOn()
    var confirm by remember { mutableStateOf(false) }
    ChildFrame(state) {
        Spacer(Modifier.weight(0.4f))
        Text("${t.icon} ${t.name}", fontSize = 34.sp, fontWeight = FontWeight.SemiBold, color = C.text)
        Spacer(Modifier.height(16.dp))
        WedgeClock(state.now, t.endsAt, Modifier.size(280.dp))
        Spacer(Modifier.height(16.dp))
        Text(minutesLeftText(t.endsAt - state.now), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = C.text)
        Text("Slutar kl ${Planner.fmt(t.endsAt)}", fontSize = 20.sp, color = C.soft)
        Spacer(Modifier.weight(1f))
        if (confirm) {
            Text("Vill du sluta nu?", fontSize = 22.sp, color = C.text)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SoftButton("Nej", Modifier.weight(1f)) { confirm = false }
                SoftButton("Ja, sluta", Modifier.weight(1f), color = C.wedgeLight) { state.stopTimer() }
            }
        } else {
            TextButton(onClick = { confirm = true }) { Text("Sluta tidigare", fontSize = 20.sp, color = C.soft) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun TimerFinishedScreen(state: AppState, t: TimerState) {
    ChildFrame(state) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(180.dp).clip(CircleShape).background(C.wedgeLight), contentAlignment = Alignment.Center) { Text(t.icon, fontSize = 90.sp) }
        Spacer(Modifier.height(24.dp))
        Text("Tiden är slut", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = C.text)
        Spacer(Modifier.height(8.dp))
        Text("${t.name} är klart för nu.\nSpara och avsluta i lugn takt.", fontSize = 22.sp, color = C.soft, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        BigButton("Okej") { state.dismissTimerDone() }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun TimerSetupScreen(state: AppState) {
    var custom by remember { mutableStateOf(state.store.customActivity) }
    var picked by remember { mutableStateOf<Activity?>(null) }
    var minutes by remember { mutableIntStateOf(20) }
    val acts = defaultActivities + Activity(if (custom.isBlank()) "Egen" else custom, "✏️")
    Column(Modifier.fillMaxSize().background(C.bg).systemBarsPadding().padding(20.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { state.route = Route.CHILD }) { Text("← Tillbaka", fontSize = 20.sp, color = C.soft) }
        }
        Text("Vad ska du göra?", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = C.text)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 3) {
            acts.forEach { a ->
                val sel = picked?.icon == a.icon && picked?.name == a.name || (a.icon == "✏️" && picked?.icon == "✏️")
                Column(
                    Modifier.weight(1f).height(104.dp).clip(RoundedCornerShape(20.dp))
                        .background(if (sel) C.sageLight else C.card)
                        .border(if (sel) 3.dp else 1.dp, if (sel) C.sageDark else C.sand, RoundedCornerShape(20.dp))
                        .clickable { picked = a },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    Text(a.icon, fontSize = 38.sp); Text(a.name, fontSize = 20.sp, color = C.text, maxLines = 1)
                }
            }
        }
        if (picked?.icon == "✏️") {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(custom, { custom = it.take(24) }, label = { Text("Skriv vad du ska göra") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(22.dp))
        Text("Hur länge?", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = C.text)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 4) {
            listOf(5, 10, 15, 20, 30, 45, 60, 90).forEach { m ->
                val sel = m == minutes
                Box(Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(18.dp)).background(if (sel) C.sageLight else C.card)
                    .border(if (sel) 3.dp else 1.dp, if (sel) C.sageDark else C.sand, RoundedCornerShape(18.dp)).clickable { minutes = m },
                    contentAlignment = Alignment.Center) { Text("$m min", fontSize = 20.sp, color = C.text) }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            WedgeClock(state.now, state.now + minutes * 60_000L, Modifier.size(150.dp))
        }
        Spacer(Modifier.height(18.dp))
        val ok = picked != null && !(picked?.icon == "✏️" && custom.isBlank())
        BigButton(if (ok) "Starta ${minutes} min" else "Välj något först", color = if (ok) C.sageDark else C.sand, textColor = if (ok) Color.White else C.soft) {
            val a = picked ?: return@BigButton
            if (a.icon == "✏️") { if (custom.isBlank()) return@BigButton; state.store.customActivity = custom; state.startTimer(Activity(custom.trim(), "✏️"), minutes) }
            else state.startTimer(a, minutes)
        }
    }
}

@Composable
fun CalmScreen(state: AppState, animate: Boolean) {
    var pause by remember { mutableStateOf(false) }
    // 4 s in, 6 s ut, mjuk kurva. Ingen blinkning.
    var phase by remember { mutableLongStateOf(1_000L) }
    if (animate) LaunchedEffect(Unit) {
        var start = -1L
        while (true) { androidx.compose.runtime.withFrameMillis { t -> if (start < 0) start = t; phase = (t - start) % 10_000 } }
    }
    val (scale, inhale) = breath(phase)
    Column(Modifier.fillMaxSize().background(C.blueLight).systemBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) { TextButton(onClick = { state.route = Route.CHILD }) { Text("← Tillbaka", fontSize = 20.sp, color = C.soft) } }
        Spacer(Modifier.weight(0.5f))
        if (!pause) {
            Text("Andas med cirkeln", fontSize = 26.sp, color = C.text)
            Spacer(Modifier.height(24.dp))
            Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize().scale(scale).clip(CircleShape).background(C.blue.copy(alpha = 0.55f)))
                Box(Modifier.fillMaxSize(0.5f).scale(scale).clip(CircleShape).background(Color.White.copy(alpha = 0.55f)))
            }
            Spacer(Modifier.height(24.dp))
            Text(if (inhale) "Andas in …" else "Andas ut …", fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = C.text)
            Spacer(Modifier.weight(1f))
            BigButton("Jag behöver en paus", color = C.blue, textColor = C.text) { pause = true }
        } else {
            Text("🤍", fontSize = 90.sp)
            Spacer(Modifier.height(18.dp))
            Text("Det är okej att ta en paus.", fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = C.text, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("Gå till din lugna plats.\nSäg till en vuxen när du vill.", fontSize = 22.sp, color = C.soft, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            BigButton("Andas igen", color = C.blue, textColor = C.text) { pause = false }
        }
        Spacer(Modifier.height(8.dp))
    }
}

fun breath(phase: Long): Pair<Float, Boolean> {
    fun ease(x: Float) = (1f - kotlin.math.cos(Math.PI * x).toFloat()) / 2f
    return if (phase < 4_000) (0.55f + 0.45f * ease(phase / 4_000f)) to true
    else (1f - 0.45f * ease((phase - 4_000) / 6_000f)) to false
}
