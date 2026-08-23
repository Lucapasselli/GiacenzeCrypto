'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { apriDb } = require('../src/db');
const { Coperture } = require('../src/coperture');
const { analizzaArgomenti, analizzaData, elaboraMoneta } = require('../strumenti/manutenzioneBuchi');

const GIORNO = 24 * 60 * 60 * 1000;

test('analizzaArgomenti: --chiave=valore e flag booleani', () => {
    const p = analizzaArgomenti(['--symbol=BTC', '--dry-run', 'ignorato', '--since=2024-01-01']);
    assert.deepEqual(p, { symbol: 'BTC', 'dry-run': true, since: '2024-01-01' });
});

test('analizzaData: accetta epoch in ms e date ISO', () => {
    assert.equal(analizzaData('1700000000000', '--since'), 1700000000000);
    assert.equal(analizzaData('2024-01-01', '--since'), Date.parse('2024-01-01'));
    assert.throws(() => analizzaData('non-una-data', '--since'), /non valido/);
});

function nuovoContesto() {
    const db = apriDb(':memory:');
    return { db, coperture: new Coperture(db) };
}

test('nessun buco: non chiama cercaPrezziStoriciFn', async () => {
    const { db, coperture } = nuovoContesto();
    const since = GIORNO * 10;
    const until = GIORNO * 11;
    coperture.aggiungiIntervallo('BTC', since, until);

    let chiamato = false;
    await elaboraMoneta({
        db, coperture, cfg: { exchange: ['binance'] }, symbol: 'BTC', since, until, dryRun: false,
        ccxtLib: {}, cercaPrezziStoriciFn: async () => { chiamato = true; return []; },
    });

    assert.equal(chiamato, false);
});

test('dry-run: elenca il buco ma non scarica né tocca le coperture', async () => {
    const { db, coperture } = nuovoContesto();
    let chiamato = false;

    await elaboraMoneta({
        db, coperture, cfg: { exchange: ['binance'] }, symbol: 'BTC',
        since: GIORNO * 10, until: GIORNO * 11, dryRun: true,
        ccxtLib: {}, cercaPrezziStoriciFn: async () => { chiamato = true; return []; },
    });

    assert.equal(chiamato, false);
    assert.equal(coperture.intervalli('BTC').length, 0);
});

test('buco reale: scarica, salva e registra la copertura allineata al giorno', async () => {
    const { db, coperture } = nuovoContesto();
    const since = GIORNO * 10 + 5 * 60 * 60 * 1000; // metà giornata
    const until = GIORNO * 10 + 6 * 60 * 60 * 1000;
    let ricevuto = null;

    await elaboraMoneta({
        db, coperture, cfg: { exchange: ['binance', 'okx'] }, symbol: 'BTC', since, until, dryRun: false,
        ccxtLib: { finto: true }, pausaMs: 0,
        cercaPrezziStoriciFn: async (args) => {
            ricevuto = args;
            return [{ timestamp: args.since, prices: { binance: 61000 } }];
        },
    });

    assert.equal(ricevuto.baseSymbol, 'BTC');
    assert.deepEqual(ricevuto.exchangeIds, ['binance', 'okx']);
    assert.equal(ricevuto.since, GIORNO * 10);
    assert.equal(ricevuto.until, GIORNO * 11 - 1);
    assert.equal(coperture.completa('BTC', since, until), true);

    const righe = db.prepare('SELECT * FROM Prezzi WHERE symbol = ?').all('BTC');
    assert.equal(righe.length, 1);
});

test('più buchi nel periodo: uno scaricamento per ciascuno', async () => {
    const { db, coperture } = nuovoContesto();
    coperture.aggiungiIntervallo('BTC', GIORNO * 10, GIORNO * 11 - 1); // giorno 10 già coperto
    // buco: giorno 11; poi giorno 12 già coperto; poi buco: giorno 13
    coperture.aggiungiIntervallo('BTC', GIORNO * 12, GIORNO * 13 - 1);

    const chiamate = [];
    await elaboraMoneta({
        db, coperture, cfg: { exchange: ['binance'] }, symbol: 'BTC',
        since: GIORNO * 10, until: GIORNO * 14 - 1, dryRun: false,
        ccxtLib: {}, pausaMs: 0,
        cercaPrezziStoriciFn: async (args) => { chiamate.push(args); return []; },
    });

    assert.equal(chiamate.length, 2);
    assert.equal(chiamate[0].since, GIORNO * 11);
    assert.equal(chiamate[1].since, GIORNO * 13);
});
