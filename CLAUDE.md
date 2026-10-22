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

# Run the built JAR directly (substitute the <version> from pom.xml, currently 1.0.65.01)
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

**Static H2 connections leak between test classes, and `IsolamentoConnessioniH2` contains it**: closed `Connection`s stayed in
`DatabaseH2`'s static fields and a later class fell back to the network, downloading Node into a deleted `@TempDir` until `/tmp`
was full. The extension (autodetected, `junit-platform.properties` + `META-INF/services/…Extension`) nulls closed connections
before and after each class. A new test class that needs a DB must still open its own. Details: `nocommit/Documentazione/Dettagli_Test.md`.

**Tests do not use the network unless the class says `@Tag(GuardiaRete.TAG)`** (`"rete"`): the autodetected `GuardiaRete` sets
`Funzioni.ReteDisabilitataPerTest` and fails a test that still connects out or runs Node on `Scripts/` (suite 158 s → 63 s). Fix
a failure with a controvalore or a cached price, tag only when the network is what is tested. Details: `nocommit/Documentazione/Dettagli_Test.md`.

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

**Known exchanges and their wallet groups live in `RW_Predefiniti.json`, not in code (2026-10-12).** The `exchange` array gives each
exchange `id`, `nome`, `gruppo` (`Wallet 1xx`, explicit: no more index arithmetic from a table in `DatabaseH2`) and optional
`nomiSorgente` (lowercase spellings of its name in field `[3]`: `"okex"`, `"nexo pro"`...). `Principale_GruppiWalletRW` reads it once
(`exchangeNoti()`, `RicaricaExchangeNoti()` after the background config update brings a new copy). A source name that no
exchange lists (Coinbase Pro, GDAX) stays a wallet of its own. Adding an exchange or a spelling is a JSON edit only; older app
versions ignore `nomiSorgente`. `DatiPredefinitiRWTest` checks ids/groups are unique and every spelling maps back.

### Data storage: what is and isn't a database

The `.db` extension is used for **both** H2 databases and plain text files — do not assume a `.db` path is SQL.

Flat files in `workingDirectory`, reached through `VarStatiche` getters:
- `movimenti.crypto.db` (`getFile_CryptoWallet()`) — **the primary movement store**: one `;`-delimited line per movement, loaded into `Principale.MappaCryptoWallet`
- `cambioUSDEUR.db`, `crypto.com.fiatwallet.db`, `crypto.com.cardwallet.db`, `crypto.com.dati.db`, `crypto.com.fiatwallet.tipimovimentiPers.db`

### Database layer (`DatabaseH2.java`)

Three embedded H2 connections, static fields, all opened by `CreaoCollegaDatabase()` (which also creates/migrates every table;
returns `false` if another session holds the file lock):
- `connection` → `database.mv.db`: session options and token/exchange registries and metadata caches. **Not** the movements.
- `connectionPersonale` → `personale.mv.db`: user preferences and user-owned data (`OPZIONI`, `PrezziNew` of hand-entered
  prices, `WALLETS`, `WALLETGRUPPO`, `GRUPPO_ALIAS`, `EXCHANGEAPI`, `EMONEY`, `GIACENZEBLOCKCHAIN`, `EXCHANGETOKENS`, ...).
- `connectionPrezzi` → `prezzi.mv.db`: fetched price cache (`PrezziNew`, `PrezziKO`), custom tuning flags
  (`AUTO_COMPACT_FILL_RATE=0`, `RETENTION_TIME=0`).
Settings: `Opzioni_Scrivi/Leggi` (database.mv.db) and `Pers_Opzioni_Scrivi/Leggi` (personale.mv.db), restored in `Principale.AggiornaSpunte()`.
Traps:
- **The price DB must be compacted on command** (`CompattaPrezzi/CompattaPrincipale(Riapri)`, `SHUTDOWN DEFRAG`; error 90121 is
  expected). The connection object is **replaced**: never cache `connectionPrezzi` in a field or hold a `PreparedStatement`
  across the call. Deleting unreachable rows is deliberately not done.
- **The personal `PrezziNew` has an extra `gruppo` column** and a key one column longer than the cache's. It holds the user's
  year-end prices (Quadro RW is built on them): never drop and recreate it. `DammiPrezzoDaDatabasePersonale` ignoring the
  personalised price for a non-blank source is a **deliberate decision**, not a bug.
- `PrezziKO` (in `connectionPrezzi`) caches tokens/dates where the price lookup failed.
Dettagli: `nocommit/Documentazione/Dettagli_DatabaseH2.md`, `Analisi_DB_Prezzi_Manutenzione.md`.

### Main window (`Principale.java`)

Central JFrame holding the static maps other classes read: `MappaCryptoWallet` (all movements), `Mappa_ChainExplorer`,
`Mappa_AddressRete_Nome`, `MappaRetiSupportate` (populated at startup by `VarCondivise.CompilaMappaChain()` /
`CompilaMappaRetiSupportate()`). Traps:
- **Mass operations must not reuse the single-item function in a loop**: every dialog close refocuses the main window and
  `TabellaCryptodaAggiornare` triggers a full `Funzioni_AggiornaTutto()`. Pattern (bulk SCAM removal): a `…SenzaConferma`
  variant, one modal `Download` progress window with the loop on another thread, a single `Funzioni_AggiornaTutto()` at the end,
  then `TabellaCryptodaAggiornare = false`.
- `PopUp_IDTrans` (first selected row) and `PopUp_IDTransSelezionati` (all) must be refilled by **every** table that opens the
  movements popup in `Funzioni_RichiamaPopUpdaTabella` (including `GUI_DettaglioTransazione`), or the menu acts on the previous selection.
- I cinque contatori del pulsante "Errori" non nascono insieme: tre dal ciclo di `TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa`,
  due (`NumErroriGiacenzeNegative`, `NumErroriMovimentiCollegati`) dal thread di `Funzione_CaricaTabelleSecondarieInBackgroud`:
  il testo sta in `Errori_AggiornaPulsante()`, chiamato da entrambi i punti.
- "Movimenti collegati incoerenti" controlla i `[20]` (simmetria): una nuova classificazione che scriva `[20]` in un verso solo
  compare come errore, o la si scrive simmetrica o la si dichiara in `MovimentiCollegati.FormaPrecedenteDual`.
- "Giacenze negative" (`Funzioni.ControllaSaldiNegativi`, per exchange + sotto-wallet) **non è un doppione** di "parte del LiFo
  mancante" (lettera `A` in `v[38]`, per gruppo wallet): le etichette del dialogo devono continuare a distinguerle.
  Il dialogo del LiFo (`Principale_LifoMancante`) conta i depositi che stanno nella giacenza ma non nella pila, e
  `CausaleDepositoRiconosciuta` è una **copia** della catena dei depositi di `ElaboraMovimento`: un ramo nuovo nel motore va
  aggiunto anche lì (`Principale_LifoMancanteTest` lo verifica).
Dettagli: `nocommit/Documentazione/Dettagli_Principale_GUI.md`.

### Separating GUI from operational logic (ongoing refactor)

`Principale.java` is ~16.000 lines because GUI and logic grew together. **Direction of travel: `Principale` holds the Swing
wiring, each tab's operational logic lives in a companion class `Principale_<Tab or area>`** (existing: `_GiacenzeaData`,
`_Opzioni_Pulizie`, `_QuadroRW`, `_Movimenti_SeparaUnisci`, `_MovimentiCollegati`, `_Dichiarazioni`, `_CommissioniCollegate`,
`_FiltriMovimenti`, `_GruppiWalletRW`, `_EmoneyMancanti`, `_RecuperoScarti`...). Mirror the existing pattern (provisional rules):
- methods `public static`, **no Swing fields**, no reference to `Principale`; what is needed is passed in (the `JTable`, a
  `Window owner`, a wallet name); shared state via the static maps (`import static Principale.MappaCryptoWallet`);
- the method returns what the GUI needs to react (typically a `boolean` "something changed"): **refreshing tables stays in
  `Principale`**, so the handler is a thin delegation;
- pragmatic, not strict MVC: extracted classes still open `AppDialog` confirmations and set wait cursors using the `Window`.
When adding a feature to a tab that has a companion class, put the logic there. **Extracting further areas is planned work to
do with the user: do not undertake a broad migration unprompted.** Table of what each class holds: `nocommit/Documentazione/Dettagli_Principale_GUI.md`.

### Il costo di carico delle rimanenze in "Giacenze a data"

Il costo di carico LIFO accanto a "Valore (in Euro)" lo produce `Principale_GiacenzeaData.CalcolaCostiCaricoRimanenze`. Invarianti:
- **La passata non è filtrata per wallet, la lettura sì**: un giroconto interno non porta con sé nessun costo (`v[16]`/`v[17]`
  vuoti), quindi una pila costruita sul solo wallet selezionato mostrerebbe giacenze a costo zero. `CostiDettaglioToken.Avanza`
  va chiamato su **tutti** i movimenti anteriori alla data. Pila per gruppo wallet, chiave di riga `Moneta;Tipo;Address;Rete`.
- **I costi non si ricalcolano**: lotti da `v[17]`. Le regole di carico/scarico (`ElaboraLotti`) sono la **terza** copia dopo il
  motore e la PARTE 2 di `Calcoli_RT`.
- Colonne derivate della principale (7-9) **vuote, non zero** per FIAT, giacenza ≤ 0 e (la differenza) token senza prezzo.
  *Errori* è la **10**, *InfoPrezzo* l'**11**.
- Tabella dettaglio: costi in coda al model (**13-17**, blockchain **18**), l'ID è alla colonna 8 **del model**. Ogni lettura va
  fatta con `getModel().getValueAt(convertRowIndexToModel(riga), N)`, mai `getValueAt(riga, N)`.
- Il prezzo unitario nullo non è un prezzo (`v[15]` ha due decimali). Il prezzo di mercato **non si cerca durante la costruzione
  della tabella**: si completa in background (`CompletaPrezzoDettaglio`, scartato se la generazione è cambiata).
- *Qta Blockchain* / *Qta OKX* / *Qta Binance*: un saldo non letto è **"n.d.", mai zero**; i nodi `nodiStorico` si aggiungono solo
  dopo averli verificati a due altezze (alcuni rispondono `0x0`); si confronta il solo sotto-wallet `"Wallet"`.
- Scheda *Grafico*: i prezzi giornalieri **non sono prezzi fiscali** e non toccano mai `PrezziNew` né i marcatori (tabella propria `PrezziGiornalieri`).
- L'altezza del pannello giacenze del dettaglio sta nel layout (`.form` e `initComponents`), non in un `setPreferredSize`.
Dettagli completi: `nocommit/Documentazione/Dettagli_GiacenzeaData.md`, `Analisi_Giacenze_Exchange.md`.

### Filtri della tabella movimenti — due meccanismi, non uno

I **filtri di riga** (`RowFilter`: colonne dell'header, campo di ricerca) agiscono su un modello già costruito; i **filtri di
caricamento** (wallet/gruppo, token, documento di origine, date, TI, SCAM, senza prezzo, LiFo mancante) decidono quali righe
esistono, vivono in `Principale_FiltriMovimenti.FiltriMovimenti.Passa()` e cambiarli costa un ricaricamento. Il documento di
origine **non può** essere un `RowFilter` (il model dichiara 40 colonne, `Converti_String_Object` ne produce 46). Il filtro per
colonna è `Tabelle.FiltroValori` (i valori vivono solo in `tableFilters`). La scheda *Filtri* e l'imbuto dei dettagli derivano da
**una lista sola** (`RigheCaricamento` + `RigheDiRiga`) e si ricompongono da `FiltriMovimenti_AggiornaIndicatori`.
`Tabelle.java` è CRLF. Dettagli: `nocommit/Documentazione/Dettagli_FiltriMovimenti.md`, `Analisi_Filtri_Movimenti.md`.

### The movement row model — read this before touching any calculation

A movement is **not** a class: it is a `String[]` of `Importazioni.ColonneTabella` (currently **46**) columns, stored in `Principale.MappaCryptoWallet` keyed by column `[0]` (the movement ID). Every engine addresses fields positionally — `v[5]`, `v[16]`, `v[18]`, `v[33]` — so:

- Changing `ColonneTabella` or reordering columns breaks the import pipeline, the calculation engines, the tables and the CSV round-trip at once.
- `[0]` is the ID; its last `_XX` segment is the **categoria** (read by `Calcoli_PlusvalenzeNew` as `IDTS[4]`). `[5]` is the transaction-type description, `[18]` the subtype/marker.
- `nocommit/Documentazione/Analisi_Campo5_Campo18_Categoria.md` is the reference for which campo5/campo18/categoria combinations are legal and where each is produced.
- `[42]` is the **lignaggio**: a random `UUID` written the first time a movement is modified by hand, and the key of its modification history (`MOVIMENTI_STORICO`). Blank means "never modified manually". It must be **copied verbatim** by anything that rebuilds a movement — that is why it is deliberately absent from `MovimentiCrypto.CampiNonCopiabiliVerbatim` and present in `Principale_Movimenti_SeparaUnisci.CampiDaRiportare`; dropping it orphans that movement's history. See `MovimentiStorico` and `nocommit/Documentazione/Analisi_Storico_Modifiche_Movimenti.md`, section "v6".
- `[43]` is the **linked-fee group key** (`CommissioniCollegate`): one shared key written on a fee (`CM`) **and** on the
  movement(s) it belongs to. **Informative only** (no engine reads it, outside `Impronta` and the golden master). **Fees
  only**: the Dual contract key lives in `[45]`.
- `[44]` is the **derivative marker** (`Derivati`, type from the config key `causaliDerivati`), identification only: derivatives
  are still computed as crypto-attività, wrong on purpose and warned. **Do not** put derivative markers in campo 18:
  `ElaboraMovimento` silently skips a DC/PC whose campo 18 it does not know.
- `[45]` is the **operation key** (`GruppoOperazione`): today only `Binance_DualInvestment.Abbina` writes it, `DUAL-<id>`, on
  Purchase and Settlement. A generated movement never carries it and **derives** it from its `[20]` group (`ChiaveEffettiva`):
  the group is `[20]`, the identity `[45]`. Separate from `[43]` and from `[20]` (the engine reads `[20]`, `Impronta` hashes
  it). Informative only.
- Copy rules of `[42]`/`[43]`/`[45]`: absent from `MovimentiCrypto.CampiNonCopiabiliVerbatim`, present in
  `Principale_Movimenti_SeparaUnisci.CampiDaRiportare` and `CampiProvenienzaUnione`. *Duplica* (`Funzioni.DuplicaMovimento`)
  drops them and refuses group members and generated `AU` rows (`isDuplicabile`).
- Short rows are padded to 46 via `Importazioni.RiempiVuotiArray()` on load (this sets `VersioneCambiata`); a backup made with 46
  columns is refused by versions that handle 45.
- The table model stops at `v[39]` plus derived tail columns; operations are shown through the derived column **42
  "Operazione"**. A new tail column needs `Principale.form` (the `<Column>` **and** the `columnCount` of `<Table>`), **all three**
  arrays in `initComponents` (names, `types`, `canEdit`: a missing `types` makes the app not start, no unit test sees it, launch
  it on the virtual display), the loading loop and `LayoutColonneMovimenti.COLONNA_MASSIMA`.
- **The operation of a movement is computed, never stored** (`OperazioniCalcolate`): union-find over `[20]`, `[43]`, `[45]` and
  the same `[24]` on the same `[3]` within an hour. Display and filter only; never save or cite its ID.
Full text of these points: `nocommit/Documentazione/Dettagli_ModelloRigaMovimento.md`.

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

`GRUPPO_PERIODO_RW` (`personale.mv.db`) è la tabella unica dei periodi di detenzione per gruppo wallet, un rigo per periodo,
`TipoRigo` CRYPTO o FIAT (logica in `Principale_GruppiWalletRW`, GUI `GUI_PeriodiDetenzioneRW`/`GUI_ModificaPeriodoDetenzione`).
I dati dell'intermediario vivono **solo** sul rigo FIAT; ogni operazione scrive subito via `salvaAdesso()` (niente *Salva*).
Codice bene 14: IVAFE o solo monitoraggio è una scelta dell'utente. Le due giacenze medie FIAT hanno divisori diversi (conto
corrente /365, liquidità /giorni del tratto): **non è un doppione da unificare**. Dettagli: `Dettagli_Quadri_RW_RT.md`,
`Analisi_QuadroRW_Periodi_IVAFE.md`, `Analisi_QuadroRW_Codice14.md`.

### L'archivio delle dichiarazioni (`Dichiarazioni/`) e l'avviso E-Money

Ogni stampa è una dichiarazione archiviata: `Dichiarazioni/<anno>_<RW|RT>_<data>/` con due PDF, un Excel e `manifest.json`. Da non rompere:
- Un solo flusso per i due pulsanti (`RW_GeneraDichiarazione`/`RT_GeneraDichiarazione`): i dialoghi **prima**, poi
  `Principale_Dichiarazioni.Genera` scrive in `.in_corso_*` e rinomina solo se tutto è valido; gli scrittori non hanno dialoghi e lanciano eccezione.
- Il **manifest è l'unica fonte** (nessuna tabella nel DB: il ripristino aggiunge cartelle, non le cancella). I valori archiviati
  sono la fotografia della tabella: **un'opzione nuova che cambia i valori dei quadri va aggiunta anche in
  `RW_BozzaDichiarazione`/`Dichiarazioni_OpzioniComuni`**. L'Excel W/RW usa la stessa fotografia del calcolo (`RW_FotografiaGiacenze`).
- Dopo ogni calcolo `RW_ControllaDefinitiva`/`RT_ControllaDefinitive` confronta con la definitiva e l'etichetta rossa dice l'effetto, mai cosa fare.
- I documenti di origine copiati stanno **fuori** da `Totali`/`Righi`/`Opzioni`/`Errori`. La Continuità solo a richiesta.
- Avviso E-Money (`Principale_EmoneyMancanti`, `EmoneyToken`, elenco in `config/varie/EmoneyToken.json`): solo sui pulsanti *Calcola*.
- **Mai `new PdfReader(percorso)` su un file che poi si sposta o si cancella**: OpenPDF lo mappa in memoria, su Java 9+
  non riesce a rilasciare la mappa e su Windows il file resta bloccato fino al GC (la rinomina di `.in_corso_*` falliva
  sempre). Si passa il `byte[]` (`Stampe.PdfValido`, `Principale_Normativa.EstraiPdf`). Su Linux non si vede.
Dettagli: `nocommit/Documentazione/Dettagli_Quadri_RW_RT.md`.

### Le note di compilazione dei quadri e i file di `config/`

- I testi stampati in coda ai quadri vivono in `config/varie/NoteCompilazione.json` (`NoteCompilazione.java`), non nel codice.
  Gli anni sono segnaposto `{anno}`/`{annoPrec}`/`{annoSucc}`; una chiave datata (`W8.2024`, `T.2025`) vale «da quell'anno in
  avanti» e ripiega sulla chiave nuda, quindi correggere solo il 2024 richiede **due** chiavi (`W8.2024` e `W8.2025` col testo di
  prima). Il quadro T/RT non si sceglie con un `if (Anno<…)` nel codice. Una nota mancante dichiara di mancare **nel report**.
  Il blocco "OPZIONI SCELTE PER IL CALCOLO" resta nel codice. `NoteCompilazioneTest`.
- **Ogni file di `config/varie`/`config/importmappe` tenuto in memoria va nel registro `RicaricaConfig.VOCI`** (o in
  `LETTI_A_OGNI_USO`): `RicaricaConfigTest` fallisce altrimenti. Va anche in `MappeCausali.FILE_VARIE`/`FILE_DI_SISTEMA` per essere installato.
- `Simboli.json` e `StatiEsteri.json` (cambiare un `privilegiato` cambia l'aliquota IVAFE, senza versione) non hanno liste di
  ripiego nel codice: il default è la copia nel jar. `"versioneMinimaApp"` al primo livello fa usare il jar alle versioni vecchie.
  `Parametri.json` (default fissati da un test) e `Runtime.json` (versioni di Node e CCXT) hanno note nel file di dettaglio.
- La direzione: le cose piccole da file di configurazione su GitHub aggiornati all'avvio, il versionamento per bug e nuove funzioni.
Dettagli: `nocommit/Documentazione/Dettagli_Configurazione_Note.md`.

### Movement types (`MovimentiCrypto.java`)
Defines the canonical movement type map. Each raw label from an import (e.g. `STAKING REWARDS`, `DUST-CONVERSION`) is normalised to a short code (e.g. `RW`, `SC`). Understand these codes before touching import or calculation code.

**Whenever a code change adds or modifies a campo5/campo18/categoria combination** (new movement classification, new marker, new auto-assignment rule, etc.), update `nocommit/Documentazione/Analisi_Campo5_Campo18_Categoria.md` to document the new combination in the same style as the existing tables/notes.

### Import pipeline
- `Importazioni.java` — dispatcher and shared utilities; `ImportazioneGenerica.java` — JSON-configured import for arbitrary
  CSV formats (`config/import/`); `Importazioni_Gestione` / `Importazioni_Resoconto` — the Swing wrapping; exchange-specific
  classes (`CDC_FiatECardWallet`, `BinanceTaxReportClient`, `CcxtInterop`); DeFi/blockchain (`Funzioni_WalletDeFi`,
  `TransazioneDefi`, `Trans_Solana`, `ERC20MetadataReader`).
- **BASE is read from public RPC nodes** (`NodoPubblicoDefi`, config in `Reti.json`), used **from now on only**: do not propose
  re-downloading old history. Wallets with code (ERC-4337, EIP-7702) are refused.
- **In a generic-CSV config `nomeExchange` becomes `[3]` and `nomeWallet` `[4]`, and `[3]` is load-bearing twice**: it is part
  of the re-import dedup key (`F_buildKeyMovimento`, instant to the second) **and** of the wallet-group lookup. Changing a
  config's `nomeExchange` re-imports everything as duplicates and moves the movements to another group.
- Config primitives with traps: `fusoPerPeriodo` (never add to a config already in use: moves the instant, duplicates on
  re-import), `fusoDaIntestazione`, `colonneCalcolate`, `regoleRiga`, `coppia`/`importoQuote`, `tipoSeSola`,
  `noteAcquistoDaSaldo`, `tipoSeCrypto` (type by coin: FIAT keeps `mappaCausali`), `walletSpecularePerCausale` (the causale must also be in `causaliChiuse`), `causaliAllertaDerivati` /
  `causaliDerivati`. A config using new code needs a `versioneMinimaApp`, **except a config already in use while
  installations up to 1.0.65.07 are around**: they overwrite their copy and hide the entry (no jar copy for
  `config/import`). Later versions keep the local copy when the downloaded file needs a newer version
  (`Funzioni.TrattieniCopiaLocale`). A key read by `ConfigurazioneImport.carica` must be added to `CHIAVI_NOTE`
  (unknown keys are listed in the import report; `ChiaviConfigImportTest`).
- **Bitget via API** (`Bitget_API`, Tax API, key permission `taxr`, Java-only, no Node): each record becomes a row of
  the CSV export and goes through `Bitget CSV.json` via `ImportazioneGenerica.importaRighe`. API type codes that differ
  from the CSV labels go in the config's `causaliAPI`, never in code. The daily interest sum takes the date of the **last**
  row of the day (`consolidaCausaliPerGiorno(..., true)`), the CSV keeps the first: that is what makes "last movement
  + 1 s" restart safe. Bitget CSV exports use a fixed offset that differs per export, so CSV and API movements never dedupe.
- `girocontoFiat` (`GirocontiFiat`): euro moved between two own wallets paired as PTW/DTW; declare the rule mirrored in both configs.
- Binance Simple Earn, Dual Investment (`Binance_DualInvestment.Abbina`, same coin vs different coin, bug C18),
  Bybit deposits/withdrawals enrichment: see the details file.
- **Binance Auto-Invest** (`Binance_AutoInvest.Abbina`, Subscription + Rebalancing + Redemption files together): same
  form as the Dual same-coin case on sub-wallet `Auto-Invest`. The Rebalancing file has **no direction**: the signs
  come from the exact closing of each coin (subscriptions ± rebalancings − redemptions = 0), prices only as fallback.
  Rebalancings are direct crypto-crypto swaps by the user's decision. Details and limits: `Analisi_Binance_AutoInvest.md`.
- **Scambio differito**: never remove several movements with a loop over IDs collected beforehand, use `Funzioni.RimuoviMovimenti`;
  `GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito` returns `false` having touched nothing if it cannot make the
  IDs unique, and every caller must check it (bug C17). A re-import must recognise a movement that no longer has the file's form
  (`FormeImportate`).
- Field `[32]` ("prezzato") has three states: `"SI"`, `""`/`null` (re-check), `"NO"` (don't ask again).
- `AttesaConnessione`: after three retries over nine minutes the import **aborts rather than writing unpriced data**.
Full mechanics: `nocommit/Documentazione/Dettagli_Import.md` and `Analisi_Import_Meccanismi.md`.

### Price fetching (`Prezzi.java`)

Several exchanges in parallel via OkHttp, cached in `connectionPrezzi`; CCXT goes through the Node layer. Rules to keep:
- **Which leg prices a two-coin movement**: the FIAT leg if any, otherwise the **incoming** leg, the outgoing one only if the
  incoming has no (or zero) price or is not quoted within `PRECISIONE_PREZZO_MS` (5 min) while the outgoing is
  (`Prezzi.OrdineGambe`, shared by `DammiPrezzoInfoTransazione` and `MovimentiCrypto.DammiMonetaPrioritaria`). Every source
  must store the **quote** time. CoinMarketCap homonyms are guarded (`SCOSTAMENTO_MAX_COINMARKETCAP`, `ScegliOmonimo`; the
  coin name lives in the cache's exchange column only). **Stored values are not recomputed**: the rule applies to prices
  computed from now on.
- **`config/varie/AliasPrezziToken.json` changes fiscal numbers without a version bump**: edit it as a fiscal change. Each
  entry after the first 12 carries `dal` (ISO date, strictly after) so past declarations do not move; always read aliases
  through `AliasPrezziToken.Alias(address, rete, istante)` / `StessoPrezzo(simbolo, istante)`, never the two maps. A new entry
  added in year N gets `dal = N-01-01`. `Calcoli_RW_GoldenMasterTest` is the gate.
- Address tokens outside the CoinGecko list go to DefiLlama (`PrezziDefiLlama`, only `/batchHistorical`), fetched in batches
  **before** valuation and only for pairs with no local price and no KO (this is the whole protection of past declarations).
  Imports (EVM, Solana, OKX, Binance) pre-fetch with the **same source as their consumer**.
- Option `OPZIONE_DEFI_SOLO_DEFILLAMA_COINGECKO` (off by default) skips the exchange-by-symbol fallbacks; no `PrezziKO` is
  written for a price it blocks.
- The remote on-chain price service is **opt-in**: do not flip `ServizioPrezziClient.OPZIONE_ABILITATO_DEFAULT` to `"SI"`
  without asking. The local CCXT path downloads a whole day per `(coin, day)` tracked by `PrezziGiorniCCXT`.
Full mechanics: `nocommit/Documentazione/Dettagli_Prezzi.md`, `Analisi_Prezzo_Gamba_Scambi.md`, `Analisi_Alias_Prezzi_Token_CCXT.md`,
`Analisi_Prezzi_LP_DefiLlama.md`, `Analisi_VPS_Prezzi_Sito.md`, `Analisi_Prezzi_Scaricamento_Costi.md`.

### The `Scripts/` Node.js layer

Exchange and price logic partly lives in JavaScript run **out of process on a real Node binary** (no embedded JS engine,
despite the leftover `graalvm.version` in the pom). `CcxtInterop` downloads Node (`NODE_VERSION`) into `tools/node` and
installs ccxt. Traps:
- **Both versions are pinned and self-correcting** (`CcxtInterop.CCXT_VERSION` is reinstalled whenever `node_modules/ccxt`
  differs): bump the constant when a script starts relying on a newer endpoint (bug C14). Skipped when `NODE_ESTERNO != null`
  (flatpak/MSIX). npm comes inside Node; `isolaConfigNpm()` keeps the machine's `.npmrc` out.
- Script arguments are consumed **positionally**: a new argument goes at the **end**, never in the middle.
- `Historical_Multi_Eur.js` also runs as a long-lived `--servizio` process (`ServizioNodePrezzi`): module-level caches must be
  reset per message or expire.
- A change to Binance import or CCXT price fetching may belong in `Scripts/*.js` rather than in Java: check both sides.
- **OKX**: the API covers the whole history; `billId` and `ordId` are not interchangeable; an incomplete download imports
  **nothing** (C13); `after` means a `billId` on Trading but a timestamp on Funding (C8); the recognised hostname must be
  fed back into the caller (C9). Simple Earn reconstruction (`OKX_InteressiEarn`) is **off by default**, the program books only
  what the API returns. Movements skipped as unknown are recovered from the NDJSON documents (`ScartiImport`): any new
  pre-processing step of the import must be mirrored in `ScartiImport.LeggiRigheOKX` and `VERSIONE_RILETTORE_OKX` bumped. The
  OKX Card wallet is found on-chain (`OKX_WalletCarta`), X Layer is imported from the public node (`Trans_XLayer`).
- Testing an unpublished `config/` change needs `-Dconfig.nonAllineare=true` (startup alignment overwrites local edits).
Full mechanics: `nocommit/Documentazione/Dettagli_Scripts_Node_OKX.md`, `Analisi_Bug_Criticita.md`, `Analisi_Carta_OKX_XLayer.md`,
`Analisi_Recupero_Movimenti_Sconosciuti.md`.

### Source documents — field `[41]` and `DocumentiFonte`

Every imported movement carries in `[41]` the numeric id of its source file; copies live gzipped in `DocumentiFonte/`, the
registry is `DOCUMENTIFONTE` in `personale.mv.db`, driven by `DocumentiFonte.java`. Traps:
- `[41]` holds an **id only**, never a path (`;` is deleted, not escaped); it is outside `Impronta()` and the golden master.
- The dedup hash is of the *original* content, not of the `.gz` (gzip embeds an mtime); `Annulla` only on a registration with
  `Nuovo == true`; `DocumentoFonteCorrente` must not be reset in `AzzeraContatori()`.
- Deleting a document with movements deletes them too, **provisionally** (`AccodaCancellazione` → `SalvaBuffer` from
  `Scrivi_Movimenti_Crypto`; "Annulla" calls `ScartaBuffer`). `Leggi(int)` does not filter pending ids, `Elenco()` does.
- **Generated movements inherit `[41]`** from the movement they derive from: a new classification that generates movements
  must copy it. Credentials never enter the NDJSON documents (`DocumentiFonte.UrlSenzaChiave`).
Full mechanics: `nocommit/Documentazione/Analisi_DocumentiFonte_Movimenti.md` and `Dettagli_ChiediIA_DocumentiFonte.md`.

### "Chiedi a IA" — asking an external chatbot about a movement

Popup entry that hands one movement to a chatbot in the user's browser, no API key and no network call from the app:
`ChatbotIA` (list in `ChatbotIA.json` in the working directory, **data not code**), `PromptIA` (two privacy profiles,
`COMPLETO` and `GENERICO`), `GUI_ChiediIA` (always shows an editable preview before anything leaves the machine).
Details: `nocommit/Documentazione/Dettagli_ChiediIA_DocumentiFonte.md`.

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

A field holding a credential (API key, secret, passphrase) is a `JPasswordField` with `CampiSegreti.AggiungiOcchio` after
`initComponents()`; a table showing credentials masks them in the renderer only (`CampiSegreti.NascondiColonne`), never in
the model. `CampiSegretiTest` fails on a `.form` component named `*ApiKey*`/`*ApiSecret*`/`*Passphrase*` left as `JTextField`.

## Adding a new EVM-compatible blockchain

**Since 2026-10-13 a chain is a voce of `config/varie/Reti.json`** (`Reti`, in the reload registry): `id`, `nome` (the label
`"nome (id)"` is also the logo key), `defi`, `chainidEtherscan`, `simbolo`, `coingecko`/`defillama`, the three browser
pages (`urlTransazione`, `urlTokenWallet`, `urlWallet` with `{hash}`/`{wallet}`/`{token}`), `nodiStorico`/`senzaStorico`.
From it come both combos, `isValidDefiWallet`, the explorer links, `Mappa_ChainExplorer`, `MappaRetiSupportate` and the
historical-state nodes; `BASE` also carries `nodoPubblico` (import nodes and tokens) and `XLAYER` `tokenNodo` (card tokens,
order matters). For a chain covered by Etherscan V2 that is all (checked 2026-10-13: Moralis, the provider table and the price paths derive the chain from these maps), except the optional `GOPLUS_CHAIN_ID` entry if GoPlus covers it. **The explorer API URL is never written in the file**: it
receives the user's ApiKey, so `Reti.Rete.api()` builds it from `chainidEtherscan`, and the non-Etherscan ones
(`CRO`, `SOL`, `BTC`, `XLAYER`) are in `Reti.API_NON_ETHERSCAN`. Still in code, on purpose (behaviour or keys):
`Importazioni.DeFi_ProviderDefault`, `DeFi_ProviderBlockscoutUrl`, `CHAIN_ID_BLOCKSCOUT_PRO`,
`Principale.Opzioni_ProviderDefi_OpzioniPerChain`, `Principale.GOPLUS_CHAIN_ID`, and `GeneraLoghi` (dev tool). A node
added to `nodiStorico`/`nodoPubblico` must be verified at two heights first (some public nodes answer `0x0` instead of an
error). `ChainConfigurazioneCompletaTest` and `RetiTest` check the rest.

Keyless public services (DefiLlama, Banca d'Italia, mempool, public Coinbase/Binance candles, CMC historical data, GoPlus,
Cronos explorer) take their base URL from `config/varie/IndirizziServizi.json` (`IndirizziServizi.Url`, https, host and
path only). Endpoints that carry a key or a signature stay in code for the same reason as above.

## Packaging & distribution

A single workflow, `.github/workflows/release.yml`, triggered manually (`workflow_dispatch`), on Temurin JDK 25 (LTS: a non-LTS version stops getting security fixes after six months, and its runtime is what every package ships). It reads the version straight from `pom.xml`, builds the fat JAR with `maven-shade-plugin` (`-DskipTests`) and then packages it with `jpackage` in four jobs: `build-windows` (portable app-image, cross-platform portable JAR bundle, and an installer built with Inno Setup), `build-linux` (app-image + `.deb`), `build-macos` (app-image + `.dmg`, on an OS matrix) and `create-release`, which collects the artifacts and publishes the GitHub release via `softprops/action-gh-release`. Each job copies the runtime-side folders (`Scripts/`, `Immagini/`, `logo.png`) next to the JAR.

**The packaged runtime has an explicit module list, `packaging/runtime/moduli.txt`** (86 MB instead of the 127 MB
of jpackage's default set). After building the jar every job runs `java packaging/runtime/ModuliRuntime.java <jar>`,
which fails the build if jdeps finds a module missing from the list, then writes `MODULI_RUNTIME`/`OPZIONI_JLINK` to
`$GITHUB_ENV` for `--add-modules`/`--jlink-options` (`--include-locales=it,en`). jdeps cannot see modules loaded only
at runtime (`jdk.charsets`: without it OpenPDF text extraction fails with the bare message `MacRoman`;
`jdk.localedata`: `Locale.ITALY` formats in English; `jdk.zipfs`): those are added by hand, and a missing one shows
only in the packages, never from Maven or NetBeans. Check a change on a runtime built from the list (`jlink` without
`--strip-native-commands`, then `./mvnw test -Djvm=<runtime>/bin/java`; the tests alone also need `jdk.httpserver`).

**No bitcoinj, no Bouncy Castle in the jar** (2026-10-22, 14 MB of 29): xpub/ypub/zpub addresses come from
`IndirizziBitcoin` (BIP32 public derivation, RIPEMD-160, Base58Check, Bech32, JDK only, `IndirizziBitcoinTest`).
`bcprov` stays as a test dependency for Keccak. OpenPDF needs it only to decrypt AES PDFs, which the app does not read:
`Principale_Normativa.Estrai` catches the resulting `LinkageError` so such a document just drops out of the search.
The form templates in `Immagini/` must stay unencrypted: `Stampe.Aggiungi*` do not catch it.

### Automatic updates and the beta channel (`Aggiornamenti`, `AggiornamentoInstallazione`)

The app checks GitHub Releases: stable = `releases/latest`, beta = the list including prereleases, highest version by
`Funzioni.ConfrontaVersioni`. Betas are **prereleases in the same repo**, built by `release.yml` with `canale = beta`
(installer + portables only; `verifica-canale` rejects a 3-digit beta or a 4-digit stable). Traps:
- **Intermediate builds are numbered `<last stable>.NN`**, never `<next stable>.NN`, or the beta sorts after its stable.
- The tag must stay `Giacenze_Crypto_<version>` (`PREFISSO_TAG`; clients up to 1.0.65 also HEAD SourceForge on it).
- Self-install only for the Inno installer and the portables; `.deb`/AUR/flatpak/macOS/Store get a link. Portables are
  updated by **copying over, never deleting** (the default workdir is the launch folder, so user data may live there).
- Nothing is downloaded or installed without the user's consent; install closes through `Principale.ChiudiProgramma`
  (same path as the window's X) after an automatic backup. Windows path not yet tested on a real machine.
Details: `nocommit/Documentazione/Analisi_Aggiornamenti_Automatici.md`.

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

All the working `.md` documents (bug analyses, performance analyses, the change log, and the `Dettagli_*.md` that hold the
long mechanics once written here) live in **`nocommit/Documentazione/`**, which is in `.gitignore`: many contain the user's
own data, wallets and configuration. Find them with `ls` (`Analisi_<tema>.md`, `Pubblicazione_*.md`, `Dettagli_<area>.md`) and
`grep`; the ones to know by name are `StoricoModifiche.md` (change log, current month), `Analisi_Bug_Criticita.md` (bugs by ID,
referenced by the tests) and `Analisi_Campo5_Campo18_Categoria.md`. `nocommit/` also holds `GoldenMaster/`, `StrumentiTest/`
and `Prototipi/`. **Write new technical `.md` documents there**, not in the top-level `Documentazione/`, which is historical
since 2026-08-18 (still live there: `IstruzioniVarie.txt`, `MappatureImport.txt`, the `.pptx`, `FormuleUtili.xlsx`,
`Schemi.odg`). Change-log entries before 2026-08-24 cite the old `test/Documentazione/…` paths: historical, do not rewrite.
Tables of the former document index and the full mechanics: `nocommit/Documentazione/Dettagli_Documentazione.md`.

## User documentation — Markdown under `docs/`, published by GitHub Pages

The manuals the application opens are pages whose **only source** is the Markdown in `docs/documentazione/` (the `.odt`
in `Documentazione/` are not updated any more). `DocumentiAiuto` holds one constant per page plus `NOVITA_VERSIONI`.
- The PDFs next to the pages are **generated** (`docs/strumenti/genera-pdf.sh` → `GeneraPdfDocumentazione`): re-run after
  editing a page; `DocumentiAiutoTest` fails if either half is missing. It converts only the Markdown subset the manuals use.
- Headings carry explicit `{#ancora}` ids; images live in `docs/documentazione/immagini/<pagina>/`; screenshots are taken on
  an **anonymised copy** of the data, never on the real archive.
- `changelog.md` is also inside the jar and feeds the "Novità della versione" dialog (`NovitaVersione`, `GUI_NovitaVersione`):
  the notes of a version must be written in its `## Versione` section (a test build shows the **topmost** one) before
  publishing. `NovitaVersione.Html` converts only a Markdown subset; `NovitaVersioneTest` checks it.
- Startup dialogs go in `Principale.AvvisiAvvio`, one after the other in a single `invokeLater`.
Full rules: `nocommit/Documentazione/Dettagli_Documentazione.md`.

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
