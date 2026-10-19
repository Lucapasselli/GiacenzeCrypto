// Scarica le conversioni fatte con Binance Convert (sapi convert/tradeFlow).
// Output su stdout: { "Binance_Convert": [ ...righe grezze di Binance... ] }
// Ritmo, ripetizioni e uscita con errore: vedi Binance_Comune.js.
const { creaExchange, sincronizzaOrario, chiama, esciConErrore, logInfo } = require('./Binance_Comune.js');

const GIORNI_MASSIMI = 30;   // intervallo massimo accettato da Binance
const LIMITE = 1000;         // righe massime per richiesta
const GIORNO_MS = 24 * 60 * 60 * 1000;

/**
 * Conversioni di un intervallo. L'endpoint non ha pagine (niente current/size): se l'intervallo contiene
 * piu' di LIMITE righe Binance lo dice con moreData, e l'intervallo si divide a meta'.
 */
async function conversioniIntervallo(exchange, startTime, endTime) {
    const resp = await chiama(exchange, 'convert/tradeFlow', { startTime, endTime, limit: LIMITE });
    const records = resp?.list || [];
    if ((resp?.moreData === true || records.length >= LIMITE) && endTime - startTime > 1000) {
        const meta = Math.floor((startTime + endTime) / 2);
        return [
            ...await conversioniIntervallo(exchange, startTime, meta),
            ...await conversioniIntervallo(exchange, meta + 1, endTime)
        ];
    }
    return records;
}

async function main() {
    const [, , exchangeId, apiKey, secret, startDate] = process.argv;
    const startTime = Number(startDate);
    const endTime = Date.now();
    const exchange = creaExchange(exchangeId, apiKey, secret);
    await sincronizzaOrario(exchange);

    const allConversions = [];
    for (let inizio = startTime; inizio < endTime; ) {
        const fine = Math.min(inizio + GIORNI_MASSIMI * GIORNO_MS, endTime);
        allConversions.push(...await conversioniIntervallo(exchange, inizio, fine));
        inizio = fine + 1;
    }
    logInfo(`Recupero completato. Totale conversioni: ${allConversions.length}`);
    console.log(JSON.stringify({ Binance_Convert: allConversions }, null, 2));
}

main().catch(esciConErrore);
