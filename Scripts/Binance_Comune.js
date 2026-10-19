// Funzioni comuni agli script Binance: creazione del client, ritmo delle richieste, ripetizioni.
//
// RITMO. Lo da' il limitatore di ccxt, che per ogni endpoint conosce il "peso" dichiarato da Binance
// (voce 'cost' in ccxt/js/src/binance.js): 0,1 per depositi e dust, 1 per gli asset dividend, 2 per i
// prelievi, 4 per myTrades, 15 per i premi Earn, 20 per Convert, 600 per fiat/orders. Il tempo fra due
// chiamate e' costo x rateLimit. Prima ogni script aspettava un tempo fisso uguale per tutte le chiamate
// (da 2 a 30 secondi), da 10 a 300 volte piu' del necessario: uno scaricamento senza novita' durava
// cinque minuti. Il rateLimit e' 75 ms invece dei 50 di ccxt: un margine del 50% per restare sotto i
// limiti anche se sulla stessa rete o con la stessa chiave lavora qualcos'altro.
//
// ERRORI. Una chiamata fallita per rete o limite di richieste si ripete; tutto il resto, e anche questi
// dopo MAX_TENTATIVI, fa uscire lo script con codice 1. E' l'unico modo in cui Java (Download.ErroriNodeJS)
// si accorge del guasto: uno script che finisse "bene" con meno dati farebbe importare uno scaricamento
// incompleto, e il successivo partirebbe dopo l'ultimo movimento, scavalcando per sempre quelli persi.

const ccxt = require('ccxt');

const RATE_LIMIT_MS = 75;
const MAX_TENTATIVI = 6;
// Oltre questa attesa (Retry-After di un blocco 418, che puo' durare giorni) non si aspetta: si esce.
const ATTESA_MASSIMA_MS = 5 * 60 * 1000;

function logInfo(msg) { console.error(`[INFO] ${msg}`); }
function logError(msg) { console.error(`[ERROR] ${msg}`); }
function sleep(ms) { return new Promise(resolve => setTimeout(resolve, ms)); }

/** Client Binance spot con il limitatore di ccxt acceso. */
function creaExchange(exchangeId, apiKey, secret) {
    return new ccxt[exchangeId]({
        apiKey,
        secret,
        enableRateLimit: true,
        rateLimit: RATE_LIMIT_MS,
        timeout: 60000,
        options: {
            defaultType: 'spot',
            recvWindow: 10000,
            adjustForTimeDifference: true,
            // Bastano le coppie spot: i mercati futures costano altre due richieste e diversi MB.
            fetchMarkets: { types: ['spot'] }
        }
    });
}

/** Allinea l'orologio a quello di Binance (una richiesta). Non blocca se fallisce. */
async function sincronizzaOrario(exchange) {
    try {
        await exchange.loadTimeDifference();
        logInfo(`Differenza oraria con Binance: ${exchange.timeDifference} ms`);
    } catch (e) {
        logError(`Sincronizzazione dell'orario non riuscita: ${e.message}`);
    }
}

/** Peso della chiamata secondo ccxt; 1 se l'endpoint non e' censito. */
function costo(exchange, api, endpoint) {
    const c = exchange.api?.[api]?.get?.[endpoint]?.cost;
    return (typeof c === 'number') ? c : 1;
}

function attesaRichiestaDaBinance(exchange) {
    const h = exchange.last_response_headers || {};
    const s = Number(h['retry-after'] ?? h['Retry-After']);
    return Number.isFinite(s) && s > 0 ? s * 1000 : 0;
}

/**
 * Esegue una GET firmata con il peso dell'endpoint, ripetendola sugli errori transitori.
 * Gli altri errori (chiave senza permessi, parametro rifiutato, -6021 intervallo troppo ampio...) passano
 * al chiamante cosi' come sono.
 * @param costoEsplicito peso per gli endpoint che ccxt non censisce (altrimenti varrebbero 1)
 */
async function chiama(exchange, endpoint, params, api = 'sapi', costoEsplicito = undefined) {
    const config = { cost: costoEsplicito ?? costo(exchange, api, endpoint) };
    for (let tentativo = 1; ; tentativo++) {
        try {
            return await exchange.fetch2(endpoint, api, 'GET', params, undefined, undefined, config);
        } catch (e) {
            // NetworkError comprende RateLimitExceeded, DDoSProtection, RequestTimeout, InvalidNonce e
            // il "fetch failed" di una linea caduta.
            if (!(e instanceof ccxt.NetworkError) || tentativo >= MAX_TENTATIVI) throw e;
            let attesa;
            if (e instanceof ccxt.InvalidNonce) {
                await sincronizzaOrario(exchange);
                attesa = 1000;
            } else if (e instanceof ccxt.RateLimitExceeded || e instanceof ccxt.DDoSProtection) {
                attesa = Math.max(attesaRichiestaDaBinance(exchange), 30000 * tentativo);
            } else {
                attesa = Math.min(3000 * 2 ** (tentativo - 1), 60000);
            }
            if (attesa > ATTESA_MASSIMA_MS) throw e;
            logError(`${endpoint}: tentativo ${tentativo} fallito (${e.constructor.name}: ${e.message}), riprovo fra ${Math.round(attesa / 1000)} s`);
            await sleep(attesa);
        }
    }
}

/** Ripete una chiamata ccxt unificata (es. fetchMyTrades) con le stesse regole di chiama(). */
async function ripeti(fn, etichetta) {
    for (let tentativo = 1; ; tentativo++) {
        try {
            return await fn();
        } catch (e) {
            if (!(e instanceof ccxt.NetworkError) || tentativo >= MAX_TENTATIVI) throw e;
            const attesa = (e instanceof ccxt.RateLimitExceeded || e instanceof ccxt.DDoSProtection)
                ? 30000 * tentativo
                : Math.min(3000 * 2 ** (tentativo - 1), 60000);
            logError(`${etichetta}: tentativo ${tentativo} fallito (${e.constructor.name}: ${e.message}), riprovo fra ${Math.round(attesa / 1000)} s`);
            await sleep(attesa);
        }
    }
}

/** Uscita per errore: messaggio su stderr e codice 1, che Java tratta come scaricamento non riuscito. */
function esciConErrore(e) {
    logError(`Errore critico: ${e && e.message ? e.message : e}`);
    process.exit(1);
}

module.exports = { creaExchange, sincronizzaOrario, chiama, ripeti, esciConErrore, logInfo, logError, sleep };
