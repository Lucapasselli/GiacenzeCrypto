// Scarica i movimenti fiat di Binance: ORDERS (depositi/prelievi fiat, sapi fiat/orders) e PAYMENTS
// (acquisti/vendite di crypto in fiat, sapi fiat/payments).
// Output su stdout: { "Binance_fiat": { "orders": [...], "payments": [...] } }
// Ritmo, ripetizioni e uscita con errore: vedi Binance_Comune.js.
const { creaExchange, sincronizzaOrario, chiama, esciConErrore, logInfo } = require('./Binance_Comune.js');

// ORDERS: 0 = depositi, 1 = prelievi. PAYMENTS: 0 = acquisti, 1 = vendite.
const FIAT_ORDER_TYPES = [0, 1];
const FIAT_PAYMENT_TYPES = [0, 1];
const RIGHE = 100;
const PAGINE_MASSIME = 1000;

/**
 * Tutte le pagine di un endpoint fiat per un tipo di transazione. Questi endpoint non hanno un intervallo
 * massimo: una richiesta copre l'intero periodo.
 */
async function scaricaFiat(exchange, endpoint, startTime, endTime, transactionType, etichetta) {
    const results = [];
    for (let page = 1; page <= PAGINE_MASSIME; page++) {
        const resp = await chiama(exchange, endpoint, {
            transactionType,
            beginTime: startTime,
            endTime,
            page,
            rows: RIGHE
        });
        const data = resp?.data || [];
        results.push(...data.map(d => ({ ...d, movimento: etichetta })));
        if (data.length < RIGHE) break;
    }
    return results;
}

async function main() {
    const [, , exchangeId, apiKey, secret, startDate] = process.argv;
    const startTime = Number(startDate);
    const endTime = Date.now();
    const exchange = creaExchange(exchangeId, apiKey, secret);
    await sincronizzaOrario(exchange);

    // PAYMENTS prima di ORDERS: fiat/orders pesa 90000 (ccxt: 600 volte una chiamata normale) e il
    // limitatore fa attendere anche la chiamata che segue. Messe in coda, l'attesa e' una sola.
    const payments = [];
    for (const type of FIAT_PAYMENT_TYPES) {
        const data = await scaricaFiat(exchange, 'fiat/payments', startTime, endTime, type, type === 0 ? 'acquisto' : 'vendita');
        payments.push(...data);
        logInfo(`PAYMENTS type=${type}: ${data.length}`);
    }
    const orders = [];
    for (const type of FIAT_ORDER_TYPES) {
        const data = await scaricaFiat(exchange, 'fiat/orders', startTime, endTime, type, type === 0 ? 'deposito' : 'prelievo');
        orders.push(...data);
        logInfo(`ORDERS type=${type}: ${data.length}`);
    }
    logInfo(`Recupero completato. Totale ORDERS: ${orders.length}, PAYMENTS: ${payments.length}`);
    console.log(JSON.stringify({ Binance_fiat: { orders, payments } }, null, 2));
}

main().catch(esciConErrore);
