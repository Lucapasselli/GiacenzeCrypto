# Normativa — archivio dei documenti ufficiali sulla fiscalità delle cripto-attività

Raccolta dei testi **ufficiali** che governano il trattamento fiscale delle cripto-attività
in Italia: le leggi di bilancio dal 2023 al 2026 e le norme che richiamano, la prassi
dell'Agenzia delle entrate, le istruzioni di compilazione dei modelli dichiarativi e la
modulistica ISEE/DSU.

Serve a consultare le regole **senza dover andare a cercarle online**, su cinque siti
diversi, ogni volta che serve verificare un dettaglio.

**Nessun documento di questa cartella è commentato, riassunto o interpretato.** Tutto ciò
che è stato prodotto qui dentro è dichiarato come tale, in testa a ogni file e nella
colonna `tipo` di `fonti.csv`.

## Le due categorie di file, e perché la distinzione è la cosa più importante

| tipo | che cos'è | dove sta |
|---|---|---|
| **ufficiale** | scaricato tale e quale da chi lo pubblica: Normattiva, Agenzia delle entrate, Ministero del lavoro, Ufficio delle pubblicazioni dell'Unione europea | `Leggi/Originale/`, `Prassi_AgenziaEntrate/`, `Istruzioni_Dichiarazioni/`, `ISEE_DSU/` |
| **derivato** | prodotto dagli strumenti di `Strumenti/` a partire dai file ufficiali: estratti riformattati e testi accostati. **Non ha valore ufficiale** | `Leggi/Estratti/`, `Leggi/Consolidato/` |

Un derivato non contiene mai parole che non stiano già in un testo ufficiale: gli estratti
riformattano, i consolidati accostano. Ma un testo accostato **non è un testo promulgato**,
e in caso di dubbio fa fede Normattiva. Ogni file derivato lo dichiara nelle prime righe.

`fonti.csv` è l'indice di provenienza di **tutti** i 119 file dell'archivio (tutto
quello che sta nelle quattro cartelle di documenti; non se stesso, non il README, non gli
strumenti): chi lo pubblica, con quale
identificativo ufficiale, da quale indirizzo, quando, e l'impronta SHA-256. È quello che
rende verificabile la promessa "solo documenti ufficiali" — si può ricalcolare l'impronta e
riscaricare dall'indirizzo indicato.

## Com'è organizzato

```
Normativa/
├── README.md                      questo file
├── fonti.csv                      provenienza e impronta di ogni file
├── Leggi/
│   ├── Originale/                 testi ufficiali in formato Akoma Ntoso (XML)
│   │   ├── LeggiBilancio/         le quattro leggi di bilancio, testo originario e vigente
│   │   ├── NormeRichiamate/       le norme che le leggi di bilancio modificano o citano
│   │   └── NormeUnioneEuropea/    MiCA e DAC8, in PDF dalla Gazzetta ufficiale UE
│   ├── Estratti/                  le sole parti che riguardano le cripto-attività, in Markdown
│   └── Consolidato/               ogni articolo modificato, com'era e com'è diventato
├── Prassi_AgenziaEntrate/         circolari, risoluzioni, provvedimenti, risposte a interpello
├── Istruzioni_Dichiarazioni/      istruzioni Redditi PF (3 fascicoli) e 730, dal 2023 al 2026
├── ISEE_DSU/                      modello DSU, istruzioni e decreto che le approva
└── Strumenti/                     gli script che hanno scaricato e prodotto tutto questo
```

## Da dove cominciare

- **Che cosa dice la legge, oggi** → `Leggi/Consolidato/`. Un file per disposizione, con il
  testo dell'articolo a ogni data rilevante: prima e dopo ciascuna legge di bilancio.
- **Il testo di una legge di bilancio** → `Leggi/Estratti/LeggiBilancio/`.
- **Come si compila il quadro** → `Istruzioni_Dichiarazioni/`. Il **quadro RW** (monitoraggio
  e imposta sul valore delle cripto-attività) e il **quadro RT** (plusvalenze) stanno nel
  **fascicolo 2** di Redditi PF, non nel primo.
- **Come l'Agenzia interpreta una norma** → `Prassi_AgenziaEntrate/`. Il documento di
  riferimento resta la **circolare 30/E del 27 ottobre 2023**, 118 pagine.

## Mappa cronologica delle regole sulle cripto-attività

| quando | che cosa | dove |
|---|---|---|
| **L. 197/2022, art. 1, commi 126-147** — legge di bilancio 2023, in vigore dal 1° gennaio 2023 | introduce l'intera disciplina: lettera *c-sexies)* dell'art. 67 TUIR, comma 9-bis dell'art. 68, comma 3-bis dell'art. 110, quadro RW, imposta di bollo e imposta sul valore delle cripto-attività, rideterminazione del valore al 1° gennaio 2023 e regolarizzazione | `Leggi/Estratti/LeggiBilancio/LB2023_*` |
| **L. 213/2023** — legge di bilancio 2024 | **nessuna disposizione sulle cripto-attività** | `Leggi/Estratti/LeggiBilancio/LB2024_*_nessuna_disposizione_cripto.md` |
| **L. 207/2024, art. 1, commi 23-29** — legge di bilancio 2025 | aliquota generale al 26%; **33%** sui redditi della lettera *c-sexies)* realizzati dal 1° gennaio 2026; **soppressa la soglia di 2.000 euro**; rideterminazione del valore al 1° gennaio 2025 con imposta sostitutiva del 18% | `Leggi/Estratti/LeggiBilancio/LB2025_*` |
| **L. 199/2025, art. 1, comma 28** — legge di bilancio 2026 | **26% in luogo del 33%** per i *token di moneta elettronica denominati in euro*; la mera conversione euro/EMT non realizza plusvalenze né minusvalenze | `Leggi/Consolidato/LB2025_art1_comma_24_aliquota_evoluzione.md` |
| **L. 199/2025, art. 1, commi 32-34** | le giacenze in criptovalute entrano nel patrimonio mobiliare rilevante ai fini **ISEE** | `Leggi/Consolidato/DL201-2011_art5_ISEE_criptovalute_evoluzione.md` |

## Tre cose che è meglio sapere prima di usare l'archivio

**1. Le leggi di bilancio scrivono solo la variazione, mai il testo risultante.** «Dopo la
lettera c-quinquies) è inserita la seguente…» non dice come resta l'articolo. Il testo
integrato non sta in nessuno dei due atti. La cartella `Consolidato/` lo ricostruisce
**senza scrivere nulla**: accosta due testi ufficiali, la fotografia dell'articolo prima e
quella dopo, entrambe scaricate da Normattiva. L'unica parte derivata è l'accostamento.

**2. Normattiva non porta subito la modifica nel corpo dell'articolo.** Per qualche
settimana la tiene fra le modifiche annunciate nei metadati. È successo proprio all'art. 67
del TUIR: la fotografia al **1° gennaio 2023**, giorno di entrata in vigore, **non contiene
ancora la lettera c-sexies)**, che compare solo in una fotografia successiva (fra il 25
febbraio e il 1° marzo 2023). Per questo dell'articolo 67 sono conservate entrambe. Gli
articoli 68 e 110, e le altre norme modificate, risultano invece aggiornati già al
1° gennaio 2023. **Chi legge una fotografia "post" e non trova la modifica non deve
concludere che non ci sia**: deve guardare la fotografia successiva.

**3. Le cripto-attività non sono ancora nella DSU.** Il comma 32 della legge di bilancio
2026 modifica l'art. 5 del D.L. 201/2011, ma il comma 33 rinvia a un decreto ministeriale
che deve modificare l'art. 5 del D.P.C.M. 159/2013, e il comma 34 dà agli enti novanta
giorni dall'entrata in vigore di quel decreto. Alla data di questo scarico **né il
regolamento ISEE né le istruzioni alla DSU nominano le criptovalute**: il modello in
`ISEE_DSU/` è quello approvato con D.M. 2 marzo 2026, n. 3, e non le contiene.

## Rigenerare o aggiornare

Gli strumenti stanno in `Strumenti/`, sono in Python 3 senza dipendenze esterne, e ognuno è
guidato da un file JSON che elenca che cosa scaricare o produrre. L'ordine è questo:

```bash
cd Normativa/Strumenti
python3 scarica_normattiva.py          # leggi e norme richiamate, da Normattiva (atti.json)
python3 scarica_documenti.py elenco_prassi.json       # prassi dell'Agenzia delle entrate
python3 scarica_documenti.py elenco_istruzioni.json   # istruzioni Redditi PF e 730
python3 scarica_documenti.py elenco_isee_dsu.json     # modello e istruzioni DSU
python3 scarica_documenti.py elenco_norme_ue.json     # MiCA e DAC8
python3 genera_estratti.py             # Leggi/Estratti   (estratti.json)
python3 consolida.py                   # Leggi/Consolidato (consolidato.json)
python3 genera_fonti.py                # fonti.csv — sempre per ultimo
```

Due strumenti servono a **trovare** che cosa scaricare, non a scaricarlo, e vanno rilanciati
quando si cerca materiale nuovo:

- `prassi_ade_indice.py` percorre gli indici ufficiali di circolari, risoluzioni, risposte
  agli interpelli, principi di diritto e risposte a consulenza giuridica, anno per anno, e
  segnala quelli che parlano di cripto-attività. Sull'ultima esecuzione ha esaminato **1640
  documenti dal 2023 al 2026** e ne ha trovati 8 pertinenti, tutti presenti in
  `Prassi_AgenziaEntrate/`. Scrive un elenco di candidati
  da rivedere a mano: il filtro per parole chiave è un aiuto, non un giudizio — la risposta
  n. 78/2025, per dire, ha per oggetto «l'articolo 110, comma 3-bis, del TUIR» e la parola
  "cripto" nel titolo non ce l'ha.
- `istruzioni_ade_indice.py` ricostruisce gli indirizzi delle istruzioni dei modelli, che
  cambiano a ogni ristampa e non seguono uno schema stabile fra un anno e l'altro.

`genera_fonti.py` segnala con `SCONOSCIUTO` qualunque file presente sul disco ma non
descritto da nessuna configurazione: è un errore da correggere, non una riga da ignorare.

## Che cosa NON c'è, e perché

- **I PDF della Gazzetta Ufficiale delle leggi di bilancio.** Sono pubblicate in Supplemento
  Ordinario insieme a tutte le tabelle del bilancio dello Stato: il PDF del solo supplemento
  che contiene la legge di bilancio 2023 pesa **207 MB**. I testi qui conservati vengono da
  **Normattiva**, che è il portale ufficiale dello Stato per la consultazione della
  normativa vigente, in formato Akoma Ntoso — lo stesso testo, strutturato e cercabile.
- **La bozza in consultazione della circolare 30/E** (giugno 2023). Esiste ed è sul sito
  dell'Agenzia, ma è una bozza: in un archivio di consultazione diventerebbe silenziosamente
  "la regola".
- **Le versioni precedenti delle istruzioni alla DSU.** Il Ministero del lavoro le pubblica a
  indirizzo fisso e le sostituisce a ogni nuovo modello: le vecchie non sono più raggiungibili.
- **La tariffa allegata al D.P.R. 642/1972** in forma leggibile. I commi 144 e 145 della legge
  di bilancio 2023 modificano l'art. 13 della *tariffa*, non l'art. 13 del corpo del decreto;
  Normattiva rende la tariffa come tabella di testo a larghezza fissa, di fatto illeggibile.
  L'estratto conserva l'articolo del corpo, e la nota lo dice.
