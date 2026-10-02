---
layout: default
title: Configurazione JSON per le importazioni personalizzate
---

# Configurazione JSON per le importazioni personalizzate

Questa guida spiega come creare un file JSON per importare qualunque file CSV nella raccolta dei movimenti crypto. Il JSON descrive:

- dove si trovano i dati nel CSV;

- come interpretarli (date, segni, causali);

- come trasformarli (rinomina monete, pulizia nomi, wallet).

Ogni proprietà ha un valore predefinito. Se non la inserisci nel JSON, viene usato il default indicato.

## Struttura generale {#struttura-generale}

```json
{
"nomeExchange": "...",
"fontePrezzoPreferita": "",
"fornitore": "...",
"estrazione": "...",
"descrizione": "",
"versione": "",
"versioneMinimaApp": "",
"nomeWallet": "...",
"testing": false,
"separatore": ",",
"separatoreValoreMoneta": "",
"coppia": { ... },
"encoding": "UTF-8",
"righeIntestazione": 1,
"rigaIntestazione": 1,
"autoDetectColonne": false,
"mappaAutoDetect": { ... },
"formatoData": "yyyy-MM-dd HH:mm:ss",
"fuso": "UTC",
"consolidaRigheStessaData": false,
"tolleranzaSecondiConsolidamento": 2,
"causaliDifferite": [],
"causaliConsolidaPerGiorno": [],
"minutiScambioDifferito": 15,
"colonne": { ... },
"raggruppamentoPerCausale": { ... },
"colonneControvalore": { ... },
"mappaCausali": { ... },
"causalePerNota": [ ... ],
"causaliChiuse": [ ... ],
"causaliUscita": [ ... ],
"causaliEntrata": [ ... ],
"versoForzato": "",
"causaliScambiaGambe": [ ... ],
"gambaDoppiaConSegnoSuUscita": false,
"ricostruisciLordoSeFeeSuMonetaUscita": false,
"ricostruisciLordoSeFeeSuMonetaEntrata": false,
"rimuoviCaseSensitive": false,
"rimuoviDaNomeMoneta": [ ... ],
"rinominaMonete": { ... },
"walletPerCausale": { ... },
"walletSpecularePerCausale": { ... },
"girocontoFiat": { ... },
"causaliDerivati": { ... },
"causaliAllertaDerivati": [ ... ],
"campiExtra": { ... },
"centralizzato": false,
"separatoreCausale": ".",
"causaliUppercase": false
}
```

## 1. Intestazione {#1-intestazione}

### `nomeExchange` {#nomeexchange}

**Tipo:** stringa | **Default:** "" (stringa vuota)

Nome dell'exchange o della piattaforma sorgente. Se lasciato vuoto, il programma chiede interattivamente il nome in fase di importazione.

```json
"nomeExchange": "Binance"
```

⚠️ **Non cambiarlo dopo aver importato dei movimenti.** Il nome finisce nel campo Exchange del movimento
e il programma lo usa in due modi. Per riconoscere i duplicati quando reimporti lo stesso file, e per
decidere a quale gruppo wallet appartiene il movimento nel calcolo fiscale. Se lo modifichi, i movimenti
già in archivio non vengono più riconosciuti, quindi un nuovo import li duplica, e passano a un altro
gruppo wallet, a meno che tu non li riunisca con un alias. `nomeWallet` invece non ha effetti fiscali e può
essere cambiato liberamente.

### `fontePrezzoPreferita` {#fonteprezzopreferita}

**Tipo:** stringa | **Default:** "" (nessuna preferenza)

Exchange da preferire, fra quelli già scaricati, quando si cerca il prezzo di un movimento importato da
questa configurazione — lo stesso id usato da CCXT, minuscolo (`"binance"`, `"okx"`, `"cryptocom"`,
`"bybit"`, `"coinbase"`, `"bitstamp"`, `"kucoin"`). Non limita da chi si scarica: tutti gli exchange
configurati vengono comunque interrogati al momento del download dei prezzi; questo campo sceglie solo
quale dei prezzi già in cache usare quando più di uno è disponibile per lo stesso movimento. Si applica
solo ai movimenti che non hanno già un prezzo o un controvalore nel CSV stesso (colonne `prezzo` e
`valoreEuro` entrambe assenti/vuote), e solo al momento dell'import — le rivalorizzazioni fiscali
successive non lo consultano.

```json
"fontePrezzoPreferita": "binance"
```

### `nomeWallet` {#nomewallet}

**Tipo:** stringa | **Default:** "Principale"

Nome del wallet di destinazione. Può essere sovrascritto causale per causale tramite walletPerCausale.

```json
"nomeWallet": "Spot"
```

### `testing` {#testing}

**Tipo:** booleano | **Default:** false

Se true, segnala in fase di importazione che il file di configurazione è in fase di test e l’importazione potrebbe non essere affidabile.

```json
"testing": true
```

### `fornitore` e `estrazione` {#fornitore-e-estrazione}

**Tipo:** stringa | **Default:** "" (stringa vuota)

Decidono **dove compare la configurazione** nelle due tendine della finestra di importazione:
`fornitore` è la voce della prima tendina (l'exchange o il servizio), `estrazione` quella della seconda
(quale export di quel fornitore si sta caricando).

Vanno indicati solo quando non si ricavano da soli: per un exchange basta `nomeExchange`, e per i
formati che contengono i movimenti di piattaforme diverse (CoinTracking, Tatax) basta la parola nel nome
del file. Se mancano entrambi si usa il nome del file di configurazione. `estrazione` va scritta **senza
ripetere** il nome del fornitore, che è già nella prima tendina.

```json
"fornitore": "OKX",
"estrazione": "Funding"
```

### `descrizione` {#descrizione}

**Tipo:** stringa | **Default:** "" (nessun suggerimento)

Testo libero che descrive la configurazione: da dove si scarica il file, cosa copre, quali sono i limiti
noti. Non cambia in nessun modo l'importazione. Viene mostrato come suggerimento (tooltip) quando passi il
mouse sulla voce nella finestra di importazione. Per andare a capo e limitare la larghezza conviene
scriverlo in HTML, come fanno le configurazioni ufficiali.

```json
"descrizione": "<html><div style='width:380px'>Export CSV dei movimenti di conto. Gli interessi giornalieri sono sommati in un solo movimento.</div></html>"
```

### `versione` {#versione}

**Tipo:** stringa | **Default:** ""

Numero di revisione del file (per esempio `"1.002"`). Il programma non lo usa per importare: serve a chi
mantiene la configurazione per capire quale revisione è installata, e conviene aumentarlo a ogni modifica
se condividi il file con altri.

### `versioneMinimaApp` {#versioneminimaapp}

**Tipo:** stringa | **Default:** "" (nessun vincolo)

Versione minima del programma che sa interpretare questa configurazione, per esempio `"1.0.64.06"`. Va
indicata quando il file usa una funzione del formato aggiunta di recente (tra queste `separatoreValoreMoneta`,
`causaliScambiaGambe`, `versoForzato`). Un'installazione più vecchia non propone la configurazione nella
finestra di importazione, invece di proporla e poi leggerla male senza accorgersene. Se il vincolo non serve,
lascia il campo vuoto.

```json
"versioneMinimaApp": "1.0.64.06"
```

## 2. Struttura fisica del CSV {#2-struttura-fisica-del-csv}

### `separatore` {#separatore}

**Tipo:** stringa | **Default:** ","

Carattere che divide le colonne nel CSV.

| Valore | Quando usarlo |
|---|---|
| "," | Standard internazionale, default. |
| ";" | Export da Excel con impostazioni europee. |
| "\t" | File TSV (tab-separated). |
| "\\|" | Caso raro, usato da alcuni exchange. |

```json
"separatore": ";"
```

### `encoding` {#encoding}

**Tipo:** stringa | **Default:** "UTF-8"

Usa "UTF-8" nella maggior parte dei casi. Se i caratteri speciali risultano corrotti, prova "ISO-8859-1" oppure "Windows-1252".

### `righeIntestazione` {#righeintestazione}

**Tipo:** numero intero | **Default:** 1

Quante righe iniziali del CSV vanno saltate prima dei dati. Se il CSV non ha intestazione, imposta 0.

### `rigaIntestazione` {#rigaintestazione}

**Tipo:** numero intero | **Default:** 1

Quale riga (indice 1-based) contiene i nomi delle colonne. Usata solo se autoDetectColonne è true.

### `autoDetectColonne` {#autodetectcolonne}

**Tipo:** booleano | **Default:** false

Se true, legge i nomi delle colonne dalla riga di intestazione e assegna automaticamente gli indici tramite mappaAutoDetect.

### `mappaAutoDetect` {#mappaautodetect}

**Tipo:** oggetto chiave-valore

Mappa il nome dell'intestazione CSV al nome del campo logico. Valori validi: data, causale, moneta, quantita, segno, valoreEuro, monetaFee, quantitaFee, idTransazione, idGruppo, wallet. (`prezzo`, `monetaUscita`, `quantitaUscita`, `causale2`, `causale3` non sono riconosciuti dall'auto-detect: vanno indicati a mano in `colonne` se servono.)

```json
"autoDetectColonne": true,
"mappaAutoDetect": {
"Timestamp": "data",
"Type": "causale",
"Asset": "moneta",
"Amount": "quantita",
"Fee Amount": "quantitaFee",
"Fee Asset": "monetaFee",
"TxID": "idTransazione"
}
```

### `formatoData` {#formatodata}

**Tipo:** stringa | **Default:** "yyyy-MM-dd HH:mm:ss"

Formato della data/ora nel CSV, con i token Java di DateTimeFormatter.

| Token | Significato | Esempio |
|---|---|---|
| yyyy | Anno a 4 cifre | 2025 |
| yy | Anno a 2 cifre | 25 |
| MM | Mese a 2 cifre | 06 |
| dd | Giorno a 2 cifre | 17 |
| HH | Ora 0-23 | 18 |
| mm | Minuti | 39 |
| ss | Secondi | 10 |

```json
"formatoData": "dd.MM.yyyy HH:mm:ss"
"formatoData": "MM/dd/yy HH:mm"
```

### `fuso` {#fuso}

**Tipo:** stringa | **Default:** "UTC"

Fuso orario delle date nel CSV. Vengono convertite nel fuso locale dell'app.

```json
"fuso": "UTC"
"fuso": "Europe/Rome"
"fuso": "UTC+2"
```

## 3. Colonne del CSV {#3-colonne-del-csv}

Mappa i campi logici agli indici numerici delle colonne del CSV, contando da 0. Usa -1 per indicare che una colonna non è presente.

```json
"colonne": {
"data": 0,
"causale": 1,
"moneta": 2,
"quantita": 3,
"segno": -1,
"valoreEuro": -1,
"prezzo": -1,
"monetaFee": -1,
"quantitaFee": -1,
"idTransazione": -1,
"wallet": -1,
"monetaUscita": -1,
"quantitaUscita":-1,
"note": -1,
"causale2": -1,
"causale3": -1
}
```

### Come trovare l'indice di una colonna {#come-trovare-lindice-di-una-colonna}

Apri il CSV, guarda la riga di intestazione e conta le colonne partendo da 0.

```text
"Timestamp","Tipo","Asset","Quantita","Valore EUR"
0 1 2 3 4
```

Quindi: "data": 0, "causale": 1, "moneta": 2 e così via.

### Campi disponibili {#campi-disponibili}

| Campo | Descrizione |
|---|---|
| data | Obbligatorio - data e ora del movimento. |
| causale | Tipo di operazione nel CSV (Deposit, Trade, Staking...). |
| moneta | Simbolo del token (BTC, ETH...). |
| quantita | Quantità del movimento (negativa=uscita, positiva=entrata). |
| segno | Colonna separata che indica il segno (+ o -). |
| valoreEuro | Controvalore in euro già calcolato nel CSV. |
| prezzo | Prezzo unitario del token al momento dell'operazione. |
| monetaFee | Simbolo della moneta usata per la commissione. |
| quantitaFee | Quantità della commissione pagata. |
| idTransazione | ID univoco per raggruppare righe correlate dello stesso trade. |
| wallet | Nome del wallet, se variabile riga per riga. |
| monetaUscita | Moneta in uscita (CSV con entrata e uscita sulla stessa riga). |
| quantitaUscita | Quantita in uscita (CSV con entrata e uscita sulla stessa riga). |
| note | Colonna di testo libero da riportare nel campo Note del movimento. Serve anche a `causalePerNota` (vedi sezione 4). |

### Importo e moneta nella stessa cella {#importo-e-moneta-nella-stessa-cella}

**Tipo:** stringa | **Default:** "" (importo e moneta sono due colonne distinte)

Alcuni export scrivono importo e simbolo insieme, per esempio `407.57 AME`. In questo caso indica lo
stesso indice di colonna sia per `quantita` sia per `moneta` (e, se serve, per `quantitaUscita`/`monetaUscita`
e `quantitaFee`/`monetaFee`), e in `separatoreValoreMoneta` il carattere che divide i due pezzi. Quando le
due colonne coincidono il programma separa il numero dal simbolo usando quel carattere.

```json
"separatoreValoreMoneta": " ",
"colonne": { "quantita": 5, "moneta": 5, "quantitaFee": 6, "monetaFee": 6 }
```

### Coppia di trading in una sola cella {#coppia}

**Tipo:** oggetto `{ colonna, separatore, base, quote }` | **Default:** assente

Alcuni export di trading non hanno colonne per le monete e riportano solo la coppia, per esempio `KCS-ETH`.
Il blocco `coppia` dice dove si trova la coppia e come dividerla. Il programma aggiunge a ogni riga due
**colonne virtuali** alle posizioni `base` e `quote`, che poi usi nelle `colonne` come una qualsiasi altra
colonna (di solito `base` come `moneta` e `quote` come `monetaUscita`).

| Campo | Descrizione |
|---|---|
| `colonna` | Indice della colonna che contiene la coppia. |
| `separatore` | Carattere che divide base e quota. Default `-`. Non può essere vuoto. |
| `base` | Indice della prima colonna virtuale. Scegli un numero **oltre l'ultima colonna vera** del CSV. |
| `quote` | Indice della seconda colonna virtuale, stessa regola. |

Una riga la cui cella non contiene il separatore viene scartata e segnalata come `COPPIA NON VALIDA`.

```json
"coppia": { "colonna": 3, "separatore": "-", "base": 16, "quote": 17 },
"colonne": { "moneta": 16, "quantita": 7, "monetaUscita": 17, "quantitaUscita": 8 },
"causaliScambiaGambe": ["SELL"]
```

L'esempio è quello di KuCoin: la coppia sta nella colonna 3, le colonne virtuali 16 e 17 vengono dopo
l'ultima colonna vera, e le vendite (`SELL`) hanno i ruoli di entrata e uscita invertiti rispetto agli
acquisti (vedi `causaliScambiaGambe`).

### Causale composita {#causale-composita}

Alcuni CSV distribuiscono il tipo di operazione su più colonne. Si possono combinare fino a 3 colonne in una causale composita concatenate da separatoreCausale.

| Parametro | Default | Descrizione |
|---|---|---|
| causale2 (in colonne) | -1 | Indice della seconda colonna causale. |
| causale3 (in colonne) | -1 | Indice della terza colonna causale. |
| separatoreCausale | "." | Carattere di giunzione tra le parti. |
| causaliUppercase | false | Se true converte ogni parte in maiuscolo prima di concatenare. |

Esempio: causale=1 ("Trade"), causale2=2 ("Buy"), separatoreCausale="." produce la chiave "Trade.Buy" da usare in mappaCausali.

```json
"colonne": { "causale": 1, "causale2": 2 },
"separatoreCausale": ".",
"mappaCausali": { "Trade.Buy": "SCAMBIO CRYPTO-CRYPTO" }
```

### Entrata e uscita sulla stessa riga {#entrata-e-uscita-sulla-stessa-riga}

Alcuni CSV (es. Koinly, CoinTracking) riportano l'intero scambio su una sola riga. In quel caso moneta/quantita = lato entrata, monetaUscita/quantitaUscita = lato uscita.

```json
"colonne": {
"data": 8, "causale": 0,
"moneta": 2, "quantita": 1,
"monetaUscita": 4, "quantitaUscita": 3,
"monetaFee": 6, "quantitaFee": 5
}
```

### `gambaDoppiaConSegnoSuUscita` {#gambadoppiaconsegnosuuscita}

**Tipo:** booleano | **Default:** false

Per gli export (tipico Nexo) in cui **ogni** riga ha due colonne moneta, ma solo quando le monete sono
diverse si tratta di uno scambio. Quando la moneta di uscita coincide con quella principale (interessi,
versamenti, prelievi...) la riga è un movimento a gamba singola e il verso sta nel segno della colonna di
uscita. Con `true` il programma applica questa regola, altrimenti un accredito di interessi `NEXO → NEXO`
diventerebbe un finto scambio. Se l'importo principale è zero si usa quello della colonna di uscita.

### `causaliScambiaGambe` {#causaliscambiagambe}

**Tipo:** array di causali CSV originali | **Default:** [] (nessuna)

Per le righe che portano già entrambe le gambe, scambia il ruolo di entrata e uscita delle due colonne prima
di ogni altra elaborazione. Serve quando l'export usa colonne dal ruolo fisso (per esempio "Deal amount" e
"Total") ma la direzione reale dipende dalla causale: con `Buy` entra "Deal amount" ed esce "Total", con
`Sell` accade il contrario. Elenca qui le causali per cui le colonne vanno invertite. Se moneta e quantità
condividono la stessa cella (`separatoreValoreMoneta`) simbolo e importo si scambiano insieme.

```json
"causaliScambiaGambe": ["Sell"]
```

### `idGruppo` (nella sezione colonne) {#idgruppo-nella-sezione-colonne}

**Tipo:** numero intero | **Default:** -1 (assente)

Colonna su cui raggruppare le righe **quando non coincide con quella dell'identificativo**. I due ruoli
sono distinti: `idTransazione` dice *chi è* la riga — finisce nel movimento ed è ciò su cui lavora il
controllo dei duplicati — mentre `idGruppo` dice *con chi sta*.

Nell'export di trading di OKX, ad esempio, ogni gamba e ogni esecuzione parziale hanno un `id` diverso ma
condividono l'`Order id`: usare il primo anche per raggruppare spezzerebbe ogni scambio in gambe isolate.
Quando `idGruppo` non è indicata si raggruppa come sempre sull'identificativo, quindi le configurazioni
che non la usano non cambiano comportamento.

Quando `idGruppo` è presente, il file viene inoltre **ordinato** per quel campo (e poi per
`idTransazione`) prima dell'importazione, perché il raggruppamento lavora su righe consecutive e gli
export intercalano le gambe di ordini diversi.

```json
"colonne": { "idTransazione": 0, "idGruppo": 1 }
```

### `raggruppamentoPerCausale` {#raggruppamentopercausale}

**Tipo:** oggetto causale CSV → indice colonna | **Default:** {} (nessun override)

Sovrascrive la colonna di raggruppamento **solo per le causali elencate**. Serve quando le gambe di uno
stesso movimento condividono una colonna diversa da `idGruppo`/`idTransazione` e i loro timestamp non
sono identici.

Caso reale (Coinbase): le due gambe di un `Convert` hanno la stessa identica stringa nella colonna
`note` (`"Converted 22 ALEPH to 4.68065032 ZETA"`) ma sull'export a volte cadono a 1-2 secondi di
distanza; raggruppando sul timestamp esatto verrebbero spezzate in un deposito e un prelievo separati.
Raggruppando sulla colonna `note` (indice 10) si ricompongono. Va abbinato a `consolidaRigheStessaData`
+ `causaliDifferite` + una `tolleranzaSecondiConsolidamento` sufficiente. Una gamba con valore diverso
in quella colonna apre comunque un gruppo nuovo, quindi movimenti distinti non si mescolano.

```json
"raggruppamentoPerCausale": { "Convert": 10 },
"consolidaRigheStessaData": true,
"tolleranzaSecondiConsolidamento": 60,
"causaliDifferite": ["Convert"]
```

## 4. Mappatura delle causali {#4-mappatura-delle-causali}

### `mappaCausali` {#mappacausali}

**Tipo:** oggetto chiave-valore

Traduce le causali originali del CSV nelle tipologie interne dell'applicazione. Ogni causale deve avere una voce qui, altrimenti la riga viene scartata. La ricerca è case insensitive per default.

```json
"mappaCausali": {
"Deposit": "TRASFERIMENTO-CRYPTO",
"Withdrawal": "TRASFERIMENTO-CRYPTO",
"Trade": "SCAMBIO CRYPTO-CRYPTO",
"Staking Rewards": "STAKING REWARDS",
"Commission": "COMMISSIONI",
"earn":"EARN",
"cashback":"CASHBACK"
}
```

Per ignorare righe senza contarle come scarti, mappa su "IGNORA" (vale anche "NON CONSIDERARE"):

```json
"mappaCausali": { "Internal Transfer": "IGNORA" }
```

**Tipologie interne che puoi usare come valore:**

| Valore | Quando usarlo |
|---|---|
| `SCAMBIO CRYPTO-CRYPTO` | Scambio fra due monete. Anche `DUST-CONVERSION` per la conversione dei saldi minimi. |
| `ACQUISTO CRYPTO` / `VENDITA CRYPTO` | Acquisto o vendita di crypto contro valuta FIAT. |
| `DEPOSITO FIAT` / `PRELIEVO FIAT` | Versamento o prelievo di valuta FIAT. |
| `TRASFERIMENTO-CRYPTO` | Deposito o prelievo di crypto. Il verso lo decide il segno della quantità, per questo **non esistono** tipologie separate per deposito e prelievo. |
| `TRASFERIMENTO-CRYPTO-INTERNO` | Spostamento fra comparti dello stesso exchange. Non genera plusvalenze. |
| `SCAMBIO DIFFERITO` | Metà di uno scambio che compare come due righe indipendenti (vedi sezione 6). |
| `REWARD`, `ALTRE-REWARD`, `EARN`, `STAKING REWARDS`, `CASHBACK`, `AIRDROP` | Ricompense di vario tipo. |
| `COMMISSIONI` | Commissione pagata come movimento a sé. |
| `IGNORA` | Riga da saltare senza segnalarla come scartata. |

Una causale non presente in `mappaCausali` (e non coperta da `causalePerNota`) fa scartare la riga, che
compare fra i movimenti sconosciuti del resoconto con il testo `CAUSALE SCONOSCIUTA`. Per altri esempi apri
le configurazioni ufficiali nella cartella `config/import/`.

### `causalePerNota` {#causalepernota}

**Tipo:** array di regole `{ causale, notaContiene, tipo }` | **Default:** [] (nessuna regola)

Riclassifica una riga in base al **testo libero della colonna `note`**, quando la causale del CSV da
sola non basta a distinguere la natura del movimento. Richiede che `colonne.note` sia impostata.

Ogni regola:

| Campo | Obbligatorio | Descrizione |
|---|---|---|
| `causale` | no | Causale CSV che la riga deve avere. Se omessa, la regola vale per qualsiasi causale. |
| `notaContiene` | sì | Sottostringa cercata nella colonna `note` (confronto **case-insensitive**). |
| `tipo` | sì | Tipologia interna da assegnare (le stesse usabili in `mappaCausali`: `EARN`, `REWARD`, `AIRDROP`, `STAKING REWARDS`, `COMMISSIONI`, ...). |

Le regole si valutano **in ordine: vince la prima che combacia**. Se nessuna regola combacia si usa la
normale `mappaCausali`.

Caso reale (Coinbase): i premi arrivano tutti come causale `Receive` e si riconoscono solo dalla nota.

```json
"colonne": { "note": 10 },
"causalePerNota": [
  { "causale": "Receive", "notaContiene": "from Coinbase Earn",     "tipo": "EARN" },
  { "causale": "Receive", "notaContiene": "from Coinbase Referral", "tipo": "REWARD" },
  { "causale": "Receive", "notaContiene": "airdrop",                "tipo": "AIRDROP" }
]
```

### `causaliChiuse` {#causalichiuse}

**Tipo:** array di stringhe (tipologie interne)

Tipologie interne che devono essere trattate come movimenti singoli anche se condividono l'ID transazione con altre righe. Usa per staking, cashback, commissioni, depositi e prelievi. Si indica la **tipologia interna** (il valore di `mappaCausali`), non la causale del CSV. In vecchie configurazioni la chiave si chiama `movimentoChiuso`, che il programma accetta ancora come sinonimo.

```json
"causaliChiuse": [
"TRASFERIMENTO-CRYPTO",
"STAKING REWARDS","CASHBACK","COMMISSIONI"
]
```

## 5. Gestione del segno {#5-gestione-del-segno}

Per default il segno viene letto dalla quantità (negativa = uscita, positiva = entrata). Se la quantità è sempre positiva e il verso si deduce dalla causale, usa causaliUscita e causaliEntrata.

### `causaliUscita` {#causaliuscita}

**Tipo:** array di causali CSV originali

Le righe con queste causali avranno la quantità forzata negativa.

```json
"causaliUscita": ["Withdrawal","Trade Sell","Fee"]
```

### `causaliEntrata` {#causalientrata}

**Tipo:** array di causali CSV originali

Le righe con queste causali avranno la quantità forzata positiva.

```json
"causaliEntrata": ["Deposit","Trade Buy","Staking Rewards","Cashback"]
```

### `versoForzato` {#versoforzato}

**Tipo:** stringa `"ENTRATA"` o `"USCITA"` | **Default:** "" (nessuna forzatura)

Forza il verso di **ogni** riga del file, qualunque sia la causale. Serve per gli export divisi per
direzione, come i file dei soli depositi o dei soli prelievi di Gate.io, in cui non c'è nessuna colonna
causale affidabile da cui dedurre il verso. A differenza di `causaliUscita`/`causaliEntrata` non dipende dal
testo della causale. Qualunque altro valore viene ignorato.

```json
"versoForzato": "ENTRATA"
```

**Esempio pratico**

```json
"Data","Tipo","Moneta","Quantita"
"2025-01-15","Deposito","BTC","0.5"
"2025-01-16","Prelievo","ETH","1.2"
"causaliUscita": ["Prelievo"],
"causaliEntrata": ["Deposito","Staking","Cashback"]
```

## 6. Consolidamento righe correlate {#6-consolidamento-righe-correlate}

Alcuni exchange suddividono una singola operazione in più righe CSV (es. riga per moneta venduta + riga per moneta acquistata). Le opzioni seguenti permettono di raggrupparle.

### `consolidaRigheStessaData` {#consolidarighestessadata}

**Tipo:** booleano | **Default:** false

- false: ogni riga è un movimento indipendente.

- true: le righe con stesso ID transazione o timestamp vicino vengono raggruppate.

```json
"consolidaRigheStessaData": true
```

### `tolleranzaSecondiConsolidamento` {#tolleranzasecondiconsolidamento}

**Tipo:** numero intero | **Default:** 2

Usato solo se `consolidaRigheStessaData` è true. Quanti secondi di differenza sono tollerati tra due righe perché vengano considerate parte della stessa operazione.

```json
"tolleranzaSecondiConsolidamento": 5
```

### `causaliDifferite` {#causalidifferite}

**Tipo:** array di causali CSV originali

Se specificato, la tolleranza temporale viene applicata solo ai gruppi che contengono almeno una riga con queste causali; per le altre viene richiesto timestamp identico.

```json
"causaliDifferite": ["Trade","Swap"]
```

### `causaliConsolidaPerGiorno` {#causaliconsolidapergiorno}

**Tipo:** array di causali CSV originali | **Default:** [] (nessuna)

Somma le righe con queste causali in **un solo movimento per giorno, moneta e causale**. È pensato per gli
export che accreditano micro-interessi molte volte al giorno (un export Bitget ne conteneva 28.000): senza
la somma l'archivio si riempie di decine di migliaia di movimenti minuscoli. Il movimento risultante prende
la data del primo accredito del giorno e le altre colonne dalla prima riga, con la sola quantità sostituita
dalla somma. La commissione della prima riga viene azzerata, perché non va attribuita all'intero giorno.
Il giorno è quello del fuso del tuo sistema. La somma avviene prima del raggruppamento delle righe.

```json
"causaliConsolidaPerGiorno": ["Interest"]
```

### La causale `SCAMBIO DIFFERITO` e `minutiScambioDifferito` {#scambio-differito}

Alcune operazioni (l'Auto-Invest di Binance, un recupero fondi, una redistribuzione di token) mostrano nel CSV due righe indipendenti — un prelievo e un deposito, spesso di monete diverse — che in realtà sono le due metà dello stesso scambio, avvenuto "dietro le quinte" dell'exchange. Mappando la causale su `SCAMBIO DIFFERITO` (invece che su `TRASFERIMENTO-CRYPTO`) quella riga entra anche nella ricerca automatica di abbinamento a fine import: se, fra tutte le righe `SCAMBIO DIFFERITO` di questa importazione, un prelievo trova un deposito entro `minutiScambioDifferito` minuti e con un controvalore che non si scosta di oltre il 10%, i due movimenti vengono trasformati in un vero scambio (trasferimento verso una piattaforma fittizia, scambio, trasferimento di ritorno) invece di restare due movimenti scollegati.

`SCAMBIO DIFFERITO` va sempre aggiunta anche a `causaliChiuse` (è un movimento a gamba singola, come `TRASFERIMENTO-CRYPTO`).

**Tipo:** numero intero (minuti) | **Default:** 15

```json
"minutiScambioDifferito": 15
```

Il valore di default (15 minuti) è quello storicamente usato per Binance; un altro exchange, con tempi di regolamento diversi, può richiederne uno diverso.

### `idTransazione` (nella sezione colonne) {#idtransazione-nella-sezione-colonne}

Se il CSV ha una colonna con un ID univoco per transazione, indicarla consente di raggruppare righe anche se i timestamp differiscono oltre la tolleranza.

```json
"colonne": { "idTransazione": 5 }
```

## 7. Gestione commissioni {#7-gestione-commissioni}

### `ricostruisciLordoSeFeeSuMonetaUscita` {#ricostruiscilordosefeesumonetauscita}

**Tipo:** booleano | **Default:** false

Controlla se, quando la commissione è nella stessa moneta dell'uscita, il sistema deve ricostruire il lordo del movimento principale.

- true: ricostruisce il lordo e genera anche il movimento commissione separato.

- false: non modifica il movimento principale, genera solo il movimento commissione.

Esempio: uscita -500 USDC e fee 1 USDC. Con true: movimento principale -499 USDC + movimento commissione -1 USDC. Usalo quando la quantità nel CSV è già comprensiva della fee (riga unica).

```json
"ricostruisciLordoSeFeeSuMonetaUscita": true
```

### `ricostruisciLordoSeFeeSuMonetaEntrata` {#ricostruiscilordosefeesumonetaentrata}

**Tipo:** booleano | **Default:** false

Controlla se, quando la commissione è nella stessa moneta dell'entrata, il sistema deve ricostruire il lordo del movimento principale.

- true: ricostruisce il lordo e genera anche il movimento commissione separato.

- false: non modifica il movimento principale, genera solo il movimento commissione.

Esempio: entrata 500 USDC e fee 1 USDC. Con true: movimento principale 501 USDC + movimento commissione -1 USDC. Usalo quando la quantità nel CSV è al netto della fee (riga unica).

```json
"ricostruisciLordoSeFeeSuMonetaEntrata": true
```

### `colonneControvalore` {#colonnecontrovalore}

**Tipo:** oggetto | **Default:** assente

Per gli export in cui la colonna del totale **comprende sia la commissione sia lo spread** (tipico di
Coinbase retail: `Total (inclusive of fees and/or spread)`). Per le cripto-attività la **commissione
non è deducibile** dal costo di carico (art. 68 c. 9-bis TUIR), ma lo **spread sì** — non è una
commissione. Con questo blocco, per le sole causali indicate, il controvalore usato come costo di
carico diventa **`totale − commissione`** (lo spread resta dentro), invece della colonna `valoreEuro`.

| Campo | Descrizione |
|---|---|
| `totale` | Indice colonna del totale comprensivo di commissione e spread. |
| `commissione` | Indice colonna della commissione. Conta solo se è un importo **positivo** (alcune righe dust contengono un rapporto di spread con segno: viene ignorato). |
| `valuta` | Indice colonna con la valuta di `totale`/`commissione` (es. la colonna "Price Currency" = `EUR`). Usata per la gamba FIAT sintetica e per il movimento commissione. |
| `causali` | Causali CSV per cui applicare il ricalcolo `totale − commissione`. |
| `causaliConMovimentoCommissione` | Sottoinsieme di `causali`: righe che portano la **sola gamba crypto in entrata**. Per queste viene **sintetizzata la gamba FIAT in uscita** di importo `totale − commissione` (l'operazione diventa un vero acquisto FIAT→crypto invece di un semplice deposito) e la commissione esce come **movimento `COMMISSIONI` a sé** nella valuta indicata. |
| `noteAcquistoDaSaldo` | Elenco di testi (confronto senza distinzione fra maiuscole e minuscole) che, nella colonna `note`, indicano un acquisto pagato **dal saldo** dell'exchange (per Coinbase `"EUR Wallet"`). Vale solo per le causali di `causaliConMovimentoCommissione`. Se la nota non è vuota e **non** contiene nessuno di questi testi, l'acquisto è stato pagato con uno strumento esterno al saldo (carta, Apple Pay, Google Pay, bonifico diretto): in quel caso non si sintetizzano né la gamba FIAT né il movimento commissione, e resta il solo deposito crypto, caricato a costo pieno (`totale − commissione`) senza muovere euro. Nota vuota oppure elenco vuoto: comportamento normale. |

Per le causali in `causali` ma **non** in `causaliConMovimentoCommissione` (es. gli scambi
crypto/crypto, dove le monete sono già nette e la commissione è espressa solo in euro) viene corretto
solo il controvalore, senza movimento commissione aggiuntivo.

```json
"colonne": { "valoreEuro": 7 },
"colonneControvalore": {
  "totale": 8,
  "commissione": 9,
  "valuta": 5,
  "causali": ["Buy", "Convert"],
  "causaliConMovimentoCommissione": ["Buy"],
  "noteAcquistoDaSaldo": ["EUR Wallet"]
}
```

## 8. Pulizia e normalizzazione nomi moneta {#8-pulizia-e-normalizzazione-nomi-moneta}

### `rimuoviDaNomeMoneta` {#rimuovidanomemoneta}

**Tipo:** array di stringhe

Rimuove testi indesiderati dal nome del token. Supporta la sintassi ? per troncare tutto ciò che viene dopo o prima.

| Sintassi | Effetto su BTC.STAKING@CRYPTO.COM | Risultato |
|---|---|---|
| .STAKING? | Rimuove .STAKING e tutto quello che segue | BTC |
| ?.STAKING | Rimuove .STAKING e tutto quello che precede | @CRYPTO.COM |
| .STAKING | Rimuove solo la parola esatta | BTC@CRYPTO.COM |

Le regole vengono applicate in sequenza. Prima quelle più ampie (con ?).

```json
"rimuoviDaNomeMoneta": [".STAKING?",".EARN?",".LOCKED?","@CRYPTO.COM"]
```

### `rimuoviCaseSensitive` {#rimuovicasesensitive}

**Tipo:** booleano | **Default:** false

- false: la ricerca ignora maiuscole e minuscole.

- true: la ricerca e case sensitive esatta.

### `rinominaMonete` {#rinominamonete}

**Tipo:** oggetto chiave-valore

Rinomina un simbolo moneta. La rinomina avviene dopo la pulizia con rimuoviDaNomeMoneta.

```json
"rinominaMonete": { "IOTA": "MIOTA", "LUNA2": "LUNA", "WBTC": "BTC" }
```

## 9. Wallet per causale e giroconti fra wallet {#9-wallet-per-causale}

### `walletPerCausale` {#walletpercausale}

**Tipo:** oggetto chiave-valore

Permette di assegnare un wallet diverso da nomeWallet per specifiche causali. La chiave è la causale originale del CSV.

```json
"walletPerCausale": {
"Staking Rewards": "Staking", "Earn": "Earn", "Lockup": "Locked"
}
```

### `walletSpecularePerCausale` {#walletspecularepercausale}

**Tipo:** oggetto chiave-valore

Per le causali indicate viene creato, oltre al movimento normale, un **secondo movimento uguale e contrario** sul wallet indicato: se la riga porta 100 USDT in uscita dal wallet principale, ne viene generata l'entrata di 100 USDT sul wallet della gamba speculare. La chiave è la causale originale del CSV.

Serve per le operazioni che spostano una moneta in un comparto dell'exchange e la restituiscono più tardi, con un rendimento e a volte in un'altra moneta: sono due righe del CSV distanti settimane, che l'importazione non può accoppiare da sola. Le due gambe tengono il saldo esatto su entrambi i lati, e la differenza fra quanto è uscito e quanto è rientrato resta come **giacenza negativa** sul comparto, segnalata fra gli errori e da sistemare a mano registrando la reward corrispondente.

```json
"walletSpecularePerCausale": {
"Dual Savings Purchase": "Investimenti", "Dual Savings Settlement": "Investimenti"
}
```

Tre cose da sapere:

- **Il nome dell'exchange non cambia**: entrambe le gambe restano sullo stesso exchange, cambia solo il nome del wallet. È voluto, perché l'exchange determina il gruppo wallet usato per il calcolo fiscale.

- **La causale va mappata su `TRASFERIMENTO-CRYPTO-INTERNO`** e quel valore va aggiunto anche a `causaliChiuse`. Così le due gambe sono spostamenti interni, che non generano plusvalenze, e restano due movimenti distinti anche quando più righe cadono nello stesso secondo.

- **Vale solo per le righe che muovono una moneta sola.** Su una riga che ne muove già due la gamba speculare non viene creata e l'importazione prosegue con il solo movimento normale.

### `girocontoFiat` {#girocontofiat}

**Tipo:** oggetto | **Default:** assente

Per gli **euro** (o un'altra valuta FIAT) spostati fra due wallet tuoi, per esempio da Coinbase ad
Coinbase Pro. Nei CSV sono un prelievo FIAT su un wallet e un deposito FIAT sull'altro, indistinguibili da un
bonifico dalla banca: chi conta i depositi FIAT per sapere quanto ha versato conta due volte gli stessi soldi,
e il quadro RW li legge come apporto e prelievo. Con questo blocco, alla fine di ogni importazione, il
programma cerca nell'**intero archivio** le coppie e le trasforma in un normale trasferimento fra wallet.
La categoria resta deposito/prelievo FIAT, così il saldo si sposta davvero da un wallet all'altro.

| Campo | Descrizione |
|---|---|
| `controparte` | `nomeExchange` dell'altro wallet del giroconto. |
| `causali` | Causali CSV dei giroconti su **questo** wallet. |
| `causaliControparte` | Causali CSV dei giroconti sul wallet controparte. |
| `secondiTolleranza` | Scarto massimo in secondi fra le due gambe. Default 60. |

```json
"girocontoFiat": {
  "controparte": "Coinbase",
  "causali": ["deposit", "withdrawal"],
  "causaliControparte": ["Exchange Deposit", "Exchange Withdrawal", "Pro Deposit", "Pro Withdrawal"],
  "secondiTolleranza": 60
}
```

Tre cose da sapere:

- **Dichiara la regola in entrambe le configurazioni**, ognuna con le parti invertite. Così funziona in
  qualunque ordine importi i due file e basta reimportare uno dei due per sistemare anche i movimenti già in
  archivio.
- **Una coppia viene riconosciuta solo se entrambe le causali CSV sono fra quelle dichiarate.** È questo che
  tiene fuori un normale bonifico dalla banca (`Deposit` sul wallet retail), che non è un giroconto.
- Importo uguale, segno opposto, stessa valuta e scarto di tempo entro la tolleranza. Una riga senza
  controparte resta com'è. Lo stesso abbinamento si può fare a mano dalla classificazione del movimento.

## 10. Derivati {#10-derivati}

Il programma **non calcola i redditi da derivati** (futures, perpetui, opzioni, Dual Investment, art. 67 c-quater TUIR): li tratta come cripto-attività, e questo non è corretto sul piano fiscale. Le due chiavi seguenti servono a farli riconoscere e ad
avvisare l'utente.

### `causaliDerivati` {#causaliderivati}

**Tipo:** oggetto causale CSV → tipo | **Default:** {}

Per ogni causale elencata, i movimenti che nascono da quella riga ricevono nel campo Derivato un tipo che li
identifica. Il tipo non cambia il calcolo, serve solo a riconoscerli e filtrarli. Una causale presente qui fa
comparire anche l'avviso di fine importazione e l'avvertenza nelle stampe dei quadri W/RW e T/RT.

I tipi usati dalle configurazioni ufficiali sono `PNL` (risultato realizzato), `FUNDING`, `BONUS`,
`RIMBORSO_COMMISSIONI`, `CONSEGNA`, `DUAL` e `COMMISSIONE`.

```json
"causaliDerivati": {
"funding": "FUNDING", "trade": "PNL", "feeRefund": "RIMBORSO_COMMISSIONI"
}
```

### `causaliAllertaDerivati` {#causaliallertaderivati}

**Tipo:** array di causali CSV originali | **Default:** []

Fa comparire l'avviso sui derivati a fine importazione per le causali elencate, senza marcare i movimenti.
Le causali di `causaliDerivati` entrano automaticamente anche qui. Le configurazioni ufficiali ripetono le
stesse causali in **entrambe** le chiavi, perché le versioni del programma più vecchie ignorano
`causaliDerivati` e leggono solo questa. Se scrivi una configurazione solo per la tua installazione basta
`causaliDerivati`.

```json
"causaliAllertaDerivati": ["Dual Savings Purchase"]
```

## 11. Campi extra {#11-campi-extra}

### `campiExtra` {#campiextra}

**Tipo:** oggetto indiceMovimento -> indiceColonnaCSV

Copia il contenuto di una colonna CSV in un campo specifico del movimento. Funzione avanzata e raramente necessaria.

```json
"campiExtra": { "7": 9 }
```

## 12. Centralizzato {#12-centralizzato}

### `centralizzato` {#centralizzato}

**Tipo:** booleano | **Default:** false

Indica che questo file è gestito centralmente dal repository ufficiale. All'avvio del programma, quando vengono controllati i file di configurazione da GitHub, se un file locale ha centralizzato: true e non e più presente nel repository, viene automaticamente eliminato.

I file creati localmente dall'utente non devono avere centralizzato: true, altrimenti potrebbero essere cancellati involontariamente.

```json
"centralizzato": true
```

## Esempi completi {#esempi-completi}

Esempio 1: Binance Spot - righe separate per ogni lato dello scambio CSV "Date(UTC)","OrderNo","Pair","Type","Filled","Total","Fee","Fee Coin"

```text
"2025-03-10 14:22:01","123456","BTCUSDT","BUY","0.01 BTC","620.50 USDT","0.00001","BTC"
"2025-03-10 14:22:01","123456","BTCUSDT","SELL","620.50 USDT","","0.62","USDT"
```

JSON:

```json
{
"nomeExchange": "Binance", "nomeWallet": "Principale",
"separatore": ",", "formatoData": "yyyy-MM-dd HH:mm:ss", "fuso": "UTC",
"consolidaRigheStessaData": true, "tolleranzaSecondiConsolidamento": 2,
"colonne": {
"data": 0, "idTransazione": 1, "causale": 3,
"moneta": 2, "quantita": 4, "quantitaFee": 6, "monetaFee": 7
},
"mappaCausali": { "BUY": "SCAMBIO CRYPTO-CRYPTO", "SELL": "SCAMBIO CRYPTO-CRYPTO" }
}
```

### Esempio 2: Riga singola per movimento - es. Tatax {#esempio-2-riga-singola-per-movimento---es-tatax}

CSV:

```text
"Data","Tipo","Asset","Quantita"
"2025-09-15 10:00:00","Deposito","BTC","0.5"
"2025-09-16 11:30:00","Staking","BTC.STAKING@CRYPTO.COM","0.0001"
"2025-09-17 09:00:00","Prelievo","ETH","0.1"
```

JSON:

```json
{
"nomeExchange": "Crypto.com", "nomeWallet": "Principale",
"separatore": ",", "formatoData": "yyyy-MM-dd HH:mm:ss", "fuso": "UTC",
"consolidaRigheStessaData": false,
"colonne": { "data": 0, "causale": 1, "moneta": 2, "quantita": 3 },
"mappaCausali": {
"Deposito": "TRASFERIMENTO-CRYPTO", "Prelievo": "TRASFERIMENTO-CRYPTO",
"Staking": "STAKING REWARDS"
},
"causaliChiuse": ["TRASFERIMENTO-CRYPTO","STAKING REWARDS"],
"causaliEntrata": ["Deposito","Staking"],
"causaliUscita": ["Prelievo"],
"rimuoviDaNomeMoneta": [".STAKING?",".EARN?","@CRYPTO.COM"],
"rimuoviCaseSensitive": false
}
```

### Esempio 3: Scambio su riga singola - es. CoinTracking {#esempio-3-scambio-su-riga-singola---es-cointracking}

CSV:

```text
"Tipo","Acquisto","Cur.","Vendita","Cur.","Fee","Cur.Fee","Exchange","Data"
"Operazione","2132","CRO","487.04","USDC","1.18","USDC","Crypto.com","17.09.2025 18:39:10"
"Deposito","89.4","CRO","","","","","Crypto.com","28.08.2025 07:38:42"
"Prelievo","","","24.32","USDC","","","Crypto.com","03.09.2025 18:11:09"
```

JSON:

```json
{
"nomeExchange": "Crypto.com Exchange", "nomeWallet": "Principale",
"separatore": ",", "formatoData": "dd.MM.yyyy HH:mm:ss", "fuso": "UTC",
"colonne": {
"data": 8, "causale": 0, "moneta": 2, "quantita": 1,
"monetaUscita": 4, "quantitaUscita": 3, "quantitaFee": 5, "monetaFee": 6
},
"mappaCausali": {
"Deposito": "TRASFERIMENTO-CRYPTO", "Prelievo": "TRASFERIMENTO-CRYPTO",
"Operazione": "SCAMBIO CRYPTO-CRYPTO"
},
"ricostruisciLordoSeFeeSuMonetaUscita": false,
"ricostruisciLordoSeFeeSuMonetaEntrata": true
}
```

### Esempio 4: Causale composita su più colonne {#esempio-4-causale-composita-su-più-colonne}

CSV:

```text
"Date","Category","SubType","Amount","Currency"
"2025-01-10","Trade","Buy","0.005","BTC"
"2025-01-10","Trade","Sell","200","USDT"
"2025-01-11","Earn","Staking","0.0001","ETH"
```

JSON:

```json
{
"nomeExchange": "Exchange XYZ", "separatore": ",",
"formatoData": "yyyy-MM-dd", "fuso": "UTC",
"consolidaRigheStessaData": true,
"colonne": { "data": 0, "moneta": 4, "quantita": 3, "causale": 1, "causale2": 2 },
"separatoreCausale": ".", "causaliUppercase": false,
"mappaCausali": {
"Trade.Buy": "SCAMBIO CRYPTO-CRYPTO",
"Trade.Sell": "SCAMBIO CRYPTO-CRYPTO",
"Earn.Staking": "STAKING REWARDS"
},
"causaliChiuse": ["STAKING REWARDS"],
"causaliEntrata": [], "causaliUscita": []
}
```

## Checklist rapida {#checklist-rapida}

- Apri il CSV e conta le colonne partendo da 0.

- Identifica data, causale, moneta e quantità; compila la sezione colonne.

- Elenca tutte le causali presenti nel CSV e compila mappaCausali.

- Se le quantità sono sempre positive, compila causaliUscita e causaliEntrata.

- Se ogni riga è un movimento indipendente usa consolidaRigheStessaData: false; se più righe appartengono allo stesso trade usa true.

- Se i nomi moneta contengono suffissi, compila rimuoviDaNomeMoneta.

- Se ci sono commissioni, mappa monetaFee e quantitaFee; se la quantità è già comprensiva della fee abilita il flag ricostruisciLordo appropriato.

- Se la colonna del totale include commissione **e** spread (Coinbase), usa colonneControvalore per scorporare la sola commissione dal costo di carico.

- Se un acquisto può essere pagato anche con carta o bonifico diretto (Coinbase), usa `noteAcquistoDaSaldo` per non creare euro in uscita che non sono mai passati dal saldo.

- Se la natura del movimento è scritta solo nel testo libero di una colonna (es. "from Coinbase Earn"), mappa colonne.note e aggiungi le regole in causalePerNota.

- Se le gambe di uno stesso movimento condividono una colonna diversa da idGruppo ma non il timestamp esatto, usa raggruppamentoPerCausale.

- Se alcuni movimenti devono andare su wallet separati, compila walletPerCausale.

- Se una causale sposta una moneta in un comparto dell’exchange e una seconda causale la restituisce settimane dopo (depositi vincolati, Dual Investment), usa walletSpecularePerCausale.

- Se le intestazioni CSV variano di versione in versione, usa autoDetectColonne con mappaAutoDetect.

- Se importo e moneta stanno nella stessa cella (`407.57 AME`), usa `separatoreValoreMoneta`. Se c'è solo la coppia di trading (`KCS-ETH`), usa `coppia`.

- Se il file contiene solo depositi o solo prelievi e non ha una colonna causale utile, usa `versoForzato`.

- Se il verso delle due gambe dipende dalla causale (Buy/Sell sulle stesse colonne), usa `causaliScambiaGambe`. Se ogni riga ha due monete ma solo a volte è uno scambio, usa `gambaDoppiaConSegnoSuUscita`.

- Se l'export accredita micro-interessi decine di volte al giorno, somma le righe con `causaliConsolidaPerGiorno`.

- Se gli euro passano fra due tuoi wallet (Coinbase e Coinbase Pro), dichiara `girocontoFiat` in entrambe le configurazioni.

- Se il file contiene derivati, elenca le causali in `causaliDerivati` per avere l'avviso e il marcatore.

- Prima di condividere il file, compila `descrizione` e, se usa funzioni recenti, `versioneMinimaApp`.

- Se il tipo di operazione è su più colonne, usa causale2/causale3 con separatoreCausale.

- Se il CSV non riporta né prezzo né controvalore e vuoi che venga usato il prezzo di un exchange specifico fra quelli già scaricati, compila fontePrezzoPreferita.

## Dove mettere il file {#dove-mettere-il-file}

Nella cartella di lavoro del programma si trovano due cartelle di configurazione:

- `config/import/` — è la cartella attuale, sincronizzata con il repository ufficiale: qui arrivano le
  configurazioni distribuite con il programma;
- `ImportConfig/` — la cartella storica, mantenuta per compatibilità con le installazioni precedenti. Da
  qui vengono letti soltanto i file **non** marcati `"centralizzato": true`, cioè quelli scritti
  dall'utente.

Un file JSON messo in una di queste due cartelle compare nella finestra di importazione insieme agli
import nativi, ordinato per fornitore ed estrazione (il vecchio prefisso `[JSON]` non c'è più: la
distinzione è nei campi, non nell'etichetta).

I file creati personalmente vanno lasciati **senza** `centralizzato` (o con `"centralizzato": false`):
non verranno mai eliminati automaticamente dall'aggiornamento dal repository remoto.

[Torna all'indice della documentazione](./)
