// npm i ccxt@latest
//
// Scarica i "bills" (movimenti contabili) di OKX, che sono l'equivalente API dell'export CSV
// gestito da Importazioni.Ex_OKX_Importa: ogni riga e' una variazione di saldo su una moneta.
//
// Vengono interrogati i due conti separatamente, esattamente come i due CSV Funding e Trading:
//   - Funding account -> /api/v5/asset/bills          (depositi, prelievi, trasferimenti, earn, reward)
//   - Trading account -> /api/v5/account/bills-archive (trade, conversioni, fee)
//
// ATTENZIONE: entrambi gli endpoint coprono solo gli ultimi 3 MESI. Per lo storico piu' vecchio
// resta necessario l'import del CSV. Se startDate e' piu' vecchia viene silenziosamente limitata.
//
// argv: exchangeId apiKey secret startDate(ms) tokens passphrase
// La passphrase e' in ULTIMA posizione perche' tutti gli altri script destrutturano argv
// per posizione e non devono essere toccati.

const ccxt = require('ccxt');

// ======================= Costanti & Utils =======================

// Gli endpoint bills di OKX coprono al massimo 3 mesi. Si tiene un margine di qualche giorno
// per non farsi rifiutare la richiesta al limite esatto della finestra.
const FINESTRA_MAX_MS = 88 * 24 * 60 * 60 * 1000;
const LIMIT = 100;        // massimo consentito da OKX per pagina
const MAX_PAGINE = 500;   // 50.000 movimenti: ben oltre quanto stia in 3 mesi

function logProg(msg) { console.error(`[PROG] ${msg}`); }
function log(msg) { console.error(`[Node-LOG] ${msg}`); }

function sleep(ms) { return new Promise(resolve => setTimeout(resolve, ms)); }

/**
 * Pagina un endpoint bills di OKX andando a ritroso con il solo parametro `after`
 * (che per OKX e' un billId, NON un timestamp) e fermandosi quando la pagina torna
 * corta o quando i movimenti diventano piu' vecchi di startTime.
 *
 * Volutamente NON si usano `begin`/`end`: non e' verificato che questi endpoint li
 * onorino, e una finestra temporale ignorata renderebbe la paginazione silenziosamente
 * inutile (stessa pagina richiesta all'infinito, scartata come duplicata). Il filtro
 * temporale viene quindi applicato qui sui risultati.
 */
async function fetchBills(exchange, metodo, startTime, endTime, etichetta) {
  const out = [];
  const seenIds = new Set();
  let after;
  let pagina = 0;
  let tsMin = Number.MAX_SAFE_INTEGER;
  let tsMax = 0;
  let completo = false;

  while (pagina < MAX_PAGINE) {
    pagina++;
    const request = { limit: String(LIMIT) };
    if (after !== undefined) request.after = after;

    let risposta;
    let tentativo = 0;
    while (true) {
      try {
        risposta = await exchange[metodo](request);
        break;
      } catch (e) {
        tentativo++;
        if (tentativo >= 4) {
          log(`${etichetta} errore definitivo alla pagina ${pagina}: ${e.message}`);
          return { bills: out, completo: false };
        }
        const attesa = 3000 * tentativo;
        log(`${etichetta} errore (tentativo ${tentativo}): ${e.message} - riprovo tra ${attesa}ms`);
        await sleep(attesa);
      }
    }

    const data = (risposta && Array.isArray(risposta.data)) ? risposta.data : [];
    if (data.length === 0) { completo = true; break; }

    let nuovi = 0;
    let piuVecchio;
    let raggiuntoInizio = false;
    for (const b of data) {
      piuVecchio = b.billId;
      const ts = Number(b.ts);
      if (ts < startTime) { raggiuntoInizio = true; continue; }
      if (ts > endTime) continue;
      if (seenIds.has(b.billId)) continue;
      seenIds.add(b.billId);
      if (ts < tsMin) tsMin = ts;
      if (ts > tsMax) tsMax = ts;
      out.push(b);
      nuovi++;
    }
    logProg(`${etichetta} pagina ${pagina}: +${nuovi} nuovi, tot=${out.length}`);

    if (raggiuntoInizio) { completo = true; break; }
    if (data.length < LIMIT) { completo = true; break; }
    if (piuVecchio === undefined || piuVecchio === after) { completo = true; break; }
    after = piuVecchio;
    await sleep(300);
  }

  if (!completo) {
    log(`${etichetta}: raggiunto il limite di ${MAX_PAGINE} pagine, POSSIBILI MOVIMENTI MANCANTI`);
  }
  // Log dell'intervallo realmente recuperato: se e' piu' stretto di quello richiesto,
  // l'endpoint ha troncato lo storico e deve essere visibile.
  if (out.length > 0) {
    log(`${etichetta}: ${out.length} movimenti, dal ${new Date(tsMin).toISOString()} al ${new Date(tsMax).toISOString()} (richiesto dal ${new Date(startTime).toISOString()})`);
  } else {
    log(`${etichetta}: nessun movimento nell'intervallo richiesto`);
  }
  return { bills: out, completo };
}

// ======================= MAIN =======================

async function main() {
  const [, , exchangeId, apiKey, secret, startDateArg = "0", tokensArg = "", passphrase = ""] = process.argv;

  const endTime = Date.now();
  let startTime = Number(startDateArg);
  if (!Number.isFinite(startTime) || startTime <= 0) startTime = endTime - FINESTRA_MAX_MS;

  const limiteMin = endTime - FINESTRA_MAX_MS;
  if (startTime < limiteMin) {
    log(`Data richiesta (${new Date(startTime).toISOString()}) oltre il limite di 3 mesi degli endpoint bills di OKX: limitata a ${new Date(limiteMin).toISOString()}`);
    startTime = limiteMin;
  }

  const ExchangeClass = ccxt[exchangeId] || ccxt.okx;
  const exchange = new ExchangeClass({
    apiKey,
    secret,
    password: passphrase,   // in CCXT la passphrase di OKX si chiama "password"
    enableRateLimit: true,
    timeout: 60000
  });

  try {
    await exchange.loadTimeDifference();
    exchange.options.adjustForTimeDifference = true;
  } catch (e) { /* non bloccante */ }

  const funding = await fetchBills(exchange, 'privateGetAssetBills', startTime, endTime, 'Funding')
    .catch(e => { log(`Funding fallito: ${e.message}`); return { bills: [], completo: false }; });

  const trading = await fetchBills(exchange, 'privateGetAccountBillsArchive', startTime, endTime, 'Trading')
    .catch(e => { log(`Trading fallito: ${e.message}`); return { bills: [], completo: false }; });

  console.log(JSON.stringify({
    okx_fundingBills: funding.bills,
    okx_tradingBills: trading.bills,
    okx_completo: funding.completo && trading.completo
  }));
}

main().catch(err => {
  console.log(JSON.stringify({ okx_fundingBills: [], okx_tradingBills: [], okx_completo: false, error: err.message }));
});
