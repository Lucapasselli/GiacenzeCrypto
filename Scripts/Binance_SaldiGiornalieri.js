/**
 * Binance Account Snapshot (SPOT) – Snapshot a una data specifica
 *
 * USO : node snapshot.js binance API_KEY SECRET 2024-03-15
 *
 * Limiti Binance:
 * - 1 snapshot al giorno
 * - max 30 snapshot per chiamata
 */

const ccxt = require('ccxt');

/* ================= CONFIG ================= */

const CONFIG = {
    minDelayMs: 5000,
    maxRetries: 5,
    baseBackoffMs: 5000,
    maxBackoffMs: 60000,
    snapshotWindowDays: 15   // +/- giorni attorno alla data target
};

/* ================= LOG ================= */

function logInfo(msg)  { console.error(`[INFO] ${msg}`); }
function logError(msg) { console.error(`[ERROR] ${msg}`); }
function logDebug(msg) { console.error(`[DEBUG] ${msg}`); }

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

/* ================= RATE LIMIT ================= */

let lastRequestTime = 0;

async function smartRateLimit() {
    const now = Date.now();
    const diff = now - lastRequestTime;
    if (diff < CONFIG.minDelayMs) {
        await sleep(CONFIG.minDelayMs - diff);
    }
    lastRequestTime = Date.now();
}

async function handleRateLimit(attempt) {
    const backoff = Math.min(
        CONFIG.baseBackoffMs * Math.pow(2, attempt - 1),
        CONFIG.maxBackoffMs
    );
    logInfo(`Rate limit – attendo ${backoff / 1000}s`);
    await sleep(backoff);
}

/* ================= API SAFE CALL ================= */

async function safeApiCall(exchange, endpoint, params) {
    let attempt = 1;

    while (attempt <= CONFIG.maxRetries) {
        try {
            await smartRateLimit();
            logDebug(`API ${endpoint} (try ${attempt})`);
            return await exchange.fetch2(endpoint, 'sapi', 'GET', params);
        } catch (err) {
            logError(err.message);

            if (err.message.includes('429')) {
                if (attempt < CONFIG.maxRetries) {
                    await handleRateLimit(attempt++);
                    continue;
                }
            }

            if (
                err.message.toLowerCase().includes('timeout') ||
                err.message.toLowerCase().includes('network')
            ) {
                if (attempt < CONFIG.maxRetries) {
                    await sleep(3000);
                    attempt++;
                    continue;
                }
            }

            throw err;
        }
    }
}

/* ================= DATE UTILS ================= */

function toUtcMidnight(dateStr) {
    const [y, m, d] = dateStr.split('-').map(Number);
    return Date.UTC(y, m - 1, d, 0, 0, 0);
}

/* ================= SNAPSHOT LOGIC ================= */

async function fetchSnapshotAtDate(exchange, targetDateStr) {
    const targetTs = toUtcMidnight(targetDateStr);
    const dayMs = 24 * 60 * 60 * 1000;

    const startTime = targetTs - CONFIG.snapshotWindowDays * dayMs;
    const endTime   = targetTs + CONFIG.snapshotWindowDays * dayMs;

    logInfo(`Target date UTC: ${new Date(targetTs).toISOString()}`);
    logInfo(`Fetching snapshots range...`);

    const response = await safeApiCall(
        exchange,
        'accountSnapshot',
        {
            type: 'SPOT',
            startTime,
            endTime
        }
    );

    const snapshots = response?.snapshotVos || [];

    if (snapshots.length === 0) {
        throw new Error('Nessuno snapshot disponibile nel range richiesto');
    }

    // Trova snapshot più vicino alla data target
    let best = null;
    let minDiff = Number.MAX_SAFE_INTEGER;

    for (const snap of snapshots) {
        const diff = Math.abs(snap.updateTime - targetTs);
        if (diff < minDiff) {
            minDiff = diff;
            best = snap;
        }
    }

    logInfo(
        `Snapshot selezionato: ${new Date(best.updateTime).toISOString()}`
    );

    return best;
}

/* ================= MAIN ================= */

async function main() {
    try {
        const args = process.argv.slice(2);
        if (args.length !== 4) {
            logError('Uso: node snapshot.js binance apiKey secret YYYY-MM-DD');
            process.exit(1);
        }

        const [exchangeId, apiKey, secret, dateStr] = args;

        const exchange = new ccxt[exchangeId]({
            apiKey,
            secret,
            enableRateLimit: true,
            options: {
                defaultType: 'spot',
                recvWindow: 10000,
                adjustForTimeDifference: true
            }
        });

        if (exchangeId === 'binance') {
            logInfo('Sincronizzazione orario Binance...');
            await exchange.loadTimeDifference();
        }

        const snapshot = await fetchSnapshotAtDate(exchange, dateStr);

        console.log(JSON.stringify({
            snapshotDateRequested: dateStr,
            snapshotDateReturned: new Date(snapshot.updateTime).toISOString(),
            balances: snapshot.data.balances
        }, null, 2));

    } catch (err) {
        logError(`Errore critico: ${err.message}`);
        process.exit(1);
    }
}

main();
