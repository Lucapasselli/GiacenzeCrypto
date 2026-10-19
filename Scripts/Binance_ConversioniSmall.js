// Scarica le conversioni dei piccoli saldi in BNB ("Small Assets Exchange", sapi asset/dribblet).
// Output su stdout: { "Binance_smallAssetConversions": [ ...un record per moneta convertita... ] }
// Ritmo, ripetizioni e uscita con errore: vedi Binance_Comune.js.
//
// Prima lo script caricava anche tutti i mercati (spot, margin, futures) per stimare con le candele la moneta
// di arrivo: da tempo Binance la dichiara nella risposta, e quel caricamento era rimasto come costo puro.
const { creaExchange, sincronizzaOrario, chiama, esciConErrore, logInfo } = require('./Binance_Comune.js');

const GIORNI_FINESTRA = 28;
const GIORNO_MS = 24 * 60 * 60 * 1000;
// Binance restituisce al massimo le ultime 100 conversioni dell'intervallo: a quel numero si divide.
const MASSIMO_RISPOSTA = 100;

function appiattisci(dribblets) {
    const records = [];
    for (const dribblet of dribblets) {
        if (!dribblet.userAssetDribbletDetails) continue;
        for (const detail of dribblet.userAssetDribbletDetails) {
            records.push({
                ...detail,
                toAsset: detail.targetAsset || detail.toAsset || 'UNKNOWN',
                conversionMatchConfidence: "1.00", // moneta di arrivo dichiarata da Binance
                totalServiceChargeAmount: dribblet.totalServiceChargeAmount,
                totalTransferedAmount: dribblet.totalTransferedAmount,
                transId: dribblet.transId
            });
        }
    }
    return records;
}

async function conversioniIntervallo(exchange, startTime, endTime) {
    const resp = await chiama(exchange, 'asset/dribblet', { startTime, endTime });
    const dribblets = resp?.userAssetDribblets || [];
    if (dribblets.length >= MASSIMO_RISPOSTA && endTime - startTime > 1000) {
        const meta = Math.floor((startTime + endTime) / 2);
        return [
            ...await conversioniIntervallo(exchange, startTime, meta),
            ...await conversioniIntervallo(exchange, meta + 1, endTime)
        ];
    }
    return appiattisci(dribblets);
}

async function main() {
    const [, , exchangeId, apiKey, secret, startDate, assetArrayStr = ''] = process.argv;
    const startTime = Number(startDate);
    const endTime = Date.now();
    const assetArray = assetArrayStr.split(',').map(s => s.trim()).filter(Boolean);
    const exchange = creaExchange(exchangeId, apiKey, secret);
    await sincronizzaOrario(exchange);

    const allConversions = [];
    for (let inizio = startTime; inizio < endTime; ) {
        const fine = Math.min(inizio + GIORNI_FINESTRA * GIORNO_MS, endTime);
        const records = await conversioniIntervallo(exchange, inizio, fine);
        allConversions.push(...(assetArray.length > 0 ? records.filter(r => assetArray.includes(r.fromAsset)) : records));
        inizio = fine + 1;
    }
    logInfo(`Recupero completato. Totale conversioni: ${allConversions.length}`);
    console.log(JSON.stringify({ Binance_smallAssetConversions: allConversions }, null, 2));
}

main().catch(esciConErrore);
