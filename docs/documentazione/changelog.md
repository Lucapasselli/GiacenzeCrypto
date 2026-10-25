---
layout: default
title: Novità delle versioni
---

# Novità delle versioni

Elenco delle modifiche introdotte dalle varie versioni di **Giacenze Crypto**, dalla più recente alla
più vecchia.

I pacchetti pronti all'uso vengono pubblicati a ogni rilascio:

- **portabile multipiattaforma** (Windows, Linux e macOS; richiede un runtime Java installato, anche
  open);
- **portabile e installer per Windows**;
- **portabile e pacchetto `.deb` per Linux**, più il pacchetto **AUR** per le distribuzioni derivate da
  Arch e il **flatpak**;
- **immagine DMG per macOS**, sia per processori Apple Silicon sia per Intel. Poiché l'applicazione non è
  firmata, può essere necessario eseguire `xattr -cr /Applications/Giacenze_Crypto.app` perché parta.

> **Aggiornare il programma** — per le versioni portabili basta sovrascrivere la cartella, dopo averne
> fatto una copia di sicurezza. **Su macOS** la sovrascrittura della cartella comporta la perdita dei
> dati: va aggiornato manualmente solo il contenuto. Le versioni con installer si installano sopra la
> precedente mantenendo i dati (dalla versione 1.0.4 in poi). In ogni caso è consigliabile una copia di
> sicurezza, che si può fare dal programma stesso con *Opzioni – Backup / Ripristino*.

In caso di problemi si può chiedere supporto sul
[gruppo Telegram](https://t.me/+6kfy5mjov-I2ODY8).

## Versione 1.0.66 (in lavorazione) {#versione-1066}

**Nuove implementazioni**

- **Wallet Algorand (in prova)**: in *Gestione Wallet* si può ora scegliere la rete *Algorand (ALGO)*. I movimenti si scaricano dall'indexer pubblico di Nodely, senza ApiKey. Ogni transazione, o gruppo di transazioni eseguite insieme come uno swap su Tinyman o Pact, diventa un solo movimento con quanto è entrato e uscito dal wallet, comprese le transazioni interne delle applicazioni. Le commissioni restano a parte, le ricompense di partecipazione pagate fino al 2022 diventano premi, l'abilitazione a ricevere un token (opt-in) è una sola commissione. I prezzi dei token vengono da DefiLlama anche per quelli che CoinGecko non collega ad Algorand, cioè quasi tutti. I token si riconoscono dal loro id: quelli che hanno lo stesso nome di altri, come le quote delle pool di Tinyman (TMPOOL2) o dei mercati di Alpha, mostrano l'id accanto al nome, così non si mescolano nel calcolo. Alla prima importazione di un wallet il programma confronta la somma dei movimenti con il saldo sulla blockchain e segnala le monete che non coincidono. Le ricompense di governance arrivano come normali pagamenti e vanno classificate a mano.

- **Wallet XRP Ledger (in prova)**: in *Gestione Wallet* si può ora scegliere la rete *Ripple (XRP)*. I movimenti si scaricano da un nodo pubblico di Ripple con tutta la storia, senza ApiKey. Per ogni transazione il programma legge quanto è cambiato davvero nel wallet: XRP, token e NFT. Così un pagamento parziale conta per quanto è stato consegnato, e un ordine sul DEX eseguito da un altro diventa uno scambio anche se il wallet non ha firmato nulla. Lo stesso vale per depositi e prelievi nelle AMM, assegni ed escrow. Le transazioni fallite costano solo la commissione. Un token si riconosce da codice ed emittente: se due emittenti diversi usano lo stesso codice, per esempio un falso USD, il secondo mostra l'emittente accanto al nome e non si mescola con il primo nel calcolo. I token LP delle AMM prendono il nome della coppia, per esempio LP XRP/SOLO. Alla prima importazione il programma confronta la somma dei movimenti con saldi, token e NFT sulla blockchain e segnala le differenze. I token MPT non vengono ancora importati.

- **Wallet Tron (in prova)**: in *Gestione Wallet* si può ora scegliere la rete *Tron (TRX)*. I movimenti si scaricano da TronGrid e TronScan, senza ApiKey: TRX, token TRC20 (come USDT) e TRC10, comprese le commissioni bruciate per energia e banda e i TRX che i contratti mandano al wallet, anche nelle transazioni fatte da altri. Uno swap è un solo scambio. Congelare, scongelare, delegare e votare non sono movimenti, le ricompense di voto ritirate diventano premi di staking. I token che TronScan riconosce come truffa compaiono già marcati come SCAM, e i trasferimenti finti con cui questi token simulano un'uscita dal wallet (l'avvelenamento dell'indirizzo) vengono scartati. Se due token diversi hanno lo stesso simbolo, il nome resta a quello verificato da TronScan e l'altro mostra l'indirizzo accanto. Alla prima importazione il programma confronta la somma dei movimenti con TRX (compresi quelli congelati), TRC10 e TRC20 sulla blockchain e segnala le differenze, per esempio i token che trattengono una commissione a ogni trasferimento. La prima importazione di un wallet con molti accrediti di token può richiedere qualche minuto.

- **Conti ether.fi Cash (in prova)**: in *Gestione Wallet* l'elenco delle reti ha ora la voce *ether.fi Cash (OP)*: si sceglie quella e si inserisce l'indirizzo del conto, quello su cui si ricevono le ricariche. Il programma verifica che sia davvero un conto ether.fi prima di aggiungerlo. Un conto inserito sulla rete *Optimism (OP)* viene riconosciuto lo stesso. Il saldo di questi conti non sta nel wallet ma è depositato nel protocollo di ether.fi, e le operazioni non le firma il wallet: per questo la lettura dall'explorer vedeva quasi niente. Ora i movimenti si leggono dagli eventi di ether.fi sui nodi pubblici di Optimism, senza ApiKey: ricariche, scambi fatti dall'app (vault Midas e Liquid, swap), spese con la carta, interessi sulle monete depositate e NFT ricevuti. Una spesa con la carta è un cash out della moneta spesa, al suo valore di mercato nell'istante del pagamento. Gli interessi diventano un premio a ogni movimento della moneta che li matura. Gli NFT arrivati senza nessuna operazione, di solito truffe, compaiono già marcati come SCAM. Una ricarica fatta da un proprio wallet importato nel programma si abbina da sola al prelievo da cui parte, e insieme diventano un trasferimento fra wallet propri. Le ricariche fatte direttamente da un exchange, per esempio da OKX, restano per ora depositi da classificare: i prelievi scaricati da OKX non riportano l'hash della transazione con cui abbinarle. Il conto va nel gruppo wallet *ether.fi* (Wallet 117). A ogni importazione il programma confronta la somma dei movimenti con il saldo del conto e segnala le differenze. Se i nodi di Optimism non rispondono, un wallet su Optimism non ancora riconosciuto non viene importato finché non si riesce a verificare che cosa è. Se il conto era già stato importato come wallet normale, il programma propone di togliere quei movimenti e reimportarli. Lo stesso indirizzo esiste anche su Arbitrum e sulle altre reti da cui si ricarica: lì è solo un passaggio, e non va inserito.

- **Token truffa marcati SCAM all'importazione dei wallet DeFi**: scaricando un wallet su Ethereum, Arbitrum, BSC e le altre reti lette da un explorer, il programma marca già come SCAM i token che sono truffe con certezza, e lascia come prima tutti quelli su cui c'è anche un solo dubbio. Vale solo per transazioni che il wallet non ha firmato e per movimenti senza prezzo di mercato, e serve una di tre prove. La prima è l'uscita dal wallet di un token che lì non è mai entrato: è l'"avvelenamento dell'indirizzo", un finto trasferimento verso un indirizzo quasi uguale a quello a cui si sono appena mandati fondi veri, con lo stesso importo, spesso con un token che si chiama proprio ETH o USDC. Non vale sulle reti lette dai nodi pubblici da un certo momento in poi, come BASE. La seconda è un simbolo che imita quello di una moneta nota con lettere di altri alfabeti o caratteri invisibili, per esempio una C cirillica dentro "USDC". La terza è un link nel nome, come "Visit https://…". Un token che si era già rinominato o tolto dagli SCAM, uno quotato, uno con un prezzo già presente nell'archivio e una moneta elettronica non vengono mai toccati. Senza la marcatura un finto ETH in uscita finiva nella stessa pila del vero ETH e ne abbassava la giacenza. Il messaggio di fine importazione elenca i token marcati e il motivo. La marcatura si toglie da *Analisi Crypto – Gestione Token Scam – Rimuovi da SCAM*. I movimenti già in archivio non vengono riesaminati.

- **Simboli in alfabeti che il font del programma non ha**: nelle tabelle, un nome con lettere che il font del programma non contiene, per esempio i finti USDC scritti in lisu o cherokee, si legge ora con i font del sistema invece di comparire come una fila di quadratini. Le emoji restano come prima. Dove il sistema non ha quei caratteri restano i quadratini.

- **Prezzi DefiLlama su Optimism**: come su Algorand, su Optimism vale ora anche un prezzo DefiLlama con affidabilità esattamente 0,9 (per esempio liquidRWA di ether.fi). I prezzi già salvati non cambiano.

- **Aggiornamenti e canale beta**: il programma controlla le nuove versioni direttamente sulle pubblicazioni del progetto su GitHub e non salta più una versione se la numerazione avanza di più di un passo. Dalla finestra *Informazioni* (clic sul titolo) il pulsante *Aggiornamenti...* permette di controllare subito, di scegliere il canale **stabile** o **beta** (che propone anche le versioni di prova intermedie, per esempio 1.0.65.01) e di disattivare il controllo all'avvio. Quando c'è una versione nuova il programma chiede se si vuole aggiornare. Con l'installer per Windows e con le versioni portable la scarica, ne verifica l'integrità e, dopo un'ultima conferma, fa un backup dell'archivio, si chiude, si aggiorna e si riavvia da solo. Con il pacchetto .deb, l'AUR, il flatpak e su macOS apre invece la pagina da cui scaricarla. Una versione si può anche ignorare: verrà segnalata la successiva.

- **Novità delle versioni di prova**: all'avvio di una versione di prova (beta) il programma mostra solo le novità di quella versione, e non più tutte quelle raccolte per la prossima versione, che restano a un clic dal pulsante *Tutte le novità della …* della stessa finestra. Chi salta qualche versione vede anche le novità di quelle saltate, e lo stesso vale fra le versioni stabili. Le stesse note sono la descrizione della versione di prova nella pagina delle pubblicazioni su GitHub.

- **Particolarità degli import per exchange**: nella finestra *Importazioni*, sotto la scelta dell'estrazione, un collegamento apre la nuova guida [Particolarità degli import per exchange](particolarita-exchange.html) sulla sezione dell'exchange selezionato (la stessa guida è anche fra le *Istruzioni*). Spiega quali file scaricare, quali file accessori servono e quando, e in che ordine importarli. Ci sono tutti gli exchange e le piattaforme della finestra: Binance (file delle transazioni, Financial Report, API, Simple Earn Flessibile, Dual Investment e Auto-Invest), Bitget, Bybit, Bybit EU, Coinbase, Coinbase Pro, CoinTracking, Crypto.com, Gate.io, KuCoin, Nexo, Nexo Pro, OKX e Tatax.

- **Bitget dalle API (in prova)**: in *Opzioni – ApiKey* si può ora aggiungere Bitget, con una chiave creata sul sito con il solo permesso "Tax" in lettura (la passphrase solo se la chiave ne ha una). Il programma scarica tutto lo storico del conto spot, interessi di Earn compresi, e lo importa con le stesse regole del file CSV, ma con l'orario esatto di ogni movimento: l'export CSV di Bitget scrive gli orari con uno scostamento che cambia da un export all'altro e può fermarsi qualche ora prima della fine del periodo. Chi passa alle API deve prima eliminare i movimenti Bitget importati da CSV, che non verrebbero riconosciuti come già presenti: il programma lo ricorda prima di scaricare. I fondi spostati verso un trading bot e ritorno sono depositi e prelievi da classificare, come nel CSV. Le operazioni interne ai bot non ci sono, né nelle API né nel CSV. I movimenti dei conti futures, margin e P2P vengono solo segnalati nel resoconto. I buoni accreditati da Bitget ("Crypto Voucher Distribution") sono importati come ricompensa.

- **Importazione da Bitget (in prova)**: le righe "Automatic deposit" e "Automatic withdrawal", cioè i fondi spostati fra il conto spot e un bot di trading, non vengono più scartate come causale sconosciuta: diventano depositi e prelievi da classificare, come "Opening of trading bot position" e "Closing of trading bot position", che prima erano ignorate. Le operazioni fatte dal bot non sono nel file dei movimenti spot e non vengono importate. L'impegno di BGB su LaunchX e il token ricevuto diventano un unico scambio, invece di un premio con il BGB che restava in giacenza. Chi aveva già importato file di Bitget deve eliminare quei movimenti e reimportarli.

- **Importazione da Bybit EU (in prova)**: Bybit EU (Bybit EU GmbH, Austria) è un intermediario diverso da Bybit e ha ora importazioni sue, *Importazioni – Bybit EU*, per i file "Asset Change Details" dei conti Funding e Unified Trading Account e per il dettaglio depositi e prelievi. I movimenti finiscono in un gruppo wallet proprio (Wallet 116), con l'Austria come Stato estero nel quadro W/RW. Le sottoscrizioni e i riscatti di Easy Earn non vengono importati: la moneta in Earn resta nella giacenza di Bybit EU. Il gruppo wallet di Bybit (Wallet 109) non ha più il periodo con i dati di Bybit EU GmbH, che non è l'intermediario della piattaforma globale: Stato estero e identificativo di Bybit globale vanno inseriti a mano, se servono. Chi aveva importato file di Bybit EU con le configurazioni di Bybit deve eliminare quei movimenti e reimportarli con quelle di Bybit EU.

- **Binance dalle API, staking e RWUSD**: lo scaricamento importa ora anche lo staking di SOL (SOL in BNSOL) e di ETH (ETH in WBETH), la sottoscrizione di RWUSD con USDC e i relativi riscatti, e i "Tax Payment" pagati con Binance Pay, nella stessa forma dell'importazione da CSV. Lo staking e la sottoscrizione sono scambi. Il riscatto di WBETH è uno scambio all'accredito dell'ETH. Il riscatto di BNSOL e di RWUSD sono un prelievo alla richiesta e un deposito all'accredito, qualche giorno dopo, da abbinare come scambio differito. Il Tax Payment è un cash out della moneta usata, al valore in euro del pagamento. Un riscatto in corso al momento dello scaricamento viene importato da quello successivo. Binance Pay conserva solo gli ultimi 18 mesi. Gli interessi del vecchio Locked Staking erano già importati. La crescita di valore di BNSOL e WBETH non è un accredito e non viene importata come premio. Se Binance rifiuta una di queste richieste per un motivo diverso da un prodotto non disponibile per il conto, lo scaricamento si ferma senza importare nulla.

- **Binance dalle API, controllo delle giacenze**: a fine scaricamento il programma confronta le giacenze Binance dell'archivio con i saldi attuali dell'account, e mostra le monete che non coincidono. Le API di Binance non restituiscono le operazioni dei bot di trading, di margin e futures, né i pagamenti Binance Pay più vecchi di 18 mesi, e senza il confronto la giacenza risultava sbagliata senza alcun segnale. Lo stesso avviso elenca i prodotti non disponibili per il conto e i pagamenti Binance Pay non importati. Con "Non mostrare più" l'avviso non compare più finché il suo contenuto resta uguale: se una differenza cambia, ricompare.

- **Prezzo di RWUSD**: RWUSD, la moneta di Binance legata al dollaro, non è quotata da nessun exchange e i suoi movimenti restavano senza valore. Dal 2026 prende il prezzo di USDC. Gli anni precedenti non cambiano, e i movimenti già importati mantengono il valore che hanno.

- **Binance CSV**: il riscatto di un piano Auto-Invest legato a un indice ("Index-Auto-Investment - Redeem Positions") non viene più scartato come causale sconosciuta. Le monete del piano stavano in un conto Auto-Invest a parte e arrivano nello Spot solo al riscatto: ognuna diventa un deposito da classificare. Chi aveva importato un file con queste righe può reimportarlo, vengono aggiunte solo quelle mancanti.

- **Binance, piani Auto-Invest dai file di dettaglio (in prova)**: nuova voce *Importazioni – Binance – Dettaglio Auto-Invest*, da usare dopo aver importato il CSV delle transazioni. Si scelgono insieme i file Subscription, Rebalancing e Redemption che Binance fornisce dalla cronologia Auto-Invest. Nel CSV delle transazioni molti movimenti dei piani restavano da classificare: un piano su più monete ha una sola uscita per tutte le monete comprate, un piano legato a un indice ha solo l'uscita, perché le monete restano nel conto Auto-Invest fino al riscatto, e fino a metà 2023 l'importo prelevato da Flexible Savings non compare affatto. Con i file di dettaglio ogni uscita viene abbinata alle monete che ha comprato, l'uscita mancante viene aggiunta, e gli acquisti, i ribilanciamenti mensili e il riscatto dei piani indice vengono ricostruiti sul sotto-wallet *Auto-Invest* di Binance. I movimenti importati mantengono quantità e valore. I ribilanciamenti sono scambi diretti fra le monete vendute e quelle comprate, ripartiti per valore. Il file non dice quali monete sono state vendute e quali comprate: per un piano riscattato il programma lo ricava esattamente dalle quantità, per un piano ancora aperto dai prezzi di mercato, e il resoconto avvisa che quantità di pochi centesimi possono avere il verso sbagliato. Gli scambi differiti abbinati in automatico che coincidono con il file restano come sono, quelli che univano movimenti di piani diversi vengono rifatti e il resoconto li elenca con la data. Se fra i file scelti c'è il Rebalancing ma manca il Subscription o il Redemption, prima di iniziare il programma lo segnala e permette di rifare la selezione o di proseguire comunque. I file si possono anche rinominare: il tipo di ciascuno si riconosce dal contenuto, e il fuso orario, che Binance scrive nel nome, se manca si ricava dal confronto con il file delle transazioni. Un file che non è un file di dettaglio Auto-Invest, per esempio il file delle transazioni scelto per errore, viene segnalato prima di iniziare.

- **Binance CSV, carta Binance**: le spese con la carta e i relativi cashback ("Spending" e "Cashback" del conto Funding, negli export dal 2022) non vengono più scartati come causali sconosciute. Una spesa in euro diventa un prelievo fiat e un rimborso un deposito fiat, una spesa pagata in cripto un prelievo da classificare, il cashback una ricompensa. Senza queste righe la giacenza in euro risultava più alta del reale. Chi aveva importato un file con queste righe può reimportarlo, vengono aggiunte solo quelle mancanti.

- **Bybit e Bybit EU, carta Bybit**: le spese con la carta Bybit nel file del conto Funding (righe "Bybit Card") non vengono più scartate come causali sconosciute. Una spesa in euro diventa un prelievo fiat. Una spesa coperta da una cripto, per esempio USDC, diventa una vendita della cripto in euro seguita dal prelievo fiat della spesa. Senza queste righe la giacenza in euro e quella della cripto usata risultavano più alte del reale, e la vendita della cripto mancava. Chi aveva importato un file con queste righe può reimportarlo, vengono aggiunte solo quelle mancanti.

- **Importazioni, configurazioni personalizzate**: nella finestra *Importazioni* una configurazione scritta dall'utente si riconosce a colpo d'occhio. Il nome dell'estrazione ha davanti *Personalizzata:*, nell'elenco è in corsivo e viene dopo le voci del programma dello stesso exchange, e passando il mouse sulla voce compaiono il nome del file e la cartella in cui si trova. Prima una configurazione personale che indicava un exchange già previsto, per esempio Bybit, compariva in mezzo alle voci del programma senza nessuna differenza.

- **Importazioni, opzioni non riconosciute**: se una configurazione di importazione contiene un'opzione che il programma non conosce, per un errore di battitura in una configurazione personale o perché la configurazione è stata aggiornata per una versione più recente del programma, il resoconto dell'importazione la segnala per nome. Prima veniva ignorata senza nessun avviso, e alcuni movimenti potevano risultare importati in modo diverso da quello previsto.

- **Configurazioni di importazione e versioni precedenti**: quando una configurazione distribuita con il programma viene aggiornata per una versione più recente di quella installata, il programma continua a usare la copia che aveva già, invece di sostituirla e smettere di proporla nella finestra *Importazioni*. La configurazione nuova arriva con l'aggiornamento del programma.

- **Prova di una configurazione di importazione**: nella finestra *Importazioni* il nuovo pulsante *Prova configurazione* legge un file CSV con una configurazione JSON, quella selezionata o una qualunque scelta da file, e mostra cosa produrrebbe senza importare nulla: i movimenti, un riepilogo di cosa esce da ogni causale, il saldo di ogni moneta e le righe scartate con il motivo. Dopo aver corretto il JSON la si ripete con un clic, e il resoconto si copia in testo semplice per incollarlo in un chatbot. Serve a chi scrive una configurazione personale, da solo o con un'IA.

- **Configurazioni della community**: le configurazioni di importazione scritte dagli utenti si possono condividere e scaricare. Nella finestra *Importazioni* il pulsante *Configurazioni della community...* apre il catalogo: quelle scaricate compaiono nelle tendine con il prefisso *Community:*. Dalla finestra della prova, dopo una prova senza problemi, *Condividi con la community...* propone la propria configurazione, che diventa scaricabile solo dopo il controllo dell'autore del programma. Insieme al file parte l'esito della prova, mai le righe del CSV. Il programma controlla ogni file scaricato prima di salvarlo. L'informativa sulla privacy descrive cosa viene conservato.

- **Tipi di movimento non riconosciuti nelle configurazioni**: se una configurazione di importazione assegna a una causale un tipo che il programma non conosce, per esempio `reward` in minuscolo, il resoconto dell'importazione lo segnala. Prima quelle righe venivano importate in silenzio come movimenti generici, classificati solo dalle monete, e una ricompensa risultava un deposito da classificare. L'importazione non cambia, cambia solo l'avviso.

- **Configurazioni di importazione scritte con un'IA**: il manuale *Configurazione JSON per le importazioni personalizzate* spiega cosa dare a un assistente come ChatGPT o Claude per fargli scrivere una configurazione corretta, e quali sono i suoi errori più frequenti. Le regole delle configurazioni sono pubblicate anche come schema JSON, che un editor come Visual Studio Code usa per segnalare gli errori mentre si scrive. La chiave `$schema` che lo collega non compare più fra le opzioni non riconosciute del resoconto. Il manuale documenta ora tutte le opzioni, e i suoi esempi sono controllati da un test. Due di questi esempi non funzionavano: quello con la sola data, che faceva scartare tutte le righe, e quello di Binance.

- **Wallet DeFi su BASE, token ricevuti e mai spostati**: la lettura dai nodi pubblici non vede un token ricevuto senza fare nulla e mai spostato, per esempio un airdrop. Nella finestra dei wallet DeFi il nuovo pulsante *Token da cercare...* permette di indicare il contratto di questi token, che si vedono nel proprio wallet o sulla pagina del wallet nell'explorer (il pulsante *Apri il wallet nell'explorer* la apre). Prima della ricerca il programma mostra nome, simbolo e saldo attuale di ogni token, perché molti token arrivati senza richiesta sono SCAM, e scarta quelli che l'importazione segue già. Poi ne cerca i movimenti e li aggiunge all'archivio, senza ripetere quelli che ci sono già, e scarica anche i nuovi movimenti del wallet come *Aggiorna Selezionati*. Da lì in avanti il token viene seguito dallo scaricamento normale. Gli NFT ERC-1155 non si possono cercare in questo modo.

- **Chiavi API nascoste a prima vista**: le chiavi degli exchange (API Key, API Secret e Passphrase della finestra *Exchange API*) e le ApiKey delle *Opzioni* (Helius, Etherscan, CoinGecko, Moralis, Cronos, UniSat, Blockscout, NodeReal) compaiono ora come pallini. Per leggerle si preme l'occhio a destra della casella, e premuto di nuovo le nasconde. Nella tabella delle chiavi della finestra *Exchange API* il pulsante *Mostra chiavi* le mette in chiaro, e premuto di nuovo le nasconde. Le chiavi restano salvate come prima, si tratta solo di non mostrarle a chi guarda lo schermo o in uno screenshot. La stessa finestra ricorda di usare solo chiavi **in sola lettura**, create sul sito dell'exchange senza permessi di trading né di prelievo: al programma basta leggere i movimenti.

- **Pacchetti più leggeri**: i pacchetti con Java incluso (installer e portable per Windows, portable e .deb per Linux, macOS e l'edizione del Microsoft Store) sono circa 30 MB più piccoli, la versione portable multipiattaforma circa 14 MB. Il Java incluso contiene solo le parti che il programma usa, e la libreria usata per ricavare gli indirizzi Bitcoin da una chiave xpub, ypub o zpub è stata sostituita da poche funzioni scritte apposta, che danno gli stessi indirizzi. Il Java incluso è ora la versione 25, che riceve gli aggiornamenti di sicurezza, e mostra date e numeri in formato italiano (per esempio i mesi del grafico delle giacenze), come già accadeva con il Java installato sul computer.

- **Importazione da Coinbase, ordini Advanced Trade e vendite**: le righe "Advanced Trade Buy", "Advanced Trade Sell" e "Sell" non vengono più scartate come causali sconosciute. Il file Coinbase riporta solo la cripto comprata o venduta, l'altra moneta (euro o USDC) e il suo importo stanno nella nota della riga: il programma li legge da lì. Un ordine contro euro diventa un acquisto o una vendita, uno contro USDC uno scambio fra cripto, e la commissione diventa un movimento a parte nella moneta in cui è stata pagata, come per gli acquisti semplici. Chi aveva già importato file di Coinbase può reimportarli, vengono aggiunte solo le righe mancanti.

- **Una ricompensa riportata a deposito da classificare**: nel menu del movimento, *Cambia Tipologia Reward* ha la nuova scelta *Deposito da classificare*. Serve quando l'exchange registra come ricompensa una moneta che non lo è, per esempio il BTTC ricevuto su Binance nella ridenominazione di BTT del gennaio 2022, che il file riporta come airdrop. Il movimento diventa un deposito da classificare, con lo stesso valore, e si classifica come gli altri, anche come scambio differito con il prelievo della vecchia moneta: prima la finestra dello scambio differito non lo proponeva. Vale per i movimenti selezionati che sono ricompense in entrata in cripto, non per quelle create dal programma classificando un trasferimento. La modifica compare nelle *Versioni precedenti* del movimento.

**Correzione di bug**

- **Cambia Tipologia Reward su più movimenti**: con più movimenti selezionati la nuova tipologia veniva assegnata solo al primo. Ora vale per tutti i movimenti selezionati che sono ricompense, e quelli che non lo sono vengono elencati senza essere modificati. Il rimborso di una ricompensa resta un rimborso: prima cambiandone la tipologia la descrizione perdeva "RIMBORSO". Il cambio di tipologia compare nelle *Versioni precedenti* del movimento.

- **Importazione da Coinbase, ETH in staking**: lo spostamento di ETH nello staking di Coinbase, che nel file diventa ETH2, era ignorato come un semplice spostamento interno. L'ETH risultava così più alto del reale e la successiva riconversione di ETH2 in ETH partiva da una giacenza di ETH2 negativa. Ora è uno scambio fra ETH ed ETH2. Gli spostamenti nello staking della stessa moneta continuano a non produrre movimenti. Chi aveva già importato file di Coinbase può reimportarli, viene aggiunto solo lo scambio mancante.

- **Importazione da Coinbase, acquisti dal saldo in italiano**: un acquisto pagato con il saldo in euro, con la nota in italiano ("using Portafoglio Euro"), era trattato come pagato con la carta: il programma registrava solo la cripto ricevuta e gli euro restavano nel saldo. Ora diventa un acquisto con uscita di euro e commissione a parte. Chi aveva già importato quel movimento deve eliminarlo prima di reimportare il file.

- **Bybit e Bybit EU, dettaglio depositi e prelievi**: un deposito veniva completato con hash e indirizzo solo se Bybit lo accreditava entro un'ora dall'orario del file. Su Bybit EU l'accredito può arrivare molte ore dopo (su un export verificato fino a 13 ore e mezza), e quei depositi restavano senza hash. Ora, se nell'ora successiva non c'è nessun deposito che corrisponda, il programma lo cerca fino a 24 ore dopo. Chi deposita spesso la stessa quantità non perde gli abbinamenti che aveva: il deposito arrivato entro l'ora resta quello scelto.

- **Simple Earn Flessibile, file scaricato dal sito**: lo storico delle ricompense Real-time APR si può scaricare anche dal sito di Binance, non solo dall'app, ed è lo stesso file. Il sito lascia scegliere il fuso orario e lo scrive nel nome del file: con un fuso diverso da UTC le ricompense Real-time, che nel file hanno solo il giorno, venivano spostate di qualche ora, e quella del 1° gennaio finiva nell'anno precedente. Ora il giorno di quelle righe viene letto sempre come giorno UTC, qualunque fuso si sia scelto. La guida [Particolarità degli import per exchange](particolarita-exchange.html#binance-simple-earn) spiega come scaricarlo dal sito.

- **Importazioni, loghi di KuCoin e Nexo Pro**: nella finestra *Importazioni* KuCoin e Nexo Pro comparivano senza logo e, come Gate.io, con la nota "(utilizzabile dalla versione …)" accanto al nome. Ora la nota non c'è più, perché tutte le versioni in uso propongono da sole solo le importazioni che sanno leggere, e i tre exchange hanno il loro logo.

- **Binance dalle API**: lo scaricamento è molto più veloce (sui dati di prova da 13 a 2 minuti per nove mesi di movimenti): le richieste seguono ora i limiti dichiarati da Binance per ciascun tipo di dato, invece di un'attesa fissa di diversi secondi fra una richiesta e l'altra. Un'interruzione momentanea della connessione non fa più fallire lo scaricamento, la richiesta viene ripetuta. Se una parte dei dati non arriva comunque, non viene importato nulla: prima si importava il resto, e i movimenti mancanti non venivano più ripresi dagli scaricamenti successivi. Corretti anche alcuni casi in cui dei movimenti andavano persi: i prelievi fatti nelle una o due ore successive all'ultimo movimento importato, le distribuzioni (airdrop, Launchpool, HODLer) arrivate nello stesso secondo a cavallo di due pagine della risposta, le esecuzioni dello stesso ordine con lo stesso orario oltre i 1000 trade di una coppia, le conversioni Convert oltre le 100 in un mese. I prelievi annullati, respinti o falliti e i depositi respinti non vengono più importati come movimenti. Le operazioni dei bot di trading di Binance non sono disponibili nelle API, ma solo nel file CSV: chi usa un bot deve importare Binance da CSV.

- **Prezzi da Bitget**: scaricando molti prezzi con Bitget come fonte preferita (per esempio importando i suoi CSV) circa un'ora su otto veniva rifiutata da Bitget per troppe richieste, e il prezzo veniva preso da un altro exchange o, per i token quotati solo su Bitget, restava mancante. Ora le richieste a Bitget sono più distanziate e non vengono più rifiutate: lo scaricamento dei prezzi da Bitget è più lento ma i prezzi arrivano dalla fonte preferita. I movimenti già importati mantengono i prezzi che hanno.

- **Importazione dei wallet Solana**: quando una transazione apre un conto token del wallet stesso (per esempio il conto USDC al primo acquisto di USDC), la rete trattiene circa 0,002 SOL di deposito, che tornano al wallet alla chiusura del conto. Il programma li contava come SOL ceduti: uno scambio di 1 SOL ne cedeva 1,00203928, uno scambio da USDC a STEP aveva anche 0,00203928 SOL fra le monete cedute, l'apertura di un conto vuoto diventava una cessione di SOL. Ora quel deposito non è più un movimento, né all'apertura né alla chiusura del conto. Resta un costo il deposito pagato per aprire il conto di un altro, per esempio inviando un token a un indirizzo che non l'ha mai avuto. Chi ha già importato un wallet Solana deve eliminarne i movimenti e reimportarlo: l'importazione riprende dall'ultimo blocco importato, e senza reimportare i depositi già contati come SOL ceduti non tornerebbero più nella giacenza alla chiusura dei conti, che potrebbe risultare negativa. Eliminando i movimenti si perdono anche le classificazioni e i collegamenti fatti a mano su quel wallet. Il saldo SOL del programma comprende i depositi ancora fermi nei conti token aperti, quindi può essere di poco superiore a quello mostrato dagli explorer.

- **Prezzi storici mancanti per alcune monete**: quando un exchange quota una moneta sia in euro sia in dollari (o USDT, USDC), il programma usava sempre la coppia in euro, anche nei periodi in cui questa non era ancora quotata, e il movimento restava senza prezzo. Era il caso, per esempio, di DAR su Binance, quotata in USDT da novembre 2021 ma in euro solo da maggio 2022. Ora, se la coppia in euro non ha quotazioni nel periodo, il programma prova le altre coppie dello stesso exchange. I prezzi già trovati in passato non cambiano. Per recuperare i movimenti già importati senza prezzo basta selezionarli e usare *Ricalcola Prezzi* dal menu contestuale: per quei movimenti il programma richiede di nuovo agli exchange le ore in cui non aveva trovato nessun prezzo.

- **Prezzi di MATIC ed ETH2**: dopo la migrazione di MATIC a POL (4 settembre 2024) nessun exchange quota più MATIC, e ETH2, l'ETH in staking di Coinbase, non ha mai avuto quotazioni in euro o in dollari. Il loro valore al 31/12, per esempio quello di fine 2024 nel quadro W/RW, restava così senza prezzo. Ora MATIC dal 4 settembre 2024 prende il prezzo di POL ed ETH2 quello di ETH, anche negli anni passati, perché prima non c'era nessun prezzo da cambiare. Un prezzo inserito a mano resta valido e ha la precedenza: prima, aggiungendo all'elenco una moneta da prezzare come un'altra, il prezzo personalizzato salvato con il suo nome (per esempio MATIC) veniva ignorato senza nessun avviso. I movimenti già importati mantengono il valore che hanno.

- **Archivio delle dichiarazioni su Windows**: generando il quadro T/RT o il quadro W/RW compariva l'errore "Dichiarazione non generata" e non veniva archiviato nessun file. Dopo averli scritti il programma rilegge i PDF per controllarli, e quella rilettura li lasciava bloccati fino a quando Windows non li rilasciava, così la cartella della dichiarazione non si poteva spostare nell'archivio. Ora i PDF vengono rilasciati subito.

- **Normative, ricerca nel testo dei documenti**: con i pacchetti che includono Java (installer e portable per Windows, portable e .deb per Linux, macOS e l'edizione del Microsoft Store) la ricerca non trovava le parole contenute in alcuni PDF e nel registro compariva l'errore "MacRoman". Al Java incluso nei pacchetti mancava il supporto di alcune codifiche dei caratteri, ora presente.

## Versione 1.0.65 {#versione-1065}

**Nuove implementazioni**

- **Archivio delle dichiarazioni.** Ogni stampa dei quadri viene ora conservata nella nuova scheda *Dichiarazioni – Archivio dichiarazioni*. Una stampa del quadro W/RW produce sempre insieme il PDF del quadro W, quello del quadro RW e l'Excel di dettaglio, e lo stesso vale per il quadro T/RT (PDF del quadro T, del quadro RT ed Excel di dettaglio, nuovo, con il suo pulsante *Crea Excel con i Dettagli*). I file nascono tutti dallo stesso calcolo e vengono archiviati solo se sono riusciti tutti, così PDF ed Excel riportano sempre gli stessi valori. Il pulsante *Stampa Report PDF* chiede quale dei due quadri aprire, il pulsante *Crea Excel con i Dettagli* apre l'Excel. Se il quadro non era aggiornato viene ricalcolato prima, e una ristampa identica all'ultima archiviata propone di aprire quella invece di crearne una copia. Selezionando una dichiarazione nell'elenco si vedono sotto i totali, i righi, le opzioni di calcolo usate e lo stato dei file, che sono di sola lettura e vengono controllati: un file modificato o mancante viene segnalato. Per ogni anno si può segnare la dichiarazione **definitiva**, una per il quadro W/RW e una per il T/RT: segnandola viene creato anche un backup dei dati, collegato alla dichiarazione, e cancellare quel backup chiede una conferma in più. Dopo ogni calcolo dei quadri, se i valori di un anno sono diversi da quelli della sua definitiva (per esempio dopo una reimportazione o una correzione di prezzi) lo segnala una scritta rossa accanto ai pulsanti di stampa, con le differenze nel suggerimento. A ogni dichiarazione si possono aggiungere una nota (per esempio data di invio e protocollo) e degli allegati (per esempio la ricevuta di presentazione), due dichiarazioni dello stesso quadro si possono confrontare voce per voce, e ognuna si può esportare in uno zip o eliminare (con una conferma in più se è la definitiva). Il pulsante *Continuità* confronta i valori finali della definitiva W/RW dell'anno prima con i valori iniziali della dichiarazione scelta, solo se entrambe sono calcolate con le stesse opzioni e con il metodo dei soli valori di inizio e fine anno, perché con gli altri metodi i due valori differiscono per costruzione. Le dichiarazioni entrano nei backup, e il ripristino di un backup le aggiunge senza mai sovrascrivere o cancellare quelle già presenti.
- **Documenti di origine nelle dichiarazioni archiviate.** Generando una dichiarazione si può includere la copia dei documenti da cui nascono i movimenti (CSV importati, risposte JSON/NDJSON scaricate dagli exchange e dalle blockchain, così come sono stati ricevuti): in modo predefinito quelli con movimenti nell'anno della dichiarazione, a scelta anche quelli degli anni precedenti. Ogni copia ha un nome che dice gruppo wallet, exchange o wallet, periodo e documento, un indice li elenca tutti e segnala i movimenti dell'anno senza documento, e il programma controlla che non vengano modificati. Entrano nell'esportazione e nei backup.
- **Quadro W/RW: cambiare anno non avvia più il calcolo.** Scelto l'anno, il quadro si svuota e si calcola con il pulsante *Calcola*. La stampa e l'Excel, se il quadro è vuoto, lo calcolano da soli prima di generare i file.
- **Excel di dettaglio del quadro W/RW rifatto.** Prima conteneva una serie di fogli per ogni rigo del quadro, con i dati di tutto il gruppo e di tutto l'anno: un gruppo diviso in periodi o con la liquidità ripeteva gli stessi fogli, il dettaglio della liquidità mancava e le giacenze di inizio e fine anno erano calcolate in modo diverso dal quadro (comprendevano gli euro e le giacenze negative, e i prezzi venivano richiesti di nuovo). Ora c'è un riepilogo di tutti i righi e, per ogni gruppo wallet, i fogli *Calcoli* (ogni riga con il periodo a cui appartiene), *Giacenze* (le stesse del calcolo del quadro), *Liquidità* (giacenza media, valore dichiarato, codice bene, Stato estero e IVAFE) e *Movimenti*. Corretto anche un difetto per cui l'esportazione cambiava i nomi dei gruppi nel dettaglio del quadro a video.
- **Quadro W/RW: un rigo per ogni periodo di detenzione cripto.** I periodi CRYPTO impostati in *Opzioni – Gruppi Wallet Crypto* sono ora attivi nei calcoli e nella stampa: il rigo di un gruppo si spezza a ogni confine di periodo, ogni periodo ha il proprio bollo (con il bollo pagato l'imposta è zero), il nome del rigo riporta le date del periodo e selezionandolo il dettaglio mostra solo le sue righe. Con le opzioni B, C e D le posizioni aperte vengono chiuse e riaperte al confine allo stesso prezzo di mercato, quindi il valore finale di un periodo coincide con il valore iniziale del successivo. Un gruppo senza periodi continua a produrre il rigo unico di sempre. In stampa i righi di periodo portano le date accanto al nome e i giorni del rigo col bollo restano vuoti quando il gruppo ha detenuto per tutto il periodo.
- **Quadro W/RW: giacenze di inizio e fine anno calcolate in un solo modo.** L'opzione A e la casella "per i Wallet che pagano già il bollo mostra solo giacenza ad inizio fine anno" usano ora la stessa fotografia delle giacenze. Gli NFT sono sempre compresi e gli euro mai. Per un gruppo aperto durante l'anno la moneta del primo movimento vale quanto il movimento, e una giacenza negativa non viene conteggiata ma segnalata come errore.
- **Quadro W/RW, liquidità: nuova opzione "valore finale maggiore fra giacenza media e saldo".** In *Opzioni – Opzioni Calcolo RW/W* (disattivata in modo predefinito), per la liquidità diversa dai conti correnti (codice bene 14) il valore finale diventa il maggiore fra la **giacenza media annua** e il saldo a fine periodo, e su quel valore si calcola anche l'IVAFE. La giacenza media si calcola come nelle istruzioni della DSU e nella scheda dell'Agenzia delle Entrate: somma dei saldi giornalieri divisa per 365, anche se il conto è stato aperto solo per una parte dell'anno e anche negli anni bisestili. Il rigo avvisa quando prevale la media e con l'opzione attiva compare anche un conto aperto e svuotato nel corso dell'anno. I conti correnti continuano a riportare la giacenza media, come chiedono le istruzioni del quadro RW.
- **Stampa del Quadro W/RW**: nel riepilogo "Opzioni scelte per il calcolo" c'è ora anche la parte della liquidità (regime dell'IVAFE, calcolo del valore finale, conti correnti).
- **Importazione da Gate.io**: storico degli scambi a pronti, depositi e prelievi, con il gruppo wallet preconfigurato *Wallet 115 – Gate.io*.
- **Importazione da Bybit (in prova)**: file CSV "Asset Change Details" dei conti Spot, Funding, Derivati e Unified Trading Account, con scambi, commissioni, conversioni dei residui, airdrop e interessi di Earn e Launchpool. I giroconti fra i conti non vengono importati. Il risultato dei derivati (PnL e funding) viene sommato per giorno e resta da classificare. Un impegno su Launchpad e il token ricevuto nello stesso momento diventano un unico scambio, mentre un token arrivato da solo resta un airdrop (chi aveva già importato il 2022 con la configurazione precedente deve prima eliminare quei movimenti, altrimenti si duplicano).
- **Bybit, dettaglio depositi e prelievi**: il file "Withdraw & Deposit History" (in *Importazioni – Bybit*, "Dettaglio depositi e prelievi") non aggiunge movimenti ma completa i depositi e i prelievi già importati dagli Asset Change Details con l'hash della transazione, l'indirizzo e, nelle note, la rete usata. Va letto dopo i file dei conti Spot e Funding e si può rileggere senza effetti. La commissione di rete non viene separata: la crea la classificazione del trasferimento.
- **Valore degli scambi fra cripto**: il valore di uno scambio fra due cripto-attività è ora quello della cripto **ricevuta**, come indica la circolare 30/E dell'Agenzia delle Entrate. Il valore della cripto ceduta si usa solo se quella ricevuta non ha un prezzo, oppure se il prezzo della cripto ricevuta non è preciso (rilevato a più di 5 minuti dallo scambio, come capita in DeFi con i token poco scambiati) mentre quello della cripto ceduta lo è, oppure se il prezzo viene da CoinMarketCap e si discosta di oltre il 10% da quello dell'altra cripto, prezzata da un'altra fonte (quasi sempre un token omonimo).
- **Prezzi da CoinMarketCap e monete con lo stesso simbolo**: quando CoinMarketCap ha più monete con lo stesso simbolo (per esempio BIT: BitDAO e Biconomy Exchange Token), il programma sceglie quella con la capitalizzazione media più alta nel periodo del movimento (il volume degli scambi solo se nessuna ha una capitalizzazione), invece di quella più grande oggi. Il nome della moneta scelta compare nel dettaglio del movimento accanto al simbolo, per esempio "BIT (BitDAO)". I prezzi di CoinMarketCap già scaricati per questi simboli vengono cancellati una volta e riscaricati. Dalla sezione prezzi, scaricando da tutte le fonti, compaiono anche le altre monete con lo stesso simbolo, così si può scegliere quella giusta. Corretta anche la ripartizione degli scambi a più monete (conversione dei piccoli saldi): quando il prezzo della moneta ricevuta era sottostimato, tutta la moneta ricevuta finiva su una sola moneta ceduta. Se da una parte c'è una valuta legale (acquisto o vendita) il valore resta l'importo in valuta. Vale per i movimenti importati o riprezzati da questa versione in avanti: i valori già presenti non cambiano, e chi vuole applicarlo anche a quelli può rigenerarne i prezzi.
- **Movimenti su derivati riconosciuti**: i movimenti su contratti derivati (perpetui e futures di Bybit, Dual Investment di Binance) vengono marcati all'importazione. Il programma non ne gestisce ancora il trattamento fiscale e li considera come cripto-attività, cosa che non è corretta: lo segnalano l'avviso a fine importazione e le stampe dei quadri W/RW e T/RT, dall'anno del primo movimento su derivati in poi (le cripto ricevute o pagate con i derivati restano nel calcolo anche negli anni dopo).
- **Prezzi dagli exchange per i token equivalenti.** Token come WETH su Base o USDC.e vengono prezzati come la moneta principale sugli exchange, al minuto invece che all'ora. L'elenco delle equivalenze sta in un file aggiornato all'avvio. Per non cambiare le dichiarazioni già presentate, le equivalenze nuove valgono solo dalla loro data di introduzione, salvo attivare in *Opzioni – Opzioni di Calcolo* la casella *Prezzi dagli exchange anche per gli anni già dichiarati* (attiva in modo predefinito solo sulle installazioni nuove).
- **Menu contestuale dei movimenti: "Unisci movimenti omogenei".** Fonde in un unico movimento più movimenti dello stesso tipo, della stessa moneta e dello stesso wallet registrati a non più di un secondo l'uno dall'altro, sommandone quantità e controvalore. La conferma avvisa dei possibili effetti su una futura reimportazione e sul costo di carico.
- **"Trasla Orario" con l'ora legale.** Lo spostamento può ora essere diverso fra i movimenti in ora solare e quelli in ora legale, per sistemare file importati con orari privi del fuso. Per esempio, un file con orari UTC letto come ora italiana va spostato di +1 ora d'inverno e di +2 d'estate, un file che usa sempre l'ora invernale italiana di 0 d'inverno e +1 d'estate. Prima di procedere un riepilogo mostra quanti movimenti cadono in ciascuna stagione e un esempio di come cambia l'orario.
- Nella finestra **"Filtri..."** della tabella dei movimenti c'è ora la scelta **"Periodo (Anno)"**, che imposta in un clic le date sull'intero anno scelto.
- **Recupero dei prezzi più veloce**: il processo che interroga gli exchange resta attivo fra una richiesta e l'altra invece di ripartire ogni volta, "Riscarica tutti i prezzi dalle fonti" interroga gli exchange in parallelo, e il calcolo del quadro RW e di "Giacenze a data" non richiede più i prezzi delle monete con giacenza zero. Anche le importazioni dei wallet DeFi e quelle via API di OKX e Binance chiedono ora i prezzi tutti insieme prima di registrare i movimenti, invece che uno alla volta: su un wallet Cronos di prova l'importazione è passata da oltre 3 minuti a circa un minuto. I prezzi scelti non cambiano.
- **Binance, ricompense "Real-time APR" di Simple Earn.** Nuova importazione in *Importazioni – Binance*, "Simple Earn Flessibile - Real-time APR": legge lo storico delle ricompense scaricato dall'app di Binance (*Assets – Earn – Flexible – Rewards*, icona di download) e importa le ricompense Real-time APR, che l'export normale delle transazioni non contiene. Senza, le ricompense di Simple Earn Flessibile risultano più basse del vero e dopo un prelievo totale su Binance compaiono giacenze negative. I Bonus Tiered APR dello stesso file sono già nell'export normale e vengono ignorati.
- **Novità della versione.** Al primo avvio dopo un aggiornamento si apre una finestra con le novità e i bug corretti della versione. Si può riaprire in qualunque momento dal pulsante *Novità di questa versione* della finestra *Informazioni*, che compare facendo clic sul titolo in alto.
- **Colonna e dettaglio "Operazione".** I movimenti che fanno parte della stessa operazione (un trasferimento con la sua commissione, uno scambio differito, un contratto Dual Investment, i movimenti con lo stesso hash sullo stesso exchange) sono riconosciuti insieme. La colonna "Operazione", da mostrare con *Colonne...*, permette di filtrarli tutti con un clic, e nel dettaglio di un movimento la riga "Operazione" elenca gli altri, con il loro tipo e la classificazione.
- Il **manuale del Quadro W/RW** è stato aggiornato con i periodi di detenzione, la liquidità presso intermediari esteri, i gruppi preconfigurati e la stampa. I PDF dei manuali hanno ora la stessa veste grafica delle stampe dei quadri.
- **OKX via API, interessi di Simple Earn.** OKX restituisce lo storico degli interessi solo per l'ultimo mese: se lo storico ricevuto comincia dopo il giorno richiesto, un avviso a fine scaricamento lo segnala. In *Exchange API – Particolarità OKX* si può attivare il calcolo degli interessi mancanti (disattivato in modo predefinito). Il totale di ogni periodo si ricava da saldo, sottoscrizioni e riscatti ed è esatto, la ripartizione sui singoli giorni è invece una stima in proporzione al capitale. Una seconda casella estende il calcolo agli anni fino al 2025.
- **OKX via API, posizioni On-chain Earn convertite in token di Liquid Staking.** Quando OKX converte una posizione On-chain Earn chiusa in un token di Liquid Staking (per esempio LYUSDC), il programma ricostruisce lo scambio dallo storico degli ordini, che ora viene letto per intero. Prima il movimento restava fra quelli non riconosciuti. LYUSDC viene prezzato come USDC.
- **Carta OKX.** Il wallet usato dalla carta OKX, sulla rete X Layer, viene individuato da solo dagli scaricamenti via API, mostrato in *Exchange API – Particolarità OKX* e aggiunto ai wallet DeFi nel gruppo di OKX. I suoi movimenti si leggono dal nodo pubblico della rete, senza ApiKey, e si aggiornano a ogni scaricamento OKX via API: gli interessi di Aave sul saldo diventano ricompense, i pagamenti con la carta sono registrati come uscite al momento dell'addebito, le ricariche dal conto OKX vengono abbinate al trasferimento corrispondente e i cashback riconosciuti come tali. Si leggono le monete usate dalla carta (USDG, USDC, USDT e il token di Aave su USDG), mentre i movimenti di OKB vengono solo segnalati.
- **Rettifica giacenza anche per le valute FIAT.** In *Giacenze a data* e in *Verifica Saldi Negativi* il pulsante di rettifica funziona ora anche per euro e altre valute. Non si sceglie nessuna classificazione: il programma crea un deposito FIAT o un prelievo FIAT della differenza, con una nota facoltativa come per le cripto.
- **Token con prezzo classificabili come SCAM.** Un token che ha movimenti valorizzati si può ora identificare come SCAM, anche in blocco da *Depositi e Prelievi*. Prima il programma lo impediva, ora mostra un avviso che ricorda che di solito i token scam non hanno prezzo e chiede conferma. Il valore dei movimenti non viene azzerato.
- **Giacenze a data: confronto con la blockchain.** Scegliendo un singolo wallet DeFi con il sotto-wallet "Wallet" (oppure "Tutti", se il wallet non ne ha altri) compare accanto alla quantità la colonna *Qta Blockchain*, con la giacenza letta direttamente dalla blockchain alla fine del giorno scelto: verde se coincide con quella calcolata dai movimenti, rossa se è diversa, "n.d." se non si è potuta leggere. La lettura avviene in sottofondo dai nodi pubblici della rete, senza ApiKey, e le giacenze di giorni passati, lette una volta, restano memorizzate. Funziona per le reti EVM (per MONAD solo alla data di oggi), non per Solana e Bitcoin. Si confrontano solo i token già presenti nell'archivio, e alcune differenze sono attese: i token SCAM mostrano saldi finti, i token che crescono da soli (per esempio gli aToken di Aave o quelli "reflection") non corrispondono mai ai movimenti.
- **Dettaglio movimento: giacenza sulla blockchain prima e dopo.** Per i movimenti dei wallet DeFi (reti EVM) i riquadri delle monete coinvolte hanno due righe in più, *Blockchain prima* e *dopo*: il saldo dell'indirizzo alla fine del blocco precedente e alla fine del blocco del movimento, verde se coincide con l'archivio e rosso se no. Il confronto è con il sotto-wallet "Wallet" prima e dopo tutti i movimenti dello stesso blocco (una transazione ne genera spesso più di uno, per esempio la commissione), e la nota sotto il riquadro riporta i due valori dell'archivio. I token messi in una piattaforma o in garanzia non sono più all'indirizzo e restano fuori dal confronto. Corretto anche un difetto di visualizzazione: con due monete il riquadro di destra poteva uscire dalla finestra, coprendo la freccia per passare al movimento successivo.
- **Giacenze a data: la blockchain anche nel dettaglio dei movimenti.** Con lo stesso wallet, la tabella di dettaglio dei movimenti di una moneta ha la colonna *Qta Blockchain* subito dopo la *Qta Residua*: il saldo dell'indirizzo alla fine del blocco di ogni movimento, verde se coincide con la Qta Residua e rosso se no, così si vede da quale movimento l'archivio smette di coincidere. Le righe dello stesso blocco (per esempio la commissione e lo scambio) si confrontano tutte con la Qta Residua dopo l'ultima di loro, riportata nel suggerimento della cella. I movimenti senza blocco, come le rettifiche inserite a mano, restano vuoti. Tutte le giacenze lette dalla blockchain per blocchi o giorni passati (colonna, dettaglio dei movimenti, dettaglio del movimento) restano memorizzate: la seconda volta compaiono subito, senza rete. Quelle di oggi si leggono ogni volta, perché cambiano. Se il saldo della blockchain è diverso, il pulsante *Sistema Qta Residua* propone già nel campo la giacenza che fa coincidere l'archivio con la blockchain alla fine di quel blocco, e il resto procede come sempre (classificazione del movimento di rettifica e nota). Corretto anche un difetto di *Giacenze a data*: dopo una rettifica o un altro aggiornamento dei movimenti il dettaglio del wallet tornava a "Tutti", e il sotto-wallet scelto andava rimesso a mano.
- **Wallet DeFi su BASE senza ApiKey.** Da ottobre 2026 Blockscout fornisce i dati di BASE solo con un abbonamento, e nessun altro explorer gratuito copre più la rete. I movimenti si leggono ora direttamente dai nodi pubblici della rete, senza nessuna chiave. Vengono trovate tutte le transazioni fatte dal wallet (anche quelle fallite e le approvazioni, con la commissione completa della quota pagata a Ethereum), i movimenti di ETH e quelli dei token principali della rete e dei token già presenti nell'archivio per quel wallet. Resta fuori solo un token ricevuto senza fare nulla, diverso da questi e mai spostato. La prima importazione di un wallet richiede da uno a qualche minuto, le successive pochi secondi. Gli smart wallet (per esempio Coinbase Smart Wallet) non sono supportati e vengono segnalati.
- **Importazione da KuCoin**: storico degli scambi a pronti (con le commissioni), depositi e prelievi, nel gruppo wallet preconfigurato di KuCoin. Le esecuzioni parziali di uno stesso ordine restano movimenti separati. I futures non sono importabili, perché l'export non contiene il risultato delle posizioni.
- **Importazione di più file in una volta.** Nella finestra di importazione si possono scegliere più file insieme (tranne il dettaglio del Dual Investment): vengono letti uno dopo l'altro, con una sola barra di avanzamento e un solo resoconto finale. Ogni file resta un documento di origine a sé.
- **Crypto.com App**: i tre export dell'app (cripto, Fiat Wallet e carta) hanno la stessa intestazione, e caricarne uno nella voce sbagliata produceva solo causali sconosciute. Ora il programma riconosce il file e avvisa, con la possibilità di saltarlo o di importarlo comunque. L'importazione ha anche la barra di avanzamento e si può interrompere.
- **Wallet Bitcoin con più indirizzi.** Per chi non ha la chiave pubblica estesa (xpub) si può creare un wallet Bitcoin con un nome e indicarne gli indirizzi (*Gestione Wallet*, "Indirizzi del wallet BTC..."). Le transazioni fra gli indirizzi dello stesso wallet, come i resti, restano interne invece di diventare prelievi e depositi. Se l'elenco cambia dopo l'importazione, il programma rilegge solo gli indirizzi aggiunti o tolti e chiede conferma prima di cambiare i movimenti.
- **Giroconti in euro fra due wallet propri.** I versamenti in euro fra Coinbase e Coinbase Pro vengono abbinati da soli a fine importazione come trasferimento fra wallet propri, invece di restare un prelievo e un deposito slegati (che i filtri contavano come versamenti dalla banca e il quadro W/RW come apporti). Reimportare uno dei due file sistema anche i movimenti già presenti. Lo stesso abbinamento si può fare a mano: *Classifica Movimento* è ora disponibile anche per depositi e prelievi in valuta, e l'assegnazione automatica di *Depositi/Prelievi* abbina anche le coppie in euro (un deposito non può mai superare il prelievo).
- **Commissioni collegate ai movimenti.** Ogni commissione importata ricorda a quale movimento appartiene: il dettaglio del movimento la mostra, e cancellando un movimento il programma chiede se eliminare anche le commissioni rimaste senza movimento. Per l'archivio già presente, *Opzioni – Commissioni collegate* le abbina in base all'hash e all'orario (i casi dubbi restano liberi), e il menu contestuale ha *Collega commissioni* e *Scollega commissioni*. È solo un'informazione: nessun calcolo cambia.
- **Dettaglio movimento: giacenze prima e dopo.** Sotto i dati del movimento due riquadri, rosso per la moneta in uscita e verde per quella in entrata, mostrano quantità, valore, costo di carico e costo unitario prima e dopo il movimento, per tutti i wallet, per il gruppo e per il wallet. Il dettaglio indica anche il costo di carico unitario delle due gambe e, dove il calcolo non lo registra (per esempio nei trasferimenti fra wallet dello stesso gruppo), un costo di carico solo informativo. Il nuovo pulsante *Modifica Prezzo* apre la gestione del prezzo, e al ritorno il dettaglio mostra già i valori ricalcolati.
- **Giacenze a data: valori unitari e plusvalenza latente.** La tabella principale ha le colonne *Valore unitario*, *Costo unitario* e *Plus/Minus Latente* (valore meno costo di carico), vuote per le valute, per le giacenze a zero o negative e per i token senza prezzo. Il dettaglio dei movimenti di una moneta ha il costo di carico di ogni movimento, il prezzo unitario e, per la quantità residua dopo ogni riga, valore, costo di carico e plusvalenza latente. Le colonne si scelgono con il pulsante *Colonne...*, e solo quantità e plusvalenza sono colorate in verde o rosso. Il prezzo delle ricompense molto piccole non risulta più zero: si usa la quotazione di mercato del momento, quando è disponibile.
- **Prezzi dei token di pool e vault.** I token che rappresentano quote di pool o vault (per esempio i GM di GMX o i vault di Beefy) vengono ora prezzati automaticamente da DefiLlama, che prima per questi token non veniva interrogato. Per un token non censito su CoinGecko il prezzo si accetta solo se DefiLlama lo dà come affidabile. I prezzi già usati nelle dichiarazioni presentate non cambiano: si cercano solo i token che non hanno ancora un prezzo.
- **Prezzi dei token DeFi solo da DefiLlama e CoinGecko.** Nuova casella in *Opzioni – Opzioni di Calcolo*, disattivata in modo predefinito. Per alcuni token con indirizzo (le monete più diffuse e i prezzi più vecchi di un anno) il programma cercava il prezzo sugli exchange per simbolo, che può appartenere a un'altra moneta con lo stesso nome. Con la casella attiva il prezzo si prende solo da DefiLlama e CoinGecko, e se non lo hanno il token resta senza prezzo. Fanno eccezione i token dichiarati equivalenti a una moneta degli exchange (per esempio WETH o USDC.e). Vale per i prezzi cercati da quel momento: i movimenti già valorizzati non cambiano, mentre i valori di fine anno del quadro W/RW, che si calcolano ogni volta, possono cambiare.
- **Recupero dei movimenti non riconosciuti (OKX via API).** Se uno scaricamento OKX trova movimenti di un tipo che il programma non conosce ancora, li scarta, e lo scaricamento successivo, che riparte dall'ultimo movimento, non li richiede più. Ora il programma li ricorda: quando una nuova versione o un aggiornamento delle configurazioni li fa riconoscere, li rilegge dai documenti già scaricati e propone di recuperarli, sempre chiedendo conferma. I movimenti che mancano di sicuro sono già spuntati, quelli dubbi (per esempio cancellati a mano) no, e quelli non scelti non vengono più proposti. Riconosciute anche le sottoscrizioni dirette di Liquid Staking (per esempio da SOL a OKSOL).
- **Avviso di E-Money mancanti.** Premendo *Calcola* nei quadri W/RW e T/RT il programma avvisa se nei movimenti ci sono token di moneta elettronica noti (per esempio EURI) che non sono nella sezione E-Money, e dice quanti movimenti cambierebbero aggiungendoli, anche negli anni già dichiarati. Si può aggiungerli e ricalcolare, rimandare o non riceverne più la proposta.
- **Filtri della tabella dei movimenti.** Nel pannello dei dettagli del movimento c'è la scheda *Filtri*, con l'elenco di tutti i filtri attivi e un cestino per togliere ciascuno. Accanto ai dettagli del movimento un imbuto filtra la tabella su quel valore (wallet, causale, moneta, hash, documento di origine...), e un secondo clic toglie il filtro. Nella finestra *Filtri...* l'elenco delle monete mostra solo quelle dei movimenti che gli altri criteri lasciano vedere.
- **Depositi/Prelievi**: la tabella dei movimenti da classificare ha la colonna *Note* e il pulsante *Colonne...* per scegliere le colonne da mostrare.
- **Binance CSV**: riconosciute altre causali. La rata di un piano Auto-Invest ("Auto-Invest - Deduct Asset", negli export dal 2024) viene abbinata all'acquisto come scambio, i pagamenti e gli invii di Binance Pay ("Transfer") diventano depositi e prelievi da classificare, "NFT - Burn and Win" una ricompensa, e "Pre Auth - Capture" viene ignorata perché si annulla da sola. Nei Dual Investment liquidati in una moneta diversa i movimenti generati stanno ora sul sotto-wallet *Dual Savings*, come per quelli nella stessa moneta.
- **Bybit Funding**: riconosciute altre causali. "Convert" diventa un acquisto quando si cede una valuta (per esempio da euro a USDC) e uno scambio fra due cripto, "Airdrop Bonus" un airdrop. Un prelievo annullato ("Withdrawal Canceled") non viene più importato, insieme al prelievo che annulla, perché la moneta non è mai uscita dal conto. Se il prelievo non è nello stesso file, o è già nell'archivio da un'importazione precedente, l'annullamento entra come deposito da classificare.
- **Errori, "Parte del LIFO mancante".** La spiegazione elenca ora tutte le cause e non rimanda più d'ufficio a *Verifica Saldi Negativi*, che segnala solo le giacenze andate sotto zero, di solito per movimenti in entrata mai importati. Con l'opzione che esclude dal calcolo i movimenti non classificati, i depositi non classificati non caricano il LIFO pur contando nella giacenza, e lo stesso vale per un deposito classificato come trasferimento senza il prelievo collegato o con una classificazione che il calcolo non riconosce: in questi casi la giacenza non va sotto zero. Questi ultimi due casi vengono ora cercati e contati, con l'elenco dei movimenti da riclassificare. La finestra offre di mostrare i movimenti con il LIFO mancante, di aprire *Verifica Saldi Negativi* oppure, se ci sono movimenti non classificati e l'opzione è attiva, di classificarli.
- **Chiusura del programma**: alla domanda se salvare i movimenti modificati c'è anche *Annulla*, che lascia il programma aperto. Prima una chiusura per sbaglio costringeva a salvare o a perdere le modifiche.
- Tolto il pulsante "Ricalcola i prezzi delle transazioni" da *Opzioni – Varie*: il ricalcolo si fa dal menu contestuale dei movimenti (*Ricalcola Prezzi*), sui movimenti selezionati.
- La **guida ai file JSON di importazione personalizzati** è stata completata con tutte le chiavi che il programma legge, e corretta negli esempi.
- **Giacenze a data: confronto con le giacenze di OKX e Binance.** Scegliendo il wallet OKX o Binance (con il dettaglio "Tutti"), la colonna usata per la blockchain mostra la giacenza secondo l'exchange, verde se coincide con quella calcolata dai movimenti e rossa se no. Per OKX il dato viene dagli scaricamenti via API già fatti, senza nuove richieste, e vale per qualunque data coperta dagli scaricamenti, anche nel dettaglio dei movimenti, riga per riga. Le monete in Earn sono comprese, ma OKX non fornisce gli interessi maturati nel frattempo: in quei periodi la cella riporta "(in Earn)" e non si colora. Per Binance il confronto è possibile solo con la data di oggi, perché Binance non conserva lo storico dei saldi: si leggono i saldi attuali di Spot, Funding e Simple Earn tramite le API configurate. Restano fuori i Dual Investment in corso.
- **Giacenze a data: grafico della giacenza e del valore.** Il dettaglio dei movimenti di una moneta ha ora due schede, *Tabella* (quella di sempre) e *Grafico*. Il grafico mostra la giacenza della moneta nel tempo e il suo valore in euro, sia a fine di ogni giorno sia subito dopo ogni movimento. Su ogni variazione della giacenza c'è un triangolo, verde se la moneta entra e rosso se esce: passandoci sopra con il mouse si vede il movimento che l'ha causata (data, tipo, wallet, quantità e giacenza dopo), e con un clic lo si ritrova selezionato nella tabella. Con la rotella del mouse si ingrandisce un periodo, trascinando ci si sposta, con un doppio clic si torna a tutto il periodo. I prezzi giornalieri vengono scaricati la prima volta in sottofondo dagli exchange (dalla serie di DefiLlama per i token con indirizzo) e poi restano memorizzati. Servono solo al grafico: non cambiano nessun valore dei calcoli e delle dichiarazioni.
- **Classificazione movimenti: i filtri di associazione si ricordano.** I due elenchi in fondo alla finestra (escursione in percentuale sul valore e in giorni) conservano l'ultima scelta e la ripropongono alla classificazione successiva.
- **Rinomina di un token senza indirizzo o rete.** Il messaggio di conferma dice ora a quali movimenti si applicherà (tutti i wallet, un wallet con i suoi sotto-wallet o un gruppo), quanti sono, e ricorda che la rinomina non resta valida per le importazioni future.

**Correzione di bug**

- **Note nella classificazione verso piattaforme a rendita e collaterale.** Classificando un movimento come trasferimento a o da Vault/Piattaforma a Rendita, o come blocco o sblocco di collaterale, la nota scritta nella finestra non veniva salvata. Ora resta sul movimento classificato.
- **Scambio differito, movimenti proposti.** Nella finestra di classificazione l'intervallo di date dei movimenti proposti era spostato: mancavano quelli nelle 24 ore precedenti e comparivano alcuni oltre il limite scelto. Ora l'intervallo va da 24 ore prima fino al numero di giorni indicato dopo il movimento.
- **Installazione di ccxt, avvisi di npm.** Le comunicazioni di npm che non sono errori (per esempio la disponibilità di una nuova versione) comparivano nella finestra degli errori. Ora vanno fra i messaggi normali.
- **Quadro W/RW con l'opzione A**: se un gruppo nasceva durante l'anno con un deposito in euro, quegli euro entravano nel valore iniziale del rigo cripto.
- **Quadro W/RW, gruppi col bollo con "mostra solo giacenza"**: una giacenza negativa veniva conteggiata come valore positivo, gonfiando il rigo invece di segnalare l'errore.
- Un **prezzo personalizzato** salvato per un token con indirizzo non veniva più riletto quando il token è fra quelli prezzati come una moneta equivalente.
- La marcatura automatica come SCAM per imitazione non colpisce più un token il cui indirizzo è censito su CoinGecko (per esempio l'ETH di Wormhole su BSC).
- **Reimportazione di un file già importato.** Senza *Sovrascrivi esistenti* non rientrano più come doppioni il prelievo di un trasferimento classificato con commissione (la classificazione ne riduce la quantità) e i movimenti spostati con *Trasla Orario* o modificati a mano dopo l'importazione. Con *Sovrascrivi esistenti* vengono ritrovati anche questi movimenti e quelli registrati da versioni precedenti con un formato diverso dell'identificativo, che prima venivano aggiunti una seconda volta. La sovrascrittura sostituisce sempre il movimento con la riga del file, e se il movimento era stato modificato a mano la versione sostituita resta consultabile da *Versioni precedenti*.
- **Scambi differiti chiusi nello stesso secondo.** Due scambi differiti con il deposito nello stesso secondo (succedeva con i contratti Dual Investment) ricevevano lo stesso identificativo e il secondo cancellava il primo. Ora gli identificativi sono sempre distinti. Gli scambi già danneggiati compaiono fra gli errori come *Movimenti collegati incoerenti* e si ricostruiscono da lì, oppure ripassando il file di dettaglio del Dual Investment.
- **Dual Investment nella stessa moneta.** La ricompensa ora sta sul sotto-wallet *Dual Savings* e il Settlement resta con la quantità importata, così annullare o eliminare uno qualunque dei movimenti del contratto riporta tutto come era stato importato. I contratti già abbinati si aggiornano ripassando il file di dettaglio, senza cambiare nessun calcolo.
- **Movimenti generati o collegati.** I movimenti creati dal programma (commissioni, gambe degli scambi differiti e dei Dual Investment) e quelli che fanno parte di un gruppo classificato si possono modificare solo nel prezzo e nelle note: cambiare quantità, monete o data lasciava il gruppo incoerente. Allo stesso modo non si possono più duplicare, e il duplicato di un movimento è ora indipendente dall'originale (non ne condivide più la storia delle modifiche).
- **Eliminazione di più movimenti collegati.** Selezionando insieme il prelievo e il deposito di uno scambio differito, il deposito restava in archivio. Anche il numero dei movimenti cancellati insieme a un wallet è ora quello vero.
- **Binance, bonus di Simple Earn contati due volte.** Negli export del 2026 Binance scrive ogni bonus giornaliero di Simple Earn ("Bonus Tiered APR") due volte, sul conto Earn ("Rewards Income") e sul conto Spot ("Interest" o "Rewards"), con la stessa moneta e quantità, e il programma li importava tutti e due come reddito. Ora si importa solo la riga del conto Spot, come negli anni precedenti. Per i bonus già importati, all'avvio un avviso propone di togliere i doppioni: solo le righe del conto Earn che hanno la gemella sul conto Spot, che resta. L'avviso si può non mostrare più, e la stessa pulizia è in *Opzioni – Pulizie – Ricompense Binance doppie*. Fino al salvataggio si può annullare. Dopo la pulizia possono comparire giacenze negative su Binance: le ricompense "Real-Time APR" di Simple Earn non compaiono in nessun export di Binance (si vedono nell'app, in *Earn – Flexible – Rewards*) e vanno aggiunte a parte.
- **Documento di origine dei movimenti generati.** Le gambe e le ricompense create dai Dual Investment, dai trasferimenti verso piattaforme e dallo scambio WCRO-CRO prendono ora il documento del movimento da cui nascono, e il filtro per documento le mostra insieme agli altri. Quelle già in archivio vengono completate all'avvio.
- **Importazione CSV di OKX, fuso orario.** Gli export recenti di OKX indicano nella prima riga il fuso scelto sul sito (per esempio UTC+8), e il programma ora lo usa. Prima gli orari venivano sempre letti come ora italiana, e un export fatto con un altro fuso finiva spostato di qualche ora. Se il file indica UTC+1 o UTC+2 resta l'ora italiana, che tiene conto dell'ora legale.
- **OKX, conto Funding.** I rendimenti di BTC Yield+ e le righe "Rewards" vengono importati come ricompense, e la sottoscrizione di BTC Yield+ viene ignorata come le altre sottoscrizioni Earn. Prima finivano fra i movimenti non riconosciuti.
- **Binance, "Cash Voucher".** Vengono importati come ricompensa e non più come cashback. I movimenti già importati restano come sono: si correggono a mano o reimportando il file con *Sovrascrivi esistenti*.
- **OKX, importazione CSV della cronologia di trading**: le conversioni fatte dall'app OKX ("Convert") non venivano importate e finivano fra le righe sconosciute.
- **"Ricalcola Prezzi" dal menu contestuale** aggiornava il valore del movimento ma non la fonte del prezzo, che restava quella di prima.
- **Dettaglio movimento di un trasferimento**: la riga "Trasferimento da ... a ..." restava vuota quando il trasferimento aveva una commissione o altri movimenti collegati.
- Un'**equivalenza di prezzo** arrivata con l'aggiornamento automatico delle configurazioni valeva solo dal riavvio successivo, e un movimento importato nel frattempo poteva restare senza prezzo.
- L'**avviso sui derivati** a fine importazione compariva anche per righe che l'importazione ignora (i giroconti del conto Derivati di Bybit).
- Quando un **explorer DeFi** rifiuta una richiesta senza ApiKey, il messaggio mostrava una pagina web illeggibile e invitava a riprovare. Ora chiede di inserire la ApiKey.
- Dopo una richiesta fallita alla Banca d'Italia, ogni conversione fra dollaro ed euro aspettava un secondo, e le importazioni con molti prezzi in dollari rallentavano parecchio.

## Versione 1.0.64 {#versione-1064}

**Nuove implementazioni**

- **Quadro W/RW: periodi di detenzione per gruppo wallet.** In *Opzioni – Gruppi Wallet Crypto* ogni gruppo ha ora una propria sezione con i wallet che lo compongono, i dati fiscali e i **periodi di detenzione** di cripto e di liquidità, uno per riga. Per ogni periodo si indicano le date, se paga il bollo, lo Stato estero (scelto dall'elenco ufficiale delle istruzioni di Redditi PF), l'identificativo dell'intermediario e, per la liquidità, se si tratta di un conto corrente. Ogni operazione (aggiunta, modifica, rimozione, ripristino) viene salvata subito. I gruppi preconfigurati per ciascun exchange (*Wallet 101…*) mostrano il nome dell'exchange nei titoli e nelle finestre, e i gruppi già esistenti non vengono più modificati da soli.
**Attenzione:** La nuova parte relativa allo spit programmato per la parte cripto per ora non è attivo nelle stampe e calcoli del quadro RW.
- **Quadro W/RW: parte "liquidità" (valuta estera).** Il quadro comprende ora anche i righi per la liquidità detenuta presso un intermediario estero (codice bene 14), con la propria numerazione nella stampa e una casella in *Opzioni – Opzioni Calcolo RW/W* per includerli o meno. L'IVAFE ordinaria dello 0,20% si può **liquidare oppure lasciare "solo monitoraggio"**, a scelta. Per gli Stati a fiscalità privilegiata (D.M. 4 maggio 1999) dal 2024 l'aliquota è il 4‰. Per un **conto corrente estero** l'IVAFE è calcolata in misura fissa (34,20 euro) e la stampa mostra lo Stato e gli estremi del conto. Per l'app di Crypto.com la parte in euro viene dal Fiat Wallet e non dai movimenti cripto.
- **Stampa del Quadro W/RW rinnovata**: copertina, testata e piè di pagina numerato, fascia di margine con marcatore e filigrana; i nomi dei gruppi e le note stanno ora nella colonna di sinistra senza sovrapporsi al modulo.
- **Note di compilazione dei quadri W/RW e T/RT in un file aggiornabile.** I testi stampati in coda ai quadri vivono in un file di configurazione, scaricato dal repository e aggiornato all'avvio, così una correzione non richiede più una nuova versione del programma. Gli anni si adeguano da soli alla dichiarazione, e una nota può avere un testo diverso a partire da un certo anno. Corretta in questa occasione la nota W8 del 730. Anche i dati predefiniti del quadro W/RW sono ora in un file di configurazione.
- **Nuova colonna "costo di carico" in "Giacenze a data"**, accanto a "Valore (in Euro)": mostra il costo di carico LIFO della giacenza indicata sulla riga, calcolato per gruppo wallet come nel motore delle plusvalenze, quindi corretto anche per le monete arrivate da un giroconto interno.
- **Nuova opzione "Scambi cripto-cripto sempre fiscalmente rilevanti"** (*Opzioni – Opzioni di Calcolo*, disattivata in modo predefinito): se attiva, anche lo scambio fra monete con le stesse caratteristiche e funzioni genera plusvalenza e nuovo costo di carico.
- **Storico delle modifiche ai movimenti.** Quando si modifica a mano un movimento, le versioni precedenti vengono conservate e si consultano dal pulsante **"Versioni precedenti"** nel dettaglio e nella modifica del movimento, anche prima di salvare. Modificare un movimento non fa più perdere il documento di origine e gli altri campi di provenienza, e la cancellazione di un movimento o di un documento è provvisoria fino al salvataggio, come il resto delle modifiche.
- **"Trasla Orario"** (menu contestuale dei movimenti): sposta l'orario di un movimento insieme ai movimenti che il programma ha generato a partire da esso, senza sciogliere gli abbinamenti manuali (trasferimento tra wallet, scambio differito, vault/collaterale).
- **Gestione documentale**: un documento si può ora eliminare **insieme ai suoi movimenti** (selezione singola, doppia conferma); come per i movimenti, la cancellazione resta provvisoria finché non si salva.
- **Segnalazioni e invio dei log.** Dal resoconto di importazione si possono inviare allo sviluppatore le righe scartate ("Invia segnalazione errori") e da *Opzioni – Varie – Invia log* si può inviare il log del programma. Il testo è modificabile e parte solo dopo una conferma esplicita; indirizzi e hash vengono oscurati prima dell'invio. Anche le segnalazioni molto grandi vengono compresse e inviate senza errori.
- **Binance – Dual Investment.** Un nuovo file di dettaglio ("Advanced Earn – Dual Investment History", in *Importazioni – Binance*) permette di abbinare acquisto e liquidazione di ogni contratto. Se la moneta liquidata è la stessa sottoscritta, la parte sottoscritta si sposta su un sotto-wallet senza toccare il costo di carico e solo l'eventuale eccedenza diventa reddito (reward); se la moneta è diversa si crea un vero scambio. A fine importazione compare sempre un avviso fiscale sui **derivati**: il programma li tratta come scambio cripto-cripto per approssimazione e non ne calcola il reddito. Le **giacenze negative** sono ora conteggiate anche fra gli errori del pulsante "Errori".
- **Nexo**: l'importazione è divisa in due configurazioni, una per il formato a 9 colonne e una per il nuovo formato a 11 colonne (con Fee e Fee Currency); le **commissioni** dei movimenti "Exchange" vengono ora registrate come movimento a sé, perché le commissioni sulle cripto-attività non sono deducibili.
- **Binance CSV**: riconosciute altre causali (Launchpool Airdrop – User Claim Distribution, BNSOL Staking – Extra Rewards, Token Swap – Redenomination/Rebranding, Earn – Airdrop Distribution, Vega – Funds Transfer, Funds Transfer Request – Vega, Campaign Rewards).
- Il **doppio clic su una riga** ora apre il dettaglio del movimento.
- **Menu contestuale dei movimenti: "Ricalcola Prezzi"**. Rifà la ricerca del prezzo solo sui movimenti selezionati, **compresi i token per cui una ricerca precedente era fallita**. Prima di partire mostra un avviso con le conseguenze; l'operazione si può annullare dalla finestra di avanzamento e in quel caso non cambia nulla.
- **Menu contestuale dei movimenti: "Conferma che il token non ha prezzo (valorizza a Zero)"**. Disponibile quando tutti i movimenti selezionati sono senza prezzo: li valorizza a zero senza scrivere un prezzo personalizzato per il token, e il movimento esce dagli errori "senza prezzo".
- **Rettifica giacenza in "Giacenze a data"**: i messaggi che propongono il movimento di deposito o prelievo da creare indicano ora anche il controvalore approssimato (per esempio «di 0.00032244 TAO corrispondenti a circa € 0.10»), ricavato dai prezzi già noti, da un movimento vicino nel tempo o, in mancanza, dalla ricerca online.
- Nella finestra **"Classifica Movimento"** e nella tabella **Depositi/Prelievi** è comparsa la colonna **"Gruppo Wallet"**, accanto a "Exchange / Wallet", con l'alias del gruppo (o il suo nome se non ne ha uno).
- Nella tabella dei movimenti, dal pulsante **"Colonne..."** si possono ora mostrare anche due colonne aggiuntive: **"Fonte Prezzi"** (da dove viene il prezzo usato per il movimento) e **"Alias Gruppo Wallet"** (l'alias del gruppo wallet a cui il movimento appartiene, o il nome del gruppo se non ne ha ancora uno).

**Correzione di bug**

- **Importazione da CSV: una virgola dentro un campo tra virgolette** (per esempio la virgola delle migliaia in un importo) spezzava la riga e faceva scartare il movimento. Ora il campo viene letto correttamente.
- Se **cade la connessione durante un'importazione** il programma riprova tre volte nell'arco di nove minuti e poi la interrompe senza importare nulla: prima scriveva migliaia di movimenti con prezzo `0,00`, senza avvisare.
- **CCXT non veniva mai aggiornato** dopo la prima installazione, per cui uno scaricamento OKX poteva fermarsi con l'errore `exchange[metodo] is not a function`. Ora la versione installata viene controllata e riallineata, l'aggiornamento non è più influenzato dalla configurazione npm della macchina (che su Windows lo faceva fallire) e un calo di connessione durante l'installazione non blocca più l'intera finestra.
- **Prezzi**: il pre-scarico di un import da CSV non riscarica più a ogni importazione le stesse coppie moneta/ora e non richiede più quotazioni per il segnaposto "-" usato da Nexo al posto della commissione; i **prezzi personalizzati** su NFT e token con nome non maiuscolo venivano salvati ma non riletti; il flag "prezzato" dei movimenti a cui è stato dato un valore a posteriori si aggiorna, e non restano più contati fra gli errori "senza prezzo".
- **E-Money**: il simbolo di un token (per esempio `EURe`) è ora riconosciuto senza distinguere maiuscole e minuscole, con la possibilità di un'eccezione per singolo token.
- **Binance CSV**: le conversioni ("Binance Convert") con le due gambe registrate a un secondo di distanza venivano importate come un deposito e un prelievo separati invece che come un unico scambio.
- L'opzione **"I movimenti di deposito e prelievo non classificati non vengono considerati"** non restava salvata e si spegneva a ogni avvio (chi l'aveva usata deve rispuntarla una volta). L'**ultima cartella usata** nell'importazione generica da JSON è ora la stessa degli altri import.
- Il download dei movimenti dai wallet **DeFi** su **AVAX** e **BASE** non usa più come predefinito Moralis, che non offre più un piano gratuito: chi installava il programma ora non riusciva a scaricare nulla senza abbonamento.

## Versione 1.0.63 {#versione-1063}

**Nuove implementazioni**

- L'importazione da **Coinbase Pro / GDAX** è stata rivista. I movimenti di questo exchange vengono ora registrati su un wallet distinto ("Coinbase Pro"), separato da quello di Coinbase "retail", così le due storie non si mescolano. I versamenti e i prelievi in euro sono classificati come depositi/prelievi FIAT, quelli in cripto come depositi/prelievi cripto; i giroconti tra Coinbase e Coinbase Pro arrivano come trasferimenti cripto da abbinare a mano con "Classifica Movimento".
- La voce **"Classifica Movimento" del menu contestuale** (tasto destro sulla tabella dei movimenti) lavora ora sulla selezione multipla, esattamente come il pulsante omonimo: si possono selezionare più righe e classificarle in un'unica finestra.
- L'evidenziazione blu della riga selezionata si estende ora anche agli **elenchi e ai menu a tendina**, per un aspetto uniforme in tutta l'interfaccia.

**Correzione di bug**

- **Coinbase – acquisti pagati con carta.** Un acquisto di cripto pagato con carta, Apple/Google Pay o bonifico, cioè non con il saldo in euro dell'exchange, generava un'uscita di euro fittizia e un movimento di commissioni separato. Ora questi acquisti vengono caricati al costo pieno effettivamente sostenuto, con plusvalenza zero e senza movimentare euro, come già avviene per l'app di Crypto.com. Gli acquisti pagati con il saldo in euro ("using EUR Wallet") restano invariati.
- **Coinbase – giroconti verso Coinbase Pro.** Un prelievo da Coinbase con nota del tipo "Sent … to …" non viene più abbinato automaticamente a un deposito su Coinbase Pro: quel trasferimento resta da classificare a mano, evitando accoppiamenti errati.
- Il pulsante **"Elimina Movimento"** aveva il testo scuro su sfondo rosso pieno, poco leggibile: ora il testo è bianco.

## Versione 1.0.62 {#versione-1062}

**Nuove implementazioni**

- Nuova scheda di primo livello **"Dichiarazioni"**, subito prima di "Opzioni", che raccoglie in un unico posto i quadri **RW** (giacenze / Quadro W-RW) e **RT** (plusvalenze / Quadro T-RT) — prima erano dentro "Analisi Crypto" — insieme alla nuova sezione "Normative".
- Aggiunta la sezione **"Normative"**: un archivio consultabile dei documenti fiscali ufficiali sulle cripto-attività (leggi di bilancio dal 2023, circolari, risoluzioni e risposte dell'Agenzia delle Entrate, istruzioni dei modelli Redditi PF e 730, modulistica ISEE/DSU e le norme europee MiCA/DAC8). Si può filtrare per categoria, data e testo (ricerca nel contenuto dei documenti, non solo nei titoli), vedere il dettaglio di ogni fonte e aprire la copia locale o la pagina ufficiale. L'archivio si scarica e si aggiorna da GitHub con il pulsante "Aggiorna da GitHub".
- Aggiunta nel quadro RT la scheda **"Dettagli x Tipologia"**: per l'anno selezionato mostra le plusvalenze suddivise per tipologia di movimento e per mese, con l'elenco dei movimenti di ciascuna tipologia e un grafico dell'andamento mensile.
- La **tabella dei movimenti è ora personalizzabile**: il nuovo pulsante "Colonne..." apre una finestra da cui scegliere quali colonne mostrare, mentre l'ordine e la larghezza si regolano trascinando le intestazioni. La disposizione scelta viene ricordata tra una sessione e l'altra e si può riportare a quella predefinita.
- Nuova finestra **"Informazioni"**, che si apre facendo clic sul titolo in alto: elenca le fonti dei dati e le relative attribuzioni ("Powered by CoinGecko", "Powered by Etherscan.io APIs" e le altre), ricorda che i dati restano sul dispositivo e che il programma non gestisce seed phrase o chiavi private, e riporta le licenze dei componenti di terze parti.
- Aggiunte l'importazione da **Coinbase** (formato dell'app "retail") e da **Coinbase Pro / GDAX**, più configurazioni di importazione pronte per **Nexo** e **Bitget**. Il motore di importazione da CSV generico è stato ampliato: rimozione automatica dei simboli di valuta (€, $, £, ¥) dagli importi, righe con la stessa moneta in entrata e in uscita trattate come un movimento a gamba singola, consolidamento giornaliero dei micro-interessi, riclassificazione di una riga in base al testo della colonna note (per riconoscere per esempio i premi "Coinbase Earn", i referral e gli airdrop che arrivano tutti come semplici accrediti), controvalore degli acquisti calcolato al netto della sola commissione, e un testo descrittivo che compare come suggerimento sulle voci di importazione.
- Scaricamento da **OKX** più solido: i codici delle causali OKX sono stati spostati in un file aggiornabile senza cambiare versione del programma (come già le altre mappe di importazione), è stato riconosciuto un nuovo tipo di scambio a pronti, e uno scaricamento che raggiunge il tetto massimo di pagine può ora essere ripreso dal punto esatto alla richiesta successiva, invece di ripartire da zero.
- Aggiunto un **servizio prezzi condiviso**, facoltativo e **disattivato in modo predefinito** (casella in "Opzioni" - "Opzioni di Calcolo"): quando attivo, i prezzi vengono richiesti a un servizio che li ricava direttamente dallo stato delle pool on-chain, senza usare dati di mercato di terzi. Il cambio euro/dollaro applicato a questi prezzi è più preciso, perché letto anch'esso da una pool on-chain al minuto invece che dal cambio giornaliero della Banca d'Italia. Anche il pulsante "Riscarica tutti i prezzi dalle fonti" interroga questo servizio quando è abilitato.
- Aggiunto il supporto a tre nuove blockchain: **HyperEVM**, **Ink** e **Robinhood Chain**.
- Quando un deposito di denaro (fiat) avviene nello stesso istante di un acquisto fatto con quel denaro, il deposito viene ora registrato **prima** dell'acquisto: non si vede più una giacenza in euro momentaneamente negativa. Vale per i movimenti importati d'ora in avanti.
- I manuali sono ora **pagine web** sul sito del progetto, aggiornate e con la stessa veste grafica del sito; i PDF restano comunque disponibili. Il programma è **uscito dalla fase beta**: il titolo della finestra non riporta più la dicitura "Beta".

**Correzione di bug**

- Corretto un difetto per cui lo **scaricamento di Binance dalle API non funzionava più**: gli script di importazione rifiutavano la richiesta e l'operazione si interrompeva subito.
- Uno **scaricamento OKX incompleto** importava comunque la parte già arrivata: siccome la data di partenza si ricava dall'ultimo movimento importato, questo spostava la data in avanti e lasciava un buco permanente nello storico. Ora, se lo scaricamento non è completo, non viene importato nulla e la richiesta successiva riparte esattamente dallo stesso punto.
- Corretto un difetto per cui la **finestra principale poteva aprirsi più grande dello schermo** (dimensioni salvate da un monitor più grande, o ripristinate da un backup fatto su un'altra macchina), con bordi e pulsanti fuori dall'area visibile e la finestra non più ridimensionabile con il mouse. Ora la dimensione viene adattata allo schermo e la finestra parte massimizzata.
- La colonna "Timestamp" dei movimenti poteva contenere del testo non numerico proveniente da una colonna dell'export di Binance; ora quel valore viene sempre mantenuto corretto e i movimenti già in archivio vengono sistemati automaticamente al primo avvio.
- La voce "Crea movimento di scambio da Deposito/Prelievo" funziona ora anche quando il deposito e il prelievo sono registrati con un secondo di differenza, e non solo quando cadono nello stesso identico istante; il movimento riunito prende la data della gamba più recente.
- Rimossa la fonte prezzi **CryptoCompare**, il cui livello gratuito è stato chiuso il 21/05/2026: le richieste rispondevano solo con un errore di chiave mancante.
- Il suggerimento del pulsante "Errori" non compare più quando il conteggio è a zero, e quando compare torna a disporsi su tre righe come previsto.
- Nel tema scuro il pannello di dettaglio della sezione "Normative" restava con lo sfondo bianco.

## Versione 1.0.61 {#versione-1061}

**Nuove implementazioni**

- Il programma porta ora con sé il proprio carattere di scrittura (Noto Sans): l'interfaccia si presenta identica su Windows, Linux e Mac e su tutti i pacchetti, senza più dipendere dai font installati sul computer. Prima, quando i caratteri richiesti non erano presenti, il sistema ne sostituiva uno diverso e più largo, e i testi risultavano tagliati o i pulsanti disallineati. Di conseguenza sono state tolte dai testi dell'interfaccia le frecce, le spunte e le emoji, che un carattere di questo tipo non può disegnare.
- La marcatura "Identifica come SCAM" nella scheda "Depositi e Prelievi" funziona ora su più righe selezionate: viene chiesta una sola conferma, che elenca anche i token con movimenti diversi da un semplice deposito o prelievo, e al termine viene mostrato un riepilogo dei token marcati e di quelli lasciati normali con il motivo (già classificati, privi di indirizzo o rete, già SCAM, con movimenti valorizzati). Selezionando una riga sola resta la procedura di prima, che è anche l'unica in grado di togliere lo stato SCAM.

**Correzione di bug**

- I movimenti scaricati da OKX vengono ora registrati tutti sul wallet "Principale", invece di essere divisi fra i conti "Funding" e "Trading": i giroconti fra i due conti non vengono importati (sono spostamenti interni allo stesso utente), quindi tenerli separati faceva risultare la stessa moneta spezzata su due portafogli senza alcun movimento che ne giustificasse il passaggio. Questo era anche un bug perchè poteva portare a segnalazioni di giacenze negative su okx in maniera errata. La stessa regola vale per le importazioni da file CSV di OKX. I movimenti già presenti in archivio non vengono spostati: restano sui wallet con cui erano stati importati.
- Corretto un difetto per cui, marcando come SCAM più token in una volta sola, lo stesso token poteva essere proposto due volte.
- Corretto un difetto per cui il primo scaricamento dell'elenco monete da CoinMarketCap riempiva il log di errori: l'elenco contiene token diversi con lo stesso simbolo e i doppioni venivano rifiutati uno per uno. Ora vengono scartati prima della scrittura tenendo il token più importante, e il conteggio mostrato al termine indica quelli effettivamente registrati.

## Versione 1.0.60 {#versione-1060}

**Nuove implementazioni**

- Aggiunto lo scaricamento dei movimenti di OKX direttamente dalle API: si inseriscono chiave, secret e passphrase in "Opzioni" - "ApiKey" e il programma scarica i movimenti del conto Funding e del conto Trading, i rendimenti di Simple Earn (raggruppati per giornata) e, su richiesta, l'archivio storico trimestrale che risale fino al 2021. Alla prima richiesta dell'archivio viene chiesto l'anno di apertura del conto, per non interrogare OKX su trimestri sicuramente vuoti. Aggiunte anche due configurazioni di importazione (OKX_Funding e OKX_Trading) per i nuovi formati di export CSV di OKX.
- Aggiunta la funzione "Backup / Ripristino" in "Opzioni": crea un unico archivio con movimenti, impostazioni, gruppi wallet, prezzi inseriti a mano, documenti di origine e configurazioni di importazione, e permette di ripristinarlo su un'altra installazione o dopo un problema. Prima di ogni ripristino viene creato in automatico un backup di sicurezza e, al termine, il programma ricalcola le plusvalenze e verifica che coincidano con quelle registrate nell'archivio. I prezzi di mercato scaricati si possono includere per intero oppure nel solo insieme necessario ai calcoli, molto più leggero.
- Ogni movimento importato ricorda ora il file da cui proviene: il file viene conservato compresso e può essere riaperto dal programma. La nuova scheda "Analisi Crypto" - "Gestione Documentale" (raggiungibile anche da "Opzioni" - "Export" - "Documenti di origine") elenca i documenti importati con i movimenti che ancora vi fanno riferimento, e permette di aprirli, esportarli o eliminare quelli non più collegati a nessun movimento. Per gli scaricamenti da API e da wallet DeFi, dove un file non esiste, viene registrata la risposta grezza del server (senza le chiavi API, che vengono oscurate).
- Aggiunto il pulsante "Filtri..." sopra la tabella dei movimenti, che raccoglie in un'unica finestra tutti i criteri di filtro e ne indica il numero attivo. Fra questi ce n'è uno nuovo: si possono vedere i soli movimenti che hanno un documento di origine, quelli che non ce l'hanno, oppure quelli provenienti da un documento preciso.
- Le plusvalenze vengono ora ricalcolate a partire dal primo movimento realmente modificato invece che sempre dall'inizio: dopo un'importazione o una modifica l'attesa si riduce di molto. Si può tornare al ricalcolo completo togliendo la spunta in "Opzioni" - "Opzioni di Calcolo".
- L'avvio del programma mostra ora una vera barra di avanzamento, con l'indicazione della fase in corso, al posto della scritta "Caricamento in corso".
- Tema scuro rivisto: sfondi e testi hanno colori propri (prima restavano quelli grigi di serie della libreria grafica) e tutte le icone vengono ridisegnate in chiaro. Il cambio di tema, come già prima, richiede un riavvio per essere completo.
- Aggiunta in "Opzioni" - "Pulizie" la nuova scheda "Compattazione database": i database, in particolare quello dei prezzi, arrivano a occupare molto più spazio dei dati che contengono e possono essere compattati senza perdere una sola riga (nell'archivio di prova 2.847 MB tornati a 565 MB). La compattazione viene eseguita anche in automatico alla chiusura del programma, ma solo quando il recupero è consistente.
- Aggiunto il supporto a dieci nuove blockchain: OP Mainnet, Linea, Blast, Unichain, World Chain, Taiko, Abstract, Katana, Sonic e Mantle.
- Le importazioni della rete Gnosis passano da Etherscan a Blockscout, perché dal 01/09/2026 Gnosis esce dal piano gratuito di Etherscan; il passaggio è automatico anche per chi aveva già salvato le proprie preferenze nella scheda "Preferenze Provider DeFi".
- Il menu contestuale dei movimenti (quello che compare facendo tasto destro del mouse sulla tabella) lavora ora sulla selezione multipla: "Elimina Movimento" cancella tutte le righe selezionate e "Modifica Note" le aggiorna tutte, chiedendo se accodare il nuovo testo a quello esistente oppure sostituirlo; le voci che agiscono su un solo movimento vengono invece disattivate quando le righe selezionate sono più di una.
- La finestra per la modifica delle note ha ora la stessa veste grafica di tutti gli altri messaggi del programma, con l'area di testo su più righe ridimensionabile.
- Aggiunta nella scheda "RT & Analisi P&L" la casella "Non calcolare l'anno corrente", che evita di dover recuperare i prezzi attuali di tutte le monete in giacenza quando interessa solo un anno già chiuso.
- Migliorata sensibilmente la velocità di caricamento e di aggiornamento delle tabelle: sono state riscritte le somme delle colonne, il controllo dei saldi negativi e la conversione delle date, le tabelle secondarie vengono calcolate senza bloccare la finestra e la tabella dei movimenti viene ricostruita solo quando la sua scheda è a video. La scheda "Gestione Token Scam" si apre ora in una frazione del tempo di prima.
- Aggiunte le righe alternate colorate, che seguono il tema, anche alla tabella dei gruppi wallet e a quella dei documenti importati.

**Correzione di bug**

- Il pulsante "Interrompi" ferma ora davvero l'importazione da API, anche durante il recupero dei prezzi, e un errore restituito dall'exchange interrompe l'importazione invece di proseguire in silenzio con dati parziali.
- Corretto un difetto per cui due ricalcoli delle plusvalenze avviati contemporaneamente (per esempio durante un'operazione di massa) potevano sovrascriversi a vicenda producendo risultati errati senza alcun messaggio.
- Corretto il messaggio "Transazione eliminata" che compariva grigio e non disegnato fino al termine del ricalcolo; ora il lavoro viene svolto prima, con il puntatore di attesa.
- Corretto un difetto per cui alcune voci del menu contestuale non venivano disattivate sulle righe che non sono movimenti.
- Corretto un difetto per cui, tornando sulla scheda dei movimenti, venivano ricalcolate inutilmente anche le tabelle secondarie, e un altro per cui cambiando l'opzione "calcola LIFO per gruppo wallet" alcune tabelle venivano ricalcolate due volte.
- Corretti alcuni errori che potevano presentarsi all'apertura di una richiesta di conferma priva della finestra proprietaria.

## Versione 1.0.59 {#versione-1059}

**Nuove implementazioni**

- Aggiunta la funzione "Chiedi a IA": dal tasto destro su un movimento si prepara una domanda già compilata con i dati di quel movimento e la si apre direttamente nel chatbot preferito (ChatGPT, Claude, Perplexity, Copilot, Grok, Gemini, Le Chat, DeepSeek). Il testo viene sempre mostrato prima dell'invio e può essere modificato, e si può scegliere tra il profilo "Completo" (tutti i dati, hash e indirizzi compresi, così il chatbot può leggere la transazione sull'explorer) e il profilo "Generico" (solo causale, monete e rete, senza dati riconducibili a voi). Facoltativamente si può chiedere anche l'inquadramento fiscale italiano. L'elenco dei chatbot è modificabile nel file ChatbotIA.json che viene creato nella cartella dei dati.
- Aggiunte due nuove voci nel menu contestuale dei movimenti: "Separa in Deposito/Prelievo", che divide un movimento che coinvolge due monete nelle due operazioni indipendenti, e "Crea movimento di scambio da Deposito/Prelievo", che è l'operazione inversa e riunisce un deposito e un prelievo non ancora classificati in un unico scambio.
- Aggiunte in "Opzioni" - "Varie" due caselle per registrare nel log i JSON scaricati durante l'importazione dei wallet DeFi e durante il recupero dei prezzi: servono per capire cosa è successo quando qualcosa non torna (attenzione, con queste opzioni attive il log cresce molto rapidamente).
- Gli e-money token denominati in euro (per esempio EURe) vengono ora valorizzati 1:1 con l'euro a partire dalla data di decorrenza indicata in "Opzioni" - "E-MoneyToken", senza più cercarne la quotazione su DefiLlama o sugli altri servizi di prezzo: un token che per definizione vale un euro non deve risentire di spread e illiquidità. Ne beneficiano anche le valorizzazioni di inizio e fine anno del quadro RW.
- Nella classificazione manuale, scegliendo "CASH OUT" su un prelievo DeFi il programma propone ora di classificare allo stesso modo anche tutti gli altri prelievi non ancora classificati dello stesso tipo (stessa rete, stessa moneta, stesso contratto e stessa controparte), come già avveniva per i reward e per i trasferimenti.
- La rimozione in blocco dello stato SCAM da più token chiede ora una sola conferma ed esegue tutto con una barra di avanzamento, invece di chiedere conferma token per token rallentando progressivamente.
- Il programma è ora rilasciato con licenza MIT: aggiunti i file LICENSE e THIRD-PARTY-LICENSES.md con l'elenco delle licenze dei componenti di terze parti utilizzati.

**Correzione di bug**

- Corretto un bug che poteva rovesciare la classificazione dei movimenti sui token con molti decimali: quando la quantità veniva scritta in notazione scientifica (ad esempio 2.5E-9) il programma la interpretava come quantità in uscita anche se era positiva, trasformando un deposito in un prelievo oppure scartando del tutto il movimento con l'errore "Movimento incoerente, ci sono due monete in uscita". Il difetto riguardava sia la creazione dei movimenti sia le importazioni da exchange e da wallet DeFi. Era comunque un caso più teorico che reale.
- Corretto un bug per cui l'importazione di un wallet DeFi si interrompeva del tutto, perdendo anche le transazioni già scaricate, quando l'explorer restituiva un token privo di nome, simbolo o decimali (tipico dei contratti di spam e degli airdrop non richiesti) oppure quando si usava Blockscout, che chiama in modo diverso il campo con l'hash delle transazioni interne.
- Corretta una particolarità della rete Gnosis: alcune piattaforme movimentano ancora il vecchio contratto del token EURe insieme a quello nuovo e l'explorer registrava così due movimenti al posto di uno; ora il movimento sul contratto vecchio viene eliminato automaticamente.
- Modificata l'assegnazione del prezzo quando si associa un prelievo a un deposito avvenuto molto tempo dopo (ad esempio dei token depositati in attesa di un airdrop): ora il prezzo viene preso alla data del deposito, cioè nel momento in cui l'operazione si conclude davvero, seguendo il principio di cassa; prima in alcuni casi veniva usato il prezzo della moneta uscente alla data del prelievo.
- Corretto un bug per cui alcuni swap sulla rete Solana venivano importati come "PRELIEVO CRYPTO" invece che come "SCAMBIO CRYPTO", perché una delle due gambe dello scambio veniva scartata quando l'explorer non indicava a chi apparteneva il token account; il proprietario viene ora ricercato in due modi diversi prima di rinunciare. La correzione vale per le nuove importazioni.
- Corretto un errore che poteva presentarsi aprendo l'explorer da un movimento privo di alcuni dati.

## Versione 1.0.58 {#versione-1058}

**Nuove implementazioni**

- Aggiunta DefiLlama come nuova fonte per lo scaricamento dei prezzi.
- Aggiunta rete Poligon , Monad e migliorato importazioni Solana (un grazie al contributor peppers96)
- Aggiunto pulsante per riscaricare tutti i prezzi dalle fonti direttamente dalla finestra di modifica prezzo di un movimento.
- Aggiunta in "Opzioni" una nuova sezione "Preferenze Provider DeFi" per scegliere, per ogni chain, quale provider usare per scaricare i movimenti (Moralis/Blockscout/Etherscan/Cronoscan/Helius), con possibilità di impostare un URL e un'ApiKey Blockscout personalizzati.
- Aggiunto supporto alla Blockchain Gnosis Chain.
- Aggiunta pulsante per identificazione automatica SCAM/SPAM dei depositi/prelievi non classificati tramite l'API di sicurezza GoPlusLabs, estesa anche al riconoscimento di token phishing/impersonazione senza bisogno di chiamate API.
- Aggiunta nuova schermata "Gestione Token Scam" con l'elenco dei token marcati come SCAM/SPAM, da cui è possibile rimuovere la marcatura o eliminare in blocco i movimenti associati.
- Aggiunta la classificazione dei movimenti relativi a prestiti DeFi (ricezione fondi, messa/sblocco collaterale, liquidazione forzata) e gestione automatica dei token di debito AAVE.
- La finestra principale ora ricorda dimensione e stato massimizzato tra una sessione e l'altra, invece di riaprirsi sempre con le dimensioni predefinite.

**Correzione di bug**

- Corretto bug che nel caso si cercasse di classificare più token come scam in contemporanea il programma a volte proponeva il rigo errato
- Corretto bug che impediva al programma in alcuni casi di fare il controllo delle giacenze CRO a fine scaricamento dati dalla cronoschain
- Corretto bug nello scarico dei dati da cointracking, il programma teneva per buono il prezzo del csv anche se questo era a zero
- Corretto bug per cui interrompere uno scaricamento poteva erroneamente interrompere anche un altro scaricamento aperto in sequenza.
- Corretto bug per cui interrompere uno scaricamento di movimenti DeFi durante il recupero prezzi poteva lasciare in tabella movimenti parzialmente importati, invece di scartare l'intera sessione.
- Corretto bug nello scaricamento delle transazioni dalla rete Cronos che poteva causare la perdita silenziosa di transazioni storiche; ora eventuali errori del server vengono segnalati con un messaggio chiaro.
- Corretto bug per cui, modificando un movimento, se il nuovo ID calcolato coincideva per caso con quello di un altro movimento esistente il salvataggio si bloccava con l'errore "Movimento con Stesso ID"; ora viene generato automaticamente un ID alternativo.
- Corretto il movimento sintetico WCRO->CRO generato durante la sistemazione delle giacenze CRO a fine importazione, che poteva risultare incompleto.
- Migliorata la cache dei prezzi irrecuperabili in modo da evitare di ripetere inutilmente le stesse richieste API in futuro.

## Versione 1.0.57 {#versione-1057}

**Nuove implementazioni**

- Aggiunto supporto alla Blockchain Bitcoin con importazione delle transazioni BTC. Per la gestione dei token BRC-20 e Runes è necessaria l'ApiKey gratuita di UniSat da inserire in "Opzioni" - "ApiKey".
- Aggiunta ricerca dei prezzi da coinmarketcap e rimosso cryptocompare perchè diventato a pagamento.
- Aggiunto sotto "Opzioni" - "Pulizie" una nuova tab "Pulizia PrezziKO" per eliminare i record dei prezzi non recuperabili in un periodo selezionato. (Serve per pulire l'elenco dei token con prezzi non recuperabili per poter poi ritentare lo scaricamento dei prezzi adesso che ho aggiunto alle fonti anche coinmarketcap)
- Migliorato il motore di importazioni CSV generiche: aggiunto supporto a mappaAutoDetect nei file JSON di configurazione per il rilevamento automatico delle colonne dalle intestazioni del CSV; migliorata la gestione del fuso orario con rilevamento dal nome del file e richiesta interattiva se non specificato.
- Aggiornati i file di configurazione JSON delle importazioni: aggiunto supporto al vecchio layout di Cointracking, aggiunto supporto alla nuova versione di Tatax, aggiornato il file di configurazione per Binance CSV.
- Impostato all'avvio del programma lo scaricamento automatico dei file di configurazione per i CSV degli exchange in modo tale da riuscire a gestire le modifiche sui files (che regolarmente fanno gli exchange) senza constringervi a scaricare una nuova versione del programma.
- In "Opzioni" - "E-MoneyToken" inserito la possibilità di precompilare la tabella secondo certe regole

## Versione 1.0.56 {#versione-1056}

**Nuove implementazioni**

- Aggiunto possibilità di parametrizzare le importazioni dei csv tramite file di configurazione JSON per permettere di importare dati da quasi tutti i csv Leggere la documentazione per capire come operare.
- Aggiunto nuove causali per le importazioni da crypto.com
- Modificato buona parte dei messaggi pop-up informativi per renderli più visibili e chiari.

**Correzione di bug**

- Corretto bug sull'export della tabella del quadro RW in xlxs tramite tasto destro del mouse

## Versione 1.0.55 {#versione-1055}

**Nuove implementazioni**

- Aggiornato programma per funzionare con le nuove limitazioni di etherscan che partono dal 01/06/2026
- Sostituito font di sistema con font identico su tutti i sistemi operativi per evitare problemi di visualizzazione su Mac e Linux
- Sostituito animazione iniziale con una più semplice per evitare problemi di flickering su Linux.

**Correzione di bug**

- Corretto bug su una movimentazione di acquisto crypto su crypto.com app
- Corretto bug importazioni da cointracking, nella versione precedente alcuni righi non venivano importati e non veniva data nessuna segnalazione

## Versione 1.0.54 {#versione-1054}

**Nuove implementazioni**

- Implementato importazione anche per il formato dei nuovi file scaricabili da binance
- Inserito nuove causali per le importazione da Crypto.com APP

**Correzione di bug**

- Corretto una tipologia di importazione presente su Crypto.com App che veniva classificata come deposito crypto invece che come acquisto crypto obbligando ad una successiva classificazione manuale.

## Versione 1.0.53 {#versione-1053}

**Nuove implementazioni**

**Correzione di bug**

- Corretto Bug che impediva di salvare le transazioni scaricate attraverso le API di Binance.

## Versione 1.0.52 {#versione-1052}

**Nuove implementazioni**

- Aggiornato i moduli dei quadri T/RT W/RW per il 2026 e modificato le istruzioni presenti sul PDF
- Aggiunto pulsante sulla funzione "Giacenze a data" per confermare i valori a zero per i prodotti segnalati come "senza prezzo".
- Modificato sotto scocca tutte le importazioni affinchè siano più semplici le implementazioni in futuro.

**Correzione di bug**

- Corretto Bug per cui in alcuni casi non funzionava sulla sezione del quadro RW la modifica dei prezzi di inizio anno.
- Corretto Bug che in alcuni casi impediva il corretto caricamento della tabella in "Giacenze a data" qualora si utilizzassero ordinamenti random.

## Versione 1.0.51 {#versione-1051}

**Nuove implementazioni**

- Nell'inserimento manuale inserito nuovo pulsante per l'assenganzione automatica del prezzo e uno per l'inserimento manuale veloce.
- Aggiornato importazione da crypto.com exchange per adeguarsi al nuovo formato del 2026.

**Correzione di bug**

- Sistemato bug sull'assegnazione del prezzo nel caso di molti movimenti nello stesso secondo sulla stessa moneta.

## Versione 1.0.50 {#versione-1050}

**Nuove implementazioni**

- Implementato l'importazione diretta dai CSV di Crypto.com Exchange

**Correzione di bug**

- Adeguato importazione Tatax al suo nuovo standard di file
- Corretto errore bloccante su talune importazioni dal file csv di Binance

## Versione 1.0.49 {#versione-1049}

**Nuove implementazioni**

- Sotto "Opzioni" -> "Opzioni di Calcolo" aggiunto possibilità di disabilitare il calcolo automatico delle plusvalenze, questo velocizza le operazioni sui dati in presenza di molti movimenti.
- Sotto "Opzioni" -> "Opzioni Rewards" aggiunto la possibilità di considerare i cashback crypto alla stregua di quelli FIAT e tra l'altro si potrà scegliere da che anno cominciare a considerarli in questo modo. (Il costo di carico sarà quello relativo al momento del ricevimento e non verrà generata nessuna plusvalenza) Prima di questa modifica i cashback potevano essere solo caricati a costo zero o caricati alla stregua di una reward da staking.

**Correzione di bug**

- Corretto Bug che rallentava il caricamento della tabella in presenza di filtri attivi
- Corretto Bug che impediva l'importazione di alcuni file da Tatax

## Versione 1.0.48 {#versione-1048}

**Nuove implementazioni**

- Etherscan ha messo lo scaricamento di alcune chain a pagamamento (es.BSC,BASE,AVAX) e deprecato altre (es. Cronos), per ovviare al problema sono state implementate nuove API. Per BASE, BSC, AVA  verranno utilizzate le API di Moralis (dalle prove che ho fatto sono leggermente meno precise di quelle etherscan nel senso che ogni tanto potrebbero perdere qualche transazione, circa 1 ogni 5000) Per la cronoschain invece verranno utilizzate le api proprie simili a quelle di etherscan con l'unico neo che il wallet bisogna scaricarlo ogni volta per la sua interezza per cui porterà via un pò di tempo.
- Implementata nuova funzione per la gestione dei prezzi che comporta quanto segue :
- I prezzi vengono ora scaricati da 6 exchange contemporaneamente con la precisione al minuto.
- Nei dettagli delle transazioni ci sarà scritta la fonte del prezzo e la sua precisione.
- Premendo per la modifica del prezzo ora verrà mostrata una maschera dove si potrà scegliere tra vari prezzi nonchè inserirne uno personalizzato.
- I prezzi presi da coingecko rimangono con precisione oraria perchè le api gratuite permettono solo quel tipo di richiesta.
- Lo scaricamento dei prezzi darà più lento ma molto più preciso.
- Il primo scarico in assoluto potrebbe durare parecchi minuti perchè viene scaricato l'ambiente per JavaScript (Indispensabile per utilizzare la nuova libreria CCXT).
- Su alcune tabelle ora è possibile la selezione della cella per dei copia/incolla più mirati, i campi vengono inoltre ripuliti dei tag html.
- Migliorate le prestazioni sul caricamento della tabella principale.

## Versione 1.0.47 {#versione-1047}

**Correzione di bug**

- Eherscan ha deprecato le Api V1, il programma usava ancora quelle Api per verificare se la chiave era valida. Questo causava l'impossibilità di scaricare nuove transazioni dalle chain, sistemata la problematica.

## Versione 1.0.46 {#versione-1046}

**Nuove implementazioni**

- Aggiunto possibilità di scegliere che le commissioni non generenino plusvalenze per attivare andare su "Opzioni" - "Opzioni di Calcolo" e biffare l'opzione "Le commissioni non generano plusvalenza"
- Nella classificazione dei prelievi aggiunto possibilità di classificare un movimento in uscita come "Burn", utile per gli NFT. (Questo movimento non genererà plusvalenze)
- Nella funzione "Classificazione Depositi/Prelievi" ora è possibile classificare più movimenti utilizzando la selezione multipla. (Per farlo selezionare più righi e quindi classifica movimenti, attenzione che i movimenti selezionati devono essere dello stesso tipo, es. solo depositi o solo prelievi)
- Aggiunto il pulsante "Exchange API" nella pagina principale dove si potranno inserire gli api in sola lettura degli exchange crypto (per ora solo Binance) (dalla stessa funzione è poi possibile scaricare i movimenti) Per Binance è consigliabile usare le Api Tasse NB. E' preferibile non scaricare più di 6 mesi di storico per risultati più attendibili visto che molti exchange hanno questo limite NB 2. Su binance non è gestito per ora lo staking di SOL ed ETH.
- Aggiunto file di log per la memorizzazione degli errori e delle operazioni fatte (il file si trova nella root del programma)

**Correzione di bug**

- Corretto problema che in alcuni casi impediva il corretto funzionamento dei filtri su colonna nella tabella principale.

## Versione 1.0.45 {#versione-1045}

**Nuove implementazioni**

- Aggiunto un nuovo filtro nella tabella principale che trova i movimenti a cui manca una parte delle stack del LiFo (tipicamente mancano acquisti per coprire la transazione)
- Aggiunto icone di Alert sui movimenti con errori da correggere, posizionando il mouse sull'icona compare la spiegazione
- Aggiunto al pulsante degli Errori anche i casi in cui manca parte del LiFo, mancando parte dello stack infatti verrebbe calcolata una plusvalenza più alta se non vengono corretti.
- Aggiunto Funzione "Verifica Saldi Negativi" sotto "Analisi Crypto", in questa funzione vengono elencati tutti i momenti in cui c'è stato un saldo negativo su un qualsiasi wallet. Attenzione : Questo non significa che tutti i movimenti segnalati abbiano portato ad un errore nello Stack del LiFo che ricordo non è legato al dettaglio di un singolo Wallet ma alla totalità degli stessi piuttosto che al Gruppo (dipende dalle impostazioni scelte), è opportuno lo stesso effettuare tutte le correzioni per avere la plusvalenza il più corretta possibile.
- Aggiunto messaggio che, qualora si cercasse di stampare il quadro RT relativo ad anni precedenti il 2023, ricorda che in quegli anni c'erano regole diverse e che la stampa del quadro potrebbe non essere corretta (il quadro era diverso e anche le regole, ad esempio i proventi d Staking andavano teoricamente in RL).

**Correzione di bug**

- Corretto funzione per la rettifica giacenza che in taluni casi poteva portare ad un errore nella generazione dell'ID del movimento.
- Corretto errore nella funzione "Scambio Crypto Differito", poteva capitare che i movimenti non venissero ordinati correttamente e portare ad errori visualizzabili nella nuova funzione "Verifica Saldi Negativi", per correggere basta togliere l'associazione del movimento e rifarlo, a quel punto l'ordine dei movimenti verrà creato correttamente.
- Corretto bug che poteva portare cali di prestazioni fino al blocco del programma applicando i filtri sulle tabelle in un certo modo
- Corretto bug che portava alla selezione errata del rigo di un paio di tabelle nel caso fossero applicati dei filtri.
- Corretto bug che in taluni casi poteva mostrare uno stack del LiFo vuoto utilizzando la nuova funzione "Mostra LiFo Transazione"

## Versione 1.0.44 {#versione-1044}

**Nuove implementazioni**

- Implementato visualizzazione dello stack del LiFo della transazione tramite tasto destro sulla riga della tabella selezionando "Mostra LiFo Transazione"
- Aggiunto ulteriori causali all'import di Binance
- Cominciato la conversione delle icone a quelle vettoriali per una più alta resa
- Migliorato performance dei filtri sulle tabelle che su molti movimenti (>20.000) potevano in taluni casi risultare molto lenti.
- Aggiunto pacchetti DMG per MacOS e DEB per le distribuzioni basate su Debian/Ubuntu

**Correzione di bug**

- Corretto bug che poteva portare alla visualizzazione dei giorni negativi sugli Exchange per cui si era selezionata l'opzione "paga bollo"
- Corretto bug che non settava sul database al primo avvio del programma l'opzione presente in opzioni - opzioni di calcolo -> "Fino al 31-12-2022 considera tutti gli scambi crypto-crypto fiscalmente rilevanti" In sostanza da programma veniva visualizzata l'opzione spuntata ma sul database non scriveva nulla e quindi l'opzione non veniva considerata. L'opzione veniva poi scritta correttamente nel database la prima volta che ci si agiva fisicamente sopra premendoci con il mouse, da quel punto tutto poi funzionava correttamente.

## Versione 1.0.43 {#versione-1043}

**Nuove implementazioni**

- Implementato i filtri stile Excel su alcune tabelle, tasto destro sull'intestazione della colonna per modificarli.
- Se sulla colonna ci sono numeri sotto il titolo della colonna compare anche la somma di tutti i righi visibili.

**Correzione di bug**

- Corretto errore sulle importazioni da cointracking, se nel file c'erano più righi identici veniva preso solo il primo.
- Corretto particolarità che impediva agli utenti Linux e Mac di aprire il popupMenu' sulle tabelle.

## Versione 1.0.42 {#versione-1042}

**Nuove implementazioni**

- Aggiunto lo splashscreen in avvio del programma perché in alcuni casi poteva metterci anche molti secondi prima di far vedere la maschera principale dando quindi l'impressione di non aver avviato il programma.
- Cambiato colore della selezione della tabella con tema scuro perché quello precedente quasi non si riusciva a leggere.

**Correzione di bug**

- Corretto importazione dei dust conversion di crypto.com app che potevano portare ad errori di giacenza sui CRO.
Se non vi tornano le giacenze dei CRO di fine anno consiglio di rifare l'importazione dei file con questa versione.
L'errore in questione poteva portare o ad errori minimi (di pochi CRO) o ad errori macroscopici (in talune circostanze spostava la virgola e portava al caricamento ad esempio di 1 milione di CRO anziché 1)
- Sistemato importazioni del FIAT Wallet di Crypto.com, prima faceva fede sempre l'ultima importazione (ogni nuova importazione cancellava quelle vecchie), adesso, come succede nelle altre importazioni, si possono accodare le importazioni.

## Versione 1.0.41 {#versione-1041}

**Nuove implementazioni**

- Aggiunto nel PopUpMenu la possibilità di cambiare la tipologia di Reward ricevuta.
- Aggiunto sezione per le Donazioni qualora voleste contribuire al progetto.

**Correzione di bug**

- Corretto importazione EUR su Crypto.com APP CVS (Il nuovo CSV non mette più il segno meno sugli EUR in uscita)
- Sistemato problema che portava ad avere delle giacenze iniziale errate sull'RW nel caso in cui ci fosse stata abilitata un opzione qualsiasi dalla B alla D + Opzione mostra solo giacenze fine anno se paga bollo + terza opzione sui trasferimenti.
- Aggiunto casistiche su importazioni da cointracking.
- Sistemato casistica particolare su importazione del CASH CSV di Crypto.com
- Nelle stampe per il Quado W/RW ora se il valore finale o quello iniziale è molto prossimo allo zero ma non zero questo viene arrotondato per eccesso e valorizzato a 1.

## Versione 1.0.40 {#versione-1040}

**Nuove implementazioni**

- In depositi/prelievi aggiunta nuova tabella che fa vedere i movimenti correlati (utile per vedere ad esempio i trasferimenti dove vanno o gli scambi differiti)
- Al PopUpMenu aggiunto la funzione "Modifica Note" e "Modifica Prezzo"
- Adesso pe piccolissime differenze è possibile classificare un movimento come trasferimento anche se la qta di prelievo sul wallet i parenza è minore di quella di deposito sul wallet i destinazione.
- Nelle importazioni da Cointracking o Tatax ora è possibile scegliere dei nomi personalizzati per i Wallet.

**Correzione di bug**

- Importando i dati da Tatax i token messi in Staking vengono gestiti con un estensione es. ETH.STAKING@BINANCE, con questa nuova versione gli viene ripristinato il nome originale (es. ETH) per evitare problemi poi con i prezzi e con il LiFo.
- Sempre negli import da Tatax i vari EARN, CASHBACK etc.. arrivano con prezzo Zero, ora il programma se vede che arrivano in questo modo gli assegna un valore.
- Nella classificazione "Scambio Crypto differito" se il movimento di deposito non aveva prezzo anche lo scambio generato veniva valorizzato a zero ma senza segnalare la mancanza del prezzo.
Adesso questa cosa non succede più.
- Nelle stampe dell'RW anche per gli anni bisestili ora i giorni di detenzione vengono messi al massimo a 365 anche per gli anni bisestili, questo per uniformarsi al software dell'AdE che non consente di inserire giorni di detenzione maggiori di 365.

## Versione 1.0.39 {#versione-1039}

**Nuove implementazioni**

- Inserito filtro x Wallet e x Token nella tabella delle transazioni e nella tabella deli depositi/prelievi
- Aggiunto avviso in caso di disponibilità di una nuova versione software.
- In fase di import dalla blockchain inserito richiesta per effettuare o meno la rettifica automatica delle giacenze del token di riferimento della chain a fine importazione. (Prima veniva fatto automaticamente).
- Nella gestione della classificazione dei depositi prelievi inserito possibilità di modificare i filtri automatici sui trasferimenti.

**Correzione di bug**

- Corretto gestione delle FIAT diverse dall'euro in fase di importazione dal Binance Tax Report

## Versione 1.0.38 {#versione-1038}

**Nuove implementazioni**

- Dato che attualmente i prezzi, se non presi dal CSV, vengono presi con una precisione oraria, da questa versione per gli scambi fiscalmente rilevanti verrà preso come prioritario il prezzo della stablecoin/FIAT presente nella transazione e mai quella della cripto che potrebbe essere piuttosto volatile.
- Aggiunto Tasto "Ricalcola i prezzi delle transazioni" nella sezione "Opzioni - Varie" per permettere di ricalcolare i prezzi delle transazioni secondo le nuove regole. Prima del ricalcolo verrà mostrata una finestra in cui si potrà scegliere da che anno effettuare il ricalco. Per sicurezza verrà anche salvata una copia delle movimentazioni crypto nella cartella /Backup che rimarrà per 6 mesi. Ad ogni modo se non si salva, la modifica dei prezzi non è permanente, basta premere annulla nella schermata principale per tornare alla situazione iniziale.

**Correzione di bug**

- Corretto diversi problemini di visualizzazione / errori bloccanti in casi molto particolari
- Corretto Bug che, in fase di importazione dal Binace Tax Report, impediva di valorizzare i prezzi dei prelievi.

## Versione 1.0.37 {#versione-1037}

**Nuove implementazioni**

- Aggiunto popUpMenu su quasi tutte le tabelle del programma e su alcuni campi testo (si attiva con il tasto destro del mouse). Dal PopUp menù si potrà :
- Copiare la selezione
- Incollare gli appunti
- Vedere i dettagli della transazione selezionata (comodo ad esempio sulle tabelle dove non vi sono tutti i dettagli).
- Esportare la tabella in Excel (per permettere ulteriori elaborazioni e calcoli in autonomia).
- Aggiunto 2 nuove causali utilizzate dal csv di Binance
- Aggiunto la possibilità di classificare come Scam più token alla volta dalla funzione Depositi/Prelievi.

**Correzione di bug**

- Corretto Bug che, qualora vi fossero anni senza movimentazioni, non permetteva il cambio del prezzo sul dettaglio movimenti del quadro RT, inoltre faceva vedere a video il riferimento del prezzo errato.
- Corretto Bug che non permetteva di associare un Wallet che all'interno del nome aveva l'apostrofo ad un Gruppo.

## Versione 1.0.36 {#versione-1036}

**Nuove implementazioni**

- Aggiunto pulsante "Modifica Movimento" in funzione "Classifica Depositi/Prelievi"
- Aggiunto pulsante "Duplica Movimento" in funzione "Classifica Depositi/Prelievi"

**Correzione di bug**

- Corretto bug che impediva di creare un movimento opposto su un wallet in Defi dalla funzione "Crea movimento opposto" in "Classifica Depositi/Prelievi"
- Coretto bug per cui il prezzo di WETH poteva risultare errato
- Corretto bug nella sezione RW che in caso risultasse selezionata l'opzione "il LiFo viene applicato alla totalità dei Wallet" nella sezione "Opzioni RW" poteva in taluni casi particolari portare ad un errore.

## Versione 1.0.35 {#versione-1035}

**Nuove implementazioni**

- Aggiunto importazioni di Binance dal Financial Report NB : Importare o il Financial Report o il csv classico, mai entrambi per lo stesso periodo. Il Financial Report dovrebbe essere un po' più preciso soprattutto per quanto riguarda i prezzi.
- Formattati alcuni campi numerici per rendere più leggibile la cifra.

**Correzione di bug**

- Corretto nome intestazione colonne nella sezione "RT & Analisi P&L", i nomi della colonna 2 e 3 erano invertiti.
- Corretto bug che poteva portare al blocco del programma nel caso di doppia rettifica giacenza sulla stessa moneta.
- Corretto Bug che poteva portare a salvare prezzi errati nella sezione giacenze a data qualora vi fossero stati inseriti prezzi personalizzati.
- Nel pacchetto 1.0.34 avevo dimenticato di inserire le immagini del quadro T/RT e il file dei token gestiti da coincap. Questo portava all'impossibilità di recuperare alcuni prezzi nonchè l'impossibilità di stampare il quadro T/RT correttamente.
- Sistemato Bug che mandava in blocco il programma nel caso in cui si lanciasse l'eleaborazione del quaro T/RT senza dati.

## Versione 1.0.34 {#versione-1034}

**Nuove implementazioni**

- Aggiunto Api di Coinbase e Cryptocompare per il recupero dei prezzi
- Aggiunto possibilità di inserire le ApiKey di Coincap (Opzioni - ApiKey)
- Aggiunto possibilità di inserire le ApiKey di Coingecko per incrementare la velocità di acquisizione sui prezzi della DeFi (Opzioni - ApiKey) (Ad oggi quindi i prezzi delle Cripto vengono recuperati da Binance,Coinbase,Cryptocompare,Coincap(con Apikey dedicata) e Coingecko (Per la defi e solo ultimi 365gg)
- In "RT & Analisi P&L" Aggiunto possibilità di stampare il Quadro T per i redditi 2024 (con le prossime versioni verrà aggiunto anche il quadro RT)

**Correzione di bug**

- Coincap essendo diventato a pagamento non permetteva più lo scaricamento dei prezzi causando errori nel recupero, aggiunto la possibilità di integrare la loro ApiKey.

## Versione 1.0.33 {#versione-1033}

**Nuove implementazioni**

- Aggiunto PMC (Prezzo Medio di Carico) nel dettaglio della funzione "RT & Analisi P&L"

**Correzione di bug**

- Corretto Bug che impediva di inserire prezzi personalizzati su rete Solana
- Corretto Bug che impediva di recuperare i prezzi salvati su rete Solana
- Corretto qualche bug grafico

## Versione 1.0.32 {#versione-1032}

**Nuove implementazioni**

- Aggiunto supporto alla Blockchain Solana (Sono necessarie le ApiKey di Helius da inserire su "Opzioni" - "ApiKey") (Si può creare tranquillamente la chiave api gratuita dal loro sito registrandosi)
- Aggiunto supporto a Berachain e Avalanche
- Da questa versione i movimenti di tutte le chain evm supportate vengo importati attraverso le api di etherscan. Sarà quindi necessario registrarsi su etherscan e generare una ApiKey (gratuita) per poter scaricare i movimenti. L'ApiKey poi andrà inserita in "Opzioni" - "ApiKey".
- Aggiunto anno 2024 nella sezione del Quadro RW, per il momento i moduli sono però ancora quelli dell'anno passato.

## Versione 1.0.31 {#versione-1031}

**Nuove implementazioni**

- Aggiunto ulteriori causali di Binance per le importazioni
- Ad ogni fine importazione dalla chain  aggiunto controllo ed eventuale sistemazione giacenza sul token di riferimento della chain. Questo si rente necessario per i layer 2 in quanto le api non ritornano la parte di commissione relativa al layer 1. Fortunatamente questa parte di commissione è di circa 1 centesimo per ogni transazione quindi i movimenti correttivi saranno di piccola entità.
- Aggiunto BTC alle chain disponibili per importazioni da tatax e cointracking.
- Nelle Pulizie adesso è possibile impostare dei range di date entro quale cancellare i dati.
- Nella funzione "Giacenze a Data" aggiunto segnalazione per i token senza prezzo
- Nella funzione "Calssificazione Depositi/Prelievi" - "Classifica Movimento" ora c'è una nuova colonna con il controvalore in euro della transazione.

**Correzione di bug**

- Corretto erroreche poteva portare ad un blocco delle importazioni di Crypto.com
- Corretto errore che impediva di gestire correttamente i gruppi wallet se sul nome dei wallet erano presenti degli apici

## Versione 1.0.30 {#versione-1030}

**Nuove implementazioni**

- Compilato i pacchetti con la versione Opensource del Runtime Java (in caso di problemi sulla versione multipiattaforma scaricare il jre su questo sito https://adoptium.net/temurin/releases/)
- in "Opzioni" - "Varie" aggiunto bottone "Disclaimer" e "Avvertenze/Problemi Noti" per raggiungere velocemente la documentazione.
- In "Opzioni" - "Opzioni di calcolo" aggiunto la possibilità di non considerare nei calcoli per la plusvalenza i movimenti non classificati (come succedeva prima della versione 1.29)
- In "Modifica Movimento" aggiunto tasto "Sblocca Modifiche" per poter modificare tutti i campi del movimento
- In "Classificazione Depositi/Prelievi" aggiunto la possibilità di classificare i movimenti come "Donazione"
- In "Analisi Crypto" "Classificazione Depositi/Prelievi" aggiunto pulsante "Identifica come SCAM" (già presente nella funzione Giacenze a data)

**Correzione di bug**

- Import da Wallet : Gli Explorer possono riconoscere come transazioni reali log di smartcontract (solitamente sono prelievi di token SCAM), ovviamente essendo transazioni fittizie non sono da considerare le fee. Da questa versione il programma non imputa più quelle fee al wallet per questo tipo di transazioni. Le transazioni resteranno comunque visibili per coerenza con quanto presente nell'explorer, saranno poi eventualmente da escludere da eventuali calcoli classificandole come SCAM.
- Import da Wallet : Se per sbaglio si inviano fondi allo stesso wallet di partenza il programma prima riconosceva solo un movimento di prelievo, ora lo considera come uno scambio a vuoto. Di fatto non c'è nessun movimento di fondi se non le fee.
- Corretto bug nelle importazioni di Binance che portava alla non classificazione di alcuni movimenti.
- Corretto bug nelle importazioni da Tatax che portava ad un errore sui prezzi
- Corretto bug nelle importazioni di Tatax riguardante l'orario, su file csv infatti l'orario viene riportato in UTC, adesso viene convertito nell'orario italiano.
- Corretto bug nelle esportazioni di Tatax, ora l'orario prima di essere esportato viene convertito in UTC

## Versione 1.0.29 {#versione-1029}

**Nuove implementazioni**

- Cambiato come il programma gestisce il calcolo per le plusvalenze dei movimenti di deposito e prelievo non categorizzati.
Con le versioni precedenti il programma semplicemente ignorava quelle transazioni (non veniva generata nessuna plusvalenza fino a che il movimento non veniva categorizzato)
Con la versione attuale il programma si comporta nel seguente modo :
- Deposito non categorizzato = Deposito con Costo di Carico Zero
- Prelievo non categorizzato = CashOut Facendo in questo modo avrò delle plusvalenze più alte fino a che non categorizzerò correttamente i movimenti ma altresì qualora dimenticassi di categorizzarli, questi sarebbero comunque conteggiati nella maniera meno vantaggiosa per il contribuente e quindi meno contestabile dal fisco.
- Cambiato il tipo dato della colonna Plusvalenza e Valore Transazione ora sono di tipo numerico quindi ordinabili correttamente
- In Opzioni - Varie aggiunto la possibilità di abilitare il tema scuro.
- Implementata nuova funzione in "Analisi Crypto" - "RT & Analisi P&L" dove si potrà vedere l'analisi delle plusvalenze realizzate e non per ogni singolo anno, inoltre per ogni anno si potranno vedere le plusvalenze per ogni token e nel caso sia abilita l'opzione "Abilita Calcolo plusvalenze per gruppo Wallet" anche divise per gruppo wallet.
Per ogni token poi ci sarà la possibilità di vedere lo stack del LiFo con i vari costi di carico in modo da poter pianificare al meglio la realizzazione delle plusvalenze future.
- Inserito nuove casistiche riguardanti le importazioni da Binance
- Aggiunta Blockchain BASE alla lista di quelle gestite nativamente. (Funzione Inserisci Wallet)
- Aggiunta Blockchain ABITRUM alla lista di quelle gestite nativamente. (Funzione Inserisci Wallet)

**Correzione di bug**

- Corretto bug che impediva in taluni casi il recupero del prezzo
- Corretto bug che portava ad errori sugli arrotondamenti sulle plusvalenze nel caso di scambi tra diversi wallet e qualora non fosse biffata l'opzione "Abilita Calcolo plusvalenze per gruppo wallet"
- Sistemato un bug i un caso particolarissimo che impediva il caricamento dei dati da CDCApp
- Corretto possibile mancata imputazione del prezzo dalla funzione automatica della modifica transazione
- Corretto errore bloccante in importazione da Tatax

## Versione 1.0.28 {#versione-1028}

**Nuove implementazioni**

- Quadro RW : Introdotto un ulteriore opzione sul metodo di calcolo per l'utilizzo del LiFo anche sui SubMovimenti (Vedi Documentazione).
- Aggiunto Icone ai pulsanti per un più facile ed intuitivo utilizzo.
- Aggiunto possibilità di importare i dati da Tatax.
- In "Transazione Crypto" aggiunto checkbox "Vedi solo movimenti non valorizzati" che mostrerà solo i movimenti in cui il programma non è riuscito a recuperare i prezzi. In questo modo tramite il tasto "Modifica Movimento" sarà facile individuarli e andare ad assegnare un prezzo alla transazione.

## Versione 1.0.27 {#versione-1027}

**Nuove implementazioni**

- Quadro RW/W : Adesso se si sceglie l'opzione di considerare solo le giacenze di inizio e fine anno il programma non mette di default 365 giorni di detenzione ma qualora il conto/wallet fosse stato aperto nel periodo di riferimento farà il calcolo a partire dal primo movimento del wallet alla fine dell'anno.
- Quadro RW/W : Inserito tasto per la stampa di un report più dettagliato dei calcoli in formato Excel
- Quadro RW/W : Nella stampa del Report in PDF aggiunto il quadro RW e numerose istruzioni
- Riorganizzato la tabella delle opzioni sul quadro W/RW in modo che sia più chiara

**Correzione di bug**

- Quadro RW/W : le giacenze negative venivano segnalate ma il programma le mostrava senza il segno meno

## Versione 1.0.26 {#versione-1026}

**Nuove implementazioni**

- Inserito nuova sezione in "Opzioni - Opzioni Rewards" dove è possibile decidere come trattare ogni tipologia di reward
- Implementato Api di CoinCap al posto di Cryptohistory per l'assegnazione dei prezzi poichè quest'ultima non fornisce più il servizio
- Inserito possibilità di stampare una prima bozza del quadro W
- Inserito possibilità di calcolare i valore per il quadro W anche per gli anni precedenti al 2023 (Attenzione, vengono usate le stesse regole del 2023, potrebbe essere sbagliato)

**Correzione di bug**

- Sistemato bug che impediva di correggere il valore iniziale e finale di un token nella sezione RW
- Sistemato bug che impediva di calcolare i valori con la media ponderata nel caso di classificazioni particolari

## Versione 1.0.25 {#versione-1025}

**Nuove implementazioni**

- Una volta selezionata la data dalla Funzione giacenze a data questa viene memorizzata per i successivi accessi
- Una volta selezionato un file da importare la path viene salvata per le successive importazioni per evitare di dover riselezionare l'intero percorso
- Aggiunto tipologia "finance.lockup.dpos_compound_interest.crypto_wallet" a quelle supportate dalle importazioni da CDC APP
- Aggiunto 3 nuove tipologie per le importazioni da Binance

**Correzione di bug**

- Corretto errore nelle importazioni dei wallet in DeFi per cui non venivano conteggiate le fee per le operazioni di deposito da Piattaforme DeFi
- Corretto tasto Dettaglio DeFi non funzionante su rete Ethereum

## Versione 1.0.24 {#versione-1024}

**Nuove implementazioni**

- In "Opzioni" - "Gruppi Wallet Crypto" inserita possibilità di aggiungere un Alias e indicare se è già stato pagato il bollo
- Aggiunto le seguenti nuove opzioni di calcolo per i dati relativi al quadro W\RW su "Opzioni" - "Crypto" : 1 - Possibilità di vedere semplicemente le giacenze di inizio e fine anno 2 - Possibilità di scegliere la condizione per cui viene creato un nuovo quadro RW 3 - Per i Wallet in cui è già stato pagato il bollo aggiunta possibilità di vedere le sole giacenze di inizio e fine anno 4 - Dato possibilità di gestire diversamente le Reward 5 - Aggiunto ulteriore opzione per la gestione dei trasferimenti tra Wallet
- Nella sezione "Analisi Crypto" - "W/RW" aggiunto le seguenti voci 1 - Totale IC Dovuto 2 - Dettaglio IC per singolo rigo RW 3 - Bottone con link a documentazione su come vengono calcolati i Qadri e spiegazioni delle varie opzioni disponibili
- Nella schermata "Transazioni Crypto" aggiunto in fondo ulteriori dettagli sul calcolo della Plusvalenza Totale

**Correzione di bug**

- Correzione di vari bug che potevano portare a blocchi in determinate condizioni
- Sistemato un errore di calcolo sul prezzo automatico nella funzione di inserimento manuale del movimento nel momento in cui si vende per Euro.

## Versione 1.0.23 {#versione-1023}

**Nuove implementazioni**

- Aggiunto nuove opzioni di calcolo per i dati relativi al quadro W\RW su "Opzioni" - "Crypto"

**Correzione di bug**

- Corretto problemi su modifiche movimenti automatici
- Aggiunto gestione sui token ibridi delle stable legate al dollaro sugli echange chiamate USD (il loro valore lo lego a quello di giornata del dollaro)

## Versione 1.0.22 {#versione-1022}

**Correzione di bug**

- Corretto errore che impediva la creazione di movimenti manualmente

## Versione 1.0.21 {#versione-1021}

**Nuove implementazioni**

- Aggiunto log dettagliato durante le importazioni

**Correzione di bug**

- Corretto errore per importazioni risaltenti a prima del 2017

## Versione 1.0.20 {#versione-1020}

Nota Importante : Il programma al primo avvio dovrà fare un adeguamento degli archivi che potrebbe portare via qualche minuto
**Nuove implementazioni**

- Nella Tab "Analisi Crypto" aggiunta sezione reletiva al calcolo del quadro RW secondo il metodo LIFO con media ponderata. Nota 1 - Il programma è in una prima fase beta e potrebbe contenere errori, usare per test ma non consiglio di usarla per la dichiarazione Nota 2 - Per vedere il calcolo dei giorni è opportuno correggere tutti gli errori che presenta
- Nella Tab "Opzioni - Crypto" aggiunto le seguenti opzioni 1 - Possibilità di calcolare il quadro rw facendo un nuovo rigo per ogni operazione (attualmente il nuovo rigo viene fatto solo se avviene un operazione fiscalmente rilevante) 2 - Aggiunto la possibilità di ritenere fiscalmente rilevanti per i movimenti fino al 31-12-2022 tutti gli scambi (questo ovviamente influisce sui costi di carico del 2023 e di conseguenza sulle plusvalenze) 3 - aggiunto la possibilità di considerare tutte le reward fino al 31-12-2022 come provento a costo di carico Zero
- In "Analisi Crypto - Giacenze a Data" inserito la possibiltà di non far vedere i token scam
- In Opzioni - Export ora è possibile generare un file di export con i vostri dati per Tatax Nota 1 - Non verranno esportati i token contrassegnati come scam Nota 2 - Verranno generati 2 file di cui uno con la scritta defi alla fine. Quello con la scritta defi contiene le movimentazioni generate in automatico dal programma quando gli dite che un determinato movimento è un versamento/prelievo su un vault in defi.
- Implementata importazione da OKX (E' importante caricare sia il Csv del Funding Wallet che quello del Trading altrimenti vi ritroverete con dei dati incompleti)

**Correzione di bug**

- Sistemato problemi in importazioni sul csv del "Crypto Wallet" e  di Binance.
- Sistemato problemi vari che potevano portare a freeze del programma in talune circostanze.

## Versione 1.0.19 {#versione-1019}

**Correzione di bug**

- Aggiunto ulteriori voci che prima venivano scartare nell'importazione del Fiat e del Crypto wallet di Crypto.com
- Tolto dalle importazioni di Crypto.com tutti i movimenti di blocco e sblocco dei token per il limit order in quanto generavano un sacco di righe inutili.

## Versione 1.0.18 {#versione-1018}

**Nuove implementazioni**

- Aggiunto ulteriore provider per la ricerca dei prezzi poichè coingeko ha limitato le richieste api gratuite agli ultimi 365 giorni
- In Opzioni - "Gruppi Wallet Crypto" è possibile scegliere di calcolare la plusvalenza divisa per gruppi di wallet
- In inserimento manuale ora se si seleziona una riga verranno riportate automaticamente Data, ora e Wallet per velocizzare gli inserimenti.
- Sui movimenti inseriti manualmente è ora possibile modificare tutti i campi

**Correzione di bug**

- Sistemato problemi in importazioni sul csv del "Crypto Wallet" e del "Fiat Wallet" di Crypto.com
- Sistemato blocchi in fase di importazione dalla defi (dovuti al cambio di politiche del piano gratuito di coingecko)

## Versione 1.0.17 {#versione-1017}

**Nuove implementazioni**

- Modificato visualizzazione dettagli riga
- In "Opzioni - Gruppi Wallet Crypto" aggiunto la possibilità di raggruppare i wallet a piacere (Questa funzione è una predisposizione per future implentazioni ma attualmente è fine a se stessa)
- In "Opzioni - E-Money Token" aggiunto la possibilità di indicare quali monete e da quando sono E-Money Token. Questo inciderà sul calcolo delle plusvalenze,che verranno immediatamente ricalcolate, in quanto lo scambio crypto-EMT è fiscalmente rilevante.

## Versione 1.0.16 {#versione-1016}

**Nuove implementazioni**

- Aggiunto import da rete Ethereum
- Aggiunto tasto per l'esportazione in csv dei movimenti crypto.

**Correzione di bug**

- Sistemato problema per cui quando si aggiungenva un nuovo wallet CRO alla prima importazione non veniva lanciato il controllo sulle giacenze.

## Versione 1.0.15 {#versione-1015}

**Nuove implementazioni**

- Aggiunto correzione automatica giacenze CRO sulle importazioni da cronos chain (Alcuni movimenti di CRO es. alcuni scambi da CRONOS-POS a Cronos.org o alcune fees gestite da smart contract non vengono visti dall'explorer, questo provocava errori sulle giacenze di CRO, adesso ad ogni fine importazioni vengono controllati tutti i movimenti e per ogni blocco viene richiesta la giacenza dei CRO del Wallet, poi viene in automatico creato un movimento correttivo per portare la giacenza di CRO alla quantità esatta) Per correggere i movimenti pregressi già importati basta rilanciare l'importazione dei wallet e verrà tutto gestito in automatico, l'unica pecca è che porterà via più tempo di prima.
- Nella funzione "Classificazione Trasferimenti Crypto" aggiunto possibilità di classificare i movimenti come "Trasferimenti da e per Vault/Piattaforma di rendita" per identificare i trasferimenti verso piattaforme di rendita es. PancakeSwap,VVS,Beefy etc... Tra le altre cose nel momento in cui viene classificato il movimento in questo modo il programma identifica lo smart contract e chiede se identificare allo stesso modo tutti gli altri movimenti che interagiscono con lo stesso smartcontract, questo dovrebbe velocizzare di molto le classificazioni sui trasferimenti.
- Nella funzione "Classificazione Trasferimenti Crypto" il pulsante assegnazione automatica ora riconosce alcuni movimenti automaticamente e li classifica in maniera corretta es. riconosce in automatico gli scambi tra WCRO e CRO e li classifica.

**Correzione di bug**

- Tolto pulsante che compariva nella prima pagina e che non aveva alcuna funzione
- Sulla sezione card wallet e fiat wallet corretto sezione rimanenze nella tabella dettagli movimento (prima,solo nella sezione dettaglio movimenti, non veniva considerata la giacenza di inizio anno e le rimanenze partivano da zero)

## Versione 1.0.14 {#versione-1014}

**Nuove implementazioni**

- Aggiunto importazione file CSV Binance Attenzione : 1 - Dai file CSV di Binance non vengono estratti gli spostamenti interni (Es. quando passate i token da SPOT a EARN) in quanto non rilevanti e perchè Binance ha cambiato diverse volte nel tempo il formato del file CSV per questi movimenti 2 - Potrebbero esserci piccole o grandi imprecisioni sui file CSV di Binance, ho riscontrato le seguenti problematiche: A - A volte sul CSV mancano alcuni movimenti di Earn sui BNB (me ne mancavano 8 in un anno anche se poi riestraendo il CSV qualche mese dopo sono ricomparsi) B - Binance ad un certo punto non mette più nel file CSV i movimenti sulle coppie che ha tolto dal suo listino (non conosco il timing però) Questo significa che potrebbero mancarvi dei trade su coppie che non esistono più.
- Nuove implementazioni della funzione "Giacenze a Data" 1 - Migliorata la grafica sulle tabelle 2 - si può identificare un token come scam o cambiare il suo nome se arriva dalla DEFI 3 - Si può modificare il prezzo di un token ad una certa data 4 - Per i wallet in defi si può vedere la situazione attuale delle giacenze di tutti i token detenuti attraverso il relativo explorer 5 - Si possono creare dei movimenti correttivi (es. mi accorgo che di un certo token a fine anno ho una giacenza errata posso inserire dei movimenti correttivi) (Questo succede spesso con scheetcoins con tokenomics strane (es. tasse o premi su blockchain ricevibili al momento di una movimentazione))
- Nella funzione "Classificazione Trasferimenti Crypto" aggiunto la classificazione "Scambio Differito" dove verranno associati un movimento di prelievo e uno di deposito anche differiti nel tempo (Viene usata ad esempio per classificare gli scambi attraverso bridge, scambio attraverso piattaforme con del timing o cmq scambi che richiedono del tempo per essere completati)

**Correzione di bug**

- Corretto bug in importazioni che cancellava le associazioni fatte sulla classificazione movimenti
- Corretto bug dovuto ad arrotondamenti che poteva portare a costi di carico negativi (anche se di importi molto piccoli)
- Corretto bug su importazioni da cointracking su file in lingua Italiana (Prima venivano gestiti solo quelli in inglese)

## Versione 1.0.13 {#versione-1013}

**Nuove implementazioni**

- Sotto "Analisi Crypto" inserita nuova funzione chiamata "Giacenze a data" che permette di calcolare le giacenze delle crypto con relativo valore ad una specifica data.

**Correzione di bug**

- Corretti diversi bug nelle importazioni da blockchain (reimportare i wallet per applicare le correzioni sul pregresso)

## Versione 1.0.12 {#versione-1012}

**Nuove implementazioni**

- Aggiunto importazione di wallet da rete cronoschain (no ERC1155)
- Aggiunto possibilità di classificare un movimento di deposito come acquisto, verrà chiesto l'importo pagato in euro. (Utile per acquisti di crypto da chain o da amici in contanti o altro)

**Correzione di bug**

- Corretto bug su importazione di wallet da chain con più di 10000 movimenti (Qualora vi fossero sarà da rifare l'importazione per sistemare i movimenti)
- Altri vari piccoli BugFix

## Versione 1.0.11 {#versione-1011}

- Corretto errata visualizzazione costi di carico sui depositi e prelievi
- Corretto calcolo plusvalenza in caso di rimborsi
- Altri piccoli bugfix

## Versione 1.0.10 {#versione-1010}

- Sistemato calcolo delle plusvalenze sugli NFT
- Implementato importazione dati su acquisti di beni/servizi con crypto da CSV Crypto.com
- Implementato importazioni acquisti ricorrenti e scambi crypto tra app Crypto.com da file CSV
- Ora il bottone dettaglio DEFI si illumina e può essere premuto solamente nel caso in cui vi siano i dati

## Versione 1.0.9 {#versione-109}

**Nuove implementazioni**

- Implementato importazioni dati dal file csv di Crypto.com che comprendono anche la nuova modalità di Staking
- Implementato importazione e gestione degli NFT ERC721 su rete BSC
- Implementato possibilità di aggiungere, modificare ed eliminare movimenti manualmente

**Correzione di bug**

- Corretto importazione dati nel caso in cui la data nel CSV di Crypto.com sia quella attuale
- Corretto errata assegnazione di valori null al posto di blanc durante le importazioni dal SCV di Crypto.com
- Corretto errore di importazione in rari casi nel caso di dustConversion dal file CSV di Crypto.com
- Corretto problema sul bottone interrompi in fase di importazione dati da blockchain

## Versione 1.0.8 {#versione-108}

Corretto errore che impediva l'importazione del csv crypto.com app nel caso di acquisti crypto con carta
Implementato importazione diretta da blockchain su rete BSC inserendo il numero di wallet
(ancora non supportato importazione di scambi con NFT, a breve verrà implementata la parte relativa agli NFT ERC721)
(per i token ERC1155 bscscan non ha ancora implementato correttamente le API , non appena lo farà saranno implementati anche quelli)
Problema noto : Il bottone interrompi in fase di importazione dati potrebbe non funzionare (verrà corretto nella prossimaversione)

Note: Per colpa delle limitazioni sulle chiamate delle Api gratuite la prima importazione del wallet potrebbe impegnare molto tempo

## Versione 1.0.7 {#versione-107}

Implementato importazione dati delle cripto da Crypto.com App
Implementato importazione dati delle cripto da Cointracking.info
Implementato classificazione dei movimenti di trasferimento tra wallet
Implementato calcolo della plusvalenza con il metodo Lifo
NOTE:
- La plusvalenza viene generata solo nei seguenti casi (gli scambi Crypto-Crypto non generano plusvalenza) 1 - Cashout 2 - Cashback, reward e similari 3 - Commisioni (se esplicitate in fase di importazione)
- Per un corretto calcolo è importante inserire tutti i movimenti da tutti i wallet ed exchange dall'inizio della detenzione
- NFT e derivati non sono per ora implementati.

## Versione 1.0.6 {#versione-106}

Corretto problema per cui si analizzava un periodo senza movimenti il saldo iniziale veniva messo a zero invece che calcolarlo dai movimenti precedenti.
Tolto filtri per importazioni dati card wallet, ora importa anche file con più campi di quelli previsti, attenzione a caricare il file corretto.
Questo filtro è stato tolto perchè in taluni casi il file del card wallet presentava dei dati aggiuntivi e quindi non veniva importato.

## Versione 1.0.5 {#versione-105}

Migliorato layout di stampa

## Versione 1.0.4 {#versione-104}

Sistemato problema di calcolo sulla diffeenza delle date, in alcuni casi particolare veniva arrotondato per difetto e poteva influire di 1 giorno sul calcolo della giacenza media
Sistemato problema sul cardwalle in cui non veniva mai considerata la rimanenza iniziale se imputata a mano
Aggiunto nelle tabelle e nel pdf le rimanenza data per data
Aggiunto logo

## Versione 1.0.3 {#versione-103}

Sistemato problema di calcolo sui bonifici verso la banca fatti dal FiatWallet (prima venivano erroneamente sommati invece che sottratti)
Aggiunto pulsante per la generazione di un report in pdf sia sul Fiat Wallet che sul Card Wallet

## Versione 1.0.2 {#versione-102}

Sistemato problema con gestione dei movimenti anomali

## Versione 1.0.1 {#versione-101}

Modificato importazione movimenti, prima non venivano importati i movimenti eseguiti nel medesimo secondo benchè diversi
Ora vengono scartati solo i movimenti con stessa ora (al secondo),causale e importo.
+
Ora si possono importare più csv dello stesso tipo es. se ho 2 csv del fiat wallet, 1 del 2021 e 1 del 2022
li posso importare entrambi nel programma e avrò i dati aggregati dei 2 anni.

[Torna all'indice della documentazione](./)
