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
"nomeWallet": "...",
"testing": false,
"separatore": ",",
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
"colonne": { ... },
"raggruppamentoPerCausale": { ... },
"colonneControvalore": { ... },
"mappaCausali": { ... },
"causalePerNota": [ ... ],
"causaliChiuse": [ ... ],
"causaliUscita": [ ... ],
"causaliEntrata": [ ... ],
"ricostruisciLordoSeFeeSuMonetaUscita": false,
"ricostruisciLordoSeFeeSuMonetaEntrata": false,
"rimuoviCaseSensitive": false,
"rimuoviDaNomeMoneta": [ ... ],
"rinominaMonete": { ... },
"walletPerCausale": { ... },
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
"Deposit": "DEPOSITO-CRYPTO",
"Withdrawal": "PRELIEVO-CRYPTO",
"Trade": "SCAMBIO CRYPTO-CRYPTO",
"Staking Rewards": "STAKING REWARDS",
"Commission": "COMMISSIONI",
"earn":"EARN",
"cashback":"CASHBACK"
}
```

Per ignorare righe senza contarle come scarti, mappa su "IGNORA":

```json
"mappaCausali": { "Internal Transfer": "IGNORA" }
```

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

Tipologie interne che devono essere trattate come movimenti singoli anche se condividono l'ID transazione con altre righe. Usa per staking, cashback, commissioni, depositi e prelievi.

```json
"causaliChiuse": [
"DEPOSITO-CRYPTO","PRELIEVO-CRYPTO",
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
  "causaliConMovimentoCommissione": ["Buy"]
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

## 9. Wallet per causale {#9-wallet-per-causale}

### `walletPerCausale` {#walletpercausale}

**Tipo:** oggetto chiave-valore

Permette di assegnare un wallet diverso da nomeWallet per specifiche causali. La chiave è la causale originale del CSV.

```json
"walletPerCausale": {
"Staking Rewards": "Staking", "Earn": "Earn", "Lockup": "Locked"
}
```

## 10. Campi extra {#10-campi-extra}

### `campiExtra` {#campiextra}

**Tipo:** oggetto indiceMovimento -> indiceColonnaCSV

Copia il contenuto di una colonna CSV in un campo specifico del movimento. Funzione avanzata e raramente necessaria.

```json
"campiExtra": { "7": 9 }
```

## 11. Centralizzato {#11-centralizzato}

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
"Deposito": "DEPOSITO-CRYPTO", "Prelievo": "PRELIEVO-CRYPTO",
"Staking": "STAKING REWARDS"
},
"causaliChiuse": ["DEPOSITO-CRYPTO","PRELIEVO-CRYPTO","STAKING REWARDS"],
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
"Deposito": "DEPOSITO-CRYPTO", "Prelievo": "PRELIEVO-CRYPTO",
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

- Se la natura del movimento è scritta solo nel testo libero di una colonna (es. "from Coinbase Earn"), mappa colonne.note e aggiungi le regole in causalePerNota.

- Se le gambe di uno stesso movimento condividono una colonna diversa da idGruppo ma non il timestamp esatto, usa raggruppamentoPerCausale.

- Se alcuni movimenti devono andare su wallet separati, compila walletPerCausale.

- Se le intestazioni CSV variano di versione in versione, usa autoDetectColonne con mappaAutoDetect.

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
