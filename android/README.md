# Min dag 2.1 (Android)

En lugn stödapp för ett barn med autism: morgon- och kvällsrutiner steg för steg, tårtbitsklocka (Time Timer-stil), "Starta tid", lugn-läge och nattläge. Den har ingen belöning, ingen skärmtid och ingen låsning.

Teknik: Kotlin och Jetpack Compose, minSdk 26, targetSdk 34, paket `se.robgra.mindag`.

- `Planner.kt`: ren logik som avgör vilken skärm som ska visas, när det är natt och vilka påminnelser som kommer härnäst. Enhetstestad.
- `Alarms.kt` och `Receivers.kt`: exakta larm i en kedja (nästa händelse planeras när föregående körs). Larmen planeras om vid omstart, uppdatering, ändrad tid och när appen öppnas.
- `NightService.kt`: förgrundstjänst som bara körs under natten. Den lyssnar på `ACTION_USER_PRESENT` och visar en mjuk nattpåminnelse.
- `SafetyWorker.kt`: skyddsnät med WorkManager. Planerar om alla larm var tredje timme.
- `Oem.kt`: guider och direktlänkar till tillverkarnas batterispärrar (Xiaomi, Huawei, Samsung, OPPO, vivo).
- `Recovery.kt`: återställningskod för glömd PIN. Bara en hash sparas.
- `Photos.kt`: egna foton till steg via systemets fotoväljare eller kameran. Ingen lagringsbehörighet; bilderna beskärs till 512×512 och sparas privat.
- `FamilyBridge.kt`: valfri, skyddad koppling till Familjeappen. Se `FAMILJEAPPEN.md`.
- Byggvarianter: `sideload` (GitHub/Drive) och `play`. Se `PLAY-NOTES.md`.
- `res/raw/mjuk_ton.wav`: egen mjuk tvåtonsklocka. Den ligger här som `mjuk_ton.wav.b64`; avkoda med `base64 -d mjuk_ton.wav.b64 > mjuk_ton.wav`.

## Bygga

Kräver JDK 17, Android SDK 34 och Gradle 8.7:

```
gradle :app:testSideloadDebugUnitTest   # logiktester + Robolectric-skärmbilder i app/build/screenshots
gradle :app:assembleSideloadRelease     # kräver keystore.properties (storeFile, storePassword, keyAlias, keyPassword)
```

Keystoren ligger inte i repot.
