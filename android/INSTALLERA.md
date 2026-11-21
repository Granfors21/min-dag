# Installera Min dag

Min dag är en lugn stödapp som bara ligger på barnets telefon. Den visar morgon- och kvällsrutinen ett steg i taget och har en tårtbitsklocka, "Starta tid", ett lugn-läge och ett nattläge. Den har ingen belöning och ingen skärmtid, och den låser ingenting, inte heller på natten.

Ladda ner på barnets telefon: https://github.com/Granfors21/min-dag/releases/latest/download/min-dag.apk

## Har du redan 2.0?

Öppna länken och tryck **Installera**. 2.1 installeras som en uppdatering, och alla rutiner, foton och PIN-koden finns kvar.

Efter uppdateringen: gå in i inställningarna (⚙ och PIN) och tryck **Skapa återställningskod** under PIN-kod. Skriv ner koden.

## Har du den gamla Min dag (1.x, med låsning och belöning)?

Den har ett annat paketnamn och en annan signeringsnyckel. Den måste tas bort först:

1. Stäng av den gamla appen som **enhetsadministratör**: Inställningar → Säkerhet → Enhetsadministratörsappar.
2. Stäng av dess **tillgänglighetstjänst** under Inställningar → Tillgänglighet.
3. **Avinstallera** den gamla Min dag.
4. Installera den nya från länken ovan.

## Första start (en vuxen gör detta)

1. Välj en **PIN-kod** med 4 siffror.
2. Du får en **återställningskod** (t.ex. `K7QM-4XPA`). Fota den med din egen telefon eller skriv ner den. Glömmer du PIN-koden skriver du in koden under **Glömt PIN-koden?**. Då väljer du en ny PIN och allt annat finns kvar.
3. Tryck **Tillåt** på allt i listan: aviseringar, batteri obegränsad och exakta larm.
4. Har telefonen en egen batterispärr (Samsung, Xiaomi, Huawei, OPPO, OnePlus, vivo) visar appen en knapp som öppnar rätt inställning och berättar vad du ska slå på.
5. Under ⚙ ställer du in tider, steg, bilder eller egna foton, skoldagar, nattläge och ljud.

## Hur säkra är påminnelserna?

- Rutinstart och läggdags går som **väckarklockelarm**, det Android prioriterar högst. En liten larmikon syns i statusraden. Det går att stänga av under "Extra säkra larm".
- Larmen planeras om när telefonen startar, när appen uppdateras, när tiden ändras och när appen öppnas. Dessutom körs ett **skyddsnät var tredje timme**.
- Under ⚙ → **Påminnelser just nu** ser du nästa planerade påminnelse och när det senaste larmet kom fram.
- Ärligt: om någon trycker **Tvinga stopp** på Min dag stänger Android av alla larm tills appen öppnas igen. Det går inte att komma runt.

## Familjeappen

Min dag fungerar bra bredvid Familjeappen. Läs [FAMILJEAPPEN.md](FAMILJEAPPEN.md).

## Bra att veta

Allt sparas bara i telefonen. Det finns inget konto och appen behöver inte internet. Egna foton sparas i appens privata lagring, och appen får ingen åtkomst till resten av bildgalleriet.
