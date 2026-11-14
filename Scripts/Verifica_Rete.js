// Verifica della raggiungibilita' degli exchange dallo STESSO stack di rete che usano le importazioni e i prezzi:
// Node + ccxt (+ la correzione Ccxt_Rete.js caricata con `node --require`, vedi ServizioNodePrezzi.OpzioniPreload).
// Lanciato da DiagnosiServizi.java (pulsante "Verifica la disponibilita' dei servizi"), mai dalle importazioni.
//
// Perche' esiste (2026-11-13): una segnalazione da Windows mostrava il servizio prezzi muto per 300 s senza dire
// perche'. Java raggiunge Internet con un'altra libreria, quindi un exchange raggiungibile da Java e non da Node
// (antivirus o firewall su node.exe, rete senza IPv6 con il difetto di undici, proxy) si vede solo confrontando
// i due percorsi. Qui si usa `exchange.fetch(url)` di ccxt: stessa libreria HTTP, stesso dispatcher, stesse
// impostazioni di rete, ma nessun markets da scaricare e nessuna chiave.
//
// Uscita: una riga JSON per messaggio su stdout, i log su stderr.
//   {"tipo":"ambiente","node":"v24.7.0","ccxt":"4.5.77","ipv6":false,"proxy":false,"correzione":true}
//   {"tipo":"exchange","exchange":"okx","ok":true,"ms":212,"nota":""}
//   {"tipo":"exchange","exchange":"kucoin","ok":false,"ms":15003,"errore":"..."}
// Ogni exchange e' interrogato in parallelo con un limite di 15 s; un limite generale di 25 s chiude comunque tutto.

'use strict';

const os = require('os');

// Endpoint pubblici leggeri (ora del server o un ticker), gli stessi host che ccxt usa per i prezzi storici.
const ENDPOINT = {
    binance: 'https://api.binance.com/api/v3/time',
    cryptocom: 'https://api.crypto.com/exchange/v1/public/get-tickers?instrument_name=BTC_USDT',
    bybit: 'https://api.bybit.com/v5/market/time',
    okx: 'https://www.okx.com/api/v5/public/time',
    coinbase: 'https://api.exchange.coinbase.com/time',
    bitstamp: 'https://www.bitstamp.net/api/v2/ticker/btceur/',
    kucoin: 'https://api.kucoin.com/api/v1/timestamp',
    bitget: 'https://api.bitget.com/api/v2/public/time',
};

const LIMITE_EXCHANGE_MS = 15000;
const LIMITE_GENERALE_MS = 25000;

function scrivi(oggetto) {
    process.stdout.write(JSON.stringify(oggetto) + '\n');
}

// Errori che vogliono dire "la richiesta non e' arrivata o non e' tornata": rete, DNS, TLS, timeout.
// Una risposta HTTP di errore (403 di Cloudflare, 429, 5xx) vuol dire invece che l'host e' stato raggiunto.
const ERRORE_DI_RETE = /fetch failed|ETIMEDOUT|ECONNREFUSED|ECONNRESET|ENOTFOUND|EAI_AGAIN|EHOSTUNREACH|ENETUNREACH|UND_ERR|timed? ?out|socket|certificate|self.signed|network/i;

function breve(testo) {
    const t = String(testo || '').replace(/\s+/g, ' ').trim();
    return t.length > 220 ? t.substring(0, 220) + '...' : t;
}

async function verificaExchange(ccxt, id) {
    const inizio = Date.now();
    try {
        if (!ccxt[id]) return { tipo: 'exchange', exchange: id, ok: false, ms: 0, errore: 'exchange assente in questa versione di ccxt' };
        const ex = new ccxt[id]();
        ex.timeout = LIMITE_EXCHANGE_MS;
        await ex.fetch(ENDPOINT[id], 'GET');
        return { tipo: 'exchange', exchange: id, ok: true, ms: Date.now() - inizio, nota: '' };
    } catch (err) {
        const ms = Date.now() - inizio;
        const testo = breve(((err && err.message) || err) + (err && err.cause && err.cause.code ? ` (${err.cause.code})` : ''));
        const nome = (err && err.constructor && err.constructor.name) || 'Errore';
        const eRete = (ccxt.RequestTimeout && err instanceof ccxt.RequestTimeout) || ERRORE_DI_RETE.test(testo);
        if (eRete) return { tipo: 'exchange', exchange: id, ok: false, ms, errore: `${nome}: ${testo}` };
        // L'host ha risposto, con un errore HTTP: raggiungibile.
        return { tipo: 'exchange', exchange: id, ok: true, ms, nota: `risposta con errore (${nome})` };
    }
}

async function main() {
    let ccxt;
    try {
        ccxt = require('ccxt');
    } catch (e) {
        scrivi({ tipo: 'errore', errore: `ccxt non caricabile: ${breve(e.message)}` });
        process.exit(0);
    }

    let ipv6 = false;
    try {
        const schede = os.networkInterfaces();
        for (const nome in schede) {
            for (const a of schede[nome] || []) {
                if ((a.family === 'IPv6' || a.family === 6) && !a.internal && !a.address.toLowerCase().startsWith('fe80')) ipv6 = true;
            }
        }
    } catch (e) { /* solo informativo */ }
    const proto = ccxt.Exchange && ccxt.Exchange.prototype;
    scrivi({
        tipo: 'ambiente',
        node: process.version,
        piattaforma: `${process.platform}-${process.arch}`,
        ccxt: ccxt.version || '?',
        ipv6,
        proxy: ['HTTPS_PROXY', 'HTTP_PROXY', 'ALL_PROXY'].some(n => process.env[n] || process.env[n.toLowerCase()]),
        correzione: !!(proto && proto.getDispatcherOptions && proto.getDispatcherOptions.__giacenzeRete),
    });

    const ids = process.argv.length > 2 ? process.argv[2].split(',').map(s => s.trim()).filter(s => ENDPOINT[s]) : Object.keys(ENDPOINT);
    const fatti = new Set();
    const inizio = Date.now();
    const limite = setTimeout(() => {
        for (const id of ids) {
            if (!fatti.has(id)) scrivi({ tipo: 'exchange', exchange: id, ok: false, ms: Date.now() - inizio, errore: `nessuna risposta entro ${LIMITE_GENERALE_MS / 1000} s` });
        }
        process.exit(0);
    }, LIMITE_GENERALE_MS);

    await Promise.all(ids.map(async id => {
        const esito = await verificaExchange(ccxt, id);
        fatti.add(id);
        scrivi(esito);
    }));
    clearTimeout(limite);
    process.exit(0);
}

main();
