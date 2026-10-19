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
  nome): il programma lo usa per collocare i movimenti all'ora giusta.
- **Reimportare lo stesso file non crea doppioni**: vengono aggiunti solo i movimenti che mancano.
  Con *Sovrascrivere movimenti già presenti* invece i movimenti già importati vengono riscritti come
  li descrive il file, e quelli che erano stati classificati a mano tornano da classificare (il
  resoconto di fine importazione li elenca).

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
scheda dei movimenti (vedi [più sotto](#binance-api)).

### Il file delle transazioni {#binance-transazioni}

È l'export *Storico Asset – Esporta i registri delle transazioni* del sito di Binance. Come
scaricarlo è spiegato nel [manuale con le schermate](export-import-csv.html#binance): va scelto il fuso
**UTC** e il formato **CSV**, e il file non va rinominato.

Il file contiene scambi, conversioni, depositi e prelievi, interessi e bonus di Earn, airdrop e
distribuzioni, le spese con la carta Binance e i pagamenti Binance Pay. Alcune cose da sapere:

- **Gli spostamenti interni** (per esempio da Spot a Earn e ritorno) non vengono importati: per il
  programma Spot ed Earn sono lo stesso wallet.
- **Conversioni e piccoli saldi convertiti in BNB** a volte hanno le due gambe registrate a qualche
  secondo o minuto di distanza: il programma le riunisce in un unico scambio.
- **La carta Binance**: una spesa in euro diventa un prelievo fiat, una spesa pagata in cripto un
  prelievo da classificare, il cashback una ricompensa.
- **Binance Pay**: pagamenti e invii diventano depositi e prelievi da classificare.
- **Dual Investment e Auto-Invest** compaiono nel file solo in parte, senza i dati che servono ad
  abbinarne i movimenti: per questi servono i file di dettaglio descritti più sotto.
- **I bonus di Simple Earn negli export dal 2026** sono scritti due volte, sul conto Spot e sul conto
  Earn ("Rewards Income"): il programma importa solo quelli del conto Spot. Se un import fatto con una
  versione precedente li ha portati in archivio tutti e due, la pulizia è in *Opzioni – Pulizie –
  Ricompense Binance doppie*.
- **Le operazioni dei bot di trading** si trovano solo in questo file, non nelle API.

### Il Financial Report {#binance-financial-report}

È il report che Binance genera una volta all'anno (*Account – Report finanziari*). Contiene gli stessi
movimenti del file delle transazioni in un'altra forma, quindi è un'**alternativa**: per lo stesso
periodo si importa o l'uno o l'altro, mai entrambi.

### Lo scaricamento dalle API {#binance-api}

Con una chiave API in **sola lettura** inserita con il pulsante *Exchange API* della scheda dei
movimenti, il programma scarica i movimenti direttamente da Binance, senza file (il pulsante non c'è
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

**Dove si scarica**: dall'app di Binance, *Assets – Earn – Flexible – Rewards*, icona di download. Il
file si importa con la voce *Simple Earn Flessibile - Real-time APR*, in qualunque momento rispetto al
file delle transazioni. Le righe Bonus Tiered APR dello stesso file vengono ignorate, perché sono già
in archivio.

Le Real-time APR nel file hanno solo il giorno, senza l'ora: il programma le registra all'inizio della
giornata (00:00 UTC), così una ricompensa del 31 dicembre resta nell'anno giusto anche in ora italiana.

> **NB** — lo storico scaricabile dall'app può non arrivare agli anni più vecchi. Se dopo l'import
> restano giacenze negative su Binance, la parte mancante sono probabilmente ricompense di periodi che
> il file non copre, e vanno aggiunte a mano.

### Dual Investment {#binance-dual-investment}

Nel file delle transazioni un contratto Dual Investment lascia due righe, *Dual Savings Purchase*
(l'acquisto) e *Dual Savings Settlement* (la liquidazione), che diventano un prelievo e un deposito da
classificare. Il file non dice a quale contratto appartengono, e fra le due possono passare settimane o
mesi: abbinarle a mano è lungo e facile da sbagliare.

**Il file di dettaglio** si scarica dalla cronologia dei prodotti Earn di Binance, sezione *Advanced
Earn – Dual Investment History*, e si importa con la voce *Dettaglio Dual Investment* **dopo** il file
delle transazioni. Per ogni contratto il programma trova acquisto e liquidazione e li sistema:

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
