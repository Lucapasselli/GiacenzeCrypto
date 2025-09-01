// Script per scaricare tutte le conversioni di token da Binance usando ccxt
// Con correzione automatica del time difference per Binance
// Gestisce automaticamente range >90gg suddividendoli in sotto-intervalli
// Recupera tutte le conversioni fatte tramite Binance Convert

const ccxt = require('ccxt');

// Configurazione
const BINANCE_CONFIG = {
    minDelayMs: 5000,
    maxRetries: 5,
    baseBackoffMs: 5000,
    maxBackoffMs: 60000,
    requestsPerMinute: 20,
    burstLimit: 5,
    maxQueryDays: 30 // Massimo intervallo di giorni consentito da Binance
};

// Utility functions
function logError(message) { console.error(`[ERROR] ${message}`); }
function logInfo(message) { console.error(`[INFO] ${message}`); }
function logDebug(message) { console.error(`[DEBUG] ${message}`); }

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
async function safeApiCall(exchange, endpoint, params) {
    let attempt = 1;

    while (attempt <= BINANCE_CONFIG.maxRetries) {
        try {
            await smartRateLimit();
            logDebug(`Chiamata API (tentativo ${attempt}): ${endpoint}`);
            const response = await exchange.fetch2(endpoint, 'sapi', 'GET', params);
            return response;
        } catch (error) {
            logError(`Tentativo ${attempt} fallito: ${error.message}`);

            // Gestione specifica per intervallo troppo grande
            if (error.message.includes('-6021') || error.message.includes('Query time range too large')) {
                throw new Error('TIME_RANGE_TOO_LARGE');
            }

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

// Suddivide un intervallo in chunk di 90 giorni
function splitTimeRange(startTime, endTime) {
    const chunks = [];
    const maxChunkMs = BINANCE_CONFIG.maxQueryDays * 24 * 60 * 60 * 1000;
    let currentStart = startTime;

    while (currentStart < endTime) {
        const currentEnd = Math.min(currentStart + maxChunkMs, endTime);
        chunks.push({ startTime: currentStart, endTime: currentEnd });
        currentStart = currentEnd + 1; // +1ms per evitare overlap
    }

    return chunks;
}

// Recupera conversioni per un singolo intervallo di tempo
async function fetchConversionsForInterval(exchange, startTime, endTime) {
    const intervalConversions = [];
    let current = 1;
    const maxIterazioni = 1000;
    const size = 100;

    logInfo(`Recupero conversioni da ${new Date(startTime).toISOString()} a ${new Date(endTime).toISOString()}`);

    while (current <= maxIterazioni) {
        try {
            const params = {
                startTime: startTime,
                endTime: endTime,
                size: size,
                current: current
            };
            logInfo(`Recupero Pag.${current}`);
            const response = await safeApiCall(exchange, 'convert/tradeFlow', params);

            const records = response?.list || [];
            logDebug(`Ricevuti ${records.length} records di conversione`);

            if (records.length === 0) break;

            intervalConversions.push(...records);

            if (records.length < size) break;
            logInfo(`Trovata nuova pagina di dati per il periodo indicato`);

            current++;
        } catch (error) {
            if (error.message === 'TIME_RANGE_TOO_LARGE') {
                throw error;
            }
            logError(`Errore durante iterazione ${current}: ${error.message}`);
            throw error;
        }
    }

    return intervalConversions;
}

// Funzione principale per recuperare tutte le conversioni
async function fetchAllConversions(exchange, startTime, endTime) {
    const allConversions = [];
    const timeChunks = splitTimeRange(startTime, endTime);

    logInfo(`Recupero storico conversioni (${timeChunks.length} chunk da ${BINANCE_CONFIG.maxQueryDays}gg)`);

    for (const [index, chunk] of timeChunks.entries()) {
        logInfo(`Processo chunk ${index + 1}/${timeChunks.length}: ${new Date(chunk.startTime).toISOString()} - ${new Date(chunk.endTime).toISOString()}`);

        try {
            const conversions = await fetchConversionsForInterval(exchange, chunk.startTime, chunk.endTime);

            allConversions.push(...conversions);
            logInfo(`Trovati ${conversions.length} records (totale: ${allConversions.length})`);
        } catch (error) {
            if (error.message === 'TIME_RANGE_TOO_LARGE') {
                logError(`Errore imprevisto: intervallo già suddiviso ma ancora troppo grande`);
            }
            throw error;
        }
    }

    logInfo(`Recupero completato. Totale conversioni: ${allConversions.length}`);
    return allConversions;
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
        const endTime = Date.now();
        //const endTime = Number(1690753487000);
        let startTime = Number(startDate);

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
            } catch (e) {
                logError(`Errore durante la sincronizzazione dell'orario: ${e.message}`);
            }
        }

        // Recupera conversioni
        const allConversions = await fetchAllConversions(exchange, startTime, endTime);

        // Output
        console.log(JSON.stringify({ Binance_Convert: allConversions }, null, 2));

    } catch (error) {
        logError(`Errore critico: ${error.message}`);
        process.exit(1);
    }
}

main();
