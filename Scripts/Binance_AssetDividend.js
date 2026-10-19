// Scarica le distribuzioni di Binance (sapi asset/assetDividend): airdrop, Launchpool, HODLer, BNB Vault,
// premi Earn (questi ultimi scartati in Java perche' arrivano gia' dagli script Earn).
// Output su stdout: { "Binance_Rewards": [ ...righe grezze di Binance... ] }
// Ritmo, ripetizioni e uscita con errore: vedi Binance_Comune.js.
const { creaExchange, sincronizzaOrario, chiama, esciConErrore, logInfo, logError } = require('./Binance_Comune.js');

const GIORNI_MASSIMI = 180;  // intervallo massimo accettato da Binance
const LIMITE = 500;          // righe massime per richiesta
const GIORNO_MS = 24 * 60 * 60 * 1000;

/**
 * Distribuzioni di un intervallo. Binance le restituisce dalla piu' recente e senza pagine: si scende
 * spostando la fine dell'intervallo. La nuova fine e' l'istante PIU' VECCHIO della pagina, incluso: le
 * distribuzioni dello stesso secondo sono spesso decine, e con "istante - 1" quelle rimaste fuori dalla
 * pagina andavano perse. Le righe gia' viste si riconoscono dall'id.
 */
async function distribuzioniIntervallo(exchange, startTime, endTime, visti) {
    const risultato = [];
    let fine = endTime;
    for (;;) {
        const resp = await chiama(exchange, 'asset/assetDividend', { startTime, endTime: fine, limit: LIMITE });
        const records = resp?.rows || [];
        let nuovi = 0;
        for (const r of records) {
            const chiave = r.id != null ? String(r.id) : `${r.tranId}|${r.asset}|${r.divTime}|${r.amount}`;
            if (visti.has(chiave)) continue;
            visti.add(chiave);
            risultato.push(r);
            nuovi++;
        }
        if (records.length < LIMITE) break;
        const piuVecchio = Math.min(...records.map(r => Number(r.divTime)));
        if (nuovi === 0) {
            // Una pagina intera tutta dello stesso secondo: non si puo' scendere senza saltarlo.
            logError(`Piu' di ${LIMITE} distribuzioni nello stesso secondo (${new Date(piuVecchio).toISOString()}): si prosegue oltre`);
            fine = piuVecchio - 1;
        } else {
            fine = piuVecchio;
        }
        if (fine <= startTime) break;
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
    const visti = new Set();
    for (let inizio = startTime; inizio < endTime; ) {
        const fine = Math.min(inizio + GIORNI_MASSIMI * GIORNO_MS, endTime);
        allRewards.push(...await distribuzioniIntervallo(exchange, inizio, fine, visti));
        inizio = fine + 1;
    }
    logInfo(`Recupero completato. Totale rewards: ${allRewards.length}`);
    console.log(JSON.stringify({ Binance_Rewards: allRewards }, null, 2));
}

main().catch(esciConErrore);
