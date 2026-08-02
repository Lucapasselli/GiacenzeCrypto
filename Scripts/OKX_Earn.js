// DIAGNOSTICO - non e' collegato all'applicazione, si lancia a mano.
//
// I rendimenti dei prodotti Earn di OKX NON compaiono ne' nei bill del conto Funding
// (/api/v5/asset/bills) ne' in quelli del conto Trading, e nemmeno negli export CSV
// "Funding History" / "Trading History": in quei file si vedono solo la sottoscrizione e il
// riscatto del capitale. Gli interessi restano dentro il prodotto Earn e hanno endpoint propri,
// sotto la sezione "Financial Product" delle API:
//
//   /api/v5/finance/savings/balance             saldo attuale dei prodotti Simple Earn Flexible
//   /api/v5/finance/savings/lending-history     storico degli interessi maturati giorno per giorno
//   /api/v5/finance/staking-defi/orders-active  posizioni aperte di On-chain Earn / staking
//   /api/v5/finance/staking-defi/orders-history storico ordini On-chain Earn, con i rendimenti
//
// Questo script li interroga tutti e quattro e stampa la risposta GREZZA, senza interpretarla:
// serve a vedere che forma hanno davvero i dati (campi, unita' di misura, profondita' storica)
// prima di decidere come importarli. Sono tutte chiamate di sola lettura.
//
// Uso, dalla cartella di lavoro (quella che contiene tools/node):
//   NODE_PATH=tools/node/node_modules tools/node/<distribuzione>/bin/node \
//       Scripts/OKX_Earn.js okx <apiKey> <secret> 0 "" <passphrase> [hostname]
//
// ATTENZIONE: le credenziali passate cosi' restano nella cronologia della shell.
//
// argv: exchangeId apiKey secret startDate(ms) tokens passphrase hostname

const ccxt = require('ccxt');

// Stessi domini regionali di OKX_Bills.js: una chiave creata su un'entita' regionale non esiste
// sulle altre. Se il dominio e' gia' noto conviene passarlo come ultimo argomento.
const HOSTNAME_CANDIDATI = ['my.okx.com', 'eea.okx.com', 'www.okx.com', 'app.okx.com'];

function log(msg) { console.error(`[Node-LOG] ${msg}`); }

async function risolviHostname(exchange, hostnamePreferito) {
  const candidati = hostnamePreferito
    ? [hostnamePreferito, ...HOSTNAME_CANDIDATI.filter(h => h !== hostnamePreferito)]
    : [...HOSTNAME_CANDIDATI];

  for (const host of candidati) {
    exchange.hostname = host;
    try {
      await exchange.privateGetAssetBills({ limit: '1' });
      log(`Dominio OKX riconosciuto: ${host}`);
      return host;
    } catch (e) {
      log(`${host}: ${e.message}`);
    }
  }
  return null;
}

async function chiama(exchange, metodo, richiesta, etichetta) {
  try {
    const risposta = await exchange[metodo](richiesta);
    const dati = (risposta && Array.isArray(risposta.data)) ? risposta.data : [];
    log(`${etichetta}: ${dati.length} record`);
    return dati;
  } catch (e) {
    log(`${etichetta}: ERRORE ${e.message}`);
    return { errore: e.message };
  }
}

// Lo storico interessi arriva a pagine da 100 e gli accrediti sono ORARI (uno per moneta per ora,
// verificato sui dati reali): una sola pagina copre poco piu' di un giorno. Serve quindi paginare
// all'indietro per sapere fin dove arriva davvero lo storico.
const MAX_PAGINE_EARN = 60;   // 6000 record ≈ 80 giorni con 3 monete: abbastanza per vedere il limite

/**
 * Pagina all'indietro lo storico degli interessi usando `after` (per OKX: restituisce i record
 * precedenti al timestamp indicato) e si ferma quando la pagina torna corta, quando il timestamp
 * non avanza piu' o al raggiungimento del tetto di pagine.
 */
async function storicoInteressi(exchange, startTime) {
  const out = [];
  //I record non hanno un id: la coppia moneta+timestamp e' l'unica chiave disponibile, e sui dati reali
  //e' univoca (100 record, 100 timestamp distinti). Serve perche' non e' verificato che questo endpoint
  //onori `after`, e una pagina ripetuta gonfierebbe in silenzio il totale giornaliero.
  const visti = new Set();
  let after;
  for (let pagina = 1; pagina <= MAX_PAGINE_EARN; pagina++) {
    const richiesta = { limit: '100' };
    if (after !== undefined) richiesta.after = String(after);

    let dati;
    try {
      const risposta = await exchange.privateGetFinanceSavingsLendingHistory(richiesta);
      dati = (risposta && Array.isArray(risposta.data)) ? risposta.data : [];
    } catch (e) {
      log(`Simple Earn - storico interessi, errore alla pagina ${pagina}: ${e.message}`);
      return { righe: out, completo: false, errore: e.message };
    }
    if (dati.length === 0) return { righe: out, completo: true };

    const piuVecchio = Math.min(...dati.map(r => Number(r.ts)));
    let nuovi = 0;
    for (const r of dati) {
      const chiave = `${r.ccy}|${r.ts}`;
      if (visti.has(chiave)) continue;
      visti.add(chiave);
      if (Number(r.ts) < startTime) continue;
      out.push(r);
      nuovi++;
    }
    log(`Simple Earn - storico interessi, pagina ${pagina}: +${nuovi} nuovi su ${dati.length}, tot=${out.length}`);

    //Superata la data richiesta si smette: il resto e' gia' stato importato in passato
    if (piuVecchio < startTime) return { righe: out, completo: true };
    if (dati.length < 100) return { righe: out, completo: true };
    if (nuovi === 0 || (after !== undefined && piuVecchio >= after)) {
      //La pagina non e' avanzata: questo endpoint non sta onorando `after`. NON si dichiara completo,
      //altrimenti si spaccerebbe per "tutto lo storico" quel poco che sta nella prima pagina.
      log(`Simple Earn - storico interessi: la paginazione non avanza, l'endpoint sembra ignorare 'after'. `
        + `Lo storico recuperato si ferma a ${new Date(piuVecchio).toISOString()}.`);
      return { righe: out, completo: false };
    }
    after = piuVecchio;
    await new Promise(r => setTimeout(r, 200));
  }
  //Tetto raggiunto: lo storico e' piu' lungo del massimo che si e' disposti a scaricare in una volta.
  return { righe: out, completo: false };
}

async function main() {
  const [, , exchangeId, apiKey, secret, startDateArg = "0", tokensArg = "", passphrase = "", hostnameArg = ""] = process.argv;

  const exchange = new (ccxt[exchangeId] || ccxt.okx)({
    apiKey,
    secret,
    password: passphrase,
    enableRateLimit: true,
    timeout: 60000
  });

  const host = await risolviHostname(exchange, hostnameArg.trim());
  if (!host) {
    console.log(JSON.stringify({ error: "nessun dominio OKX ha riconosciuto la chiave API" }, null, 2));
    return;
  }
  exchange.hostname = host;

  //startDate = 0 significa "tutto lo storico disponibile", ed e' il caso della diagnostica
  let startTime = Number(startDateArg);
  if (!Number.isFinite(startTime) || startTime < 0) startTime = 0;

  const interessi = await storicoInteressi(exchange, startTime);
  if (interessi.righe.length > 0) {
    const ts = interessi.righe.map(r => Number(r.ts));
    log(`Simple Earn - storico interessi: ${interessi.righe.length} record, dal `
      + `${new Date(Math.min(...ts)).toISOString()} al ${new Date(Math.max(...ts)).toISOString()}`
      + (interessi.completo ? " (storico completo)" : " (INTERROTTO prima della fine)"));
  }

  const risultato = {
    okx_hostname: host,
    savings_balance:      await chiama(exchange, 'privateGetFinanceSavingsBalance', {}, 'Simple Earn - saldo'),
    savings_lending:      interessi.righe,
    savings_lending_completo: interessi.completo,
    staking_attivi:       await chiama(exchange, 'privateGetFinanceStakingDefiOrdersActive', {}, 'On-chain Earn - posizioni aperte'),
    staking_storico:      await chiama(exchange, 'privateGetFinanceStakingDefiOrdersHistory', { limit: '100' }, 'On-chain Earn - storico ordini')
  };

  console.log(JSON.stringify(risultato, null, 2));
}

main().catch(err => {
  console.log(JSON.stringify({ error: err.message }, null, 2));
});
