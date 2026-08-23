'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const express = require('express');
const { apriDb } = require('../src/db');
const { Coperture } = require('../src/coperture');
const { Semaforo } = require('../src/concorrenza');
const { creaRoutePrezzo } = require('../routes/prezzo');

// Il route legge sempre config/monete.json (nessuna cache, vedi configurazione.js): i test usano
// il file vero del repository, che elenca BTC/USDC fra le monete abilitate.

async function avviaApp(cercaPrezziStorici) {
    const db = apriDb(':memory:');
    const coperture = new Coperture(db);
    const semaforo = new Semaforo(3);
    const app = express();
    app.use('/v1', creaRoutePrezzo({
        db,
        coperture,
        semaforo,
        lucchetti: new Set(),
        ccxtLib: {},
        cercaPrezziStorici: cercaPrezziStorici || (async () => []),
    }));

    const server = app.listen(0, '127.0.0.1');
    await new Promise((resolve) => server.once('listening', resolve));
    const { port } = server.address();
    return {
        url: (percorso) => `http://127.0.0.1:${port}${percorso}`,
        chiudi: () => new Promise((resolve) => server.close(resolve)),
    };
}

test('moneta non in config/monete.json: 400', async () => {
    const app = await avviaApp();
    try {
        const res = await fetch(app.url('/v1/prezzo?symbol=UNAMONETAINVENTATA&timestamp=1700000000000'));
        assert.equal(res.status, 400);
    } finally {
        await app.chiudi();
    }
});

test('timestamp mancante: 400', async () => {
    const app = await avviaApp();
    try {
        const res = await fetch(app.url('/v1/prezzo?symbol=BTC'));
        assert.equal(res.status, 400);
    } finally {
        await app.chiudi();
    }
});

test('timestamp nel futuro: 400', async () => {
    const app = await avviaApp();
    try {
        const res = await fetch(app.url(`/v1/prezzo?symbol=BTC&timestamp=${Date.now() + 3_600_000}`));
        assert.equal(res.status, 400);
    } finally {
        await app.chiudi();
    }
});

test('richiesta valida su moneta mai vista: scarica, salva, risponde 200 risolto', async () => {
    const timestamp = 1700000000000;
    // il punto finto cade dentro la finestra stretta ±10 minuti richiesta, non solo dentro
    // l'intervallo più ampio (allineato alle ore) che il servizio scarica per riempire il buco.
    const app = await avviaApp(async () => [
        { timestamp, prices: { binance: 61000 } },
    ]);
    try {
        const res = await fetch(app.url(`/v1/prezzo?symbol=BTC&timestamp=${timestamp}`));
        const corpo = await res.json();
        assert.equal(res.status, 200);
        assert.equal(corpo.esito, 'risolto');
        assert.deepEqual(corpo.punti, [{ timestamp, prices: { binance: 61000 } }]);
    } finally {
        await app.chiudi();
    }
});

test('semaforo esaurito: 503 occupato', async () => {
    const db = apriDb(':memory:');
    const coperture = new Coperture(db);
    const semaforo = new Semaforo(1);
    semaforo.acquisisci();
    const app = express();
    app.use('/v1', creaRoutePrezzo({
        db, coperture, semaforo, lucchetti: new Set(), ccxtLib: {},
        cercaPrezziStorici: async () => [],
    }));
    const server = app.listen(0, '127.0.0.1');
    await new Promise((resolve) => server.once('listening', resolve));
    const { port } = server.address();

    try {
        const res = await fetch(`http://127.0.0.1:${port}/v1/prezzo?symbol=BTC&timestamp=1700000000000`);
        const corpo = await res.json();
        assert.equal(res.status, 503);
        assert.equal(corpo.esito, 'occupato');
    } finally {
        await new Promise((resolve) => server.close(resolve));
    }
});
