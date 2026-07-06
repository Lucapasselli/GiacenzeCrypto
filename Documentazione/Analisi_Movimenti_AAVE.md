# Analisi movimenti AAVE — wallet Arbitrum

> Promemoria basato sull'analisi di `test/2025/movimenti.crypto.db` (62 righe relative ad AAVE, wallet `0x598d4d62424c54e7611253e5b83319c5affd9018` su rete ARB). Il gestionale non ha logica dedicata ad AAVE: tratta questi movimenti in modo generico come scambi/depositi/prelievi di token ERC20. L'interpretazione "protocollare" qui sotto è ricostruita a partire dai dati on-chain presenti nel file.

## Come ragiona AAVE (i due tipi di token)

AAVE non tiene un "saldo" in un database interno: rappresenta tutto con token ERC20 che vengono **coniati (mint)** e **bruciati (burn)**. Nel wallet compaiono due famiglie di token:

| Tipo token | Esempio | Cosa rappresenta |
|---|---|---|
| **aToken** (collaterale) | `aArbUSDT`, `aArbLINK`, `aArbWETH`, `aArbUSDCn` | Ricevuto quando depositi (`supply`). È la tua quota di credito verso il pool: matura interessi automaticamente (rebasing), quindi il saldo cresce nel tempo anche senza nuove transazioni. Viene bruciato quando prelevi (`withdraw`) o quando fa da garanzia in una liquidazione. |
| **debtToken variabile** | `variableDebtArbLINK`, `variableDebtArbUSDT`, `variableDebtArbWBTC`, `variableDebtArbUSDCn`, `variableDebtArbWETH` | Coniato a **tuo carico** quando prendi in prestito (`borrow`): non lo ricevi come asset utile, è la "fotografia" del tuo debito che cresce con gli interessi. Viene bruciato quando ripaghi (`repay`) o quando il debito viene liquidato al posto tuo. |

Ogni operazione è quindi sempre un mint o un burn (o entrambi nella stessa tx) di questi gettoni, mai un vero "trasferimento" di soldi in senso classico.

## Le funzioni del contratto che appaiono nei dati

| Metodo AAVE | Cosa succede | Effetto sul bilancio |
|---|---|---|
| `supply` / `supplyWithPermit` / `depositETH` | Depositi collaterale | Bruci l'asset (es. USDT), conii l'aToken (es. aArbUSDT) 1:1 |
| `borrow` | Prendi un prestito | Conii il debtToken (es. variableDebtArbUSDT) — non c'è un "OUT", solo un IN: è debito, non un guadagno |
| `withdraw` / `withdrawETH` | Ritiri il collaterale | Bruci l'aToken, ricevi l'asset sottostante |
| `swapDebt` | Cambi la valuta del tuo debito (es. da debito LINK a debito USDT) | Brucia il vecchio debtToken, conia il nuovo, senza toccare liquidità reale |
| `flashLoanSimple` (usato qui come collateral-swap) | Cambi il collaterale da un asset a un altro senza chiuderlo prima | Brucia l'aToken vecchio, conia il nuovo, usando un flash loan "sotto al cofano" per fare lo swap atomico |
| `swapAndRepay` | Usi parte del collaterale per ripagare debito in un'altra valuta | Brucia aToken (collaterale) e debtToken (debito) nella stessa tx |
| `repayWithATokens` | Ripaghi il debito usando direttamente l'aToken invece del token sottostante | Brucia aToken e debtToken contemporaneamente, 1:1 in USD, senza mai uscire dal protocollo |

## Timeline della posizione (ricostruita)

**Fase 1 — Apertura posizione (24 ago – 29 set 2025)**
Depositi progressivi come collaterale: USDT, LINK, WETH (via `supply`/`depositETH`), e prestiti aperti prima in LINK, poi USDT, poi WBTC, spostando il debito da un asset all'altro con `swapDebt` più volte (24/8, 3/9, 15/9, 18/9, 25/9 x2, 2/10). Tipico di chi rincorre il tasso d'interesse più conveniente sul debito, senza mai davvero "ripagare" nulla — è solo un cambio di denominazione del debito.

**⚠️ Evento 1 — Liquidazione parziale (10 ott 2025, 23:17)**
Tx `0x37d562f9...`: nella stessa transazione vengono bruciati **contemporaneamente** `aArbLINK -52.88` e `variableDebtArbUSDCn -806.000001`, **senza alcun incasso per il wallet**. Firma classica di una `liquidationCall`: un liquidatore terzo ripaga il debito USDCn e in cambio si prende (brucia a carico del wallet) il collaterale LINK + bonus di liquidazione. Avvenuto solo ~5 ore e mezza dopo l'apertura di quel prestito USDCn (17:44 borrow → 23:17 liquidato): la posizione era probabilmente già vicina alla soglia di liquidazione o c'è stato un movimento di prezzo sfavorevole rapido.

**Fase 2 — Chiusura volontaria del resto (11 ott 2025, 22:41–22:43)**
Tre transazioni distinte in sequenza: `flashLoanSimple` (chiude parte del debito USDCn usando USDT come collaterale), `swapAndRepay` (ripaga altro debito usando LINK come collaterale), infine `withdraw` del LINK residuo. Chiusura ordinata fatta volontariamente, verosimilmente tramite un'interfaccia/aggregatore che orchestra queste operazioni AAVE V3.

**Fase 3 — Posizioni piccole/test (13 feb – 18 apr 2026)**
Supply/withdraw isolati di LINK e WETH, piccolo borrow WBTC aperto e in parte ripagato con importi minimi (dust).

**Fase 4 — Nuova posizione USDC/WBTC (2 mag 2026)**
Depositi USDC → borrow USDCn → `repayWithATokens` (ripaghi il debito usando direttamente l'aToken, comodo perché non serve liquidità esterna) → altri supply/withdraw.

**⚠️ Evento 2 — Seconda liquidazione (3 mag 2026, 22:37)**
Stessa identica firma: tx `0xf1ebd694...` brucia insieme `aArbUSDCn -4.14` e `variableDebtArbWBTC -0.0000497`, senza incasso. Importi piccoli ma stesso meccanismo di liquidazione automatica.

**Fase 5 — Posizione attuale aperta (5 lug 2026)**
Ultimo movimento nel file: supply USDT (250) + borrow WETH (0.09) + supply WETH (0.18) → posizione ancora aperta a fine dataset.

## Verifica di quadratura (bilancio netto sommando tutti i movimenti)

```
aArbLINK                0.00   → posizione chiusa (aperta e liquidata/prelevata per intero)
aArbUSDCn               0.00   → posizione chiusa
variableDebtArbLINK     0.00   → debito azzerato
variableDebtArbUSDT     0.00   → debito azzerato
variableDebtArbWBTC     0.00   → debito azzerato
--- posizione ancora APERTA ---
aArbUSDT              250.07   → collaterale attivo
aArbWETH                0.18   → collaterale attivo
variableDebtArbWETH     0.09   → debito attivo
```

Tutto torna: ogni token aperto è stato chiuso (a mano o per liquidazione) tranne la posizione più recente, ancora attiva con USDT + WETH come collaterale e un piccolo debito in WETH.

## Nota sulla classificazione fiscale nel gestionale

Il programma non ha logica dedicata ad AAVE: classifica questi movimenti in automatico come:

- **DC (Deposito a costo 0)** per i `borrow` — corretto concettualmente: ricevere un prestito non è un acquisto imponibile, non ha un "costo".
- **PC (Prelievo crypto)** per i burn "a vuoto" — sia per i repay diretti (`repayWithATokens`, `swapAndRepay`) sia per le due liquidazioni. **Da verificare a mano**: una liquidazione è fiscalmente una perdita del collaterale (evento di disposal potenzialmente rilevante), mentre un semplice repay volontario che brucia solo il debtToken non dovrebbe generare plusvalenza/minusvalenza essendo solo l'estinzione di un debito. Il programma oggi le tratta allo stesso modo (PC generico).

**Righe da ricontrollare manualmente per il Quadro RT:**
- `2025-10-10 23:17` — tx `0x37d562f938b9f1266c257779f205dc1317ad4a5f711b6e74c78b863d73b20e11` (liquidazione: aArbLINK -52.88 / variableDebtArbUSDCn -806.000001)
- `2026-05-03 22:37` — tx `0xf1ebd694657c76906f6d5024471b98ab87bc7db3147935ce00570daa2d3d8623` (liquidazione: aArbUSDCn -4.14 / variableDebtArbWBTC -0.0000497)

### Prossimo passo eventuale
Controllare come `Calcoli_PlusvalenzeNew.java` tratta effettivamente queste righe PC/DC nel motore LIFO, per capire se le liquidazioni producono un calcolo di plusvalenza sensato o se serve un aggiustamento manuale.
