/**
 * Download storico SOL Staking Binance (endpoint ufficiali)
 *
 * Endpoint:
 *  - stakingHistory
 *  - redemptionHistory
 *  - bnsolRewardsHistory
 *
 * Vincoli Binance:
 *  - max 3 mesi per query
 *  - default 30 giorni se start/end mancanti
 */

const ccxt = require('ccxt');

/* ===================== CONFIG ===================== */

const BINANCE_CONFIG = {
    minDelayMs: 5000,
    maxRetries: 5,
    baseBackoffMs: 5000,
    maxBackoffMs: 60000,
    maxQueryDays: 90,      // 3 mesi
    defaultDays: 30,
    pageSize: 100,
    maxPages: 1000
};

/* ===================== LOG ===================== */

function logError(msg) { console.error(`[ERROR] ${msg}`); }
function logInfo(msg)  { console.error(`[INFO] ${msg}`); }
function logDebug(msg) { console.error(`[DEBUG] ${msg}`); }

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

/* ===================== RATE LIMIT ===================== */

let lastRequestTime = 0;

async function smartRateLimit() {
    const now = Date.now();
    const diff = now - lastRequestTime;

    if (diff < BINANCE_CONFIG.minDelayMs) {
        await sleep(BINANCE_CONFIG.minDelayMs - diff);
    }

    lastRequestTime = Date.now();
}

async function handleRateLimit(attempt) {
    const backoff = Math.min(
        BINANCE_CONFIG.baseBackoffMs * Math.pow(2, attempt - 1),
        BINANCE_CONFIG.maxBackoffMs
    );
    logInfo(`Rate limit – attendo ${backoff / 1000}s (tentativo ${attempt})`);
    await sleep(backoff);
}

/* ===================== API CALL ===================== */

async function safeApiCall(exchange, endpoint, params) {
    let attempt = 1;

    while (attempt <= BINANCE_CONFIG.maxRetries) {
        try {
            await smartRateLimit();
            logDebug(`API ${endpoint} (try ${attempt})`);
            return await exchange.fetch2(endpoint, 'sapi', 'GET', params);
        } catch (err) {
            logError(err.message);

            if (
                err.message.includes('429') ||
                err.message.toLowerCase().includes('rate limit')
            ) {
                if (attempt < BINANCE_CONFIG.maxRetries) {
                    await handleRateLimit(attempt++);
                    continue;
                }
            }

            if (
                err.message.toLowerCase().includes('timeout') ||
                err.message.toLowerCase().includes('network')
            ) {
                if (attempt < BINANCE_CONFIG.maxRetries) {
                    await sleep(3000);
                    attempt++;
                    continue;
                }
            }

            throw err;
        }
    }
}

/* ===================== TIME RANGE ===================== */

/**
 * Applica le regole Binance:
 * - nessuno dei due → ultimi 30gg
 * - solo start → start + 30gg
 * - solo end → end - 30gg
 * - max 90gg per chunk
 */
function normalizeTimeRange(startTime, endTime) {
    const now = Date.now();
    const dayMs = 24 * 60 * 60 * 1000;

    if (!startTime && !endTime) {
        endTime = now;
        startTime = now - BINANCE_CONFIG.defaultDays * dayMs;
    } else if (startTime && !endTime) {
        endTime = startTime + BINANCE_CONFIG.defaultDays * dayMs;
    } else if (!startTime && endTime) {
        startTime = endTime - BINANCE_CONFIG.defaultDays * dayMs;
    }

    return { startTime, endTime };
}

function splitTimeRange(startTime, endTime) {
    const chunks = [];
    const maxMs = BINANCE_CONFIG.maxQueryDays * 24 * 60 * 60 * 1000;
    let currentStart = startTime;

    while (currentStart < endTime) {
        const currentEnd = Math.min(currentStart + maxMs, endTime);
        chunks.push({ startTime: currentStart, endTime: currentEnd });
        currentStart = currentEnd + 1;
    }

    return chunks;
}

/* ===================== FETCHERS ===================== */

async function fetchPaged(exchange, endpoint, startTime, endTime) {
    const all = [];
    let current = 1;

    while (current <= BINANCE_CONFIG.maxPages) {
        const params = {
            startTime,
            endTime,
            current,
            size: BINANCE_CONFIG.pageSize
        };

        const response = await safeApiCall(exchange, endpoint, params);
        const list = response?.list || [];

        if (list.length === 0) break;

        all.push(...list);
        if (list.length < BINANCE_CONFIG.pageSize) break;

        current++;
    }

    return all;
}

async function fetchSolStakingChunk(exchange, chunk) {
    return {
        staking: await fetchPaged(
            exchange,
            'sol-staking/sol/history/stakingHistory',
            chunk.startTime,
            chunk.endTime
        ),
        redemption: await fetchPaged(
            exchange,
            'sol-staking/sol/history/redemptionHistory',
            chunk.startTime,
            chunk.endTime
        ),
        rewards: await fetchPaged(
            exchange,
            'sol-staking/sol/history/bnsolRewardsHistory',
            chunk.startTime,
            chunk.endTime
        )
    };
}

/* ===================== MAIN AGGREGATOR ===================== */

async function fetchAllSolStaking(exchange, startTime, endTime) {
    const normalized = normalizeTimeRange(startTime, endTime);
    const chunks = splitTimeRange(normalized.startTime, normalized.endTime);

    const result = {
        staking: [],
        redemption: [],
        rewards: []
    };

    logInfo(`Chunk totali: ${chunks.length}`);

    for (const [i, chunk] of chunks.entries()) {
        logInfo(
            `Chunk ${i + 1}/${chunks.length}: ` +
            `${new Date(chunk.startTime).toISOString()} → ${new Date(chunk.endTime).toISOString()}`
        );

        const data = await fetchSolStakingChunk(exchange, chunk);
        result.staking.push(...data.staking);
        result.redemption.push(...data.redemption);
        result.rewards.push(...data.rewards);
    }

    return result;
}

/* ===================== ENTRY POINT ===================== */

async function main() {
    try {
        const args = process.argv.slice(2);
        if (args.length < 3) {
            logError('Uso: node sol_staking.js binance apiKey secret [startTime] [endTime]');
            process.exit(1);
        }

        const [exchangeId, apiKey, secret, startTimeArg, endTimeArg] = args;

        const startTime = startTimeArg ? Number(startTimeArg) : null;
        const endTime = endTimeArg ? Number(endTimeArg) : null;

        const exchange = new ccxt[exchangeId]({
            apiKey,
            secret,
            enableRateLimit: true,
            options: {
                defaultType: 'spot',
                adjustForTimeDifference: true,
                recvWindow: 10000
            }
        });

        if (exchangeId === 'binance') {
            logInfo('Sincronizzazione orario Binance...');
            await exchange.loadTimeDifference();
        }

        const data = await fetchAllSolStaking(exchange, startTime, endTime);

        console.log(JSON.stringify({ SOL_Staking: data }, null, 2));

    } catch (err) {
        logError(`Errore critico: ${err.message}`);
        process.exit(1);
    }
}

main();

