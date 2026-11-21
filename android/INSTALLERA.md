# Installera Min dag 2.0

Min dag 2.0 är en lugn stödapp som bara ligger på barnets telefon. Den visar morgon- och kvällsrutinen ett steg i taget, har en tårtbitsklocka, en "Starta tid"-timer, ett lugn-läge och ett nattläge. Den har ingen belöning, ingen skärmtid och låser ingenting.

## 1. Avinstallera den gamla Min dag först

Den nya versionen har ett nytt paketnamn (`se.robgra.mindag`) och en ny signeringsnyckel. Den installeras därför inte som en uppdatering.

1. Om den gamla appen är **enhetsadministratör**: öppna **Inställningar → Säkerhet → Enhetsadministratörsappar** (heter olika på olika telefoner) och stäng av Min dag.
2. Stäng av den gamla appens **tillgänglighetstjänst** under **Inställningar → Tillgänglighet**.
3. Avinstallera den gamla **Min dag**: håll fingret på ikonen och välj **Avinstallera**.

Annars får du två appar som båda heter Min dag, och den gamla fortsätter låsa telefonen.

## 2. Installera den nya

1. Öppna länken på barnets telefon: https://github.com/Granfors21/min-dag/releases/latest/download/min-dag.apk
2. Tryck på filen när den laddats ner. Tillåt installation från okända källor om telefonen frågar.
3. Tryck **Installera**.

## 3. Första start (en vuxen gör detta)

1. Välj en **PIN-kod** med 4 siffror. Den skyddar inställningarna.
2. Tryck **Tillåt** på allt i listan:
   - **Aviseringar**: mjuka påminnelser.
   - **Batteri: obegränsad**: annars kan telefonen stoppa påminnelserna.
   - **Exakta larm**: påminnelsen kommer på minuten.
   - **Nattpåminnelse i helskärm**: nattskärmen visas direkt.
3. Samsung, Xiaomi, Huawei, OnePlus med flera har egna batterispärrar. Lägg till Min dag under "Appar som aldrig ska sova", "Autostart" eller liknande.
4. Öppna inställningarna med kugghjulet ⚙ uppe till höger. Där ställer du in tider, steg, bilder, skoldagar, nattläge och ljud.

## Bra att veta

- Allt sparas bara i telefonen. Det finns inget konto och appen behöver inte internet.
- **Glömd PIN:** gå till **Inställningar → Appar → Min dag → Lagring → Rensa data**. Då återställs allt till standard.
- Om någon tvångsstoppar appen ("Tvinga stopp") tar Android bort alla påminnelser tills appen öppnas igen.
