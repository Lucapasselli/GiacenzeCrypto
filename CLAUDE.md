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

# Run the built JAR directly (substitute the <version> from pom.xml, currently 1.0.64.01)
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

**I contatori del pulsante "Errori" sono quattro e non nascono tutti insieme.** Tre (`NumErroriMovNoPrezzo`, `NumErroriMovSconosciuti`, `NumErroriStackLiFoMancante`) li calcola il ciclo di `TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa`; il quarto, `NumErroriGiacenzeNegative`, arriva dal thread di `Funzione_CaricaTabelleSecondarieInBackgroud`, che finisce **dopo**. Per questo il testo del pulsante sta in `Errori_AggiornaPulsante()`, chiamato da tutti e due i punti: da uno solo mostrerebbe sempre il conteggio della passata precedente, e zero al primo caricamento.

**"Giacenze negative" non è un doppione di "parte del LiFo mancante"**, anche se le due voci del dialogo portano alla stessa scheda. La prima viene da `Funzioni.ControllaSaldiNegativi`, che ragiona per **exchange + sotto-wallet** (`[3];[4];token`); la seconda conta i movimenti con la lettera `A` in `v[38]`, scritta dal motore delle plusvalenze e quindi ragionata per **gruppo wallet**. Un trasferimento interno azzera esplicitamente quella `A`, perciò un comparto svuotato a forza di giroconti (es. un sotto-wallet alimentato via `walletSpecularePerCausale`) produce la prima e non la seconda. Le etichette del dialogo devono continuare a distinguerle. `ControllaSaldiNegativi` scorre `MappaCryptoWallet` nell'ordine della mappa, che è una `TreeMap` sull'ID il cui prefisso è `yyyyMMddHHmmss`: il conteggio è quindi **cronologico a prescindere dall'ordine in cui i CSV sono stati importati**, e un rientro caricato prima della sua uscita viene segnalato finché manca l'altra riga.

### Separating GUI from operational logic (ongoing refactor)

`Principale.java` is ~16 000 lines and its `.form` is over 500 KB, because GUI and business logic grew together in the same file. **The direction of travel is to keep the two as separate as possible**: `Principale` should hold the Swing wiring, and the operational logic of each tab should live in its own companion class.

This extraction is already under way — it is a deliberate, incremental refactor, not accidental structure:

| Class | Holds |
|---|---|
| `Principale_GiacenzeaData.java` | operational logic of the "Giacenze a data" tab |
| `Principale_Opzioni_Pulizie.java` | data-cleanup operations from the options tab |
| `Principale_Movimenti_SeparaUnisci.java` | three popup operations on movements: splitting one two-coin movement into an independent deposit + withdrawal, merging an unclassified deposit + withdrawal back into a single swap, and merging N already-classified movements of the same type/coin/wallet into one by summing their quantities |

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

⚠️ La colonna ha spostato gli indici della tabella: *Errori* è la **7** e *InfoPrezzo* la **8**, sia nei
lettori di `Principale` sia in `Tabelle.ColoraRigheTabella0GiacenzeaData`.

### Filtri della tabella movimenti — due meccanismi, non uno

Sulla tabella dei movimenti agiscono **due** famiglie di filtri, e confonderle porta a scrivere
codice che non funziona: i **filtri di riga** (`RowFilter` Swing — colonne dell'header, campo di
ricerca) agiscono su un modello già costruito; i **filtri di caricamento** (wallet/gruppo, token,
documento di origine, date, TI, SCAM, senza prezzo, LiFo mancante) decidono quali righe esistono e
vivono in `Principale_FiltriMovimenti.FiltriMovimenti.Passa()` — cambiarli costa un ricaricamento.

Il documento di origine **non può** essere un `RowFilter`: il modello dichiara 40 colonne ma
`Converti_String_Object` produce 45 elementi, e `DefaultTableModel.justifyRows` tronca ogni riga a
`getColumnCount()` — i campi `[40]`-`[44]` non entrano mai nel modello. `Passa()` non legge nulla per
conto proprio: riceve dati già calcolati dal ciclo di caricamento. Il dialogo `GUI_FiltriMovimenti`
scrive un unico record (`FiltriCorrenti`) e ricarica una volta sola — la barra non ha più nessun
controllo di filtro oltre a ricerca/Filtri…/Azzera Filtri.

Meccanica completa (perché i due `RowFilter` non applicano nulla da soli, `GroupLayout` da
ricostruire e non modificare a righe, il difetto noto sul wallet singolo senza gruppo) in
`nocommit/Documentazione/Analisi_Filtri_Movimenti.md`.

### The movement row model — read this before touching any calculation

A movement is **not** a class: it is a `String[]` of `Importazioni.ColonneTabella` (currently **45**) columns, stored in `Principale.MappaCryptoWallet` keyed by column `[0]` (the movement ID). Every engine addresses fields positionally — `v[5]`, `v[16]`, `v[18]`, `v[33]` — so:

- Changing `ColonneTabella` or reordering columns breaks the import pipeline, the calculation engines, the tables and the CSV round-trip at once.
- `[0]` is the ID; its last `_XX` segment is the **categoria** (read by `Calcoli_PlusvalenzeNew` as `IDTS[4]`). `[5]` is the transaction-type description, `[18]` the subtype/marker.
- `nocommit/Documentazione/Analisi_Campo5_Campo18_Categoria.md` is the reference for which campo5/campo18/categoria combinations are legal and where each is produced.
- `[42]` is the **lignaggio**: a random `UUID` written the first time a movement is modified by hand, and the key of its modification history (`MOVIMENTI_STORICO`). Blank means "never modified manually". It must be **copied verbatim** by anything that rebuilds a movement — that is why it is deliberately absent from `MovimentiCrypto.CampiNonCopiabiliVerbatim` and present in `Principale_Movimenti_SeparaUnisci.CampiDaRiportare`; dropping it orphans that movement's history. `[43]`/`[44]` are the only free columns left. See `MovimentiStorico` and `nocommit/Documentazione/Analisi_Storico_Modifiche_Movimenti.md`, section "v6".
- Short rows are padded to 45 and blank-filled via `Importazioni.RiempiVuotiArray()` on load.

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

**In a generic-CSV config, `nomeExchange` becomes movement field `[3]` and `nomeWallet` field `[4]` — and `[3]` is load-bearing twice over.** `costruisciMovimenti` calls `creaMovimento(mOUT, mIN, exchange, wallet, …)` with `exchange = nomeExchange` in the `Wallet` slot: `[3] = nomeExchange`, `[4] = nomeWallet`. Field `[3]` is then the exchange key in the re-import dedup (`Importazioni.F_buildKeyMovimento` = `giorno|[3]|monetaU|qtaU|monetaE|qtaE`) **and** the key of the fiscal wallet-group lookup (`Calcoli_PlusvalenzeNew` reads `DatabaseH2.Pers_GruppoWallet_Leggi(v[3])`). So changing a config's `nomeExchange` re-imports every already-stored movement of that source as a duplicate (different `[3]`), and moves those movements into a different wallet group unless a `GRUPPO_ALIAS` re-unites them. `nomeWallet` (`[4]`) is fiscally inert — like the OKX note above. The two `config/import/Coinbase*.json` use this on purpose: retail keeps `nomeExchange` "Coinbase", `Coinbase Pro GDAX.json` v2.000 sets it to "Coinbase Pro" so GDAX is a distinct wallet/group, and the retail↔Pro giroconti (`Pro/Exchange Deposit/Withdrawal` → `TRASFERIMENTO-CRYPTO`, and the GDAX `deposit`/`withdrawal` in crypto → auto `DC`/`PC`) are left with blank `campo18` for manual *Classifica Movimento* pairing. That config also drops the old `type.unit` composite causale (`causale2`/`separatoreCausale`): one bare causale per `type`, EUR-vs-crypto on `deposit`/`withdrawal` resolved by `RitornaTipologiaTransazione` from the coin's FIAT/Crypto type. Pinned by `ImportazioneGenericaCoinbaseProGdaxTest`.

**`colonneControvalore.noteAcquistoDaSaldo` (`Coinbase CSV.json` ≥ v1.006) — Buy pagati con carta non devono movimentare euro.** For a causale in `causaliConMovimentoCommissione` (only `Buy`), `costruisciMovimenti` normally synthesises a FIAT-out leg of `Total − fee` (→ categoria `AC`) plus a separate `COMMISSIONI` movement. When `noteAcquistoDaSaldo` is set and the row's Notes are non-empty and contain **none** of its markers (`"EUR Wallet"`), the Buy was paid from outside the exchange balance (card, Apple/Google Pay, direct bank): both the synthetic FIAT leg and the `COMMISSIONI` movement are skipped, and the lone crypto-in movement has its ID categoria rewritten `_DC`→`_AC` with `campo5="ACQUISTO CRYPTO"` and **`campo18` left empty** — the exact shape the Crypto.com App importer already produces (`Importazioni.java`, "Forzo il fatto che sia un acquisto crypto"). `Calcoli_PlusvalenzeNew`'s `v[18].contains("DAC") || TipoID.equals("AC")` branch then loads it at full cost `campo[15]` (= `Total − fee`), plusvalenza 0, no EUR moved. Empty Notes → unchanged (synthesised FIAT leg). Pinned by `ImportazioneGenericaCoinbaseTest`.

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
compute derivative income (art. 67 c-quater TUIR) and treats these as a crypto-crypto exchange by
approximation. Full mechanics in `nocommit/Documentazione/Analisi_Import_Meccanismi.md`.

**`Binance_DualInvestment.Abbina()` treats a matched contract differently depending on whether the
settlement coin equals the subscribed coin.** Different coin (e.g. subscribed USDT, settled BTC) is a
genuine permuta and goes through `GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito`
(five synthetic movements, both endpoints renumbered). Same coin — the common case, principal +
interest settled back in the subscribed currency — would make that same swap permute the *entire*
settled amount instead of only the excess, so it goes through
`Binance_DualInvestment.CreaMovimentiDualInvestmentStessaMoneta` instead: Purchase and Settlement keep
their own IDs (only content fields are mutated, exactly like `CreaMovimentoTrasferimentoA/Da`'s Vault
mechanism, not renumbered), a mirror TI pair moves the **subscribed quantity only** to/from a
`Dual Savings` sub-wallet (PTW/DTW markers, same wallet group ⇒ invisible to the LIFO stack, see the
"costo di carico" note above), the Settlement movement's own quantity/value are reduced to that
subscribed quantity, and — only if the settlement paid more than the subscription — the exact
difference becomes an independent `REWARD` movement priced on the same per-unit value already computed
for Settlement at import (no new price lookup). Unlike the generic Vault mechanism, the reward here is
exact from the contract's own two quantities (known from the detail CSV), not inferred from an
aggregate sub-wallet balance — deliberately **not** reusing `CreaMovimentoTrasferimentoA/Da` themselves,
to avoid risking their aggregate-balance/hash-based-reward logic for a case they were never designed
for.

**"Scambio differito"** (`SCAMBIO DIFFERITO`) recognises a withdrawal and a deposit on independent
CSV rows, after the whole import is written, as the two halves of one exchange happening "behind the
scenes" (Auto-Invest, Token Swap). `Importazioni.ConsolidaMovimentiDifferiti` matches them within a
configurable time/value tolerance and splits the pair into five movements through a synthetic
platform, priced at the deposit date. The "already exists" dedup check must run **before** writing,
not after, or the automatic pairing never fires on a normal import.

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

**npm's own version is not tracked separately — it ships inside the pinned Node distribution.** `getNpmPath()` only ever resolves `npm[.cmd]` from inside `NODE_DIR/node-<NODE_VERSION>-<platform>/`, never a system-wide npm, so its version already moves in lockstep with `NODE_VERSION` with nothing extra to pin. What *is* machine-dependent is npm's **config**: on a real user's Windows machine (2026-09-04) `npm install` printed `npm warn Unknown user config "allow-scripts"` — a key this app never sets, read from that machine's own `.npmrc` (some unrelated tool's leftover). Since our installs are meant to be internal and deterministic, `isolaConfigNpm()` points `npm_config_userconfig`/`npm_config_globalconfig` at a file that deliberately does not exist before every `npm install` in `installCcxt()`/`installModuleNode()` — npm treats a missing config file as "nothing extra to read" (documented behavior, not an error), so the install no longer depends on whatever a given machine's own npm config happens to contain.

Callers (`CcxtInterop.fetchMovimento()`, and several methods in `Prezzi.java`) then build a path `getPathRisorse() + "Scripts/<name>.js"` and launch it as a child process, passing credentials/dates/tokens as command-line arguments and parsing JSON from stdout. Scripts cover Binance endpoints (`Binance_Trades.js`, `Binance_Conversioni.js`, `Binance_EarnFlessibili.js`, `Binance_SaldiGiornalieri.js`, …), generic movement fetching (`FetchMovimenti.js`) and historical prices (`Historical_Multi_Eur.js`).

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
- `tools/node/` — standalone Node distribution, downloaded on first CCXT use (`CcxtInterop.NODE_DIR`)

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

**Write new technical `.md` documents there, not in `Documentazione/`.** The top-level `Documentazione/` folder is **historical** since 2026-08-18: the `.odt`/`.pdf`/`.txt` manuals that used to be the source now live as Markdown under `docs/documentazione/` (below), and the old files are kept only as a record of what was published before — do not update them. What is still live there is the material that never became a manual: `Documentazione/IstruzioniVarie.txt` (referenced elsewhere in this file), `MappatureImport.txt`, the `.pptx` presentations, `FormuleUtili.xlsx` and `Schemi.odg`.

Entries written **before 2026-08-24** still cite the old `test/Documentazione/…`, `test/GoldenMaster/…` and `test/StrumentiTest/…` paths — including in the changelog archives, per the note on historical entries below — because that is where those files lived at the time; do not rewrite them.

## User documentation — Markdown under `docs/`, published by GitHub Pages

The manuals the application opens are **pages, not files**: the source is the Markdown in
`docs/documentazione/`, which GitHub Pages renders as `.html` with the same name, and
`DocumentiAiuto` holds one constant per page (`disclaimer.html`, …) plus `NOVITA_VERSIONI`
(`changelog.html`, the per-version change list, converted from `Documentazione/Readme.txt`).
Five things the arrangement depends on:

- **The Markdown is the only source.** The `.odt` originals in `Documentazione/` are not regenerated
  from it and are not updated any more; editing them changes nothing that anyone reads.
- **The PDFs published next to the pages are generated, not written.** Every version up to 1.0.61 opens
  `…/documentazione/<nome>.pdf`, so those URLs must keep answering — and must not answer with stale
  text. `docs/strumenti/genera-pdf.sh` rebuilds them from the same Markdown (`md2html.py` + LibreOffice);
  re-run it after editing a page. `DocumentiAiutoTest` fails if either half is missing.
- **Headings carry explicit `{#ancora}` ids.** The in-page links are written against them instead of
  against kramdown's generated slugs, which differ on accents and punctuation (`À`, `–`, `/`) and would
  silently break. In the PDFs `md2html.py` turns each of them into an `<a name="…"></a>` **as well as**
  an `id=`: LibreOffice's HTML import builds its bookmarks from `<a name>`, and while it merely stripped
  them the index at the top of every manual pointed at nothing and was not clickable.
- **LibreOffice ignores the CSS that governs the page, and honours the HTML attributes instead.** Two
  consequences that `md2html.py` has to work around, both of which produced the "immagini enormi e fuori
  posto" of the first PDFs: `max-width` on `img` does nothing — an image with no `width`/`height`
  attribute is imported at its nominal 96 dpi, so a 2260 px screenshot lands 60 cm wide on a 21 cm page
  and is cut off at the margin — and a table with no `width` attribute is laid out at its natural width,
  losing the last column the same way. So the converter measures every image with PIL and writes the
  attributes itself (`DPI_IMMAGINI` = 150, capped at 16 × 20 cm inside an A4 with 2 cm margins), and
  emits `width="100%"` on the tables. For the same reason the font is declared per element: `body` alone
  does not reach the headings, which fall back to LibreOffice's Liberation Serif.
- **Images live in `docs/documentazione/immagini/<pagina>/`**, numbered in reading order; they were
  extracted from the `.odt` originals, so a screenshot that is redone must replace the numbered file.

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
