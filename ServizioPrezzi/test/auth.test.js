'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const express = require('express');
const { verificaApiKey, limitatoreVelocita } = require('../src/auth');

test('verificaApiKey rifiuta senza una chiave attesa configurata', () => {
    assert.throws(() => verificaApiKey(''), /PREZZI_API_KEY/);
    assert.throws(() => verificaApiKey(undefined), /PREZZI_API_KEY/);
});

async function avviaApp(middleware) {
    const app = express();
    app.use('/v1', middleware, (req, res) => res.json({ ok: true }));
    const server = app.listen(0, '127.0.0.1');
    await new Promise((resolve) => server.once('listening', resolve));
    const { port } = server.address();
    return {
        url: (percorso) => `http://127.0.0.1:${port}${percorso}`,
        chiudi: () => new Promise((resolve) => server.close(resolve)),
    };
}

test('chiave mancante o sbagliata: 401', async () => {
    const app = await avviaApp(verificaApiKey('segreta'));
    try {
        let res = await fetch(app.url('/v1/qualcosa'));
        assert.equal(res.status, 401);

        res = await fetch(app.url('/v1/qualcosa'), { headers: { 'X-Api-Key': 'sbagliata' } });
        assert.equal(res.status, 401);
    } finally {
        await app.chiudi();
    }
});

test('chiave corretta: passa', async () => {
    const app = await avviaApp(verificaApiKey('segreta'));
    try {
        const res = await fetch(app.url('/v1/qualcosa'), { headers: { 'X-Api-Key': 'segreta' } });
        assert.equal(res.status, 200);
    } finally {
        await app.chiudi();
    }
});

test('rate limit: oltre la soglia risponde 429', async () => {
    const app = await avviaApp(limitatoreVelocita({ finestraMs: 60_000, maxRichieste: 2 }));
    try {
        assert.equal((await fetch(app.url('/v1/x'))).status, 200);
        assert.equal((await fetch(app.url('/v1/x'))).status, 200);
        const terza = await fetch(app.url('/v1/x'));
        assert.equal(terza.status, 429);
        assert.equal((await terza.json()).esito, 'troppe-richieste');
    } finally {
        await app.chiudi();
    }
});
