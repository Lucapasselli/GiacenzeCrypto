const ccxt = require('ccxt');

// Configurazione
const BINANCE_CONFIG = {
    minDelayMs: 2500,
    maxRetries: 5,
    maxQueryDays: 90,
    stakingProducts: ['STAKING', 'F_DEFI', 'L_DEFI']
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

async function safeApiCall(exchange, endpoint, params) {
    let attempt = 1;
    
    while (attempt <= BINANCE_CONFIG.maxRetries) {
        try {
            await smartRateLimit();
            logDebug(`Chiamata API (tentativo ${attempt}): ${endpoint}`, params);
            const response = await exchange.fetch2(endpoint, 'sapi', 'GET', params);
            logDebug(`Risposta API: ${JSON.stringify(response, null, 2)}`);
            return response;
        } catch (error) {
            logError(`Tentativo ${attempt} fallito: ${error.message}\nDettagli: ${JSON.stringify(error, null, 2)}`);
            
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

function splitTimeRange(startTime, endTime) {
    const chunks = [];
    const maxChunkMs = BINANCE_CONFIG.maxQueryDays * 24 * 60 * 60 * 1000;
    let currentStart = startTime;
    
    while (currentStart < endTime) {
        const currentEnd = Math.min(currentStart + maxChunkMs, endTime);
        chunks.push({ 
            startTime: currentStart, 
            endTime: currentEnd,
            range: `${new Date(currentStart).toISOString()} - ${new Date(currentEnd).toISOString()}`
        });
        currentStart = currentEnd + 1;
    }
    
    return chunks;
}

async function fetchStakingProductRewards(exchange, startTime, endTime, assetArray, product) {
    const rewards = [];
    const timeChunks = splitTimeRange(startTime, endTime);
    
    for (const chunk of timeChunks) {
        logInfo(`Processo chunk ${product}: ${chunk.range}`);
        
        const params = {
            product,
            txnType: 'INTEREST',
           // product : 'ALL',
            //txnType: 'REDEMPTION',
            //txnType: 'SUBSCRIPTION',
            startTime: chunk.startTime,
            endTime: chunk.endTime,
            size: 100
        };
        
        try {
            //mining/merchantIncome
            //const response = await safeApiCall(exchange, 'mining/merchantIncome', params);
            const response = await safeApiCall(exchange, 'staking/stakingRecord', params);
            //const response = await safeApiCall(exchange, 'staking/positionHistory', params);
            const filtered = assetArray.length > 0 
                ? response.filter(tx => assetArray.includes(tx.asset))
                : response;
            
            rewards.push(...filtered);
            logInfo(`Trovati ${filtered.length} records per ${product} in questo chunk`);
        } catch (error) {
            if (error.message === 'TIME_RANGE_TOO_LARGE') {
                logError(`Intervallo ancora troppo grande dopo suddivisione!`);
            }
            throw error;
        }
    }
    
    return rewards;
}

async function fetchAllStakingRewards(exchange, startTime, endTime, assetArray) {
    const allRewards = {};
    
    for (const product of BINANCE_CONFIG.stakingProducts) {
        try {
            logInfo(`Inizio recupero ${product}...`);
            allRewards[product.toLowerCase()] = await fetchStakingProductRewards(
                exchange, 
                startTime, 
                endTime, 
                assetArray, 
                product
            );
            logInfo(`Completato ${product}. Trovati: ${allRewards[product.toLowerCase()].length} records`);
        } catch (error) {
            logError(`Fallito recupero ${product}: ${error.message}`);
            allRewards[product.toLowerCase()] = [];
        }
    }
    
    return allRewards;
}

async function main() {
    try {
        const args = process.argv.slice(2);
        if (args.length !== 5) {
            logError('Uso: node staking.js exchangeId apiKey secret startDate assetArray');
            process.exit(1);
        }
        
        const [exchangeId, apiKey, secret, startDate, assetArrayStr] = args;
        const assetArray = assetArrayStr.split(',').map(s => s.trim()).filter(Boolean);
        const startTime = Number(startDate);
        const endTime = Date.now();
        
        logInfo(`Inizializzazione exchange ${exchangeId}...`);
        const exchange = new ccxt[exchangeId]({
            apiKey,
            secret,
            options: { 
                adjustForTimeDifference: true,
                recvWindow: 60000
            },
            enableRateLimit: true 
        });

        if (exchangeId === 'binance') {
            logInfo("Sincronizzazione orario Binance...");
            await exchange.loadTimeDifference();
            logInfo(`Differenza oraria: ${exchange.timeDifference}ms`);
        }
        
        logInfo(`Recupero rewards da ${new Date(startTime).toISOString()} a ${new Date(endTime).toISOString()}`);
        const stakingRewards = await fetchAllStakingRewards(exchange, startTime, endTime, assetArray);
        
        console.log(JSON.stringify(stakingRewards));
        
    } catch (error) {
        logError(`ERRORE GLOBALE: ${error.message}\nStack: ${error.stack}`);
        process.exit(1);
    }
}

main();