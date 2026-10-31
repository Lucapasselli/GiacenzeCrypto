# Novità delle versioni di prova

Una sezione per ogni versione di prova (beta), con ciò che è cambiato rispetto alla beta precedente. La mostra il
programma all'avvio di una beta, ed è la descrizione della beta su GitHub. L'elenco completo delle novità della versione
in preparazione è nella sezione "in lavorazione" di docs/documentazione/changelog.md, dove ogni voce di questo file
compare con lo stesso titolo. Questo file non è una pagina del sito.

## Versione 1.0.65.17

**Nuove implementazioni**

- **Fallimenti delle piattaforme (in prova)**: in fondo alla scheda *Fallimenti piattaforme* c'è ora la casella *Tratta i rimborsi come scambi ordinari fra cripto-attività*, spenta di default. Di default ogni rimborso è un realizzo, con la sua plusvalenza o minusvalenza. Con la casella spuntata il rimborso segue le regole di ogni altro scambio fra cripto-attività: il credito di un BTC rimborsato in ETH non genera plusvalenza e passa il costo di carico all'ETH, quello di una stablecoin E-Money rimborsato in ETH realizza, secondo il tipo della moneta che era depositata alla data del rimborso e le opzioni sugli scambi. Vale anche per i rimborsi già registrati. Il blocco, la restituzione in natura, i rimborsi oltre il credito e la chiusura non cambiano. Negli anni in cui c'è un rimborso la scelta compare fra le opzioni stampate dei quadri RT e W/RW e nell'archivio delle dichiarazioni.

## Versione 1.0.65.16

**Nuove implementazioni**

- **Giacenze a data, costo medio netto**: spuntando la casella *Mostra costi netti* sotto la tabella compaiono cinque colonne, nascoste di default: *Costo totale*, *Vendite*, *Costo medio netto*, *Profitti/Perdite totali* e le *Note*. Il costo medio netto è (speso - incassato) diviso la quantità posseduta, come nella scheda *My Portfolio* di CoinGecko, e può scendere sotto zero. Non è il costo di carico LIFO e non entra nelle dichiarazioni. Il calcolo vale per tutti i wallet insieme e per simbolo e tipo del token. Un movimento senza valore in euro lascia vuote le colonne e le note lo segnalano.

- **Giacenze a data, manuale e pulsante Guida**: la scheda ha ora il pulsante *Guida...*, sopra il dettaglio dei movimenti, che apre un manuale completo con screenshot su dati inventati. Spiega la tabella, il costo di carico, il dettaglio dei movimenti, il grafico, la lettura di un solo wallet, *Sistema Qta Residua*, i token SCAM e il confronto con blockchain ed exchange. Le colonne dei costi netti hanno un manuale a parte, richiamato dal primo.

## Versione 1.0.65.15

**Nuove implementazioni**

- **Fallimenti delle piattaforme (in prova)**: nuova scheda *Analisi Crypto – Fallimenti piattaforme* per chi aveva monete su una piattaforma fallita, come Celsius o FTX. Alla data della domanda di fallimento le monete, e gli euro, diventano un credito che conserva il costo di carico, e ogni rimborso registrato è uno scambio dal credito alle monete ricevute, con la sua plusvalenza o minusvalenza. Ci sono anche la chiusura della procedura, l'annullamento dell'ultima operazione, la scelta del valore del credito nel quadro W/RW e una guida passo per passo con gli esempi di Celsius, FTX e Voyager.

- **Importazione da Exodus (in prova)**: nella finestra *Importazioni* c'è ora la voce *Exodus – Transazioni*, per l'export CSV delle transazioni del wallet Exodus. Depositi e prelievi diventano trasferimenti da classificare, e le ricompense di partecipazione di Algorand diventano premi.

- **Giacenze a data, tutte le colonne dei movimenti nel dettaglio**: nel dettaglio dei movimenti di una moneta il pulsante *Colonne...* propone anche tutte le colonne della tabella dei movimenti, per esempio *Causale originale*, *Hash Transazione* o *Note*.

**Correzione di bug**

- **Avviso di movimento in ingresso mancante sulla moneta sbagliata**: con la tabella dei movimenti filtrata o ordinata, il suggerimento dell'icona poteva descrivere il movimento di un'altra riga. Ora descrive sempre quello della riga su cui si trova il mouse.

- **Quadro T/RT, "Dettagli x Tipologia", e dettagli del quadro W/RW più veloci**: dal secondo caricamento in poi queste tabelle impiegavano qualche secondo a riempirsi quando le righe erano migliaia. Ora si riempiono subito.

## Versione 1.0.65.14

**Nuove implementazioni**

- **Prova di una configurazione di importazione**: nella finestra *Importazioni* il pulsante *Prova configurazione* legge un file CSV con una configurazione JSON e mostra cosa produrrebbe senza importare nulla: i movimenti, cosa esce da ogni causale, il saldo di ogni moneta e le righe scartate con il motivo. Il resoconto si copia in testo semplice per incollarlo in un chatbot.

- **Configurazioni della community**: il pulsante *Configurazioni della community...* della finestra *Importazioni* apre il catalogo delle configurazioni condivise dagli utenti, e quelle scaricate compaiono nelle tendine con il prefisso *Community:*. Dopo una prova senza problemi si può proporre la propria, che diventa scaricabile solo dopo il controllo dell'autore del programma.

- **Configurazioni di importazione scritte con un'IA**: il manuale *Configurazione JSON per le importazioni personalizzate* spiega cosa dare a un assistente come ChatGPT o Claude per fargli scrivere una configurazione corretta. Le regole sono pubblicate anche come schema JSON per gli editor, e il manuale documenta ora tutte le opzioni.

- **Tipi di movimento non riconosciuti nelle configurazioni**: se una configurazione di importazione assegna a una causale un tipo che il programma non conosce, per esempio `reward` in minuscolo, il resoconto dell'importazione lo segnala. Prima quelle righe venivano importate in silenzio come movimenti generici.

**Correzione di bug**

- **Equivalenze dei prezzi valide per tutti gli anni**: le monete prezzate come un'altra sugli exchange (WETH come ETH, USDC.e come USDC) usano l'equivalenza in tutti gli anni, e la casella *Prezzi dagli exchange anche per gli anni già dichiarati* non c'è più: un prezzo inserito a mano ha sempre la precedenza. Con la casella attiva, com'era sulle installazioni nuove, MATIC restava senza prezzo a fine 2022 e fine 2023: ora prima del 4 settembre 2024 MATIC si prezza come MATIC. Per chi aveva la casella spenta i valori degli anni passati nel quadro W/RW, ricalcolati, possono cambiare: di solito di pochi centesimi, ma una moneta che prima restava senza prezzo ora prende il suo valore intero.

## Versione 1.0.65.13

**Nuove implementazioni**

- **Una ricompensa riportata a deposito da classificare**: *Cambia Tipologia Reward* ha la scelta *Deposito da classificare*, per una moneta che l'exchange registra come ricompensa ma non lo è, per esempio il BTTC ricevuto su Binance nella ridenominazione di BTT del gennaio 2022. Il movimento diventa un deposito da classificare con lo stesso valore, e si può abbinare come scambio differito al prelievo della vecchia moneta.

**Correzione di bug**

- **Cambia Tipologia Reward su più movimenti**: con più movimenti selezionati la tipologia veniva assegnata solo al primo. Ora vale per tutte le ricompense selezionate, e il rimborso di una ricompensa non perde più "RIMBORSO" nella descrizione.

- **Prezzi di MATIC ed ETH2**: dopo la migrazione a POL nessun exchange quota più MATIC, ed ETH2 di Coinbase non ha mai avuto quotazioni in euro o dollari, quindi il loro valore al 31/12 (per esempio a fine 2024) restava senza prezzo. Ora MATIC prende il prezzo di POL dal 4 settembre 2024 ed ETH2 quello di ETH. Un prezzo inserito a mano resta valido e ha la precedenza.

## Versione 1.0.65.12

**Nuove implementazioni**

- **Wallet Algorand (in prova)**: in *Gestione Wallet* c'è la rete *Algorand (ALGO)*. I movimenti si scaricano dall'indexer pubblico di Nodely, senza ApiKey, compresi gli swap su Tinyman e Pact e le ricompense di partecipazione.

- **Wallet XRP Ledger (in prova)**: in *Gestione Wallet* c'è la rete *Ripple (XRP)*, letta da un nodo pubblico di Ripple con tutta la storia, senza ApiKey: XRP, token, NFT, ordini sul DEX e AMM.

- **Wallet Tron (in prova)**: in *Gestione Wallet* c'è la rete *Tron (TRX)*, letta da TronGrid e TronScan senza ApiKey: TRX, token TRC20 e TRC10, commissioni e ricompense di voto. I token che TronScan riconosce come truffa arrivano già marcati SCAM.

- **Conti ether.fi Cash (in prova)**: in *Gestione Wallet* c'è la voce *ether.fi Cash (OP)* per il conto ether.fi. I movimenti si leggono dagli eventi di ether.fi su Optimism: ricariche, scambi fatti dall'app, spese con la carta, interessi. Le ricariche da un proprio wallet si abbinano da sole al prelievo da cui partono.

- **Prezzi DefiLlama su Optimism**: su Optimism vale anche un prezzo DefiLlama con affidabilità esattamente 0,9, per esempio quello di liquidRWA di ether.fi.

- **Token truffa marcati SCAM all'importazione dei wallet DeFi**: scaricando un wallet letto da un explorer, i token che sono truffe con certezza arrivano già marcati SCAM. Sono i finti trasferimenti in uscita di un token mai entrato nel wallet, i simboli che imitano una moneta nota con lettere di altri alfabeti e i nomi con un link. Il messaggio di fine importazione li elenca. Si tolgono da *Analisi Crypto – Gestione Token Scam – Rimuovi da SCAM*.

- **Novità delle versioni di prova**: da questa versione all'avvio di una beta si vedono solo le sue novità, come queste, e anche quelle delle beta saltate. Il pulsante *Tutte le novità della 1.0.66* mostra l'elenco completo della versione in preparazione.

- **Simboli in alfabeti che il font del programma non ha**: nelle tabelle questi nomi si leggono con i font del sistema invece di comparire come quadratini. Le emoji restano come prima.
