# Min dag och Familjeappen

## Krockar de?

| Del i Familjeappen | Påverkan på Min dag | Status |
|---|---|---|
| Spärrskärm (tillgänglighet + "visa ovanpå") | Utan ändring spärras Min dag vid läggdags, och när skärmtiden är slut, precis som andra appar. | Kräver Familjeappen **0.8.1-mindag**: där räknas Min dag som en undantagen app. |
| Skärmtid (användningsstatistik) | Utan ändring räknas tiden i Min dag in i barnets skärmtid. | 0.8.1-mindag: räknas inte. |
| Nätlås / webbfilter (lokal VPN) | Ingen. Min dag använder aldrig internet. | Fungerar redan. I 0.8.1-mindag går Min dag dessutom helt utanför VPN:en. |
| Aviseringar | Familjeappen läser och tystar inte andra appars aviseringar. | Fungerar redan. |
| Tillgänglighet och överlägg | Min dag använder inget av dem, så de kan inte krocka. | Fungerar redan. |

Familjeappen godkänner Min dag först när paketnamnet `se.robgra.mindag` **och** signeringscertifikatet stämmer. En app som bara kallar sig Min dag kan alltså inte smita förbi.

## Valfri koppling: läggdags från Familjeappen

Min dag har en lokal, skyddad koppling som bara Familjeappen får använda. Familjeappen kontrolleras via paketnamnet `se.robgra.app` och Robins signeringscertifikat.

- **Familjeappen → Min dag:** läggdags och vakna för varje kväll. Min dag använder tiderna bara om du har slagit på **⚙ → Familjeappen → Låt Familjeappen styra läggdags** i Min dag. Orimliga tider ignoreras.
- **Min dag → Familjeappen:** status för pågående rutin, aktuellt steg, om rutinen är klar och om det är natt.

Allt sker inne i telefonen. Inget går över nätet.

Det som fungerar nu, med Min dag 2.1:

- Kopplingen och inställningen finns i Min dag.

Det som kräver Familjeappen 0.8.1-mindag (patchen `patches/mindag-compat.patch` i Familjeappens projekt):

- Undantaget från spärr, skärmtid och nätlås.
- Att läggdags skickas till Min dag när reglerna ändras och när telefonen startar om.
- En funktion som läser Min dags status. Den används inte ännu i Familjeappens gränssnitt.
