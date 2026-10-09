# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Giacenze Crypto** is a Java 21 Swing desktop application for Italian crypto tax reporting. It tracks cryptocurrency movements, calculates capital gains (plusvalenze) using the LIFO method, and produces data for Italian tax forms (Quadro W/RW for wealth declaration, Quadro T/RT for capital gains).

## Build & Run

```bash
# Run the test suite (JUnit 5, characterization tests of the LIFO capital-gains engine)
./mvnw test

# Build fat JAR
./mvnw clean package

# Run from source (matches nbactions.xml dev configuration)
./mvnw process-classes exec:exec -Dexec.appArgs="--NoJarPath --workdir ./test/2025/"

# Run a single test class, or a single method
./mvnw test -Dtest=CalcoliPlusvalenzeNewStackLifoTest
./mvnw test -Dtest=CalcoliPlusvalenzeNewStackLifoTest#nomeDelMetodo

# Run the built JAR directly (substitute the <version> from pom.xml, currently 1.0.64.03)
java -jar target/Giacenze_Crypto-<version>-jar-with-dependencies.jar --NoJarPath --workdir ./test/2025/
```

The project uses JDK 26 at runtime; source/target compatibility is JDK 21 (`maven.compiler.source/target` in `pom.xml`). `.mvn/jvm.config` passes `--enable-native-access=ALL-UNNAMED` and `--sun-misc-unsafe-memory-access=allow` to suppress third-party library warnings from Maven itself.

`pom.xml` declares `lib/` as a file-based Maven repository (`unknown-jars-temp-repo`), but the directory now holds only an empty `.index/` and every declared dependency resolves from Central — it is leftover NetBeans scaffolding.

### Tests

Tests live in `src/test/java/` (same package as production code, to reach package-private statics like `Principale.MappaCryptoWallet`). They are **characterization tests**: they pin the current fiscal behavior, including known bugs (referenced by ID from `nocommit/Documentazione/Analisi_Bug_Criticita.md`). If a bug fix intentionally changes a result, update the corresponding test in the same commit.

| Test class | Covers |
|---|---|
| `CalcoliPlusvalenzeNewStackLifoTest` | LIFO stack mechanics |
| `CalcoliPlusvalenzeNewAggiornaPlusvalenzeTest` | `AggiornaPlusvalenze()` on synthetic movements |
| `CalcoliPlusvalenzeNewGoldenMasterTest` | full recompute against the real local dataset |
| `EccezioniDefiTest` | DeFi movement exceptions |
| `PrezziEmoneyEuroTest` | EMoney tokens pinned to EUR |
| `Principale_Movimenti_SeparaUnisciTest` | splitting a movement into deposit/withdrawal, merging them back into a swap, and merging N same-type movements by summing quantities |
| `MovimentiCryptoCreaMovimentoTest` | `MovimentiCrypto.creaMovimento()` — in/out direction and resulting categoria |
| `SegnoQuantitaM7Test` | `Funzioni.isNegativo()`, `Moneta.InvertiQta()` and CEX in/out classification (bug M7) |
| `CcxtInteropConvertOKXBillsTest` | `CcxtInterop.convertOKXBills()` — OKX API bills → the 19-field intermediate rows shared with the OKX CSV import |

**Static H2 connections leak between test classes, and `IsolamentoConnessioniH2` contains it** (2026-10-05).
The 45 classes that open a temporary DB close it in `@AfterAll` but leave the closed `Connection` in
`DatabaseH2`'s static fields, with the working directory still pointing at their deleted `@TempDir`. A later
class that opens no DB (`Principale_Movimenti_SeparaUnisciTest`) then passed the `connectionPrezzi == null`
guard of `Prezzi.CercaPrezzoPreciso`, failed on the closed cache, fell back to the network and downloaded
Node (~237 MB) into the deleted directory, recreating it: one orphan `/tmp/junit*` per suite run, until `/tmp`
(a RAM tmpfs here) was full and the import tests failed with "Spazio esaurito sul device". The extension,
autodetected for every class (`src/test/resources/junit-platform.properties` +
`META-INF/services/org.junit.jupiter.api.extension.Extension`), nulls closed connections before and after
each class, so such a class behaves as when run alone. A new test class that needs a DB must still open its
own. A few classes (Dual Investment, Gate.io, KuCoin, Nexo imports) still download Node into their own
`@TempDir` on every run, which is network and time, not a leak.

**Golden master** (`CalcoliPlusvalenzeNewGoldenMasterTest`) needs the private dataset `test/Dichiarazione 2025/` (`movimenti.crypto.db` + `personale.mv.db`), which is deliberately **not** in git. It self-skips via JUnit `Assumptions` when the dataset is missing — a "skipped" result on a fresh clone is expected, not a failure. On its first run with the dataset present it writes the baseline `nocommit/GoldenMaster/plusvalenze.golden` and aborts as skipped; run again to actually compare. It copies the personal DB into a `@TempDir`, so the real dataset is never opened or locked. When a diff is **expected** (new movements imported, or a bug fix that deliberately changes results), verify every difference is explainable, delete the baseline file, and re-run to regenerate it.

### CLI arguments accepted by `Giacenze_Crypto.main()`

| Flag | Effect |
|------|--------|
| `--NoJarPath` | Sets resource path to `./` instead of detecting from JAR location |
| `--workdir <path>/` | Working directory for DB and data files (must end with `/`, supports `HOME` token) |
| `--workInRisorse` | Sets working directory equal to resource path |
| `--risorse <path>/` | Override resource path (must end with `/`) |
| `--debug` | Opens a log window on startup |
| `--fontSize <n>` | Override global font size |
| `--fontFamily <name>` | Override global font family |

## Architecture

### Entry point & startup
`Giacenze_Crypto.java` — parses CLI args, sets paths in `VarStatiche`, connects H2 databases, sets FlatLaf theme, then opens `Principale`.

### Splash di avvio e barra di avanzamento (`SplashAvvio.java`)

The splash runs before the database is open and before the L&F is installed, so it is
dependency-free (only `VarStatiche`). Two things are not obvious enough to rediscover by reading
the code: it does **not** paint through Swing — the content is drawn into a `BufferedImage` and
blitted with `finestra.getGraphics().drawImage(...)` from the loading thread itself, because a
`repaint()`-driven bar would sit at 0 for the whole slow phase; and the phase weights in
`SplashAvvio.Fase` are adaptive (measured at each startup, averaged into `avvio.tempi.db`), so
**adding or moving a `fase(...)` call requires the enum order to still match execution order** — the
bar never goes backwards, so an out-of-order phase silently freezes it.

`GeneraSplash` regenerates `splash.png` + the HiDPI variants from the same drawing routine
(`SplashAvvio.disegnaContenuto`) — re-run it after any change to the splash graphics. Full design
(why no animation, blit throttling, measured phase weights) in
`nocommit/Documentazione/Analisi_Performance_Caricamento.md` §10.

### Static configuration (`VarStatiche.java`)
Single source of truth for:
- `pathRisorse` — where images and bundled resources live
- `workingDirectory` — where runtime data lives (databases, backups, imports)
- All DB connection URL getters (`getDBPrincipale()`, `getDBPersonale()`, `getDBPrezzi()`)
- All file path helpers (DB files, CSV files, folder paths)

`VarStatiche.Versione` is **not** hardcoded: `leggiVersione()` reads `versione` from the bundled `src/main/resources/com/giacenzecrypto/giacenze_crypto/version.properties`, which holds the literal `${project.version}`, substituted by Maven resource filtering. To bump the displayed version, change `<version>` in `pom.xml` — nothing else.

The `<build><resources>` block in `pom.xml` is deliberately split into **two** `<resource>` entries on the same directory: the first with `<filtering>true</filtering>` and `<include>**/version.properties</include>`, the second without filtering and with the mirror `<exclude>`. The split is not cosmetic — almost every other resource is a binary image (PNG/SVG under `Images/`, `Temi/`, splash, logo) and indiscriminate filtering would corrupt them. Keep both blocks: without the filtered one `leggiVersione()` sees the literal `${…}` (it guards with `startsWith("${")`) and falls back to `"sconosciuta"`, which then shows in the window title. For the same reason the shade plugin's `ManifestResourceTransformer` must keep `<manifestEntries><SplashScreen-Image>splash.png</SplashScreen-Image></manifestEntries>`, or `SplashAvvio.splashNativo()` gets `null` from `SplashScreen.getSplashScreen()` and the native splash never appears. Both had gone missing from `pom.xml` and were restored on 2026-07-30.

### Data storage: what is and isn't a database

The `.db` extension is used for **both** H2 databases and plain text files — do not assume a `.db` path is SQL.

Flat files in `workingDirectory`, reached through `VarStatiche` getters:
- `movimenti.crypto.db` (`getFile_CryptoWallet()`) — **the primary movement store**: one `;`-delimited line per movement, loaded into `Principale.MappaCryptoWallet`
- `cambioUSDEUR.db`, `crypto.com.fiatwallet.db`, `crypto.com.cardwallet.db`, `crypto.com.dati.db`, `crypto.com.fiatwallet.tipimovimentiPers.db`

### Database layer (`DatabaseH2.java`)
Three embedded H2 connections managed as static fields, all opened by `CreaoCollegaDatabase()`, which also creates/migrates every table:
- `connection` → `database.mv.db` — session options plus token/exchange registries and metadata caches (`OPZIONI`, `GESTITIBINANCE`, `GESTITICOINBASE`, `GESTITICOINGECKO`, `GESTITICOINMARKETCAP`, `GESTITICRYPTOHISTORY`, `TOKENSOLANA`, `RINOMINATOKEN`, `PROVIDERDEFI`, `GOPLUSSECURITY`, …). It does **not** hold the movements.
- `connectionPersonale` → `personale.mv.db` — user preferences and user-owned data: `OPZIONI`, `PrezziNew` (manually entered prices), `WALLETS`, `WALLETGRUPPO`, `GRUPPO_ALIAS` (wallet groups for Quadro RW), `EXCHANGEAPI` (API keys), `EMONEY`, `GIACENZEBLOCKCHAIN`, `EXCHANGETOKENS`
- `connectionPrezzi` → `prezzi.mv.db` — fetched market price cache (`PrezziNew`, `PrezziKO`), opened with custom H2 tuning flags (`AUTO_COMPACT_FILL_RATE=0`, `RETENTION_TIME=0`, large `CACHE_SIZE`)

The app enforces single-instance via H2's file locking — `CreaoCollegaDatabase()` returns `false` if another session holds the lock.

**The price DB's tuning flags (`AUTO_COMPACT_FILL_RATE=0`, `RETENTION_TIME=0`) are why it has to be
compacted on command** — replaced pages are never reused, so `prezzi.mv.db` bloats over time (measured:
2.847 MB of file for 565 MB of data). `DatabaseH2.CompattaPrezzi/CompattaPrincipale(boolean Riapri)`
run `SHUTDOWN DEFRAG` without deleting a row; error **90121** while releasing the statement is
expected, not a failure. The connection object is **replaced, not reset** — nothing may cache
`connectionPrezzi` in a field or hold a `PreparedStatement` across the call. Full mechanics
(why the lock is per-file, `LeggiStatoCompattazione()` cost, why the estimate under-reports the
recoverable space) in `nocommit/Documentazione/Analisi_DB_Prezzi_Manutenzione.md`, which also covers
why deleting unreachable rows is deliberately not done (phases 2/3, deferred).

**Settings persistence** uses a key-value `OPZIONI` table in each database:
- `DatabaseH2.Opzioni_Scrivi/Leggi` — writes/reads from `database.mv.db` (session state)
- `DatabaseH2.Pers_Opzioni_Scrivi/Leggi` — writes/reads from `personale.mv.db` (user preferences)

Settings are restored at startup in `Principale.AggiornaSpunte()`, called from the constructor after `initComponents()`.

**The personal `PrezziNew` (in `connectionPersonale`) is not the same shape as the price cache**: since 2026-09-17 it has an extra `gruppo VARCHAR DEFAULT 'TUTTI' NOT NULL` column, and its primary key is `(timestamp, exchange, symbol, rete, address, gruppo)` — one column longer than the cache's. Before that date the wallet group was glued into `exchange` as `"Personalizzato (TUTTI)"` and the reader had to match it by substring. The migration lives in `CreaoCollegaDatabase`, guarded by `TabellaSenzaColonna`, and its step order is forced by H2: key columns must be `NOT NULL` (error 90023) while `ADD COLUMN … DEFAULT` creates a nullable one, so `SET NOT NULL` goes between the backfill and the new key. The table must never be dropped and recreated the way `MOVIMENTI_STORICO` was — it holds the user's hand-entered year-end prices, which is what Quadro RW is built on. `gruppo` is always `'TUTTI'` today (`Funzioni.getGruppoWalletXPrezzi` returns that constant); the column exists so that a future per-wallet-group price preference needs no further schema change. Note the asymmetry, which **looks like a bug and is a deliberate decision**: `DammiPrezzoDaDatabasePersonale` has a real `else`, so a caller passing a non-blank source (every import path) gets **no** personalised price at all. Left as is on purpose — personalised prices are looked up on the exact timestamp and the user corrects a wrong price on the movement itself, so the case does not arise in practice, while "fixing" it would change values on already-imported movements.

**PrezziKO table** (in `connectionPrezzi`): caches tokens/dates where price lookup failed. Schema: `symbol VARCHAR, timestamp BIGINT, rete VARCHAR, address VARCHAR`. Written by `Prezzi.PrezzoIrrecuperabileDaDB_Scrivi()`, queried by `Prezzi.PrezzoIrrecuperabileDaDB_Leggi()`.

### Main window (`Principale.java`)
The central JFrame. Holds several critical static maps that other classes read:
- `MappaCryptoWallet` — all loaded crypto movements
- `Mappa_ChainExplorer` — chain name → API endpoint config
- `Mappa_AddressRete_Nome` — token contract address → canonical symbol
- `MappaRetiSupportate` — set of supported chain identifiers

These maps are populated at startup by `VarCondivise.CompilaMappaChain()` and `VarCondivise.CompilaMappaRetiSupportate()`.

**Mass operations must not reuse the single-item function in a loop.** Each interactive function opens a dialog, and every time the main window regains focus on its close, `TabellaCryptodaAggiornare` triggers a full `Funzioni_AggiornaTutto()` — so a loop over a large selection recomputes everything once per item. The pattern established by the bulk SCAM removal (`Principale.java`, "Rimozione stato SCAM") is: a `…SenzaConferma` variant of the operation, one modal `Download` progress window kept open for the whole run with the loop on a separate thread, then a single `Funzioni_AggiornaTutto()` at the end followed by `TabellaCryptodaAggiornare = false`, so the next focus regain does not repeat it.

Two more statics carry the context of the movements popup menu: `PopUp_IDTrans` (ID of the **first** selected row) and `PopUp_IDTransSelezionati` (IDs of **all** selected rows, added for the split/merge menu items). Both are filled in `Funzioni_RichiamaPopUpdaTabella` before the menu opens. The invariant is easy to break: **every** table that opens that popup must repopulate the list, otherwise the menu acts on the previous table's selection — which is why `GUI_DettaglioTransazione`, which has its own popup and its own `Funzioni_RichiamaPopUpdaTabella`, had to be aligned too.

**I contatori del pulsante "Errori" sono cinque e non nascono tutti insieme.** Tre (`NumErroriMovNoPrezzo`, `NumErroriMovSconosciuti`, `NumErroriStackLiFoMancante`) li calcola il ciclo di `TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa`; il quarto, `NumErroriGiacenzeNegative`, e il quinto, `NumErroriMovimentiCollegati` (2026-10-05), arrivano dal thread di `Funzione_CaricaTabelleSecondarieInBackgroud`, che finisce **dopo**. Per questo il testo del pulsante sta in `Errori_AggiornaPulsante()`, chiamato da tutti e due i punti: da uno solo mostrerebbe sempre il conteggio della passata precedente, e zero al primo caricamento.

**"Movimenti collegati incoerenti" (quinto contatore) controlla i `[20]`, non un calcolo.** `MovimentiCollegati.Controlla`
(logica pura, gira sulla copia della collezione nel thread in background, 10-30 ms su 20.000 movimenti) verifica che
ogni riferimento di `[20]` punti a un movimento esistente che lo ricambia: tutte le forme legittime sono simmetriche,
e sugli archivi reali dell'utente il controllo dà zero. Le eccezioni sono di tre tipi: scambio differito sovrascritto
(bug C17, il prelievo cita generati che citano un altro prelievo, riparabile con *Ricostruisci gli scambi*),
contratto Dual a moneta uguale nella forma precedente (bug C18, versi unici per costruzione: si converte ripassando
il file di dettaglio, quindi **non** va contato fra gli "altri"), altri riferimenti rotti (a mano, annullando la
classificazione). Il dialogo (`Principale_MovimentiCollegati.Gestisci`) rifà il controllo sulla mappa viva prima di
riparare, e un solo `Funzioni_AggiornaTutto()` segue le riparazioni. Una nuova forma di classificazione che scriva
`[20]` in un verso solo comparirebbe qui come errore: o la si scrive simmetrica, o la si dichiara in
`MovimentiCollegati.FormaPrecedenteDual`. La chiave `DUAL-` in `[45]` invece non si conta e non può
essere incompleta: sta solo sui movimenti originali, e i generati la ricavano dal loro gruppo `[20]`
(`GruppoOperazione.ChiaveEffettiva`). Un dato in un posto solo: il gruppo lo dice `[20]`, l'identità `[45]`.

**"Giacenze negative" non è un doppione di "parte del LiFo mancante"**, anche se le due voci del dialogo portano alla stessa scheda. La prima viene da `Funzioni.ControllaSaldiNegativi`, che ragiona per **exchange + sotto-wallet** (`[3];[4];token`); la seconda conta i movimenti con la lettera `A` in `v[38]`, scritta dal motore delle plusvalenze e quindi ragionata per **gruppo wallet**. Un trasferimento interno azzera esplicitamente quella `A`, perciò un comparto svuotato a forza di giroconti (es. un sotto-wallet alimentato via `walletSpecularePerCausale`) produce la prima e non la seconda. Le etichette del dialogo devono continuare a distinguerle. `ControllaSaldiNegativi` scorre `MappaCryptoWallet` nell'ordine della mappa, che è una `TreeMap` sull'ID il cui prefisso è `yyyyMMddHHmmss`: il conteggio è quindi **cronologico a prescindere dall'ordine in cui i CSV sono stati importati**, e un rientro caricato prima della sua uscita viene segnalato finché manca l'altra riga.

### Separating GUI from operational logic (ongoing refactor)

`Principale.java` is ~16 000 lines and its `.form` is over 500 KB, because GUI and business logic grew together in the same file. **The direction of travel is to keep the two as separate as possible**: `Principale` should hold the Swing wiring, and the operational logic of each tab should live in its own companion class.

This extraction is already under way — it is a deliberate, incremental refactor, not accidental structure:

| Class | Holds |
|---|---|
| `Principale_GiacenzeaData.java` | operational logic of the "Giacenze a data" tab |
| `Principale_Opzioni_Pulizie.java` | data-cleanup operations from the options tab |
| `Principale_QuadroRW.java` | the CRYPTO rows of the Quadro W/RW summary table (`RighiCrypto`): one row per wallet group and per CRYPTO holding period, bollo of the period, year-end/period snapshot for bollo periods. Extracted from `RW_CalcolaRW` on 2026-09-26 when rows became per period |
| `Principale_Movimenti_SeparaUnisci.java` | three popup operations on movements: splitting one two-coin movement into an independent deposit + withdrawal, merging an unclassified deposit + withdrawal back into a single swap, and merging N already-classified movements of the same type/coin/wallet into one by summing their quantities |
| `Principale_MovimentiCollegati.java` | the "Movimenti collegati incoerenti" entry of the Errori button: shows what `MovimentiCollegati.Controlla` found and rebuilds overwritten deferred swaps on request. The logic itself is in `MovimentiCollegati` (no Swing), shared with `Binance_DualInvestment` |
| `Principale_CommissioniCollegate.java` | the linked-fee operations of phase 3: one-shot, re-runnable pairing of the archive's fees (*Opzioni → Commissioni collegate*), manual *Collega/Scollega commissioni* from the movements popup, the rows shown in the movement detail. None of them recomputes anything — they only turn on *Salva* |

**These rules are provisional**: they simply describe what the two existing classes already do, and are still to be reviewed and agreed with the user — open points include whether dialogs and wait cursors belong in the extracted classes at all, and whether shared state should keep being reached through the static maps or be passed in instead. Until that review happens, mirror the existing pattern rather than inventing a different one.

Follow the shape of the existing extractions when moving code:

- Name the class `Principale_<Tab or area>`, in the same package.
- Methods are `public static`; the class holds **no Swing fields** and no reference to `Principale`. Whatever it needs is passed in — the `JTable` to read the selection from, a `Window owner` to parent dialogs, a wallet name, etc. (`GiacenzeaData_Funzione_SistemaQta(JTable, String Wallet, Window owner)`).
- Shared state is reached the same way as before, via the static maps (`import static Principale.MappaCryptoWallet`).
- The extracted method returns what the GUI needs in order to react — typically a `boolean` "something changed". **Refreshing tables stays in `Principale`**, so the event handler collapses to a thin delegation:

  ```java
  if (Principale_GiacenzeaData.GiacenzeaData_Funzione_SistemaQta(
          GiacenzeaData_TabellaDettaglioMovimenti, Giacenzeadata_Walleta_Label.getText().trim(), this))
      GiacenzeaData_CompilaTabellaToken(true);
  ```

- The split is pragmatic, not strict MVC: the extracted classes still open `AppDialog` confirmations and set wait cursors, using the `Window` passed in. What must **not** stay in `Principale` is the operational work itself.

When adding a feature to a tab that already has a companion class, put the logic there rather than growing `Principale`. **Extracting further areas is planned work to be done together with the user — do not undertake a broad migration of existing code unprompted.**

### Il costo di carico delle rimanenze in "Giacenze a data"

La colonna accanto a "Valore (in Euro)" è il costo di carico LIFO della giacenza mostrata sulla riga,
prodotto da `Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze(DataRiferimento)`. Tre cose non ovvie:

- **La passata non è filtrata per wallet, la lettura sì.** Le quantità della tabella si possono filtrare
  a monte perché sommare è locale; il LIFO no. Soprattutto, **un giroconto interno non porta con sé
  nessun costo** — `Calcoli_PlusvalenzeNew` lascia `v[16]` e `v[17]` vuoti quando la controparte è dello
  stesso gruppo — quindi una pila costruita sui soli movimenti del wallet selezionato mostrerebbe
  giacenze positive a costo zero per ogni moneta arrivata da un giroconto. Fissato da
  `unaMonetaArrivataConUnGiroContoInternoConservaIlSuoCostoDiCarico`.
- **La pila è per gruppo wallet come nel motore, ma la moneta è la chiave della riga**
  (`Moneta;Tipo;Address;Rete`) e non il solo simbolo: due righe con lo stesso simbolo su reti diverse
  leggerebbero altrimenti lo stesso lotto due volte. In lettura i lotti dei gruppi in gioco si uniscono e
  si riordinano per ID del movimento (che comincia con `yyyyMMddHHmmss`, quindi è già cronologico), così
  "Tutti", un gruppo e un singolo wallet sono la stessa lettura ad ampiezze diverse.
- **I costi non si ricalcolano**: i lotti si caricano con `v[17]`, che il motore delle plusvalenze ha già
  scritto. Le regole di carico/scarico (TI ignorati, PTW scaricato dal DTW di destinazione, DTW che
  scarica il gruppo di provenienza) sono quelle della PARTE 2 di `Calcoli_RT`: è la **terza** copia di
  quelle regole, dopo il motore e il quadro RT, e il javadoc lo dichiara invece di nasconderlo.
- **Il dettaglio movimento usa le stesse pile** (`GiacenzeAttornoAlMovimento`, giacenze prima/dopo su
  globale/gruppo/wallet): le regole stanno in `ElaboraLotti`, unica copia nella classe, e il dettaglio la
  percorre fino al movimento con `headMap(ID)` — per ID, non per data, come il motore — filtrata sui
  simboli del movimento. Il filtro va applicato a ogni carico/scarico e non al movimento intero, perché un
  PTW e il suo DTW possono avere simboli diversi. In `GUI_DettaglioTransazione` si vedono in due riquadri sotto la
  tabella (rosso uscita, verde entrata, `Pannello_Giacenze`), non più come righe HTML della tabella a due colonne:
  il contenuto è `Principale_GiacenzeaData.TabelleDettaglio` (solo stringhe), i `JTable` si costruiscono in codice. ⚠️
  L'altezza del pannello (215) sta **nel layout** (`.form` e `initComponents`): un `setPreferredSize` nel costruttore,
  dopo la `pack()` di `initComponents`, non ridimensiona il dialogo.

- **Colonne derivate della tabella principale** (*Valore unitario*, *Costo unitario*, *Differenza valore − costo*,
  indici 7-9): restano **vuote, non zero**, per FIAT, giacenza ≤ 0 e — la differenza — token senza prezzo, che
  altrimenti mostrerebbe l'intero costo come perdita. Il costo unitario divide il costo **esatto**
  (`CostoEsattoDelleRimanenze`), non quello mostrato a due decimali, che su un token da frazioni di centesimo
  darebbe zero. Tutto in `Principale_GiacenzeaData.ValoriDerivatiRiga`.
- **Tabella dettaglio movimenti: cinque colonne di costo in coda al model** (indici **13-17**: costo di carico del
  movimento da `v[16]`/`v[17]`, prezzo unitario, valore e costo di carico della qta residua, differenza valore − costo,
  quest'ultima vuota se manca un termine o la giacenza residua è ≤ 0). Stanno **in coda al model** perché
  `Tabelle.ColoraRigheTabella1GiacenzeaData` è condiviso con la tabella dei saldi negativi e con quella dei token SCAM e ha
  indici fissi (5, 7, 10-12), e perché selezione e popup leggono l'ID alla colonna 8 **del model**; a video si riordinano
  solo le colonne: l'ID e le altre interne (8-12) **non sono nella vista** (`LayoutColonneMovimenti.PROFILO_GIACENZE_DETTAGLIO`)
  e il costo di carico del movimento (13) sta prima di *Qta Residua*. Conseguenza: qualunque lettura da questa tabella va
  fatta con `getModel().getValueAt(convertRowIndexToModel(riga), N)`, mai con `getValueAt(riga, N)` (indice di vista), e
  `ColoraRigheTabella1GiacenzeaData` converte la colonna in indice di model. L'utente sceglie quali colonne vedere col
  pulsante *Colonne...* (stesso dialogo `GUI_ColonneMovimenti` e stessa classe `LayoutColonneMovimenti` della tabella
  movimenti, parametrizzati da un `Profilo`; salvataggio in `personale.mv.db`, chiave `GiacenzeaData_Dettaglio_LayoutColonne`).
  Colori: con `putClientProperty(Tabelle.PROP_COLORA_SOLO_QTA_E_DIFFERENZA, TRUE)` solo *Quantità* e *Differenza* sono
  verdi/rosse (per segno), il resto resta del colore normale e *Qta Residua* ha il proprio.
  - **Il prezzo unitario nullo non è un prezzo**: `v[15]` ha due decimali, una ricompensa da frazioni di centesimo vale
    `0.00` e dividerla azzerava il valore dell'intera giacenza residua (visto su ETH, 0,015 ETH a 0 euro contro 29,59 di
    costo). `PrezzoUnitarioNelMovimento` usa quindi, in ordine: `v[40]`; valore/quantità solo se il valore è ≥ 10 €; il
    **prezzo di mercato all'istante del movimento** (cache personale/prezzi ±5 min, archivio orario `XXXEUR`, movimenti
    vicini ≥ 10 €, quotazione più vicina entro ±1/6/24 h; mai la rete); valore/quantità se ≥ 1 €; altrimenti vuoto. Limite
    noto: sul dataset reale ETH non ha quotazioni locali fra 2022-02-10 (fine dell'archivio orario) e 2025-02 (inizio della
    cache al minuto): lì le ricompense minuscole restano senza prezzo.
  - **Il prezzo di mercato non si cerca durante la costruzione della tabella**: la cache dei prezzi ha la chiave che comincia
    col timestamp, quindi ogni finestra scandisce i prezzi di *tutte* le monete, e migliaia di righe valevano secondi (la
    tabella si apriva con un ritardo visibile). `ColonneCostiDettaglio` usa solo le fonti che non leggono nulla
    (`PrezzoUnitarioNelMovimento(…, false)`); le righe rimaste senza prezzo (`PrezzoDaCompletare`) si completano in
    background da `Principale.GiacenzeaData_CompletaPrezziDettaglio` (`CompletaPrezzoDettaglio`), a blocchi, scrivendo nel
    model sul thread grafico solo se `GiacenzeaData_GenerazioneDettaglio` è ancora quella di partenza e la riga è la stessa
    (ID in colonna 8). `AggiornaCoperturaPrezzi()` (una `MIN/MAX` all'inizio di ogni costruzione) evita di interrogare la
    cache per i movimenti fuori dal suo intervallo.
  - Il costo residuo è la stessa regola della principale (**passata non filtrata, lettura sì**):
    `CostiDettaglioToken.Avanza` va chiamato su **tutti** i movimenti anteriori alla data, anche su quelli che il filtro
    wallet scarta, altrimenti un giroconto interno fa sparire il costo. `CostiCaricoRimanenze` tiene i totali di ogni pila
    (`TotaliPila`) e i lotti già convertiti in `BigDecimal` (`LottiNumerici`): la lettura che copre tutta la pila — "Tutti"
    o un gruppo intero — è la somma già pronta invece di una riconversione da testo di tutti i lotti a ogni riga (su BTC,
    4.237 righe: da ~650 a ~100 ms). Le colonne di costo e quelle unitarie non si sommano nell'header
    (`putClientProperty("ColonneSenzaSomma", …)`, letta da `Tabelle.Tabelle_getSommeColonne`). Fissato da
    `lUltimaRigaDelDettaglioCoincideConLaTabellaPrincipale` e da `LayoutColonneGiacenzeDettaglioTest`.

⚠️ Le colonne derivate hanno spostato gli indici della tabella principale: *Errori* è la **10** e *InfoPrezzo*
la **11** (prima 7 e 8), sia nei lettori di `Principale` sia in `Tabelle.ColoraRigheTabella0GiacenzeaData`.

### Filtri della tabella movimenti — due meccanismi, non uno

Sulla tabella dei movimenti agiscono **due** famiglie di filtri, e confonderle porta a scrivere
codice che non funziona: i **filtri di riga** (`RowFilter` Swing — colonne dell'header, campo di
ricerca) agiscono su un modello già costruito; i **filtri di caricamento** (wallet/gruppo, token,
documento di origine, date, TI, SCAM, senza prezzo, LiFo mancante) decidono quali righe esistono e
vivono in `Principale_FiltriMovimenti.FiltriMovimenti.Passa()` — cambiarli costa un ricaricamento.

Il documento di origine **non può** essere un `RowFilter`: il modello dichiara 40 colonne ma
`Converti_String_Object` produce 46 elementi, e `DefaultTableModel.justifyRows` tronca ogni riga a
`getColumnCount()` — i campi `[40]`-`[45]` non entrano mai nel modello. `Passa()` non legge nulla per
conto proprio: riceve dati già calcolati dal ciclo di caricamento. Il dialogo `GUI_FiltriMovimenti`
scrive un unico record (`FiltriCorrenti`) e ricarica una volta sola — la barra non ha più nessun
controllo di filtro oltre a ricerca/Filtri…/Azzera Filtri.

**La scheda «Filtri» e l'imbuto dei dettagli** (2026-10-02). Nel pannello a schede accanto ai dettagli del movimento
(`TransazioniCrypto_TabbedPane`) la scheda *Filtri* elenca **tutti** i filtri attivi, uno per riga, con un bidone che toglie
*quel* filtro: i criteri della finestra *Filtri…* (il bidone ricarica la tabella), il campo di ricerca, e **una riga per ogni
colonna filtrata** (il bidone toglie tutti i valori di quella colonna). La lista è una sola
(`Principale_FiltriMovimenti.RigheCaricamento` + `RigheDiRiga`, testata senza GUI) e da lì derivano scheda, tooltip e numero del
pulsante, che non possono divergere: per questo `FiltriMovimenti.Descrizione` è costruita da `Criteri()`, e ogni colonna filtrata
conta 1 (prima tutte insieme contavano 1). Il filtro per colonna è `Tabelle.FiltroValori`, **non più un `RowFilter` anonimo**: i
valori scelti vivono solo lì, in `tableFilters`, e un secondo posto dove ricordarli verrebbe sovrascritto a ogni ricostruzione.
Chi sta dentro `Tabelle` (il popup dell'header) avvisa `Tabelle.tableFiltersListener`, che `Principale` aggancia ad
`AggiornaPulsante`: prima di questo il popup non aggiornava né la banda né il contatore. La scheda si ricompone da
`FiltriMovimenti_AggiornaIndicatori`, il punto da cui passa ogni cambio (ricarica, digitazione nella ricerca, focus alla finestra).
Le righe filtrabili dei dettagli (terza colonna *Filtra*, `RigaDettaglioFiltrabile`) filtrano la colonna di model **sul valore letto
dal model della tabella movimenti, non dal testo mostrato** (che può essere HTML o composto) e **sostituiscono** il filtro che la
colonna aveva già; l'imbuto è **pieno** (`Icone.ImbutoPieno`) quando il filtro della riga è proprio quello attivo (`FiltroRigaAttivo`: per una colonna solo se ammette *esattamente* quel valore, con più valori scelti dal popup resta vuoto) e un secondo clic lo toglie — lo stato si decide a ogni disegno del renderer, non al riempimento dei dettagli, perché i filtri cambiano da molte parti e `AggiornaSchedaFiltri` ridisegna la tabella; il documento di origine (`[41]`, fuori dal model) passa dal criterio *Documento* e quindi ricarica la tabella.
`Tabelle.java` è CRLF: le modifiche vanno fatte preservando i fine riga.

Meccanica completa (perché i due `RowFilter` non applicano nulla da soli, `GroupLayout` da
ricostruire e non modificare a righe, il difetto noto sul wallet singolo senza gruppo) in
`nocommit/Documentazione/Analisi_Filtri_Movimenti.md`.

### The movement row model — read this before touching any calculation

A movement is **not** a class: it is a `String[]` of `Importazioni.ColonneTabella` (currently **46**) columns, stored in `Principale.MappaCryptoWallet` keyed by column `[0]` (the movement ID). Every engine addresses fields positionally — `v[5]`, `v[16]`, `v[18]`, `v[33]` — so:

- Changing `ColonneTabella` or reordering columns breaks the import pipeline, the calculation engines, the tables and the CSV round-trip at once.
- `[0]` is the ID; its last `_XX` segment is the **categoria** (read by `Calcoli_PlusvalenzeNew` as `IDTS[4]`). `[5]` is the transaction-type description, `[18]` the subtype/marker.
- `nocommit/Documentazione/Analisi_Campo5_Campo18_Categoria.md` is the reference for which campo5/campo18/categoria combinations are legal and where each is produced.
- `[42]` is the **lignaggio**: a random `UUID` written the first time a movement is modified by hand, and the key of its modification history (`MOVIMENTI_STORICO`). Blank means "never modified manually". It must be **copied verbatim** by anything that rebuilds a movement — that is why it is deliberately absent from `MovimentiCrypto.CampiNonCopiabiliVerbatim` and present in `Principale_Movimenti_SeparaUnisci.CampiDaRiportare`; dropping it orphans that movement's history. See `MovimentiStorico` and `nocommit/Documentazione/Analisi_Storico_Modifiche_Movimenti.md`, section "v6".
- `[43]` is the **linked-fee group key** (`CommissioniCollegate`, 2026-09-29): one opaque value written on a fee (`CM`) **and** on the movement(s) it belongs to — a shared key, not a pointer, because `[0]` changes in half the program (Trasla Orario, Modifica Movimento, classification, scambio differito, Separa/Unisci). Written at import (DeFi gas on every row of the tx, generic CSV per row/order, OKX per bill or on the rebuilt swap, Binance/CoinTracking/ccxt). **Informative only**: no engine reads it, it is outside `Impronta` and the golden master; it exists for future derivatives, where fees are deducted — spot fees are not. Same copy rules as `[42]`: absent from `CampiNonCopiabiliVerbatim`, present in `CampiDaRiportare` and `CampiProvenienzaUnione`. Merges fuse the groups (`FondiChiavi`), deletions ask once whether to delete fees left with no movement (`CommissioniOrfane`). Transfer fees created by classification (`CM` `AU`) deliberately get no key. **Fees only**: from 2026-09-29 to 2026-10-05 the field also held the Dual Investment contract key, which fused a fee linked to the Purchase into the contract group (losing which movement it belonged to) and needed five `isGruppoDual` exceptions; the contract key now lives in `[45]`, and rows of that period are moved on load (`GruppoOperazione.MigraAllaFormaAttuale`). Existing archives are paired by hand (`Principale_CommissioniCollegate.AbbinaArchivio`: same `[24]`+`[3]` within an hour, else the **only** movement in the same second on the same `[3]`; ambiguous cases are left alone by agreement — on the real dataset that links ~79% of fees, the rest being mostly gas-only DeFi txs). Design in `nocommit/Documentazione/Analisi_Commissioni_Collegate.md`.
- `[44]` is the **derivative marker** (`Derivati`, 2026-09-29): a short type (`PNL`, `FUNDING`, `BONUS`, `COMMISSIONE`, `DUAL`...) written by the generic import from the config key `causaliDerivati` (causale → type), blank on every other movement. **Identification only**: no engine reads it, derivatives are still computed as crypto-attività — wrong on purpose until the user has studied the fiscal cases, and said so by the end-of-import warning (`Importazioni.TestoAvvisoDerivati`) and by the W/RW and T/RT prints (`Derivati.TestoAvvisoQuadro`, note key `DERIVATI`, plus "Movimenti su derivati" in the RT table's *Errori* column, which the print button does not count as an error). Do **not** put derivative markers in campo 18 before the engine handles them: `ElaboraMovimento` silently skips a DC/PC whose campo 18 it does not know. Copy rules as `[43]`. Configs keep the same causali in `causaliAllertaDerivati` too, for installed versions that ignore `causaliDerivati`. The full design for when derivatives are handled (c-quater, RT sez. II-A, RW codice 9) is in `nocommit/Documentazione/Analisi_Derivati.md`. The row was widened to 46 for `[45]` on 2026-10-05 (a known, already-handled operation: `Importazioni.ColonneTabella`, padding on load, `Backup_Compatibilita`). The load pads every older row, which sets `VersioneCambiata` and rewrites the movements file at startup with a backup, so the move of the contract keys is saved without the user pressing *Salva*. A backup made with 46 columns is refused by versions that handle 45.
- `[45]` is the **operation key** (`GruppoOperazione`, 2026-10-05): «these movements are the same operation». Today only `Binance_DualInvestment.Abbina` writes it, `DUAL-<id contratto>` (`GruppoOperazione.ChiaveDual`, id from column 3 of the detail CSV), and **only on Purchase and Settlement**. **One fact, one place** (the user's choice, 2026-10-05): the group (who is linked to whom) is `[20]`, the identity is `[45]`, so a generated movement (`AU`: mirror legs, reward, the three deferred-swap movements) never carries the key and **derives** it from the originals of its `[20]` group (`GruppoOperazione.ChiaveEffettiva`; none if they carry different keys). It is one of the inputs of the **computed operation** (`OperazioniCalcolate`, see the paragraph after this list), which is what column 42 and the movement detail show. For a few hours on 2026-10-05 the key was propagated to generated rows instead, and could be left incomplete by a manual reclassification (3 of 5): the load removes keys on generated rows (`GruppoOperazione.MigraAllaFormaAttuale`). It is where saved groups of other operations will go. It is separate from `[43]` on purpose (see above): a fee keeps its own link to its movement, and the movement also carries the contract identity, no fusion. It is **not** `[20]` either: the engine reads `[20]` to pair PTW/DTW (`Calcoli_PlusvalenzeNew`), `Impronta` hashes it, and undoing a classification cleans every ID listed there. **Informative only**, outside `Impronta` and the golden master. It is an **identity**: it stays on Purchase/Settlement when their classification is undone (re-passing the file finds them by it). Copy rules as `[42]`/`[43]` (`CampiDaRiportare`, `CampiProvenienzaUnione`), a duplicate drops it. **Filtering:** the table model stops at `v[39]` plus derived tail columns, so operations are shown through the derived model column **42 "Operazione"** ("Gruppo Collegato" until 2026-10-05; hidden by default, offered in *Colonne...*; `z[42]` = the computed operation key in the loading loop; like column 41 "Alias Gruppo Wallet" which is not `v[41]` either) — the search box matches every model column, hidden ones included, so typing `DUAL-` or a contract id finds the whole contract, and the column's header filter selects one operation exactly. `LayoutColonneMovimenti.COLONNA_MASSIMA` is 42; any new tail column needs `Principale.form` (the `<Column>` entry **and** the `columnCount` attribute of `<Table>`: left at 42 with 43 columns, the GUI Builder refuses to load the model), **all three** arrays of the model in `initComponents` (header names, `types`, `canEdit` — forgetting `types` made `getColumnClass` throw `Index 42 out of bounds` and the app did not start, which no unit test sees: launch the app on the virtual display after touching them), the loop and that constant. Contracts matched before 2026-10-02 have no key, and re-passing the detail file fills it in: `Abbina` also accepts Purchase/Settlement that are *already* matched, recognised by campo 18 (`PTW/DTW - Trasferimento a/da Dual Investment` same coin, `PTW/DTW - Scambio Differito` different coin) and by the quantity they carry *after* matching (same coin: the Settlement holds the whole settled amount, or only the capital if matched before 2026-10-05), and on those only writes the key (`MarcaContratto`), after converting/repairing them (see the Dual paragraph under "Import pipeline") — never re-runs the matching, which would double the mirror legs. A candidate keyed to another contract is excluded, one keyed to this contract wins; a half-matched contract is reported and left alone.
- **Duplicating a movement** (`Funzioni.DuplicaMovimento`, *Duplica* in Depositi/Prelievi) makes an independent
  movement: no `[42]` lignaggio (it used to share the original's history chain), no `[43]`/`[45]`, but it keeps a
  single-movement classification and `[41]`. A member of a classified group (`[20]` not blank) or a generated `AU`
  row is refused (`Funzioni.isDuplicabile`): the copy would claim a group that does not cite it, and a cross-group
  DTW copy would move the cost basis a second time.
- Short rows are padded to 46 and blank-filled via `Importazioni.RiempiVuotiArray()` on load.

**The operation of a movement is computed, never stored** (`OperazioniCalcolate`, 2026-10-05, user's choice). Two
movements are in the same operation if linked, even through others, by `[20]` (classification group), `[43]` (fee
link), `[45]` (identity) or the same `[24]` on the same `[3]` within an hour (non-`AU` rows, values of at least 4
characters — `-` and `_` are used as placeholders by the thousand on real data — and at most 20 rows per value).
Shown with the `[45]` identity of a member if any (`DUAL-…`), else `OP-` + the first member's ID: display and filter
only, recomputed at every table load, never to be saved or cited (that ID changes when the movement does). Computed
on the **whole** map, never on the filtered rows, in `TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa` (only
when the rows are built) and in `TransazioniCrypto_AggiornaColonnaGruppoCollegato`. Union-find on the row objects
(identity), with `[20]` IDs looked up in the case-insensitive map: a first version keyed on lowercased ID strings cost
216 ms on 102.000 movements, this one 61 ms (5 ms on the user's 24.000). Nothing stored means the past is covered,
undoing/deleting/unlinking dissolves operations by itself, and no engine is affected. The movement detail
(`OperazioniCalcolate.RigheDettaglio`) lists the other members with type and classification, then the fee rows;
it replaced the bare `[20]` IDs ("Movimenti Correlati").

**Never decide the direction of a movement from a `-` inside the quantity string.** Quantities are stored as strings and `BigDecimal.toString()` spontaneously emits scientific notation for small values (`2.5E-9`, common on tokens with many decimals), so `Qta.contains("-")` reads a **positive** quantity as an outgoing one. Since campo 5, campo 18 and the categoria are derived from that in/out split, the effect is a reversed fiscal classification — bug **M7**, fixed on 2026-07-31 across `MovimentiCrypto.creaMovimento`, `TransazioneDefi` and `Importazioni`. Use `Funzioni.isNegativo(String)`, which tests `new BigDecimal(v).signum() < 0` and intentionally falls back to the old textual test for non-numeric values. For the same reason `Moneta.InvertiQta()` only flips the leading sign and must never `replace("-","")`, which would also delete the exponent's sign and turn `1.5E-8` into `1.5E8`.

**Line endings are mixed**: about half the `.java` files (27 of 54, `Importazioni.java` and `Funzioni.java` among them) still use **CRLF**, the more recent ones LF. A scripted or regex-based edit that rewrites a whole file will silently convert them and produce a diff of thousands of lines. Preserve the file's existing endings, and check `git diff --stat` before committing.

### Calculation engines
- `Calcoli_PlusvalenzeNew.java` — LIFO stack capital gains engine; categorises each movement into one of 10 fiscal types and computes gain/loss and cost basis

**`AggiornaPlusvalenze()` is incremental** (2026-08-11): it fingerprints every movement, finds the
smallest key whose data changed, and replays from the closest checkpoint at or before it — nothing
outside the engine has to declare a modification. **The one list to maintain by hand is
`OpzioniRicalcolo.Epoca()`**: everything the engine reads that is *not* in a movement row (personal
options, `Mappa_Wallet_Gruppo`, `Mappa_EMoney`, `DettagliLifoCompleti`) — adding an option to the
engine without adding it there produces silently stale results. Replay always runs to the **end** of
the map, so cost is proportional to how far back the change is. On exception the `try/finally`
discards fingerprints and checkpoints, so a transient failure cannot become permanent. Full mechanics
(the `v[20]` digest for PTW/DTW ordering, which fields survive a full recompute unchanged) in
`nocommit/Documentazione/Analisi_Ricalcolo_Incrementale_Plusvalenze.md`.
- `Calcoli_RW.java` — Quadro W/RW (annual wealth declaration) calculations
- `Calcoli_RW_Giacenze.java` — the **only** year-start/year-end crypto holdings snapshot (per wallet group, date window, injectable price): feeds both Rilevanza A (`Calcoli_RW.ChiudiRWGiacenzeFinali`) and the bollo-group substitution (`Funzioni.RW_GiacenzeInizioFineAnno`, now a delegate). Until 2026-09-26 those were two implementations that disagreed; don't reintroduce a second one. Rules and history in `nocommit/Documentazione/Analisi_QuadroRW_Crypto_Periodi.md` §8
- **CRYPTO rows are split by holding period** (`GRUPPO_PERIODO_RW` rows with `TipoRigo = CRYPTO`, since 2026-09-26): `Calcoli_RW_PeriodiCrypto.trattiCrypto` cuts the year at every period boundary (gaps = rows without a period, group bollo flag); for methods B/C/D `Calcoli_RW.AggiornaRWFR` closes every open LIFO lot of the group at 23:59 of the day before a boundary and reopens it at 00:00, priced at the boundary, so **no detail row ever spans two periods** and a detail row belongs to the period containing its end date `[9]`. The summary-table key is `Wallet NN|CRYPTO|` for an unsplit group (unchanged) and `Wallet NN|CRYPTO|yyyy-MM-dd/yyyy-MM-dd` for a period row. A group without CRYPTO periods produces exactly the old single row (verified on the real dataset). Decisions in `nocommit/Documentazione/Analisi_QuadroRW_Crypto_Periodi.md` §6
- `Calcoli_RW_Fiat.java` — the FIAT half of Quadro W/RW (foreign currency held at a foreign intermediary), into the separate map `Principale.Mappa_RW_ListeXGruppoWallet_Fiat` so the CRYPTO path — and its golden master — stays byte-identical
- `Calcoli_RT.java` — Quadro T/RT (capital gains tax form) calculations

### I periodi di detenzione del quadro W/RW

`GRUPPO_PERIODO_RW` (`personale.mv.db`) è la tabella unica per come un gruppo wallet ha detenuto
crypto e valuta estera nel tempo, un rigo per periodo, `TipoRigo` CRYPTO o FIAT. Logica in
`Principale_GruppiWalletRW`, GUI in `GUI_PeriodiDetenzioneRW` + `GUI_ModificaPeriodoDetenzione`. I
dati fiscali dell'intermediario (Stato estero, identificativi, conto corrente) vivono **solo** sul
rigo FIAT; una data di inizio mancante non significa più "dal primo movimento" ma si risolve da
`finestreEffettive` contro gli altri periodi. Il pulsante *Salva* non c'è più: ogni operazione
(aggiunta, modifica, rimozione, ripristino) scrive subito via `salvaAdesso()`. Dettagli completi
(schema storico a 4 tabelle, `IdentificativoISEE`, `intervalliFiat`, `StatiEsteri.CODICE_ITALIA`,
meccanica IVAFE sul conto corrente estero, eccezione Crypto.com App per la parte FIAT, validazione
di `salvaAdesso`) in `nocommit/Documentazione/Analisi_QuadroRW_Periodi_IVAFE.md`.

**Codice bene 14 (liquidità)**: l'IVAFE ordinaria 0,20% si liquida oppure il rigo resta "solo
monitoraggio", a scelta dell'utente (`Calcoli_RW_Fiat.OPZIONE_LIQUIDITA_SOLO_MONITORAGGIO`, opzione
in *Opzioni → Opzioni Calcolo RW/W*) — non più una scelta silenziosa del programma. L'aliquota è il
4‰ per gli Stati "privilegiati" del D.M. 4/5/1999 dal 2024 in poi, altrimenti 2‰; la barratura della
colonna 21 sul modulo stampato resta manuale. Analisi legale e di implementazione complete in
`nocommit/Documentazione/Analisi_QuadroRW_Codice14.md`.

**Rigo di liquidità spezzato sugli apporti (circ. 12/E/2016 § 14.1)** e, a scelta, sui prelievi:
`Calcoli_RW_Fiat.OPZIONE_SPEZZA_SU_APPORTI/PRELIEVI` + soglia (default spente, 500 €). Un apporto è
deciso dalla **natura** della gamba (`NaturaGamba`: entrata di un `DF`, `viban_deposit` del Fiat Wallet),
mai dal segno — una vendita di crypto porta euro ma non è un apporto. Il motore ignora i tagli in solo
monitoraggio e sui tratti conto corrente, qualunque cosa dica la GUI. Meccanica nella sezione finale
dello stesso documento.

**Le due giacenze medie della parte FIAT hanno divisori diversi, e non è un doppione da unificare.**
Conto corrente (codice 1): somma dei saldi / **365** sempre (`GIORNI_GIACENZA_MEDIA_ANNUA`, provv. AdE
28/05/2015 punto 3.1), perché l'IVAFE è fissa e la media decide solo colonna 8 e soglia dei 5.000 €.
Liquidità codice 14 con l'opzione max-media: somma / **giorni del tratto** (`mediaPeriodo`), perché la
media diventa la base di un'imposta proporzionale già rapportata ai giorni — con /365 i giorni
contavano due volte e spezzare un rigo faceva pagare circa la metà (corretto il 2026-09-27).

### Le note di compilazione dei quadri stanno in un JSON, non nel codice

I testi stampati in coda ai quadri W/RW e T/RT vivono in `config/varie/NoteCompilazione.json`,
letti da `NoteCompilazione.java`. Sono istruzioni fiscali: cambiano con la modulistica, e finche'
stavano nei blocchi di testo di `Principale`/`Stampe` correggere una riga sbagliata voleva dire
pubblicare una versione. Il file segue la strada delle mappe causali e di `TipiOKX` — copia di default
nel jar (`config/varie/` **e'** la sorgente del `/Varie/` del jar, vedi il `<resource>` del pom; non sta
in `config/importmappe/`, che e' solo per le mappe dell'import), installata al primo avvio da
`MappeCausali.InstallaDefaultSeMancanti()` e riallineata a ogni apertura da
`Funzioni.AggiornamentoConfigDaRepositoryUnicaChiamata`. Il nome e' in `MappeCausali.FILE_VARIE`; la
strada disco→jar e' la stessa delle mappe, scelta con l'enum `MappeCausali.Cartella` (`MAPPE`/`VARIE`).
Quattro cose non ovvie:

- **Gli anni della dichiarazione sono segnaposto**, `{anno}` / `{annoPrec}` / `{annoSucc}`, risolti alla
  stampa. Scritti come cifre il file sarebbe da riscrivere ogni gennaio, cioe' esattamente il problema
  che si voleva togliere. Restano cifre vere gli anni che citano una **regola** (l'entrata in vigore del
  regime nel 2023, gli anni delle minusvalenze riportabili): quelli non seguono l'anno della
  dichiarazione. Tre note ricevono anche un valore calcolato dal chiamante (`{righi}`, `{totale}`).
- **Una nota puo' avere delle varianti datate, e valgono «da quell'anno in avanti».** Oltre alla chiave
  nuda il file accetta `W8.2024`, `T.2025`: `NoteCompilazione.ChiaveApplicabile` prende la variante piu'
  recente che non superi l'anno d'imposta e ripiega sulla chiave nuda, che e' quindi il testo valido
  *prima* della prima variante. Sotto `ANNO_PRIMA_VARIANTE` (2023) non si cerca: li' comincia il regime
  delle cripto-attivita' e per gli anni prima il programma avverte gia' che il report puo' sbagliare.
  Il ripiego e' a cascata e non «anno esatto o niente» perche' altrimenti la chiave nuda dovrebbe essere
  due testi insieme — quello degli anni piu' vecchi della prima variante e quello degli anni piu' recenti
  dell'ultima — e le annualita' in cui l'istruzione non e' cambiata andrebbero ricopiate tutte.
  ⚠️ Conseguenza: correggere solo il 2024 richiede **due** chiavi, `W8.2024` con la correzione e
  `W8.2025` col testo di prima, altrimenti la correzione si porta dietro anche gli anni successivi.
  **Per questo il quadro T/RT non si sceglie piu' con un `if (Anno<2025)` nel codice**: quel bivio era la
  coppia `T_ANTE_2025`/`T_2025`, diventata `T` + `T.2025`, e i quattro involucri `Stampe.NoteCompilazione*`
  sono spariti con lui. Un secondo meccanismo che fa la stessa cosa e' solo il posto da cui i due
  divergono.
- **Il ripiego e' visibile, non silenzioso.** `MappeCausali.CaricaConRipiego` prova disco poi jar; se
  manca anche quella, o manca la chiave, `Testo()` restituisce un avviso che finisce **nel report**. Una
  nota che sparisce in silenzio da un documento fiscale e' peggio di una che dichiara di mancare.
- **Il blocco "OPZIONI SCELTE PER IL CALCOLO" e' rimasto nel codice**, di proposito: non e' una nota ma
  un resoconto, si compone dalle caselle spuntate e dall'elenco E-Money dell'utente. Nel file sarebbero
  finiti frammenti che senza il codice non significano nulla.
- **`FontApplicazioneTest` non copriva queste stringhe nemmeno prima**: quel test salta i blocchi di
  testo proprio perche' sono le note fiscali, che finiscono nel PDF e contengono frecce a decine (che
  infatti non si stampano, coi font base-14 di OpenPDF). Spostarle non ha tolto una garanzia; al suo
  posto c'e' `NoteCompilazioneTest`, che verifica chiavi, assenza di orfane e segnaposto risolti.

**La direzione e' questa**: a regime le cose piccole si gestiscono da file di configurazione su GitHub
aggiornati all'avvio, e il versionamento resta per correzioni di bug e nuove funzioni. Quando serve
aggiungere un file di questo tipo, il giro e' quello descritto qui sopra — non inventarne un altro.

### Movement types (`MovimentiCrypto.java`)
Defines the canonical movement type map. Each raw label from an import (e.g. `STAKING REWARDS`, `DUST-CONVERSION`) is normalised to a short code (e.g. `RW`, `SC`). Understand these codes before touching import or calculation code.

**Whenever a code change adds or modifies a campo5/campo18/categoria combination** (new movement classification, new marker, new auto-assignment rule, etc.), update `nocommit/Documentazione/Analisi_Campo5_Campo18_Categoria.md` to document the new combination in the same style as the existing tables/notes.

### Import pipeline
- `Importazioni.java` — dispatcher and shared utilities; routes each file type to a handler
- `ImportazioneGenerica.java` — JSON-configured import for arbitrary CSV formats (config files in `ImportConfig/`)
- `Importazioni_Gestione.java` / `Importazioni_Resoconto.java` — Swing dialogs wrapping the import flow
- Exchange-specific classes: `CDC_FiatECardWallet.java`, `BinanceTaxReportClient.java`, `CcxtInterop.java`
- DeFi/blockchain: `Funzioni_WalletDeFi.java`, `TransazioneDefi.java`, `Trans_Solana.java`, `ERC20MetadataReader.java`

**BTC multi-address wallets (`WalletBtcMultiIndirizzo`, 2026-09-28).** A BTC entry in `WALLETS` is
one of three kinds, decided by the string itself: an extended key (addresses derived), a single
address, or a **name** whose addresses live in `WALLET_BTC_INDIRIZZI` (`personale.mv.db`) — resolved in
`Trans_Bitcoin.IndirizziDaScansionare`. `Trans_Bitcoin.isNomeWalletValido` is what keeps a name from
being mistaken for the other two (`isExtendedKey` only checks the prefix). Grouping at import, not via
`GRUPPO_ALIAS`, is the point: `analizza` classifies in/out against the address *set*, so per-address
imports turn change outputs into fake withdrawals + deposits. Changing the list of an already-imported
wallet re-reads only the history of the added/removed addresses — only transactions touching them can
change meaning — and only **up to the wallet's highest imported `v[23]`**: adding a newer transaction
would move the incremental import's restart point past other addresses' unread ones. A transaction that
touched the old set but has no rows was deleted by the user and must not be resurrected. The plan is
computed from `Trans_Bitcoin.Analisi` without building `TransazioneDefi`, because `InserisciMonete`
already fetches prices. Known gap: the address list is persisted immediately while movement changes
wait for *Salva*, so discarding them leaves list and movements out of step.

**In a generic-CSV config, `nomeExchange` becomes movement field `[3]` and `nomeWallet` field `[4]` — and `[3]` is load-bearing twice over.** `costruisciMovimenti` calls `creaMovimento(mOUT, mIN, exchange, wallet, …)` with `exchange = nomeExchange` in the `Wallet` slot: `[3] = nomeExchange`, `[4] = nomeWallet`. Field `[3]` is then the exchange key in the re-import dedup (`Importazioni.F_buildKeyMovimento` = `yyyyMMddHHmmss|[3]|monetaU|qtaU|monetaE|qtaE`, the instant to the second taken from the ID, not the day) **and** the key of the fiscal wallet-group lookup (`Calcoli_PlusvalenzeNew` reads `DatabaseH2.Pers_GruppoWallet_Leggi(v[3])`). So changing a config's `nomeExchange` re-imports every already-stored movement of that source as a duplicate (different `[3]`), and moves those movements into a different wallet group unless a `GRUPPO_ALIAS` re-unites them. `nomeWallet` (`[4]`) is fiscally inert — like the OKX note above. The two `config/import/Coinbase*.json` use this on purpose: retail keeps `nomeExchange` "Coinbase", `Coinbase Pro GDAX.json` v2.000 sets it to "Coinbase Pro" so GDAX is a distinct wallet/group, and the retail↔Pro giroconti (`Pro/Exchange Deposit/Withdrawal` → `TRASFERIMENTO-CRYPTO`, and the GDAX `deposit`/`withdrawal` in crypto → auto `DC`/`PC`) are left with blank `campo18` for manual *Classifica Movimento* pairing. That config also drops the old `type.unit` composite causale (`causale2`/`separatoreCausale`): one bare causale per `type`, EUR-vs-crypto on `deposit`/`withdrawal` resolved by `RitornaTipologiaTransazione` from the coin's FIAT/Crypto type. Pinned by `ImportazioneGenericaCoinbaseProGdaxTest`.

**Binance Simple Earn rewards: what the export contains and what it doesn't** (2026-10-06, verified on the user's data
and on the app's *Earn – Flexible – Rewards* history). Every daily **Bonus Tiered APR** is in the Spot account as
`Simple Earn Flexible Interest` / `Simple Earn Locked Rewards` and, in exports from 2026, a second time in the Earn
account as `… - Rewards Income`, same coin and amount, 0-2 h earlier: `Binance CSV.json` ≥ 1.017 sets the Earn rows to
IGNORA (reason in `_commentoRewardsIncome`), and `RicompenseEarnDoppie` removes those already imported (startup warning
with "non mostrare più", `RicompenseEarnDoppie.OPZIONE_NON_MOSTRARE`, and *Opzioni – Pulizie – Ricompense Binance
doppie*). The **Real-Time APR** rewards are in **no** export row: they accrue inside the product and come out only in
redemption amounts, which the import ignores (subscriptions/redemptions are IGNORA, Spot and Earn are one pool), so an
archive is short of them and a full withdrawal shows negative balances. ⚠️ A balance check alone is misleading here: the
doubled Earn rows partly filled that gap, which is why they first looked like real credits. What decides is a stretch
between two full withdrawals (product empty at both ends): redemptions minus subscriptions there is the hidden
Real-Time APR (Feb-May 2025: 1.07 USDC, with no Earn row at all). The Real-Time APR come from a **separate file** downloaded from the
app (*Assets – Earn – Flexible – Rewards*, download icon: `Time,Coin,Amount,Type`), imported by
`config/import/Binance Simple Earn Flexible.json` (Real-time → EARN, Bonus → IGNORA). Its Real-time rows have the
date only: the generic import's `oraSeSoloData` (2026-10-06, `ConfigurazioneImport.oraSeSoloData`, used by
`parseDataRaw` only when the full `formatoData` fails) sets them at 00:00:00 UTC — not end of day, which is already the
next day in Italian time and would move the 31/12 reward into the next tax year. Verified on the user's archive: 443
rows, 12.96 USDC in Jan-Jun 2026, re-import adds nothing, the USDC gap drops from 23.64 to 10.69 (the 2025 Real-Time
APR, outside that file).

**`girocontoFiat` — euro moved between two of the user's own wallets (`GirocontiFiat`, 2026-09-28).**
The GDAX EUR `deposit`s are not direct SEPA transfers: each one is the other half of a retail
`Exchange Deposit`/`Pro Deposit`. Left alone, the pair is an unrelated PF + DF, so a "deposito FIAT"
filter double-counts the money and `Calcoli_RW_Fiat` reads both halves as apporto/prelievo. At the end
of every generic import, `GirocontiFiat.Abbina` scans the **whole** archive and pairs them as PTW/DTW,
through the manual classification function. The categoria stays PF/DF so the balance really moves
between wallets. The rule is declared **mirrored in both configs**, so import order doesn't matter and
re-importing either file fixes existing data. A pair needs both raw causali `[7]` in the declared sets:
that is what keeps a bank `Deposit` out. `Calcoli_RW_Fiat.isGirocontoStessoGruppo` makes the legs
INTERNA only when the counterpart is in the **same** wallet group. Towards another group the euro
really enters another rapporto, so there it stays an apporto. The same pairing can be done by hand:
`GUI_ClassificazioneMovimento` offers PF/DF only "nessuna selezione" and the giroconto, and refuses a
deposit larger than the withdrawal, which would otherwise become a euro reward. An unclassified PF/DF
is still **not** counted as an error: the counter uses `isDepositoPrelievoClassificabile(…, false)`.
Pinned by `GirocontiFiatTest`.

**`colonneControvalore.noteAcquistoDaSaldo` (`Coinbase CSV.json` ≥ v1.006) — Buy pagati con carta non devono movimentare euro.** For a causale in `causaliConMovimentoCommissione` (only `Buy`), `costruisciMovimenti` normally synthesises a FIAT-out leg of `Total − fee` (→ categoria `AC`) plus a separate `COMMISSIONI` movement. When `noteAcquistoDaSaldo` is set and the row's Notes are non-empty and contain **none** of its markers (`"EUR Wallet"`), the Buy was paid from outside the exchange balance (card, Apple/Google Pay, direct bank): both the synthetic FIAT leg and the `COMMISSIONI` movement are skipped, and the lone crypto-in movement has its ID categoria rewritten `_DC`→`_AC` with `campo5="ACQUISTO CRYPTO"` and **`campo18` left empty** — the exact shape the Crypto.com App importer already produces (`Importazioni.java`, "Forzo il fatto che sia un acquisto crypto"). `Calcoli_PlusvalenzeNew`'s `v[18].contains("DAC") || TipoID.equals("AC")` branch then loads it at full cost `campo[15]` (= `Total − fee`), plusvalenza 0, no EUR moved. Empty Notes → unchanged (synthesised FIAT leg). Pinned by `ImportazioneGenericaCoinbaseTest`.

**`coppia` — a trading pair in one cell (`KuCoin Spot Trades.json`, 2026-10-01).** When the export has no coin
column, only `KCS-ETH`, `"coppia": {colonna, separatore, base, quote}` makes `ImportazioneGenerica.leggiCSV` append two
**virtual columns** to every row at the `base`/`quote` indices (pick them past the CSV's last real column), which the config then
uses as ordinary `moneta`/`monetaUscita`. It is done at file-read time, not inside `costruisciMovimenti`, because grouping, price
pre-collection and `causaliScambiaGambe` all read the coin from the row. A row whose cell has no separator is discarded
(`COPPIA NON VALIDA`). Also worth knowing from the KuCoin exports: a withdrawal's `Amount` is the **net** and the fee is on top
(the opposite of Gate.io), partial fills of one order are several identical rows and all of them enter on the first import (dedup
is against the archive, not within the batch), and the Futures export has no config because it carries no PnL. Pinned by
`ImportazioneGenericaKuCoinTest`.

**`fusoDaIntestazione` — the file states its own time zone (OKX ≥ Funding 1.002 / Trading 1.003, 2026-10-07).** Recent OKX
exports carry `Time Zone:UTC+8` in the first line: the zone chosen on OKX at export time, not necessarily Italy's (a real
user's Funding was UTC+8 next to a Trading in UTC+2, and the fixed `Europe/Rome` shifted it by 6 h). With the key set,
`leggiCSV` replaces `fuso` with the declared offset, **after** the per-file choice of `Importazioni_Gestione`, so it wins over
both — unless it is one of the offsets the config's zone takes during the year (`ConfigurazioneImport.FusoCompatibile`):
`UTC+2`/`UTC+1` keep `Europe/Rome`, which knows about DST. Whether OKX writes a fixed offset or today's offset for winter
rows is **not verified** (no winter export available). Exports without the line keep the config's zone. Pinned by
`ImportazioneGenericaOkxFusoTest`.

**`tipoSeSola` — a causale that is half of a pair the file doesn't link (2026-10-03, Bybit Spot ≥ 1.002).** Bybit
2022 writes the BIT committed to a Launchpad (`commitmentForLaunchpad`) and the token received
(`airdropAssetIncrease`) as two unrelated rows within 1 s, but the token can also arrive alone and then it is a genuine
airdrop. A rule `{causale, con:[partners], tipo}` gives the row `tipo` only when **no other row of its group** has a
causale in `con`; otherwise the normal `mappaCausali` type applies (here `SCAMBIO CRYPTO-CRYPTO`, so the pair becomes one
swap through `causaliDifferite`'s 1 s window). The decision is per **group**, made by
`ConfigurazioneImport.tipoMovimentoNelGruppo`, which `consolidaGruppo` uses instead of `tipoMovimentoPerRiga` — the same
type is then passed to `costruisciMovimenti` as `tipoForzato`, never recomputed. The fallback type must be in
`causaliChiuse` (AIRDROP is), or the lone row is swallowed by the swap accumulator. Not a pairing engine: it only sees the
rows `raggruppaRighe` already put together, so a pair further apart than the tolerance stays two rows.

**`walletSpecularePerCausale`** emits a mirror leg (same coin, inverted quantity via
`Moneta.InvertiQta()`, never `replace("-","")`) for causali that move a coin into an exchange
sub-compartment and return it later with a yield. The counterparty is the same wallet group on
purpose — the transfer must **not** touch the LIFO stack. The causale must also be in `causaliChiuse`,
or a multi-row group loses everything: **`causaliChiuse`** decides whether two legs of a group merge
into one movement or stay split, independently of `raggruppaRighe`'s row grouping
(`TRASFERIMENTO-CRYPTO` is closed, `SCAMBIO CRYPTO-CRYPTO` is not). It is **not** what Binance Dual
Investment uses today — those stay plain `TRASFERIMENTO-CRYPTO` (categoria PC/DC, campo18 blank),
abbinati by hand or via `Binance_DualInvestment.Abbina()` from the "Advanced Earn - Dual Investment
History" detail CSV, since that file carries a contract id the normal transaction export doesn't. Any
config can list causali in `causaliAllertaDerivati` (same raw-causale matching as `causaliDifferite`)
to raise a fiscal disclaimer in `Importazioni_Resoconto` at the end of import — the program doesn't
compute derivative income (art. 67 c-quater TUIR) and treats these movements as crypto-attività, which
the warning states is not correct. `causaliDerivati` (causale → type) also marks them in `[44]` and
implies the warning. Full mechanics in `nocommit/Documentazione/Analisi_Import_Meccanismi.md`.

**`Binance_DualInvestment.Abbina()` treats a matched contract differently depending on whether the
settlement coin equals the subscribed coin.** Different coin (e.g. subscribed USDT, settled BTC) is a
genuine permuta and goes through `GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito`
(five synthetic movements, both endpoints renumbered). Same coin — the common case, principal +
interest settled back in the subscribed currency — would make that same swap permute the *entire*
settled amount instead of only the excess, so it goes through
`Binance_DualInvestment.CreaMovimentiDualInvestmentStessaMoneta` instead: Purchase and Settlement keep
their own IDs **and their imported quantity and value** (only campo 5/18/20 change, not renumbered), the
subscribed quantity enters the `Dual Savings` sub-wallet, the exact excess enters it too as an
independent `REWARD` (priced on the Settlement's per-unit value already computed at import, no new price
lookup), and the **whole** settled amount leaves it (PTW/DTW markers, same wallet group ⇒ invisible to
the LIFO stack, see the "costo di carico" note above). All movements of the contract form **one
symmetric `[20]` group**, so undoing or deleting any of them restores Purchase and Settlement exactly as
imported, and re-passing the file re-matches. This is the form since 2026-10-05 (bug C18): before, the
Settlement was **reduced** to the capital and the reward sat on the main wallet, with one-way `[20]`, so
undoing from the Settlement lost the reward and the reduced Settlement no longer matched its CSV row in
the re-import dedup. Re-passing the detail file converts old-form contracts (`AggiornaFormaStessaMoneta`,
engine results unchanged, verified on a real dataset) and rebuilds a different-coin contract whose swap
was overwritten by bug C17 (`MovimentiCollegati.RiparaScambioDifferito`: only the loser's own rows are undone and redone,
with a rollback that copies the old content back into the same arrays). Both work on the rows alone, no
file needed, so another trigger can reuse them. A contract settling below the subscription in the same
coin is not matched. In the sub-wallet the reward's ID (`00` before the third segment) sorts before the
exit's (`0`), so the per-ID negative-balance check never sees it below zero. Unlike the generic Vault mechanism, the reward here is
exact from the contract's own two quantities (known from the detail CSV), not inferred from an
aggregate sub-wallet balance — deliberately **not** reusing `CreaMovimentoTrasferimentoA/Da` themselves,
to avoid risking their aggregate-balance/hash-based-reward logic for a case they were never designed
for.

**Bybit "Withdraw & Deposit History" (`Bybit_DepositiPrelievi`, 2026-09-30) enriches, it never
imports.** Every row is already in the Spot/Funding Asset Change Details as a PC/DC to classify; the file
only adds hash `[24]`, address `[30]` and the chain **in the note `[21]`** — never in `[34]`, which is the
token's network and part of the coin's identity (lots, prices). Blank fields only, so re-reading is a
no-op; unique candidate or nothing, filtered on the raw on-chain causali in `[7]`. Withdrawals are gross
in Spot and net in this file: the network fee is deliberately **not** split off here, because classifying
the transfer already creates the `CM` `AU` from the difference. Design in
`nocommit/Documentazione/Analisi_Bybit_DepositiPrelievi.md`.

**"Scambio differito"** (`SCAMBIO DIFFERITO`) recognises a withdrawal and a deposit on independent
CSV rows, after the whole import is written, as the two halves of one exchange happening "behind the
scenes" (Auto-Invest, Token Swap). `Importazioni.ConsolidaMovimentiDifferiti` matches them within a
configurable time/value tolerance and splits the pair into five movements through a synthetic
platform, priced at the deposit date. The "already exists" dedup check must run **before** writing,
not after, or the automatic pairing never fires on a normal import. The swap (MS) and the exit leg (MT2)
take the **deposit's** timestamp and the **withdrawal's** ID tail, so two swaps settled in the same second
used to get the same ID and the second silently overwrote the first (bug C17, two Dual contracts lost their
permuta on a real dataset). `GUI_ClassificazioneMovimento.IDScambioDifferito` now computes all five IDs
**before** removing the originals and makes the three generated ones unique with `getIDUnivoco`; if it
cannot, `CreaMovimentiScambioCryptoDifferito` returns `false` having touched nothing, and every caller must
check it. Because undoing a deferred swap **renames** its endpoints, never remove several movements with a loop
over IDs collected beforehand: the deposit, renamed by the removal of its withdrawal, was skipped and survived
(fixed 2026-10-05). Use `Funzioni.RimuoviMovimenti`, which collects the row objects and reads each one's current
ID at removal time (identity, not content), and also queues the history deletion of its lignaggio; the movements
table, the SCAM token tab and `CancellaMovimentazioniXWallet` use it (the callers of the latter used to call it
twice as a workaround). The BTC address-set re-read keeps its own loop on purpose: it carries the lignaggio over
to the rebuilt rows. The same renaming made a re-import with «sovrascrivi esistenti» add both endpoints again (the
file's ID no longer matched): `Importazioni.InserisciMovimentosuMappaCryptoWallet(…, Sovrascrivendo=true)` now also
looks for the renamed form (`IDRinominatoScambioDifferito`, only if campo 18 confirms a deferred-swap endpoint),
undoes the swap like any other overwritten classification and puts the file row in its place. **Only when
overwriting**: every other caller has already made the ID unique, so a renamed match there would be a different
movement that merely shares the original ID. Overwritten classifications are counted
(`Importazioni.ClassificazioniAnnullate`, carried by `Esito` across files) and reported in the import summary.
**A re-import must recognise a movement that no longer has the file's form** (`FormeImportate`, 2026-10-06): a
withdrawal whose quantity a transfer classification changed (generated fee `CM` or reward `RW`), a movement moved by
Trasla Orario or edited by hand (other instant, ID or quantity), a movement imported by an older version with another
ID format (`_1_1_RW` vs `_001_001_RW`). `FormeImportate.FormePrecedenti` lists the earlier forms: the withdrawal before
classification (`GUI_ClassificazioneMovimento.PrelievoPrimaDellaClassificazione`, whose arithmetic `GeneratiDaCompensare`
is shared with the undo, so they cannot diverge) and every version in the modification history (`MovimentiStorico.Versioni`,
unsaved buffer included). Without «sovrascrivi» their keys join the dedup set (the key holds the instant **to the
second**, so even a 1 s shift used to re-import the row). With «sovrascrivi», when the file row's ID is not in the map
(nor its deferred-swap renamed form), `FormeImportate.Indice`, built once per import in `ScriviListaSuMappaCrypto`,
finds the row to replace by earlier ID, then by key of any form; each archive row goes to one file row only (partial
fills with identical keys), generated `AU` rows are never taken, and rows that some file row finds by its own ID are
excluded up front. «Sovrascrivi» overwrites everything by the user's decision (2026-10-06): the file row replaces the
moved or hand-corrected movement and undoes the classifications of what it replaces. A replaced movement that has a
lignaggio hands it to the file row, with a history entry (`MovimentiStorico.OP_SOVRASCRITTURA`) holding the replaced
row, so *Versioni precedenti* shows what the overwrite discarded (user's choice, 2026-10-06; before, the history was
orphaned). Only with «sovrascrivi», and never over a file row that already carries a lignaggio. Limit: the lignaggio exists
since 2026-09-16, so a movement edited before has no history and still re-enters (on the user's 2026 archive 555 `M`
rows, none with lignaggio; one of them, edited by 1 s, is a real case). Already-affected archives are found by the fifth Errori counter and repaired by
`MovimentiCollegati.RiparaScambioDifferito` (also called when the Dual detail file is re-passed). The two renumbered originals (`00`/`04`) are deliberately not made unique: undoing the
classification strips the prefix expecting the original ID back.

**Field `[32]`** ("prezzato") has three states, not two: `"SI"` priced, `""`/`null` needs
re-checking (the SCAM-unmark convention), `"NO"` already tried, don't ask again — a movement can
contradictorily hold `"NO"` with a non-zero price, which `Prezzi.isMovimentoPrezzato` now corrects.

**`AttesaConnessione`** (2026-09-12): if the network drops mid-import, the import retries three times
over nine minutes and then **aborts entirely rather than writing partial, unpriced data** — a dropped
line previously wrote thousands of movements at `0.00` silently, distinguishable from "no price
exists" only by a counter.

Full mechanics of all four in `nocommit/Documentazione/Analisi_Import_Meccanismi.md`.

### Price fetching (`Prezzi.java`)
Queries several exchanges in parallel via OkHttp. Prices are cached in `connectionPrezzi`. Exchange access via CCXT goes through the Node layer below.

**Which leg prices a two-coin movement (2026-09-29, user's decision):** the FIAT leg if there is one (a purchase or sale: the corrispettivo is the currency amount), otherwise the **incoming** leg, and the outgoing one only if the incoming has no price or a zero price. This is circ. AdE 30/E p. 50-51 ("valore normale della cripto-attività ricevuta... alla data in cui lo scambio è concluso", fallback art. 9 c.3 TUIR). The same order (`Prezzi.OrdineGambe`) drives both places that choose: `Prezzi.DammiPrezzoInfoTransazione` (price to look up) and `MovimentiCrypto.DammiMonetaPrioritaria` (legs already priced by the importer). **Precision exception** (same day, user's decision): if the incoming leg's price is not quoted within `Prezzi.PRECISIONE_PREZZO_MS` (5 minutes) of the movement and the outgoing leg's is, the outgoing leg wins — mainly for DeFi, where illiquid tokens often have only hourly quotes (DefiLlama/CoinGecko) while the coin given has minute candles. `Prezzi.isPrezzoPreciso` measures `|InfoPrezzo.timestamp − movement time|`, so every source must store the **quote** time, not the requested one (the old hourly archives use the start of the hour: "coingecko (Old)" wrongly used the requested time until 2026-09-29). A personalised price and a price without a quote time (explicit value: CSV controvalore, price passed by the caller, `"|||Fonte"` in `[40]`) count as precise. The unit of precision is the whole three-pass order in `DammiPrezzoInfoTransazione` (precise, then any non-zero, then zero), mirrored in `DammiMonetaPrioritaria(…, Timestamp)`; Separa/Unisci feeds it the legs' `[15]`/`[40]`, otherwise the unpriced `Moneta` objects made the outgoing leg win every fusion. Three more rules (same day, found on a real Bybit import): (1) legs with a **zero or missing quantity** are excluded from the choice (`OrdineGambe`) — in multi-coin swaps a zero-weight leg keeps the other side's InfoPrezzo, so a SON withdrawal showed BIT as its price source; (2) before declaring a leg imprecise, `Prezzi.CercaPrezzoPreciso` looks it up again with `includiVecchi=false`, because `CambioXXXEUR` reads the old hourly archive **before** the minute cache (order pinned by `PrezziOrdinePrioritaTest` for past declarations) — only inside the two-leg choice, so single-coin valuations (RW year-end) keep the old order; (3) **CoinMarketCap homonyms**: if the chosen price comes from CoinMarketCap and its value deviates by more than `SCOSTAMENTO_MAX_COINMARKETCAP` (10%) from the other leg's, and the other leg is **not** priced by CoinMarketCap too, the other leg wins (`Prezzi.ValoriTroppoDiversi`). Root cause, fixed the same day: the CoinMarketCap map kept one id per symbol by **today's** rank (BIT -> Biconomy 11500 instead of BitDAO 11221, which fell in rank after the MNT migration). `GESTITICOINMARKETCAP` now holds **every** coin of a symbol (key Symbol+CmcId, with name and rank; the old one-row-per-symbol table is dropped on startup and re-downloaded), and `Prezzi.RecuperaPrezziDaCoinMarketCap` downloads every homonym and stores the one with the **highest average market cap in the window**, volume only if no candidate has one (`ScegliOmonimo`; volume alone picked Biconomy over BitDAO in Apr-May 2022 — CMC reports a wash-traded volume above BitDAO's for a token at $0.00001, with market cap 0) under a cache source that names the coin, `"CoinMarketCap (BitDAO)"` (`Prezzi.EtichettaCmc`, since 2026-09-30, for every symbol, homonyms or not) — **the name lives in the cache's exchange column only**: the `InfoPrezzo` cache constructor turns it into `Fonte = "CoinMarketCap"` + `NomeMoneta`, and `Ritorna40` writes it next to the symbol, so `[40]` reads `BIT (BitDAO)|ts|price|CoinMarketCap` (the user's choice: shown as "Moneta di riferimento"). `InfoPrezzo.Moneta` stays the bare symbol because half the program compares it with the legs; anything reading `[40]`'s first field raw must go through `InfoPrezzo.SeparaNome`, first deleting in that window any other non-homonym CoinMarketCap row of the symbol (`CancellaAltriPrezziCoinMarketCap`: the bare `"CoinMarketCap"` of before and another coin that won an overlapping window would otherwise be read first, `ORDER BY exchange`). A caller asking for source `"CoinMarketCap"` (the `[40]` of an older movement) matches `CoinMarketCap%`. From the prices section ("all sources", `TuttiGliOmonimi=true`) the losers are stored too, as `"CoinMarketCap omonimo (<name>)"` (shown to the user as plain CoinMarketCap + the name), so the user can pick one; `DammiPrezzoDaDatabase` excludes `CoinMarketCap%omonimo%` (which also covers the 2026-09-29 label), the list shown by `GUI_ModificaPrezzo` does not. On the first map in the new format **all** CoinMarketCap cache rows (`LIKE 'CoinMarketCap%'`) of every ambiguous symbol are deleted once (option `CoinMarketCap_OmonimiPuliti_2`, checked on **every** `RecuperaCoinsCoinMarketCap` call, not only after a map download (inside it, a map younger than 24 h meant it never ran), and written **only if the DELETE succeeded** — the first version wrote it even when the delete was cancelled at shutdown, so the wrong rows stayed forever), so they are re-downloaded right — with **one** `DELETE … IN (…)`: the cache key starts with `timestamp`, so any condition on symbol is a full scan (~36 s on 48M rows), and the first version's one-DELETE-per-symbol loop over ~800 symbols froze the app for hours. The map is downloaded in pages of 5.000 (`Prezzi.PAGINA_MAPPA_CMC`, `start=`), since active coins are >8.000 and BitDAO is rank 5.046; a map in an older format is re-downloaded even within 24 h (`OPZIONE_VERSIONE_MAPPA_CMC`) — this also changes RW year-end values read live from the cache, by the user's decision. Details in `nocommit/Documentazione/Analisi_Prezzo_Gamba_Scambi.md`. Related fix in `TransazioneDefi.AssegnaPesiaPartiTransazione`: a side's weight base is never less than the sum of that side's known values (`BaseLato`), otherwise an underestimated transaction value gave one priced coin a weight capped at 1 and every other coin zero. The incoming leg is recognised by the **sign** of the quantity, because `creaMovimento` receives the legs in arbitrary order (`TransazioneDefi` passes the incoming one first). EMT in euro are crypto-attività here (permuta, circ. 30/E p. 49): they follow the incoming-leg rule and are priced 1:1 only when they are the chosen leg. Before, the choice was the "most reliable" leg (FIAT, EMT, USD, `SimboliPrioritari`, then the outgoing one), which in deferred swaps valued the coin given at the deposit date. **Stored values are not recomputed**: the rule applies to prices computed from now on (import, classification, manual re-pricing), so both golden masters are unchanged. Measured effect on the real dataset in `nocommit/Documentazione/Analisi_Prezzo_Gamba_Scambi.md`.

**Token alias for exchange pricing live in `config/varie/AliasPrezziToken.json`** (since 2026-09-25,
read by `AliasPrezziToken.Carica()` from `VarCondivise.CompilaMappaChain()`, and **again** when the background config update downloads a new copy — it is the only `config/varie` file held in memory, and before 2026-10-08 a new entry stayed inactive until restart, leaving that session's imports unpriced; `Applica` swaps in new maps, never clears the live ones): `alias` fills
`Principale.Mappa_AddressRete_Nome` (WETH on Base → ETH, USDC.e → USDC: priced via CCXT at 1-minute
resolution instead of hourly DefiLlama/CoinGecko), `stessoPrezzo` fills `Mappa_MoneteStessoPrezzo`.
Three non-obvious points: the file **changes fiscal numbers without a version bump** (Quadro RW values
31/12 live), so edit it as a fiscal change; an alias nulls the address, so a hand-entered price saved
by address+rete is read first by `Prezzi.PrezzoPersonalizzatoTokenConAlias` — keep that ahead of any
alias move; only `riferimento: true` entries (the canonical token, or the bridged one where no canonical exists —
USDT on BSC) feed the automatic SCAM marking of `AliasPrezziToken.MotivoImpersonazione`, and a
token whose address+rete is in `GESTITICOINGECKO` is never an impersonation
(`MotivoImpersonazioneEsclusiCensiti` — by address, never by symbol; the name rule and GoPlus still apply).
`Calcoli_RW_GoldenMasterTest` sets the alias state explicitly (before 2026-09-25 it didn't load them at
all, so its result depended on test order) and checks it three ways: `rw.golden` with the option off,
`rw_anniPassati.golden` with it on, and — with no baseline — that with the option off every year closed
before the first `dal` is identical to the historical 12 entries (`AliasPrezziToken.Predefinite()`). That
last test is the one that fails if an entry is added without `dal`. WPOL/WMATIC is left out on purpose (the POL ticker was not
MATIC before 09/2024).
**Past declarations must not move**: each entry added after the first 12 carries `dal` (ISO date), and an
alias applies only to prices at instants strictly **after** 00:00 of that day — strictly, because
`Calcoli_RW` prices the year-end of year N at `01/01/N+1 00:00`, which is also the start of N+1. So every
pricing reader goes through `AliasPrezziToken.Alias(address, rete, istante)` /
`StessoPrezzo(simbolo, istante)`, never through the two maps directly (they hold all entries, undated).
A new entry added in year N gets `dal = N-01-01`. The option `AliasPrezzi_AncheAnniPassati`
(*Opzioni di calcolo*, "Prezzi dagli exchange anche per gli anni già dichiarati") ignores every `dal`; it is
initialised once, "SI" only on a new installation (`movimenti.crypto.db` absent or empty). Details in `nocommit/Documentazione/Analisi_Alias_Prezzi_Token_CCXT.md`.

**Address tokens outside the CoinGecko list are priced by DefiLlama** (`PrezziDefiLlama`, 2026-10-09, user's rules):
`CambioAddressEUR` no longer marks them KO before asking. A DefiLlama price is accepted if `confidence` > 0,9, otherwise
only for listed tokens (as before, when confidence was ignored) — one rule, `PrezziDefiLlama.Ammesso`. **The only
endpoint is `/batchHistorical`** (many tokens × their own instants per call, `searchWidth=1h` = one hour *per side*,
URL refused above ~9 KB): `/chart` was dropped — pool/vault tokens have a point there only every 3-5 hours (the
instant data is far denser), its 500-point cap is per *call*, and the old code slept 2 s per token. Prices are fetched
in batches **before** valuation: EVM explorer and Moralis imports, Solana import, `PreScaricaPrezzi` (Ricalcola prezzi,
Trasla orario) and `PreScaricaPrezziMonete` (W/RW, T/RT, Giacenze a data); `CambioAddressEUR` asks a single instant
only if no batch did. Batches ask **only pairs with no local price and no KO** (`PreScaricaCoppie`): the cache returns
the quote closest to the instant, so a new DefiLlama row could otherwise displace the one an already-filed year was
declared with. That is the whole protection of past declarations (no `dal` guard, user's choice). ⚠️ The single path
must skip DefiLlama for an instant a batch already asked (`GiaChiestoAllIstante`), or every unpriced token redoes its
call: with `/chart` that took Giacenze a data from 26 s to 284 s. Import instants must match `TransazioneDefi`'s own
round trip through text (`IstanteImport`). **DeFi imports pre-fetch the exchange prices too** (`PrezziImportDeFi`:
one walk of the downloaded JSON gives the address pairs for DefiLlama and the symbol pairs — native coin when the tx
moves value or the wallet pays the fee, tokens with an alias — for `Prezzi.PreScaricaPrezziSimboli`). That one drops
pairs covered by personal prices or the old hourly `XXXEUR` archive (`CopertaDaPrezzoLocale`), which single-coin
valuation reads before downloading and `FiltraRichiesteGiaCoperte` does not: without it the test wallet asked 218
pairs instead of ~90. Measured on a real Cronos wallet (339 tx, 46 tokens, address prices removed from the cache):
200-208 s before 2026-10-09, 52-79 s now, of which ~19 s explorer downloads and ~7 s the daily CoinGecko list. Measurements and decisions in `nocommit/Documentazione/Analisi_Prezzi_LP_DefiLlama.md`.

**The remote path reads on-chain DEX prices (Fase 1-bis, 2026-08-25) and is opt-in — see `ServizioPrezziClient.OPZIONE_ABILITATO_DEFAULT`.** The old CCXT-based shared cache was retired entirely (all seven configured exchanges were confirmed, from their own published API terms, to forbid redistributing market data to third parties even for non-commercial use — see `nocommit/Documentazione/Analisi_VPS_Prezzi_Sito.md`). The replacement reads prices directly from DEX pool state (`ServizioPrezzi/src/onchain/`), which is nobody's market data. It was briefly switched on by default on 2026-08-26, once the `/v1/monete` transparency endpoint (below) made the service's coverage independently checkable, then reverted to disabled-by-default the same day at the user's request pending further testing — do not flip `OPZIONE_ABILITATO_DEFAULT` back to `"SI"` without asking. It's still a checkbox in *Opzioni → Opzioni di Calcolo* (`Prezzi_Opzioni_CheckBox_ServizioOnchain`), persisted as `ServizioPrezziClient.OPZIONE_ABILITATO` in `personale.mv.db`. Tests override it with the `prezzi.servizio.abilitato` system property.

`CambioXXXEUR` tries a remote pull-through cache (`ServizioPrezziClient.tentaRecupero`) before the
local CCXT fetch, already converted to EUR server-side, then falls back to CCXT. The local CCXT path
downloads a **whole calendar day** of quotes at once per `(coin, day)`, tracked by a persistent
marker (`PrezziGiorniCCXT`) so re-imports and incremental recalcs over an already-marked range launch
zero Node processes — forward-only, movements already imported keep their stored price. The remote
service also exposes `GET /v1/monete` (coverage transparency, used as a fast local pre-check) and
`GET /v1/prezzi?symbol=&da=&a=` (range variant, not yet consumed by the Java client). Full mechanics
(source-name rewriting for storage efficiency, exact day-marker invariants, why the golden master
can't exercise this path) in `nocommit/Documentazione/Analisi_VPS_Prezzi_Sito.md`.

### The `Scripts/` Node.js layer

Part of the exchange and price logic lives in JavaScript, not Java, and runs **out of process on a real Node binary** — there is no embedded JS engine (no GraalVM/polyglot dependency in `pom.xml`, no `Context.eval` in the sources), despite the `graalvm.version` property and GraalVM repository left in the pom.

`CcxtInterop.java` bootstraps that runtime:
- `ensureNodeInstalled()` downloads a standalone Node distribution (`NODE_VERSION`) from nodejs.org on first use, picking OS/arch automatically, and extracts it into `tools/node` under the working directory
- `installCcxt()` runs `npm install ccxt@<CCXT_VERSION>` into that distribution
- `getNodeExePath()` resolves the platform-specific `node` executable

**CCXT is pinned (`CcxtInterop.CCXT_VERSION`) and self-correcting, the same principle as `NODE_VERSION`, since 2026-09-04.** Before that, `installCcxt()` ran a bare `npm install ccxt` exactly once — whatever "latest" happened to be on that installation's first API import — and never touched it again, because the only check was "does `node_modules/ccxt` exist". An install from months earlier stayed on that old version forever, silently. A user reported OKX API downloads failing on `Funding errore ... : exchange[metodo] is not a function` for `privateGetAssetBillsHistory` (bug **C14**, `nocommit/Documentazione/Analisi_Bug_Criticita.md`): their installed ccxt predated CCXT adding the `asset/bills-history` endpoint, so the implicit method plainly didn't exist — Trading (`privateGetAccountBillsArchive`, an older endpoint) kept working the whole time, which is what made it look OKX-specific rather than an ambient library problem. `installCcxt()` now reads `node_modules/ccxt/package.json`'s `"version"` and reinstalls whenever it doesn't match `CCXT_VERSION`, so bumping the constant is what future devs do when a script starts relying on an endpoint/method not yet in the pinned version — exactly like bumping `NODE_VERSION`. **This check is skipped when `NODE_ESTERNO != null`** (flatpak/MSIX): that ccxt copy sits in a read-only prefix installed by the packaging manifest, and `getNpmPath()` throws in that mode on purpose (see the flatpak section below) — `installCcxt()` returns immediately instead of ever reaching it.

**npm's own version is not tracked separately — it ships inside the pinned Node distribution.** `getNpmPath()` only ever resolves `npm[.cmd]` from inside `getNodeDir()/node-<NODE_VERSION>-<platform>/`, never a system-wide npm, so its version already moves in lockstep with `NODE_VERSION` with nothing extra to pin. What *is* machine-dependent is npm's **config**: on a real user's Windows machine (2026-09-04) `npm install` printed `npm warn Unknown user config "allow-scripts"` — a key this app never sets, read from that machine's own `.npmrc` (some unrelated tool's leftover). Since our installs are meant to be internal and deterministic, `isolaConfigNpm()` points `npm_config_userconfig`/`npm_config_globalconfig` at a file that deliberately does not exist before every `npm install` in `installCcxt()`/`installModuleNode()` — npm treats a missing config file as "nothing extra to read" (documented behavior, not an error), so the install no longer depends on whatever a given machine's own npm config happens to contain.

Callers (`CcxtInterop.fetchMovimento()`, and several methods in `Prezzi.java`) then build a path `getPathRisorse() + "Scripts/<name>.js"` and launch it as a child process, passing credentials/dates/tokens as command-line arguments and parsing JSON from stdout. Scripts cover Binance endpoints (`Binance_Trades.js`, `Binance_Conversioni.js`, `Binance_EarnFlessibili.js`, `Binance_SaldiGiornalieri.js`, …), generic movement fetching (`FetchMovimenti.js`) and historical prices (`Historical_Multi_Eur.js`).

**`Historical_Multi_Eur.js` is the exception: for batched prices it runs as a long-lived process**
(`--servizio`, managed by `ServizioNodePrezzi.java`, one batch per stdin line, closed after 10 minutes
idle), with the old one-process-per-batch path kept as fallback. "Riscarica tutti i prezzi dalle fonti"
uses it too, with the `tutti` flag (all exchanges in parallel, no cascade) and a ±60-minute window that
matches what `GUI_ModificaPrezzo` then reads from the cache. So its module-level caches outlive a
batch: anything added there must be reset per message or expire, or a transient failure becomes
permanent for the session. It also hands its cached markets to ccxt (`consegnaMarkets`) and keeps them
spot-only without `info` (`markets_v2_*.json`) — without the first, `fetchOHLCV` silently re-downloads
every exchange's market list over the network. Details in
`nocommit/Documentazione/Analisi_Prezzi_Scaricamento_Costi.md` §10.

**A change to Binance import or CCXT price fetching may belong in `Scripts/*.js` rather than in Java** — check both sides. These scripts ship alongside the JAR, like `Immagini/`.

Arguments are consumed **positionally** (`const [, , exchangeId, apiKey, secret, …] = process.argv`) by all ~17 scripts, so a new argument must be appended at the **end** — never inserted in the middle. That is why `fetchMovimento` grew its extra parameters (`passphrase`, then `hostname`) as trailing overloads.

**The OKX API now covers the whole history** — Funding via `asset/bills-history`, Trading via
`account/bills-archive` (~3 months) plus `OKX_Archivio.js` for the quarterly archives back to
February 2021. The two `ImportConfig/OKX_*.json` CSV configs remain only for history older than
that. **OKX has no single domain**: `OKX_Bills.js` probes regional hostnames
(`my.okx.com`/`eea.okx.com`/`www.okx.com`/`app.okx.com`) and remembers the winner, but
`OKX_Archivio.js` does not probe — the recognised hostname must be fed back into the caller's local
variable, not merely persisted (bug C9). `billId` (row identity, dedup key) and `ordId` (which rows
belong to the same order) are not interchangeable and decode differently between the two OKX
accounts (Funding/Trading) and even between `type` codes on each. An incomplete OKX download
**imports nothing at all** (bug C13) — `startDate` only advances on success, so a partial import
would silently skip movements forever. The `after` parameter means a `billId` on Trading but a
timestamp on Funding, and getting this wrong doesn't error, it silently truncates history (bug C8).

**OKX Simple Earn interest can be reconstructed, not only downloaded** (`OKX_InteressiEarn`, 2026-10-07), but
only with `OPZIONE_RICOSTRUZIONE` (*Exchange API → Particolarità OKX*), **off by default**: the program is fiscal,
so by default it books only what the exchange API returns (user's choice). Off, nothing below runs except a
notice when the returned history starts after the requested day (`TestoAvvisoStoricoCorto`).
`finance/savings/lending-history` returns only the last month (720 hourly records) and the interest has no
bill type, so a day that leaves the window before an import is lost, and the oldest day of a truncated window
arrives half-full (dropped by `ScartaPrimoGiornoTroncato`, since its daily ID would block completing it). The
per-period total is exact: current balance + redemptions − subscriptions, from the Funding bills 75/76 fetched
over the whole history (`savings_flussi` of `OKX_Earn.js`, which reuses `OKX_Bills.fetchBills`), checked against
OKX's `earnings` for the open period. The missing part is split over uncovered days by capital (an estimate),
one REWARD per day keyed `EARN-<ccy>-<yyyymmdd>-RIC`, so a re-run adds nothing. Existing interest is recognised
by the `EARN-` key, archive **and** current batch, never by causale. Days inside the window just downloaded get
no share even without interest (OKX sends zero records, or none: BTC from 28/09/2026), but zero days of
**past** windows leave no trace and can still receive a share. Days up to 2025
(`PRIMO_GIORNO_SEMPRE_REGISTRATO`) are dropped **after** the split unless `OPZIONE_ANCHE_ANNI_PASSATI` (off by
default, *Exchange API → Particolarità OKX*): from 2026 every download goes through the reconstruction, so the
data are either absent or right, and they are always booked (the user's choice, 2026-10-07). On the user's archive the API flow counts (32 subscriptions, 27 redemptions, back to 2022) match the
CSV-era Earn transfers plus the 2026 ones, so the old "Savings" product used the same type codes.

**Movements skipped as unknown are recovered from the source documents** (`ScartiImport`, 2026-10-08). An API download
starts at the last movement + 1 s, so a bill the maps did not know (OKX `type 328` before 2026-09-09) was never asked
for again: a real user lacked 27 OKSOL staking rewards. The raw bills are still in the download's NDJSON
(`DocumentiFonte`), so they are re-converted with today's maps. `Ex_OKX_ImportaDaAPI` records the skipped bills in
`SCARTI_IMPORT` (`personale.mv.db`, key `Funding:<billId>`, taken **before** `[2]` becomes "Principale"), and
`fetchMovimenti` keeps a document that added nothing but has skipped bills (before, it was deleted with them). Three rules
the analysis depends on: presence is judged per **unit** (what the import would turn into one movement: the swap rows of a
`Ex_OKX_Raggruppa` group together, every other row alone), never per bill, because a multi-fill order's `[24]` cites only
some bill ids; a unit absent from the archive is **certain** only if registered, otherwise **uncertain** (it may be a
movement the user deleted: on `test/2025`, 498 such units); a unit partly in the archive with a registered leg is **mixed**
and only reported. The re-reading must repeat the import's own pre-processing of the rows: bill dedup and `AbbinaLiquidStakingOnChain` with the `staking_storico` of the document's `OKX_Earn` answers — without it a `type 330` looked like a recoverable swap with only its incoming leg (found on the reporting user's backup: 43 OKSOL from nothing). Any new step the import applies to the rows before `Ex_OKX_ImportaDaAPI` must be mirrored in `ScartiImport.LeggiRigheOKX` (and `VERSIONE_RILETTORE_OKX` bumped). The analysis never consolidates (that fetches prices): `Ex_OKX_Raggruppa` was split out of
`Ex_OKX_RaggruppaEConsolida` for it, and only confirmed units go through `Ex_OKX_ImportaDaAPI`, with `[41]` = the original
document. Recovery is **always confirmed** (`GUI_RecuperoScarti`; certain pre-ticked, uncertain not, unticked and mixed
become `IGNORATO` and are never proposed again, "Chiedi più tardi" marks nothing). It runs after every API download
(`GUI_ExchangeAPI`), at startup (`AvvisiAvvio`) and when the background config update brings new maps
(`Principale_RecuperoScarti.ControllaQuandoLibero`, which waits for the main window to be active). Cost is nil when nothing
changed: `SCARTI_ANALISI` stores per document the hash of the maps it was read with (`ScartiImport.ImprontaOKX`, content of
`OKX_Tipi.json` + `OKX.json` + `VERSIONE_RILETTORE_OKX` — bump the latter when the re-reading logic changes). A document with
candidates is not marked, so after a discarded save they come back. A confirmed unit the import does not write (ID collision, a category `Ex_OKX_Consolida` ignores) becomes `IGNORATO` (`NonRecuperati`), or it would come back forever; the import gets **copies** of the rows, since it rewrites `[2]`, which is part of the register key. OKX only for now (bills, Trading archive; not the Earn
rows). Design in `nocommit/Documentazione/Analisi_Recupero_Movimenti_Sconosciuti.md`.

**The OKX Card wallet is found on-chain, never asked to the user** (`OKX_WalletCarta`, 2026-10-07). The card
spends from an "OKX Pay Wallet", an ERC-4337 smart-contract wallet on **X Layer** (chain id 196), funded by the
Funding bill `type` 325 "Transfer from exchange to smart wallet", which carries no address or hash and is **not**
in `asset/withdrawal-history` (checked). For each 325 bill the public node `rpc.xlayer.tech` (no key, `eth_getLogs`
max 100 blocks, ~1 block/s) is searched from 2 min before to 15 min after for a transfer of the same coin and exact
amount whose recipient answers `entryPoint()` with a known EntryPoint; several bills must agree, else nothing is
chosen. The address is stored in the personal option `OKX_WalletCarta.OPZIONE_WALLET` and shown in *Exchange API →
Particolarità OKX* — not in `WALLETS`, because the program does not support X Layer yet (Etherscan v2 and Routescan
don't cover it). On the user's account the USDG went on to Aave V3 on X Layer (`aXlrUSDG`, rebasing). How to read
X Layer without keys (public node capped at 100 blocks/`eth_getLogs` and ~5 req/s, so a state-bisection scan via
Multicall3) and the questions still open until a real card payment exists are in
`nocommit/Documentazione/Analisi_Carta_OKX_XLayer.md`.

Full mechanics (quarter-list bounding by `startDate`, suspended-quarter recovery, `OKX_Tipi.json`
data-driven type codes, CCXT rate-limit costs, bugs C8/C9/C12/C13/C14 in detail) in
`nocommit/Documentazione/Analisi_Bug_Criticita.md`.

### Source documents — field `[41]` and `DocumentiFonte`

Every imported movement carries, in `[41]`, the numeric id of the file it came from. The copies live
**gzipped** in `DocumentiFonte/` (`VarStatiche.getCartella_DocumentiFonte()`), the registry is the
`DOCUMENTIFONTE` table in `personale.mv.db`, and the whole thing is driven by `DocumentiFonte.java`.
`[41]` holds an **id only** — never a path: `Scrivi_Movimenti_Crypto` deletes `;` rather than escaping it.

Four things not obvious that the design depends on: `[41]` is outside `Impronta()` and the golden
master, which is what makes stamping it free; the dedup hash is of the *original* content, not the
stored `.gz` (gzip embeds an mtime, so hashing the archive would break re-import dedup); `Annulla`
may only run on a registration with `Nuovo == true`, since a re-import of an already-known file adds
zero movements and reuses the existing id; and `DocumentoFonteCorrente` must not be reset in
`AzzeraContatori()`, because several import branches call it twice.

Deleting a document with movements attached deletes those movements too (single selection only,
double confirmation), and since 2026-09-18 the deletion is **provisional**, exactly like deleting a
movement on its own: `Principale_DocumentiFonte.EliminaDocumentoConMovimenti` removes the movements
from the map and calls `DocumentiFonte.AccodaCancellazione(Id)` — the document disappears from
`DocumentiFonte.Elenco()` (so from the panel) immediately, but the registry row and the `.gz` are
untouched. `DocumentiFonte.SalvaBuffer(Map)` makes it real, called from `Importazioni.
Scrivi_Movimenti_Crypto` right next to `MovimentiStorico.SalvaBuffer` — the same instant the movement
deletion itself stops being undoable. `Leggi(int)` deliberately does **not** filter pending-deletion
ids (`Annulla` reads it to resolve the file to delete); only `Elenco()` does. The "Annulla" button in
"Transazioni Crypto" (discard unsaved changes) calls `DocumentiFonte.ScartaBuffer()` to drop the queue
— its own liveness re-check inside `SalvaBuffer` would already stop a live document from being deleted,
but without `ScartaBuffer` it would stay wrongly hidden from the panel until the next real save.
**Generated movements inherit `[41]` from the movement they derive from** (deferred swap, transfer fee/reward,
and since 2026-10-05 also the Dual same-coin legs and reward, the platform/Vault mirror and corrective reward, the
WCRO withdrawal of the WCRO-CRO swap): a new classification that generates movements must copy it too, or the
document filter shows the operation by halves. Older generated rows are filled at load
(`DocumentiFonte.CompletaDocumentoGenerati`, ~2 ms on 20.000 movements, memory only, no *Salva*): from the original
of the `[20]` group with the same instant in the ID, else the document common to all originals, else nothing. On
archives imported before `[41]` existed most originals have no document either, so little gets filled.
Credentials never enter the NDJSON documents written for API/DeFi downloads —
`DocumentiFonte.UrlSenzaChiave` redacts keys from blockchain-explorer URLs.

Full mechanics (hash/backup ordering guarantees, the repeated-pass removal sweep, the two GUI
mount points sharing one operational class) in
`nocommit/Documentazione/Analisi_DocumentiFonte_Movimenti.md`.

### "Chiedi a IA" — asking an external chatbot about a movement

A popup-menu entry on movements that hands a single movement over to an external chatbot in the user's browser. Three classes, no API key and no network call from the app itself:

- `ChatbotIA.java` — the list of chatbots and the URL building. The list is **data, not code**: it lives in `ChatbotIA.json` in the working directory (`VarStatiche.getFile_ChatbotIA()`), written with defaults on first use. The prefill query parameter (`?q=…`) is undocumented by the providers and changes without notice, so a broken chatbot is fixed by editing the JSON, not by recompiling. A bot with an empty `paramQuery` is opened on its plain page with the question left in the clipboard, and `lunghezzaMax` is the URL length beyond which it falls back to the clipboard anyway.
- `PromptIA.java` — builds the question text from the row in `MappaCryptoWallet`, under one of two privacy profiles: `COMPLETO` (everything, including transaction hash, wallet and counterparty addresses, amounts and countervalue — identifies the wallet) and `GENERICO` (only causale, coin symbols, chain and platform name, and only when the platform is not an address: no hashes, no addresses, no amounts, no dates). Optionally appends a request for an Italian fiscal framing, answered on a fixed last line (`CATEGORIA CONSIGLIATA: …`) drawn from general categories, **not** from the combo of `GUI_ClassificazioneMovimento`.
- `GUI_ChiediIA.java` — hand-written `JDialog` (no `.form`, the content is dynamic). It always shows a preview of the text, which the user can edit, before anything leaves the machine; the first use shows a privacy warning. Choices are remembered as user preferences through `DatabaseH2.Pers_Opzioni_Scrivi/Leggi` (keys `IA_Chatbot`, `IA_Profilo`, `IA_Fiscale`, `IA_AvvisoPrivacy` in `personale.mv.db`).

The menu entry is enabled only when the selected row really is a movement (`MappaCryptoWallet.get(ID) != null`), because the popup is shared with tables whose rows are not movements. `Funzioni_WalletDeFi.UrlExplorerTx(Rete, Hash)` returns the explorer URL for a transaction without opening it — extracted from `ApriExplorer()` so the prompt can include the link.

### Table utilities (`Tabelle.java`)
Central class for all JTable rendering, filtering, and sorting:
- `Tabelle_InizializzaHeader(JTable)` — applies the full header renderer (bold+centered, sort icons, filter icons). Call in the constructor after `initComponents()` for tables that also get `Tabelle_FiltroColonne`. Uses `tableFilters.computeIfAbsent` so the same map instance is shared; `Tabelle_FiltroColonne` no longer re-applies the renderer.
- `Tabelle_ApplicaHeaderBoldCentrato(JTable)` — lightweight bold+centered header for tables without filter support.
- `Tabelle_FiltroColonne(JTable, JTextField, popup)` — sets up right-click column filtering and `TableRowSorter`. Does **not** re-apply the header renderer (done once at construction).
- `Tabelle_getSommeColonne(JTable)` — asynchronously computes column totals shown in the header.

## NetBeans GUI builder rules

Every Swing form has a paired `.form` XML file. **Always edit both** when adding or changing UI components:

| File | Purpose | Editable? |
|------|---------|-----------|
| `.form` | Source of truth for NetBeans | Yes — add new components here |
| `initComponents()` (between `//GEN-BEGIN:initComponents` and `//GEN-END:initComponents`) | Generated from `.form` | Yes — add matching Java code here |
| Event handlers (`//GEN-FIRST:event_X` … `//GEN-LAST:event_X`) | Stubs generated by NetBeans; body editable | Yes — add logic inside |
| Variables (`//GEN-BEGIN:variables` … `//GEN-END:variables`) | Generated field declarations | Yes — add new `private` fields here |

Code added **after** `initComponents()` in constructors (table header setup, extra listeners, etc.) is safe and never overwritten.

## Adding a new EVM-compatible blockchain

Per `Documentazione/IstruzioniVarie.txt`, when adding a new EVM chain:
1. Add to `Mappa_ChainExplorer` in `VarCondivise.CompilaMappaChain()` with etherscan v2 chainid URL
2. Update `Funzioni_WalletDeFi.isValidDefiWallet()`
3. Update `Funzioni_WalletDeFi.ApriSituazioneWallet()`
4. Update `Funzioni_WalletDeFi.ApriMovimentiWallet()`
5. Update `Funzioni_WalletDeFi.UrlExplorerTx()` — the per-chain explorer URLs, previously inline in `ApriExplorer()`, now live there and are shared with "Chiedi a IA"
6. Add to `GUI_GestioneWallets.java`

Etherscan v2 chain IDs are listed in `Documentazione/IstruzioniVarie.txt`.

## Packaging & distribution

A single workflow, `.github/workflows/release.yml`, triggered manually (`workflow_dispatch`), on Temurin JDK 24. It reads the version straight from `pom.xml`, builds the fat JAR with `maven-shade-plugin` (`-DskipTests`) and then packages it with `jpackage` in four jobs: `build-windows` (portable app-image, cross-platform portable JAR bundle, and an installer built with Inno Setup), `build-linux` (app-image + `.deb`), `build-macos` (app-image + `.dmg`, on an OS matrix) and `create-release`, which collects the artifacts and publishes the GitHub release via `softprops/action-gh-release`. Each job copies the runtime-side folders (`Scripts/`, `Immagini/`, `logo.png`) next to the JAR.

### Distro packages — AUR and flatpak, both built from the published release

Two more workflows package what `release.yml` has already published, rather than rebuilding it:
`.github/workflows/aur.yml` (AUR `giacenze-crypto-bin`, sources in `packaging/aur/`) and
`.github/workflows/flatpak.yml` (bundle `.flatpak`, sources in `packaging/flatpak/`). Both take the
*Portable Multipiattaforma* zip as input and compute its checksum themselves. Neither is triggered by
`on: release` — a release created with the default `GITHUB_TOKEN` does not trigger other workflows —
so each is `workflow_call`ed from `release.yml` behind an opt-in input, and remains launchable alone.
The release tag is `Giacenze_Crypto_<versione>`, not `v<versione>`, and both packagers depend on it.

**The working directory differs on purpose between the packages, and getting it wrong loses data
silently.** `.deb` and AUR pass `--workdir "HOME/GiacenzeCrypto/"`, so a user moving between them
finds their archive in place. The flatpak **must not**: without `--filesystem=home` flatpak mounts a
tmpfs over `$HOME`, the writes succeed, H2 creates fresh databases and everything disappears on close
with no error at all (verified in a real sandbox). It uses `$XDG_DATA_HOME` instead. Note that
`Giacenze_Crypto.setWorkDir` does a textual `replace("HOME", …)`, not a delimited token, so a path
containing the uppercase string `HOME` would be mangled.

**Node and ccxt are bundled in the flatpak and nowhere else** (`CcxtInterop.NODE_ESTERNO`, since
Flathub forbids downloading code at runtime and `/app` is read-only). When unset — every other
package — Node still downloads into `<workdir>/tools/node` exactly as before. Details, sandbox
permissions and the migration recipe in `nocommit/Documentazione/Pubblicazione_Flatpak.md` and
`nocommit/Documentazione/Pubblicazione_AUR.md`.

### The font is inside the jar, and that changes what a missing glyph does

`FontApplicazione` registers the four Noto Sans faces at startup, before the splash — chosen because
it is the default sans-serif of most Linux distributions, i.e. what the layout is already tuned
against. **A registered physical font does not fall back to another font for glyphs it lacks**, so
UI strings must not use characters Noto Sans doesn't have (arrows, check marks, emoji);
`FontApplicazioneTest` scans every `.form` and string literal and fails on any such character or on
any component asking for a non-bundled font family. Full design (why `registerFont` returning
`false` is normal, not an error; the flatpak font-bundling that was removed) in
`nocommit/Documentazione/Analisi_Font_Tema_Icone.md`.

**Two distinct image locations — do not confuse them:**
- `src/main/resources/Images/` — **on the classpath**, inside the JAR: all the UI icons, loaded as `getResource("/Images/24_Xxx.png")`. This is where a new menu or button icon goes (24×24 monochrome PNG, plus the source SVG in the same folder). A missing file here is an NPE in `initComponents()` at startup that no test would catch.
- `Immagini/` next to the JAR — resolved at runtime against `pathRisorse` via `VarStatiche.getPathImmagini()`, and **not** on the classpath. It holds only the scanned tax-form backgrounds (`QuadroRW_2024.jpg`, `QuadroT_2025.pdf`, …) used by the printing code, and must be distributed alongside the JAR.

### Theme and icons — they are one change, never two

Every icon is monochrome black (PNGs drawn black, SVGs use `stroke="currentColor"` which
svgSalamander renders black), so **darkening the theme and recolouring the icons must land
together**. SVG recolouring is a global `FlatSVGIcon.ColorFilter` at paint time; PNG recolouring
walks the component tree (`Icone.AdattaIconeAlTema`) since the ~144 `new ImageIcon(...)` calls are
NetBeans-generated and not worth editing individually — anything built lazily (after
`WINDOW_OPENED`) or kept as an unattached field (a `JPopupMenu`) must call it explicitly itself.
`JDateChooser` needs two opposite remedies depending on whether `setDate()` is called by a renderer
(re-run every paint) or by the user (no repaint after). Full mechanics in
`nocommit/Documentazione/Analisi_Font_Tema_Icone.md`.

Also resolved against `pathRisorse` (i.e. shipped alongside the JAR):
- `Scripts/` — Node.js scripts for Binance/CCXT imports and historical prices

Resolved against `workingDirectory`:
- `config/import/` — **the JSON import configurations actually in use**, kept aligned with the repository at startup (`VarStatiche.getCartella_ConfigImport()`)
- `ImportConfig/` — the **legacy** folder of the same files (`getCartella_ImportConfig()`), read only for entries *not* marked `"centralizzato": true`, i.e. the user's own. A centralised config dropped here is silently ignored — seed `config/import/` instead when testing an unpublished change
- `ChatbotIA.json` — chatbot list for "Chiedi a IA", created with defaults on first use
- `Backup/` and `Temporanei/` — created automatically if missing
- `tools/node/` — standalone Node distribution, downloaded on first CCXT use (`CcxtInterop.getNodeDir()`, computed on every call rather than a `static final`: as a constant it froze on whichever working directory loaded the class first, which in the test suite made `PrezziLottoCCXTTest` always skip)

## Internal technical documents — live under `nocommit/`, never committed

All the working `.md` documents (bug analyses, performance analyses, the change log) live in **`nocommit/Documentazione/`**. `/nocommit/` is in `.gitignore`, so they are **ignored by git**, not merely untracked: many contain references to the user's own data, wallets and configuration and must never end up in a commit — but the folder is also the standing home for pure-engineering deep dives that this file points to, whether or not they touch user data.

`nocommit/` also holds `GoldenMaster/` (the golden-master baseline, see below), `StrumentiTest/` (the manual/automated-GUI test tooling, see "Automated GUI tests" below) and `Prototipi/` (design prototypes). It was split out of `test/` on 2026-08-24 so that `test/` holds only the data archives used for manual runs (`test/2025/`, `test/Dichiarazione 2025/`, …); `/test/` stays in `.gitignore` too.

| document | content |
|---|---|
| `nocommit/Documentazione/StoricoModifiche.md` | the change log, **current month only** (see below) |
| `nocommit/Documentazione/StoricoModifiche_AAAA-MM.md` | closed months of the change log, one file per month |
| `nocommit/Documentazione/Analisi_Bug_Criticita.md` | known bugs by ID (incl. all OKX C8/C9/C12/C13/C14 mechanics), referenced by the characterization tests |
| `nocommit/Documentazione/Analisi_Campo5_Campo18_Categoria.md` | legal campo5/campo18/categoria combinations |
| `nocommit/Documentazione/Analisi_Performance_Caricamento.md` | performance analysis of the update cycle, incl. the splash phase-weight design |
| `nocommit/Documentazione/Analisi_DB_Prezzi_Manutenzione.md` | price-DB compaction design and the deferred delete-unreachable-rows work |
| `nocommit/Documentazione/Analisi_Filtri_Movimenti.md` | the movements-table row-filter vs. load-filter design |
| `nocommit/Documentazione/Analisi_Ricalcolo_Incrementale_Plusvalenze.md` | `AggiornaPlusvalenze()` incremental-recompute design |
| `nocommit/Documentazione/Analisi_DocumentiFonte_Movimenti.md` | source-document (`[41]`) tracking and deletion cascade |
| `nocommit/Documentazione/Analisi_QuadroRW_Codice14.md` | codice bene 14 legal analysis + liquidità/IVAFE-privilegiata implementation |
| `nocommit/Documentazione/Analisi_QuadroRW_Periodi_IVAFE.md` | `GRUPPO_PERIODO_RW` periods, conto-corrente IVAFE, Crypto.com App FIAT exception |
| `nocommit/Documentazione/Analisi_Import_Meccanismi.md` | wallet-speculare, causali chiuse, scambio differito, `[32]` flag, `AttesaConnessione` |
| `nocommit/Documentazione/Analisi_Font_Tema_Icone.md` | bundled-font glyph fallback, dark-theme + icon recolouring mechanics |
| `nocommit/Documentazione/Analisi_Normativa_Tab.md` | `Normativa/` archive scraping quirks and the "Normative"/"Dichiarazioni" tab implementation |
| `nocommit/Documentazione/Analisi_VPS_Prezzi_Sito.md` | remote price service (on-chain DEX prices) design and client integration |
| `nocommit/Documentazione/API_ServizioPrezzi.md` | practical reference for the price service's HTTP API |
| `nocommit/Documentazione/Analisi_Prezzi_Scaricamento_Costi.md` | why a price download costs ~40 requests per (coin, day), what the 8-exchange fan-out actually delivers, and the constraints (day marker, `fonte` ambiguity) any change must respect |
| `nocommit/Documentazione/Pubblicazione_Flatpak.md` / `Pubblicazione_AUR.md` | packaging recipes and sandbox/workdir details for those two distros |
| `nocommit/Documentazione/Test_Grafici_Automatici.md` | how to run automated GUI tests on the virtual display |
| `nocommit/Documentazione/Analisi_Prezzi_LP_DefiLlama.md` | automatic prices of pool/vault tokens from DefiLlama: archive measurements, endpoints, the user's rules (§8) |
| `nocommit/Documentazione/Analisi_Carta_OKX_XLayer.md` | OKX Card wallet on X Layer: observed flow, sources tried, public-node limits, state-bisection scan, open questions |

**Write new technical `.md` documents there, not in `Documentazione/`.** The top-level `Documentazione/` folder is **historical** since 2026-08-18: the `.odt`/`.pdf`/`.txt` manuals that used to be the source now live as Markdown under `docs/documentazione/` (below), and the old files are kept only as a record of what was published before — do not update them. What is still live there is the material that never became a manual: `Documentazione/IstruzioniVarie.txt` (referenced elsewhere in this file), `MappatureImport.txt`, the `.pptx` presentations, `FormuleUtili.xlsx` and `Schemi.odg`.

Entries written **before 2026-08-24** still cite the old `test/Documentazione/…`, `test/GoldenMaster/…` and `test/StrumentiTest/…` paths — including in the changelog archives, per the note on historical entries below — because that is where those files lived at the time; do not rewrite them.

## User documentation — Markdown under `docs/`, published by GitHub Pages

The manuals the application opens are **pages, not files**: the source is the Markdown in
`docs/documentazione/`, which GitHub Pages renders as `.html` with the same name, and
`DocumentiAiuto` holds one constant per page (`disclaimer.html`, …) plus `NOVITA_VERSIONI`
(`changelog.html`, the per-version change list, converted from `Documentazione/Readme.txt`).
Four things the arrangement depends on:

- **The Markdown is the only source.** The `.odt` originals in `Documentazione/` are not regenerated
  from it and are not updated any more; editing them changes nothing that anyone reads.
- **The PDFs published next to the pages are generated, not written.** Every version up to 1.0.61 opens
  `…/documentazione/<nome>.pdf`, so those URLs must keep answering — and must not answer with stale
  text. `docs/strumenti/genera-pdf.sh` rebuilds them from the same Markdown; re-run it after editing a
  page. `DocumentiAiutoTest` fails if either half is missing. Since 2026-09-27 the generator is
  `GeneraPdfDocumentazione` (in `src/main`, like `GeneraSplash`, but not called by the app): it draws the
  manuals with **the same veste grafica as the W/RW prints** through `Stampe.AttivaVesteDocumento` (cover,
  header with logo, margin band, watermark, numbered footer, embedded Noto Sans). It converts only the
  Markdown subset the manuals use; a new construct in a page needs support there. Three OpenPDF traps it
  already avoids: an image must be a **standalone element** with `setStrictImageSequence(true)` (inside a
  paragraph a tall image overflows the page bottom, without strict sequence the following text jumps
  ahead of it); glyphs Noto Sans lacks (→) fall back to the base-14 Symbol font; table columns are never
  narrower than their longest word. The old LibreOffice pipeline (`md2html.py`) was removed.
- **Headings carry explicit `{#ancora}` ids.** The in-page links are written against them instead of
  against kramdown's generated slugs, which differ on accents and punctuation (`À`, `–`, `/`) and would
  silently break. In the PDFs each id becomes a named destination (and H2/H3 also a bookmark), so the index
  at the top of every manual stays clickable.
- **Images live in `docs/documentazione/immagini/<pagina>/`**, numbered in reading order; they were
  extracted from the `.odt` originals, so a screenshot that is redone must replace the numbered file.
  Screenshots of the app are taken on an **anonymised copy** of the user's data (addresses, hashes, API keys
  and amounts altered), never on the real archive; the tooling for the Quadro RW manual is
  `nocommit/StrumentiTest/PreparaDocRW.java` + `ScattaDocRW.java`.

**The same `changelog.md` is also inside the jar** (`/Novita/changelog.md`, a `<resource>` on `docs/documentazione` in
the pom, 2026-10-06) and feeds the "Novità della versione" dialog (`GUI_NovitaVersione`, logic in `NovitaVersione`): one
source for the web page and the dialog, no network needed. It opens by itself the first time a version different from
the last one seen starts (`NovitaVersione.OPZIONE_ULTIMA_VERSIONE` in `personale.mv.db`, written before showing), never
on a new installation (empty archive), never for an unknown version; and from the *Novità di questa versione* button of
the Informazioni window. A released version (`1.0.65`) shows its own `## Versione` section, a test version (four numbers,
`1.0.64.08`) the **topmost** section, i.e. the notes being written for the next release, with both numbers in the
title. So the notes of a version must be written in that section before the version is published, and every test
build after a pom bump shows them once. `NovitaVersione.Html` converts only the Markdown subset the page uses
(headings, bold, italic, code, bullet lists with continuation lines, links made absolute, quotes); a new construct in the
page needs support there, and `NovitaVersioneTest` checks every section for leftover syntax and the font's glyphs.
Startup dialogs go in `Principale.AvvisiAvvio`, one after the other in a single `invokeLater`: a modal dialog pumps the
event queue while open, so two separately queued ones would open on top of each other.

The window title no longer says *Beta*: the program left the beta phase on 2026-08-18, so
`VarStatiche.componiTitolo` takes only the version and the Store edition no longer differs from the
complete one on this point (it was policy 10.1 that made the Store edition drop the word first).

Note that older entries in the change log (now in `StoricoModifiche_2026-07.md` and later archives) still cite the previous paths (`Documentazione/Analisi_*.md`): they are a historical record of how things were at the time and are deliberately left unchanged.

## `Normativa/` — l'archivio dei documenti fiscali ufficiali

Cartella **committata** (non è sotto `test/`, non è ignorata) che raccoglie i testi ufficiali
sulla fiscalità delle cripto-attività: leggi di bilancio 2023-2026 e norme richiamate, prassi
dell'Agenzia delle entrate, istruzioni di Redditi PF e 730, modulistica ISEE/DSU. È materiale
per il **documentale delle regole fiscali** previsto in futuro nell'applicazione, non codice.
`Normativa/README.md` è la guida per chi la consulta; qui sta solo ciò che serve per
**modificarla** senza romperla.

**Ogni file è descritto in `fonti.csv`, e quello è il punto di controllo.** Autorità,
identificativo ufficiale, indirizzo, data di scarico e SHA-256. `Strumenti/genera_fonti.py`
marca `SCONOSCIUTO` qualunque file presente sul disco ma non descritto da una configurazione —
il modo in cui la promessa "solo documenti ufficiali" resta verificabile. Rilanciare
`genera_fonti.py` **per ultimo**, dopo ogni scarico o rigenerazione.

**Ufficiale e derivato non vanno mescolati.** `Leggi/Originale/`, `Prassi_AgenziaEntrate/`,
`Istruzioni_Dichiarazioni/` e `ISEE_DSU/` contengono file scaricati tali e quali;
`Leggi/Estratti/` e `Leggi/Consolidato/` sono prodotti dagli script e **si rigenerano**, non si
modificano a mano — un consolidato accosta la fotografia ufficiale dell'articolo prima e dopo la
novella, perché il testo risultante non esiste in nessuno dei due atti pubblicati.

Il tab "Normative" (dentro "Dichiarazioni", con RW e RT) rende questo archivio consultabile in
app: mirror locale scaricato da GitHub, catalogo da `fonti.csv`, ricerca full-text in cache,
conversione degli Akoma Ntoso XML in HTML leggibile. Le quattro cose apprese scrivendo gli script
di scraping (sessione Normattiva, `dataVigenza` in ritardo sul corpo dell'atto, indici AdE non
schematici), cosa è stato lasciato fuori di proposito, e i dettagli implementativi del tab sono in
`nocommit/Documentazione/Analisi_Normativa_Tab.md`.

## Automated GUI tests

The application can be driven automatically on a **dedicated virtual X display**, so that GUI checks do not require the user's screen and cannot be disturbed by focus changes. Scripts in `nocommit/StrumentiTest/` (`avvia-display-virtuale.sh`, `avvia-app-virtuale.sh`, `ferma-display-virtuale.sh`, plus `GuiTest.java`).

Two things that are not obvious and cost a whole session to find out: Java picks its screen-capture method from the **environment**, not from `DISPLAY`, so `WAYLAND_DISPLAY` and `XDG_SESSION_TYPE` must be unset or capture fails with `SecurityException: Screen Capture in the selected area was not allowed`; and `openbox` is genuinely needed, because without a window manager Swing windows do not reliably take focus. Details and the coordinate tables are in `nocommit/Documentazione/Test_Grafici_Automatici.md`.

## Change log

After every code modification, add a short recap of what was done to `nocommit/Documentazione/StoricoModifiche.md`.

**Append the new entry at the top of the file without reading the file first.** Everything needed to write it is stated here and repeated in a comment at the head of the file itself; reading a changelog to append to it is pure cost, and the whole point of the format below is to make that read unnecessary.

- **Order**: reverse chronological, newest on top — the new entry goes immediately after the header comment.
- **Title**: `## AAAA-MM-GG`. If the day already has an entry, the next one is `## AAAA-MM-GG (2)`, then `(3)`… and still goes *above* the earlier ones of that day.
- **Size**: at most ~15 lines / ~1500 bytes. What changed and why, the files touched, the outcome of the tests.
- **What does not belong here**: the long-lived reasoning — why an API behaves that way, which invariant must not be broken, what was measured. That goes in this file (`CLAUDE.md`) or in a `nocommit/Documentazione/Analisi_*.md`, where it is found on purpose instead of being re-read every time. Entries had grown to 7–26 KB each before this rule; that is what the cap exists to prevent.
- **Never** transaction hashes or wallet addresses (they are in the ignored `test/`, but the habit is what matters).

**Monthly rotation**: the active file holds the **current month only**. On the first change of a new month, move the closed month's entries into `nocommit/Documentazione/StoricoModifiche_AAAA-MM.md`, reusing the header of the existing archive (`StoricoModifiche_2026-07.md`), and add it to the archive list in the header comment of the active file. Archives are consulted with `grep`, never read whole.
