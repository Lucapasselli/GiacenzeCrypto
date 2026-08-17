---
layout: default
title: Informativa sulla privacy - Giacenze Crypto
---

# Informativa sulla privacy

**Applicazione:** Giacenze Crypto (applicazione desktop per Windows, Linux e macOS)
**Ultimo aggiornamento:** 17 agosto 2026
**Contatto:** giacenzecrypto@gmail.com

*[English version below](#privacy-policy-english)*

## In breve

Giacenze Crypto **non raccoglie i tuoi dati**. Non c'è registrazione, non c'è un account, non c'è un
server dell'applicazione. Tutto quello che inserisci o importi resta sul tuo computer, nella cartella
di lavoro che hai scelto. **Chi sviluppa l'applicazione non riceve, non vede e non conserva nessuno
dei tuoi dati.**

## 1. Chi tratta i dati

L'applicazione è sviluppata e distribuita da un singolo sviluppatore, contattabile all'indirizzo
giacenzecrypto@gmail.com. Poiché nessun dato dell'utente viene trasmesso allo sviluppatore, lo
sviluppatore non tratta dati personali degli utenti dell'applicazione.

## 2. Quali dati restano sul tuo computer

L'applicazione salva nella cartella di lavoro, in database e file locali:

- i movimenti in criptovaluta importati o inseriti a mano (data, piattaforma, monete, quantità,
  eventuali hash di transazione e indirizzi);
- gli indirizzi pubblici dei wallet in blockchain che hai aggiunto;
- le quotazioni scaricate dai fornitori di dati, conservate come archivio storico perché un calcolo
  fiscale deve restare riproducibile negli anni;
- le tue preferenze, i gruppi di wallet e le impostazioni di calcolo;
- una copia compressa dei file che hai importato, nella cartella dei documenti di origine;
- eventuali chiavi API dei servizi che decidi di usare (vedi il punto 5).

Questi dati **non lasciano il computer** se non nei casi elencati al punto 3, e **non vengono mai
inviati allo sviluppatore**.

## 3. Cosa esce dal computer, e verso chi

L'applicazione effettua chiamate di rete solo verso servizi di terze parti, e solo per fare quello
che le hai chiesto. In particolare:

| Destinatario | Cosa gli viene inviato | Perché |
|---|---|---|
| Explorer di blockchain (Etherscan e i suoi equivalenti per le altre catene, Blockscout, Moralis, Helius, Unisat, mempool.space) | **l'indirizzo pubblico del tuo wallet** e la chiave API che hai eventualmente inserito | leggere le transazioni di quel wallet |
| CoinGecko, CoinMarketCap, DefiLlama, Binance, Coinbase Exchange e altri listini pubblici | il simbolo o l'indirizzo del contratto di un token e un intervallo di date | recuperare le quotazioni storiche |
| GoPlus Labs | l'indirizzo del contratto di un token | verificare se il token è segnalato come pericoloso |
| Banca d'Italia | un intervallo di date | il cambio ufficiale EUR/USD usato nei calcoli |
| GitHub | nessun dato tuo | controllare se esiste una versione più recente e aggiornare i file di configurazione distribuiti |
| Il tuo exchange (solo nelle edizioni che offrono questa funzione) | le richieste firmate con **le chiavi API che hai inserito tu** | scaricare i tuoi movimenti da quel conto |

L'indirizzo pubblico di un wallet è, per sua natura, l'informazione che l'explorer deve ricevere per
poterne restituire le transazioni: è la stessa cosa che accade quando lo cerchi con il browser sul
sito dell'explorer. Nessuna di queste chiamate trasmette il tuo archivio dei movimenti.

Come per qualunque programma che si collega a Internet, ogni richiesta rende necessariamente visibile
al destinatario il tuo indirizzo IP: è il funzionamento della rete, non una raccolta di dati da parte
dell'applicazione. Se una funzione non ti serve, non usarla: nessuna di queste chiamate parte da sola
oltre al controllo della versione e all'aggiornamento delle configurazioni.

L'applicazione **non contiene pubblicità, sistemi di analisi statistica, telemetria o profilazione**,
e non invia segnalazioni di errore automatiche.

## 4. La funzione "Chiedi a IA"

Se scegli di usarla, l'applicazione prepara una domanda su un singolo movimento e la apre nel tuo
browser sul sito del chatbot che hai scelto. Il testo **ti viene sempre mostrato prima**, puoi
modificarlo, e non parte niente finché non premi tu il pulsante. Sono disponibili due livelli di
dettaglio, e quello ridotto non include importi, date, hash di transazione né indirizzi. Quello che
succede al testo una volta arrivato al chatbot è regolato dalle condizioni di quel servizio, non da
questa informativa.

## 5. Chiavi API e credenziali

Alcune funzioni facoltative richiedono una chiave API che ottieni tu dal servizio interessato
(explorer di blockchain, fornitori di quotazioni e, nelle edizioni che la offrono, il tuo exchange).
Le chiavi vengono salvate **nel database locale sul tuo computer**, non sono cifrate e non vengono
inviate a nessuno tranne che al servizio a cui appartengono. Trattale come tratteresti qualunque
altro file riservato sul tuo disco, e preferisci chiavi di sola lettura dove il servizio lo consente.

Gli archivi di backup creati dall'applicazione **escludono le chiavi** salvo tua richiesta esplicita.

**L'applicazione non chiede, non gestisce e non conserva seed phrase o chiavi private dei wallet**, in
nessuna forma e in nessun file. Dei wallet in blockchain le serve soltanto l'indirizzo pubblico.

⚠️ **L'edizione distribuita sul Microsoft Store non accede ad alcun conto di exchange**: i movimenti
si importano dai file esportati dalle piattaforme e dagli indirizzi pubblici dei wallet.

## 6. Cancellazione dei dati

Poiché i dati stanno solo sul tuo computer, per cancellarli è sufficiente eliminare la cartella di
lavoro dell'applicazione (o disinstallare l'applicazione e rimuovere quella cartella). Non c'è nessuna
copia altrove da richiedere o da far cancellare.

## 7. Minori

L'applicazione non è destinata a minori di 13 anni e non raccoglie consapevolmente dati che li
riguardino.

## 8. Modifiche a questa informativa

Eventuali modifiche vengono pubblicate su questa pagina, con la data di aggiornamento in cima. Le
versioni precedenti restano consultabili nella cronologia del repository pubblico del progetto.

---

<a id="privacy-policy-english"></a>

# Privacy Policy (English)

**Application:** Giacenze Crypto (desktop application for Windows, Linux and macOS)
**Last updated:** 17 August 2026
**Contact:** giacenzecrypto@gmail.com

## Summary

Giacenze Crypto **does not collect your data**. There is no sign-up, no account and no application
server. Everything you enter or import stays on your computer, in the working folder you choose.
**The developer does not receive, see or store any of your data.**

## 1. Data controller

The application is developed and distributed by an individual developer, reachable at
giacenzecrypto@gmail.com. Since no user data is ever transmitted to the developer, the developer does
not process personal data of the application's users.

## 2. Data stored on your computer

The application stores in your working folder, in local databases and files: your crypto movements
(date, platform, coins, amounts, transaction hashes and addresses where applicable); the public
addresses of the blockchain wallets you added; market prices downloaded from data providers, kept as a
historical archive because a tax calculation must remain reproducible over the years; your preferences
and calculation settings; a compressed copy of the files you imported; and any API keys for the
services you choose to use (see section 5).

This data **does not leave your computer** except in the cases listed in section 3, and is **never
sent to the developer**.

## 3. What leaves your computer, and to whom

The application makes network requests only to third-party services, and only to do what you asked it
to do:

| Recipient | What is sent | Why |
|---|---|---|
| Blockchain explorers (Etherscan and its equivalents for other chains, Blockscout, Moralis, Helius, Unisat, mempool.space) | **your wallet's public address** and the API key you may have entered | to read that wallet's transactions |
| CoinGecko, CoinMarketCap, DefiLlama, Binance, Coinbase Exchange and other public market data sources | a token symbol or contract address and a date range | to retrieve historical prices |
| GoPlus Labs | a token contract address | to check whether the token is flagged as malicious |
| Banca d'Italia (Bank of Italy) | a date range | the official EUR/USD rate used in the calculations |
| GitHub | none of your data | to check for a newer version and to update the distributed configuration files |
| Your exchange (only in editions that offer this feature) | requests signed with **the API keys you entered yourself** | to download your own movements from that account |

A wallet's public address is by nature the information an explorer must receive in order to return its
transactions: the same thing happens when you look it up in a browser on the explorer's website. None
of these requests transmits your movement archive.

As with any program that connects to the Internet, each request necessarily makes your IP address
visible to the recipient: that is how the network works, not data collection by the application. If
you do not need a feature, do not use it: none of these requests happens on its own, apart from the
version check and the configuration update.

The application contains **no advertising, analytics, telemetry or profiling**, and sends no automatic
crash reports.

## 4. The "Ask an AI" feature

If you choose to use it, the application prepares a question about a single movement and opens it in
your browser on the chatbot website you selected. The text is **always shown to you first**, you can
edit it, and nothing is sent until you press the button yourself. Two levels of detail are available,
and the reduced one includes no amounts, dates, transaction hashes or addresses. What happens to the
text once it reaches the chatbot is governed by that service's terms, not by this policy.

## 5. API keys and credentials

Some optional features require an API key that you obtain yourself from the service concerned. Keys
are stored **in the local database on your computer**, are not encrypted, and are sent to no one
except the service they belong to. Treat them as you would any other confidential file on your disk,
and prefer read-only keys where the service allows it. Backup archives created by the application
**exclude the keys** unless you explicitly ask for them.

**The application never asks for, handles or stores wallet seed phrases or private keys**, in any form
or file. For blockchain wallets it only needs the public address.

⚠️ **The edition distributed on the Microsoft Store does not access any exchange account**: movements
are imported from files exported by the platforms and from public wallet addresses.

## 6. Deleting your data

Because the data exists only on your computer, deleting the application's working folder (or
uninstalling the application and removing that folder) deletes it all. There is no copy elsewhere to
request or to have erased.

## 7. Children

The application is not intended for children under 13 and does not knowingly collect data about them.

## 8. Changes to this policy

Any changes are published on this page, with the update date at the top. Previous versions remain
available in the history of the project's public repository.
