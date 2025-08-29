// Script per scaricare tutti i movimenti fiat da Binance (depositi/prelievi e pagamenti)
// Usa ccxt per autenticazione e gestione del time difference
// Gestisce automaticamente range >90gg suddividendoli in sotto-intervalli
// Recupera sia ORDERS (depositi/prelievi fiat) sia PAYMENTS (acquisti/vendite crypto in fiat)

const ccxt = require('ccxt');

// Tipi di transazioni da recuperare: ORDERS (0=depositi, 1=prelievi), PAYMENTS (0=buy,1=sell)
const FIAT_ORDER_TYPES = [0, 1];
const FIAT_PAYMENT_TYPES = [0, 1];

const BINANCE_CONFIG = {
    minDelayMs: 30000,
    maxRetries: 5,
    baseBackoffMs: 20000,
    maxBackoffMs: 60000,
    requestsPerMinute: 20,
    burstLimit: 5,
    maxQueryDays: 5000 // non serve impostarlo in questo endpoint
};

// Utility
function logError(msg) { console.error(`[ERROR] ${msg}`); }
function logInfo(msg) { console.error(`[INFO] ${msg}`); }
function logDebug(msg) { console.error(`[DEBUG] ${msg}`); }

function dateToTimestamp(dateStr) {
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) throw new Error(`Data non valida: ${dateStr}`);
    return date.getTime();
}

function sleep(ms) { return new Promise(resolve => setTimeout(resolve, ms)); }

// Rate limiting
let requestCount = 0;
let lastRequestTime = 0;

async function smartRateLimit() {
    const now = Date.now();
    const elapsed = now - lastRequestTime;
    if (elapsed < BINANCE_CONFIG.minDelayMs) {
        await sleep(BINANCE_CONFIG.minDelayMs - elapsed);
    }
    requestCount++;
    lastRequestTime = Date.now();
    if (requestCount % 10 === 0) logInfo(`Completate ${requestCount} richieste API`);
}

async function handleRateLimit(attempt) {
    const wait = Math.min(
        BINANCE_CONFIG.baseBackoffMs * Math.pow(2, attempt - 1),
        BINANCE_CONFIG.maxBackoffMs
    );
    logInfo(`Rate limit raggiunto - attendo ${wait / 1000}s (tentativo ${attempt})`);
    await sleep(wait);
}

// Chiamata API sicura con retry
async function safeApiCall(exchange, endpoint, params) {
    let attempt = 1;
    while (attempt <= BINANCE_CONFIG.maxRetries) {
        try {
            await smartRateLimit();
            logDebug(`Chiamata API (tentativo ${attempt}): ${endpoint}`);
            const response = await exchange.fetch2(endpoint, 'sapi', 'GET', params);
            return response;
        } catch (err) {
            logError(`Tentativo ${attempt} fallito: ${err.message}`);
            if (err.message.includes('429') || err.message.includes('rate limit')) {
                if (attempt < BINANCE_CONFIG.maxRetries) {
                    await handleRateLimit(attempt++);
                    continue;
                }
                throw new Error(`Rate limit persistente dopo ${BINANCE_CONFIG.maxRetries} tentativi`);
            }
            if (err.message.includes('timeout') || err.message.includes('network')) {
                if (attempt < BINANCE_CONFIG.maxRetries) {
                    await sleep(3000);
                    attempt++;
                    continue;
                }
                throw new Error(`Errore di connessione persistente: ${err.message}`);
            }
            throw err;
        }
    }
}

// Suddivide l'intervallo temporale in chunk da maxQueryDays
function splitTimeRange(startTime, endTime) {
    const chunks = [];
    const maxMs = BINANCE_CONFIG.maxQueryDays * 24 * 60 * 60 * 1000;
    let cursor = startTime;
    while (cursor < endTime) {
        const next = Math.min(cursor + maxMs, endTime);
        chunks.push({ startTime: cursor, endTime: next });
        cursor = next + 1; // evitare overlap
    }
    return chunks;
}

// Recupera fiat ORDERS (depositi/prelievi) per intervallo
async function fetchFiatOrdersForInterval(exchange, startTime, endTime, transactionType) {
    const results = [];
    let page = 1;
    const rows = 100;
    const maxPages = 1000;

    logInfo(`Recupero fiat ORDERS type=${transactionType} ${new Date(startTime).toISOString()} -> ${new Date(endTime).toISOString()}`);

    while (page <= maxPages) {
        const params = {
            transactionType,
            beginTime: startTime,
            endTime,
            page,
            rows
        };
        const resp = await safeApiCall(exchange, 'fiat/orders', params);
        const data = resp?.data || [];
        logDebug(`Ricevuti ${data.length} ORDERS records (pagina ${page})`);
        if (data.length === 0) break;
        //results.push(...data);
        results.push(...data.map(d => ({
            ...d,
            movimento: transactionType === 0 ? 'deposito' : 'prelievo'
        })));
        if (data.length < rows) break;
        page++;
    }
    return results;
}

// Recupera fiat PAYMENTS (buy/sell cripto) per intervallo
async function fetchFiatPaymentsForInterval(exchange, startTime, endTime, transactionType) {
    const results = [];
    let page = 1;
    const rows = 100;
    const maxPages = 1000;

    logInfo(`Recupero fiat PAYMENTS type=${transactionType} ${new Date(startTime).toISOString()} -> ${new Date(endTime).toISOString()}`);

    while (page <= maxPages) {
        const params = {
            transactionType,
            beginTime: startTime,
            endTime,
            page,
            rows
        };
        const resp = await safeApiCall(exchange, 'fiat/payments', params);
        const data = resp?.data || [];
        logDebug(`Ricevuti ${data.length} PAYMENTS records (pagina ${page})`);
        if (data.length === 0) break;
        //results.push(...data);
        results.push(...data.map(d => ({
            ...d,
            movimento: transactionType === 0 ? 'acquisto' : 'vendita'
        })));
        if (data.length < rows) break;
        page++;
    }
    return results;
}

// Funzione principale per tutti i movimenti fiat
async function fetchAllFiatMovements(exchange, startTime, endTime) {
    const allMovements = { orders: [], payments: [] };
    const chunks = splitTimeRange(startTime, endTime);

    logInfo(`Recupero storico fiat (${chunks.length} chunk da ${BINANCE_CONFIG.maxQueryDays}gg)`);

    // ORDERS
    for (const type of FIAT_ORDER_TYPES) {
        for (const [idx, c] of chunks.entries()) {
            logInfo(`ORDERS type=${type} chunk ${idx+1}/${chunks.length}`);
            const data = await fetchFiatOrdersForInterval(exchange, c.startTime, c.endTime, type);
            allMovements.orders.push(...data);
            logInfo(`Trovati ${data.length} ORDERS (totale finora: ${allMovements.orders.length})`);
        }
    }

    // PAYMENTS
    for (const type of FIAT_PAYMENT_TYPES) {
        for (const [idx, c] of chunks.entries()) {
            logInfo(`PAYMENTS type=${type} chunk ${idx+1}/${chunks.length}`);
            const data = await fetchFiatPaymentsForInterval(exchange, c.startTime, c.endTime, type);
            allMovements.payments.push(...data);
            logInfo(`Trovati ${data.length} PAYMENTS (totale finora: ${allMovements.payments.length})`);
        }
    }

    logInfo(`Recupero completato. Totale ORDERS: ${allMovements.orders.length}, PAYMENTS: ${allMovements.payments.length}`);
    return allMovements;
}

// Main
async function main() {
    try {
        const args = process.argv.slice(2);
        if (args.length !== 5) {
            logError('Uso: node script.js exchangeId apiKey secret startDate');
            process.exit(1);
        }
        const [exchangeId, apiKey, secret, startDate, assetArrayStr] = args;
        //const [exchangeId, apiKey, secret, startDate] = args;
        const endTime = Date.now();
        let startTime = Number(startDate);

        const exchange = new ccxt[exchangeId]({
            apiKey,
            secret,
            options: { defaultType: 'spot', recvWindow: 10000, adjustForTimeDifference: true },
            enableRateLimit: true,
            rateLimit: BINANCE_CONFIG.minDelayMs
        });

        // Correzione orario Binance
        if (exchangeId === 'binance') {
            try {
                logInfo("Carico time difference per Binance...");
                await exchange.loadTimeDifference();
                logInfo(`Time difference calcolato: ${exchange.timeDifference}ms`);
            } catch (e) {
                logError(`Errore sincronizzazione orario: ${e.message}`);
            }
        }

        // Recupera tutti i movimenti fiat
        const allFiat = await fetchAllFiatMovements(exchange, startTime, endTime);

        console.log(JSON.stringify({ Binance_fiat: allFiat }, null, 2));

    } catch (err) {
        logError(`Errore critico: ${err.message}`);
        process.exit(1);
    }
}

main();
