'use strict';

const express = require('express');
const { gestisciRichiesta } = require('../src/prezzoService');
const { leggiConfigurazione, monetaAbilitata } = require('../src/configurazione');

/**
 * Endpoint `/v1/prezzo`. Le dipendenze sono passate esplicitamente (non lette da variabili
 * globali del modulo) così i test possono iniettare un `cercaPrezziStorici` finto, senza toccare
 * la rete o richiedere `ccxt`.
 */
function creaRoutePrezzo({ db, coperture, semaforo, lucchetti, ccxtLib, cercaPrezziStorici }) {
    const router = express.Router();

    router.get('/prezzo', async (req, res) => {
        const symbol = (req.query.symbol || '').toString().toUpperCase();
        const timestamp = Number(req.query.timestamp);

        if (!symbol) {
            res.status(400).json({ errore: 'parametro "symbol" mancante' });
            return;
        }
        if (!Number.isInteger(timestamp) || timestamp <= 0) {
            res.status(400).json({ errore: 'parametro "timestamp" mancante o non valido' });
            return;
        }
        if (timestamp > Date.now()) {
            res.status(400).json({ errore: 'timestamp nel futuro' });
            return;
        }

        let cfg;
        try {
            cfg = leggiConfigurazione();
        } catch {
            res.status(500).json({ errore: 'configurazione del servizio non valida' });
            return;
        }

        if (!monetaAbilitata(cfg, symbol)) {
            res.status(400).json({ errore: `moneta "${symbol}" non gestita da questo servizio` });
            return;
        }

        const risultato = await gestisciRichiesta({
            db,
            coperture,
            semaforo,
            lucchetti,
            cercaPrezziStorici,
            ccxtLib,
            exchangeIds: cfg.exchange,
            symbol,
            timestamp,
        });

        res.status(risultato.esito === 'occupato' ? 503 : 200).json(risultato);
    });

    return router;
}

module.exports = { creaRoutePrezzo };
