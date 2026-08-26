# Giacenze Crypto

**Giacenze Crypto** è un programma gratuito per Windows, Linux e macOS che aiuta chi possiede criptovalute a preparare i dati per la **dichiarazione dei redditi in Italia**.

Importa automaticamente i movimenti da exchange e wallet, recupera le quotazioni storiche e calcola:

- **Quadro RT** — le plusvalenze e minusvalenze da vendite/scambi di cripto-attività;
- **Quadro RW** — il valore delle disponibilità detenute all'estero (wallet e conti su exchange) al 31/12 di ogni anno.

Serve a ridurre il lavoro manuale di raccolta ed elaborazione dei dati che di solito precede la dichiarazione: **non sostituisce** la consulenza di un commercialista.

## Sito web

👉 **[giacenzecrypto.it](https://giacenzecrypto.it)** — presentazione del programma e un motore di ricerca sulla normativa fiscale italiana in materia di cripto-attività (leggi, prassi dell'Agenzia delle entrate, istruzioni ai modelli dichiarativi).

## Come installarlo

Non serve installare Java separatamente: tutti i pacchetti elencati qui sotto lo includono già.
Tutti si scaricano dalla pagina delle **[release su GitHub](https://github.com/Lucapasselli/GiacenzeCrypto/releases/latest)** (in alternativa, gli stessi file sono anche su [SourceForge](https://sourceforge.net/projects/giacenze-crypto-com/)).

### Windows

- **Consigliato** — scarica il file `..._Installer_Windows.exe`, avvialo e segui la procedura guidata: crea l'icona sul desktop e nel menu Start come qualunque altro programma. Non servono i permessi di amministratore.
- **Versione portatile** (senza installazione, ad esempio per tenerla su una chiavetta USB) — scarica `..._Portable_Windows.zip`, estrai la cartella e avvia `Giacenze_Crypto.exe` al suo interno.

### Linux

- **Debian, Ubuntu e derivate** — scarica il file `..._Deb_Linux.deb` e installalo con il gestore pacchetti del sistema (doppio clic, oppure da terminale: `sudo dpkg -i GiacenzeCrypto_*_Deb_Linux.deb`).
- **Flatpak** (qualunque distribuzione) — scarica il file `.flatpak` e installalo con `flatpak install --user GiacenzeCrypto_*.flatpak`.
- **Versione portatile**, senza installazione — scarica `..._Portable_Linux.zip`, estrai la cartella e avvia `Giacenze_Crypto` al suo interno.

### macOS

Scarica il file `.dmg` per il tuo Mac — `..._MacOS_AppleSilicon.dmg` per i Mac con chip Apple (M1/M2/M3/…), `..._MacOS_Intel.dmg` per i Mac più vecchi con processore Intel — aprilo e trascina l'icona nella cartella Applicazioni.

### Un'alternativa per chi ha già Java installato

Se preferisci non usare nessuno dei pacchetti sopra, esiste anche un pacchetto `..._Portable_Multipiattaforma.zip`: funziona su qualsiasi sistema operativo, ma **richiede Java 21 (o più recente) già installato** sul computer — a differenza di tutti i pacchetti elencati sopra, che non ne hanno bisogno. Si estrae la cartella e si avvia con `java -jar Giacenze_Crypto.jar` da terminale.

## Come aggiornarlo

Il programma stesso avvisa quando è disponibile una nuova versione, con un pulsante ben visibile che porta direttamente alla pagina di scaricamento — non serve controllare manualmente.

- **Installer Windows**: scarica ed esegui il nuovo installer come la prima volta; sostituisce automaticamente la versione precedente e i tuoi dati restano intatti.
- **.deb**: installa il nuovo file allo stesso modo del precedente; anche qui i dati non vengono toccati.
- **macOS**: scarica il nuovo `.dmg` e trascina di nuovo l'icona in Applicazioni, sovrascrivendo quella precedente. (Siccome l'app non è firmata può richiedere l'esecuzione di questo comando "xattr -cr /Applications/Giacenze_Crypto.app" affinchè l'app parta)
- **Versioni portatili** (Windows/Linux): scarica la nuova cartella al posto della vecchia. Per sicurezza, prima di farlo conviene usare la funzione di **backup integrata nel programma** (menu Opzioni) per salvare una copia dei propri dati, così è sempre possibile ripristinarli dopo l'aggiornamento.

## Dati e privacy

L'applicazione lavora **in locale**: i movimenti, i wallet e le quotazioni scaricate vengono salvati sul tuo computer, nella cartella dati del programma. Le uniche chiamate di rete sono quelle necessarie a importare i movimenti (API degli exchange/wallet) e a recuperare le quotazioni storiche.

Il dettaglio completo — cosa resta sul computer, cosa esce e verso chi — è nell'[informativa sulla privacy](https://lucapasselli.github.io/GiacenzeCrypto/privacy/) (sorgente in `docs/privacy/`).

## Disclaimer

Questo software viene fornito "così com'è", **senza alcuna garanzia**, esplicita o implicita, di correttezza, completezza o idoneità a uno scopo particolare. Il testo completo è nel [disclaimer](https://lucapasselli.github.io/GiacenzeCrypto/documentazione/disclaimer.html).

- Il programma è uno **strumento di supporto al calcolo**: non fornisce consulenza fiscale, legale o finanziaria e **non sostituisce** il parere di un commercialista o di un consulente fiscale abilitato.
- I calcoli (plusvalenze, Quadro RT, Quadro RW) dipendono dalla correttezza e completezza dei dati importati, dalla classificazione dei movimenti e dalle quotazioni recuperate da servizi terzi (CoinGecko, Binance, Etherscan, mempool.space, Helius, ecc.), sui quali l'autore non ha alcun controllo.
- È responsabilità dell'utente **verificare l'esattezza** dei dati e dei risultati prodotti prima di utilizzarli per qualsiasi adempimento fiscale.
- L'autore e i contributori del progetto **non si assumono alcuna responsabilità** per errori, omissioni, danni diretti o indiretti derivanti dall'uso di questo software, incluse eventuali sanzioni, accertamenti o perdite economiche.

L'uso dell'applicazione è a totale rischio dell'utilizzatore.

## Licenza

Distribuito con licenza **MIT**: vedere il file [`LICENSE`](LICENSE) e
[`THIRD-PARTY-LICENSES.md`](THIRD-PARTY-LICENSES.md) per le librerie di terze parti.

## Cosa fa (dettaglio tecnico)

- **Importazione movimenti** da:
  - Crypto.com (file di export dell'app e dell'exchange)
  - Binance (API REST con firma HMAC-SHA256, oppure i CSV esportati dal sito)
  - OKX (API: conti Funding e Trading, Simple Earn e archivio storico trimestrale fino al 2021; oppure i CSV)
  - wallet DeFi/on-chain: Bitcoin (xpub/ypub/zpub via mempool.space), Solana (via Helius) e una ventina di chain EVM (Ethereum, BSC, Cronos, Arbitrum, Base, Optimism, Polygon, Avalanche, Gnosis, Linea, Blast, Unichain, World Chain, Taiko, Abstract, Katana, Sonic, Mantle, Berachain, Monad) via Etherscan multi-chain API o Blockscout
  - esportazioni di servizi di rendicontazione (CoinTracking, Tatax)
  - importazione generica da CSV, configurabile tramite descrittori JSON
- **Classificazione automatica** dei movimenti (acquisto, vendita, conversione, trasferimento, commissioni, ecc.) con possibilità di correzione manuale
- **Calcolo del costo fiscale con metodo LIFO** e determinazione della plusvalenza/minusvalenza per ogni cessione
- **Recupero quotazioni storiche** (CoinGecko, Binance e altri exchange tramite CCXT/Node.js) con conversione USD→EUR e cache locale dei prezzi
- **Generazione dei prospetti** per Quadro RT e Quadro RW, esportabili in **PDF** ed **Excel**
- **Gestione multi-wallet e multi-anno**, con backup e ripristino dell'intero archivio (movimenti, impostazioni, gruppi wallet, prezzi inseriti a mano, documenti di origine e configurazioni di importazione)
- **Tracciabilità delle importazioni**: ogni movimento ricorda il file da cui proviene, conservato compresso e riapribile dal programma
- Interfaccia grafica con tema chiaro/scuro (FlatLaf), con il carattere di scrittura incluso nel programma

## Per chi vuole compilarlo dal codice sorgente

Questa sezione è per sviluppatori: chi vuole solo usare il programma può ignorarla e seguire "Come installarlo" più sopra.

Requisiti: Java 21 (JDK) e Maven.

```bash
# Compilazione (produce target/Giacenze_Crypto-<versione>-jar-with-dependencies.jar)
mvn package

# Esecuzione
java -jar target/Giacenze_Crypto-<versione>-jar-with-dependencies.jar
```

Argomenti opzionali utili in fase di avvio:

```
--debug              # apre una finestra di log flottante
--workdir <path/>    # cartella dati personalizzata (deve terminare con /)
--NoJarPath          # usa ./ come percorso risorse (utile in esecuzione da IDE)
--fontSize 14              # forza la dimensione del font globale
--fontFamily "Noto Sans"   # forza il font globale
```

Il JAR va distribuito insieme alla cartella `Immagini/` (icone e immagini non sono incluse nel classpath).

## Riferimenti

- **Sito web**: https://giacenzecrypto.it
- **Codice sorgente**: https://github.com/Lucapasselli/GiacenzeCrypto
- **Pacchetti compilati**: https://github.com/Lucapasselli/GiacenzeCrypto/releases (anche su
  [SourceForge](https://sourceforge.net/projects/giacenze-crypto-com/)
- **Documentazione**: https://lucapasselli.github.io/GiacenzeCrypto/documentazione/
- **Novità delle versioni**: https://lucapasselli.github.io/GiacenzeCrypto/documentazione/changelog.html
- **Canale YouTube**: https://www.youtube.com/@cryptofer82
- **Gruppo Telegram**: https://t.me/+6kfy5mjov-I2ODY8
- **Changelog Completo**: https://lucapasselli.github.io/GiacenzeCrypto/documentazione/changelog.html
