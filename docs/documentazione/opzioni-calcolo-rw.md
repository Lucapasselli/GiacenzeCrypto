---
layout: default
title: Opzioni di calcolo del Quadro RW
---

# Opzioni di calcolo del Quadro W/RW

- [Introduzione](#introduzione)
- [Panoramica delle opzioni](#panoramica-delle-opzioni)
- [Caso studio 1 con esempi di calcolo](#caso-studio-1-con-esempi-di-calcolo)
- [Gestione dei trasferimenti tra wallet](#gestione-dei-trasferimenti-tra-wallet)
- [Gruppi wallet: gruppi preconfigurati e bollo](#gruppi-wallet-preconfigurati-e-bollo)
- [Caso studio 2: trasferimenti tra wallet](#caso-studio-2--esempi-di-gestione-dei-trasferimenti-tra-wallet)
- [Valorizzazione di staking, airdrop, cashback e reward](#valorizzazione-degli-staking-airdrop-cashback-e-reward-varie)
- [Gestione del LIFO nel Quadro W/RW](#gestione-del-lifo-nel-quadro-wrw)
- [Periodi di detenzione: un rigo per periodo](#periodi-di-detenzione)
- [Se il bollo è pagato](#opzione--se-bollo-pagato-mostra-solo-le-giacenze-di-inizio-e-fine-anno)
- [Liquidità presso intermediari esteri](#liquidita-presso-intermediari-esteri)
- [Valori di inizio e fine anno](#valori-di-inizio-e-fine-anno)
- [Stampa del quadro](#stampa-del-quadro)
- [Gestione degli errori](#gestione-degli-errori)

## Introduzione {#introduzione}

Le opzioni di calcolo per il quadro W/RW si trovano in **Opzioni** – **Opzioni Calcolo RW/W**, i gruppi
wallet e i loro periodi di detenzione in **Opzioni** – **Gruppi Wallet Crypto**. 
Il quadro si calcola e si stampa dalla scheda **Dichiarazioni** – **RW/W**.

Partiamo però dallo spiegare qual è la logica che sta dietro il calcolo del quadro W/RW.

Le istruzioni dicono che

“*Nel quadro RW devono essere riportate le consistenze degli investimenti e delle attività valorizzate all’inizio di ciascun periodo d’imposta ovvero al primo giorno di detenzione (di seguito, “valore iniziale”) e al termine dello stesso ovvero al termine del periodo di detenzione nello stesso (di seguito, “valore finale”), nonché il periodo di possesso.*“

Preso alla lettera, questo significa che servirebbe un rigo per ogni investimento: ogni acquisto, ogni
scambio, ogni reward apre un nuovo periodo di detenzione con il suo valore iniziale, e ogni vendita o fine
anno lo chiude con il suo valore finale. Il programma fa proprio questo: per ogni nuovo investimento, o
all’inizio dell’anno, apre un rigo imputando il valore iniziale, e alla chiusura dello stesso, o alla fine
dell’anno, lo chiude con il valore finale.

> **NB** — resta comunque la possibilità di scegliere di visualizzare il solo valore iniziale e finale, senza alcun tipo di calcolo accessorio.

Un rigo per ogni movimento renderebbe però il quadro ingestibile. Per questo i righi così ottenuti vengono
poi **accorpati in un unico rigo per ogni gruppo wallet**, sommando i valori iniziali e finali e calcolando
i giorni di detenzione tramite media ponderata sul valore finale. Se per un gruppo sono stati definiti dei
[periodi di detenzione](#periodi-di-detenzione), l’accorpamento avviene per gruppo **e per periodo**: un
rigo per ciascun periodo.

Questa possibilità viene data dalle istruzioni di compilazione del quadro RW che così recitano

“***In presenza di più operazioni della stessa natura, il contribuente può aggregare i dati per indicare un insieme di attività finanziarie omogenee caratterizzate, cioè, dai medesimi codici “investimento” e “Stato Estero”**. In tal caso il contribuente indicherà nel quadro RW i valori complessivi iniziali e finali del periodo di imposta, la media ponderata dei giorni di detenzione di ogni singola attività rapportati alla relativa consistenza, nonché l’IVAFE complessiva dovuta per il gruppo di attività. La predetta compilazione semplificata del quadro RW è ammessa a condizione che sia predisposto e conservato un apposito prospetto da esibire o trasmettere, su richiesta all’Amministrazione finanziaria, in cui sono specificati i dati delle singole attività finanziarie (in conformità ai criteri di valorizzazione delle attività contenuti nella circolare n. 38/E del 2013), i criteri di raggruppamento di queste ultime nonché le modalità di calcolo dell’IVAFE. *“

Il prospetto di dettaglio richiesto è quello che il programma mostra nella tabella **Dettaglio RW** e che si
può esportare in Excel con il pulsante **Crea Excel con i Dettagli**.

Es. Supponiamo che :

- All’ **01/01/2023** detenga **1 BTC** per un valore di **10000€**

- Il **30/01/2023** acquisti **1** ulteriore **BTC** per **15000€**

- Al **31/12/2023** il prezzo di 1 BTC sia 30000€ quindi detengo in totale **2 BTC** per un valore di **60000€**

In questo caso il programma genererà 2 righi :

RW1 : Val.Iniziale = 10000 € - Val.Finale = 30000€ - Giorni di detenzione = 365 - IC = 60€

RW2 : Val.Iniziale = 15000 € - Val.Finale = 30000€ - Giorni di detenzione = 335 - IC = 55€

Questi 2 righi però facendo parte di uno medesimo wallet e rispettando le regole di cui sopra verranno poi aggregati dal programma nel seguente modo.

- **Val. iniziale tot.** = Val. iniziale RW1 + Val. iniziale RW2 = **25.000 €**
- **Val. finale tot.** = Val. finale RW1 + Val. finale RW2 = **60.000 €**
- **GG di det. tot.** = (V.F.RW1 × GG.RW1 + V.F.RW2 × GG.RW2) / (V.F.RW1 + V.F.RW2) = **350**
- **IC tot.** = V.F.tot / 365 × GG.tot × 0,2% = **115 €**

## Panoramica delle opzioni {#panoramica-delle-opzioni}

Il programma prevede diverse opzioni per i calcoli del quadro W/RW per soddisfare diverse interpretazioni, un po' come caso studio un po' perché effettivamente non si capisce quale sia la più aderente alla normativa, insomma in questo modo ognuno è libero di fare un po' come crede.

Queste sono le opzioni nel loro complesso :

![Le opzioni di calcolo del Quadro W/RW](immagini/opzioni-calcolo-rw/01.png)

Come si può vedere le opzioni si distinguono sostanzialmente in 3 sezioni :

**Prima Sezione** - Si sceglie il momento in cui il programma deve scindere i righi.

In pratica qui si cerca di interpretare cosa intendono per “*fine periodo di detenzione*”.

A seconda di cosa scelgo vado a indicare al programma quando deve terminare un rigo RW che termina appunto a fine anno oppure **al termine del periodo di detenzione**.

Vediamo le opzioni a disposizione :

**Opzione A** – Questa è l’unica opzione che non prevede calcoli.

Viene messo come **valore iniziale** il valore del wallet a **inizio anno o primo movimento assoluto** e come **valore finale** il valore del wallet a **fine anno**.

**Opzione B** – Significa che il termine del periodo di detenzione delle cripto in mio possesso avviene solamente nel momento in cui ritorno in FIAT o faccio cashout.

**Opzione C** – Significa che il termine del periodo di detenzione avviene nel momento in cui c’è uno scambio fiscalmente rilevante (es. scambio per NFT ,Emoney Token o FIAT), ovviamente questo determina anche l’inizio di un nuovo periodo di detenzione per la cripto attività entrante.

**Opzione D** - Significa che il termine del periodo di detenzione avviene su ogni trade ossia nel momento in cui finisco di detenere una certa cripto.

Anche qui il termine del periodo di detenzione delle cripto uscente equivale anche all’inizio di un nuovo periodo di detenzione per la cripto entrante.

**Seconda Sezione** - Si sceglie come gestire casistiche particolari che possono riguardare la gestione dei trasferimenti così come la gestione delle reward etc...

**Terza Sezione** - Riguarda la **liquidità** (euro e altre valute) detenuta presso intermediari esteri: se
includerla nel quadro e come trattarne l’IVAFE. È descritta nel capitolo
[Liquidità presso intermediari esteri](#liquidita-presso-intermediari-esteri).

## Caso studio 1 con esempi di calcolo {#caso-studio-1-con-esempi-di-calcolo}

Supponiamo che le seguenti siano la totalità delle operazioni effettuate da un utente fino a fine 2023 :

| **Data** | **Tipo Scambio** | **Mon. Uscita** | **Qta Uscita** | **Mon. Entrata** | **Qta Entrata** | **Valore Transazione** |
|---|---|---|---|---|---|---|
| 2022-07-01 | ACQUISTO CRYPTO | EUR | -18100 | BTC | 1 | € 18100.00 |
| 2023-01-10 | SCAMBIO CRYPTO | BTC | -0.5 | ETH | 6.5 | € 8009.79 |
| 2023-01-15 | ACQUISTO CRYPTO | EUR | -1930 | BTC | 0.1 | € 1930.00 |
| 2023-05-30 | SCAMBIO CRYPTO | ETH | -2.5 | USDC | 4730 | € 4412.98 |
| 2023-08-30 | VENDITA CRYPTO | ETH | -4 | EUR | 6360 | € 6359.10 |

Nel caso specificato mi ritrovo ad avere ad inizio **01/01/2023** le seguenti Crypto:

| **Moneta** | **Quantità** | **Valore** |
|---|---|---|
| BTC | 1 | € 15428.81 |

Mentre alla fine del **31/12/2023** queste:

| **Moneta** | **Quantità** | **Valore** |
|---|---|---|
| BTC | 0.6 | € 22983.46 |
| USDC | 4730 | € 4286.31 |

Vediamo come saranno ora i calcoli e a quanto ammontarà l’IC a seconda dell’opzione scelta.

### Cosa succede se si seleziona l’opzione A {#cosa-succede-se-si-seleziona-lopzione-a}

![Opzione A selezionata](immagini/opzioni-calcolo-rw/02.png)

Se si selezione l’opzione A andiamo a dire al programma che quello che ci interessa per la compilazione del quadro W/RW sono solamente il valore iniziale e finale del Wallet.

Il Valore iniziale sarà il valore del Wallet all’inizio dell’anno o nel caso in cui il wallet sia stato movimentato per la prima volta in assoluto nell’anno di competenza fiscale sarà il valore del primo movimento.

Stessa cosa per la data iniziale.

Il Valore e la data finali saranno invece quelle del 31/12.

Nel nostro esempio quindi la situazione del Wallet sarà questa :

| **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|
| BTC | 1 | 2023-01-01 | € 15428.81 | BTC | 0.6 | 2023-12-31 | € 22983.46 | 365 | Fine Anno |
| USDC | 0 | 2023-01-01 | € 0.00 | USDC | 4730 | 2023-12-31 | € 4286.31 | 365 | Fine Anno |

Che si traduce nel seguente rigo per l’RW

| **Valore Iniziale** | **Valore Finale** | **Giorni di Detenzione** | **IC Calcolata** |
|---|---|---|---|
| € 15428.81 | € 27269.77 | 365 | € 54.54 |

#### Le regole della fotografia di inizio e fine anno {#regole-della-fotografia-di-inizio-e-fine-anno}

La stessa “fotografia” delle giacenze viene usata dall’opzione A e, per i gruppi che pagano il bollo, dalla
casella [“mostra solo giacenza ad inizio fine anno”](#opzione--se-bollo-pagato-mostra-solo-le-giacenze-di-inizio-e-fine-anno).
Segue queste regole:

- **Cripto e NFT sì, valute no.** Gli NFT fanno parte delle giacenze, gli euro e le altre valute no (la
  liquidità ha una [sezione a parte](#liquidita-presso-intermediari-esteri)).
- **Inizio anno.** Per un gruppo che esisteva già, il valore iniziale è la giacenza alle 00:00 del 1°
  gennaio, prezzata a quell’istante: i movimenti del 1° gennaio non contano.
- **Gruppo aperto durante l’anno.** La data iniziale è quella del primo movimento del gruppo, per tutte le
  monete. Parte con una giacenza solo la moneta **entrata** con quel movimento, valorizzata al valore del
  movimento (o al prezzo di mercato di quell’istante, se il movimento ha valore zero come capita con airdrop
  e reward). La moneta eventualmente ceduta nello stesso movimento non conta come giacenza iniziale.
- **Giacenze negative.** Una giacenza negativa (tipicamente un movimento mancante) non viene conteggiata:
  vale zero e il rigo viene segnalato come errore, così da poterla correggere.

### Cosa succede se si seleziona l’opzione B {#cosa-succede-se-si-seleziona-lopzione-b}

![Opzione B selezionata](immagini/opzioni-calcolo-rw/03.png)

Se selezioniamo l’opzione B andiamo a dire al programma che la discriminante per la chiusura / apertura di un nuovo rigo del quadro RW è sostanzialmente lo scambio tra Crypto e FIAT ed eventuali cashout o Rewards.

In questo caso i valori che comporranno poi i singoli quadri sono questi :

| **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|
| BTC | 0.3076923 | 2023-01-01 | € 4747.33 | ETH | 4 | 2023-08-30 | € 6359.10 | 242 | Vendita |
| BTC | 0.5 | 2023-01-01 | € 7714.41 | BTC | 0.5 | 2023-12-31 | € 19152.88 | 365 | Fine Anno |
| BTC | 0.1 | 2023-01-15 | € 1930.00 | BTC | 0.1 | 2023-12-31 | € 3830.58 | 351 | Fine Anno |
| BTC | 0.1923077 | 2023-01-01 | € 2967.08 | USDC | 4730 | 2023-12-31 | € 4286.31 | 365 | Fine Anno |

Mentre l’RW aggregato è il seguente:

| **Valore Iniziale** | **Valore Finale** | **Giorni di Detenzione** | **IC Calcolata** |
|---|---|---|---|
| € 17358.82 | € 33628.87 | 340.15 | € 62.68 |

Come si può notare l’unico movimento che ha generato una chiusura anticipata del quadro è stata la vendita avvenuta il 30/08/2023 di 4 ETH.

Sempre in questo esempio si può notare che come valore iniziale è stato presa una quota del valore iniziale dei btc posseduti calcolati tramite LiFo, infatti all’inizio dell’anno avevo dei BTC che poi ho venduto per ETH e alla fine ho scambiato per EURO.

E’ stato quindi il solo passaggio in Euro a determinare la fine dell’investimento che era partito dai BTC che avevo ad inizio anno.

### Cosa succede se si seleziona l’opzione C {#cosa-succede-se-si-seleziona-lopzione-c}

![Opzione C selezionata](immagini/opzioni-calcolo-rw/04.png)

In questo caso la discriminante per la chiusura di un rigo quadro W/RW non è più il solo passaggio a FIAT ma bensì tutti gli scambi fiscalmente rilevanti.

**N.B**. per questo esempio considererò lo scambio tra ETH e USDC fiscalmente rilevante.

In questo caso i valori che comporranno poi i singoli quadri sono questi :

| **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|
| BTC | 0.1923077 | 2023-01-01 | € 2967.08 | ETH | 2.5 | 2023-05-30 | € 4412.98 | 150 | Scambio |
| BTC | 0.3076923 | 2023-01-01 | € 4747.33 | ETH | 4 | 2023-08-30 | € 6359.10 | 242 | Vendita |
| BTC | 0.5 | 2023-01-01 | € 7714.41 | BTC | 0.5 | 2023-12-31 | € 19152.88 | 365 | Fine Anno |
| BTC | 0.1 | 2023-01-15 | € 1930.00 | BTC | 0.1 | 2023-12-31 | € 3830.58 | 351 | Fine Anno |
| USDC | 4730 | 2023-05-30 | € 4412.98 | USDC | 4730 | 2023-12-31 | € 4286.31 | 216 | Fine Anno |

Mentre l’RW aggregato è il seguente:

| **Valore Iniziale** | **Valore Finale** | **Giorni di Detenzione** | **IC Calcolata** |
|---|---|---|---|
| € 21771.80 | € 38041.85 | 301.30 | € 62.81 |

A differenza del caso precedente qui anche lo scambio con USDC che abbiamo deciso a posteriori fosse rilevante fiscalmente (lo si può scegliere dalla funzione presente in Opzioni – EMoney Token) ha generato un momento in cui viene chiuso un rigo del quadro RW.

### Cosa succede se si seleziona l’opzione D {#cosa-succede-se-si-seleziona-lopzione-d}

![Opzione D selezionata](immagini/opzioni-calcolo-rw/05.png)

In questo caso ogni trade comporta la chiusura di un rigo del quadro W/RW.

In questo caso i valori che comporranno poi i singoli quadri sono questi :

| **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|
| BTC | 0.5 | 2023-01-01 | € 7714.41 | BTC | 0.5 | 2023-01-10 | € 8009.79 | 10 | Scambio |
| ETH | 2.5 | 2023-01-10 | € 3080.69 | ETH | 2.5 | 2023-05-30 | € 4412.98 | 141 | Scambio |
| ETH | 4 | 2023-01-10 | € 4929.10 | ETH | 4 | 2023-08-30 | € 6359.10 | 233 | Vendita |
| BTC | 0.5 | 2023-01-01 | € 7714.41 | BTC | 0.5 | 2023-12-31 | € 19152.88 | 365 | Fine Anno |
| BTC | 0.1 | 2023-01-15 | € 1930.00 | BTC | 0.1 | 2023-12-31 | € 3830.58 | 351 | Fine Anno |
| USDC | 4730 | 2023-05-30 | € 4412.98 | USDC | 4730 | 2023-12-31 | € 4286.31 | 216 | Fine Anno |

Mentre l’RW aggregato è il seguente:

| **Valore Iniziale** | **Valore Finale** | **Giorni di Detenzione** | **IC Calcolata** |
|---|---|---|---|
| € 29781.59 | € 46051.64 | 248.53 | € 62.71 |

In questo caso, ogni movimento ha generato la chiusura di un rigo del quadro e la conseguente apertura di una nuova.

## Gestione dei trasferimenti tra Wallet {#gestione-dei-trasferimenti-tra-wallet}

L’Agenzia delle Entrate specifica che

“*Nel quadro RW, potrà essere compilato un rigo per ogni “portafoglio” o “conto digitale” o altro sistema di archiviazione o conservazione detenuto dal contribuente*“.

Detta così non è molto chiaro cosa intende l’AdE per portafoglio o conto digitale e potrebbe prestarsi a più interpretazioni.

Sicuramente ogni exchange è un portafoglio a parte ma per la defi come ci si comporta?

Si crea un rigo per ogni chiave privata, un rigo per ogni indirizzo e per ogni chain o un rigo per ogni portafoglio inteso come trust wallet, ledger etc?

Anche qui nel dubbio il programma lascia all’utente la possibilità di scegliere, in **Opzioni** – **Gruppi Wallet Crypto**.

![Assegnazione dei wallet ai gruppi](immagini/opzioni-calcolo-rw/06.png)

Come si può vedere dall’immagine è data piena libertà all’utente di decidere in che gruppo fanno parte i vari wallet/indirizzi defi caricati, verrà poi generato un nuovo rigo RW per ogni Gruppo Wallet (o uno per ogni periodo, se il gruppo ha dei [periodi di detenzione](#periodi-di-detenzione)).

Selezionando un gruppo, la parte inferiore della schermata ne mostra il riepilogo: gli exchange e i wallet
che lo compongono con il numero di movimenti, i dati fiscali e i periodi di detenzione. Da qui si arriva
anche ai pulsanti **Rinomina Alias**, **Periodi di detenzione e dati fiscali** e **Raggruppa exchange noti**,
descritti nel capitolo successivo.

**Ma come si comporta il programma in caso di trasferimenti?**

Di default in caso di trasferimenti il token viene spostato in toto con tutta la sua storia sul wallet di destinazione ma ovviamente ci sono diverse opzioni per decidere come gestire i trasferimenti.

**N.B. :** Affinchè il programma consideri correttamente i trasferimenti è importante classificare correttamente i movimenti di deposito e prelievo nella funzione “**Classificazione Depositi/Prelievi**” presente nella sezione “**Analisi Crypto**”.

Se questo non viene fatto i Depositi e Prelievi dovuti a dei trasferimenti non verranno trattati correttamente generando quindi errori di calcolo sia per quanto riguarda la plusvalenza sia per quanto riguarda il quadro RW.

## Gruppi wallet: gruppi preconfigurati e bollo {#gruppi-wallet-preconfigurati-e-bollo}

**Alias.** Ogni gruppo ha un nome fisso (*Wallet 01*, *Wallet 02*, …) e un **alias**, cioè il nome con cui
compare nel quadro e nella stampa (per esempio *Ledger* o *TrustWallet 1*). L’alias si cambia con il
pulsante **Rinomina Alias**.

**Gruppi preconfigurati per gli exchange.** I gruppi da *Wallet 101* a *Wallet 114* sono riservati ai
principali exchange: Binance, Coinbase, Kraken, Crypto.com, OKX, Bitpanda, Bitget, KuCoin, Bybit, Revolut,
Nexo, Bitstamp, Gemini e Bitfinex. Portano già l’alias con il nome dell’exchange e, dove sono noti, i suoi
dati fiscali (Stato estero e identificativo dell’intermediario) e i suoi periodi. Quando compare per la
prima volta un exchange riconosciuto (in un archivio nuovo, oppure un exchange mai importato prima), i suoi
movimenti vengono associati al gruppo preconfigurato. La scelta dell’utente ha sempre la precedenza: un
wallet già messo in un gruppo non viene più spostato.

I wallet già presenti in un archivio esistente non vengono spostati da soli, perché cambierebbero i righi
degli anni già dichiarati. Per raggruppare anche il pregresso si usa il pulsante **Raggruppa exchange noti**: sposta
nei gruppi preconfigurati solo i wallet degli exchange riconosciuti che si trovano ancora in *Wallet 99*
(i wallet da classificare), e prima di procedere chiede conferma avvisando che il ricalcolo degli anni già
presentati non coinciderà più con quanto dichiarato.

**Bollo pagato dall’intermediario.** La colonna **Bollo Pagato dall’Intermediario** della tabella indica i
gruppi per cui l’intermediario ha già assolto l’imposta: per quei gruppi l’imposta calcolata dal programma
è zero (vedi [Se il bollo è pagato](#opzione--se-bollo-pagato-mostra-solo-le-giacenze-di-inizio-e-fine-anno)).
Se il gruppo ha dei periodi di detenzione, il bollo si indica **periodo per periodo**: spuntare la casella
lo applica a tutti i periodi cripto del gruppo, mentre se i periodi hanno impostazioni diverse la casella
resta bloccata e il suggerimento elenca il bollo di ciascun periodo.

## Caso Studio 2 : Esempi di gestione dei trasferimenti tra wallet {#caso-studio-2--esempi-di-gestione-dei-trasferimenti-tra-wallet}

Supponiamo di avere la seguente situazione:

A inizio 2023 anno possiedo 1 BTC sul Wallet A e 1 ETH sul Wallet B.

Il 30 di Giugno 2023 sposto 0,25 BTC dal Wallet A al Wallet B

Quindi facciamo finta che la situazione dei movimenti sui miei wallet sia la seguente :

| **Data** | **Wallet** | **Tipo Scambio** | **Mon. Uscita** | **Qta Uscita** | **Mon. Entrata** | **Qta Entrata** | **Valore Transazione** |
|---|---|---|---|---|---|---|---|
| 2022-07-16 | **A** | ACQUISTO CRYPTO | EUR | -20766.17 | BTC | 1 | 20766.17 |
| 2022-07-16 | **B** | ACQUISTO CRYPTO | EUR | -1241.60 | ETH | 1 | 1241.60 |
| 2023-06-30 | **A** | TRASFERIMENTO TRA WALLET | BTC | -0.25 |  |  | 6997.28 |
| 2023-06-30 | **B** | TRASFERIMENTO TRA WALLET |  |  | BTC | 0.25 | 6997.28 |

Nel caso specificato mi ritrovo ad avere al **01/01/2023** le seguenti Crypto:

| **Wallet** | **Moneta** | **Quantità** | **Valore** |
|---|---|---|---|
| **A** | BTC | 1 | € 15504.13 |
| **B** | ETH | 1 | € 1121.06 |

Mentre al **31/12/2023** queste:

| **Wallet** | **Moneta** | **Quantità** | **Valore** |
|---|---|---|---|
| **A** | BTC | 0,75 | € 28729.32 |
| **B** | BTC | 0,25 | € 9576.44 |
| **B** | ETH | 1 | € 2067.20 |

Nelle pagine successive vedremo come questo caso viene trattato a seconda delle opzioni scelte.

### Calcoli con biffata l’opzione per non considerare gli spostamenti tra Wallet di proprietà {#calcoli-con-biffata-lopzione-per-non-considerare-gli-spostamenti-tra-wallet-di-proprietà}

![Opzione: non considerare gli spostamenti tra wallet di proprietà](immagini/opzioni-calcolo-rw/07.png)

Questa la situazione riguardo i quadri RW per i 2 wallet

| **Wallet di riferimento** | **Valore Iniziale** | **Valore Finale** | **Giorni di Detenzione** | **IC Calcolata** |
|---|---|---|---|---|
| 01 ( **A** ) | € 11628.10 | € 28729.32 | 365.00 | € 57.46 |
| 02 ( **B** ) | € 4997.09 | € 11643.64 | 365.00 | € 23.29 |

IC TOTALE : **€ 80.75**

Questo invece il dettaglio della composizione del Wallet **A**

| **Wallet Iniziale** | **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Wallet Finale** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 01 ( **A** ) | BTC | 0.75 | 2023-01-01 | € 11628.10 | 01 ( **A** ) | BTC | 0.75 | 2023-12-31 | € 28729.32 | 365 | Fine Anno |

E quindi il dettaglio del Wallet **B**

| **Wallet Iniziale** | **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Wallet Finale** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 01 ( **A** ) | BTC | 0.25 | 2023-01-01 | € 3876.03 | 02 ( **B** ) | BTC | 0.25 | 2023-12-31 | € 9576.44 | 365 | Fine Anno |
| 02 ( **B** ) | ETH | 1 | 2023-01-01 | € 1121.06 | 02 ( **B** ) | ETH | 1 | 2023-12-31 | € 2067.20 | 365 | Fine Anno |

Come si può notare se non si sceglie nessuna delle opzioni riguardanti la gestione dei trasferimenti i 0.25 BTC che ho trasferito sul Wallet **B** risultano come fossero sempre appartenuti ad esso.

### Calcoli con biffata l’opzione per mantenere il valore iniziale sul Wallet di origine {#calcoli-con-biffata-lopzione-per-mantenere-il-valore-iniziale-sul-wallet-di-origine}

![Opzione: mantieni il valore iniziale sul wallet di origine](immagini/opzioni-calcolo-rw/08.png)

Questa la situazione riguardo i quadri RW per i 2 wallet

| **Wallet di riferimento** | **Valore Iniziale** | **Valore Finale** | **Giorni di Detenzione** | **IC Calcolata** |
|---|---|---|---|---|
| 01 ( **A** ) | € 15504.13 | € 28729.32 | 365.00 | € 57.46 |
| 02 ( **B** ) | € 1121.06 | € 11643.64 | 365.00 | € 23.29 |

IC TOTALE : **€ 80.75**

Questo invece il dettaglio della composizione del Wallet **A**

| **Wallet Iniziale** | **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Wallet Finale** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 01 ( **A** ) | BTC | 0.75 | 2023-01-01 | € 11628.10 | 01 ( **A** ) | BTC | 0.75 | 2023-12-31 | € 28729.32 | 365 | Fine Anno |

E quindi il dettaglio del Wallet **B**

| **Wallet Iniziale** | **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Wallet Finale** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 01 ( **A** ) | BTC | 0.25 | 2023-01-01 | € 3876.03 | 02 ( **B** ) | BTC | 0.25 | 2023-12-31 | € 9576.44 | 365 | Fine Anno |
| 02 ( **B** ) | ETH | 1 | 2023-01-01 | € 1121.06 | 02 ( **B** ) | ETH | 1 | 2023-12-31 | € 2067.20 | 365 | Fine Anno |

In questo caso il valore iniziale resta sempre sul Wallet di origine mentre il valore finale e i giorni di detenzione sul wallet di destinazione.

Nel rigo RW del Wallet **A** il valore iniziale corrisponde infatti al valore dell’intero BTC che possedevo all’1/1 mentre il valore finale è relativo alla sola parte detenuta a fine anno ovvero 0,75.

Discorso analogo per il Wallet **B** il valore iniziale è relativo al solo ETH detenuto ad inizio anno mentre il valore finale è la somma dei 0,25 BTC spostati e l’ETH rimanente.

I colori nella tabella sono indicativi di cosa andrà nel rigo del Wallet **A** e cosa andrà nel rigo del Wallet **B**.

Se il wallet di origine ha dei [periodi di detenzione](#periodi-di-detenzione), il valore iniziale va nel
rigo del periodo che contiene la data di origine della moneta trasferita.

### Calcoli con biffata l’opzione per chiudere ed aprire un nuovo rigo W/RW in caso di trasferimenti {#calcoli-con-biffata-lopzione-per-chiudere-ed-aprire-un-nuovo-rigo-wrw-in-caso-di-trasferimenti}

![Opzione: chiudi e apri un nuovo rigo in caso di trasferimento](immagini/opzioni-calcolo-rw/09.png)

In questo caso ogni volta che sono di fronte ad un trasferimento di fondi tra un wallet ed un altro di mia proprietà chiuderò la posizione nel wallet di origine per aprirne una nuova nel wallet di destinazione.

Partendo sempre dal nostro esempio questa sarà la situazione riguardo i quadri RW per i 2 wallet

| **Wallet di riferimento** | **Valore Iniziale** | **Valore Finale** | **Giorni di Detenzione** | **IC Calcolata** |
|---|---|---|---|---|
| 01 ( **A** ) | € 15504.13 | € 35726.60 | 328.96 | € 64.40 |
| 02 ( **B** ) | € 8118.34 | € 11643.64 | 216.96 | € 13.84 |

IC TOTALE : **€ 78.24**

Questo il dettaglio della composizione del Wallet **A**

| **Wallet Iniziale** | **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Wallet Finale** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 01 ( **A** ) | BTC | 0.25 | 2023-01-01 | € 3876.03 | 01 ( **A** ) | BTC | 0.25 | 2023-06-30 | € 6997.28 | 181 | Trasferimento su altro Wallet |
| 01 ( **A** ) | BTC | 0.75 | 2023-01-01 | € 11628.10 | 01 ( **A** ) | BTC | 0.75 | 2023-12-31 | € 28729.32 | 365 | Fine Anno |

E quindi il dettaglio del Wallet **B**

| **Wallet Iniziale** | **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Wallet Finale** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 01 ( **B** ) | BTC | 0.25 | 2023-06-30 | € 6997.28 | 02 ( **B** ) | BTC | 0.25 | 2023-12-31 | € 9576.44 | 185 | Fine Anno |
| 02 ( **B** ) | ETH | 1 | 2023-01-01 | € 1121.06 | 02 ( **B** ) | ETH | 1 | 2023-12-31 | € 2067.20 | 365 | Fine Anno |

Come si può notare i 0,25 BTC spostati in data 30/06/2023 generano le seguenti posizioni sui 2 Wallet :

Sul Wallet A → 0.25 BTC posseduti dal 01/01/2023 al 30/06/2023 (181gg)

Sul Wallet B → 0.25 BTC posseduti dal 30/06/2023 al 31/12/2023 (185gg)

Questo andrà ovviamente ad incidere sul calcolo dell’IC perché inciderà anche il valore di BTC al momento dello spostamento.

L’IC infatti passa dagli **€ 80.75** dei precedenti esempi ai **€ 78.24** attuali.

## Valorizzazione degli Staking, Airdrop, Cashback e Reward varie {#valorizzazione-degli-staking-airdrop-cashback-e-reward-varie}

In “**Opzioni** – **Opzioni Rewards**” sarà possibile scegliere per ogni tipologia di reward se si tratta o meno di un “**provento da detenzione**”.

![Tabella delle opzioni Rewards](immagini/opzioni-calcolo-rw/10.png)

**Cosa significa questo?**

Significa che il provento viene tassato al momento della ricezione sull’intero valore e quello poi diventa il nuovo costo di carico per le future vendite.

Esempio :

Il 03/10/2023 riceviamo 0,01 ETH come provento da Staking del valore di € 15,83.

Quel valore genera immediatamente una plusvalenza pari a € 15,83 e diventa il nuovo costo di carico per quei 0,01 ETH.

Nel caso in cui invece si decidesse che quel tipo di Reward non debba essere trattata come “provento da detenzione” semplicemente il costo di carico della Reward sarà pari a Zero.

**Per quanto riguarda il calcolo sull’RW invece come si comporta il programma?**

Partendo dall’esempio precedente supponiamo di avere una situazione di questo genere :

| **Data** | **Tipo Scambio** | **Mon. Uscita** | **Qta Uscita** | **Mon. Entrata** | **Qta Entrata** | **Valore Transazione** | **Costo di Carico** | **Plusvalenza** |
|---|---|---|---|---|---|---|---|---|
| 2022-07-16 | ACQUISTO CRYPTO | EUR | -1241.60 | ETH | 1 | € 1241.60 | € 1241.60 | € 0.00 |
| 2023-10-03 | STAKING REWARD |  |  | ETH | 0.01 | € 15.83 | € 15.83 | € 15.83 |

In questo esempio mi ritrovo ad inizio e fine anno a detenere quanto segue :

01/01/2023 → 1.00 ETH del valore di € 1121.06

31/12/2023 → 1.01 ETH del valore di € 2087.87

**Caso 1 - La reward è classificata come “provento da detenzione”**

Quadro RW aggregato:

| **Wallet di riferimento** | **Valore Iniziale** | **Valore Finale** | **Giorni di Detenzione** | **IC Calcolata** |
|---|---|---|---|---|
| 01 (A) | € **1136.89** | € 2087.87 | 362.28 | € 4.14 |

Dettaglio delle movimentazioni :

| **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|
| ETH | 1 | 2023-01-01 | € 1121.06 | ETH | 1 | 2023-12-31 | € 2067.20 | 365 | Fine Anno |
| ETH | 0.01 | 2023-10-03 | € **15.83** | ETH | 0.01 | 2023-12-31 | € 20.67 | 90 | Fine Anno |

**Caso 2 - la reward non è classificata come “provento da detenzione” o è stata biffata la seguente opzione**

![Opzione sulle reward fino al 31/12/2022](immagini/opzioni-calcolo-rw/11.png)

Quadro RW aggregato:

| **Wallet di riferimento** | **Valore Iniziale** | **Valore Finale** | **Giorni di Detenzione** | **IC Calcolata** |
|---|---|---|---|---|
| 01 (A) | € **1121.06** | € 2087.87 | 362.28 | € 4.14 |

Dettaglio delle movimentazioni :

| **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|
| ETH | 1 | 2023-01-01 | € 1121.06 | ETH | 1 | 2023-12-31 | € 2067.20 | 365 | Fine Anno |
| ETH | 0.01 | 2023-10-03 | € **0.00** | ETH | 0.01 | 2023-12-31 | € 20.67 | 90 | Fine Anno |

Come si può vedere l’unica cosa che cambia è che nel primo caso il valore della reward al momento della ricezione viene aggiunto al valore iniziale del quadro W/RW mentre nel secondo caso no.

## Gestione del LiFo nel Quadro W/RW {#gestione-del-lifo-nel-quadro-wrw}

L’ultima opzione di calcolo disponibile è quella relativa a come il programma utilizza il LiFo per il calcolo dell’RW.

Di default il programma utilizza il LiFo sul singolo gruppo wallet quindi ad esempio per trovare quale BTC sto vendendo vado a cercare l’ultimo BTC acquistato (scambiato, regalato etc...) nel solo Gruppo Wallet su cui sto effettuando la vendita.

Viceversa biffando la seguente opzione vado a cercare l’ultimo BTC entrato sull’intero gruppo dei Wallet a mia disposizione.

![Opzione: LIFO applicato alla totalità dei wallet](immagini/opzioni-calcolo-rw/12.png)

**Come si traduce questo sul quadro RW?**

Per rispondere a questa domanda il modo più semplice è come al solito fare un esempio.

Supponiamo di partire da questa situazione:

| **Data** | **Wallet** | **Tipo Scambio** | **Mon. Uscita** | **Qta Uscita** | **Mon. Entrata** | **Qta Entrata** | **Valore Transazione** | **Costo di Carico** |
|---|---|---|---|---|---|---|---|---|
| 2022-07-18 | A | ACQUISTO CRYPTO | EUR | -20824.92 | BTC | 1 | € 20824.92 | € 20824.92 |
| 2022-07-18 | B | ACQUISTO CRYPTO | EUR | -20824.92 | BTC | 1 | € 20824.92 | € 20824.92 |
| 2023-05-15 | B | ACQUISTO CRYPTO | EUR | -24784.13 | BTC | 1 | € 24784.13 | € 24784.13 |
| 2023-09-01 | A | VENDITA CRYPTO | BTC | -0.5 | EUR | 12003.80 | € 12003.80 |  |

La situazione ad inizio e fine 2023 quindi sarà la seguente

| **Wallet** | **Cripto Inizio Anno** | **Qta Inizio Anno** | **Valore Inizio Anno** | **Cripto Fine Anno** | **Qta Fine Anno** | **Valore Fine Anno** |
|---|---|---|---|---|---|---|
| A | BTC | 1 | € 15428.81 | BTC | 0,5 | € 19152.88 |
| B | BTC | 1 | € 15428.81 | BTC | 2 | € 76611.53 |

La vendita dei 0,5 BTC del Wallet A riguarderà di default i BTC di inizio anno dello stesso Wallet mentre se si vuole considerare il tutto come un unico grande wallet (biffando l’opzione apposita) il BTC che andrò a vendere sarà quello acquistato sul Wallet B il 15/05/2023.

**NB.** Il programma in ogni caso non ragiona mai per Singolo Wallet ma per Gruppi di Wallet.

I Gruppi sono quelli scelti in “**Opzioni**” – “**Gruppi Wallet Crypto**”.

I Wallet appartenenti allo stesso Gruppo Wallet vengono visti dal programma come un unica entità.

### Default : LiFo sul singolo Gruppo Wallet {#default--lifo-sul-singolo-gruppo-wallet}

Con le opzioni di default il risultato è quanto segue:

Quadro RW aggregato:

| **Wallet di riferimento** | **Valore Iniziale** | **Valore Finale** | **Giorni di Detenzione** | **IC Calcolata** |
|---|---|---|---|---|
| 01 ( A ) | € 15428.82 | € 31156.68 | 318.38 | 54.35 |
| 02 ( B ) | € 40212.94 | € 76611.52 | 298.00 | 125.10 |

IC Totale = **€ 179.45**

Dettaglio delle movimentazioni Wallet A :

| **Wallet Iniziale** | **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Wallet Finale** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 01 ( A ) | BTC | 0.5 | 2023-01-01 | € 7714.41 | 01 ( A ) | BTC | 0.5 | 2023-09-01 | € 12003.80 | 244 | Vendita |
| 01 ( A ) | BTC | 0.5 | 2023-01-01 | € 7714.41 | 01 ( A ) | BTC | 0.5 | 2023-12-31 | € 19152.88 | 365 | Fine Anno |

Dettaglio delle movimentazioni Wallet B :

| **Wallet Iniziale** | **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Wallet Finale** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 02 ( B ) | BTC | 1 | 2023-01-01 | € 15428.81 | 02 ( B ) | BTC | 1 | 2023-12-31 | € 38305.76 | 365 | Fine Anno |
| 02 ( B ) | BTC | 1 | 2023-05-15 | € 24784.13 | 02 ( B ) | BTC | 1 | 2023-12-31 | € 38305.76 | 231 | Fine Anno |

Come si vede il mezzo BTC venduto sul Wallet A è quello di inizio anno dello stesso Wallet

### Opzionale : LiFo applicato alla totalità dei Wallet {#opzionale--lifo-applicato-alla-totalità-dei-wallet}

![Opzione: LIFO applicato alla totalità dei wallet](immagini/opzioni-calcolo-rw/13.png)

Con questa opzione biffata il risultato sarà invece il seguente

Quadro RW aggregato:

| **Wallet di riferimento** | **Valore Iniziale** | **Valore Finale** | **Giorni di Detenzione** | **IC Calcolata** |
|---|---|---|---|---|
| 01 ( A ) | € 24784.14 | € 31156.68 | 184.38 | 31.48 |
| 02 ( B ) | € 30857.62 | € 76611.52 | 365.00 | 153.22 |

IC Totale : **€ 184.70**

Dettaglio delle movimentazioni Wallet A :

| **Wallet Iniziale** | **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Wallet Finale** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 02 ( B ) | BTC | 0.5 | 2023-05-15 | 12392.07 | 01 ( A ) | BTC | 0.5 | 2023-09-01 | 12003.80 | 110 | Vendita |
| 02 ( B ) | BTC | 0.5 | 2023-05-15 | 12392.07 | 01 ( A ) | BTC | 0.5 | 2023-12-31 | 19152.88 | 231 | Fine Anno |

Dettaglio delle movimentazioni Wallet B :

| **Wallet Iniziale** | **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Wallet Finale** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 01 ( A ) | BTC | 1 | 2023-01-01 | 15428.81 | 02 ( B ) | BTC | 1 | 2023-12-31 | 38305.76 | 365 | Fine Anno |
| 02 ( B ) | BTC | 1 | 2023-01-01 | 15428.81 | 02 ( B ) | BTC | 1 | 2023-12-31 | 38305.76 | 365 | Fine Anno |

Come si può notare si formano delle aberrazioni nei calcoli, monete partite dal Wallet B le ritrovo a fine anno nel Wallet A e viceversa questo appunto perché il LiFo viene applicato alla totalità dei wallet ma i Wallet poi devono essere distinti per l’RW.

Anche l’IC totale sarà diversa perché cambia il periodo di detenzione e l’IC viene rapportato ad esso.

### Opzionale : LiFo applicato anche ai SubMovimenti {#opzionale--lifo-applicato-anche-ai-submovimenti}

Per poter spiegare bene questa opzione bisogna prima partire da un esempio più semplice.

Supponiamo di avere la seguente situazione :

**A** - 01/01/2023 → **Acquisto 1 ETH** per **1000 EUR** (Residuo Wallet 1 ETH)

**B** - 01/02/2023 → **Acquisto 1 ETH** per **2000 EUR** (Residuo Wallet 2 ETH)

**C** - 01/03/2023 → **Vendo 2 ETH** per **0,2BTC** (Residuo Wallet 0,2 BTC)

**D** - 01/04/2023 → **Vendo 0,15 BTC** per **10000 EUR** (Residuo Wallet 0,05 BTC)

Nella modalità di Default, il programma, nel momento in cui gestisco il movimento **C**, prende e mette nello stack relativo ai 0,2 BTC acquistati l’origine del suo dato ( i 2 acquisti di ETH ) così come sono (seguendo il LiFo) e stessa cosa nel momento della vendita dei 0,15 BTC.

![Opzione sui sub-movimenti non attiva](immagini/opzioni-calcolo-rw/14.png)

**Default** (opzione non biffata) esempio grafico :

![Schema del LIFO sui soli movimenti reali](immagini/opzioni-calcolo-rw/15.gif)

**Default** (opzione non biffata) Tabella di dettaglio :

| **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|
| ETH | 0,5 | 2023-01-01 | € 1000.00 | BTC | 0.05 | 2023-04-01 | € 3333.33 | 91 | Vendita |
| ETH | 1 | 2023-02-01 | € 1000.00 | BTC | 0.1 | 2023-04-01 | € 6666.67 | 60 | Vendita |
| ETH | 0,5 | 2023-01-01 | € 1000.00 | BTC | 0.05 | 2023-12-31 | € 1915.29 | 365 | Fine Anno |

**Default** (opzione non biffata) Quadro RW aggregato:

| **Wallet di riferimento** | **Valore Iniziale** | **Valore Finale** | **Giorni di Detenzione** | **IC Calcolata** |
|---|---|---|---|---|
| 01 (A) | € 3000.00 | € 11915.29 | 117.70 | € 7.68 |

Nel momento in cui scelgo invece di utilizzare questa opzione, il programma è come se dividesse ogni singolo movimento reale in molti sub-movimenti e su ognuno di questi ci applica il LiFo.

![Opzione sui sub-movimenti attiva](immagini/opzioni-calcolo-rw/16.png)

(Opzione biffata) esempio grafico :

![Schema del LIFO applicato ai sub-movimenti](immagini/opzioni-calcolo-rw/17.gif)

(Opzione biffata) Tabella di dettaglio :

| **Moneta Iniziale** | **Qta Iniziale** | **Data Inizio Detenzione** | **Valore Inizio Detenzione** | **Moneta Finale** | **Qta Finale** | **Data Fine Detenzione** | **Valore Fine detenzione** | **GG di detenzione** | **Causale** |
|---|---|---|---|---|---|---|---|---|---|
| ETH | 0,5 | 2023-02-01 | € 1000.00 | BTC | 0.05 | 2023-04-01 | € 3333.33 | 60 | Vendita |
| ETH | 1 | 2023-01-01 | € 1000.00 | BTC | 0.1 | 2023-04-01 | € 6666.67 | 91 | Vendita |
| ETH | 0,5 | 2023-02-01 | € 1000.00 | BTC | 0.05 | 2023-12-31 | € 1915.29 | 334 | Fine Anno |

(Opzione biffata) Quadro RW aggregato:

| **Wallet di riferimento** | **Valore Iniziale** | **Valore Finale** | **Giorni di Detenzione** | **IC Calcolata** |
|---|---|---|---|---|
| 01 (A) | € 3000.00 | € 11915.29 | 121.39 | € 7.93 |

Questa tra l’altro sembrerebbe essere, al netto di errori di valutazione, la modalità che viene utilizzata anche da Tatax per i calcoli.

Come si può notare questa opzione può creare delle situazioni un po' strane, se ad esempio prima di vendere per euro converto i BTC in WBTC succederà che verranno nuovamente invertiti i movimenti originali portando a valori diversi di IC e giorni di detenzione (in particolari saranno uguali a quelli del primo esempio), questo pur avendo BTC e WBTC stesso identico valore.

Ricapitolando:

- **Default** (Opzione non barrata)→ LiFo applicato ai soli movimenti Reali.

- Con Opzione Biffata → LiFo applicata a tutti i sub-movimenti.

## Periodi di detenzione: un rigo per periodo {#periodi-di-detenzione}

Di norma un gruppo wallet produce **un rigo per anno**. A volte però nel corso dell’anno cambia qualcosa che
rende sbagliato un rigo unico: l’intermediario inizia (o smette) di applicare l’imposta di bollo, l’exchange
trasferisce i conti a una società di un altro Stato, un conto viene chiuso e poi riaperto. Per questi casi
ogni gruppo può avere dei **periodi di detenzione**, e il quadro produce **un rigo per ciascun periodo**.

I periodi si gestiscono da **Opzioni** – **Gruppi Wallet Crypto**: si seleziona il gruppo e si preme
**Periodi di detenzione e dati fiscali**.

![I periodi di detenzione di un gruppo](immagini/opzioni-calcolo-rw/18.png)

Ogni riga è un periodo, di tipo **CRYPTO** (le cripto-attività) oppure **FIAT** (la
[liquidità](#liquidita-presso-intermediari-esteri)), e i due tipi sono indipendenti. Con **Aggiungi** e
**Modifica** si apre la finestra di dettaglio:

![Modifica di un periodo di detenzione](immagini/opzioni-calcolo-rw/19.png)

- **Data inizio** e **Data fine**. Una data fine vuota indica un periodo ancora aperto. Una data inizio vuota
  viene **dedotta**: il periodo comincia il giorno dopo la fine del periodo precedente dello stesso tipo e,
  se non ce n’è nessuno, dal primo movimento del gruppo. È la situazione tipica di un cambio: un periodo
  chiuso il 30/06 e il successivo senza date.
- **Calcolo iniziale** e **Calcolo finale**: come valutare la giacenza al bordo del periodo (vedi più sotto).
- **Bollo exchange** (solo CRYPTO): in quel periodo l’intermediario ha già assolto l’imposta di bollo.
- **Stato estero**, **È conto corrente estero**, **Identificativo fiscale**, **Identificativo ISEE**,
  **Note** e **Fonte** (solo FIAT): i dati dell’intermediario per la parte liquidità.

Due periodi dello stesso tipo non possono sovrapporsi: il programma lo segnala come errore e non salva. Un
**buco** fra due periodi è invece ammesso (un conto chiuso e riaperto) e viene solo segnalato. Ogni
operazione viene salvata subito, senza un pulsante *Salva*. **Ripristina riga** e **Ripristina tutto**
riportano i periodi ai valori predefiniti del programma: per esempio per il gruppo preconfigurato di Binance
(*Wallet 101*) vengono proposti due periodi cripto, con il bollo pagato dall’intermediario fino al 30/06/2026 e non più dal 01/07/2026. La colonna
**Origine** indica se una riga è quella predefinita, se è stata modificata o se è stata aggiunta a mano.

### Il risultato nel quadro {#periodi-il-risultato-nel-quadro}

Nell’esempio il gruppo *Binance* ha due periodi cripto, con il bollo pagato fino al 30/06/2025. Il quadro
2025 mostra due righi distinti, con le date del periodo accanto al nome:

![Due righi per lo stesso gruppo, uno per periodo](immagini/opzioni-calcolo-rw/20.png)

Il primo rigo (bollo pagato) ha imposta zero, il secondo no. Selezionando un rigo, il **Dettaglio RW**
mostra soltanto le righe di quel periodo:

![Il dettaglio del secondo periodo](immagini/opzioni-calcolo-rw/21.png)

Un gruppo **senza periodi**, o con un solo periodo che copre tutto l’anno, produce esattamente il rigo unico
di sempre.

### Come vengono calcolati i righi di periodo {#periodi-come-vengono-calcolati}

- **Il rigo si spezza a ogni confine di periodo** che cade dentro l’anno, anche fra due periodi con le
  stesse impostazioni.
- **Opzioni B, C e D.** Al confine le posizioni ancora aperte del gruppo vengono chiuse alle 23:59 del giorno
  prima e riaperte alle 00:00 del giorno del confine, allo stesso prezzo di mercato: il valore finale di un
  periodo è quindi il valore iniziale del successivo, e nessuna riga di dettaglio scavalca un confine. Vale
  anche con l’opzione “LiFo applicato alla totalità dei Wallet”.
- **Opzione A** (e periodi col bollo con la casella “mostra solo giacenza”). Ogni periodo è una fotografia:
  giacenza all’inizio del primo giorno e alla fine dell’ultimo, con le
  [regole della fotografia](#regole-della-fotografia-di-inizio-e-fine-anno). L’inizio di un periodo
  successivo funziona come un inizio d’anno: conta la giacenza alle 00:00, non i movimenti del giorno.
- **Buchi.** Un tratto dell’anno non coperto da nessun periodo diventa un rigo a sé, con il bollo indicato
  per il gruppo nella tabella dei gruppi.
- **Imposta.** Si calcola su ogni rigo con la stessa formula del rigo unico (valore finale / 365 × giorni ×
  2‰).

### Calcolo iniziale e calcolo finale {#periodi-calcolo-iniziale-e-finale}

Per default il valore al bordo di un periodo è **solo il residuo**: la giacenza a inizio giornata per il
valore iniziale, a fine giornata per il valore finale. In alternativa, sui confini fissati dall’utente:

- **Calcolo iniziale**: *primo apporto della giornata + residuo* oppure *somma degli apporti della giornata +
  residuo*.
- **Calcolo finale**: *ultima uscita della giornata + residuo* oppure *somma delle uscite della giornata +
  residuo*.

Gli apporti e le uscite del giorno sono presi nell’ordine dei movimenti, esclusi i trasferimenti interni, e
valorizzati al valore del loro movimento, il residuo al prezzo del confine. Per le cripto queste modalità
hanno effetto sulle **fotografie** (opzione A e periodi col bollo con “mostra solo giacenza”): con le opzioni
B, C e D un apporto del giorno di confine è già un rigo aperto al valore del suo movimento.

## Opzione : Se Bollo Pagato mostra solo le giacenze di inizio e fine anno {#opzione--se-bollo-pagato-mostra-solo-le-giacenze-di-inizio-e-fine-anno}

![Opzione: se il bollo è pagato mostra solo le giacenze](immagini/opzioni-calcolo-rw/22.png)

Se questa opzione viene biffata e un gruppo ha il bollo già pagato dall’intermediario (colonna **Bollo
Pagato dall’Intermediario** in **Opzioni** – **Gruppi Wallet Crypto**, oppure il campo **Bollo exchange** dei
suoi periodi), per quel gruppo non verranno mostrati i valori calcolati ma bensì le giacenze di inizio e fine
anno.

In sostanza per quel gruppo e solo per quello si seguono le **regole** dell’opzione **A** descritte sopra.

Se il gruppo ha dei [periodi di detenzione](#periodi-di-detenzione), la regola vale **periodo per periodo**:
i periodi col bollo pagato mostrano le giacenze di inizio e fine periodo, gli altri i valori calcolati con
l’opzione scelta.

In ogni caso un rigo col bollo pagato ha **imposta zero** e non entra nel totale **IC Dovuta Totale**. In
stampa i suoi giorni di detenzione seguono regole diverse nei due quadri:

- **Quadro RW**: restano vuoti quando il gruppo ha detenuto per tutto il periodo (l’anno intero, o l’intero
  periodo di detenzione), altrimenti vengono indicati tra parentesi con un asterisco.
- **Quadro W**: restano vuoti quando è attiva questa casella oppure l’opzione A, altrimenti vengono indicati
  tra parentesi con un asterisco.

## Liquidità presso intermediari esteri {#liquidita-presso-intermediari-esteri}

Oltre alle cripto-attività il quadro può comprendere la **liquidità** (euro e altre valute) detenuta presso
un intermediario estero, per esempio il saldo in euro di un exchange con sede all’estero. È una parte
facoltativa, attivata dalla casella in **Opzioni** – **Opzioni Calcolo RW/W**:

![Le opzioni sulla liquidità](immagini/opzioni-calcolo-rw/23.png)

**Come si compone il rigo.** Il programma ricava il saldo in valuta di ogni gruppo dai movimenti (depositi e
prelievi di valuta, acquisti e vendite di cripto con valuta) e produce **un rigo per gruppo**, o uno per
ciascun periodo **FIAT** del gruppo. Il valore è il saldo in euro più il controvalore delle altre valute al
cambio della Banca d’Italia. Oggi è gestito il dollaro USA: per le altre valute il rigo riporta un avviso. La
data di apertura di un gruppo nato durante l’anno è quella del suo primo movimento. Un saldo negativo vale
zero e viene segnalato. Un rigo con valore iniziale e finale entrambi a zero non viene prodotto (salvo per i conti
correnti, dove conta la giacenza media).

**I dati dell’intermediario** si inseriscono in un periodo di tipo FIAT del gruppo:

![I dati fiscali nel periodo FIAT](immagini/opzioni-calcolo-rw/24.png)

![Modifica del periodo FIAT](immagini/opzioni-calcolo-rw/25.png)

- **Stato estero**: il codice dello Stato dell’intermediario, scelto dall’elenco ufficiale delle istruzioni di
  Redditi PF. La voce speciale **Italia** in cima all’elenco indica un conto presso un intermediario
  italiano: per quel periodo non viene prodotto nessun rigo, perché il quadro RW riguarda solo le attività
  estere. Per molti exchange i dati sono già compilati, ma vanno comunque verificati.
- **È conto corrente estero**: da impostare a *SI* solo per un vero conto corrente bancario.
- **Identificativo fiscale** e **Identificativo ISEE**: gli estremi dell’intermediario, riportati nella stampa
  e utili per la DSU.

**Codice bene e imposta.**

- **Liquidità** (codice bene **14**): a scelta dell’utente, con i due pulsanti della sezione, si liquida
  l’**IVAFE ordinaria dello 0,20%** oppure si dichiara in **solo monitoraggio** (colonna 16 barrata, senza
  imposta). Per gli Stati a fiscalità privilegiata (D.M. 4 maggio 1999) dall’anno d’imposta 2024 l’aliquota
  è il **4‰**, e la barratura della colonna 21 va riportata a mano.
- **Conto corrente estero** (codice bene **1**): IVAFE in misura fissa di **34,20 €**, rapportata ai giorni
  di detenzione. Come valore si indica la giacenza **media annua**: la somma dei saldi giornalieri divisa
  sempre per **365**, anche se il conto è rimasto aperto solo per una parte dell’anno (è la definizione
  dell’Agenzia delle entrate, provvedimento del 28 maggio 2015). Un conto con 9.000 € dal 1° luglio ha quindi
  una media di 9.000 × 184 / 365 = 4.536,99 € e non paga l’imposta. L’imposta non è dovuta se la
  giacenza media non supera i **5.000 €**, ma il rigo resta comunque per il monitoraggio (obbligatorio se il
  valore massimo supera i 15.000 €).

**Valore finale: giacenza media o saldo di fine periodo.** Con la casella *Sulla liquidità diversa dai conti
correnti il valore finale è il maggiore fra la giacenza media del periodo e il saldo a fine periodo*
(spenta di default) anche i righi con codice bene **14** riportano come valore finale il **maggiore** fra la
giacenza media e il saldo all’ultimo giorno del periodo. La giacenza media è quella del **periodo di
detenzione** del rigo: la somma dei saldi giornalieri divisa per i **giorni del rigo**, gli stessi che il
quadro riporta in colonna 10. Per esempio, un conto aperto il 1° ottobre con 10.000 € e sceso a 1.000 € il
1° dicembre ha 92 giorni di detenzione e una giacenza media di (61 × 10.000 + 31 × 1.000) / 92 = 6.967,39 €,
che supera il saldo finale di 1.000 €. Il valore così ottenuto è anche la base dell’IVAFE dello 0,20%, che
poi si rapporta ai giorni: 6.967,39 × 0,20% × 92/365 = 3,51 €.

Non è la giacenza media **annua** della DSU (somma dei saldi divisa per 365 comunque), che serve all’ISEE.
Usata qui conterebbe i giorni due volte, una nella media e una nell’imposta, e dividere un rigo in due
farebbe pagare meno: 10.000 € tenuti tutto l’anno pagano 20 €, e devono pagare 20 € anche se il rigo è
diviso in due periodi da 180 e 185 giorni. 
Il rigo riporta un avviso quando prevale la media, il saldo reale resta visibile nel dettaglio. 
Con l’opzione attiva compare anche il rigo di un conto aperto e svuotato nel corso dell’anno, che
altrimenti avrebbe inizio e fine a zero. Un periodo con **valore finale inserito a mano** non viene toccato, e
i conti correnti non cambiano: per loro il valore è già la giacenza media.

**Un rigo nuovo a ogni apporto di capitale.** La circolare dell’Agenzia delle entrate **12/E dell’8 aprile
2016, § 14.1** chiede che, all’interno di un unico rapporto, un **apporto di capitale** (per esempio un
versamento di contanti) divida la dichiarazione in due righi: il primo con il valore iniziale e il valore
immediatamente prima dell’apporto, il secondo con il valore subito dopo l’apporto e il valore finale. Le
semplici compravendite all’interno del rapporto non contano. Con la casella *Un deposito FIAT superiore alla
soglia chiude il rigo il giorno prima e ne apre uno nuovo* (spenta di default) il programma fa esattamente
questo:

- conta **solo la nuova liquidità**, cioè i **depositi FIAT**. La vendita di una cripto porta euro sul conto
  ma non è un apporto, e non divide il rigo. Per Crypto.com App conta solo il *bonifico in ingresso* del
  Fiat Wallet (non le vendite, non il saldo di apertura inserito a mano)
- il deposito divide il rigo solo se il suo controvalore in euro **supera** la soglia scritta nel campo a
  fianco (**500 €** di default, si può cambiare e si conferma con Invio o uscendo dal campo). Un deposito
  pari alla soglia non divide
- il rigo precedente finisce il **giorno prima** del deposito, con il saldo che c’era subito prima del
  deposito (compresi i movimenti dello stesso giorno venuti prima), e il rigo nuovo comincia il **giorno del
  deposito**, con il saldo subito dopo. Ogni rigo ha i suoi giorni di detenzione e la sua IVAFE, e porta un
  avviso che indica il deposito che lo ha aperto o chiuso
- un deposito nel primo giorno del periodo (per esempio quello che apre il conto) non divide nulla, e in
  una stessa giornata il rigo si divide una volta sola

Per esempio, 1.000 € tutto l’anno e un bonifico di 2.000 € il 10 giugno 2025 danno due righi: dal 1° gennaio
al 9 giugno (160 giorni, valore finale 1.000 €) e dal 10 giugno al 31 dicembre (205 giorni, valore iniziale e
finale 3.000 €).

La seconda casella, *Un prelievo FIAT superiore alla soglia chiude il rigo il giorno prima e ne apre uno
nuovo*, fa la stessa cosa sui **prelievi FIAT**, con una soglia propria. È una libertà in più e non un
obbligo: la circolare parla solo degli apporti, e dividere il rigo su un prelievo **alza** la base
dell’IVAFE, perché il valore più alto di prima del prelievo pesa per i suoi giorni invece di sparire.

Entrambe le caselle sono attive solo se sulla liquidità si liquida l’IVAFE: in **solo monitoraggio** non ci
sono giorni di detenzione da dichiarare e dividere il rigo non avrebbe senso. Non riguardano nemmeno i
**conti correnti**, per i quali il valore è già la giacenza media dell’anno.

Nella tabella del quadro i righi di liquidità compaiono dopo quelli cripto, con **Natura** *FIAT* e lo
**Stato estero** in colonna:

![Un rigo di liquidità nel quadro](immagini/opzioni-calcolo-rw/26.png)

L’IVAFE della liquidità **non** entra nel totale **IC Dovuta Totale** delle cripto-attività: il totale da
riportare è indicato nelle note di compilazione della stampa.

**Crypto.com App.** Per l’app di Crypto.com la liquidità in euro non viene ricavata dai movimenti cripto ma
dal **Fiat Wallet** (la scheda *Fiat Wallet Crypto.com*): il saldo del quadro è quindi quello verificabile in
quella scheda, ed è completo solo se il file del Fiat Wallet copre tutta la storia.

## Valori di inizio e fine anno {#valori-di-inizio-e-fine-anno}

I valori di inizio e fine anno (e quelli ai confini dei periodi) sono prezzi di mercato: il 31/12 di un anno
viene valorizzato alle 00:00 del 1° gennaio successivo. Alcune regole particolari:

- gli **e-money token** denominati in euro (per esempio EURe) valgono 1:1 con l’euro dalla data indicata in
  **Opzioni** – **E-Money Token (EMT)**.
- alcuni token equivalenti a una moneta principale (per esempio WETH su una rete secondaria rispetto a ETH)
  vengono prezzati come la moneta principale sugli exchange, a partire dalla data in cui il programma ha
  introdotto l’equivalenza. Per non cambiare le dichiarazioni già presentate l’equivalenza non si applica
  agli anni precedenti, salvo attivare in **Opzioni** – **Opzioni di Calcolo** la casella *Prezzi dagli
  exchange anche per gli anni già dichiarati*.
- un valore mancante o sbagliato si corregge dal dettaglio con **Modifica Valore Iniziale** o **Modifica
  Valore Finale** (vedi [Gestione degli errori](#gestione-degli-errori)).

## Stampa del quadro {#stampa-del-quadro}

Il pulsante **Stampa Report PDF** della scheda **RW/W** chiede quale quadro stampare:

![La scelta del quadro da stampare](immagini/opzioni-calcolo-rw/27.png)

Il **Quadro W** è per chi presenta il modello 730, il **Quadro RW** per chi presenta il modello Redditi
Persone Fisiche. Il report contiene un solo quadro per volta, con copertina, i righi del quadro, le note di
compilazione e il riepilogo delle opzioni scelte per il calcolo.

![Un foglio del quadro RW con due righi di periodo](immagini/opzioni-calcolo-rw/28.png)

- Ogni rigo riporta nella colonna di sinistra l’alias del gruppo, e i righi di periodo aggiungono le date
  (per esempio *Binance (dal 01/01 al 30/06)*).
- I righi con valore iniziale e finale entrambi a zero non vengono stampati.
- I righi di **liquidità** seguono quelli cripto, con la loro numerazione, il codice bene e lo Stato estero.
  Se per un gruppo manca lo Stato estero il programma lo segnala prima di stampare, e il rigo viene
  evidenziato nel report.

![I righi di liquidità in coda al quadro](immagini/opzioni-calcolo-rw/29.png)

Le **note di compilazione** stampate in coda ai quadri sono scaricate da internet e aggiornate a ogni avvio
del programma: una correzione alle istruzioni non richiede una nuova versione.

## Gestione degli Errori {#gestione-degli-errori}

Dopo il calcolo dell’ RW è possibile che vengano visualizzati diversi errori, senza la correzione degli stessi i risultati del quadro saranno anch’essi errati.

Di seguito verranno mostrati i possibili errori e le relative correzioni da effettuare.

### Giacenza Negativa {#giacenza-negativa}

![Errore di giacenza negativa](immagini/opzioni-calcolo-rw/30.png)

Per correggere la problematica è sufficiente premere il pulsante “**Correggi Errore**” alla fine della Tabella e si verrà reindirizzati alla Funzione “**Giacenze a Data**” nel rigo in cui la giacenza della crypto è diventata negativa, in questo caso è opportuno capirne il motivo e sistemare il movimento o aggiungerne uno in correzione, in caso di piccoli importi è possibile utilizzare il tasto in basso “**Sistema Qta residua**” che creerà un movimento fittizio per far tornare la giacenza del Token perlomeno a Zero.

Con l’opzione A e nelle fotografie dei gruppi col bollo una giacenza negativa non viene conteggiata: vale
zero e resta segnalata come errore finché non viene corretta.

### Movimento di apertura o chiusura non classificato {#movimento-di-apertura-o-chiusura-non-classificato}

![Errore: movimento di apertura o chiusura non classificato](immagini/opzioni-calcolo-rw/31.png)

In questo caso si tratta di movimenti di deposito o prelievo non classificati e che il programma quindi non sa come conteggiare correttamente.

Per risolvere premere il pulsante “**Correggi Errore**”, si aprirà una finestra in cui bisognerà indicare che tipo di movimento si sta gestendo.

Questo genere di errori possono essere evitati se si sistema prima il tutto nella funzione “**Classificazione Depositi/Prelievi**”.

### Valore iniziale o finale non valorizzato {#valore-iniziale-o-finale-non-valorizzato}

![Errore: valore iniziale o finale non valorizzato](immagini/opzioni-calcolo-rw/32.png)

In questo caso l’errore avviene perché il programma non è riuscito a valorizzare il token al momento dell’acquisto/vendita/fine o inizio anno (o al confine di un periodo di detenzione).

Per correggere l’errore premere il bottone “**Modifica Valore Iniziale**” o “**Modifica Valore Finale**” a seconda del caso.

Nel caso in cui il token non abbia prezzo perché ad esempio è SCAM è possibile confermare il prezzo a Zero.

[Torna all'indice della documentazione](./)
