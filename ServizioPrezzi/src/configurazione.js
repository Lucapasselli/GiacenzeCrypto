'use strict';

const fs = require('node:fs');
const path = require('node:path');

const PERCORSO_DEFAULT = path.join(__dirname, '..', 'config', 'monete.json');

/**
 * Legge `config/monete.json` a ogni chiamata (nessuna cache): un file di configurazione va
 * modificato a mano e avere effetto subito, senza un riavvio — stesso principio già seguito per
 * le mappe di configurazione lato app (`MappeCausali`, vedi CLAUDE.md).
 */
function leggiConfigurazione(percorso = process.env.PREZZI_CONFIG_PATH || PERCORSO_DEFAULT) {
    const grezzo = fs.readFileSync(percorso, 'utf8');
    const cfg = JSON.parse(grezzo);
    if (!Array.isArray(cfg.coinAbilitate) || cfg.coinAbilitate.length === 0) {
        throw new Error(`${percorso}: "coinAbilitate" mancante o vuoto`);
    }
    if (!Array.isArray(cfg.exchange) || cfg.exchange.length === 0) {
        throw new Error(`${percorso}: "exchange" mancante o vuoto`);
    }
    if (!Number.isInteger(cfg.semaforoConcorrenza) || cfg.semaforoConcorrenza < 1) {
        throw new Error(`${percorso}: "semaforoConcorrenza" mancante o non valido`);
    }
    return cfg;
}

function monetaAbilitata(cfg, symbol) {
    return cfg.coinAbilitate.some((c) => c.toUpperCase() === symbol.toUpperCase());
}

module.exports = { leggiConfigurazione, monetaAbilitata, PERCORSO_DEFAULT };
