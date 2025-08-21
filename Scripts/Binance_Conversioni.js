// Script per scaricare tutte le conversioni di piccola entità da Binance usando ccxt
// Con correzione automatica del time difference per Binance
// Gestisce automaticamente range >90gg suddividendoli in sotto-intervalli

const ccxt = require('ccxt');


// Configurazione aggiuntiva
const TARGET_ASSETS = ['BNB', 'BTC', 'ETH', 'USDC'];
const PRICE_TOLERANCE = 0.2; // 20%
const PRICE_CACHE = {};
const TRADING_PAIRS_CACHE = new Set();


//Recupero i Simboli gestiti da Binance e li salva su File
const fs = require('fs');
const path = require('path');
const os = require('os');

async function getMarketsSymbols(exchange) {
    // Usa la cartella temporanea dell'utente, creandone una dedicata alla tua app
    const tempDir = path.join(os.tmpdir(), 'myapp'); 
    if (!fs.existsSync(tempDir)) {
        fs.mkdirSync(tempDir, { recursive: true }); // Crea se non esiste
    }
        
    const marketsFile = path.join(tempDir, `markets_${exchange.id}.json`);
    let markets;

    if (fs.existsSync(marketsFile)) {
        const stats = fs.statSync(marketsFile);
        const fileAgeMs = Date.now() - stats.mtimeMs;
        const twentyFourHoursMs = 1 * 60 * 60 * 1000;//Adesso aspetta 1 ora non 24

        if (fileAgeMs > twentyFourHoursMs) {
            // File troppo vecchio → lo elimino e ricarico
            fs.unlinkSync(marketsFile);
            logInfo(`File ${marketsFile} più vecchio di 1 ore, eliminato. Carico markets da API...`);
            markets = await safeApiCallMarkets(() => exchange.loadMarkets(), "loadMarkets");
            fs.writeFileSync(marketsFile, JSON.stringify(markets, null, 2), 'utf8');
        } else {
            // File recente → uso i dati locali
            logInfo(`Carico markets da file locale ${marketsFile} (meno di 1 ore)`);
            const rawData = fs.readFileSync(marketsFile, 'utf8');
            markets = JSON.parse(rawData);
        }
    } else {
        // Nessun file → carico da API e salvo
        logInfo(`Nessun file ${marketsFile} trovato, carico markets da API...`);
        markets = await safeApiCallMarkets(() => exchange.loadMarkets(), "loadMarkets");
        fs.writeFileSync(marketsFile, JSON.stringify(markets, null, 2), 'utf8');
    }

    return markets;
}


// Chiamata API con gestione errori e retry
async function safeApiCallMarkets(fn, attemptLabel) {
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




// Funzione per recuperare le coppie di trading disponibili
async function fetchTradingPairs(exchange) {
    if (TRADING_PAIRS_CACHE.size > 0) return;
    
    try {
        await smartRateLimit();
        const markets = await getMarketsSymbols(exchange);
        Object.keys(markets).forEach(pair => TRADING_PAIRS_CACHE.add(pair));
        logInfo(`Caricate ${TRADING_PAIRS_CACHE.size} coppie di trading`);
    } catch (error) {
        logError(`Errore nel recupero delle coppie: ${error.message}`);
        throw error;
    }
}

// Funzione per recuperare i prezzi storici
async function fetchHistoricalPrice(exchange, asset, timestamp) {
    const cacheKey = `${asset}_${timestamp}`;
    
    if (PRICE_CACHE[cacheKey]) {
        return PRICE_CACHE[cacheKey];
    }

    try {
        await smartRateLimit();
        const symbol = findTradingPair(asset);
        if (!symbol) {
            logDebug(`Nessuna coppia trovata per ${asset}`);
            return null;
        }

    /*    const response = await exchange.fetchOHLCV(
            symbol,
            '1m',
            timestamp - 60000, // 1 minuto prima
            timestamp + 60000, // 1 minuto dopo
            1
        );*/
const response = await exchange.fetchOHLCV(
    symbol,
    '1m',
    timestamp - 600000, // "since" = millisecondi di inizio candela (comincio a prendere i prezzi 10 minuti prima
    60          // richiedo 60 candele e prendo quella più vicina al timetamp di riferimento (60 candele sono 1 ora di dati)
);

//FUNZIONE CHE PRENDE LA CANDELA PIU' VICINA 
   if (response && response.length > 0) {
    // Trova la candela più vicina al timestamp
    const candle = response.reduce((prev, curr) => {
        return Math.abs(curr[0] - timestamp) < Math.abs(prev[0] - timestamp) ? curr : prev;
    });
    const price = candle[4]; // close price
    logInfo(`Recupero del prezzo a data ${timestampToRomeDate(timestamp)} per ${asset}: $${price}`);
    PRICE_CACHE[cacheKey] = price;
    return price;
}



     /*   //logInfo(`Recupero del prezzo per ${asset}: ${response}`);
        if (response.length > 0) {
            
            const price = response[0][4]; // Prezzo di chiusura
            logInfo(`Recupero del prezzo per ${symbol}: ${price}`);
            PRICE_CACHE[cacheKey] = price;
            return price;
        }*/
        return null;
    } catch (error) {
        logError(`Errore nel recupero del prezzo per ${asset}: ${error.message}`);
        return null;
    }
}


function timestampToRomeDate(ts) {
  return new Date(ts).toLocaleString('it-IT', { timeZone: 'Europe/Rome' });
}


// Trova la coppia di trading disponibile per un asset
function findTradingPair(asset) {
    const stablecoins = ['USDT', 'USDC'];
    
    for (const stablecoin of stablecoins) {
        const pair1 = `${asset}/${stablecoin}`;
        //const pair2 = `${stablecoin}/${asset}`;
        
        if (TRADING_PAIRS_CACHE.has(pair1)) return pair1;
        //if (TRADING_PAIRS_CACHE.has(pair2)) return pair2;
    }
    
    return null;
}




















// Configurazione Principale
const BINANCE_CONFIG = {
    minDelayMs: 2000,
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
             // Verifica la sincronizzazione dell'orario
       /*         const serverTime = await exchange.fetchTime();
             // Aggiungi timestamp corrente a ogni richiesta
             //Questa parte potrebbe non servire, dipende dall'endpoint
            const timestampParams = {
                ...params,
                timestamp: serverTime
            };*/
            
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

// Modifica alla funzione fetchConversionsForInterval
async function fetchConversionsForInterval(exchange, startTime, endTime, assetArray) {
    await fetchTradingPairs(exchange); // Carica le coppie una volta
    
    const intervalConversions = [];
    let currentStartTime = startTime;
    let iterazione = 1;
    
    while (iterazione <= 5000) {
        try {
            const params = {
                startTime: currentStartTime,
                endTime: endTime
                //timestamp: exchange.milliseconds()
            };
            
            const response = await safeApiCall(exchange, 'asset/dribblet', params);
            const dribblets = response?.userAssetDribblets || [];
            let records = [];
            
            for (const dribblet of dribblets) {
                if (!dribblet.userAssetDribbletDetails) continue;
                
                for (const detail of dribblet.userAssetDribbletDetails) {
                    const fromAsset = detail.fromAsset;
                    const fromAmount = parseFloat(detail.amount);
                    const toAmount = parseFloat(detail.transferedAmount);
                    
                    // Calcola il rapporto di conversione
                    const conversionRatio = toAmount / fromAmount;
                    
                    // Trova il token di destinazione
                    let toAsset = null;
                    let bestMatchDiff = Infinity;
                    
                    for (const targetAsset of TARGET_ASSETS) {
                        try {
                            const fromPrice = await fetchHistoricalPrice(exchange, fromAsset, parseInt(detail.operateTime));
                            const toPrice = await fetchHistoricalPrice(exchange, targetAsset, parseInt(detail.operateTime));
                            
                            if (fromPrice && toPrice) {
                                const expectedRatio = fromPrice / toPrice;
                                const diff = Math.abs(conversionRatio - expectedRatio) / expectedRatio;
                                
                                if (diff < PRICE_TOLERANCE && diff < bestMatchDiff) {
                                    bestMatchDiff = diff;
                                    toAsset = targetAsset;
                                }
                            }
                        } catch (error) {
                            logDebug(`Errore nel confronto con ${targetAsset}: ${error.message}`);
                        }
                    }
                    
                    records.push({
                        ...detail,
                        toAsset: toAsset || 'UNKNOWN',
                        conversionMatchConfidence: toAsset ? (1 - bestMatchDiff).toFixed(2) : 0,
                        totalServiceChargeAmount: dribblet.totalServiceChargeAmount,
                        totalTransferedAmount: dribblet.totalTransferedAmount,
                        transId: dribblet.transId
                    });
                }
            }
            
            if (records.length === 0) break;
            
            // Filtra per asset se specificato
            const filteredRecords = assetArray.length > 0
                ? records.filter(record => assetArray.includes(record.fromAsset))
                : records;
            
            intervalConversions.push(...filteredRecords);
            
            // Aggiorna il cursore
            const maxTimestamp = Math.max(...records.map(r => parseInt(r.operateTime)));
            currentStartTime = maxTimestamp + 1;
            
            if (currentStartTime >= endTime) break;
            iterazione++;
        } catch (error) {
            if (error.message === 'TIME_RANGE_TOO_LARGE') throw error;
            logError(`Errore durante iterazione ${iterazione}: ${error.message}`);
            throw error;
        }
    }
    
    return intervalConversions;
}


/// Recupera conversioni per un singolo intervallo di tempo
async function fetchConversionsForInterval2(exchange, startTime, endTime, assetArray) {
    const intervalConversions = [];
    let currentStartTime = startTime;
    let iterazione = 1;
    const maxIterazioni = 5000;
    
    logInfo(`Recupero conversioni da ${new Date(startTime).toISOString()} a ${new Date(endTime).toISOString()}`);
    
    while (iterazione <= maxIterazioni) {
        try {
            const params = {
                startTime: currentStartTime,
                endTime: endTime
            };
            
            const response = await safeApiCall(
                exchange,
                'asset/dribblet',
                params
            );
            logInfo(JSON.stringify(response, null, 2));
            // Estrai i dettagli dalle transazioni
            const dribblets = response?.userAssetDribblets || [];
            let records = [];
            
            // Processa ogni transazione e i suoi dettagli
            dribblets.forEach(dribblet => {
                if (dribblet.userAssetDribbletDetails) {
                    records.push(...dribblet.userAssetDribbletDetails.map(detail => ({
                        ...detail,
                        totalServiceChargeAmount: dribblet.totalServiceChargeAmount,
                        totalTransferedAmount: dribblet.totalTransferedAmount,
                        transId: dribblet.transId
                    })));
                }
            });
            
            logDebug(`Ricevuti ${records.length} records di conversione`);
            
            if (records.length === 0) break;
            
            // Filtra per asset se specificato
            const filteredRecords = assetArray.length > 0
                ? records.filter(record => assetArray.includes(record.fromAsset))
                : records;
            
            intervalConversions.push(...filteredRecords);
            
            // Aggiorna il cursore usando l'operateTime più recente
            const maxTimestamp = Math.max(...records.map(r => parseInt(r.operateTime)));
            currentStartTime = maxTimestamp + 1;
            
            if (currentStartTime >= endTime) break;
            
            iterazione++;
        } catch (error) {
            if (error.message === 'TIME_RANGE_TOO_LARGE') {
                throw error;
            }
            logError(`Errore durante iterazione ${iterazione}: ${error.message}`);
            throw error;
        }
    }
    
    return intervalConversions;
}

// Funzione principale per recuperare tutte le conversioni
async function fetchAllConversions(exchange, startTime, endTime, assetArray) {
    const allConversions = [];
    const timeChunks = splitTimeRange(startTime, endTime);
    
    logInfo(`Recupero storico conversioni (${timeChunks.length} chunk da ${BINANCE_CONFIG.maxQueryDays}gg)`);
    
    for (const [index, chunk] of timeChunks.entries()) {
        logInfo(`Processo chunk ${index + 1}/${timeChunks.length}: ${new Date(chunk.startTime).toISOString()} - ${new Date(chunk.endTime).toISOString()}`);
        
        try {
            const conversions = await fetchConversionsForInterval(
                exchange,
                chunk.startTime,
                chunk.endTime,
                assetArray
            );
            
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
            logError('Uso: node script.js exchangeId apiKey secret startDate assetArray');
            process.exit(1);
        }
        
        const [exchangeId, apiKey, secret, startDate, assetArrayStr] = args;
        const endTime = Date.now();
        const startTime = dateToTimestamp(startDate);
        const assetArray = assetArrayStr.split(',').map(s => s.trim()).filter(Boolean);
        
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
        
        // Recupera conversioni
        const allConversions = await fetchAllConversions(exchange, startTime, endTime, assetArray);
        
        // Output
        console.log(JSON.stringify({ Binance_smallAssetConversions: allConversions }, null, 2));
        
    } catch (error) {
        logError(`Errore critico: ${error.message}`);
        process.exit(1);
    }
}

main();