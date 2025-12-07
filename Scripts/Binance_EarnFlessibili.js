// Script per scaricare tutti i rewards da Binance Flexible Earn usando ccxt
// Con correzione automatica del time difference per Binance
// Gestisce automaticamente range >90gg suddividendoli in sotto-intervalli
// Recupera tutti i tipi di rewards (BONUS, REALTIME, REWARDS)

const ccxt = require('ccxt');

// Tipi di rewards da recuperare
//const REWARD_TYPES = ['BONUS', 'REALTIME', 'REWARDS'];
const REWARD_TYPES = ['ALL'];

// Configurazione
const BINANCE_CONFIG = {
    minDelayMs: 15000,
    maxRetries: 5,
    baseBackoffMs: 5000,
    maxBackoffMs: 60000,
    requestsPerMinute: 20,
    burstLimit: 5,
    maxQueryDays: 89 // Massimo intervallo di giorni consentito da Binance
};

// Utility functions
function logError(message) { console.error(`[ERROR] ${message}`); }
function logInfo(message) { console.error(`[INFO] ${message}`); }
function logDebug(message) { console.error(`[DEBUG] ${message}`); }

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

// Recupera rewards per un singolo intervallo di tempo
async function fetchRewardsForInterval(exchange, startTime, endTime, rewardType) {
    const intervalRewards = [];
    let currentStartTime = startTime;
    let iterazione = 1;
    const maxIterazioni = 1000;
    const Size =100;
    
    logInfo(`Recupero ${rewardType} da ${new Date(startTime).toISOString()} a ${new Date(endTime).toISOString()}`);
    
    while (iterazione <= maxIterazioni) {
        try {
            const params = {
                type: rewardType,
                startTime: currentStartTime,
                endTime: endTime,
                current: iterazione,
                size: Size
            };
            logInfo(`Recupero Pag.${iterazione}`);
            const response = await safeApiCall(
                exchange,
                'simple-earn/flexible/history/rewardsRecord',
                params
            );
            
            const records = response?.rows || [];
            logDebug(`Ricevuti ${records.length} ${rewardType} records`);
            
            if (records.length === 0) break;
            
            // Filtra per asset se specificato
          /*  const filteredRecords = assetArray.length > 0
                ? records.filter(record => assetArray.includes(record.asset))
                : records;*/
            
            intervalRewards.push(...records);
            
            // Se abbiamo meno record del massimo, abbiamo finito
            if (records.length < Size) break;
            logInfo(`Trovata nuova pagina di dati per il periodo indicato`);
            
            // Aggiorna il cursore
           /* const maxTimestamp = Math.max(...records.map(r => parseInt(r.time)));
            currentStartTime = maxTimestamp + 1;
            
            if (currentStartTime >= endTime) break;*/
            
            iterazione++;
        } catch (error) {
            if (error.message === 'TIME_RANGE_TOO_LARGE') {
                throw error;
            }
            logError(`Errore durante iterazione ${iterazione}: ${error.message}`);
            throw error;
        }
    }
    
    return intervalRewards;
}

// Funzione principale per recuperare tutti i rewards
async function fetchAllRewards(exchange, startTime, endTime) {
    const allRewards = [];
    const timeChunks = splitTimeRange(startTime, endTime);
    
    logInfo(`Recupero storico rewards (${timeChunks.length} chunk da ${BINANCE_CONFIG.maxQueryDays}gg)`);
    
    for (const rewardType of REWARD_TYPES) {
        logInfo(`Processo tipo: ${rewardType}`);
        
        for (const [index, chunk] of timeChunks.entries()) {
            logInfo(`Processo chunk ${index + 1}/${timeChunks.length}: ${new Date(chunk.startTime).toISOString()} - ${new Date(chunk.endTime).toISOString()}`);
            
            try {
                const rewards = await fetchRewardsForInterval(
                    exchange,
                    chunk.startTime,
                    chunk.endTime,
                    rewardType
                );
                
                allRewards.push(...rewards);
                logInfo(`Trovati ${rewards.length} records (totale: ${allRewards.length})`);
            } catch (error) {
                if (error.message === 'TIME_RANGE_TOO_LARGE') {
                    logError(`Errore imprevisto: intervallo già suddiviso ma ancora troppo grande`);
                }
                throw error;
            }
        }
    }
    
    logInfo(`Recupero completato. Totale rewards: ${allRewards.length}`);
    return allRewards;
}

// Main
async function main() {
    try {
        const args = process.argv.slice(2);
        if (args.length !== 5) {
            logError('Uso: node script.js exchangeId apiKey secret startDate assetArray');
            process.exit(1);
        }
        
        const [exchangeId, apiKey, secret, startDate, assetArrayStr] = args;
        const endTime = Date.now();
        //const startTime = dateToTimestamp(startDate);
        let startTime = Number(startDate);
       // const assetArray = assetArrayStr.split(',').map(s => s.trim()).filter(Boolean);
        
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
                
                // Verifica la sincronizzazione dell'orario
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
        
        // Recupera rewards
        const allRewards = await fetchAllRewards(exchange, startTime, endTime);
        
        // Output
        // Per lo script FLEXIBLE:
        console.log(JSON.stringify({ Binance_earnFlexible: allRewards }, null, 2));
        
    } catch (error) {
        logError(`Errore critico: ${error.message}`);
        process.exit(1);
    }
}

main();