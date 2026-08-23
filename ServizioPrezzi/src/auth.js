'use strict';

const rateLimit = require('express-rate-limit');

/**
 * La API key è anti-abuso, non un vero segreto: viaggia dentro un programma distribuito ed è
 * estraibile (vedi Analisi_VPS_Prezzi_Sito.md). Va quindi accoppiata a un rate limit per IP, non
 * solo per chiave — qui sotto in `limitatoreVelocita`.
 */
function verificaApiKey(chiaveAttesa) {
    if (!chiaveAttesa) {
        throw new Error('PREZZI_API_KEY non impostata: il servizio non può avviarsi senza una chiave');
    }
    return (req, res, next) => {
        const chiave = req.get('X-Api-Key');
        if (chiave !== chiaveAttesa) {
            res.status(401).json({ esito: 'non-autorizzato' });
            return;
        }
        next();
    };
}

/** Rate limit per IP — indipendente dalla API key, protegge anche in caso di chiave trapelata. */
function limitatoreVelocita({ finestraMs = 60_000, maxRichieste = 120 } = {}) {
    return rateLimit({
        windowMs: finestraMs,
        limit: maxRichieste,
        standardHeaders: true,
        legacyHeaders: false,
        handler: (req, res) => {
            res.status(429).json({ esito: 'troppe-richieste' });
        },
    });
}

module.exports = { verificaApiKey, limitatoreVelocita };
