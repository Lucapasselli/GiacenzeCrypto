'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { apriDb } = require('../src/db');
const { Coperture } = require('../src/coperture');

function nuovaCoperture() {
    return new Coperture(apriDb(':memory:'));
}

// Minuti espressi come millisecondi da un'origine arbitraria, per leggibilità nei test.
const MIN = 60_000;
const h = (ore, minuti = 0) => (ore * 60 + minuti) * MIN;

test('nessuna copertura -> tutta la finestra è un buco', () => {
    const c = nuovaCoperture();
    assert.deepEqual(c.buchi('BTC', h(10), h(11)), [{ start: h(10), end: h(11) }]);
    assert.equal(c.completa('BTC', h(10), h(11)), false);
});

test('copertura esatta sulla finestra richiesta -> nessun buco', () => {
    const c = nuovaCoperture();
    c.aggiungiIntervallo('BTC', h(10), h(11));
    assert.deepEqual(c.buchi('BTC', h(10), h(11)), []);
    assert.equal(c.completa('BTC', h(10), h(11)), true);
});

test('copertura più ampia della finestra richiesta -> nessun buco', () => {
    const c = nuovaCoperture();
    c.aggiungiIntervallo('BTC', h(9), h(12));
    assert.deepEqual(c.buchi('BTC', h(10), h(11)), []);
});

test('esempio discusso: buco di 5 minuti fra due sessioni precedenti', () => {
    const c = nuovaCoperture();
    c.aggiungiIntervallo('USDC', h(12), h(16));
    c.aggiungiIntervallo('USDC', h(16, 5), h(22));

    const buchi = c.buchi('USDC', h(14), h(20));
    assert.equal(buchi.length, 1);
    assert.equal(buchi[0].start, h(16) + 1);
    assert.equal(buchi[0].end, h(16, 5) - 1);
});

test('riempire il buco fonde le tre coperture in una sola, senza frammentazione', () => {
    const c = nuovaCoperture();
    c.aggiungiIntervallo('USDC', h(12), h(16));
    c.aggiungiIntervallo('USDC', h(16, 5), h(22));

    const [buco] = c.buchi('USDC', h(14), h(20));
    c.aggiungiIntervallo('USDC', buco.start, buco.end);

    assert.deepEqual(c.intervalli('USDC'), [{ start_ts: h(12), end_ts: h(22) }]);
    assert.equal(c.completa('USDC', h(14), h(20)), true);
});

test('buco solo all\'inizio della finestra', () => {
    const c = nuovaCoperture();
    c.aggiungiIntervallo('ETH', h(11), h(13));
    const buchi = c.buchi('ETH', h(10), h(12));
    assert.deepEqual(buchi, [{ start: h(10), end: h(11) - 1 }]);
});

test('buco solo alla fine della finestra', () => {
    const c = nuovaCoperture();
    c.aggiungiIntervallo('ETH', h(9), h(11));
    const buchi = c.buchi('ETH', h(10), h(12));
    assert.deepEqual(buchi, [{ start: h(11) + 1, end: h(12) }]);
});

test('più buchi separati nella stessa finestra', () => {
    const c = nuovaCoperture();
    c.aggiungiIntervallo('SOL', h(10), h(11));
    c.aggiungiIntervallo('SOL', h(13), h(14));
    const buchi = c.buchi('SOL', h(9), h(15));
    assert.deepEqual(buchi, [
        { start: h(9), end: h(10) - 1 },
        { start: h(11) + 1, end: h(13) - 1 },
        { start: h(14) + 1, end: h(15) },
    ]);
});

test('due intervalli che si toccano esattamente vengono fusi in uno', () => {
    const c = nuovaCoperture();
    c.aggiungiIntervallo('SOL', h(10), h(11));
    c.aggiungiIntervallo('SOL', h(11) + 1, h(12));
    assert.deepEqual(c.intervalli('SOL'), [{ start_ts: h(10), end_ts: h(12) }]);
});

test('intervalli sovrapposti vengono fusi', () => {
    const c = nuovaCoperture();
    c.aggiungiIntervallo('SOL', h(10), h(12));
    c.aggiungiIntervallo('SOL', h(11), h(13));
    assert.deepEqual(c.intervalli('SOL'), [{ start_ts: h(10), end_ts: h(13) }]);
});

test('monete diverse restano indipendenti', () => {
    const c = nuovaCoperture();
    c.aggiungiIntervallo('BTC', h(10), h(11));
    assert.deepEqual(c.buchi('ETH', h(10), h(11)), [{ start: h(10), end: h(11) }]);
});

test('end < start viene rifiutato', () => {
    const c = nuovaCoperture();
    assert.throws(() => c.buchi('BTC', h(11), h(10)), RangeError);
    assert.throws(() => c.aggiungiIntervallo('BTC', h(11), h(10)), RangeError);
});
