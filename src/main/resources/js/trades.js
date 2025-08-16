// Script per scaricare tutti i trade da Binance usando ccxt
// Con correzione automatica del time difference per Binance
// Gestisce automaticamente range >90gg suddividendoli in sotto-intervalli
// Filtra solo le coppie derivanti dall'array di token passato in input

const ccxt = require('ccxt');

// Configurazione
const BINANCE_CONFIG = {
    minDelayMs: 2000,
    maxRetries: 5,
    baseBackoffMs: 5000,
    maxBackoffMs: 60000,
    requestsPerMinute: 20,
    burstLimit: 5
};

// Utility functions
function logError(message) { console.error(`[ERROR] ${new Date().toISOString()}: ${message}`); }
function logInfo(message) { console.error(`[INFO] ${new Date().toISOString()}: ${message}`); }
function logDebug(message) { console.error(`[DEBUG] ${new Date().toISOString()}: ${message}`); }

function dateToTimestamp(dateString) {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) throw new Error(`Data non valida: ${dateString}`);
    return date.getTime();
}
function sleep(ms) { return new Promise(resolve => setTimeout(resolve, ms)); }

// Rate limiting
let requestCount = 0;
let lastRequestTime = 0;
async function smartRateLimit() {
    const now = Date.now();
    const timeSinceLastRequest = now - lastRequestTime;
    if (timeSinceLastRequest < BINANCE_CONFIG.minDelayMs) {
        const waitTime = BINANCE_CONFIG.minDelayMs - timeSinceLastRequest;
        await sleep(waitTime);
    }
    requestCount++;
    lastRequestTime = Date.now();
    if (requestCount % 10 === 0) logInfo(`Completate ${requestCount} richieste API`);
}
async function handleRateLimit(attempt) {
    const backoffTime = Math.min(
        BINANCE_CONFIG.baseBackoffMs * Math.pow(2, attempt - 1),
        BINANCE_CONFIG.maxBackoffMs
    );
    logInfo(`Rate limit raggiunto - attendo ${backoffTime / 1000} secondi (tentativo ${attempt})`);
    await sleep(backoffTime);
}

// Chiamata API con gestione errori e retry
async function safeApiCall(fn, attemptLabel) {
    let attempt = 1;
    while (attempt <= BINANCE_CONFIG.maxRetries) {
        try {
            await smartRateLimit();
            logDebug(`Chiamata API (tentativo ${attempt}): ${attemptLabel}`);
            return await fn();
        } catch (error) {
            logError(`Tentativo ${attempt} fallito: ${error.message}`);

            if (error.message.includes('429') || error.message.includes('rate limit')) {
                if (attempt < BINANCE_CONFIG.maxRetries) {
                    await handleRateLimit(attempt);
                    attempt++;
                    continue;
                }
                throw new Error(`Rate limit persistente dopo ${BINANCE_CONFIG.maxRetries} tentativi`);
            }

            if (error.message.includes('timeout') || error.message.includes('network')) {
                if (attempt < BINANCE_CONFIG.maxRetries) {
                    await sleep(3000);
                    attempt++;
                    continue;
                }
                throw new Error(`Errore di connessione persistente: ${error.message}`);
            }
            throw error;
        }
    }
}


// Recupera trade per una coppia e un singolo intervallo
async function fetchTradesForSymbol(exchange, symbol, startTime, endTime) {
    const intervalTrades = [];
    let currentStartTime = startTime;
    let iterazione = 1;
    const maxIterazioni = 500;

    logInfo(`Recupero trades per ${symbol} da ${new Date(startTime).toISOString()} a ${new Date(endTime).toISOString()}`);

    while (iterazione <= maxIterazioni) {
        try {
            const params = {
                startTime: currentStartTime,
                //endTime: endTime,
                limit: 500 // max consentito
            };
            const trades = await safeApiCall(
                () => exchange.fetchMyTrades(symbol, currentStartTime, params.limit, params),
                `fetchMyTrades(${symbol})`
            );
            //const trades = await exchange.fetchMyTrades(symbol, currentStartTime, 500, { recvWindow: 60000 });
            logDebug(`Ricevuti ${trades.length} trades per ${symbol} (iterazione ${iterazione})`);
            if (trades.length === 0) break;

            intervalTrades.push(...trades);
            logInfo(`[${symbol}] Trade cumulativi: ${intervalTrades.length}`);

            if (trades.length < params.limit) break;

            // Aggiorna il cursore
            const maxTimestamp = Math.max(...trades.map(t => t.timestamp));
            currentStartTime = maxTimestamp + 1;
            if (currentStartTime >= endTime) break;

            iterazione++;
        } catch (error) {
            logError(`Errore durante iterazione ${iterazione} per ${symbol}: ${error.message}`);
            throw error;
        }
    }
    return intervalTrades;
}


// ======================= Fetch Trades =======================

// Funzione principale per recuperare tutti i trade
async function fetchAllTrades(exchange, startTime, endTime, tokenArray) {
    const allTrades = [];
    const markets = await safeApiCall(() => exchange.loadMarkets(), "loadMarkets");
    const symbols = Object.keys(markets);

    // Genera tutte le coppie possibili dai token in input e verifica se esistono su Binance
    const validSymbols = [];
    for (let i = 0; i < tokenArray.length; i++) {
        for (let j = i + 1; j < tokenArray.length; j++) {
            const pair1 = `${tokenArray[i]}/${tokenArray[j]}`;
            const pair2 = `${tokenArray[j]}/${tokenArray[i]}`;
            if (symbols.includes(pair1)) validSymbols.push(pair1);
            if (symbols.includes(pair2)) validSymbols.push(pair2);
        }
    }

    if (validSymbols.length === 0) {
        logError("Nessuna coppia valida trovata su Binance per i token forniti");
        return allTrades;
    }

    for (const symbol of validSymbols) {

            logInfo(`Recupero trades per ${symbol}: ${new Date(startTime).toISOString()} - ${new Date(endTime).toISOString()}`);
            try {
                const trades = await fetchTradesForSymbol(exchange, symbol, startTime, endTime);
                allTrades.push(...trades);
                logInfo(`[${symbol}] Totale trades scaricati finora: ${allTrades.length}`);
            } catch (error) {
                throw error;
            }
    }
    logInfo(`Recupero completato. Totale trades: ${allTrades.length}`);
    return allTrades;
}

// Main
async function main() {
    try {
        const args = process.argv.slice(2);
        if (args.length !== 5) {
            logError('Uso: node script.js exchangeId apiKey secret startDate tokenArray');
            process.exit(1);
        }

        const [exchangeId, apiKey, secret, startDate, tokenArrayStr] = args;
        const endTime = Date.now();
        const startTime = dateToTimestamp(startDate);
        const tokenArray = tokenArrayStr.split(',').map(s => s.trim()).filter(Boolean);

        // Inizializza exchange
        const exchange = new ccxt[exchangeId]({
            apiKey,
            secret,
            options: { 
                defaultType: 'spot', 
                recvWindow: 10000,
                adjustForTimeDifference: true
            },
            enableRateLimit: true,
            rateLimit: BINANCE_CONFIG.minDelayMs
        });

        // CORREZIONE ORARIO PER BINANCE
        if (exchangeId === 'binance') {
            try {
                logInfo("Sto caricando il time difference per Binance...");
                await exchange.loadTimeDifference();
                logInfo(`Time difference calcolato: ${exchange.timeDifference}ms`);
                const serverTime = await exchange.fetchTime();
                const localTime = exchange.milliseconds();
                const timeDiff = serverTime - localTime;
                logInfo(`Server time: ${new Date(serverTime).toISOString()}`);
                logInfo(`Local time: ${new Date(localTime).toISOString()}`);
                logInfo(`Differenza oraria: ${timeDiff}ms`);
                if (Math.abs(timeDiff) > 10000) {
                    logError("Attenzione: differenza oraria significativa con il server Binance!");
                }
            } catch (e) {
                logError(`Errore durante la sincronizzazione dell'orario: ${e.message}`);
            }
        }

        // Recupera trades
        const allTrades = await fetchAllTrades(exchange, startTime, endTime, tokenArray);

        // Output finale
        console.log(JSON.stringify({ trades: allTrades }, null, 2));

    } catch (error) {
        logError(`Errore critico: ${error.message}`);
        process.exit(1);
    }
}
main();
