//#!/usr/bin/env node
const fs = require('fs');
const path = require('path');
const os = require('os');

// Utility functions
function logError(message) { console.error(`[ERROR] ${message}`); }
function logInfo(message) { console.error(`[INFO] ${message}`); }
function logDebug(message) { console.error(`[DEBUG] ${message}`); }

// In Node.js v15+ unhandled rejections terminate the process by default.
// CCXT 4.x creates Coinbase FUTURE/PERPETUAL promises before awaiting them; if the
// endpoint is unreachable, Node.js sees a brief "unhandled" rejection and crashes
// before CCXT's own try-catch can intercept it. Registering this handler keeps
// the process alive so CCXT can handle the rejection itself.
process.on('unhandledRejection', (reason) => {
    logError(`Unhandled rejection (ignorato, gestito da CCXT): ${reason}`);
});

// Trova il miglior simbolo disponibile per XXX/EUR
async function findBestPair(ex, baseSymbol) {
   // const markets = await ex.loadMarkets();
    const markets = await getMarketsSymbols(ex);
    
    // Se esiste direttamente la coppia con EUR
    if (`${baseSymbol}/EUR` in markets) {
       // logInfo(`Trovato corrispondenza per EUR su ${ex.id}`);
        return { pair: `${baseSymbol}/EUR`, invert: false, needsConversion: false };
    }

    // Se esiste con USD
    if (`${baseSymbol}/USD` in markets) {
       // logInfo(`Trovato corrispondenza per USD su ${ex.id}`);
        if ("EUR/USD" in markets) {
            return { pair: `${baseSymbol}/USD`, invert: false, needsConversion: true, conversionPair: "EUR/USD" };
        }
        if ("USD/EUR" in markets) {
            return { pair: `${baseSymbol}/USD`, invert: false, needsConversion: true, conversionPair: "USD/EUR" };
        }
    }

    // Se esiste con USDT
    if (`${baseSymbol}/USDT` in markets) {
       // logInfo(`Trovato corrispondenza per USDT su ${ex.id}`);
        if ("EUR/USDT" in markets) {
            return { pair: `${baseSymbol}/USDT`, invert: false, needsConversion: true, conversionPair: "EUR/USDT" };
        }
        if ("USDT/EUR" in markets) {
            return { pair: `${baseSymbol}/USDT`, invert: false, needsConversion: true, conversionPair: "USDT/EUR" };
        }
    }

    // Se esiste con USDC
    if (`${baseSymbol}/USDC` in markets) {
       // logInfo(`Trovato corrispondenza per USDC su ${ex.id}`);
        if ("EUR/USDC" in markets) {
            return { pair: `${baseSymbol}/USDC`, invert: false, needsConversion: true, conversionPair: "EUR/USDC" };
        }
        if ("USDC/EUR" in markets) {
            return { pair: `${baseSymbol}/USDC`, invert: false, needsConversion: true, conversionPair: "USDC/EUR" };
        }
    }

    return null;
}



async function getMarketsSymbols(exchange) {
    const tempDir = path.join(os.tmpdir(), 'GiacenzeCrypto');
    if (!fs.existsSync(tempDir)) {
        fs.mkdirSync(tempDir, { recursive: true });
    }

    const marketsFile = path.join(tempDir, `markets_${exchange.id}.json`);
    const oneHourMs = 1 * 60 * 60 * 6000; // 6 ore
    let markets;

    const needReload = () => {
        if (!fs.existsSync(marketsFile)) return true;
        const age = Date.now() - fs.statSync(marketsFile).mtimeMs;
        return age > oneHourMs;
    };

    if (needReload()) {
        logInfo(`Ricarico markets da API per ${exchange.id}...`);
        markets = await exchange.loadMarkets();

        // Rimuove riferimenti circolari e valori non serializzabili
        const cleanMarkets = JSON.parse(JSON.stringify(markets));
        fs.writeFileSync(marketsFile, JSON.stringify(cleanMarkets, null, 2), "utf8");
    } else {
       // logInfo(`Carico markets da file locale ${marketsFile}`);
        const raw = fs.readFileSync(marketsFile, "utf8");
        markets = JSON.parse(raw);
    }

    return markets;
}






async function fetchHistorical(ex, symbol, timeframe, since, until, limit = 1000) {
    const all_ohlcv = [];
    let current_since = since;

    while (current_since < until) {
      //  logInfo(`scarico dati per ${ex.id} , ${symbol}`);
        const ohlcv = await ex.fetchOHLCV(symbol, timeframe, current_since, limit);
       // logInfo(`i dati sono ${ohlcv}`);
        if (!ohlcv.length) break;
        all_ohlcv.push(...ohlcv);
        const last_ts = ohlcv[ohlcv.length - 1][0];
        if (last_ts === current_since) break;
        current_since = last_ts + ex.parseTimeframe(timeframe) * 1000;
    }

    return all_ohlcv.filter(c => c[0] <= until);
}

/** Implementazione condivisa da `cercaPrezziStorici` e `cercaPrezziStoriciConEsito` (sotto): fa il
 * lavoro vero, la seconda espone anche quali exchange sono falliti con un errore vero. */
async function eseguiRicerca({ ccxtLib, exchangeIds, baseSymbol, timeframe, since, until }) {
    const results = {};
    const falliti = [];

    async function safe_fetch(exchange_id) {
        try {
            if (!ccxtLib[exchange_id.toLowerCase()]) {
                return [exchange_id, [], null];
            }
            const ex = new ccxtLib[exchange_id.toLowerCase()]();

            const pairInfo = await findBestPair(ex, baseSymbol);
            if (!pairInfo) return [exchange_id, [], null];

            // OHLCV principale (es. BTC/USDT o BTC/EUR)
            const baseData = await fetchHistorical(ex, pairInfo.pair, timeframe, since, until);

            let finalData = baseData;

            // Conversione se serve (es. BTC/USDT -> BTC/EUR usando EUR/USDT)
            if (pairInfo.needsConversion) {
                const convData = await fetchHistorical(ex, pairInfo.conversionPair, timeframe, since, until);

                // Creo una mappa timestamp -> conversion rate
                const convMap = {};
                for (const c of convData) {
                    convMap[c[0]] = c[4]; // chiusura
                }

                finalData = baseData.map(c => {
                    const [ts, o, h, l, close, v] = c;
                    const conv = convMap[ts];
                    if (!conv) return null;

                    // Se conversionPair è EUR/USDT → prezzo BTC/EUR = (BTC/USDT) / (EUR/USDT)
                    // Se conversionPair è USDT/EUR → prezzo BTC/EUR = (BTC/USDT) * (USDT/EUR)
                    let factor = 1;
                    if (pairInfo.conversionPair.startsWith("EUR/")) {
                        factor = 1 / conv;
                    } else if (pairInfo.conversionPair.startsWith("USD/EUR")
                            || pairInfo.conversionPair.startsWith("USDT/EUR")
                            || pairInfo.conversionPair.startsWith("USDC/EUR")) {
                        factor = conv;
                    }

                    return [ts, o * factor, h * factor, l * factor, close * factor, v];
                }).filter(Boolean);
            }

            return [exchange_id, finalData, null];
        } catch (err) {
            // Un errore vero (rate limit, manutenzione, rete, ...) è diverso da "l'exchange non ha
            // questa coppia" (gestito sopra con un `return` pulito, non un'eccezione): qui va
            // segnalato come fallito, non confuso con "nessun dato" — vedi Analisi_VPS_Prezzi_Sito.md,
            // "Esclusioni: exchange falliti vs nessun dato".
            return [exchange_id, [], (err && err.message) || 'errore sconosciuto'];
        }
    }

    // Fetch parallelo
    const fetched = await Promise.all(exchangeIds.map(ex_id => safe_fetch(ex_id)));

    for (const [ex_id, ohlcv_data, errore] of fetched) {
        if (errore) falliti.push(ex_id);
        if (ohlcv_data.length) {
            results[ex_id] = ohlcv_data;
        }
    }

    // Combino risultati
    const combined = {};
    for (const ex_id in results) {
        for (const entry of results[ex_id]) {
            const [ts, o] = entry;
            if (!combined[ts]) combined[ts] = {};
            combined[ts][ex_id] = o;
        }
    }

    const sorted_timestamps = Object.keys(combined).map(Number).sort((a, b) => a - b);
    const punti = sorted_timestamps.map(ts => ({
        timestamp: ts,
        prices: combined[ts],
    }));

    return { punti, falliti };
}

/**
 * Interroga in parallelo gli exchange indicati e ne fonde i prezzi in EUR di `baseSymbol` nella
 * finestra [since, until]. Corpo di `main()` estratto così com'era (stesso algoritmo, stesso
 * comportamento) per essere riusabile anche da `ServizioPrezzi/` (Fase 1, VPS) senza duplicare la
 * logica di scelta della coppia/conversione — vedi CLAUDE.md e
 * `test/Documentazione/Analisi_VPS_Prezzi_Sito.md`. `ccxtLib` è iniettato invece di essere
 * richiesto qui: `ccxt` viene caricato solo dentro `main()` (unico punto che ne aveva bisogno
 * prima del refactor), così un `require()` di questo file per le sole funzioni esportate non
 * tocca mai il modulo `ccxt` — evita che `ServizioPrezzi/`, che ha una propria installazione di
 * ccxt in `ServizioPrezzi/node_modules`, debba dipendere dalla risoluzione dei moduli di
 * `Scripts/` (cartelle diverse, non annidate).
 *
 * Restituisce solo l'array di punti (contratto invariato: lo consuma anche il CLI di questo file
 * e, in futuro, il client Java via `Prezzi.java`). Chi ha bisogno di sapere quali exchange sono
 * falliti (non "nessun dato", un errore vero) usa `cercaPrezziStoriciConEsito`.
 */
async function cercaPrezziStorici(args) {
    const { punti } = await eseguiRicerca(args);
    return punti;
}

/** Come `cercaPrezziStorici`, ma espone anche `falliti` (gli id degli exchange il cui fetch ha
 * lanciato un errore in questa chiamata, distinto da "nessun dato trovato"). Usato dal servizio
 * prezzi per non confondere un blip transitorio con un'assenza di prezzo confermata — vedi
 * `ServizioPrezzi/src/esclusioni.js` e `Analisi_VPS_Prezzi_Sito.md`. */
async function cercaPrezziStoriciConEsito(args) {
    return eseguiRicerca(args);
}

async function main() {
    try {
        const ccxt = require('ccxt');

        // Parametri CLI
        const args = process.argv.slice(2);
        const params = {};
        for (let i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                const key = args[i].substring(2);
                const value = args[i + 1];
                params[key] = value;
                i++;
            }
        }

        const exchanges = params.exchanges ? params.exchanges.split(",").map(e => e.trim()) : ["binance"];
        const baseSymbol = params.symbol || "BTC";
        const timeframe = params.timeframe || "1m";
        const since = params.since ? parseInt(params.since) : new ccxt.binance().parse8601("2024-01-01T00:00:00Z");
        const until = params.until ? parseInt(params.until) : new ccxt.binance().milliseconds();

        const output = await cercaPrezziStorici({
            ccxtLib: ccxt,
            exchangeIds: exchanges,
            baseSymbol,
            timeframe,
            since,
            until,
        });

        console.log(JSON.stringify(output, null, 2));
    } catch (err) {
        console.error(JSON.stringify({ error: err.message }));
        process.exit(1);
    }
}

module.exports = { findBestPair, fetchHistorical, getMarketsSymbols, cercaPrezziStorici, cercaPrezziStoriciConEsito };

if (require.main === module) {
    main();
}
