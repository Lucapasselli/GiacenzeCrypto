# Novità delle versioni di prova

Una sezione per ogni versione di prova (beta), con ciò che è cambiato rispetto alla beta precedente. La mostra il
programma all'avvio di una beta, ed è la descrizione della beta su GitHub. L'elenco completo delle novità della versione
in preparazione è nella sezione "in lavorazione" di docs/documentazione/changelog.md, dove ogni voce di questo file
compare con lo stesso titolo. Questo file non è una pagina del sito.

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
