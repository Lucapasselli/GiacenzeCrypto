//#!/usr/bin/env node
const ccxt = require('ccxt');

// Trova il miglior simbolo disponibile per XXX/EUR
async function findBestPair(ex, baseSymbol) {
    const markets = await ex.loadMarkets();

    // Se esiste direttamente la coppia con EUR
    if (`${baseSymbol}/EUR` in markets) {
        return { pair: `${baseSymbol}/EUR`, invert: false, needsConversion: false };
    }

    // Se esiste con USDT
    if (`${baseSymbol}/USDT` in markets) {
        if ("EUR/USDT" in markets) {
            return { pair: `${baseSymbol}/USDT`, invert: false, needsConversion: true, conversionPair: "EUR/USDT" };
        }
        if ("USDT/EUR" in markets) {
            return { pair: `${baseSymbol}/USDT`, invert: false, needsConversion: true, conversionPair: "USDT/EUR" };
        }
    }

    // Se esiste con USDC
    if (`${baseSymbol}/USDC` in markets) {
        if ("EUR/USDC" in markets) {
            return { pair: `${baseSymbol}/USDC`, invert: false, needsConversion: true, conversionPair: "EUR/USDC" };
        }
        if ("USDC/EUR" in markets) {
            return { pair: `${baseSymbol}/USDC`, invert: false, needsConversion: true, conversionPair: "USDC/EUR" };
        }
    }

    return null;
}

async function fetchHistorical(ex, symbol, timeframe, since, until, limit = 1000) {
    const all_ohlcv = [];
    let current_since = since;

    while (current_since < until) {
        const ohlcv = await ex.fetchOHLCV(symbol, timeframe, current_since, limit);
        if (!ohlcv.length) break;
        all_ohlcv.push(...ohlcv);
        const last_ts = ohlcv[ohlcv.length - 1][0];
        if (last_ts === current_since) break;
        current_since = last_ts + ex.parseTimeframe(timeframe) * 1000;
    }

    return all_ohlcv.filter(c => c[0] <= until);
}

async function main() {
    try {
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

        const results = {};

        async function safe_fetch(exchange_id) {
            try {
                if (!ccxt[exchange_id.toLowerCase()]) {
                    return [exchange_id, []];
                }
                const ex = new ccxt[exchange_id.toLowerCase()]();

                const pairInfo = await findBestPair(ex, baseSymbol);
                if (!pairInfo) return [exchange_id, []];

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
                        } else if (pairInfo.conversionPair.startsWith("USDT/EUR") || pairInfo.conversionPair.startsWith("USDC/EUR")) {
                            factor = conv;
                        }

                        return [ts, o * factor, h * factor, l * factor, close * factor, v];
                    }).filter(Boolean);
                }

                return [exchange_id, finalData];
            } catch {
                return [exchange_id, []];
            }
        }

        // Fetch parallelo
        const promises = exchanges.map(ex_id => safe_fetch(ex_id));
        const fetched = await Promise.all(promises);

        for (const [ex_id, ohlcv_data] of fetched) {
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
        const output = sorted_timestamps.map(ts => ({
            timestamp: ts,
            prices: combined[ts],
        }));

        console.log(JSON.stringify(output, null, 2));
    } catch (err) {
        console.error(JSON.stringify({ error: err.message }));
        process.exit(1);
    }
}

main();
