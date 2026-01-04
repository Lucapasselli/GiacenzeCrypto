DISCLAIMER
Il software fornito è in versione beta ed è destinato esclusivamente a scopi di testing e valutazione. Questo programma è progettato per assistere nei calcoli legati alla dichiarazione delle Crypto-Attività, ma non garantisce l'accuratezza, la completezza o l'affidabilità dei risultati.
L'utente utilizza questo software a proprio rischio e pericolo. Nonostante gli sforzi per identificare e correggere eventuali errori, il programma potrebbe contenere bug o fornire risultati errati.
Il creatore e i contributori del software non sono responsabili per eventuali danni diretti, indiretti, accidentali o consequenziali derivanti dall'uso del programma, inclusi errori nei calcoli, problemi tecnici o altre discrepanze. Si raccomanda agli utenti di verificare autonomamente i risultati prodotti e di consultare un professionista qualificato prima di presentare la propria dichiarazione.
Utilizzando questo software, l'utente accetta integralmente questi termini.



Il programma si occupa del calcolo delle giacenze medie, saldi iniziali e finali del Fiat e Card Wallet di Crypto.com.
Offre inolte la possibilità di rendicontare le crypto detenute su diverse piattafome.

Tra i file disponibili troverete sia un Installer che un Portable per Windows.
Negli zip dove non c'è specificato nulla invece ci sono gli eseguibili multipiattaforma (Windows/mac/linux) di Java
che richiedono però l'installazione del runtime di Java per funzionare.

Se doveste riscontrare problemi chiedete supporto sul gruppo telegram https://t.me/+6kfy5mjov-I2ODY8

NB: Per aggiornare la versione del programma basta sovrascrivere la cartella originale facendo prima un backup della cartella per sicurezza.
    Per gli utenti MAC attenzione che la sovrascrittura della cartella comporterà la perdita di tutti i dati, bisognerà aggiornare manualmente solo il contenuto della stessa.
    La versione con installer invece basta installarla sopra quella vecchia e verranno mantenuti i dati, questo dalla versione 1.0.4 in poi.
    Anche qui è consigliato sempre fare una copia di sicurezza.




Changelog

ver. 1.0.49
Nuove Implementazioni : 
 - Sotto "Opzioni" -> "Opzioni di Calcolo" aggiunto possibilità di disabilitare il calcolo automatico delle plusvalenze, questo velocizza le operazioni sui dati in presenza di molti movimenti.
 - Sotto "Opzioni" -> "Opzioni Rewards" aggiunto la possibilità di considerare i cashback crypto alla stregua di quelli FIAT e tra l'altro si potrà scegliere da che anno cominciare a considerarli in questo modo. (Il costo di carico sarà quello relativo al momento del ricevimento e non verrà generata nessuna plusvalenza)
   Prima di questa modifica i cashback potevano essere solo caricati a costo zero o caricati alla stregua di una reward da staking.
Correzione Bug : 
 - Corretto Bug che rallentava il caricamento della tabella in presenza di filtri attivi
 - Corretto Bug che impediva l'importazione di alcuni file da Tatax

ver. 1.0.48
Nuove Implementazioni : 
 - Etherscan ha messo lo scaricamento di alcune chain a pagamamento (es.BSC,BASE,AVAX) e deprecato altre (es. Cronos), per ovviare al problema sono state implementate nuove API.
	Per BASE, BSC, AVA  verranno utilizzate le API di Moralis (dalle prove che ho fatto sono leggermente meno precise di quelle etherscan nel senso che ogni tanto potrebbero perdere qualche transazione, circa 1 ogni 5000)
	Per la cronoschain invece verranno utilizzate le api proprie simili a quelle di etherscan con l'unico neo che il wallet bisogna scaricarlo ogni volta per la sua interezza per cui porterà via un pò di tempo.
 - Implementata nuova funzione per la gestione dei prezzi che comporta quanto segue : 
	- I prezzi vengono ora scaricati da 6 exchange contemporaneamente con la precisione al minuto.
	- Nei dettagli delle transazioni ci sarà scritta la fonte del prezzo e la sua precisione.
	- Premendo per la modifica del prezzo ora verrà mostrata una maschera dove si potrà scegliere tra vari prezzi nonchè inserirne uno personalizzato.
	- I prezzi presi da coingecko rimangono con precisione oraria perchè le api gratuite permettono solo quel tipo di richiesta.
	- Lo scaricamento dei prezzi darà più lento ma molto più preciso. 
	- Il primo scarico in assoluto potrebbe durare parecchi minuti perchè viene scaricato l'ambiente per JavaScript (Indispensabile per utilizzare la nuova libreria CCXT).
 - Su alcune tabelle ora è possibile la selezione della cella per dei copia/incolla più mirati, i campi vengono inoltre ripuliti dei tag html.
 - Migliorate le prestazioni sul caricamento della tabella principale.
	
ver. 1.0.47
Correzione Bug : 
 - Eherscan ha deprecato le Api V1, il programma usava ancora quelle Api per verificare se la chiave era valida.
   Questo causava l'impossibilità di scaricare nuove transazioni dalle chain, sistemata la problematica.

ver. 1.0.46
Nuove Implementazioni : 
 - Aggiunto possibilità di scegliere che le commissioni non generenino plusvalenze 
    per attivare andare su "Opzioni" - "Opzioni di Calcolo" e biffare l'opzione "Le commissioni non generano plusvalenza"
 - Nella classificazione dei prelievi aggiunto possibilità di classificare un movimento in uscita come "Burn", utile per gli NFT.
    (Questo movimento non genererà plusvalenze)
 - Nella funzione "Classificazione Depositi/Prelievi" ora è possibile classificare più movimenti utilizzando la selezione multipla.
    (Per farlo selezionare più righi e quindi classifica movimenti, attenzione che i movimenti selezionati devono essere dello stesso tipo, es. solo depositi o solo prelievi)
 - Aggiunto il pulsante "Exchange API" nella pagina principale dove si potranno inserire gli api in sola lettura degli exchange crypto (per ora solo Binance)
    (dalla stessa funzione è poi possibile scaricare i movimenti)
    Per Binance è consigliabile usare le Api Tasse
    NB. E' preferibile non scaricare più di 6 mesi di storico per risultati più attendibili visto che molti exchange hanno questo limite
    NB 2. Su binance non è gestito per ora lo staking di SOL ed ETH.  
 - Aggiunto file di log per la memorizzazione degli errori e delle operazioni fatte (il file si trova nella root del programma)	
Correzione Bug : 
 - Corretto problema che in alcuni casi impediva il corretto funzionamento dei filtri su colonna nella tabella principale.

ver. 1.0.45
Nuove Implementazioni : 
 - Aggiunto un nuovo filtro nella tabella principale che trova i movimenti a cui manca una parte delle stack del LiFo (tipicamente mancano acquisti per coprire la transazione)
 - Aggiunto icone di Alert sui movimenti con errori da correggere, posizionando il mouse sull'icona compare la spiegazione
 - Aggiunto al pulsante degli Errori anche i casi in cui manca parte del LiFo, mancando parte dello stack infatti verrebbe calcolata una plusvalenza più alta se non vengono corretti.
 - Aggiunto Funzione "Verifica Saldi Negativi" sotto "Analisi Crypto", in questa funzione vengono elencati tutti i momenti in cui c'è stato un saldo negativo su un qualsiasi wallet.
	Attenzione : Questo non significa che tutti i movimenti segnalati abbiano portato ad un errore nello Stack del LiFo che ricordo non è legato al dettaglio di un singolo Wallet ma alla totalità degli stessi piuttosto che al Gruppo (dipende dalle impostazioni scelte), è opportuno lo stesso effettuare tutte le correzioni per avere la plusvalenza il più corretta possibile.
 - Aggiunto messaggio che, qualora si cercasse di stampare il quadro RT relativo ad anni precedenti il 2023, ricorda che in quegli anni c'erano regole diverse e che la stampa del quadro potrebbe non essere corretta (il quadro era diverso e anche le regole, ad esempio i proventi d Staking andavano teoricamente in RL).
Correzione Bug : 
 - Corretto funzione per la rettifica giacenza che in taluni casi poteva portare ad un errore nella generazione dell'ID del movimento.
 - Corretto errore nella funzione "Scambio Crypto Differito", poteva capitare che i movimenti non venissero ordinati correttamente e portare ad errori visualizzabili nella nuova funzione "Verifica Saldi Negativi", per correggere basta togliere l'associazione del movimento e rifarlo, a quel punto l'ordine dei movimenti verrà creato correttamente.
 - Corretto bug che poteva portare cali di prestazioni fino al blocco del programma applicando i filtri sulle tabelle in un certo modo
 - Corretto bug che portava alla selezione errata del rigo di un paio di tabelle nel caso fossero applicati dei filtri.
 - Corretto bug che in taluni casi poteva mostrare uno stack del LiFo vuoto utilizzando la nuova funzione "Mostra LiFo Transazione"

ver. 1.0.44
Nuove Implementazioni : 
 - Implementato visualizzazione dello stack del LiFo della transazione tramite tasto destro sulla riga della tabella selezionando "Mostra LiFo Transazione"
 - Aggiunto ulteriori causali all'import di Binance
 - Cominciato la conversione delle icone a quelle vettoriali per una più alta resa
 - Migliorato performance dei filtri sulle tabelle che su molti movimenti (>20.000) potevano in taluni casi risultare molto lenti.
 - Aggiunto pacchetti DMG per MacOS e DEB per le distribuzioni basate su Debian/Ubuntu
Correzione di Bug :
 - Corretto bug che poteva portare alla visualizzazione dei giorni negativi sugli Exchange per cui si era selezionata l'opzione "paga bollo"
 - Corretto bug che non settava sul database al primo avvio del programma l'opzione presente in opzioni - opzioni di calcolo -> "Fino al 31-12-2022 considera tutti gli scambi crypto-crypto fiscalmente rilevanti"
   In sostanza da programma veniva visualizzata l'opzione spuntata ma sul database non scriveva nulla e quindi l'opzione non veniva considerata.
   L'opzione veniva poi scritta correttamente nel database la prima volta che ci si agiva fisicamente sopra premendoci con il mouse, da quel punto tutto poi funzionava correttamente.

ver. 1.0.43
Nuove Implementazioni : 
 - Implementato i filtri stile Excel su alcune tabelle, tasto destro sull'intestazione della colonna per modificarli.
 - Se sulla colonna ci sono numeri sotto il titolo della colonna compare anche la somma di tutti i righi visibili.
Correzione di Bug :
 - Corretto errore sulle importazioni da cointracking, se nel file c'erano più righi identici veniva preso solo il primo.
 - Corretto particolarità che impediva agli utenti Linux e Mac di aprire il popupMenu' sulle tabelle.

ver. 1.0.42
Nuove Implementazioni : 
 - Aggiunto lo splashscreen in avvio del programma perché in alcuni casi poteva metterci anche molti secondi prima di far vedere la maschera principale dando quindi l'impressione di non aver avviato il programma.
 - Cambiato colore della selezione della tabella con tema scuro perché quello precedente quasi non si riusciva a leggere.
Correzione di Bug :
 - Corretto importazione dei dust conversion di crypto.com app che potevano portare ad errori di giacenza sui CRO.
Se non vi tornano le giacenze dei CRO di fine anno consiglio di rifare l'importazione dei file con questa versione.
L'errore in questione poteva portare o ad errori minimi (di pochi CRO) o ad errori macroscopici (in talune circostanze spostava la virgola e portava al caricamento ad esempio di 1 milione di CRO anziché 1)
 - Sistemato importazioni del FIAT Wallet di Crypto.com, prima faceva fede sempre l'ultima importazione (ogni nuova importazione cancellava quelle vecchie), adesso, come succede nelle altre importazioni, si possono accodare le importazioni.

ver. 1.0.41
Nuove Implementazioni : 
 - Aggiunto nel PopUpMenu la possibilità di cambiare la tipologia di Reward ricevuta.
 - Aggiunto sezione per le Donazioni qualora voleste contribuire al progetto.
Correzione di Bug :
 - Corretto importazione EUR su Crypto.com APP CVS (Il nuovo CSV non mette più il segno meno sugli EUR in uscita)
 - Sistemato problema che portava ad avere delle giacenze iniziale errate sull'RW nel caso in cui ci fosse stata abilitata un opzione qualsiasi dalla B alla D + Opzione mostra solo giacenze fine anno se paga bollo + terza opzione sui trasferimenti.
 - Aggiunto casistiche su importazioni da cointracking.
 - Sistemato casistica particolare su importazione del CASH CSV di Crypto.com
 - Nelle stampe per il Quado W/RW ora se il valore finale o quello iniziale è molto prossimo allo zero ma non zero questo viene arrotondato per eccesso e valorizzato a 1.

ver. 1.0.40
Nuove Implementazioni : 
 - In depositi/prelievi aggiunta nuova tabella che fa vedere i movimenti correlati (utile per vedere ad esempio i trasferimenti dove vanno o gli scambi differiti)
 - Al PopUpMenu aggiunto la funzione "Modifica Note" e "Modifica Prezzo"
 - Adesso pe piccolissime differenze è possibile classificare un movimento come trasferimento anche se la qta di prelievo sul wallet i parenza è minore di quella di deposito sul wallet i destinazione.
 - Nelle importazioni da Cointracking o Tatax ora è possibile scegliere dei nomi personalizzati per i Wallet.
Correzione di Bug :
 - Importando i dati da Tatax i token messi in Staking vengono gestiti con un estensione es. ETH.STAKING@BINANCE, con questa nuova versione gli viene ripristinato il nome originale (es. ETH) per evitare problemi poi con i prezzi e con il LiFo.
 - Sempre negli import da Tatax i vari EARN, CASHBACK etc.. arrivano con prezzo Zero, ora il programma se vede che arrivano in questo modo gli assegna un valore.
 - Nella classificazione "Scambio Crypto differito" se il movimento di deposito non aveva prezzo anche lo scambio generato veniva valorizzato a zero ma senza segnalare la mancanza del prezzo.
Adesso questa cosa non succede più.
 - Nelle stampe dell'RW anche per gli anni bisestili ora i giorni di detenzione vengono messi al massimo a 365 anche per gli anni bisestili, questo per uniformarsi al software dell'AdE che non consente di inserire giorni di detenzione maggiori di 365.

ver. 1.0.39
Nuove Implementazioni :
 - Inserito filtro x Wallet e x Token nella tabella delle transazioni e nella tabella deli depositi/prelievi
 - Aggiunto avviso in caso di disponibilità di una nuova versione software.
 - In fase di import dalla blockchain inserito richiesta per effettuare o meno la rettifica automatica delle giacenze del token di riferimento della chain a fine importazione. (Prima veniva fatto automaticamente).
 - Nella gestione della classificazione dei depositi prelievi inserito possibilità di modificare i filtri automatici sui trasferimenti.
Correzione di Bug :
 - Corretto gestione delle FIAT diverse dall'euro in fase di importazione dal Binance Tax Report

ver. 1.0.38
Nuove Implementazioni :
 - Dato che attualmente i prezzi, se non presi dal CSV, vengono presi con una precisione oraria, da questa versione per gli scambi fiscalmente rilevanti verrà preso come prioritario
   il prezzo della stablecoin/FIAT presente nella transazione e mai quella della cripto che potrebbe essere piuttosto volatile.
 - Aggiunto Tasto "Ricalcola i prezzi delle transazioni" nella sezione "Opzioni - Varie" per permettere di ricalcolare i prezzi delle transazioni secondo le nuove regole.
   Prima del ricalcolo verrà mostrata una finestra in cui si potrà scegliere da che anno effettuare il ricalco.
   Per sicurezza verrà anche salvata una copia delle movimentazioni crypto nella cartella /Backup che rimarrà per 6 mesi.
   Ad ogni modo se non si salva, la modifica dei prezzi non è permanente, basta premere annulla nella schermata principale per tornare alla situazione iniziale.
Correzione di Bug :
 - Corretto diversi problemini di visualizzazione / errori bloccanti in casi molto particolari
 - Corretto Bug che, in fase di importazione dal Binace Tax Report, impediva di valorizzare i prezzi dei prelievi.

ver. 1.0.37
Nuove Implementazioni : 
 - Aggiunto popUpMenu su quasi tutte le tabelle del programma e su alcuni campi testo (si attiva con il tasto destro del mouse).
   Dal PopUp menù si potrà :
   - Copiare la selezione
   - Incollare gli appunti
   - Vedere i dettagli della transazione selezionata (comodo ad esempio sulle tabelle dove non vi sono tutti i dettagli).
   - Esportare la tabella in Excel (per permettere ulteriori elaborazioni e calcoli in autonomia).
 - Aggiunto 2 nuove causali utilizzate dal csv di Binance
 - Aggiunto la possibilità di classificare come Scam più token alla volta dalla funzione Depositi/Prelievi.
Correzione di bug :
 - Corretto Bug che, qualora vi fossero anni senza movimentazioni, non permetteva il cambio del prezzo sul dettaglio movimenti del quadro RT, inoltre faceva vedere a video il riferimento del prezzo errato.
 - Corretto Bug che non permetteva di associare un Wallet che all'interno del nome aveva l'apostrofo ad un Gruppo.

ver. 1.0.36
Nuove Implementazioni : 
 - Aggiunto pulsante "Modifica Movimento" in funzione "Classifica Depositi/Prelievi"
 - Aggiunto pulsante "Duplica Movimento" in funzione "Classifica Depositi/Prelievi"
Correzione di bug :
 - Corretto bug che impediva di creare un movimento opposto su un wallet in Defi dalla funzione "Crea movimento opposto" in "Classifica Depositi/Prelievi"
 - Coretto bug per cui il prezzo di WETH poteva risultare errato
 - Corretto bug nella sezione RW che in caso risultasse selezionata l'opzione "il LiFo viene applicato alla totalità dei Wallet" nella sezione "Opzioni RW" poteva in taluni casi particolari portare ad un errore.

ver. 1.0.35
Nuove Implementazioni : 
 - Aggiunto importazioni di Binance dal Financial Report
   NB : Importare o il Financial Report o il csv classico, mai entrambi per lo stesso periodo.
	Il Financial Report dovrebbe essere un po' più preciso soprattutto per quanto riguarda i prezzi.
 - Formattati alcuni campi numerici per rendere più leggibile la cifra.
Correzione di bug :
 - Corretto nome intestazione colonne nella sezione "RT & Analisi P&L", i nomi della colonna 2 e 3 erano invertiti.
 - Corretto bug che poteva portare al blocco del programma nel caso di doppia rettifica giacenza sulla stessa moneta.
 - Corretto Bug che poteva portare a salvare prezzi errati nella sezione giacenze a data qualora vi fossero stati inseriti prezzi personalizzati.
 - Nel pacchetto 1.0.34 avevo dimenticato di inserire le immagini del quadro T/RT e il file dei token gestiti da coincap.
   Questo portava all'impossibilità di recuperare alcuni prezzi nonchè l'impossibilità di stampare il quadro T/RT correttamente.
 - Sistemato Bug che mandava in blocco il programma nel caso in cui si lanciasse l'eleaborazione del quaro T/RT senza dati.

ver. 1.0.34
Nuove Implementazioni
  - Aggiunto Api di Coinbase e Cryptocompare per il recupero dei prezzi
  - Aggiunto possibilità di inserire le ApiKey di Coincap (Opzioni - ApiKey)
  - Aggiunto possibilità di inserire le ApiKey di Coingecko per incrementare la velocità di acquisizione sui prezzi della DeFi (Opzioni - ApiKey)
  (Ad oggi quindi i prezzi delle Cripto vengono recuperati da Binance,Coinbase,Cryptocompare,Coincap(con Apikey dedicata) e Coingecko (Per la defi e solo ultimi 365gg)
  - In "RT & Analisi P&L" Aggiunto possibilità di stampare il Quadro T per i redditi 2024 (con le prossime versioni verrà aggiunto anche il quadro RT)
Correzione di bug :
  - Coincap essendo diventato a pagamento non permetteva più lo scaricamento dei prezzi causando errori nel recupero, aggiunto la possibilità di integrare la loro ApiKey.

ver. 1.0.33
Nuove Implementazioni:
  - Aggiunto PMC (Prezzo Medio di Carico) nel dettaglio della funzione "RT & Analisi P&L"
Correzione di bug :
  - Corretto Bug che impediva di inserire prezzi personalizzati su rete Solana
  - Corretto Bug che impediva di recuperare i prezzi salvati su rete Solana
  - Corretto qualche bug grafico

ver. 1.0.32
Nuove Implementazioni :
  - Aggiunto supporto alla Blockchain Solana (Sono necessarie le ApiKey di Helius da inserire su "Opzioni" - "ApiKey")
	(Si può creare tranquillamente la chiave api gratuita dal loro sito registrandosi)
  - Aggiunto supporto a Berachain e Avalanche
  - Da questa versione i movimenti di tutte le chain evm supportate vengo importati attraverso le api di etherscan.
	Sarà quindi necessario registrarsi su etherscan e generare una ApiKey (gratuita) per poter scaricare i movimenti.
	L'ApiKey poi andrà inserita in "Opzioni" - "ApiKey".
  - Aggiunto anno 2024 nella sezione del Quadro RW, per il momento i moduli sono però ancora quelli dell'anno passato.

ver. 1.0.31
Nuove Implementazioni :
  - Aggiunto ulteriori causali di Binance per le importazioni
  - Ad ogni fine importazione dalla chain  aggiunto controllo ed eventuale sistemazione giacenza sul token di riferimento della chain.
	Questo si rente necessario per i layer 2 in quanto le api non ritornano la parte di commissione relativa al layer 1.
	Fortunatamente questa parte di commissione è di circa 1 centesimo per ogni transazione quindi i movimenti correttivi saranno di piccola entità.
  - Aggiunto BTC alle chain disponibili per importazioni da tatax e cointracking.
  - Nelle Pulizie adesso è possibile impostare dei range di date entro quale cancellare i dati.
  - Nella funzione "Giacenze a Data" aggiunto segnalazione per i token senza prezzo
  - Nella funzione "Calssificazione Depositi/Prelievi" - "Classifica Movimento" ora c'è una nuova colonna con il controvalore in euro della transazione.
Correzione di bug :
  - Corretto erroreche poteva portare ad un blocco delle importazioni di Crypto.com
  - Corretto errore che impediva di gestire correttamente i gruppi wallet se sul nome dei wallet erano presenti degli apici

ver. 1.0.30
Nuove Implementazioni :
  - Compilato i pacchetti con la versione Opensource del Runtime Java (in caso di problemi sulla versione multipiattaforma scaricare il jre su questo sito https://adoptium.net/temurin/releases/)
  - in "Opzioni" - "Varie" aggiunto bottone "Disclaimer" e "Avvertenze/Problemi Noti" per raggiungere velocemente la documentazione.
  - In "Opzioni" - "Opzioni di calcolo" aggiunto la possibilità di non considerare nei calcoli per la plusvalenza i movimenti non classificati (come succedeva prima della versione 1.29)
  - In "Modifica Movimento" aggiunto tasto "Sblocca Modifiche" per poter modificare tutti i campi del movimento
  - In "Classificazione Depositi/Prelievi" aggiunto la possibilità di classificare i movimenti come "Donazione"
  - In "Analisi Crypto" "Classificazione Depositi/Prelievi" aggiunto pulsante "Identifica come SCAM" (già presente nella funzione Giacenze a data)
Correzione di bug :
  - Import da Wallet : Gli Explorer possono riconoscere come transazioni reali log di smartcontract (solitamente sono prelievi di token SCAM), ovviamente essendo transazioni fittizie non sono da considerare le fee.
    Da questa versione il programma non imputa più quelle fee al wallet per questo tipo di transazioni.
    Le transazioni resteranno comunque visibili per coerenza con quanto presente nell'explorer, saranno poi eventualmente da escludere da eventuali calcoli classificandole come SCAM.
  - Import da Wallet : Se per sbaglio si inviano fondi allo stesso wallet di partenza il programma prima riconosceva solo un movimento di prelievo, ora lo considera come uno scambio a vuoto.
    Di fatto non c'è nessun movimento di fondi se non le fee.
  - Corretto bug nelle importazioni di Binance che portava alla non classificazione di alcuni movimenti.
  - Corretto bug nelle importazioni da Tatax che portava ad un errore sui prezzi
  - Corretto bug nelle importazioni di Tatax riguardante l'orario, su file csv infatti l'orario viene riportato in UTC, adesso viene convertito nell'orario italiano.
  - Corretto bug nelle esportazioni di Tatax, ora l'orario prima di essere esportato viene convertito in UTC

ver. 1.0.29
Nuove Implementazioni :
  - Cambiato come il programma gestisce il calcolo per le plusvalenze dei movimenti di deposito e prelievo non categorizzati.
Con le versioni precedenti il programma semplicemente ignorava quelle transazioni (non veniva generata nessuna plusvalenza fino a che il movimento non veniva categorizzato)
	Con la versione attuale il programma si comporta nel seguente modo : 
		- Deposito non categorizzato = Deposito con Costo di Carico Zero
		- Prelievo non categorizzato = CashOut
	Facendo in questo modo avrò delle plusvalenze più alte fino a che non categorizzerò correttamente i movimenti ma altresì qualora dimenticassi di
	categorizzarli, questi sarebbero comunque conteggiati nella maniera meno vantaggiosa per il contribuente e quindi meno contestabile dal fisco.
  - Cambiato il tipo dato della colonna Plusvalenza e Valore Transazione ora sono di tipo numerico quindi ordinabili correttamente
  - In Opzioni - Varie aggiunto la possibilità di abilitare il tema scuro.
  - Implementata nuova funzione in "Analisi Crypto" - "RT & Analisi P&L" dove si potrà vedere l'analisi delle plusvalenze realizzate e non per ogni singolo anno, inoltre per ogni anno si potranno vedere le plusvalenze per ogni token e nel caso sia abilita l'opzione "Abilita Calcolo plusvalenze per gruppo Wallet" anche divise per gruppo wallet.
Per ogni token poi ci sarà la possibilità di vedere lo stack del LiFo con i vari costi di carico in modo da poter pianificare al meglio la realizzazione delle plusvalenze future. 
  - Inserito nuove casistiche riguardanti le importazioni da Binance
  - Aggiunta Blockchain BASE alla lista di quelle gestite nativamente. (Funzione Inserisci Wallet)
  - Aggiunta Blockchain ABITRUM alla lista di quelle gestite nativamente. (Funzione Inserisci Wallet)
Correzione di bug :
  - Corretto bug che impediva in taluni casi il recupero del prezzo
  - Corretto bug che portava ad errori sugli arrotondamenti sulle plusvalenze nel caso di scambi tra diversi wallet e qualora non fosse biffata l'opzione "Abilita Calcolo plusvalenze per gruppo wallet"
  - Sistemato un bug i un caso particolarissimo che impediva il caricamento dei dati da CDCApp
  - Corretto possibile mancata imputazione del prezzo dalla funzione automatica della modifica transazione
  - Corretto errore bloccante in importazione da Tatax

ver. 1.0.28
Nuove Implementazioni :
  - Quadro RW : Introdotto un ulteriore opzione sul metodo di calcolo per l'utilizzo del LiFo anche sui SubMovimenti (Vedi Documentazione).
  - Aggiunto Icone ai pulsanti per un più facile ed intuitivo utilizzo.
  - Aggiunto possibilità di importare i dati da Tatax.
  - In "Transazione Crypto" aggiunto checkbox "Vedi solo movimenti non valorizzati" che mostrerà solo i movimenti in cui il programma non è riuscito a recuperare i prezzi.
    In questo modo tramite il tasto "Modifica Movimento" sarà facile individuarli e andare ad assegnare un prezzo alla transazione.

ver. 1.0.27
Nuove Implementazioni :
  - Quadro RW/W : Adesso se si sceglie l'opzione di considerare solo le giacenze di inizio e fine anno il programma non mette di default 365 giorni di detenzione ma
    qualora il conto/wallet fosse stato aperto nel periodo di riferimento farà il calcolo a partire dal primo movimento del wallet alla fine dell'anno.
  - Quadro RW/W : Inserito tasto per la stampa di un report più dettagliato dei calcoli in formato Excel
  - Quadro RW/W : Nella stampa del Report in PDF aggiunto il quadro RW e numerose istruzioni
  - Riorganizzato la tabella delle opzioni sul quadro W/RW in modo che sia più chiara
Correzione di bug :
  - Quadro RW/W : le giacenze negative venivano segnalate ma il programma le mostrava senza il segno meno

ver. 1.0.26
Nuove Implementazioni :
  - Inserito nuova sezione in "Opzioni - Opzioni Rewards" dove è possibile decidere come trattare ogni tipologia di reward
  - Implementato Api di CoinCap al posto di Cryptohistory per l'assegnazione dei prezzi poichè quest'ultima non fornisce più il servizio
  - Inserito possibilità di stampare una prima bozza del quadro W
  - Inserito possibilità di calcolare i valore per il quadro W anche per gli anni precedenti al 2023 (Attenzione, vengono usate le stesse regole del 2023, potrebbe essere sbagliato) 
Correzione di bug :
  - Sistemato bug che impediva di correggere il valore iniziale e finale di un token nella sezione RW
  - Sistemato bug che impediva di calcolare i valori con la media ponderata nel caso di classificazioni particolari


ver. 1.0.25
Nuove implementazioni : 
	- Una volta selezionata la data dalla Funzione giacenze a data questa viene memorizzata per i successivi accessi
	- Una volta selezionato un file da importare la path viene salvata per le successive importazioni per evitare di dover riselezionare l'intero percorso
	- Aggiunto tipologia "finance.lockup.dpos_compound_interest.crypto_wallet" a quelle supportate dalle importazioni da CDC APP
  - Aggiunto 3 nuove tipologie per le importazioni da Binance
Correzione di bug :
  - Corretto errore nelle importazioni dei wallet in DeFi per cui non venivano conteggiate le fee per le operazioni di deposito da Piattaforme DeFi
  - Corretto tasto Dettaglio DeFi non funzionante su rete Ethereum

ver. 1.0.24
Nuove implementazioni : 
	- In "Opzioni" - "Gruppi Wallet Crypto" inserita possibilità di aggiungere un Alias e indicare se è già stato pagato il bollo
	- Aggiunto le seguenti nuove opzioni di calcolo per i dati relativi al quadro W\RW su "Opzioni" - "Crypto" :
		1 - Possibilità di vedere semplicemente le giacenze di inizio e fine anno
		2 - Possibilità di scegliere la condizione per cui viene creato un nuovo quadro RW
		3 - Per i Wallet in cui è già stato pagato il bollo aggiunta possibilità di vedere le sole giacenze di inizio e fine anno
		4 - Dato possibilità di gestire diversamente le Reward
		5 - Aggiunto ulteriore opzione per la gestione dei trasferimenti tra Wallet
	- Nella sezione "Analisi Crypto" - "W/RW" aggiunto le seguenti voci
		1 - Totale IC Dovuto
		2 - Dettaglio IC per singolo rigo RW
		3 - Bottone con link a documentazione su come vengono calcolati i Qadri e spiegazioni delle varie opzioni disponibili
	- Nella schermata "Transazioni Crypto" aggiunto in fondo ulteriori dettagli sul calcolo della Plusvalenza Totale
Correzione di bug :
	- Correzione di vari bug che potevano portare a blocchi in determinate condizioni
	- Sistemato un errore di calcolo sul prezzo automatico nella funzione di inserimento manuale del movimento nel momento in cui si vende per Euro.
	
ver. 1.0.23
Nuove implementazioni :
  - Aggiunto nuove opzioni di calcolo per i dati relativi al quadro W\RW su "Opzioni" - "Crypto"
Correzione di bug :
  - Corretto problemi su modifiche movimenti automatici
  - Aggiunto gestione sui token ibridi delle stable legate al dollaro sugli echange chiamate USD (il loro valore lo lego a quello di giornata del dollaro)

ver. 1.0.22
Correzione di bug :
  - Corretto errore che impediva la creazione di movimenti manualmente

ver. 1.0.21
Nuove implementazioni :
  - Aggiunto log dettagliato durante le importazioni
Correzione di bug
  - Corretto errore per importazioni risaltenti a prima del 2017

ver. 1.0.20
Nota Importante : Il programma al primo avvio dovrà fare un adeguamento degli archivi che potrebbe portare via qualche minuto
Nuove Implementazioni :
  - Nella Tab "Analisi Crypto" aggiunta sezione reletiva al calcolo del quadro RW secondo il metodo LIFO con media ponderata.
      Nota 1 - Il programma è in una prima fase beta e potrebbe contenere errori, usare per test ma non consiglio di usarla per la dichiarazione
      Nota 2 - Per vedere il calcolo dei giorni è opportuno correggere tutti gli errori che presenta
  - Nella Tab "Opzioni - Crypto" aggiunto le seguenti opzioni
      1 - Possibilità di calcolare il quadro rw facendo un nuovo rigo per ogni operazione (attualmente il nuovo rigo viene fatto solo se avviene un operazione fiscalmente rilevante)
      2 - Aggiunto la possibilità di ritenere fiscalmente rilevanti per i movimenti fino al 31-12-2022 tutti gli scambi (questo ovviamente influisce sui costi di carico del 2023 e di conseguenza sulle plusvalenze)
      3 - aggiunto la possibilità di considerare tutte le reward fino al 31-12-2022 come provento a costo di carico Zero
  - In "Analisi Crypto - Giacenze a Data" inserito la possibiltà di non far vedere i token scam
  - In Opzioni - Export ora è possibile generare un file di export con i vostri dati per Tatax
      Nota 1 - Non verranno esportati i token contrassegnati come scam
      Nota 2 - Verranno generati 2 file di cui uno con la scritta defi alla fine.
             Quello con la scritta defi contiene le movimentazioni generate in automatico dal programma quando gli dite che un determinato movimento è un versamento/prelievo su un vault in defi.
  - Implementata importazione da OKX (E' importante caricare sia il Csv del Funding Wallet che quello del Trading altrimenti vi ritroverete con dei dati incompleti)
Correzione di Bug :
  - Sistemato problemi in importazioni sul csv del "Crypto Wallet" e  di Binance.
  - Sistemato problemi vari che potevano portare a freeze del programma in talune circostanze.

ver. 1.0.19
Correzione di Bug :
  - Aggiunto ulteriori voci che prima venivano scartare nell'importazione del Fiat e del Crypto wallet di Crypto.com
  - Tolto dalle importazioni di Crypto.com tutti i movimenti di blocco e sblocco dei token per il limit order in quanto generavano un sacco di righe inutili.

ver. 1.0.18
Nuove Implementazioni :
  - Aggiunto ulteriore provider per la ricerca dei prezzi poichè coingeko ha limitato le richieste api gratuite agli ultimi 365 giorni
  - In Opzioni - "Gruppi Wallet Crypto" è possibile scegliere di calcolare la plusvalenza divisa per gruppi di wallet
  - In inserimento manuale ora se si seleziona una riga verranno riportate automaticamente Data, ora e Wallet per velocizzare gli inserimenti.
  - Sui movimenti inseriti manualmente è ora possibile modificare tutti i campi
Correzione di Bug :
  - Sistemato problemi in importazioni sul csv del "Crypto Wallet" e del "Fiat Wallet" di Crypto.com
  - Sistemato blocchi in fase di importazione dalla defi (dovuti al cambio di politiche del piano gratuito di coingecko)

ver. 1.0.17
Nuove Implementazioni :
  - Modificato visualizzazione dettagli riga
  - In "Opzioni - Gruppi Wallet Crypto" aggiunto la possibilità di raggruppare i wallet a piacere
    (Questa funzione è una predisposizione per future implentazioni ma attualmente è fine a se stessa)
  - In "Opzioni - E-Money Token" aggiunto la possibilità di indicare quali monete e da quando sono E-Money Token.
    Questo inciderà sul calcolo delle plusvalenze,che verranno immediatamente ricalcolate, in quanto lo scambio crypto-EMT è fiscalmente rilevante.

ver. 1.0.16
Nuove Implementazioni :
  - Aggiunto import da rete Ethereum
  - Aggiunto tasto per l'esportazione in csv dei movimenti crypto.
Correzione di Bug:
  - Sistemato problema per cui quando si aggiungenva un nuovo wallet CRO alla prima importazione non veniva lanciato il controllo sulle giacenze.

ver. 1.0.15
Nuove Implementazioni : 
  - Aggiunto correzione automatica giacenze CRO sulle importazioni da cronos chain
    (Alcuni movimenti di CRO es. alcuni scambi da CRONOS-POS a Cronos.org o alcune fees gestite da smart contract non vengono visti dall'explorer, 
      questo provocava errori sulle giacenze di CRO, adesso ad ogni fine importazioni vengono controllati tutti i movimenti e per ogni blocco viene richiesta la giacenza dei CRO del Wallet,
      poi viene in automatico creato un movimento correttivo per portare la giacenza di CRO alla quantità esatta)
    Per correggere i movimenti pregressi già importati basta rilanciare l'importazione dei wallet e verrà tutto gestito in automatico, l'unica pecca è che porterà via più tempo di prima.
  - Nella funzione "Classificazione Trasferimenti Crypto" aggiunto possibilità di classificare i movimenti come "Trasferimenti da e per Vault/Piattaforma di rendita" per identificare i 
    trasferimenti verso piattaforme di rendita es. PancakeSwap,VVS,Beefy etc...
    Tra le altre cose nel momento in cui viene classificato il movimento in questo modo il programma identifica lo smart contract e chiede se identificare allo stesso modo tutti gli altri
    movimenti che interagiscono con lo stesso smartcontract, questo dovrebbe velocizzare di molto le classificazioni sui trasferimenti.
  - Nella funzione "Classificazione Trasferimenti Crypto" il pulsante assegnazione automatica ora riconosce alcuni movimenti automaticamente e li classifica in maniera corretta
    es. riconosce in automatico gli scambi tra WCRO e CRO e li classifica.
Correzione di BUG :
  - Tolto pulsante che compariva nella prima pagina e che non aveva alcuna funzione
  - Sulla sezione card wallet e fiat wallet corretto sezione rimanenze nella tabella dettagli movimento
    (prima,solo nella sezione dettaglio movimenti, non veniva considerata la giacenza di inizio anno e le rimanenze partivano da zero)

ver. 1.0.14
Nuove Implementazioni : 
  - Aggiunto importazione file CSV Binance
    Attenzione : 1 - Dai file CSV di Binance non vengono estratti gli spostamenti interni (Es. quando passate i token da SPOT a EARN)
                     in quanto non rilevanti e perchè Binance ha cambiato diverse volte nel tempo il formato del file CSV per questi movimenti
                 2 - Potrebbero esserci piccole o grandi imprecisioni sui file CSV di Binance, ho riscontrato le seguenti problematiche:
                     A - A volte sul CSV mancano alcuni movimenti di Earn sui BNB (me ne mancavano 8 in un anno anche se poi riestraendo il CSV qualche mese dopo sono ricomparsi)
                     B - Binance ad un certo punto non mette più nel file CSV i movimenti sulle coppie che ha tolto dal suo listino (non conosco il timing però)
                         Questo significa che potrebbero mancarvi dei trade su coppie che non esistono più.
   - Nuove implementazioni della funzione "Giacenze a Data"
        1 - Migliorata la grafica sulle tabelle 
        2 - si può identificare un token come scam o cambiare il suo nome se arriva dalla DEFI
        3 - Si può modificare il prezzo di un token ad una certa data
        4 - Per i wallet in defi si può vedere la situazione attuale delle giacenze di tutti i token detenuti attraverso il relativo explorer
        5 - Si possono creare dei movimenti correttivi (es. mi accorgo che di un certo token a fine anno ho una giacenza errata posso inserire dei movimenti correttivi)
            (Questo succede spesso con scheetcoins con tokenomics strane (es. tasse o premi su blockchain ricevibili al momento di una movimentazione))
   - Nella funzione "Classificazione Trasferimenti Crypto" aggiunto la classificazione "Scambio Differito" dove verranno associati un movimento di prelievo e uno di deposito anche differiti nel tempo
      (Viene usata ad esempio per classificare gli scambi attraverso bridge, scambio attraverso piattaforme con del timing o cmq scambi che richiedono del tempo per essere completati)
Correzione di BUG :
   - Corretto bug in importazioni che cancellava le associazioni fatte sulla classificazione movimenti
   - Corretto bug dovuto ad arrotondamenti che poteva portare a costi di carico negativi (anche se di importi molto piccoli)
   - Corretto bug su importazioni da cointracking su file in lingua Italiana (Prima venivano gestiti solo quelli in inglese)
  

ver. 1.0.13
Nuove Implementazioni :
  - Sotto "Analisi Crypto" inserita nuova funzione chiamata "Giacenze a data" che permette di calcolare le giacenze 
    delle crypto con relativo valore ad una specifica data.
Correzione di BUG :
  - Corretti diversi bug nelle importazioni da blockchain
    (reimportare i wallet per applicare le correzioni sul pregresso)

ver. 1.0.12
Nuove Implementazioni : 
  - Aggiunto importazione di wallet da rete cronoschain (no ERC1155)
  - Aggiunto possibilità di classificare un movimento di deposito come acquisto, verrà chiesto l'importo pagato in euro.
    (Utile per acquisti di crypto da chain o da amici in contanti o altro)
Correzione di BUG :
  - Corretto bug su importazione di wallet da chain con più di 10000 movimenti
    (Qualora vi fossero sarà da rifare l'importazione per sistemare i movimenti)
  - Altri vari piccoli BugFix

ver. 1.0.11
- Corretto errata visualizzazione costi di carico sui depositi e prelievi
- Corretto calcolo plusvalenza in caso di rimborsi
- Altri piccoli bugfix

ver. 1.0.10
- Sistemato calcolo delle plusvalenze sugli NFT
- Implementato importazione dati su acquisti di beni/servizi con crypto da CSV Crypto.com
- Implementato importazioni acquisti ricorrenti e scambi crypto tra app Crypto.com da file CSV
- Ora il bottone dettaglio DEFI si illumina e può essere premuto solamente nel caso in cui vi siano i dati

ver. 1.0.9
Nuove Implementazioni : 
	- Implementato importazioni dati dal file csv di Crypto.com che comprendono anche la nuova modalità di Staking
	- Implementato importazione e gestione degli NFT ERC721 su rete BSC
	- Implementato possibilità di aggiungere, modificare ed eliminare movimenti manualmente
Correzione di BUG :
	- Corretto importazione dati nel caso in cui la data nel CSV di Crypto.com sia quella attuale
	- Corretto errata assegnazione di valori null al posto di blanc durante le importazioni dal SCV di Crypto.com
	- Corretto errore di importazione in rari casi nel caso di dustConversion dal file CSV di Crypto.com
	- Corretto problema sul bottone interrompi in fase di importazione dati da blockchain

ver. 1.0.8
Corretto errore che impediva l'importazione del csv crypto.com app nel caso di acquisti crypto con carta
Implementato importazione diretta da blockchain su rete BSC inserendo il numero di wallet
(ancora non supportato importazione di scambi con NFT, a breve verrà implementata la parte relativa agli NFT ERC721)
(per i token ERC1155 bscscan non ha ancora implementato correttamente le API , non appena lo farà saranno implementati anche quelli)
Problema noto : Il bottone interrompi in fase di importazione dati potrebbe non funzionare (verrà corretto nella prossimaversione)

Note: Per colpa delle limitazioni sulle chiamate delle Api gratuite la prima importazione del wallet potrebbe impegnare molto tempo


ver. 1.0.7
Implementato importazione dati delle cripto da Crypto.com App
Implementato importazione dati delle cripto da Cointracking.info
Implementato classificazione dei movimenti di trasferimento tra wallet
Implementato calcolo della plusvalenza con il metodo Lifo
NOTE:
	- La plusvalenza viene generata solo nei seguenti casi (gli scambi Crypto-Crypto non generano plusvalenza)
		1 - Cashout
		2 - Cashback, reward e similari 
		3 - Commisioni (se esplicitate in fase di importazione)
	- Per un corretto calcolo è importante inserire tutti i movimenti da tutti i wallet ed exchange dall'inizio della detenzione
	- NFT e derivati non sono per ora implementati. 

ver. 1.0.6
Corretto problema per cui si analizzava un periodo senza movimenti il saldo iniziale veniva messo a zero invece che calcolarlo dai movimenti precedenti.
Tolto filtri per importazioni dati card wallet, ora importa anche file con più campi di quelli previsti, attenzione a caricare il file corretto.
Questo filtro è stato tolto perchè in taluni casi il file del card wallet presentava dei dati aggiuntivi e quindi non veniva importato.

ver. 1.0.5
Migliorato layout di stampa

ver. 1.0.4
Sistemato problema di calcolo sulla diffeenza delle date, in alcuni casi particolare veniva arrotondato per difetto e poteva influire di 1 giorno sul calcolo della giacenza media
Sistemato problema sul cardwalle in cui non veniva mai considerata la rimanenza iniziale se imputata a mano
Aggiunto nelle tabelle e nel pdf le rimanenza data per data
Aggiunto logo

ver. 1.0.3
Sistemato problema di calcolo sui bonifici verso la banca fatti dal FiatWallet (prima venivano erroneamente sommati invece che sottratti)
Aggiunto pulsante per la generazione di un report in pdf sia sul Fiat Wallet che sul Card Wallet

ver. 1.0.2
Sistemato problema con gestione dei movimenti anomali

ver. 1.0.1
Modificato importazione movimenti, prima non venivano importati i movimenti eseguiti nel medesimo secondo benchè diversi
Ora vengono scartati solo i movimenti con stessa ora (al secondo),causale e importo.
+
Ora si possono importare più csv dello stesso tipo es. se ho 2 csv del fiat wallet, 1 del 2021 e 1 del 2022
li posso importare entrambi nel programma e avrò i dati aggregati dei 2 anni.
