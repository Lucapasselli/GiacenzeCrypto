'use strict';

// Comando manuale (mai schedulato, mai esposto via HTTP): controlla i prezzi di una moneta in un
// periodo, individua i buchi nelle coperture e scarica solo quelli — stesso meccanismo del
// riempimento "alla richiesta" (stessa `cercaPrezziStorici`, stesso allineamento al giorno
// intero), ma senza passare dalla finestra ±10 minuti né dal semaforo/lucchetto dell'endpoint,
// che servono solo ad arbitrare fra richieste HTTP concorrenti — qui c'è un solo processo che
// scarica in sequenza. Vedi Analisi_VPS_Prezzi_Sito.md e API_ServizioPrezzi.md.
//
// Uso:
//   node strumenti/manutenzioneBuchi.js --symbol=BTC --since=2024-01-01 --until=2024-01-31
//   node strumenti/manutenzioneBuchi.js --since=2026-07-01 --until=2026-08-01   (tutte le monete abilitate)
//   node strumenti/manutenzioneBuchi.js --symbol=BTC --dry-run                  (solo elenca i buchi)
// Senza --since/--until: ultimi 30 giorni.

const ccxt = require('ccxt');
const { apriDb } = require('../src/db');
const { Coperture } = require('../src/coperture');
const { leggiConfigurazione, monetaAbilitata } = require('../src/configurazione');
const { salvaPunti, allineaGiornoInizio, allineaGiornoFine } = require('../src/prezzoService');
const { cercaPrezziStorici } = require('../../Scripts/Historical_Multi_Eur.js');

const PAUSA_FRA_SCARICAMENTI_MS = 1000;

function analizzaArgomenti(argv) {
    const params = {};
    for (const arg of argv) {
        if (!arg.startsWith('--')) continue;
        const [chiave, valore] = arg.slice(2).split('=');
        params[chiave] = valore === undefined ? true : valore;
    }
    return params;
}

function analizzaData(valore, etichetta) {
    if (/^\d+$/.test(valore)) return Number(valore); // già epoch ms
    const ts = Date.parse(valore);
    if (Number.isNaN(ts)) throw new Error(`${etichetta} non valido: "${valore}" (usa AAAA-MM-GG o epoch in ms)`);
    return ts;
}

function attendi(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

async function elaboraMoneta({ db, coperture, cfg, symbol, since, until, dryRun, ccxtLib, cercaPrezziStoriciFn, pausaMs = PAUSA_FRA_SCARICAMENTI_MS }) {
    const buchi = coperture.buchi(symbol, since, until);
    if (buchi.length === 0) {
        console.log(`${symbol}: nessun buco, già tutto coperto nel periodo richiesto.`);
        return;
    }

    console.log(`${symbol}: ${buchi.length} buco/i nel periodo richiesto.`);
    for (const buco of buchi) {
        const daScaricareStart = allineaGiornoInizio(buco.start);
        const daScaricareEnd = allineaGiornoFine(buco.end);
        const etichetta = `${new Date(daScaricareStart).toISOString()} -> ${new Date(daScaricareEnd).toISOString()}`;

        if (dryRun) {
            console.log(`  [dry-run] scaricherei ${etichetta}`);
            continue;
        }

        console.log(`  scarico ${etichetta} ...`);
        const punti = await cercaPrezziStoriciFn({
            ccxtLib,
            exchangeIds: cfg.exchange,
            baseSymbol: symbol,
            timeframe: '1m',
            since: daScaricareStart,
            until: daScaricareEnd,
        });
        salvaPunti(db, symbol, punti);
        coperture.aggiungiIntervallo(symbol, daScaricareStart, daScaricareEnd);
        console.log(`  -> ${punti.length} punti salvati.`);
        await attendi(pausaMs);
    }
}

async function main() {
    const params = analizzaArgomenti(process.argv.slice(2));
    const adesso = Date.now();
    const since = params.since ? analizzaData(params.since, '--since') : adesso - 30 * 24 * 60 * 60 * 1000;
    const until = params.until ? analizzaData(params.until, '--until') : adesso;
    const dryRun = Boolean(params['dry-run']);

    if (until < since) {
        console.error('--until precede --since.');
        process.exitCode = 1;
        return;
    }
    if (until > adesso) {
        console.error('--until non può essere nel futuro.');
        process.exitCode = 1;
        return;
    }

    const cfg = leggiConfigurazione();
    let simboli;
    if (params.symbol) {
        const symbol = params.symbol.toUpperCase();
        if (!monetaAbilitata(cfg, symbol)) {
            console.error(`"${symbol}" non è fra le monete abilitate (config/monete.json): ${cfg.coinAbilitate.join(', ')}`);
            process.exitCode = 1;
            return;
        }
        simboli = [symbol];
    } else {
        simboli = cfg.coinAbilitate;
    }

    console.log(
        `Periodo: ${new Date(since).toISOString()} -> ${new Date(until).toISOString()}`,
        dryRun ? '(dry-run: solo elenco, nessuno scaricamento)' : ''
    );
    console.log(`Monete: ${simboli.join(', ')}`);
    console.log('');

    const db = apriDb();
    const coperture = new Coperture(db);

    for (const symbol of simboli) {
        await elaboraMoneta({
            db, coperture, cfg, symbol, since, until, dryRun,
            ccxtLib: ccxt,
            cercaPrezziStoriciFn: cercaPrezziStorici,
        });
    }

    db.close();
}

if (require.main === module) {
    main().catch((err) => {
        console.error('Errore:', err.message);
        process.exitCode = 1;
    });
}

module.exports = { analizzaArgomenti, analizzaData, elaboraMoneta };
