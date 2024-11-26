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

ver. 1.0.29
Nuove Implementazioni :
  - Cambiato come il programma gestisce il calcolo per le plusvalenze dei movimenti di deposito e prelievo non categorizzati
	Con le versioni precedenti il programma semplicemente ignorava quelle transazioni (non veniva generata nessuna plusvalenza fino a che il movimento non veniva categorizzato)
	Con la versione attuale il programma si comporta nel seguente modo : 
		- Deposito non categorizzato = Deposito con Costo di Carico Zero
		- Prelievo non categorizzato = CashOut
	Facendo in questo modo avrò delle plusvalenze più alte fino a che non categorizzerò correttamente i movimenti ma altresì qualora dimenticassi di
	categorizzarli, questi sarebbero comunque conteggiati nella maniera meno vantaggiosa per il contribuente e quindi meno contestabile dal fisco.

Correzione di bug :
  - Corretto bug che impediva in taluni casi il recupero del prezzo
  - Corretto bug che portava ad errori sugli arrotondamenti sulle plusvalenze nel caso di scambi tra diversi wallet

ver. 1.0.28
Nuove Implementazioni :
  - Quadro RW : Introdotto un ulteriore opzione sul metodo di calcolo per l'utilizzo del LiFo anche sui SubMovimenti (Vedi Documentazione).
  - Aggiunto Icone ai pulsanti per un più facile ed intuitivo utilizzo.
  - Aggiunto possibilità di importare i dati da Tatax.
  - In "Transazione Crypto" aggiunto checkbox "Vedi solo movimenti non valorizzati" che mostrerà solo i movimenti in cui il programma non è riuscito a recuperarei prezzi.
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
