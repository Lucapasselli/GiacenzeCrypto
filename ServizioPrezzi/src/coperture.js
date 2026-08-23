'use strict';

const { transazione } = require('./db');

/**
 * Tiene traccia di quali finestre temporali sono già state interrogate per intero sugli exchange,
 * per moneta — porto persistito dell'algoritmo di overlap/merge già scritto e testato lato client
 * in `Prezzi.RangeRequestManager` (Prezzi.java:3633-3708). Gli estremi sono trattati come inclusivi
 * in millisecondi, quindi due intervalli che si toccano esattamente (fine di uno = inizio-1 del
 * successivo) vengono fusi in uno solo — altrimenti la tabella si frammenterebbe ogni volta che si
 * riempie un buco adiacente a coperture già esistenti.
 */
class Coperture {
    constructor(db) {
        this.db = db;
        this._stmtIntervalli = db.prepare(
            'SELECT start_ts, end_ts FROM PrezziCoperture WHERE symbol = ? ORDER BY start_ts'
        );
        this._stmtCancella = db.prepare('DELETE FROM PrezziCoperture WHERE symbol = ?');
        this._stmtInserisci = db.prepare(
            'INSERT INTO PrezziCoperture (symbol, start_ts, end_ts) VALUES (?, ?, ?)'
        );
    }

    /** @returns {{start_ts: number, end_ts: number}[]} intervalli coperti per la moneta, ordinati */
    intervalli(symbol) {
        // node:sqlite restituisce oggetti a prototipo nullo: normalizzati qui in oggetti semplici,
        // così i chiamanti (e i test, con deepStrictEqual) non dipendono da un dettaglio del driver.
        return this._stmtIntervalli.all(symbol).map((r) => ({ start_ts: r.start_ts, end_ts: r.end_ts }));
    }

    /**
     * @returns {{start: number, end: number}[]} i sotto-intervalli di [start, end] non ancora
     * coperti, in ordine. Vuoto se la finestra è già interamente coperta.
     */
    buchi(symbol, start, end) {
        if (end < start) throw new RangeError('end < start');
        const risultato = [];
        let cursore = start;
        for (const { start_ts: startTs, end_ts: endTs } of this.intervalli(symbol)) {
            if (endTs < start) continue;
            if (startTs > end) break;
            if (startTs > cursore) {
                risultato.push({ start: cursore, end: Math.min(startTs - 1, end) });
            }
            cursore = Math.max(cursore, endTs + 1);
            if (cursore > end) break;
        }
        if (cursore <= end) risultato.push({ start: cursore, end });
        return risultato;
    }

    /** @returns {boolean} true se [start, end] è già interamente coperto */
    completa(symbol, start, end) {
        return this.buchi(symbol, start, end).length === 0;
    }

    /** Registra [start, end] come scaricato con successo, fondendolo con le coperture adiacenti/sovrapposte. */
    aggiungiIntervallo(symbol, start, end) {
        if (end < start) throw new RangeError('end < start');
        let nuovoStart = start;
        let nuovoEnd = end;
        const daTenere = [];
        for (const r of this.intervalli(symbol)) {
            if (r.end_ts < nuovoStart - 1 || r.start_ts > nuovoEnd + 1) {
                daTenere.push(r);
            } else {
                nuovoStart = Math.min(nuovoStart, r.start_ts);
                nuovoEnd = Math.max(nuovoEnd, r.end_ts);
            }
        }
        transazione(this.db, () => {
            this._stmtCancella.run(symbol);
            for (const r of daTenere) {
                this._stmtInserisci.run(symbol, r.start_ts, r.end_ts);
            }
            this._stmtInserisci.run(symbol, nuovoStart, nuovoEnd);
        });
    }
}

module.exports = { Coperture };
