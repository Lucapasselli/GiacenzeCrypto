'use strict';

const path = require('node:path');
const fs = require('node:fs');
const { DatabaseSync } = require('node:sqlite');

const PERCORSO_DEFAULT = path.join(__dirname, '..', 'data', 'prezzi.db');

/**
 * Apre (creando se serve) il database SQLite del servizio e assicura lo schema.
 * `node:sqlite` è ancora sperimentale in Node 24, ma evita una dipendenza nativa
 * compilata (better-sqlite3) sulla VPS — stessa filosofia "poche dipendenze" già
 * seguita per il client (vedi CLAUDE.md, CcxtInterop).
 */
function apriDb(percorso) {
    const file = percorso || process.env.PREZZI_DB_PATH || PERCORSO_DEFAULT;
    if (file !== ':memory:') {
        fs.mkdirSync(path.dirname(file), { recursive: true });
    }
    const db = new DatabaseSync(file);
    if (file !== ':memory:') {
        // WAL invece del rollback journal di default: da quando esiste anche lo strumento di
        // manutenzione (strumenti/manutenzioneBuchi.js) può scrivere sullo stesso file mentre il
        // servizio è in esecuzione — WAL tollera un scrittore + lettori concorrenti molto meglio.
        db.exec('PRAGMA journal_mode = WAL');
    }
    db.exec(`
        CREATE TABLE IF NOT EXISTS PrezziCoperture (
            symbol TEXT NOT NULL,
            start_ts INTEGER NOT NULL,
            end_ts INTEGER NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_coperture_symbol ON PrezziCoperture (symbol, start_ts);

        CREATE TABLE IF NOT EXISTS Prezzi (
            symbol TEXT NOT NULL,
            timestamp INTEGER NOT NULL,
            exchange TEXT NOT NULL,
            prezzo REAL NOT NULL,
            PRIMARY KEY (symbol, timestamp, exchange)
        );
        CREATE INDEX IF NOT EXISTS idx_prezzi_symbol_ts ON Prezzi (symbol, timestamp);
    `);
    return db;
}

/** `node:sqlite` non ha un helper `.transaction()` come better-sqlite3: BEGIN/COMMIT/ROLLBACK a mano. */
function transazione(db, fn) {
    db.exec('BEGIN');
    try {
        const risultato = fn();
        db.exec('COMMIT');
        return risultato;
    } catch (ex) {
        db.exec('ROLLBACK');
        throw ex;
    }
}

module.exports = { apriDb, transazione, PERCORSO_DEFAULT };
