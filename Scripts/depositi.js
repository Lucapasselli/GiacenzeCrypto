// Scarica i depositi crypto di Binance (sapi capital/deposit/hisrec).
// Output su stdout: { "deposits": [ ...righe grezze di Binance... ] }
// Ritmo, ripetizioni e uscita con errore: vedi Binance_Comune.js.
const { creaExchange, sincronizzaOrario, chiama, esciConErrore, logInfo } = require('./Binance_Comune.js');

const EPOCH_ALL_HISTORY = new Date('2017-01-01T00:00:00Z').getTime();
const FINESTRA_MS = 90 * 24 * 60 * 60 * 1000; // intervallo massimo accettato da Binance
const LIMITE = 1000;                          // righe massime per pagina

async function fetchAllDepositsRaw(exchange, globalStartTime, globalEndTime) {
  const out = [];
  const seenIds = new Set();
  let currentStart = Math.max(globalStartTime, EPOCH_ALL_HISTORY);
  let windowIdx = 0;
  const totalWindows = Math.ceil((globalEndTime - currentStart) / FINESTRA_MS);

  while (currentStart < globalEndTime) {
    windowIdx++;
    const currentEnd = Math.min(currentStart + FINESTRA_MS, globalEndTime);
    logInfo(`Depositi finestra ${windowIdx}/${totalWindows} (${new Date(currentStart).toISOString()} -> ${new Date(currentEnd).toISOString()})`);
    // Le pagine si scorrono con offset: senza, la seconda richiesta restituirebbe di nuovo la prima.
    for (let offset = 0; ; offset += LIMITE) {
      const pageData = await chiama(exchange, 'capital/deposit/hisrec', {
        limit: LIMITE,
        offset,
        startTime: currentStart,
        endTime: currentEnd
      });
      if (!Array.isArray(pageData)) throw new Error('risposta inattesa da capital/deposit/hisrec');
      for (const d of pageData) {
        const id = d.id || d.txId || d.insertTime;
        if (seenIds.has(id)) continue;
        seenIds.add(id);
        out.push(d);
      }
      if (pageData.length < LIMITE) break;
    }
    currentStart = currentEnd + 1;
  }
  const filteredOut = out.filter(item => item.insertTime >= globalStartTime && item.insertTime <= globalEndTime);
  logInfo(`Depositi totali: ${filteredOut.length}`);
  return filteredOut;
}

async function main() {
  const [,, exchangeId, apiKey, secret, startDateArg = "1483228800000"] = process.argv;
  const startTime = Math.max(Number(startDateArg), EPOCH_ALL_HISTORY);
  const endTime = Date.now();
  const exchange = creaExchange(exchangeId, apiKey, secret);
  await sincronizzaOrario(exchange);
  const deposits = await fetchAllDepositsRaw(exchange, startTime, endTime);
  console.log(JSON.stringify({ deposits }));
}

main().catch(esciConErrore);
