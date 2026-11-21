# Google Play: anteckningar för Min dag

Det finns två byggvarianter:

- `sideload`: APK:n på GitHub och Drive.
- `play`: inför Google Play. Bygg med `gradle :app:bundlePlayRelease`.

## Behörigheter och motivering

| Behörighet | Varför | Play-risk |
|---|---|---|
| `POST_NOTIFICATIONS` | Mjuka påminnelser om rutiner, timer och läggdags. | Ingen. |
| `USE_EXACT_ALARM` | Appens kärna är ett schema med tider och en timer, som en väckarklocka eller kalender. Påminnelsen ska komma på minuten. Beviljas automatiskt och kan inte stängas av av misstag. | Kräver deklarationen "Exakta larm" i Play Console. Kategorin är tillåten för larm-, timer- och kalenderappar. Godkänns den inte: byt till `SCHEDULE_EXACT_ALARM` (utan maxSdk). Appen faller redan tillbaka på inexakta larm om behörigheten saknas. |
| `SCHEDULE_EXACT_ALARM` (maxSdk 32) | Samma sak, för Android 12. | Ingen. |
| `RECEIVE_BOOT_COMPLETED` | Planerar om larmen efter omstart. | Ingen. |
| `VIBRATE` | Valfri vibration. | Ingen. |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Nattjänsten körs bara mellan läggdags och vakna. Den lyssnar på `ACTION_USER_PRESENT`, som inte kan registreras i manifestet, och visar en lugn påminnelse när barnet låser upp telefonen. | Kräver deklaration med text och gärna en video. Får vi nej kan jag stänga av funktionen i `play`-varianten. Nattskärmen när appen öppnas och påminnelsen vid läggdags fungerar ändå. |
| `WAKE_LOCK`, `ACCESS_NETWORK_STATE` | Följer med WorkManager (skyddsnätet). Appen använder aldrig internet. | Ingen. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | **Bara i `sideload`.** | Finns inte i `play`. Där öppnar knappen systemets lista över batterioptimering i stället (ingen behörighet krävs). |

Borttaget i 2.1:

- `USE_FULL_SCREEN_INTENT`. Den gav ingen nytta: barnet har redan låst upp telefonen när påminnelsen visas. Play begränsar den dessutom till samtals- och larmappar.

## Övrigt inför Play

- **Data säkerhet:** ingen data samlas in eller delas. Allt sparas lokalt, och appen har ingen internetbehörighet.
- **Målgrupp:** appen används av barn men konfigureras av en vuxen. Play-programmet "Designed for Families" ställer krav på annonser (inga finns) och dataskydd (uppfylls).
- **Exporterad ContentProvider** (`FamilyBridge`): koden släpper bara in Familjeappen. Det kontrolleras med paketnamn och SHA-256 av signeringscertifikatet. Play signerar appen med en egen nyckel ("Play App Signing"). Då måste certifikatet för Play-signeringen läggas till i `FamilyBridge.FAMILY_CERT` (och i Familjeappen, `CompanionApps.MINDAG_CERT`).
- **`<queries>`:** listar Familjeappen och några tillverkares batteriappar. Det är tillåtet: specifika paket, inte `QUERY_ALL_PACKAGES`.
- **Skärmbilder:** finns i `app/build/screenshots` efter `gradle :app:testSideloadDebugUnitTest`.
