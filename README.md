# Giacenze Crypto

**Giacenze Crypto** è un'applicazione desktop Java (Swing) pensata per aiutare chi detiene criptovalute a preparare i dati necessari alla **dichiarazione fiscale italiana**.

Il programma importa i movimenti da exchange e wallet, recupera le quotazioni storiche delle valute e calcola in automatico:

- **Quadro RT** — plusvalenze e minusvalenze da cessione/permuta di cripto-attività;
- **Quadro RW** — valore delle disponibilità detenute all'estero (wallet e conti su exchange) al 31/12 di ogni anno.

L'obiettivo è ridurre il lavoro manuale di raccolta ed elaborazione dati che normalmente precede la compilazione della dichiarazione, non sostituire la consulenza di un commercialista.

## Cosa fa

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

## Requisiti

- Java 21 (JRE/JDK)
- Sistema operativo desktop (Windows, Linux, macOS)

## Avvio

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

## Dati e privacy

L'applicazione lavora **in locale**: i movimenti, i wallet e le quotazioni scaricate vengono salvati in database H2 all'interno della cartella di lavoro (`database.mv.db`, `personale.mv.db`, `prezzi.mv.db`, ecc.). Le uniche chiamate di rete sono quelle necessarie a importare i movimenti (API degli exchange/wallet) e a recuperare le quotazioni storiche.

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

## Riferimenti

- **Codice sorgente**: https://github.com/Lucapasselli/GiacenzeCrypto
- **Pacchetti compilati**: https://github.com/Lucapasselli/GiacenzeCrypto/releases (anche su
  [SourceForge](https://sourceforge.net/projects/giacenze-crypto-com/), nell'AUR come `giacenze-crypto-bin` e in formato flatpak)
- **Documentazione**: https://lucapasselli.github.io/GiacenzeCrypto/documentazione/
- **Novità delle versioni**: https://lucapasselli.github.io/GiacenzeCrypto/documentazione/changelog.html
- **Canale YouTube**: https://www.youtube.com/@cryptofer82
- **Gruppo Telegram**: https://t.me/+6kfy5mjov-I2ODY8
