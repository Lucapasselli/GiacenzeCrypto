'use strict';

const { transazione } = require('./db');

const FINESTRA_MS = 10 * 60 * 1000; // ±10 minuti, vedi Analisi_VPS_Prezzi_Sito.md
// Allineamento al giorno intero (UTC, semplice matematica sull'epoch — nessuna conversione di
// fuso, come il resto del servizio) invece che all'ora: misurato empiricamente (BTC, tutti gli
// exchange configurati) prima di scegliere. Caso peggiore, richiesta a cavallo di mezzanotte,
// scarica 2 giorni in ~6,7s; oltre le 48-96h il costo sale in modo marcato per gli exchange
// fermi a 300 candele/chiamata (cryptocom/okx/coinbase). Vedi Analisi_VPS_Prezzi_Sito.md.
const GIORNO_MS = 24 * 60 * 60 * 1000;

function allineaGiornoInizio(ts) {
    return Math.floor(ts / GIORNO_MS) * GIORNO_MS;
}

function allineaGiornoFine(ts) {
    return Math.ceil((ts + 1) / GIORNO_MS) * GIORNO_MS - 1;
}

/** Legge i punti prezzo già salvati per [start, end], nella stessa forma restituita da cercaPrezziStorici. */
function leggiPunti(db, symbol, start, end) {
    const righe = db
        .prepare(
            'SELECT timestamp, exchange, prezzo FROM Prezzi WHERE symbol = ? AND timestamp BETWEEN ? AND ? ORDER BY timestamp'
        )
        .all(symbol, start, end);

    const perTimestamp = new Map();
    for (const r of righe) {
        if (!perTimestamp.has(r.timestamp)) perTimestamp.set(r.timestamp, {});
        perTimestamp.get(r.timestamp)[r.exchange] = r.prezzo;
    }
    return [...perTimestamp.entries()]
        .sort((a, b) => a[0] - b[0])
        .map(([timestamp, prices]) => ({ timestamp, prices }));
}

/** Salva i punti restituiti da cercaPrezziStorici. Idempotente: righe già presenti vengono sovrascritte. */
function salvaPunti(db, symbol, punti) {
    const stmt = db.prepare(
        'INSERT OR REPLACE INTO Prezzi (symbol, timestamp, exchange, prezzo) VALUES (?, ?, ?, ?)'
    );
    transazione(db, () => {
        for (const { timestamp, prices } of punti) {
            for (const [exchange, prezzo] of Object.entries(prices)) {
                if (typeof prezzo === 'number' && Number.isFinite(prezzo)) {
                    stmt.run(symbol, timestamp, exchange, prezzo);
                }
            }
        }
    });
}

/**
 * Gestisce una richiesta (symbol, timestamp): risolve dalla cache se la finestra ±10 minuti è già
 * interamente coperta, altrimenti prova a riempire i buchi scaricandoli dal vivo — ma solo se il
 * semaforo di concorrenza e il lucchetto per moneta lo permettono, altrimenti risponde subito
 * "occupato" senza scaricare nulla e senza toccare le coperture (vedi Analisi_VPS_Prezzi_Sito.md,
 * "sesta parte": una risposta parziale silenziosa farebbe segnare come coperto un range con un
 * buco dentro).
 *
 * @returns {{esito: 'risolto', punti: object[]} | {esito: 'occupato'}}
 */
async function gestisciRichiesta({ db, coperture, semaforo, lucchetti, cercaPrezziStorici, ccxtLib, exchangeIds, symbol, timestamp }) {
    const start = timestamp - FINESTRA_MS;
    const end = timestamp + FINESTRA_MS;

    if (coperture.completa(symbol, start, end)) {
        return { esito: 'risolto', punti: leggiPunti(db, symbol, start, end) };
    }

    if (lucchetti.has(symbol) || !semaforo.disponibile()) {
        return { esito: 'occupato' };
    }

    lucchetti.add(symbol);
    semaforo.acquisisci();
    try {
        const buchi = coperture.buchi(symbol, start, end);
        const daScaricareStart = allineaGiornoInizio(Math.min(...buchi.map((b) => b.start)));
        const daScaricareEnd = allineaGiornoFine(Math.max(...buchi.map((b) => b.end)));

        const punti = await cercaPrezziStorici({
            ccxtLib,
            exchangeIds,
            baseSymbol: symbol,
            timeframe: '1m',
            since: daScaricareStart,
            until: daScaricareEnd,
        });

        salvaPunti(db, symbol, punti);
        coperture.aggiungiIntervallo(symbol, daScaricareStart, daScaricareEnd);

        return { esito: 'risolto', punti: leggiPunti(db, symbol, start, end) };
    } finally {
        lucchetti.delete(symbol);
        semaforo.rilascia();
    }
}

module.exports = { gestisciRichiesta, leggiPunti, salvaPunti, allineaGiornoInizio, allineaGiornoFine, FINESTRA_MS };
