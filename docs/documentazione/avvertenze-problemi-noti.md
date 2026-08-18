---
layout: default
title: Avvertenze e problemi noti
---

# Avvertenze e problemi noti

Di seguito una serie di problemi noti riscontrati durante i test di funzionamento, insieme alle
avvertenze sul corretto utilizzo del programma.

## Importazioni {#importazioni}

- **Importazioni tramite CoinTracking o Tatax** (cioè tutte quelle non supportate direttamente dal
  programma: il supporto diretto riguarda **Binance**, **OKX** e **Crypto.com**) possono essere, a
  seconda dei casi, piuttosto imprecise. Questi servizi possono inoltre cambiare da un giorno all'altro
  il formato del file, creando ulteriori problemi in importazione. Si raccomanda di controllare i saldi
  iniziali e finali e di sistemare le eventuali incongruenze.

- **Binance.** Dove possibile conviene usare lo scaricamento diretto **dalle API** (*Opzioni – ApiKey*),
  che recupera scambi, conversioni, depositi, prelievi ed Earn. Sui file **CSV** esportati dal sito
  possono invece mancare, a seconda del periodo in cui sono stati generati, alcuni dati (scambi verso
  cripto non più supportate, alcuni Earn, ecc.): anche qui è opportuno verificare manualmente i saldi.

- **OKX.** Anche in questo caso lo scaricamento **dalle API** è la strada consigliata: recupera i
  movimenti del conto Funding e del conto Trading, i rendimenti di Simple Earn e — su richiesta —
  l'**archivio storico trimestrale**, che risale fino al 2021. L'esportazione CSV dal sito copre invece
  solo un periodo limitato, e richiede di esportare **sia** il trading **sia** il funding wallet.
  Confrontare sempre i saldi con quelli presenti sull'exchange per essere sicuri di aver importato tutto.

- **In generale gli exchange possono variare da un giorno all'altro la formattazione** dei file CSV,
  oppure introdurre nuovi tipi di riga: controllare sempre il risultato delle importazioni e, in caso di
  problemi, segnalarlo per le opportune correzioni.

## Importazioni da blockchain (DeFi) {#importazioni-da-blockchain-defi}

- Le reti supportate sono oggi: **Bitcoin**, **Solana** e le reti EVM **Ethereum, BSC, Cronos, Base,
  Arbitrum, Polygon, Avalanche, Optimism, Gnosis, Linea, Blast, Unichain, World Chain, Taiko, Abstract,
  Katana, Sonic, Mantle, Berachain, Monad**.

- Alcune movimentazioni gestite **all'interno** di piattaforme DeFi non vengono esposte dagli explorer
  e quindi non possono essere importate dal programma.

- I trasferimenti di **NFT (ERC-721) ed ERC-1155** vengono letti dove l'explorer della rete li
  espone; dove non lo fa, le relative transazioni vanno inserite a mano.

- Per i **Layer 2** (Arbitrum, Base, Optimism, Linea, ecc.) le API base degli explorer non forniscono la
  quota di commissione relativa alla L1, che quindi non viene importata. Quella parte di fee è
  generalmente molto bassa, ma con migliaia di operazioni può influire sul valore finale degli ETH
  detenuti.

- Vale in ogni caso la regola dei punti precedenti: **controllare i saldi** e sistemare le incongruenze.

## Classificazione e calcolo {#classificazione-e-calcolo}

- Tutti i movimenti di **entrata e uscita di token "sconosciuti" vanno classificati**, altrimenti il
  risultato dei calcoli sarà errato. Vedi
  [Classificazione dei movimenti](classificazioni-movimenti.html).

- Il programma è nato per la **normativa in vigore dal 2023**. Manca quindi, ad esempio, il controllo
  sulla soglia dei 51.645,69 euro per sette giorni consecutivi, che rendeva fiscalmente rilevanti le
  operazioni antecedenti al 2023.

- Il programma parte con una serie di **preimpostazioni** che potrebbero non essere quelle corrette o
  desiderate per la propria dichiarazione: è opportuno rivedere le opzioni in base alle proprie
  esigenze e interpretazioni. Vedi
  [Calcolo delle plusvalenze e opzioni](calcolo-plusvalenze-opzioni.html) e
  [Opzioni di calcolo del Quadro RW](opzioni-calcolo-rw.html).

- Le **quotazioni** vengono recuperate da servizi di terze parti e non sempre esistono per tutti i
  token e per tutte le date. I movimenti rimasti senza prezzo si individuano con l'apposito filtro
  sull'elenco movimenti e il valore può essere inserito a mano.

## Prima di lavorare sui dati {#prima-di-lavorare-sui-dati}

- Fare un **backup** prima di operazioni massive o di un aggiornamento: *Opzioni – Backup / Ripristino*
  crea un unico archivio con movimenti, impostazioni, gruppi wallet, prezzi inseriti a mano, documenti
  di origine e configurazioni di importazione.

[Torna all'indice della documentazione](./)
