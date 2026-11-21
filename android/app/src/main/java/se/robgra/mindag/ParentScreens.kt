@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package se.robgra.mindag

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Calendar

@Composable
fun ParentFrame(title: String, onBack: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(C.bg).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) TextButton(onClick = onBack) { Text("← Klar", fontSize = 18.sp, color = C.sageDark) }
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = C.text, modifier = Modifier.padding(start = 8.dp))
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), content = content)
    }
}

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(14.dp))
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.soft)
    Spacer(Modifier.height(6.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.card).padding(14.dp), content = content)
}

@Composable
fun ToggleRow(label: String, value: Boolean, sub: String? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 18.sp, color = C.text)
            if (sub != null) Text(sub, fontSize = 14.sp, color = C.soft)
        }
        Switch(value, onChange)
    }
}

@Composable
fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (label.isNotEmpty()) Text(label, fontSize = 16.sp, color = C.soft, modifier = Modifier.padding(end = 6.dp))
        RoundIcon("−", onMinus)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = C.text, modifier = Modifier.widthIn(min = 64.dp), textAlign = TextAlign.Center)
        RoundIcon("+", onPlus)
    }
}

@Composable
fun RoundIcon(t: String, onClick: () -> Unit) {
    Box(Modifier.size(42.dp).clip(CircleShape).background(C.sand).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(t, fontSize = 22.sp, color = C.text)
    }
}

/** Tid med 5-minuterssteg, begränsad till ett rimligt intervall. */
@Composable
fun TimeStepper(label: String, h: Int, m: Int, minH: Int = 0, maxH: Int = 23, onChange: (Int, Int) -> Unit) {
    fun shift(d: Int) {
        var t = h * 60 + m + d
        t = t.coerceIn(minH * 60, maxH * 60 + 55)
        onChange(t / 60, t % 60)
    }
    Stepper(label, Planner.fmt(h, m), { shift(-5) }, { shift(5) })
}

// ---------- PIN ----------

@Composable
fun PinPad(title: String, sub: String?, error: String?, onDone: (String) -> Unit, onCancel: (() -> Unit)?, footer: (@Composable () -> Unit)? = null) {
    var pin by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(C.bg).systemBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) { if (onCancel != null) TextButton(onClick = onCancel) { Text("← Tillbaka", fontSize = 18.sp, color = C.soft) } }
        Spacer(Modifier.weight(0.5f))
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = C.text, textAlign = TextAlign.Center)
        if (sub != null) { Spacer(Modifier.height(6.dp)); Text(sub, fontSize = 17.sp, color = C.soft, textAlign = TextAlign.Center) }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { i -> Box(Modifier.size(20.dp).clip(CircleShape).background(if (i < pin.length) C.sageDark else C.sand)) }
        }
        Spacer(Modifier.height(10.dp))
        Text(error ?: " ", fontSize = 16.sp, color = C.wedge)
        Spacer(Modifier.height(10.dp))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                row.forEach { k ->
                    Box(Modifier.size(78.dp).clip(CircleShape).background(if (k.isEmpty()) Color.Transparent else C.card)
                        .clickable(enabled = k.isNotEmpty()) {
                            if (k == "⌫") pin = pin.dropLast(1)
                            else if (pin.length < 4) { pin += k; if (pin.length == 4) { val p = pin; pin = ""; onDone(p) } }
                        }, contentAlignment = Alignment.Center) { Text(k, fontSize = 28.sp, color = C.text) }
                }
            }
        }
        footer?.invoke()
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun WelcomeScreen(state: AppState) {
    Column(Modifier.fillMaxSize().background(C.bg).systemBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        WedgeClock(state.now, state.now + 20 * 60_000L, Modifier.size(180.dp), wedgeColor = C.sage)
        Spacer(Modifier.height(24.dp))
        Text("Välkommen till Min dag", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = C.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("En lugn stödapp för morgon, kväll och natt.\nAllt sparas bara i den här telefonen.\n\nFörst väljer en vuxen en PIN-kod med 4 siffror. Den skyddar inställningarna.",
            fontSize = 19.sp, color = C.soft, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        BigButton("Välj PIN-kod") { state.route = Route.PIN_SETUP }
    }
}

@Composable
fun PinSetupScreen(state: AppState) {
    var first by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    val changing = state.settings.pin.isNotEmpty()
    PinPad(
        if (first == null) (if (changing) "Välj ny PIN-kod" else "Välj PIN-kod för vuxna") else "Skriv samma PIN igen",
        if (first == null) "4 siffror. Välj något barnet inte gissar, t.ex. inte 1234." else null, err,
        onDone = { p ->
            if (first == null) {
                if (p in setOf("0000", "1111", "1234", "4321")) err = "Välj en svårare kod" else { first = p; err = null }
            } else if (first == p) {
                state.updateSettings { it.copy(pin = p) }
                when {
                    !state.settings.onboarded -> state.newRecoveryCode(Route.CHECKS)
                    state.recovering -> { state.recovering = false; state.newRecoveryCode(Route.PARENT) }
                    else -> state.route = Route.PARENT
                }
            } else { first = null; err = "Koderna var olika. Försök igen." }
        },
        onCancel = if (state.recovering) null else ({ state.route = if (changing) Route.PARENT else Route.WELCOME }),
    )
}

private var wrongCount = 0
private var lockedUntil = 0L

@Composable
fun PinEntryScreen(state: AppState) {
    var err by remember { mutableStateOf<String?>(null) }
    PinPad("Vuxenläge", "Skriv PIN-koden", err, onDone = { p ->
        val now = System.currentTimeMillis()
        if (now < lockedUntil) { err = "Vänta en stund och försök igen" ; return@PinPad }
        if (p == state.settings.pin) { wrongCount = 0; state.route = state.pinTarget }
        else {
            wrongCount++
            if (wrongCount >= 5) { lockedUntil = now + 60_000; wrongCount = 0; err = "Fel kod. Vänta en minut." } else err = "Fel kod"
        }
    }, onCancel = { state.route = Route.CHILD }, footer = {
        TextButton(onClick = { state.route = Route.RECOVERY_ENTER }) { Text("Glömt PIN-koden?", fontSize = 17.sp, color = C.soft) }
    })
}

// ---------- Inställningar ----------

private val dayLetters = listOf(Calendar.MONDAY to "Mån", Calendar.TUESDAY to "Tis", Calendar.WEDNESDAY to "Ons",
    Calendar.THURSDAY to "Tor", Calendar.FRIDAY to "Fre", Calendar.SATURDAY to "Lör", Calendar.SUNDAY to "Sön")

fun typeName(t: DayType) = if (t == DayType.SCHOOL) "Skoldag" else "Ledig dag"

@Composable
fun ParentScreen(state: AppState) {
    val s = state.settings
    val ctx = LocalContext.current
    ParentFrame("Inställningar (vuxen)", onBack = { state.route = Route.CHILD }) {
        Section("Rutiner") {
            s.routines.forEach { r ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { state.editing = r.id; state.route = Route.ROUTINE_EDIT }
                    .padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(r.symbol, fontSize = 26.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${r.title} · ${typeName(r.dayType).lowercase()}", fontSize = 18.sp, color = C.text)
                        Text(if (r.enabled) "kl ${Planner.fmt(r.hour, r.minute)} · ${r.steps.size} steg · ca ${r.totalMinutes} min" else "Avstängd", fontSize = 14.sp, color = C.soft)
                    }
                    Text("Ändra ›", fontSize = 16.sp, color = C.sageDark)
                }
            }
        }
        Section("Vilka dagar är skoldagar?") {
            Text("Övriga dagar använder rutinerna för ledig dag.", fontSize = 14.sp, color = C.soft)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                dayLetters.forEach { (d, n) ->
                    val on = d in s.schoolDays
                    Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).background(if (on) C.sageLight else C.sand)
                        .border(if (on) 2.dp else 0.dp, if (on) C.sageDark else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { state.updateSettings { it.copy(schoolDays = if (on) it.schoolDays - d else it.schoolDays + d) } },
                        contentAlignment = Alignment.Center) { Text(n, fontSize = 15.sp, color = C.text) }
                }
            }
        }
        Section("Nattläge") {
            ToggleRow("Nattläge på", s.nightEnabled, "Visar en lugn nattskärm mellan läggdags och vakna. Telefonen låses inte.") { v -> state.updateSettings { it.copy(nightEnabled = v) } }
            if (s.nightEnabled && s.followFamily && s.familyNights.isNotEmpty())
                Text("Tiderna styrs nu av Familjeappen (se nedan). Tiderna här gäller om kopplingen stängs av.", fontSize = 14.sp, color = C.sageDark)
            if (s.nightEnabled) {
                Text("Natt före skoldag", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = C.text, modifier = Modifier.padding(top = 8.dp))
                NightRows(s.nightSchool) { n -> state.updateSettings { it.copy(nightSchool = n) } }
                Text("Natt före ledig dag", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = C.text, modifier = Modifier.padding(top = 12.dp))
                NightRows(s.nightFree) { n -> state.updateSettings { it.copy(nightFree = n) } }
                Spacer(Modifier.height(6.dp))
                ToggleRow("Påminn när telefonen låses upp", s.nightReminderOnUnlock, "Mjuk påminnelse om att sova. Telefonen låses inte.") { v -> state.updateSettings { it.copy(nightReminderOnUnlock = v) } }
            }
        }
        Section("Ljud och vibration") {
            ToggleRow("Mjukt ljud", s.sound) { v -> state.updateSettings { it.copy(sound = v) } }
            ToggleRow("Vibration", s.vibration) { v -> state.updateSettings { it.copy(vibration = v) } }
            OutlinedButton(onClick = { Notify.show(ctx, Notify.ID_ROUTINE, "🦷 Test", "Så här låter och ser en påminnelse ut.") }) { Text("Testa en påminnelse", fontSize = 16.sp) }
        }
        ReminderStatusSection(state)
        FamilySection(state)
        Section("Telefonen") {
            Text("Kontrollera att påminnelserna kommer fram, även när telefonen sparar batteri.", fontSize = 15.sp, color = C.soft)
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { state.route = Route.CHECKS }) { Text("Kontrollera behörigheter", fontSize = 16.sp) }
        }
        Section("PIN-kod") {
            if (s.recoveryHash.isEmpty()) {
                Text("⚠️ Ingen återställningskod finns. Skapa en, annars kan en glömd PIN bara lösas genom att rensa appens data.", fontSize = 15.sp, color = C.wedge)
                Spacer(Modifier.height(6.dp))
            }
            OutlinedButton(onClick = { state.route = Route.PIN_SETUP }) { Text("Byt PIN-kod", fontSize = 16.sp) }
            OutlinedButton(onClick = { state.newRecoveryCode(Route.PARENT) }) {
                Text(if (s.recoveryHash.isEmpty()) "Skapa återställningskod" else "Skapa ny återställningskod (den gamla slutar gälla)", fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Min dag ${BuildConfig.VERSION_NAME} · sparar allt lokalt, inget konto, ingen internet.", fontSize = 13.sp, color = C.soft)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun NightRows(n: Night, onChange: (Night) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("🌙 Läggdags", fontSize = 16.sp, color = C.soft, modifier = Modifier.weight(1f))
        TimeStepper("", n.bedH, n.bedM, minH = 17, maxH = 23) { h, m -> onChange(n.copy(bedH = h, bedM = m)) }
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("☀️ Vakna", fontSize = 16.sp, color = C.soft, modifier = Modifier.weight(1f))
        TimeStepper("", n.wakeH, n.wakeM, minH = 4, maxH = 11) { h, m -> onChange(n.copy(wakeH = h, wakeM = m)) }
    }
}

@Composable
fun RoutineEditScreen(state: AppState) {
    val id = state.editing ?: run { state.route = Route.PARENT; return }
    val r = state.settings.routines.first { it.id == id }
    var iconFor by remember { mutableStateOf<Int?>(null) }
    var photoFor by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(-1) }
    val ctx = LocalContext.current
    fun save(nr: Routine) = state.updateSettings { s -> s.copy(routines = s.routines.map { if (it.id == id) nr else it }) }
    fun setImage(i: Int, name: String?) {
        val cur = state.settings.routines.first { it.id == id }
        if (i !in cur.steps.indices) return
        save(cur.copy(steps = cur.steps.toMutableList().also { it[i] = it[i].copy(image = name) }))
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && photoFor >= 0) Photos.importUri(ctx, uri)?.let { setImage(photoFor, it) }
        photoFor = -1; iconFor = null
    }
    val shoot = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && photoFor >= 0) Photos.importUri(ctx, Uri.fromFile(Photos.cameraTemp(ctx)))?.let { setImage(photoFor, it) }
        Photos.cameraTemp(ctx).delete()
        photoFor = -1; iconFor = null
    }
    ParentFrame("${r.title} · ${typeName(r.dayType).lowercase()}", onBack = { state.route = Route.PARENT }) {
        Section("Start") {
            ToggleRow("Rutinen är på", r.enabled) { save(r.copy(enabled = it)) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Börjar", fontSize = 18.sp, color = C.text, modifier = Modifier.weight(1f))
                TimeStepper("", r.hour, r.minute, minH = if (r.kind == Kind.MORNING) 4 else 14, maxH = if (r.kind == Kind.MORNING) 12 else 23) { h, m -> save(r.copy(hour = h, minute = m)) }
            }
            Text("Mjuk påminnelse ${Planner.SOON_MIN} min innan och när det är dags.", fontSize = 14.sp, color = C.soft)
        }
        Section("Steg (i ordning)") {
            r.steps.forEachIndexed { i, st ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(14.dp)).background(C.bg).padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(if (iconFor == i) C.sageLight else C.card).clickable { iconFor = if (iconFor == i) null else i }, contentAlignment = Alignment.Center) {
                            StepPicture(st, 46.dp, 28.sp, bg = androidx.compose.ui.graphics.Color.Transparent)
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(st.text, { t -> save(r.copy(steps = r.steps.toMutableList().also { it[i] = st.copy(text = t.take(40)) })) },
                            singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 18.sp))
                    }
                    if (iconFor == i) {
                        FlowRow(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                photoFor = i
                                pick.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) { Text("🖼 Eget foto", fontSize = 15.sp) }
                            OutlinedButton(onClick = {
                                photoFor = i
                                runCatching {
                                    val f = Photos.cameraTemp(ctx)
                                    shoot.launch(androidx.core.content.FileProvider.getUriForFile(ctx, "se.robgra.mindag.files", f))
                                }
                            }) { Text("📷 Kamera", fontSize = 15.sp) }
                            if (st.image != null) TextButton(onClick = { setImage(i, null); iconFor = null }) { Text("Ta bort foto", fontSize = 15.sp, color = C.soft) }
                        }
                        Text("Eller välj en bild:", fontSize = 15.sp, color = C.soft, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Defaults.icons.forEach { ic ->
                                Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(if (ic == st.icon) C.sageLight else C.card).clickable {
                                    save(r.copy(steps = r.steps.toMutableList().also { it[i] = st.copy(icon = ic, image = null) })); iconFor = null
                                }, contentAlignment = Alignment.Center) { Text(ic, fontSize = 24.sp) }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Stepper("Tid", if (st.minutes == 0) "ingen" else "${st.minutes} min",
                            { save(r.copy(steps = r.steps.toMutableList().also { it[i] = st.copy(minutes = (st.minutes - 1).coerceAtLeast(0)) })) },
                            { save(r.copy(steps = r.steps.toMutableList().also { it[i] = st.copy(minutes = (st.minutes + 1).coerceAtMost(60)) })) })
                        Spacer(Modifier.weight(1f))
                        if (i > 0) RoundIcon("▲") { save(r.copy(steps = r.steps.toMutableList().also { java.util.Collections.swap(it, i, i - 1) })) }
                        Spacer(Modifier.width(4.dp))
                        if (i < r.steps.lastIndex) RoundIcon("▼") { save(r.copy(steps = r.steps.toMutableList().also { java.util.Collections.swap(it, i, i + 1) })) }
                        Spacer(Modifier.width(4.dp))
                        if (r.steps.size > 1) RoundIcon("✕") { save(r.copy(steps = r.steps.toMutableList().also { it.removeAt(i) })) }
                    }
                }
            }
            OutlinedButton(onClick = { save(r.copy(steps = r.steps + Step("Nytt steg", "⭐", 0))) }, enabled = r.steps.size < 15) { Text("+ Lägg till steg", fontSize = 16.sp) }
            Text("Tips: 4–8 korta steg brukar fungera bäst.", fontSize = 14.sp, color = C.soft)
        }
        Section("Återställ") {
            OutlinedButton(onClick = { save(Defaults.routines.first { it.id == id }) }) { Text("Återställ standardsteg", fontSize = 16.sp) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ChecksScreen(state: AppState, firstRun: Boolean) {
    val ctx = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    @Suppress("UNUSED_EXPRESSION") refresh
    val notif = Notify.canPost(ctx)
    val exact = Alarms.exactAllowed(ctx)
    val battery = Alarms.batteryUnrestricted(ctx)
    fun open(i: Intent) { try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {
        ctx.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    ParentFrame("Telefonens inställningar", onBack = if (firstRun) null else ({ state.route = Route.PARENT })) {
        Text("För att påminnelserna ska komma i tid behöver appen några tillåtelser. Inget av detta låser telefonen.", fontSize = 16.sp, color = C.soft)
        CheckRow("Aviseringar", "Mjuka påminnelser om rutiner, timer och natt", notif) {
            if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else open(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, ctx.packageName))
        }
        CheckRow("Batteri: obegränsad", "Annars kan telefonen stoppa påminnelser för att spara batteri", battery) {
            open(batteryIntent(ctx))
        }
        CheckRow("Exakta larm", "Påminnelsen kommer på minuten", exact) {
            if (Build.VERSION.SDK_INT >= 31) open(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
        }
        OemCard()
        Spacer(Modifier.height(20.dp))
        if (firstRun) BigButton("Klart, visa barnets vy") { state.updateSettings { it.copy(onboarded = true) }; state.route = Route.CHILD }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun CheckRow(title: String, sub: String, ok: Boolean, onFix: () -> Unit) {
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.card).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (ok) "✅" else "⚠️", fontSize = 24.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) { Text(title, fontSize = 17.sp, color = C.text, fontWeight = FontWeight.SemiBold); Text(sub, fontSize = 14.sp, color = C.soft) }
        if (!ok) Button(onClick = onFix, shape = RoundedCornerShape(14.dp)) { Text("Tillåt", fontSize = 16.sp) }
    }
}
