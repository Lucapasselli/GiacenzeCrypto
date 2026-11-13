// Correzione della connessione di ccxt, caricata in ogni processo Node con `node --require`
// (CcxtInterop.fetchMovimento e ServizioNodePrezzi.ProcessoScript): nessuno script la richiama.
//
// IL PROBLEMA (2026-11-13). Dalla 4.5.77 ccxt crea il suo agente undici con
// `autoSelectFamilyAttemptTimeout: 10` (ms), il minimo che Node accetta, per far partire quasi in parallelo
// IPv6 e IPv4. Ma quel numero e' anche il tempo concesso a OGNI tentativo di connessione: se il
// three-way handshake dura di piu' (basta un ping di 21 ms verso Cloudflare) il tentativo scade, e dopo
// tutti gli indirizzi la richiesta fallisce con ETIMEDOUT, che ccxt riporta come "fetch failed:" senza
// altro. Su un computer senza IPv6 globale falliscono cosi' tutti gli exchange, non solo uno: OKX, Binance,
// KuCoin, Coinbase, Crypto.com. Misurato con undici da solo: 10 ms -> 10 tentativi su 10 in ETIMEDOUT,
// da 25 ms in su -> tutti riusciti.
//
// LA CORREZIONE: si rimette 250 ms, il default di Node e il valore consigliato dall'RFC 8305. Si cambia
// solo quel campo, tutte le altre opzioni del dispatcher (keep-alive, connessioni, ecc.) restano di ccxt.
// Se una versione futura di ccxt cambia il metodo, la correzione non fa nulla e non rompe niente.

'use strict';

const ATTESA_TENTATIVO_MS = 250;

try {
    const ccxt = require('ccxt');
    const proto = ccxt && ccxt.Exchange && ccxt.Exchange.prototype;
    if (proto && typeof proto.getDispatcherOptions === 'function' && !proto.getDispatcherOptions.__giacenzeRete) {
        const originale = proto.getDispatcherOptions;
        const corretto = function (...argomenti) {
            const opzioni = originale.apply(this, argomenti);
            if (opzioni && typeof opzioni === 'object'
                && typeof opzioni.autoSelectFamilyAttemptTimeout === 'number'
                && opzioni.autoSelectFamilyAttemptTimeout < ATTESA_TENTATIVO_MS) {
                opzioni.autoSelectFamilyAttemptTimeout = ATTESA_TENTATIVO_MS;
            }
            return opzioni;
        };
        corretto.__giacenzeRete = true;
        proto.getDispatcherOptions = corretto;
    }
} catch (e) {
    // ccxt non ancora installato (la prima volta si installa dopo) o cambiato: si va avanti senza correzione.
}
