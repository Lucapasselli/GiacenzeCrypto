---
layout: default
title: Informativa sulla privacy - Giacenze Crypto
---

# Informativa sulla privacy

**Applicazione:** Giacenze Crypto (applicazione desktop per Windows, Linux e macOS)
**Ultimo aggiornamento:** 25 ottobre 2026
**Contatto:** giacenzecrypto@gmail.com

*[English version below](#privacy-policy-english)*

## In breve

Giacenze Crypto **non raccoglie i tuoi dati**. Non c'è registrazione e non c'è un account. Tutto quello
che inserisci o importi resta sul tuo computer, nella cartella di lavoro che hai scelto. **Chi sviluppa
l'applicazione non riceve, non vede e non conserva i tuoi dati**, salvo quello che decidi tu di
mandargli con una funzione apposita: una segnalazione di errore, l'invio dei log o la condivisione di
una configurazione di importazione (punti 5 e 6).

## 1. Chi tratta i dati

L'applicazione è sviluppata e distribuita da un singolo sviluppatore, contattabile all'indirizzo
giacenzecrypto@gmail.com. Lo sviluppatore tratta dati degli utenti solo quando sono loro a inviarglieli
con una funzione apposita, e solo per lo scopo di quella funzione (punti 5 e 6).

## 2. Quali dati restano sul tuo computer

L'applicazione salva nella cartella di lavoro, in database e file locali:

- i movimenti in criptovaluta importati o inseriti a mano (data, piattaforma, monete, quantità,
  eventuali hash di transazione e indirizzi);
- gli indirizzi pubblici dei wallet in blockchain che hai aggiunto;
- le quotazioni scaricate dai fornitori di dati, conservate come archivio storico perché un calcolo
  fiscale deve restare riproducibile negli anni;
- le tue preferenze, i gruppi di wallet e le impostazioni di calcolo;
- una copia compressa dei file che hai importato, nella cartella dei documenti di origine;
- eventuali chiavi API dei servizi che decidi di usare (vedi il punto 7).

Questi dati **non lasciano il computer** se non nei casi elencati ai punti 3, 5 e 6.

## 3. Cosa esce dal computer, e verso chi

L'applicazione effettua chiamate di rete solo verso servizi di terze parti, e solo per fare quello
che le hai chiesto. In particolare:

| Destinatario | Cosa gli viene inviato | Perché |
|---|---|---|
| Explorer di blockchain (Etherscan e i suoi equivalenti per le altre catene, Blockscout, Moralis, Helius, Unisat, mempool.space) | **l'indirizzo pubblico del tuo wallet** e la chiave API che hai eventualmente inserito | leggere le transazioni di quel wallet |
| CoinGecko, CoinMarketCap, DefiLlama, Binance, Coinbase Exchange e altri listini pubblici | il simbolo o l'indirizzo del contratto di un token e un intervallo di date | recuperare le quotazioni storiche |
| GoPlus Labs | l'indirizzo del contratto di un token | verificare se il token è segnalato come pericoloso |
| Banca d'Italia | un intervallo di date | il cambio ufficiale EUR/USD usato nei calcoli |
| GitHub | nessun dato tuo (solo il numero della versione in uso) | controllare se esiste una versione più recente, scaricarla quando chiedi di aggiornare, e aggiornare i file di configurazione distribuiti |
| Il tuo exchange (solo nelle edizioni che offrono questa funzione) | le richieste firmate con **le chiavi API che hai inserito tu** | scaricare i tuoi movimenti da quel conto |
| giacenzecrypto.it, servizio prezzi dello sviluppatore (solo se lo attivi nelle *Opzioni*, è spento di default) | il simbolo di una moneta e un istante | recuperare quotazioni da un archivio condiviso, prima di cercarle altrove |
| giacenzecrypto.it, catalogo della community | per scaricare il catalogo e le configurazioni, nessun dato tuo. Se condividi una configurazione, quello descritto al punto 5 | scaricare e condividere configurazioni di importazione |
| giacenzecrypto.it, segnalazioni | solo se le invii tu, quello descritto al punto 6 | far arrivare allo sviluppatore un errore o un log |

L'indirizzo pubblico di un wallet è, per sua natura, l'informazione che l'explorer deve ricevere per
poterne restituire le transazioni: è la stessa cosa che accade quando lo cerchi con il browser sul
sito dell'explorer. Nessuna di queste chiamate trasmette il tuo archivio dei movimenti.

Come per qualunque programma che si collega a Internet, ogni richiesta rende necessariamente visibile
al destinatario il tuo indirizzo IP: è il funzionamento della rete, non una raccolta di dati da parte
dell'applicazione. Il server di giacenzecrypto.it registra ogni richiesta con l'indirizzo IP accorciato,
senza la parte che identifica il singolo collegamento (per esempio 203.0.113.0 al posto di 203.0.113.57), e
conserva questo registro 30 giorni, per sicurezza e per statistiche di traffico aggregate. Se una funzione non ti serve, non usarla: nessuna di queste chiamate parte da sola
oltre al controllo della versione e all'aggiornamento delle configurazioni. Il controllo della versione si
può disattivare dalla finestra *Informazioni* (clic sul titolo), pulsante *Aggiornamenti...*. Una nuova
versione viene scaricata e installata solo dopo che hai confermato di volerla.

L'applicazione **non contiene pubblicità, sistemi di analisi statistica, telemetria o profilazione**,
e non invia segnalazioni di errore automatiche.

## 4. La funzione "Chiedi a IA"

Se scegli di usarla, l'applicazione prepara una domanda su un singolo movimento e la apre nel tuo
browser sul sito del chatbot che hai scelto. Il testo **ti viene sempre mostrato prima**, puoi
modificarlo, e non parte niente finché non premi tu il pulsante. Sono disponibili due livelli di
dettaglio, e quello ridotto non include importi, date, hash di transazione né indirizzi. Quello che
succede al testo una volta arrivato al chatbot è regolato dalle condizioni di quel servizio, non da
questa informativa.

## 5. Condividere una configurazione di importazione

Dalla versione 1.0.66 puoi proporre al catalogo della community una configurazione di importazione
scritta da te, dalla finestra della prova (*Condividi con la community*). Solo se lo chiedi tu, e
dopo averlo visto, viene inviato al servizio dello sviluppatore su giacenzecrypto.it:

- Il file JSON della configurazione, così com'è.
- L'esito della prova: il numero di righe lette e di movimenti prodotti e, per ogni causale del file,
  il tipo di movimento scelto e i movimenti che ne escono. **Nessuna riga del tuo file viene inviata.**
- La versione del programma e, se lo indichi, un nome visibile.

Il servizio conserva anche un'impronta non reversibile (hash) del tuo indirizzo IP, al solo scopo di
limitare il numero di invii, e la data. Il programma rifiuta un file che sembra contenere indirizzi di
wallet, email o IBAN, ma resta a te controllare che non contenga altri dati personali.

La configurazione diventa pubblica solo se lo sviluppatore la approva, con licenza MIT e con il nome
visibile, se l'hai indicato. Puoi ritirarla in ogni momento dalla finestra *Configurazioni della
community* del programma, che conserva sul tuo computer il codice per farlo, oppure scrivendo
all'indirizzo di contatto. Scaricare il catalogo e le configurazioni non invia nessun tuo dato.

Lo sviluppatore esamina le proposte anche tramite un bot Telegram privato: il file e l'esito della prova
transitano quindi anche dai server di Telegram.

## 6. Segnalazioni di errore e invio dei log

Due funzioni permettono di far arrivare allo sviluppatore un problema. Partono solo se le usi tu, dopo
una conferma, e mai da sole.

- **Invia segnalazione errori**, nel resoconto di un'importazione: invia le righe dei movimenti non
  riconosciuti mostrate nella finestra, **così come sono nel tuo file**, insieme al tipo di importazione,
  al nome del file, al numero di righe importate e scartate e alla versione del programma, del sistema
  operativo e di Java. Le righe possono contenere dati del movimento, come importi, date e identificativi:
  sono quelle che vedi prima di confermare.
- **Invia log**, in *Opzioni*: invia un riepilogo diagnostico oppure il file di log del programma.
  Prima dell'invio il programma toglie le forme note di dati identificativi (indirizzi di wallet, hash di
  transazione, il percorso della tua cartella personale, chiavi passate agli script, email, IBAN) e ti
  mostra il testo in un riquadro **che puoi modificare**: parte esattamente il testo come lo lasci. La
  rimozione automatica non è una garanzia, quindi controlla il testo prima di inviarlo.

Il servizio conserva ogni segnalazione **al massimo 30 giorni**, poi la cancella automaticamente, insieme
a un'impronta non reversibile (hash) del tuo indirizzo IP, al solo scopo di limitare il numero di invii.
Le segnalazioni servono solo a trovare e correggere errori del programma. Lo sviluppatore le consulta
anche tramite un bot Telegram privato, quindi il loro contenuto transita anche dai server di Telegram.

## 7. Chiavi API e credenziali

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

## 8. Cancellazione dei dati

Poiché i dati stanno solo sul tuo computer, per cancellarli è sufficiente eliminare la cartella di
lavoro dell'applicazione (o disinstallare l'applicazione e rimuovere quella cartella). Non c'è nessuna
copia altrove da richiedere o da far cancellare, salvo quello che hai inviato tu (punti 5 e 6): le
segnalazioni si cancellano da sole dopo 30 giorni, e per far cancellare prima una segnalazione o una
configurazione condivisa basta scrivere all'indirizzo di contatto.

## 9. Minori

L'applicazione non è destinata a minori di 13 anni e non raccoglie consapevolmente dati che li
riguardino.

## 10. Modifiche a questa informativa

Eventuali modifiche vengono pubblicate su questa pagina, con la data di aggiornamento in cima. Le
versioni precedenti restano consultabili nella cronologia del repository pubblico del progetto.

---

<a id="privacy-policy-english"></a>

# Privacy Policy (English)

**Application:** Giacenze Crypto (desktop application for Windows, Linux and macOS)
**Last updated:** 25 October 2026
**Contact:** giacenzecrypto@gmail.com

## Summary

Giacenze Crypto **does not collect your data**. There is no sign-up and no account. Everything you enter
or import stays on your computer, in the working folder you choose. **The developer does not receive, see
or store your data**, except what you decide to send through a dedicated feature: an error report, a log
or a shared import configuration (sections 5 and 6).

## 1. Data controller

The application is developed and distributed by an individual developer, reachable at
giacenzecrypto@gmail.com. The developer processes users' data only when users send it themselves through
a dedicated feature, and only for the purpose of that feature (sections 5 and 6).

## 2. Data stored on your computer

The application stores in your working folder, in local databases and files: your crypto movements
(date, platform, coins, amounts, transaction hashes and addresses where applicable); the public
addresses of the blockchain wallets you added; market prices downloaded from data providers, kept as a
historical archive because a tax calculation must remain reproducible over the years; your preferences
and calculation settings; a compressed copy of the files you imported; and any API keys for the
services you choose to use (see section 7).

This data **does not leave your computer** except in the cases listed in sections 3, 5 and 6.

## 3. What leaves your computer, and to whom

The application makes network requests only to third-party services, and only to do what you asked it
to do:

| Recipient | What is sent | Why |
|---|---|---|
| Blockchain explorers (Etherscan and its equivalents for other chains, Blockscout, Moralis, Helius, Unisat, mempool.space) | **your wallet's public address** and the API key you may have entered | to read that wallet's transactions |
| CoinGecko, CoinMarketCap, DefiLlama, Binance, Coinbase Exchange and other public market data sources | a token symbol or contract address and a date range | to retrieve historical prices |
| GoPlus Labs | a token contract address | to check whether the token is flagged as malicious |
| Banca d'Italia (Bank of Italy) | a date range | the official EUR/USD rate used in the calculations |
| GitHub | none of your data (only the version number in use) | to check for a newer version, to download it when you ask to update, and to update the distributed configuration files |
| Your exchange (only in editions that offer this feature) | requests signed with **the API keys you entered yourself** | to download your own movements from that account |
| giacenzecrypto.it, the developer's price service (only if you turn it on in *Opzioni*, it is off by default) | a coin symbol and a point in time | to retrieve prices from a shared archive before looking elsewhere |
| giacenzecrypto.it, community catalogue | none of your data to download the catalogue and the configurations. If you share a configuration, what section 5 describes | to download and share import configurations |
| giacenzecrypto.it, reports | only if you send them, what section 6 describes | to let the developer see an error or a log |

A wallet's public address is by nature the information an explorer must receive in order to return its
transactions: the same thing happens when you look it up in a browser on the explorer's website. None
of these requests transmits your movement archive.

As with any program that connects to the Internet, each request necessarily makes your IP address
visible to the recipient: that is how the network works, not data collection by the application. The
giacenzecrypto.it server logs each request with a shortened IP address, without the part that identifies the
single connection (for example 203.0.113.0 instead of 203.0.113.57), and keeps this log for 30 days, for security and aggregate traffic
statistics. If you do not need a feature, do not use it: none of these requests happens on its own, apart from the
version check and the configuration update. The version check can be turned off from the *Informazioni*
window (click the title), *Aggiornamenti...* button. A new version is downloaded and installed only after
you confirm you want it.

The application contains **no advertising, analytics, telemetry or profiling**, and sends no automatic
crash reports.

## 4. The "Ask an AI" feature

If you choose to use it, the application prepares a question about a single movement and opens it in
your browser on the chatbot website you selected. The text is **always shown to you first**, you can
edit it, and nothing is sent until you press the button yourself. Two levels of detail are available,
and the reduced one includes no amounts, dates, transaction hashes or addresses. What happens to the
text once it reaches the chatbot is governed by that service's terms, not by this policy.

## 5. Sharing an import configuration

From version 1.0.66 you can submit an import configuration you wrote to the community catalogue, from
the test window (*Condividi con la community*). Only if you ask, and after you have seen it, the
following is sent to the developer's service at giacenzecrypto.it: the configuration's JSON file as it
is; the test result (number of rows read and movements produced and, for each transaction type in the
file, the movement type chosen and the movements it produces, **no row of your file**); the application
version and, if you enter one, a display name.

The service also keeps a non-reversible fingerprint (hash) of your IP address, only to limit the number
of submissions, and the date. The application rejects a file that looks like it contains wallet
addresses, e-mail addresses or IBANs, but it is up to you to check it contains no other personal data.

The configuration becomes public only if the developer approves it, under the MIT licence and with the
display name if you entered one. You can withdraw it at any time from the *Configurazioni della
community* window of the application, which keeps the code to do so on your computer, or by writing to
the contact address. Downloading the catalogue and the configurations sends none of your data.

The developer also reviews submissions through a private Telegram bot, so the file and the test result
also pass through Telegram's servers.

## 6. Error reports and logs

Two features let you send a problem to the developer. They run only when you use them, after a
confirmation, and never on their own.

- **Invia segnalazione errori**, in an import report: sends the rows of unrecognised movements shown in
  the window, **exactly as they are in your file**, with the import type, the file name, the number of
  imported and rejected rows and the application, operating system and Java versions. The rows may
  contain movement data such as amounts, dates and identifiers: they are the ones you see before
  confirming.
- **Invia log**, in *Opzioni*: sends a diagnostic summary or the application's log file. Before sending,
  the application removes known forms of identifying data (wallet addresses, transaction hashes, the path
  of your home folder, keys passed to scripts, e-mail addresses, IBANs) and shows you the text in a box
  **you can edit**: exactly the text as you leave it is sent. Automatic removal is not a guarantee, so
  check the text before sending it.

The service keeps each report **for at most 30 days**, then deletes it automatically, together with a
non-reversible fingerprint (hash) of your IP address, only to limit the number of submissions. Reports
are used only to find and fix errors in the application. The developer also reads them through a private
Telegram bot, so their content also passes through Telegram's servers.

## 7. API keys and credentials

Some optional features require an API key that you obtain yourself from the service concerned. Keys
are stored **in the local database on your computer**, are not encrypted, and are sent to no one
except the service they belong to. Treat them as you would any other confidential file on your disk,
and prefer read-only keys where the service allows it. Backup archives created by the application
**exclude the keys** unless you explicitly ask for them.

**The application never asks for, handles or stores wallet seed phrases or private keys**, in any form
or file. For blockchain wallets it only needs the public address.

⚠️ **The edition distributed on the Microsoft Store does not access any exchange account**: movements
are imported from files exported by the platforms and from public wallet addresses.

## 8. Deleting your data

Because the data exists only on your computer, deleting the application's working folder (or
uninstalling the application and removing that folder) deletes it all. There is no copy elsewhere to
request or to have erased, except what you sent yourself (sections 5 and 6): reports delete themselves
after 30 days, and to have a report or a shared configuration deleted sooner just write to the contact
address.

## 9. Children

The application is not intended for children under 13 and does not knowingly collect data about them.

## 10. Changes to this policy

Any changes are published on this page, with the update date at the top. Previous versions remain
available in the history of the project's public repository.
