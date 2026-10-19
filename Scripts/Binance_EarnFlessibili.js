// Scarica i premi Simple Earn flessibile di Binance (sapi simple-earn/flexible/history/rewardsRecord),
// di tutti i tipi insieme (BONUS, REALTIME, REWARDS: type=ALL).
// Output su stdout: { "Binance_earnFlexible": [ ...righe grezze di Binance... ] }
// Ritmo, ripetizioni e uscita con errore: vedi Binance_Comune.js.
const { creaExchange, sincronizzaOrario, chiama, esciConErrore, logInfo } = require('./Binance_Comune.js');

const GIORNI_FINESTRA = 30;
const GIORNO_MS = 24 * 60 * 60 * 1000;
const RIGHE = 100;           // righe massime per pagina
const PAGINE_MASSIME = 1000;

async function premiIntervallo(exchange, startTime, endTime) {
    const risultato = [];
    for (let current = 1; current <= PAGINE_MASSIME; current++) {
        const resp = await chiama(exchange, 'simple-earn/flexible/history/rewardsRecord',
            { type: 'ALL', startTime, endTime, current, size: RIGHE });
        const records = resp?.rows || [];
        risultato.push(...records);
        if (records.length < RIGHE) break;
    }
    return risultato;
}

async function main() {
    const [, , exchangeId, apiKey, secret, startDate] = process.argv;
    const startTime = Number(startDate);
    const endTime = Date.now();
    const exchange = creaExchange(exchangeId, apiKey, secret);
    await sincronizzaOrario(exchange);

    const allRewards = [];
    for (let inizio = startTime; inizio < endTime; ) {
        const fine = Math.min(inizio + GIORNI_FINESTRA * GIORNO_MS, endTime);
        allRewards.push(...await premiIntervallo(exchange, inizio, fine));
        inizio = fine + 1;
    }
    logInfo(`Recupero completato. Totale rewards: ${allRewards.length}`);
    console.log(JSON.stringify({ Binance_earnFlexible: allRewards }, null, 2));
}

main().catch(esciConErrore);
