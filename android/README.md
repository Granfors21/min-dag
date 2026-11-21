# Min dag 2.0 (Android)

En lugn stödapp för ett barn med autism: morgon- och kvällsrutiner steg för steg, tårtbitsklocka (Time Timer-stil), "Starta tid", lugn-läge och nattläge. Den har ingen belöning, ingen skärmtid och ingen låsning.

Teknik: Kotlin och Jetpack Compose, minSdk 26, targetSdk 34, paket `se.robgra.mindag`.

- `Planner.kt`: ren logik som avgör vilken skärm som ska visas, när det är natt och vilka påminnelser som kommer härnäst. Enhetstestad.
- `Alarms.kt` och `Receivers.kt`: exakta larm i en kedja (nästa händelse planeras när föregående körs). Larmen planeras om vid omstart, uppdatering, ändrad tid och när appen öppnas.
- `NightService.kt`: förgrundstjänst som bara körs under natten. Den lyssnar på `ACTION_USER_PRESENT` och visar en mjuk nattpåminnelse.
- `res/raw/mjuk_ton.wav`: egen mjuk tvåtonsklocka. Den ligger här som `mjuk_ton.wav.b64`; avkoda med `base64 -d mjuk_ton.wav.b64 > mjuk_ton.wav`.

## Bygga

Kräver JDK 17, Android SDK 34 och Gradle 8.7:

```
gradle :app:testDebugUnitTest      # logiktester + Robolectric-skärmbilder i app/build/screenshots
gradle :app:assembleRelease        # kräver keystore.properties (storeFile, storePassword, keyAlias, keyPassword)
```

Keystoren ligger inte i repot.
