'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { apriDb } = require('../src/db');
const { Coperture } = require('../src/coperture');
const { Semaforo } = require('../src/concorrenza');
const { gestisciRichiesta, leggiPunti, allineaGiornoInizio, allineaGiornoFine, FINESTRA_MS } = require('../src/prezzoService');

const MIN = 60_000;
const ORA = 60 * MIN;
const GIORNO = 24 * ORA;

function nuovoContesto({ semaforoMax = 3, cercaPrezziStorici } = {}) {
    const db = apriDb(':memory:');
    return {
        db,
        coperture: new Coperture(db),
        semaforo: new Semaforo(semaforoMax),
        lucchetti: new Set(),
        ccxtLib: {},
        exchangeIds: ['binance'],
        cercaPrezziStorici: cercaPrezziStorici || (async () => []),
    };
}

test('allineamento ai giorni: arrotonda per difetto/eccesso al bordo del giorno (UTC)', () => {
    assert.equal(allineaGiornoInizio(GIORNO * 3 + 5 * ORA), GIORNO * 3);
    assert.equal(allineaGiornoInizio(GIORNO * 3), GIORNO * 3);
    assert.equal(allineaGiornoFine(GIORNO * 3 + 5 * ORA), GIORNO * 4 - 1);
    assert.equal(allineaGiornoFine(GIORNO * 4 - 1), GIORNO * 4 - 1);
});

test('finestra già coperta: risolve subito, senza chiamare cercaPrezziStorici', async () => {
    let chiamato = false;
    const ctx = nuovoContesto({ cercaPrezziStorici: async () => { chiamato = true; return []; } });
    const timestamp = ORA * 10;
    ctx.coperture.aggiungiIntervallo('BTC', timestamp - FINESTRA_MS, timestamp + FINESTRA_MS);

    const r = await gestisciRichiesta({ ...ctx, symbol: 'BTC', timestamp });

    assert.equal(r.esito, 'risolto');
    assert.deepEqual(r.punti, []);
    assert.equal(chiamato, false);
});

test('finestra coperta e con prezzi: li restituisce', async () => {
    const ctx = nuovoContesto();
    const timestamp = ORA * 10;
    const start = timestamp - FINESTRA_MS;
    const end = timestamp + FINESTRA_MS;
    ctx.coperture.aggiungiIntervallo('BTC', start, end);
    require('../src/prezzoService').salvaPunti(ctx.db, 'BTC', [
        { timestamp, prices: { binance: 61000.5 } },
    ]);

    const r = await gestisciRichiesta({ ...ctx, symbol: 'BTC', timestamp });

    assert.equal(r.esito, 'risolto');
    assert.deepEqual(r.punti, [{ timestamp, prices: { binance: 61000.5 } }]);
});

test('buco: scarica solo la parte mancante allineata ai giorni, la salva e risponde completo', async () => {
    let ricevuto = null;
    const ctx = nuovoContesto({
        cercaPrezziStorici: async (args) => {
            ricevuto = args;
            return [{ timestamp: args.since, prices: { binance: 1.0 } }];
        },
    });
    const timestamp = GIORNO * 10 + 5 * MIN;

    const r = await gestisciRichiesta({ ...ctx, symbol: 'USDC', timestamp });

    assert.equal(r.esito, 'risolto');
    assert.equal(ricevuto.baseSymbol, 'USDC');
    assert.equal(ricevuto.since, allineaGiornoInizio(timestamp - FINESTRA_MS));
    assert.equal(ricevuto.until, allineaGiornoFine(timestamp + FINESTRA_MS));
    assert.equal(ctx.coperture.completa('USDC', timestamp - FINESTRA_MS, timestamp + FINESTRA_MS), true);
});

test('buco riempito ma nessun prezzo trovato: risolto con array vuoto (buco confermato)', async () => {
    const ctx = nuovoContesto({ cercaPrezziStorici: async () => [] });
    const timestamp = ORA * 10;

    const r = await gestisciRichiesta({ ...ctx, symbol: 'MON', timestamp });

    assert.equal(r.esito, 'risolto');
    assert.deepEqual(r.punti, []);
    // la finestra è comunque coperta: una richiesta successiva non deve riscaricare
    assert.equal(ctx.coperture.completa('MON', timestamp - FINESTRA_MS, timestamp + FINESTRA_MS), true);
});

test('semaforo pieno: risponde occupato senza scaricare né toccare le coperture', async () => {
    let chiamato = false;
    const ctx = nuovoContesto({
        semaforoMax: 1,
        cercaPrezziStorici: async () => { chiamato = true; return []; },
    });
    ctx.semaforo.acquisisci(); // simula un fetch già in corso per un'altra moneta

    const timestamp = ORA * 10;
    const r = await gestisciRichiesta({ ...ctx, symbol: 'BTC', timestamp });

    assert.deepEqual(r, { esito: 'occupato' });
    assert.equal(chiamato, false);
    assert.equal(ctx.coperture.intervalli('BTC').length, 0);
});

test('lucchetto già preso per la stessa moneta: risponde occupato', async () => {
    const ctx = nuovoContesto();
    ctx.lucchetti.add('BTC');

    const r = await gestisciRichiesta({ ...ctx, symbol: 'BTC', timestamp: ORA * 10 });

    assert.deepEqual(r, { esito: 'occupato' });
});

test('il lucchetto e il semaforo si rilasciano anche se cercaPrezziStorici lancia un errore', async () => {
    const ctx = nuovoContesto({
        cercaPrezziStorici: async () => { throw new Error('rete non raggiungibile'); },
    });

    await assert.rejects(() => gestisciRichiesta({ ...ctx, symbol: 'BTC', timestamp: ORA * 10 }));

    assert.equal(ctx.lucchetti.has('BTC'), false);
    assert.equal(ctx.semaforo.disponibile(), true);
});

test('leggiPunti restituisce solo la finestra richiesta, ordinata', () => {
    const ctx = nuovoContesto();
    require('../src/prezzoService').salvaPunti(ctx.db, 'ETH', [
        { timestamp: 200, prices: { binance: 2 } },
        { timestamp: 100, prices: { binance: 1, coinbase: 1.01 } },
        { timestamp: 900, prices: { binance: 9 } }, // fuori finestra
    ]);

    const punti = leggiPunti(ctx.db, 'ETH', 0, 300);

    assert.deepEqual(punti, [
        { timestamp: 100, prices: { binance: 1, coinbase: 1.01 } },
        { timestamp: 200, prices: { binance: 2 } },
    ]);
});
