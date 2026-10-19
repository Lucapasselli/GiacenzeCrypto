// Scarica i prelievi crypto di Binance (sapi capital/withdraw/history).
// Output su stdout: { "withdrawals": [ ...righe grezze di Binance, con insertTime/completeTime in ms... ] }
// Ritmo, ripetizioni e uscita con errore: vedi Binance_Comune.js.
const { creaExchange, sincronizzaOrario, chiama, esciConErrore, logInfo } = require('./Binance_Comune.js');

const EPOCH_ALL_HISTORY = new Date('2017-01-01T00:00:00Z').getTime();
const FINESTRA_MS = 90 * 24 * 60 * 60 * 1000; // intervallo massimo accettato da Binance
const LIMITE = 1000;                          // righe massime per pagina

/** applyTime/completeTime arrivano come "YYYY-MM-DD HH:mm:ss" in UTC: vanno letti come UTC, non come ora locale. */
function normalizeBinanceTime(value) {
  if (!value) return null;
  if (!isNaN(value)) return Number(value);
  if (typeof value === 'string') {
    if (/T.*Z$/.test(value) || /[+-]\d\d:?(\d\d)?$/.test(value)) {
      return new Date(value).getTime();
    }
    const m = value.match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2}):(\d{2})(?:\.\d+)?$/);
    if (m) {
      const [, y, mo, d, h, mi, se] = m;
      return Date.UTC(+y, +mo - 1, +d, +h, +mi, +se);
    }
    return Date.parse(value);
  }
  return null;
}

async function fetchAllWithdrawalsRaw(exchange, globalStartTime, globalEndTime) {
  const out = [];
  const seenIds = new Set();
  let currentStart = Math.max(globalStartTime, EPOCH_ALL_HISTORY);
  let windowIdx = 0;
  const totalWindows = Math.ceil((globalEndTime - currentStart) / FINESTRA_MS);

  while (currentStart < globalEndTime) {
    windowIdx++;
    const currentEnd = Math.min(currentStart + FINESTRA_MS, globalEndTime);
    logInfo(`Prelievi finestra ${windowIdx}/${totalWindows} (${new Date(currentStart).toISOString()} -> ${new Date(currentEnd).toISOString()})`);
    // Le pagine si scorrono con offset: senza, la seconda richiesta restituirebbe di nuovo la prima.
    for (let offset = 0; ; offset += LIMITE) {
      const pageData = await chiama(exchange, 'capital/withdraw/history', {
        limit: LIMITE,
        offset,
        startTime: currentStart,
        endTime: currentEnd
      });
      if (!Array.isArray(pageData)) throw new Error('risposta inattesa da capital/withdraw/history');
      for (const w of pageData) {
        const id = w.id || w.txId || w.applyTime;
        if (seenIds.has(id)) continue;
        w.insertTime = normalizeBinanceTime(w.applyTime);   // stesso campo dei depositi
        w.completeTime = normalizeBinanceTime(w.completeTime);
        seenIds.add(id);
        out.push(w);
      }
      if (pageData.length < LIMITE) break;
    }
    currentStart = currentEnd + 1;
  }
  // Il filtro usa insertTime (applyTime letto come UTC). Prima usava Date.parse(applyTime), che legge
  // l'ora come locale: in Italia scartava i prelievi fatti nelle 1-2 ore successive all'ultimo movimento
  // importato, e lo scaricamento seguente, partendo da li', non li riprendeva piu'.
  const filteredOut = out.filter(item => item.insertTime >= globalStartTime && item.insertTime <= globalEndTime);
  logInfo(`Prelievi totali: ${filteredOut.length}`);
  return filteredOut;
}

async function main() {
  const [,, exchangeId, apiKey, secret, startDateArg = "1483228800000"] = process.argv;
  const startTime = Math.max(Number(startDateArg), EPOCH_ALL_HISTORY);
  const endTime = Date.now();
  const exchange = creaExchange(exchangeId, apiKey, secret);
  await sincronizzaOrario(exchange);
  const withdrawals = await fetchAllWithdrawalsRaw(exchange, startTime, endTime);
  console.log(JSON.stringify({ withdrawals }));
}

main().catch(esciConErrore);
