# Novità delle versioni di prova

Una sezione per ogni versione di prova (beta), con ciò che è cambiato rispetto alla beta precedente. La mostra il
programma all'avvio di una beta, ed è la descrizione della beta su GitHub. L'elenco completo delle novità della versione
in preparazione è nella sezione "in lavorazione" di docs/documentazione/changelog.md, dove ogni voce di questo file
compare con lo stesso titolo. Questo file non è una pagina del sito.

## Versione 1.0.66.02

**Nuove implementazioni**

- **Tab ISEE, il patrimonio per la DSU (in prova)**: nuovo tab *Dichiarazioni – ISEE* che prepara i rapporti del Quadro FC2 della DSU per le cripto-attività e la liquidità presso gli exchange, con il valore al 31/12 e gli avvisi di ognuno. Ci sono la giacenza media giornaliera, la gestione dei prezzi mancanti e la stampa di un prospetto PDF e di un Excel, archiviati come i quadri W/RW e T/RT. Il manuale si apre dal pulsante *Guida...* del tab.

- **Importazione di MiniPay e della carta MiniPay (rete Celo)**: in Gestione Wallet la nuova voce *MiniPay (CELO)* legge dai nodi pubblici di Celo, senza chiavi, i movimenti del wallet e del conto della carta, che importa come wallet a parte. Commissioni in USDT, ricariche fra wallet propri, spese con la carta e ricompense Boost arrivano già classificate, e alla fine i saldi si confrontano con quelli della blockchain.

- **Spese della carta MiniPay da CSV**: la nuova voce *MiniPay > Spese della carta (CSV)* della finestra di importazione legge la cronologia della carta e sostituisce il valore in euro delle spese già scaricate con quello realmente addebitato. Nelle note finiscono commerciante, città, categoria e spread, e prima di scrivere si vede un'anteprima con i totali.

- **Spese della carta ether.fi da CSV**: la voce *ether.fi > Spese della carta (CSV)* fa lo stesso per il conto ether.fi Cash, abbinando le spese per vicinanza di tempo e importo coerente. Se due pagamenti sono ugualmente plausibili non sceglie, e rileggere il file non duplica niente.

- **Classificazione dei prelievi, nuova voce «Spesa con carta»**: per un pagamento con carta saldato in cripto. Per il calcolo è un cashout, come prima, ma ha un nome suo e chiede il valore in euro della spesa. I pagamenti importati da ether.fi, OKX X Layer e MiniPay arrivano già con questo nome.

- **Classificazione dei depositi, nuova voce «Annullo spesa carta»**: per il rimborso di un pagamento con la carta. Il pagamento resta un cashout, il token riaccreditato entra come un acquisto a costo di carico pari al valore del pagamento, senza plusvalenza, anche se arriva in un token diverso.

- **Menu del tasto destro sui movimenti, voci raggruppate**: in alto restano *Dettagli*, *Modifica*, *Classifica* ed *Elimina Movimento*, le altre voci sono raggruppate nei sottomenu *Analizza*, *Prezzi*, *Separa e unisci* e *Altre modifiche*. Un sottomenu senza voci utilizzabili è grigio, e le voci che non servono nel punto in cui si apre il menu non compaiono più.

- **Menu del tasto destro anche nelle altre finestre**: lo stesso menu si apre ora nella finestra del LiFo, nei movimenti di un rimborso in *Fallimenti piattaforme*, nel dettaglio del movimento e nella classificazione, e il doppio clic su un movimento apre il suo dettaglio. Dopo una modifica la finestra si aggiorna da sola, e dove una modifica non è prudente le voci di modifica non ci sono.

- **Più documenti su un movimento, e file di dettaglio Binance fra i documenti di origine**: un movimento può essere collegato, oltre al file da cui nasce, ad altri documenti che lo spiegano. I file di dettaglio di Dual Investment e Auto-Invest si conservano ora fra i documenti, e la gestione documenti ha la nuova colonna *Collegati*. Eliminando un documento solo collegato i movimenti restano.

**Correzione di bug**

- **Importazione dall'app Crypto.com, acquisti ricorrenti, reward del lockup CRO e delisting dal basket**: gli acquisti ricorrenti (*Recurring Buy*) degli export recenti diventano acquisti come gli altri, invece di lasciare euro in più e cripto mancanti. Entrano anche le reward del lockup CRO dei titolari di carta e il delisting di un token dal Crypto Basket. Reimportando un file già letto si aggiungono solo i movimenti mancanti.

- **Importazione da CSV generico, numeri con la virgola decimale**: un numero come `12,5` scritto senza virgolette in un CSV separato da virgole entrava con la quantità sbagliata, e uno come `1.234,56` faceva sparire la riga. Ora queste righe finiscono fra le scartate, con il motivo che spiega il problema.

- **Importazione, lentezza con molte righe scartate**: un file grande letto con la configurazione sbagliata, in cui ogni riga viene scartata, teneva fermo l'import a lungo. Ora il tempo cresce in proporzione alle righe: 40.000 righe scartate passano da 13 secondi a meno di mezzo.

- **Binance dalle API, trade spot più vecchi di due anni**: Binance restituisce via API solo i trade spot degli ultimi due anni circa, e prima i più vecchi si perdevano senza avviso. Ora a fine scaricamento il programma segnala la data da cui mancano, e per il periodo precedente gli scambi vanno importati dal CSV della cronologia delle transazioni.

- **Binance dalle API, pagamenti Binance Pay**: dei pagamenti Binance Pay si importavano solo i "Tax Payment", gli altri lasciavano sbagliata la giacenza. Ora entrano come depositi e prelievi di cripto da classificare. Binance li restituisce solo per gli ultimi 18 mesi, e se lo scaricamento parte da prima l'avviso dice da quando mancano.

## Versione 1.0.66.01

**Nuove implementazioni**

- **Microsaldi in giallo in Giacenze a data**: la nuova spunta *Evidenzia microsaldi* colora in giallo le quantità minime, di solito residui e non giacenze vere. Chiede le soglie in euro e in quantità, non colora i saldi confermati dalle API e non cambia nessun calcolo.

- **RT e Analisi P&L, dettagli per gruppo wallet**: nuova scheda *Dettagli x Gruppo Wallet* con vendite, costi, plusvalenze e rimanenze di ogni gruppo. Con il LiFo globale i totali dell'anno non cambiano: il realizzato va al gruppo del wallet del movimento, rimanenze e latente sono ripartite in proporzione alla quantità del gruppo. Nell'Excel T/RT c'è il foglio *Gruppi wallet* e l'alias del gruppo, e *Movimenti rilevanti* mostra gruppo e alias.

- **Filtri di colonna, pulsante Seleziona filtrati**: dopo una ricerca nel popup di filtro spunta tutte e sole le voci che compaiono nell'elenco.

**Correzione di bug**

- **Importazione da Coinbase, addebiti (Debit)**: le righe "Debit" non vengono più scartate come causale sconosciuta, diventano prelievi di cripto da classificare a mano.

- **Importazione da CSV generico, riga di testa in più**: una riga di testa in più senza cifre non compare più fra le scartate come «DATA NON PARSABILE».

- **Movimenti senza prezzo e LiFo mancante, errori fuori periodo**: un prezzo scelto dalla tabella dei prezzi, anche zero, ora toglie il movimento dall'errore. Il dialogo *Errori* dice quanti movimenti stanno fuori dal periodo e propone di allargare le date per vederli.

- **Dettagli x Tipologia, ordinamento delle colonne numeriche**: nell'elenco dei movimenti quantità, corrispettivo, costo di carico e plusvalenza si ordinano ora per valore e non come testo.

## Versione 1.0.65.18

**Nuove implementazioni**

- **Ethereum senza ApiKey Etherscan (in prova)**: se nelle opzioni non c'è nessuna chiave Etherscan, i wallet su Ethereum si scaricano ora dai nodi pubblici della rete, come già per Base, e non serve più creare un account. Chi ha già la chiave resta su Etherscan. Un token ricevuto senza fare nulla e mai spostato si recupera con la ricerca di token indicati.

- **ether.fi Cash, riscatti in due tempi e swap verso altre catene (in prova)**: il riscatto di quote Midas e il prelievo dalla coda di EtherFi Liquid, che pagano dopo ore o giorni, si uniscono da soli in uno scambio fra le due monete, grazie al numero di richiesta che richiesta e pagamento hanno in comune. Uno swap verso un'altra catena esce come prelievo, e si abbina all'entrata se si inserisce come wallet su Ethereum il TradingSafe di ether.fi.

- **Giacenze a data, costo medio netto**: le cinque colonne dei costi netti non sono più nella tabella. Il pulsante *Costi netti*, attivo solo con il wallet *Tutti*, apre un dialogo con una riga per token, compresi quelli a giacenza zero, dove il risultato è il profitto o la perdita realizzati.

- **Emoji nei nomi dei token**: un token con un'emoji nel nome non compare più come un quadratino nelle tabelle, nelle etichette, nelle liste e nei messaggi.

**Correzione di bug**

- **Somme nelle intestazioni delle colonne**: in *Giacenze a Data* la somma ora compare anche sulla tabella principale. Non si sommano più quantità di token diversi né le quantità residue, la somma di quantità piccole non si arrotonda più a due decimali e la riga della somma non viene più tagliata nelle finestre con l'intestazione bassa.

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
