// Scarica i trade spot di Binance (api/v3 myTrades) per le coppie formate dai token passati in input.
// Binance non permette di scaricare i trade senza indicare la coppia: Java passa i token movimentati e
// quelli forzati a mano, e qui si interrogano tutte le coppie spot esistenti fra di loro.
// Output su stdout: { "trades": [ ...trade nel formato unificato di ccxt... ] }
// Ritmo, ripetizioni e uscita con errore: vedi Binance_Comune.js.
const { creaExchange, sincronizzaOrario, ripeti, esciConErrore, logInfo } = require('./Binance_Comune.js');

const LIMITE = 1000;          // trade massimi per richiesta
const PAGINE_MASSIME = 1000;

/**
 * Tutti i trade di una coppia da startTime in poi. La prima pagina parte dall'istante, le successive
 * dall'id del trade (fromId): ripartire dal millisecondo dopo l'ultimo trade, come si faceva prima,
 * saltava le esecuzioni dello stesso ordine con lo stesso istante rimaste oltre il limite della pagina.
 */
async function tradeCoppia(exchange, symbol, startTime) {
    const risultato = [];
    let fromId;
    for (let pagina = 1; pagina <= PAGINE_MASSIME; pagina++) {
        const trades = await ripeti(
            () => fromId === undefined
                ? exchange.fetchMyTrades(symbol, startTime, LIMITE)
                : exchange.fetchMyTrades(symbol, undefined, LIMITE, { fromId }),
            `fetchMyTrades(${symbol})`
        );
        risultato.push(...trades);
        if (trades.length < LIMITE) break;
        fromId = Math.max(...trades.map(t => Number(t.id))) + 1;
    }
    return risultato;
}

async function main() {
    const [, , exchangeId, apiKey, secret, startDate, tokenArrayStr = ''] = process.argv;
    const startTime = Number(startDate);
    const tokenArray = [...new Set(tokenArrayStr.split(',').map(s => s.trim()).filter(Boolean))];
    const exchange = creaExchange(exchangeId, apiKey, secret);
    await sincronizzaOrario(exchange);

    // Solo mercati spot (vedi creaExchange): una richiesta invece di quattro.
    const markets = await ripeti(() => exchange.loadMarkets(), 'loadMarkets');
    const validSymbols = [];
    for (let i = 0; i < tokenArray.length; i++) {
        for (let j = i + 1; j < tokenArray.length; j++) {
            for (const s of [`${tokenArray[i]}/${tokenArray[j]}`, `${tokenArray[j]}/${tokenArray[i]}`]) {
                if (markets[s] && markets[s].spot) validSymbols.push(s);
            }
        }
    }
    logInfo(`Ci sono ${validSymbols.length} possibili coppie da interrogare`);

    const allTrades = [];
    for (const [indice, symbol] of validSymbols.entries()) {
        const trades = await tradeCoppia(exchange, symbol, startTime);
        allTrades.push(...trades);
        logInfo(`[${indice + 1}/${validSymbols.length}] ${symbol}: ${trades.length} trade (totale ${allTrades.length})`);
    }
    logInfo(`Recupero completato. Totale trades: ${allTrades.length}`);
    console.log(JSON.stringify({ trades: allTrades }, null, 2));
}

main().catch(esciConErrore);
