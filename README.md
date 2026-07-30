# Giacenze Crypto

**Giacenze Crypto** è un'applicazione desktop Java (Swing) pensata per aiutare chi detiene criptovalute a preparare i dati necessari alla **dichiarazione fiscale italiana**.

Il programma importa i movimenti da exchange e wallet, recupera le quotazioni storiche delle valute e calcola in automatico:

- **Quadro RT** — plusvalenze e minusvalenze da cessione/permuta di cripto-attività;
- **Quadro RW** — valore delle disponibilità detenute all'estero (wallet e conti su exchange) al 31/12 di ogni anno.

L'obiettivo è ridurre il lavoro manuale di raccolta ed elaborazione dati che normalmente precede la compilazione della dichiarazione, non sostituire la consulenza di un commercialista.

## Cosa fa

- **Importazione movimenti** da:
  - Crypto.com (file di export)
  - Binance (API REST con firma HMAC-SHA256)
  - wallet DeFi/on-chain: Bitcoin (xpub/ypub/zpub via mempool.space), Solana (via Helius), ed EVM chain (Ethereum, BSC, CRO, Arbitrum, Base, Berachain, Avalanche, Polygon, Monad) via Etherscan multi-chain API
  - importazione generica da CSV, configurabile tramite descrittori JSON
- **Classificazione automatica** dei movimenti (acquisto, vendita, conversione, trasferimento, commissioni, ecc.) con possibilità di correzione manuale
- **Calcolo del costo fiscale con metodo LIFO** e determinazione della plusvalenza/minusvalenza per ogni cessione
- **Recupero quotazioni storiche** (CoinGecko, Binance e altri exchange tramite CCXT/Node.js) con conversione USD→EUR e cache locale dei prezzi
- **Generazione dei prospetti** per Quadro RT e Quadro RW, esportabili in **PDF** ed **Excel**
- **Gestione multi-wallet e multi-anno**, con backup del database (il ripristino, al momento, va effettuato manualmente)
- Interfaccia grafica con tema chiaro/scuro (FlatLaf)

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
--fontSize 14         # forza la dimensione del font globale
--fontFamily Inter    # forza il font globale
```

Il JAR va distribuito insieme alla cartella `Immagini/` (icone e immagini non sono incluse nel classpath).

## Dati e privacy

L'applicazione lavora **in locale**: i movimenti, i wallet e le quotazioni scaricate vengono salvati in database H2 all'interno della cartella di lavoro (`database.mv.db`, `personale.mv.db`, `prezzi.mv.db`, ecc.). Le uniche chiamate di rete sono quelle necessarie a importare i movimenti (API degli exchange/wallet) e a recuperare le quotazioni storiche.

## Disclaimer

Questo software viene fornito "così com'è", **senza alcuna garanzia**, esplicita o implicita, di correttezza, completezza o idoneità a uno scopo particolare.

- Il programma è uno **strumento di supporto al calcolo**: non fornisce consulenza fiscale, legale o finanziaria e **non sostituisce** il parere di un commercialista o di un consulente fiscale abilitato.
- I calcoli (plusvalenze, Quadro RT, Quadro RW) dipendono dalla correttezza e completezza dei dati importati, dalla classificazione dei movimenti e dalle quotazioni recuperate da servizi terzi (CoinGecko, Binance, Etherscan, mempool.space, Helius, ecc.), sui quali l'autore non ha alcun controllo.
- È responsabilità dell'utente **verificare l'esattezza** dei dati e dei risultati prodotti prima di utilizzarli per qualsiasi adempimento fiscale.
- L'autore e i contributori del progetto **non si assumono alcuna responsabilità** per errori, omissioni, danni diretti o indiretti derivanti dall'uso di questo software, incluse eventuali sanzioni, accertamenti o perdite economiche.

L'uso dell'applicazione è a totale rischio dell'utilizzatore.

## Licenza

Distribuito con licenza **MIT**. Vedere il file `pom.xml` per i dettagli.

## Riferimenti

- **Codice sorgente**: https://github.com/Lucapasselli/GiacenzeCrypto
- **Pacchetti compilati**: https://sourceforge.net/projects/giacenze-crypto-com/
- **Documentazione**: https://sourceforge.net/projects/giacenze-crypto-com/files/Documentazione/
- **Canale YouTube**: https://www.youtube.com/@cryptofer82
- **Gruppo Telegram**: https://t.me/+6kfy5mjov-I2ODY8
