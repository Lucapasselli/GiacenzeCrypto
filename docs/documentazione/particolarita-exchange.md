---
layout: default
title: Particolarità degli import per exchange
---

# Particolarità degli import per exchange

Ogni exchange esporta i propri dati a modo suo: alcuni movimenti stanno in un file a parte, alcuni
prodotti non compaiono nell'export principale, alcuni file vanno caricati in un ordine preciso. Questa
pagina raccoglie, exchange per exchange, quello che serve sapere **prima** di importare: quali file
scaricare, quali file accessori servono e quando, in che ordine caricarli e che cosa ne fa il programma.

Dalla finestra *Importazioni* il collegamento sotto la scelta dell'estrazione apre direttamente la
sezione dell'exchange selezionato.

Exchange e piattaforme: [Binance](#binance) · [Bitget](#bitget) · [Bybit](#bybit) ·
[Bybit EU](#bybit-eu) · [Coinbase](#coinbase) · [Coinbase Pro](#coinbase-pro) ·
[CoinTracking](#cointracking) · [Crypto.com](#crypto-com) · [Gate.io](#gate-io) · [KuCoin](#kucoin) ·
[Nexo](#nexo) · [Nexo Pro](#nexo-pro) · [OKX](#okx) · [Tatax](#tatax)

> **NB** — come si scarica un file passo per passo, con le immagini delle schermate degli exchange, è
> spiegato nel manuale [Esportazione e importazione da CSV](export-import-csv.html). Qui si spiega
> invece *che cosa* scaricare e *perché*.

## Regole che valgono per tutti {#regole-generali}

- **Una sola fonte per ogni periodo.** Quando un exchange offre più strade per gli stessi movimenti
  (file delle transazioni, report annuale, API), per uno stesso periodo se ne usa una sola: due fonti
  diverse descrivono gli stessi movimenti in modo diverso e il programma non sempre le riconosce come
  doppie.
- **Prima il file principale, poi gli accessori.** I file accessori di solito non aggiungono movimenti
  nuovi ma completano o sistemano quelli già importati dal file principale, che quindi deve essere già
  in archivio.
- **Non rinominare i file scaricati** quando il nome contiene il fuso orario (Binance lo scrive nel
  nome): il programma lo usa per collocare i movimenti all'ora giusta. Fanno eccezione i file di
  dettaglio Auto-Invest di Binance (vedi [più sotto](#binance-auto-invest)).
- **Non aprire e risalvare i file** con un foglio di calcolo (Excel, LibreOffice, Google Sheets) o un
  editor prima di importarli: il salvataggio può cambiare date, numeri, separatori o codifica, e il file
  non viene più letto correttamente. Se è successo, si riscarica una copia nuova dall'exchange.
- **Scaricare subito i file generati**: molti exchange preparano l'export in un secondo momento e
  tengono il collegamento per lo scaricamento solo per un periodo limitato (per esempio 30 giorni), e
  quasi tutti limitano il numero di export che si possono generare ogni mese.
- **Reimportare lo stesso file non crea doppioni**: vengono aggiunti solo i movimenti che mancano.
  Con *Sovrascrivere movimenti già presenti* invece i movimenti già importati vengono riscritti come
  li descrive il file, e quelli che erano stati classificati a mano tornano da classificare (il
  resoconto di fine importazione li elenca).
- **Le voci in prova** compaiono nella finestra con la scritta *(ATTENZIONE : in fase ALFA)*: sono
  state verificate su pochi file reali, quindi dopo l'importazione conviene controllare il resoconto e le
  giacenze con più attenzione.
- **Se restano giacenze negative**, di solito manca una parte dei movimenti: un prodotto dell'exchange
  che non ha un import, oppure un periodo non coperto dai file scaricati.

## Binance {#binance}

Per Binance l'export principale non basta a ricostruire tutto: alcuni prodotti Earn lasciano tracce
incomplete nel file delle transazioni, e per sistemarli servono file di dettaglio che Binance fornisce
a parte. Le voci della finestra di importazione sono queste.

| Voce nella finestra | Che cosa importa | Quando serve |
|---|---|---|
| **CSV** | Il file delle transazioni: è la base | Sempre, in alternativa al Financial Report |
| **Financial Report** | Il report finanziario annuale | Al posto del CSV, mai insieme sullo stesso periodo |
| **Formato storico** | Lo stesso file delle transazioni, letto dal vecchio importatore | Per i nuovi import si usa *CSV*. Un periodo già importato con questa voce non va reimportato con *CSV* |
| **Simple Earn Flessibile - Real-time APR** | Le ricompense Real-time APR di Simple Earn | Se si è usato Simple Earn Flessibile |
| **Dettaglio Dual Investment** | Abbina acquisto e liquidazione dei contratti | Se si è usato Dual Investment, dopo il CSV |
| **Dettaglio Auto-Invest** | Ricostruisce i piani Auto-Invest | Se si è usato Auto-Invest, dopo il CSV |

In alternativa ai file, i movimenti si possono scaricare dalle API con il pulsante *Exchange API* della
scheda dei movimenti, con una chiave in **sola lettura** (vedi [più sotto](#binance-api)).

### Il file delle transazioni {#binance-transazioni}

È l'export *Storico Asset – Esporta i registri delle transazioni* del sito di Binance. Come
scaricarlo è spiegato nel [manuale con le schermate](export-import-csv.html#binance): va scelto il fuso
**UTC** e il formato **CSV**, e il file non va rinominato. Con *Personalizza* si può scegliere anche
tutta la storia del conto in un solo export, lasciando *Account* e *Coin* su tutti. Con moltissimi
movimenti (alcune decine di migliaia di righe) Binance può non riuscire a generare il file: in quel caso
si divide il periodo in più export di qualche anno ciascuno e li si importa tutti. Binance limita anche
il numero di export che si possono generare ogni mese.

Il file contiene scambi, conversioni, depositi e prelievi, interessi e bonus di Earn, airdrop e
distribuzioni, le spese con la carta Binance e i pagamenti Binance Pay. Alcune cose da sapere:

- **Gli spostamenti interni** (per esempio da Spot a Earn e ritorno) non vengono importati: per il
  programma Spot ed Earn sono lo stesso wallet.
- **Conversioni e piccoli saldi convertiti in BNB** a volte hanno le due gambe registrate a qualche
  secondo o minuto di distanza: il programma le riunisce in un unico scambio.
- **La carta Binance**: una spesa in euro diventa un prelievo fiat, una spesa pagata in cripto un
  prelievo da classificare, il cashback una ricompensa.
- **Binance Pay**: pagamenti e invii diventano depositi e prelievi da classificare.
- **Ridenominazioni e migrazioni di token** (MC in BEAMX, MATIC in POL) hanno l'uscita e l'entrata a
  ore di distanza: diventano un prelievo e un deposito da abbinare come scambio differito. Nella
  ridenominazione di BTT in BTTC del gennaio 2022 Binance registra il BTTC ricevuto come airdrop, e il
  programma lo importa come ricompensa: dal menu del movimento *Cambia Tipologia Reward – Deposito da
  classificare* lo riporta a deposito, poi il prelievo di BTT si classifica come scambio differito con
  quel deposito.
- **Dual Investment e Auto-Invest** compaiono nel file solo in parte, senza i dati che servono ad
  abbinarne i movimenti: per questi servono i file di dettaglio descritti più sotto.
- **I bonus di Simple Earn negli export dal 2026** sono scritti due volte, sul conto Spot e sul conto
  Earn ("Rewards Income"): il programma importa solo quelli del conto Spot. Se un import fatto con una
  versione precedente li ha portati in archivio tutti e due, la pulizia è in *Opzioni – Pulizie –
  Ricompense Binance doppie*.
- **Le operazioni dei bot di trading** si trovano solo in questo file, non nelle API.
- **P2P, Options e Alpha** hanno export a parte (*C2C History*, lo storico delle Options, quello di
  Binance Alpha) che il programma non legge: se li si è usati e dopo l'import mancano movimenti, vanno
  aggiunti a mano.

### Il Financial Report {#binance-financial-report}

È il report che Binance genera una volta all'anno (*Account – Report finanziari*). Contiene gli stessi
movimenti del file delle transazioni in un'altra forma, quindi è un'**alternativa**: per lo stesso
periodo si importa o l'uno o l'altro, mai entrambi.

### Lo scaricamento dalle API {#binance-api}

Con una chiave API in **sola lettura** (senza permessi di trading né di prelievo) inserita con il pulsante
*Exchange API* della scheda dei movimenti, il programma scarica i movimenti direttamente da Binance, senza file (il pulsante non c'è
nell'edizione distribuita dal Microsoft Store). A fine scaricamento confronta le giacenze dell'archivio
con i saldi attuali dell'account e mostra le monete che non coincidono. Le API di Binance però non
restituiscono:

- le operazioni dei **bot di trading**, di **margin** e dei **futures**,
- i pagamenti **Binance Pay** più vecchi di 18 mesi.

Chi ha usato i bot deve quindi importare Binance dal file delle transazioni. Come per il Financial
Report, per uno stesso periodo si usa una sola fonte.

> **NB** — Binance non permette di chiedere tutti gli scambi in una volta: le API li restituiscono solo
> coppia per coppia. Il programma cerca le coppie più probabili per il periodo, ma qualcuna può
> sfuggire. Nella scheda *Particolarità Binance* della finestra *Exchange API* si possono elencare le
> monete di cui si è sicuri di aver scambiato le coppie (per BTC/USDC vanno inserite sia BTC sia USDC):
> dallo scaricamento successivo il programma chiede anche i loro scambi.

### Simple Earn Flessibile: le ricompense Real-time APR {#binance-simple-earn}

Simple Earn Flessibile paga due tipi di ricompensa:

- il **Bonus Tiered APR**, che compare nel file delle transazioni ed è già importato da lì,
- la **Real-time APR**, che **non compare in nessuna riga** del file delle transazioni: matura dentro
  il prodotto ed esce solo insieme al riscatto, che il programma non importa perché è uno spostamento
  interno.

Senza le Real-time APR le ricompense risultano più basse del vero, e dopo un prelievo totale da
Binance compaiono **giacenze negative** pari alle ricompense mancanti.

**Dove si scarica**: lo stesso file si ottiene in due modi.

- **Dall'app di Binance**: *Assets – Earn – Flexible – Rewards*, icona di download.
- **Dal sito**: nel *Download Center*
  ([binance.com/en/my/download-center](https://www.binance.com/en/my/download-center)) si scelgono
  *Data Type* **Earn - Simple Earn**, *Type* **Flexible - Rewards**, il periodo (*Time Range*) e il
  formato **CSV**, poi **Generate**. Il periodo si sceglie liberamente: conviene prendere tutto quello
  in cui si è usato Simple Earn Flessibile, oppure dividerlo in più export e importarli tutti. Come per
  il file delle transazioni, Binance prepara il file e lo rende scaricabile più tardi dalla stessa
  pagina, e limita il numero di export che si possono generare ogni mese.

I due file sono identici: stesse colonne (*Time, Coin, Amount, Type*) e stesse righe. Cambia solo il
fuso orario, che il sito lascia scegliere e scrive nel nome del file, per esempio `(UTC+2)`. Conviene
scegliere **UTC+0**, come per il file delle transazioni. Dalla versione 1.0.66 va bene anche un altro
fuso, purché il file non venga rinominato. Le versioni precedenti, con un fuso diverso da UTC, spostano
le ricompense Real-time di qualche ora.

> **Attenzione** — con un altro *Type* il Download Center dà un altro storico di Simple Earn, con cinque colonne
> (data, prodotto, tipo, quantità, moneta) e i tipi *Subscribe*, *Redeem* e *Income*. Non contiene le
> Real-time APR e, importato con questa voce, viene scartato riga per riga come causale sconosciuta.
> Serve il tipo **Flexible - Rewards**.

Il file si importa con la voce *Simple Earn Flessibile - Real-time APR*, in qualunque momento rispetto
al file delle transazioni. Le righe Bonus Tiered APR dello stesso file vengono ignorate, perché sono già
in archivio.

Le Real-time APR nel file hanno solo il giorno, senza l'ora, e il giorno è sempre quello UTC, qualunque
fuso si sia scelto per l'export. Il programma le registra all'inizio di quella giornata (00:00 UTC), così
una ricompensa del 31 dicembre resta nell'anno giusto anche in ora italiana.

> **NB** — lo storico scaricabile dall'app può non arrivare agli anni più vecchi. Se dopo l'import
> restano giacenze negative su Binance, la parte mancante sono probabilmente ricompense di periodi che
> il file non copre, e vanno aggiunte a mano.

### Dual Investment {#binance-dual-investment}

Nel file delle transazioni un contratto Dual Investment lascia due righe, *Dual Savings Purchase*
(l'acquisto) e *Dual Savings Settlement* (la liquidazione), che diventano un prelievo e un deposito da
classificare. Il file non dice a quale contratto appartengono, e fra le due possono passare settimane o
mesi: abbinarle a mano è lungo e facile da sbagliare.

**Il file di dettaglio** si scarica dal sito di Binance: icona del portafoglio in alto a destra,
*Overview*, poi nella barra a sinistra *Orders – Earn History*, scheda *Dual Investment*, pulsante di
esportazione accanto al titolo, periodo e *Generate*. Si importa con la voce *Dettaglio Dual
Investment* **dopo** il file delle transazioni. Per ogni contratto il programma trova acquisto e liquidazione e li sistema:

- **liquidazione nella stessa moneta sottoscritta** (il caso più comune): l'importo sottoscritto si
  sposta sul sotto-wallet *Dual Savings* di Binance senza toccare il costo di carico, e solo
  l'eccedenza incassata diventa una ricompensa,
- **liquidazione in una moneta diversa**: acquisto e liquidazione diventano uno scambio fra le due
  monete.

Altre cose da sapere:

- un contratto **ancora aperto** non ha la liquidazione e resta da abbinare: basta ripassare il file
  dopo la scadenza,
- un contratto liquidato nella stessa moneta per un importo **inferiore** a quello sottoscritto non
  viene abbinato e resta da classificare a mano,
- annullando un movimento di un contratto abbinato, acquisto e liquidazione tornano come erano stati
  importati, e ripassando il file l'abbinamento si rifà,
- il Dual Investment è un **derivato**: il programma lo segnala con un avviso a fine importazione e
  nelle stampe dei quadri, perché lo calcola come se fosse una cripto-attività, cosa che non è
  corretta.

### Auto-Invest {#binance-auto-invest}

Nel file delle transazioni i piani Auto-Invest sono la parte più difficile da ricostruire:

- un piano su **più monete** spende con una sola uscita e riceve più entrate,
- un piano legato a un **indice** ha solo l'uscita: le monete comprate restano in un conto Auto-Invest
  a parte, vengono ribilanciate ogni mese senza lasciare righe nel file, e arrivano nello Spot solo al
  riscatto,
- fino a metà 2023 l'importo di un piano prelevato da Flexible Savings non compare affatto, c'è solo
  l'entrata,
- l'entrata può arrivare ore dopo l'uscita.

Il risultato è un gran numero di movimenti da classificare.

**I file di dettaglio** sono tre e si scaricano dalla cronologia Auto-Invest di Binance:
**Subscription** (gli acquisti di ogni esecuzione), **Rebalancing** (i ribilanciamenti dei piani
indice) e **Redemption** (i riscatti). Si importano con la voce *Dettaglio Auto-Invest*, **dopo** il
file delle transazioni, scegliendoli **tutti insieme** nella finestra di selezione (anche se Binance li
ha divisi in più parti). Chi non ha mai usato piani legati a un indice avrà solo il file Subscription.

**I file si possono rinominare.** Il programma riconosce il tipo di ciascun file dal contenuto (la prima
riga), non dal nome. Il fuso orario invece Binance lo scrive solo nel nome (per esempio `UTC2`): se un
file l'ha perso lo prende dagli altri file scelti insieme, e se l'hanno perso tutti lo ricava dal
confronto con i movimenti del file delle transazioni, e il resoconto lo dice. Un file scelto che non è
un file di dettaglio Auto-Invest, per esempio il file delle transazioni, viene segnalato prima di
iniziare, e si può rifare la selezione o proseguire senza quel file.

**Perché tutti insieme.** Ogni file si può anche passare da solo, il programma ne riconosce il tipo
dall'intestazione, ma il file Rebalancing da solo non basta. Non dice quali monete sono state vendute e
quali comprate: il programma lo ricava dagli acquisti del file Subscription e dai riscatti del file
Redemption, e li legge solo dai file scelti **nello stesso passaggio**. Se uno dei due manca il verso
viene stimato dai prezzi di mercato, che è meno affidabile, e il resoconto lo segnala.

- **Subscription** si può passare da solo: abbina le esecuzioni dei piani, ed è l'unico file che
  serve a chi non ha mai usato piani legati a un indice.
- **Redemption** da solo sistema i riscatti, ma non aiuta a ricostruire i ribilanciamenti.
- **Rebalancing** va passato sempre insieme agli altri due. Se manca uno dei due, prima di iniziare il
  programma lo segnala e permette di rifare la selezione, oppure di proseguire comunque.
- Ripassare i tre file insieme in un secondo momento è sicuro: quello che è già a posto resta com'è e
  si completa il resto.

> **Attenzione** — un ribilanciamento già ricostruito non viene rifatto ai passaggi successivi. Se il
> file Rebalancing è stato passato da solo e il verso è uscito sbagliato, ripassare i tre file insieme
> non lo corregge: prima vanno eliminati i ribilanciamenti creati (cancellando uno scambio di un
> ribilanciamento si toglie l'intero ribilanciamento), poi si ripassano i tre file.

Il programma lavora sui movimenti già importati dal file delle transazioni, e aggiunge solo quello
che il file non contiene:

- ogni uscita viene abbinata alle monete che ha comprato, e l'uscita che manca nei piani più vecchi
  viene aggiunta,
- acquisti, ribilanciamenti e riscatti vengono ricostruiti sul sotto-wallet *Auto-Invest* di Binance,
  e i movimenti importati mantengono quantità e valore,
- i ribilanciamenti sono scambi diretti fra le monete vendute e quelle comprate, ripartiti per valore.

Il file Rebalancing **non dice** quali monete sono state vendute e quali comprate. Per un piano già
riscattato il programma lo ricava esattamente dalle quantità, per un piano ancora aperto dai prezzi di
mercato, e il resoconto avvisa che quantità di pochi centesimi possono avere il verso sbagliato.

Gli scambi che il programma aveva già abbinato da solo durante l'import del file delle transazioni
restano come sono se coincidono con il file di dettaglio. Quelli che univano movimenti di esecuzioni
diverse vengono rifatti, e il resoconto li elenca con la data.

### Ordine consigliato {#binance-ordine}

1. Il **file delle transazioni** (oppure il Financial Report, oppure le API) per tutto il periodo.
2. Lo storico **Simple Earn Flessibile**, se si è usato Simple Earn Flessibile.
3. Il **dettaglio Dual Investment**, se si è usato Dual Investment.
4. I **dettagli Auto-Invest**, se si sono usati i piani Auto-Invest.

Alla fine conviene controllare il pulsante *Errori* (per esempio le giacenze negative) e,
con le API configurate, confrontare in *Giacenze a data* le giacenze calcolate con quelle che Binance
riporta oggi.

### Problemi noti {#binance-problemi-noti}

- In passato è capitato che il file delle transazioni non contenesse alcuni movimenti, comparsi poi
  in un export scaricato mesi dopo, e che non riportasse più gli scambi su coppie tolte dal listino di
  Binance. Se una giacenza non torna, riscaricare il file per lo stesso periodo e reimportarlo
  aggiunge solo i movimenti mancanti.
- I prodotti di Binance cambiano spesso nome e causale: una riga con una causale che il programma non
  conosce viene scartata e segnalata nel resoconto di fine importazione. In quel caso è utile
  segnalarlo, perché venga aggiunta.

## Bitget {#bitget}

| Voce nella finestra | Che cosa importa | Quando serve |
|---|---|---|
| **CSV** (in prova) | L'export CSV dei movimenti del conto | Se non si usano le API |

La strada consigliata sono le **API**: con il pulsante *Exchange API* della scheda dei movimenti si
inserisce una chiave creata sul sito di Bitget con il solo permesso **Tax** in lettura, che basta a
leggere tutto lo storico del conto spot, interessi di Earn compresi. La chiave si crea dalla pagina
*Tax API* di Bitget (non da quella delle API normali). La passphrase va inserita solo se la chiave ne
ha una (il pulsante non c'è nell'edizione distribuita dal Microsoft Store).

Il file CSV è l'export dei movimenti del conto (*Assets – Fund Flows – Export*, formato CSV). Copre solo
gli **ultimi 24 mesi**: lo storico più vecchio va chiesto all'assistenza di Bitget, oppure si usano le
API.

### CSV o API {#bitget-csv-o-api}

L'export CSV scrive gli orari con uno scostamento che cambia da un export all'altro (un'ora o due
rispetto all'ora UTC con cui il programma li legge) e può fermarsi qualche ora prima della fine del
periodo richiesto. Le API invece danno l'orario esatto di ogni movimento. Per questo i movimenti
importati dal CSV e quelli scaricati dalle API **non si riconoscono fra loro**: chi passa alle API deve
prima eliminare i movimenti Bitget importati dal CSV, altrimenti entrano due volte.

### Che cosa fa il programma {#bitget-movimenti}

- **Gli interessi di Earn**, accreditati più volte al giorno, vengono sommati in un solo movimento al
  giorno per moneta.
- **Spot ed Earn** sono lo stesso wallet: sottoscrizioni e riscatti non vengono importati.
- **LaunchX**: il BGB impegnato e il token ricevuto diventano un unico scambio.
- **I fondi mandati a un trading bot** e quelli che ne rientrano diventano depositi e prelievi da
  classificare. Le operazioni interne ai bot non ci sono, né nelle API né nel CSV, e non vengono
  importate.
- I movimenti dei conti **futures, margin e P2P** vengono solo segnalati nel resoconto, non importati.
- Molti token sono quotati solo su Bitget: per i prezzi il programma preferisce Bitget.

## Bybit {#bybit}

Bybit esporta un file per ogni conto, e ogni conto ha la sua voce. I file si chiamano
*Asset Change Details*, uno per conto, e il nome del file dice di quale conto si tratta.

| Voce nella finestra | File | Che cosa contiene |
|---|---|---|
| **Spot - Asset Change Details** (in prova) | `AssetChangeDetails_spot_…` | Scambi, commissioni, conversioni dei piccoli saldi, Launchpad, prelievi |
| **Funding - Asset Change Details** (in prova) | `AssetChangeDetails_fund_…` | Interessi Easy Earn, rendimenti Launchpool, conversioni, depositi e prelievi |
| **Derivati - Asset Change Details** (in prova) | `AssetChangeDetails_contract_…` | Risultato e funding dei contratti derivati |
| **Unified Trading Account - Asset Change Details** (in prova) | `AssetChangeDetails_uta_…` | Scambi spot e derivati dopo il passaggio all'Unified Trading Account |
| **Dettaglio depositi e prelievi** | `assetHistory_withdrawDepositHistory_…` | Hash, indirizzo e rete dei depositi e prelievi già importati |

Vanno importati **tutti i conti** che si sono usati: ciascun file contiene solo i movimenti del suo
conto. Dopo il passaggio all'Unified Trading Account il conto Spot si svuota con un giroconto finale e
da lì in poi i movimenti stanno nel file dell'UTA.

### Dove si scaricano {#bybit-download}

Dalla pagina di esportazione dei dati di Bybit (*Profilo – Account – Data Export*, oppure
`bybit.com/user/assets/exportStatment`):

1. aprire la scheda **Transaction Log**,
2. in *Account* scegliere il conto principale e, se ci sono, i sotto-conti,
3. in *Type* selezionare tutti i tipi sotto **Account Change Details** (Spot, Funding, Derivati,
   Unified Trading Account),
4. in *Select date range* scegliere *Customize* e un anno intero (dal 1° gennaio al 31 dicembre),
5. esportare: Bybit prepara un archivio ZIP che contiene un file per ogni conto, da estrarre prima di
   importarlo. La preparazione può richiedere da qualche decina di minuti a qualche giorno, e il file
   compare poi in *My Export*,
6. ripetere per ogni anno in cui si è usato Bybit (ogni export copre al massimo 12 mesi).

La guida di Bybit all'esportazione è
[sul sito di Bybit](https://www.bybit.com/en/help-center/article/How-to-Self-Export-Account-Data).

> **NB** — l'esportazione fatta da sé non comprende lo storico più vecchio: le guide indicano i movimenti
> di Earn più vecchi di due anni e quelli anteriori al 2020, o al 2022 secondo altre fonti. Per quei
> periodi lo storico va chiesto all'assistenza di Bybit.

### Che cosa fa il programma {#bybit-movimenti}

- **I giroconti fra i conti Bybit** non vengono importati: per il programma i conti sono lo stesso
  wallet. Lo stesso vale per sottoscrizioni e riscatti di Easy Earn e Launchpool.
- **Gli scambi**: la commissione diventa un movimento a parte, e più esecuzioni dello stesso ordine
  nello stesso secondo diventano un solo scambio.
- **Launchpad**: i BIT impegnati e il token ricevuto nello stesso momento diventano un unico scambio. Un
  token arrivato da solo resta un airdrop.
- **Conversioni**: le conversioni dei piccoli saldi in MNT e quelle del menu *Convert* diventano scambi
  (un acquisto se si cede euro). La migrazione da BIT a MNT del 2023 è importata come scambio: chi la
  vuole trattare come un semplice cambio di nome del token la sistema a mano.
- **Un prelievo annullato** ("Withdrawal Canceled") non viene importato insieme al prelievo che annulla,
  se i due stanno nello stesso file. Se il prelievo era già in archivio da un import precedente,
  l'annullamento entra come deposito da classificare.
- **La carta Bybit** (righe "Bybit Card" del Funding): una spesa in euro diventa un prelievo fiat, una
  spesa coperta da una cripto una vendita della cripto in euro seguita dal prelievo fiat, come per
  [Bybit EU](#bybit-eu).
- **I trading bot**: i fondi mandati al bot e quelli restituiti diventano prelievi e depositi da
  classificare. Le operazioni interne al bot non sono nel file.
- **I derivati** (conto Derivati e perpetui dell'UTA): il risultato e il funding vengono sommati per
  giorno e moneta e restano depositi e prelievi da classificare. Il programma non calcola i redditi da
  derivati: li segnala con un avviso a fine importazione e nelle stampe dei quadri, perché li considera
  come cripto-attività, cosa che non è corretta.
- Le righe dei contratti inversi dei primi giorni del 2022 senza la moneta vengono saltate: il loro
  effetto è di pochi decimilionesimi di BTC.

### Il dettaglio depositi e prelievi {#bybit-depositi-prelievi}

Nei file dei conti i depositi e i prelievi on-chain non hanno l'hash della transazione né l'indirizzo.
Il file *Withdraw & Deposit History* (voce *Dettaglio depositi e prelievi*) **non aggiunge movimenti**:
completa quelli già importati con l'hash, l'indirizzo e, nelle note, la rete usata. Va letto **dopo** i
file dei conti Spot e Funding e si può rileggere senza effetti. La commissione di rete non viene
separata qui: la crea il programma quando si classifica il trasferimento.

## Bybit EU {#bybit-eu}

Bybit EU (Bybit EU GmbH, Austria) è un intermediario diverso da Bybit e ha voci sue: i movimenti finiscono
in un gruppo wallet proprio, con l'Austria come Stato estero nel quadro W/RW.

| Voce nella finestra | File | Che cosa contiene |
|---|---|---|
| **Funding - Asset Change Details** (in prova) | `AssetChangeDetails_fund_…` | Depositi in euro, conversioni, interessi Easy Earn, premi, depositi e prelievi |
| **Unified Trading Account - Asset Change Details** (in prova) | `AssetChangeDetails_uta_…` | Scambi spot, anche euro contro cripto |
| **Dettaglio depositi e prelievi** | `assetHistory_withdrawDepositHistory_…` | Hash, indirizzo e rete dei depositi e prelievi già importati |

I file si scaricano dal sito di Bybit EU (bybit.eu) come per Bybit, dalla scheda *Transaction Log*
dell'esportazione dei dati (vedi [Dove si scaricano](#bybit-download)), e vanno importati **entrambi i
conti**, Funding e Unified Trading Account.

- **I giroconti** fra Funding e Unified Trading Account non vengono importati.
- **Easy Earn**: sottoscrizioni e riscatti, anche quelli fatti direttamente dall'Unified Trading Account,
  non vengono importati. Bybit non esporta il saldo Earn, quindi la giacenza del programma comprende
  anche il capitale in Earn.
- **Convert**: con una gamba in euro diventa un acquisto, fra due cripto uno scambio.
- **Gli scambi spot**: la commissione diventa un movimento a parte.
- **"Airdrop Bonus"** (i premi delle promozioni) diventa un airdrop.
- **La carta Bybit**: una spesa in euro diventa un prelievo fiat. Una spesa coperta da una cripto (per
  esempio USDC) diventa una vendita della cripto in euro, seguita dal prelievo fiat della spesa. Il file
  della carta (`bybitCardTransactionHistory_…`) non va importato: riporta le stesse spese. Può capitare
  che una spesa della carta sia più alta dell'addebito sul Funding (su un export verificato di importi
  tondi, da 5 a 100 euro): la differenza non compare in nessun file e non viene importata.
- **Un prelievo annullato** non viene importato insieme al prelievo che annulla, se i due stanno nello
  stesso file.
- **Il dettaglio depositi e prelievi** funziona come per Bybit: non aggiunge movimenti, completa quelli
  importati dal file del Funding e va letto dopo di esso. I depositi vengono riconosciuti anche se Bybit
  li accredita fino a 24 ore dopo l'orario indicato nel file.

## Coinbase {#coinbase}

| Voce nella finestra | Che cosa importa |
|---|---|
| **CSV** (in prova) | L'export *Transactions* del conto Coinbase "retail" |

Il file copre tutta la cronologia del conto: invii e ricezioni, conversioni, acquisti, staking,
ricompense. Per scaricarlo:

1. accedere a Coinbase e aprire la pagina degli estratti conto (`accounts.coinbase.com/statements`,
   oppure *Impostazioni – Estratti conto / Report*),
2. nella sezione *Generate Custom Statement* scegliere *Asset*: tutti, *Transaction type*: tutte, un
   anno come periodo e il formato **CSV**,
3. premere *Generate*: Coinbase manda per email il collegamento per scaricare il file,
4. ripetere per ogni anno.

### Che cosa fa il programma {#coinbase-movimenti}

- **Acquisti pagati con il saldo in euro** di Coinbase: diventano uno scambio euro–cripto, con la
  commissione come movimento a parte.
- **Acquisti pagati con carta, Apple/Google Pay o bonifico**: il denaro non passa dal saldo Coinbase,
  quindi il programma non registra nessuna uscita di euro. L'acquisto viene caricato al costo pieno
  effettivamente sostenuto, come già avviene per l'app di Crypto.com. Chi ha il conto in dollari o
  sterline deve segnalarlo, perché il riconoscimento si basa sulla nota "using EUR Wallet" (o "using
  Portafoglio Euro" con la lingua italiana).
- **Ordini Advanced Trade e vendite** (*Advanced Trade Buy*, *Advanced Trade Sell*, *Sell*): il file
  riporta solo la cripto comprata o venduta, l'euro o l'USDC dell'altra parte stanno nella nota della
  riga e il programma li legge da lì. Contro euro diventano acquisti e vendite, contro USDC scambi fra
  cripto, con la commissione come movimento a parte nella moneta in cui è stata pagata.
- **Conversioni** (*Convert*): le due gambe, che a volte arrivano a un paio di secondi di distanza,
  vengono riunite in un unico scambio.
- **Ricompense**: le ricezioni con nota "from Coinbase Earn", "from Coinbase Referral" o di un airdrop
  diventano ricompense. Le altre ricezioni restano trasferimenti.
- **Staking**: gli spostamenti verso e dallo staking di Coinbase non producono movimenti, salvo il
  passaggio di ETH in staking a ETH2, che è uno scambio fra le due monete (poi riconvertite in ETH
  dalla *Retail Eth2 Deprecation* del gennaio 2025).
- **Ticker rinominati**: ZETACHAIN diventa ZETA e CGLD diventa CELO, così la stessa moneta non si
  spezza in due.
- **Trasferimenti verso e da Coinbase Pro**: quelli in euro vengono abbinati da soli a fine importazione
  come giroconto fra wallet propri (in qualunque ordine si importino i due file), quelli in cripto
  restano depositi e prelievi da classificare come trasferimento fra wallet.

## Coinbase Pro {#coinbase-pro}

| Voce nella finestra | Che cosa importa |
|---|---|
| **Pro / GDAX** (in prova) | Lo statement del conto Pro, file `account.csv` |

Coinbase Pro (prima GDAX) ha funzionato fino al 2023. I suoi movimenti vengono registrati su un wallet
distinto, **Coinbase Pro**, separato da quello di Coinbase "retail", così le due storie non si
mescolano.

> **Attenzione** — l'attività del Pro resta consultabile solo fino al 2027: chi ha usato Coinbase Pro
> deve scaricare gli estratti **entro la fine del 2026**, anche se non li importa subito.

Per scaricare il file:

1. accedere a Coinbase e aprire la pagina degli estratti conto di Coinbase Pro (compare solo a chi ha
   usato il Pro, agli altri dà un errore),
2. nella sezione *Generate Custom Report* scegliere *Report type*: **Account**, *Portfolio*: tutti,
   *Account Type*: tutti, un anno come periodo e il formato **CSV**,
3. premere *Generate*: Coinbase manda per email il collegamento per scaricare il file,
4. ripetere per ogni anno in cui si è usato il Pro.

Il report *Fills*, che Coinbase offre nella stessa pagina, non serve.

- **Gli scambi** sono ricomposti dalle due righe di ogni esecuzione, e le commissioni diventano un
  movimento a parte. I piccoli rimborsi in euro ("rebate") diventano ricompense.
- **Versamenti e prelievi in euro** sono in realtà giroconti dal conto Coinbase "retail": vengono
  abbinati da soli a fine importazione con la riga corrispondente del file di Coinbase.
- **Versamenti e prelievi in cripto** diventano depositi e prelievi da classificare come trasferimento
  fra wallet: l'altra metà sta nel file di Coinbase.

> **Attenzione** — le prime versioni di questa importazione registravano i movimenti del Pro sotto il
> nome *Coinbase*. Se erano stati importati così, vanno cancellati prima di reimportare il file,
> altrimenti entrano due volte.

## CoinTracking {#cointracking}

CoinTracking è una piattaforma di rendicontazione: serve per importare exchange e blockchain che
Giacenze Crypto non legge direttamente. Si caricano i dati su CoinTracking, li si esporta in CSV e si
importa il file qui. Come scaricare il file è spiegato nel
[manuale con le schermate](export-import-csv.html#cointrackinginfo).

| Voce nella finestra | Che cosa importa |
|---|---|
| **Formato storico** | L'export CSV, letto dall'importatore interno, che riconosce da solo le due impaginazioni del file |
| **CSV** | L'export CSV nell'impaginazione attuale |
| **CSV (vecchio layout)** | L'export CSV nell'impaginazione precedente, ancora in prova |

- **Un solo exchange o wallet per volta.** Il file di CoinTracking non dice a quale exchange appartiene
  ogni movimento: nella finestra si sceglie che cosa si importa (*Exchange*, *Wallet* o *Transazioni
  Blockchain*) e il nome del wallet di destinazione, anche personalizzato. Su CoinTracking quindi si
  carica un exchange, lo si esporta, si cancellano i dati e si ricomincia con il successivo.
- **Sempre la stessa voce** per uno stesso exchange: voci diverse descrivono gli stessi movimenti in
  modo diverso e il programma non sempre li riconosce come doppi.

## Crypto.com {#crypto-com}

| Voce nella finestra | Che cosa importa |
|---|---|
| **App CSV** | L'export *Crypto Wallet* dell'app Crypto.com |
| **Exchange CSV** | Il file delle transazioni o gli estratti mensili di Crypto.com Exchange |

Come scaricare i file è spiegato nel [manuale con le schermate](export-import-csv.html#cryptocom-app).

### L'app {#crypto-com-app}

L'app esporta tre file con la stessa intestazione: *Crypto Wallet*, *Fiat Wallet* e *carta*. Con la
voce *App CSV* si importa **solo il Crypto Wallet**, che si esporta dall'app scegliendo *Crypto
Wallet* e un periodo di al massimo **3 anni** per volta (i passaggi sono nel manuale con le
schermate). Lo storico anteriore al novembre 2022 può non essere scaricabile da sé: in quel caso va
chiesto all'assistenza di Crypto.com. Gli altri due non contengono movimenti di cripto:
si caricano dalle schede *Fiat Wallet Crypto.com* e *Carta Crypto.com*, con i pulsanti *Carica Dati Fiat
Wallet* e *Carica Dati Carta*, e servono a seguire i saldi in euro e la loro giacenza media. Se un file
del Fiat Wallet o della carta viene scelto con la voce *App CSV*, il programma lo riconosce e avvisa,
con la possibilità di saltarlo o di importarlo comunque (i suoi movimenti risulterebbero sconosciuti).

- **Crypto Earn, staking, Supercharger e lockup dei CRO** diventano in genere trasferimenti interni dal Crypto
  Wallet ai sotto-wallet dell'app (*Earn*, *Staking*, *Supercharger*, *Fondi Bloccati*) e ritorno: non
  cambiano le giacenze di Crypto.com, mostrano solo dove si trovano le monete. Interessi e ricompense
  vengono importati come tali. I blocchi degli ordini limite invece non vengono importati.
- **Crypto Basket**: acquisti, vendite e ribilanciamenti vengono registrati sul sotto-wallet *Crypto
  Basket* dell'app.
- **Ricarica della carta con cripto** e **pagamenti in cripto** diventano vendite. Il cashback della
  carta è una ricompensa.
- **Trasferimenti verso e da Crypto.com Exchange** diventano prelievi e depositi da classificare: l'altra
  metà sta nel file dell'Exchange, che il programma registra come un wallet distinto (*Crypto.com
  Exchange*).

### L'Exchange {#crypto-com-exchange}

Crypto.com Exchange offre due file: il **file delle transazioni** e gli **estratti mensili**. La voce
*Exchange CSV* li legge entrambi, ma per ogni periodo va usato **un solo tipo**, per evitare movimenti
doppi e incoerenti.

Il file delle transazioni si scarica dal sito crypto.com/exchange, con la lingua impostata
sull'inglese: *Dashboard*, poi *Orders* nella barra a sinistra, scheda *Transactions*, periodo (al
massimo **180 giorni** per volta), *Export* e *Export to CSV*. Quando il file è pronto si scarica dalla
stessa maschera. Per coprire tutta la storia del conto servono quindi più file.

## Gate.io {#gate-io}

| Voce nella finestra | File | Che cosa contiene |
|---|---|---|
| **Spot Trade History** | `Spot_TradeHistory_….csv` | Gli scambi a pronti, con le commissioni |
| **mydeposits** | `mydeposits.csv` | I depositi |
| **mywithdrawals** | `mywithdrawals.csv` | I prelievi |

I movimenti finiscono nel gruppo wallet preconfigurato di Gate.io.

I tre file si scaricano da pagine diverse del sito di Gate.io:

- gli **scambi** da *Spot Orders – Trade History*, con *All* come mercato e il periodo voluto,
- i **depositi** dalla pagina dei depositi recenti, con il suo pulsante di download,
- i **prelievi** dalla pagina dei prelievi, con il suo pulsante di download.

L'estratto conto generale che Gate.io prepara su richiesta (*Transaction History – request batch
data*) è un file diverso, che il programma non legge.

- **Non aprire e risalvare i file** con un foglio di calcolo o un editor prima di importarli: per
  Gate.io basta che cambino gli spazi o la codifica perché il file non venga più riconosciuto.
- **Le commissioni** sono quasi sempre in **POINT**, il token sconto di Gate.io: vengono importate così
  come sono, e il programma segnala POINT fra i token senza prezzo. La segnalazione non ha effetti sul
  resto.
- **I prelievi**: la commissione di rete diventa un movimento a parte.
- **I tre file non coprono tutto**: conversioni, P2P e gli altri prodotti di Gate.io non hanno un import.
  Se dopo l'importazione restano giacenze negative (per esempio su USDT o GT), mancano probabilmente
  movimenti di quei prodotti, da aggiungere a mano.

## KuCoin {#kucoin}

| Voce nella finestra | File | Che cosa contiene |
|---|---|---|
| **Spot Orders - Completed Trades** | `Spot Orders_Completed Trades.csv` | Gli scambi a pronti, con le commissioni |
| **Deposit List** | `Deposit List.csv` | I depositi |
| **Withdrawal List** | `Withdrawal List.csv` | I prelievi |

I movimenti finiscono nel gruppo wallet preconfigurato di KuCoin.

Per scaricare i file:

1. accedere a KuCoin e aprire la pagina *Export History* (`kucoin.com/account/download`, oppure
   *Orders – Spot Trade History – Export History*),
2. selezionare tutti i tipi di movimento e tutti i mercati, come periodo scegliere **1 anno** (il
   massimo per ogni export), lasciare il fuso **UTC-0** e, se si sono usati sotto-conti, spuntare
   *Includes Sub-Accounts*,
3. scegliere il formato *.csv* e confermare,
4. quando KuCoin avvisa che l'export è pronto, scaricarlo (il collegamento resta valido 30 giorni),
5. ripetere per ogni anno: KuCoin permette circa 20 export al mese.

KuCoin prepara un archivio ZIP con molte decine di file (spot, margin, futures, P2P, Earn e altri): il
programma legge i tre della tabella, gli altri si possono lasciare da parte. Lo storico più vecchio va
chiesto all'assistenza di KuCoin.

- **Gli scambi**: la commissione diventa un movimento a parte. Un ordine eseguito in più parti occupa
  più righe, anche identiche, e ognuna diventa uno scambio.
- **I prelievi**: nel file l'importo è quello arrivato a destinazione e la commissione di rete è in più.
  Il programma registra il prelievo e la commissione come due movimenti.
- **I futures non sono importabili**: il loro file elenca solo le esecuzioni dei contratti, senza il
  risultato delle posizioni.
- **I tre file non coprono tutto**: Earn, premi, conversioni e trasferimenti fra i sotto-conti non hanno
  un import. Se dopo l'importazione restano giacenze negative, mancano probabilmente movimenti di quei
  prodotti, da aggiungere a mano.

## Nexo {#nexo}

| Voce nella finestra | Che cosa importa |
|---|---|
| **CSV 11 colonne** (in prova) | L'export delle transazioni nel formato da settembre 2026, con le colonne *Fee* e *Fee Currency* |
| **CSV 9 colonne** (in prova) | L'export delle transazioni nel formato precedente, senza commissioni |

Il file si scarica dal sito di Nexo: *Transactions* nel menu in alto, poi a destra un periodo che copra
tutta la storia del conto, tutti i tipi di transazione, tutte le monete, ed *Export*.

Le due voci leggono lo stesso export in due formati: si sceglie quella che corrisponde al numero di
colonne del file (gli export scaricati prima di settembre 2026 e salvati sul disco restano a 9).

- **Gli interessi**, anche quelli dei depositi a termine, diventano ricompense di Earn.
- **Exchange** diventa uno scambio fra cripto, con la commissione come movimento a parte (solo nel
  formato a 11 colonne, l'unico che la riporta). Il cashback dello 0,5% sugli scambi è una ricompensa.
- **I "Top up Crypto" promozionali** (Campaign Rewards, Welcome Bonus, Cash-back Promo) diventano
  ricompense, gli altri depositi.
- **I depositi a termine** (Locking e Unlocking) sono spostamenti interni e non vengono importati.
- **EURX**, la rappresentazione dell'euro di Nexo, viene registrato come euro.
- **Il controvalore** del file è in dollari: il programma non lo usa e ricava il valore in euro da sé.
- **I trasferimenti verso e dal Nexo Pro** diventano prelievi e depositi: i movimenti del Pro stanno nei
  suoi file, descritti [più sotto](#nexo-pro).

## Nexo Pro {#nexo-pro}

| Voce nella finestra | File | Che cosa contiene |
|---|---|---|
| **Storico ordini (nexo_pro_trade)** | `nexo_pro_trade` | Gli ordini eseguiti sul Pro |
| **Depositi e Prelievi (DnWHistory)** | `DnWHistory` | I trasferimenti fra il Nexo e il Pro |

Il Nexo Pro è un conto a parte, ma sta nello **stesso gruppo wallet del Nexo**: i trasferimenti fra i due
restano interni al gruppo. I due file si scaricano dalla cronologia delle transazioni del Nexo Pro, per
tutti gli anni in cui lo si è usato.

- **Gli ordini eseguiti** diventano scambi fra cripto, con la commissione come movimento a parte. Il
  file non riporta l'importo pagato nella seconda moneta: il programma lo calcola come quantità per
  prezzo di esecuzione. Gli ordini annullati non vengono importati.
- **Depositi e prelievi del Pro** sono sempre trasferimenti da e verso il Nexo: diventano depositi e
  prelievi da abbinare ai "Transfer To/From Pro Wallet" dell'export del Nexo. L'abbinamento si fa con
  *Assegnazione Automatica* in *Analisi Crypto – Classificazione Depositi/Prelievi*, oppure a mano.
- Conviene importare **anche l'export del Nexo** per lo stesso periodo, altrimenti i trasferimenti
  restano senza la loro metà.

## OKX {#okx}

| Voce nella finestra | Che cosa importa |
|---|---|
| **Funding** | L'export *Funding History* del conto Funding |
| **Trading** | L'export *Trading History* del conto Trading |
| **Formato storico** | I CSV di OKX letti dal vecchio importatore |

La strada consigliata sono le **API**: con il pulsante *Exchange API* della scheda dei movimenti si
inserisce una chiave in **sola lettura** (senza permessi di trading né di prelievo), con secret e passphrase
(il pulsante non c'è nell'edizione distribuita dal Microsoft Store).

### Lo scaricamento dalle API {#okx-api}

Le API coprono tutta la storia del conto, Funding e Trading, fino al 2021. Altre cose da sapere:

- **Gli interessi di Simple Earn**: OKX li restituisce solo per l'ultimo mese, quindi conviene scaricare
  almeno una volta al mese. Per i giorni che mancano, nella scheda *Particolarità OKX* della finestra
  *Exchange API* c'è un'opzione (spenta all'inizio) che li calcola da saldo, sottoscrizioni e riscatti:
  il totale di ogni periodo è esatto, la divisione per giorno è una stima.
- **Uno scaricamento incompleto non importa nulla**: se OKX interrompe la risposta, si riprova più tardi
  senza perdere movimenti.
- **La carta OKX**: il programma trova da solo il wallet della carta sulla rete X Layer, lo mostra nella
  scheda *Particolarità OKX* e ne importa i movimenti.

### I file CSV {#okx-csv}

Se si usano i file, vanno esportati **entrambi i conti**, Funding e Trading, perché ciascun file contiene
solo una parte dei movimenti. Si scaricano dal sito di OKX: *Assets – Order Center*, scheda *Trading
history* oppure *Funding history*, pulsante *Download*, periodo e *Export*. La schermata mostra solo gli
ultimi 90 giorni, ma il file scaricato copre tutto il periodo scelto.

- **Il fuso orario**: il nome del file dice "UTC0" ma gli orari sono nel fuso scelto sull'account OKX al
  momento dell'export. Gli export recenti lo dichiarano nella prima riga e il programma lo usa, per quelli
  che non lo dichiarano assume l'ora italiana.
- **I giroconti** fra Funding e Trading e le sottoscrizioni e i riscatti di Earn non vengono importati.
- **Le commissioni** del Trading diventano un movimento a parte. Quelle di rete dei prelievi sono già
  comprese nell'importo prelevato e non si possono separare.
- **Le conversioni** dell'app OKX (*Convert*) diventano scambi.

*Formato storico* è il vecchio importatore, rimasto per chi lo usava: per i nuovi import si usano le API
oppure *Funding* e *Trading*. Come per gli altri exchange, per uno stesso periodo si usa una sola fonte.

## Tatax {#tatax}

Tatax, come CoinTracking, è una piattaforma di rendicontazione da cui si esportano in CSV i movimenti di
exchange e wallet.

| Voce nella finestra | Che cosa importa |
|---|---|
| **CSV** | L'export CSV nel formato attuale di Tatax |
| **Formato storico** | L'export CSV nel formato precedente, letto dall'importatore interno |

- **Un solo exchange o wallet per volta**: nella finestra si sceglie che cosa si importa (*Exchange*,
  *Wallet* o *Transazioni Blockchain*) e il nome del wallet di destinazione, anche personalizzato.
- **Sempre la stessa voce** per uno stesso exchange, per non avere movimenti doppi.
- Con il *Formato storico* i token in staking o in lending, che Tatax scrive con un'estensione (per
  esempio `ETH.STAKING@BINANCE`), tornano al loro nome, così prezzi e LIFO restano quelli della moneta.
