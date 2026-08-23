'use strict';

const express = require('express');
const ccxt = require('ccxt');
const { apriDb } = require('./src/db');
const { Coperture } = require('./src/coperture');
const { Semaforo } = require('./src/concorrenza');
const { leggiConfigurazione } = require('./src/configurazione');
const { verificaApiKey, limitatoreVelocita } = require('./src/auth');
const { creaRoutePrezzo } = require('./routes/prezzo');
// Stesso modulo usato dal client (ProcessBuilder verso Node) per non duplicare la logica di
// scelta della coppia/conversione — vedi Analisi_VPS_Prezzi_Sito.md, "sesta parte".
const { cercaPrezziStorici } = require('../Scripts/Historical_Multi_Eur.js');

const PORT = process.env.PORT || 4173;
// In ascolto solo su localhost: l'esposizione pubblica passa da Caddy come reverse proxy,
// stesso schema già usato per il sito (vedi Analisi_VPS_Prezzi_Sito.md).
const HOST = process.env.HOST || '127.0.0.1';

const db = apriDb();
const coperture = new Coperture(db);
const cfgIniziale = leggiConfigurazione(); // solo per dimensionare il semaforo all'avvio
const semaforo = new Semaforo(cfgIniziale.semaforoConcorrenza);
const lucchetti = new Set();

const app = express();

app.get('/health', (req, res) => {
    res.json({ ok: true });
});

app.use('/v1', limitatoreVelocita(), verificaApiKey(process.env.PREZZI_API_KEY), creaRoutePrezzo({
    db,
    coperture,
    semaforo,
    lucchetti,
    ccxtLib: ccxt,
    cercaPrezziStorici,
}));

if (require.main === module) {
    app.listen(PORT, HOST, () => {
        console.log(`Servizio prezzi in ascolto su ${HOST}:${PORT}`);
    });
}

module.exports = { app, db, coperture };
