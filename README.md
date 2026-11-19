# Min dag

En lugn dagsapp (PWA) som hjälper ett barn att ta sig igenom morgon och kväll i tydliga steg. All data sparas bara i telefonen — inget skickas till någon server.

## Ladda ner Android-appen

Direktlänk (öppna på en Android-telefon): https://github.com/Granfors21/min-dag/releases/latest/download/min-dag.apk

Reservlänk: https://granfors21.github.io/min-dag/min-dag.apk

Tillåt installation från okända källor när telefonen frågar, och tryck sedan **Installera**.

## Öppna webbappen

Adress: https://granfors21.github.io/min-dag/

På Android: öppna länken i Chrome, tryck på menyn och välj **Lägg till på startskärmen**. Därefter fungerar appen som en vanlig app, även utan internet.

Nattlås och skärmtidsgränser i webbappen sätts med [Google Family Link](https://families.google/familylink/). I Android-appen (APK) hanteras lås i appen.

## Bygga om

Källkoden publiceras som en färdig webbapp under `app/`. Binära filer (ikoner, typsnitt och APK) ligger som `.b64` och avkodas automatiskt när GitHub Pages byggs.
