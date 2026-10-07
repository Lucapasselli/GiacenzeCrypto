// Lo lancia CcxtInterop dopo OKX_Bills.js, a ogni scaricamento OKX via API: savings_lending diventa i
// rendimenti giornalieri di Simple Earn (convertOKXEarn), staking_storico serve a ricostruire la gamba in
// uscita delle posizioni On-chain Earn convertite in un token di Liquid Staking (AbbinaLiquidStakingOnChain).
// Nato come diagnostico, si puo' ancora lanciare a mano come descritto sotto.
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
// In piu' scarica dal Funding le sottoscrizioni e i riscatti di Simple Earn (type 75/76) di tutto lo
// storico, per ricostruire gli interessi che lo storico interessi non copre piu' (vedi flussiSimpleEarn).
//
// Questo script li interroga tutti e quattro e stampa la risposta GREZZA, senza interpretarla: la
// interpreta il chiamante Java. Sono tutte chiamate di sola lettura.
//
// Uso, dalla cartella di lavoro (quella che contiene tools/node):
//   NODE_PATH=tools/node/node_modules tools/node/<distribuzione>/bin/node \
//       Scripts/OKX_Earn.js okx <apiKey> <secret> 0 "" <passphrase> [hostname]
//
// ATTENZIONE: le credenziali passate cosi' restano nella cronologia della shell.
//
// argv: exchangeId apiKey secret startDate(ms) tokens passphrase hostname [stakingCompleto]
//
// Con "stakingCompleto" come ultimo argomento lo storico degli ordini On-chain Earn si scarica tutto, pagina
// per pagina, invece della sola pagina dei 100 piu' recenti. CcxtInterop lo chiede solo quando fra i bill c'e'
// una conversione in un token di Liquid Staking (funding type 330), che va abbinata alla posizione di origine.

const ccxt = require('ccxt');
const { fetchBills } = require('./OKX_Bills.js');

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

// Lo storico degli ordini On-chain Earn pagina SOLO per ordId (`after` = ordini piu' vecchi dell'ordId dato) e
// in ordine di creazione, mentre l'abbinamento dei bill 330 cerca la data di RISCATTO: una posizione aperta da
// molto e chiusa ieri sta in fondo all'elenco. Per questo non ci si ferma a una data, si va fino all'ultima pagina.
const MAX_PAGINE_STAKING = 50;   // 5000 ordini: oltre si dichiara lo storico incompleto

/**
 * Scarica lo storico degli ordini On-chain Earn. Senza `completo` chiede una sola pagina, come prima della
 * paginazione. Restituisce sempre un array di ordini, piu' l'indicazione se lo storico e' arrivato in fondo.
 */
async function storicoStaking(exchange, completo) {
  const out = [];
  const visti = new Set();
  let after;
  const maxPagine = completo ? MAX_PAGINE_STAKING : 1;
  for (let pagina = 1; pagina <= maxPagine; pagina++) {
    const richiesta = { limit: '100' };
    if (after !== undefined) richiesta.after = after;

    let dati;
    try {
      const risposta = await exchange.privateGetFinanceStakingDefiOrdersHistory(richiesta);
      dati = (risposta && Array.isArray(risposta.data)) ? risposta.data : [];
    } catch (e) {
      log(`On-chain Earn - storico ordini, errore alla pagina ${pagina}: ${e.message}`);
      return { righe: out, completo: false };
    }

    let nuovi = 0;
    let piuVecchio;
    for (const o of dati) {
      const id = String(o.ordId || '');
      if (id !== '' && (piuVecchio === undefined || BigInt(id) < BigInt(piuVecchio))) piuVecchio = id;
      if (id === '' || visti.has(id)) continue;
      visti.add(id);
      out.push(o);
      nuovi++;
    }
    log(`On-chain Earn - storico ordini, pagina ${pagina}: +${nuovi} nuovi su ${dati.length}, tot=${out.length}`);

    if (dati.length < 100) return { righe: out, completo: true };
    if (!completo) return { righe: out, completo: false };
    if (nuovi === 0 || piuVecchio === undefined || (after !== undefined && BigInt(piuVecchio) >= BigInt(after))) {
      //Pagina ripetuta: l'endpoint non sta onorando `after`, e dichiararlo completo nasconderebbe gli ordini mancanti
      log(`On-chain Earn - storico ordini: la paginazione non avanza, lo storico recuperato e' incompleto.`);
      return { righe: out, completo: false };
    }
    after = piuVecchio;
    await new Promise(r => setTimeout(r, 400));   // limite dell'endpoint: 3 richieste al secondo
  }
  log(`On-chain Earn - storico ordini: raggiunto il tetto di ${MAX_PAGINE_STAKING} pagine, storico incompleto.`);
  return { righe: out, completo: false };
}

// Sottoscrizioni (75) e riscatti (76) di Simple Earn su TUTTO lo storico del Funding: servono a
// CcxtInterop per ricostruire gli interessi dei giorni che lo storico interessi (un mese) non copre
// piu'. Il saldo attuale piu' i riscatti meno le sottoscrizioni e' l'interesse maturato.
// Il filtro `type` dell'endpoint non e' verificato: si ricontrolla qui ogni bill, e se ne arriva uno di
// un altro tipo lo si scarta e lo si scrive nel log.
const INIZIO_STORICO_FUNDING = Date.UTC(2021, 1, 1);   // febbraio 2021, come OKX_Bills.js

async function flussiSimpleEarn(exchange) {
  const out = [];
  let completo = true;
  for (const tipo of ['75', '76']) {
    const r = await fetchBills(exchange, 'privateGetAssetBillsHistory', INIZIO_STORICO_FUNDING, Date.now(),
      `Simple Earn - flussi type ${tipo}`, 'ts', undefined, { type: tipo });
    if (!r.completo) completo = false;
    let estranei = 0;
    for (const b of r.bills) {
      if (String(b.type) === tipo) out.push(b); else estranei++;
    }
    if (estranei > 0) log(`Simple Earn - flussi type ${tipo}: ${estranei} bill di altri tipi scartati, il filtro type non e' applicato dall'endpoint`);
  }
  return { righe: out, completo };
}

async function main() {
  const [, , exchangeId, apiKey, secret, startDateArg = "0", tokensArg = "", passphrase = "", hostnameArg = "", stakingArg = ""] = process.argv;

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

  const staking = await storicoStaking(exchange, stakingArg.trim() === 'stakingCompleto');
  const flussi = await flussiSimpleEarn(exchange);

  const risultato = {
    okx_hostname: host,
    savings_balance:      await chiama(exchange, 'privateGetFinanceSavingsBalance', {}, 'Simple Earn - saldo'),
    savings_lending:      interessi.righe,
    savings_lending_completo: interessi.completo,
    staking_attivi:       await chiama(exchange, 'privateGetFinanceStakingDefiOrdersActive', {}, 'On-chain Earn - posizioni aperte'),
    staking_storico:      staking.righe,
    staking_storico_completo: staking.completo,
    savings_flussi:       flussi.righe,
    savings_flussi_completo: flussi.completo
  };

  console.log(JSON.stringify(risultato, null, 2));
}

if (require.main === module) {
  main().catch(err => {
    console.log(JSON.stringify({ error: err.message }, null, 2));
  });
}

module.exports = { storicoStaking, flussiSimpleEarn };
