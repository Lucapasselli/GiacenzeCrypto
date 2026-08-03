// npm i ccxt@latest
//
// Scarica l'ARCHIVIO STORICO dei bill del conto Trading di OKX, che copre periodi ben piu' vecchi
// dei 3 mesi di account/bills-archive usato da OKX_Bills.js.
//
// Il meccanismo e' asincrono e in due tempi:
//   POST /api/v5/account/bills-history-archive  {year, quarter}  -> chiede la generazione
//   GET  /api/v5/account/bills-history-archive  {year, quarter}  -> state + fileHref quando e' pronto
// Il file e' uno ZIP che contiene un solo CSV (bills_history_archive_<dal>_<al>.csv).
// Misurato su dati reali il 03/08/2026: generazione completata in 105 secondi.
//
// LIMITI DELL'ENDPOINT:
//   - copre da febbraio 2021, ma NON il trimestre in corso;
//   - riguarda solo il conto Trading unificato, non il Funding (che pero' non ne ha bisogno:
//     asset/bills-history restituisce gia' tutto lo storico);
//   - la POST e' fortemente limitata (CCXT le assegna costo 72000, cioe' ~2,2 ore fra due
//     richieste), quindi se ne invia UNA SOLA per esecuzione.
//
// Questo script e' volutamente "stupido": scarica, scompatta, trasforma il CSV in oggetti e li
// consegna cosi' come sono. Tutta l'interpretazione delle causali resta in Java, dove vive gia'
// l'unica tabella di conversione (CcxtInterop.causaleBillOKX e tipoDaArchivioOKX).
//
// argv: exchangeId apiKey secret startDate(ms) trimestri passphrase hostname
//   trimestri = elenco separato da virgole nella forma <anno>Q<n>, es. "2026Q2,2026Q1"
//               calcolato da Java, dal piu' recente al piu' vecchio.

const ccxt = require('ccxt');
const zlib = require('zlib');

// ======================= Costanti & Utils =======================

// Quanto si resta in attesa che OKX generi il file prima di rinunciare e invitare a ritentare.
// La misura su dati reali e' 105 s: 5 minuti danno margine senza bloccare l'utente all'infinito.
const ATTESA_MAX_MS = 5 * 60 * 1000;
const ATTESA_PASSO_MS = 15000;

const ERRORI_CREDENZIALI = {
  '50119': "la chiave API non esiste su questo dominio di OKX",
  '50111': "chiave API non valida",
  '50105': "passphrase errata (e' quella scelta creando la chiave API, non la password dell'account)",
  '50113': "firma non valida: l'API secret non corrisponde alla chiave",
  '50110': "l'indirizzo IP di questo computer non e' fra quelli autorizzati per la chiave API"
};

// Non e' un errore: significa solo che per quel trimestre non e' ancora stata chiesta la generazione.
const CODICE_DA_RICHIEDERE = '51604';

function logProg(msg) { console.error(`[PROG] ${msg}`); }
function log(msg) { console.error(`[Node-LOG] ${msg}`); }
function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

function codiceErroreOKX(e) {
  const t = String((e && e.message) || '').match(/"code"\s*:\s*"?(\d+)"?/);
  return t ? t[1] : null;
}
function messaggioOKX(e) {
  const t = String((e && e.message) || '').match(/"msg"\s*:\s*"([^"]*)"/);
  return t ? t[1] : String((e && e.message) || e).slice(0, 200);
}

// ======================= ZIP =======================

/**
 * Estrae i file da uno ZIP leggendo la CENTRAL DIRECTORY e non gli header locali.
 * E' una scelta necessaria, non stilistica: nello ZIP prodotto da OKX gli header locali riportano
 * dimensione 0 (il file e' scritto in streaming, con data descriptor in coda), mentre la central
 * directory ha sempre le dimensioni corrette.
 * @return array di {nome, dati} con i contenuti gia' decompressi
 */
function estraiZip(buf) {
  // L'EOCD sta in fondo, entro gli ultimi 64KB: lo si cerca a ritroso.
  let eocd = -1;
  const minimo = Math.max(0, buf.length - 65557);
  for (let i = buf.length - 22; i >= minimo; i--) {
    if (buf.readUInt32LE(i) === 0x06054b50) { eocd = i; break; }
  }
  if (eocd < 0) throw new Error('ZIP non valido: end of central directory non trovato');

  const numVoci = buf.readUInt16LE(eocd + 10);
  let p = buf.readUInt32LE(eocd + 16);   // offset della central directory

  const risultato = [];
  for (let n = 0; n < numVoci; n++) {
    if (buf.readUInt32LE(p) !== 0x02014b50) break;
    const metodo = buf.readUInt16LE(p + 10);
    const dimCompressa = buf.readUInt32LE(p + 20);
    const lungNome = buf.readUInt16LE(p + 28);
    const lungExtra = buf.readUInt16LE(p + 30);
    const lungCommento = buf.readUInt16LE(p + 32);
    const offsetLocale = buf.readUInt32LE(p + 42);
    const nome = buf.subarray(p + 46, p + 46 + lungNome).toString('utf8');
    p += 46 + lungNome + lungExtra + lungCommento;

    if (nome.endsWith('/') || dimCompressa === 0) continue;   // cartelle

    // L'header locale serve solo per sapere dove iniziano i dati: i suoi campi lunghezza sono
    // affidabili anche quando le dimensioni non lo sono.
    const lungNomeL = buf.readUInt16LE(offsetLocale + 26);
    const lungExtraL = buf.readUInt16LE(offsetLocale + 28);
    const inizio = offsetLocale + 30 + lungNomeL + lungExtraL;
    const grezzi = buf.subarray(inizio, inizio + dimCompressa);

    risultato.push({
      nome,
      dati: metodo === 0 ? grezzi : zlib.inflateRawSync(grezzi)
    });
  }
  return risultato;
}

// ======================= CSV =======================

/** Divide una riga CSV tenendo conto delle virgolette */
function dividiRiga(riga) {
  const campi = [];
  let corrente = '';
  let dentroVirgolette = false;
  for (let i = 0; i < riga.length; i++) {
    const c = riga[i];
    if (c === '"') {
      if (dentroVirgolette && riga[i + 1] === '"') { corrente += '"'; i++; }
      else dentroVirgolette = !dentroVirgolette;
    } else if (c === ',' && !dentroVirgolette) {
      campi.push(corrente); corrente = '';
    } else corrente += c;
  }
  campi.push(corrente);
  return campi;
}

/**
 * Converte il CSV dell'archivio in oggetti, uno per riga, con le stesse chiavi dell'intestazione.
 * I valori NON vengono interpretati: l'apostrofo iniziale con cui il CSV forza il testo per Excel
 * viene pero' tolto qui, perche' e' una convenzione del formato e non un dato.
 */
function csvInOggetti(testo) {
  const righe = testo.split(/\r?\n/).filter(r => r.trim() !== '');
  if (righe.length < 2) return [];
  const intestazione = dividiRiga(righe[0]).map(s => s.trim());
  const out = [];
  for (let i = 1; i < righe.length; i++) {
    const campi = dividiRiga(righe[i]);
    const o = {};
    for (let c = 0; c < intestazione.length; c++) {
      let v = (campi[c] !== undefined ? campi[c] : '').trim();
      if (v.startsWith("'")) v = v.substring(1);
      o[intestazione[c]] = v;
    }
    out.push(o);
  }
  return out;
}

// ======================= OKX =======================

/** @return {{righe}} se il file c'e', {{daRichiedere:true}} se va chiesto, {{errore}} se e' andata male */
async function leggiArchivio(exchange, anno, trimestre) {
  try {
    const r = await exchange.privateGetAccountBillsHistoryArchive({ year: anno, quarter: trimestre });
    const d = ((r && r.data) || [])[0];
    if (d && d.fileHref) return { href: d.fileHref, stato: d.state };
    return { stato: (d && d.state) || 'ongoing' };
  } catch (e) {
    const c = codiceErroreOKX(e);
    if (c === CODICE_DA_RICHIEDERE) return { daRichiedere: true };
    if (c && ERRORI_CREDENZIALI[c]) return { errore: `${ERRORI_CREDENZIALI[c]} (codice OKX ${c})` };
    return { errore: messaggioOKX(e) };
  }
}

/** Scarica lo ZIP e ne estrae le righe del CSV */
async function scaricaRighe(href, etichetta) {
  const resp = await fetch(href);
  if (!resp.ok) throw new Error(`download fallito: HTTP ${resp.status}`);
  const buf = Buffer.from(await resp.arrayBuffer());
  const file = estraiZip(buf).filter(f => f.nome.toLowerCase().endsWith('.csv'));
  if (file.length === 0) throw new Error('nessun CSV dentro lo ZIP');
  let righe = [];
  for (const f of file) righe = righe.concat(csvInOggetti(f.dati.toString('utf8')));
  log(`${etichetta}: ${righe.length} righe da ${file.map(f => f.nome.split('/').pop()).join(', ')}`);
  return righe;
}

// ======================= MAIN =======================

async function main() {
  const [, , exchangeId, apiKey, secret, , trimestriArg = "", passphrase = "", hostnameArg = ""] = process.argv;

  const trimestri = trimestriArg.split(',').map(s => s.trim()).filter(Boolean);
  if (trimestri.length === 0) {
    console.log(JSON.stringify({ okx_archivioBills: [], okx_periodi: [], error: "nessun trimestre richiesto" }));
    return;
  }

  const ExchangeClass = ccxt[exchangeId] || ccxt.okx;
  const exchange = new ExchangeClass({
    apiKey, secret, password: passphrase,
    //Il limitatore di CCXT assegna alla POST un costo che si traduce in oltre due ore di attesa:
    //lasciandolo attivo il programma resterebbe bloccato. Il rispetto del limite e' garantito
    //invece dall'inviare una sola richiesta per esecuzione.
    enableRateLimit: false,
    timeout: 60000
  });
  exchange.hostname = hostnameArg.trim() || 'www.okx.com';

  try { await exchange.loadTimeDifference(); exchange.options.adjustForTimeDifference = true; } catch (e) { }

  const periodi = [];       // esito per trimestre, mostrato poi all'utente
  let bills = [];
  let richiestaInviata = false;
  let errore;

  for (const periodo of trimestri) {
    const m = periodo.match(/^(\d{4})Q([1-4])$/);
    if (!m) { periodi.push({ periodo, stato: 'ignorato' }); continue; }
    const anno = m[1], trimestre = 'Q' + m[2];

    logProg(`Archivio ${periodo}: verifica`);
    let esito = await leggiArchivio(exchange, anno, trimestre);

    if (esito.errore) {
      log(`${periodo}: ${esito.errore}`);
      periodi.push({ periodo, stato: 'errore', dettaglio: esito.errore });
      errore = errore || esito.errore;
      continue;
    }

    if (esito.daRichiedere) {
      //Una sola POST per esecuzione: le altre restano per i prossimi tentativi.
      if (richiestaInviata) {
        periodi.push({ periodo, stato: 'da richiedere' });
        continue;
      }
      logProg(`Archivio ${periodo}: richiesta di generazione`);
      try {
        await exchange.privatePostAccountBillsHistoryArchive({ year: anno, quarter: trimestre });
        richiestaInviata = true;
      } catch (e) {
        const c = codiceErroreOKX(e);
        const d = (c && ERRORI_CREDENZIALI[c]) ? `${ERRORI_CREDENZIALI[c]} (codice OKX ${c})` : messaggioOKX(e);
        log(`${periodo}: richiesta rifiutata - ${d}`);
        periodi.push({ periodo, stato: 'errore', dettaglio: d });
        errore = errore || d;
        continue;
      }

      const inizio = Date.now();
      while (Date.now() - inizio < ATTESA_MAX_MS) {
        await sleep(ATTESA_PASSO_MS);
        const secondi = Math.round((Date.now() - inizio) / 1000);
        logProg(`Archivio ${periodo}: in preparazione da ${secondi}s`);
        esito = await leggiArchivio(exchange, anno, trimestre);
        if (esito.href || esito.errore) break;
      }
    }

    if (esito.href) {
      try {
        const righe = await scaricaRighe(esito.href, `Archivio ${periodo}`);
        bills = bills.concat(righe);
        periodi.push({ periodo, stato: 'scaricato', righe: righe.length });
      } catch (e) {
        log(`${periodo}: ${e.message}`);
        periodi.push({ periodo, stato: 'errore', dettaglio: e.message });
        errore = errore || e.message;
      }
    } else if (!esito.errore) {
      //Generazione ancora in corso: non e' un errore, va solo ritentato piu' tardi.
      log(`${periodo}: file non ancora pronto`);
      periodi.push({ periodo, stato: 'in preparazione' });
    }
  }

  const risultato = { okx_archivioBills: bills, okx_periodi: periodi, okx_hostname: exchange.hostname };
  if (errore) risultato.error = errore;
  console.log(JSON.stringify(risultato));
}

main().catch(err => {
  console.log(JSON.stringify({ okx_archivioBills: [], okx_periodi: [], okx_hostname: "", error: err.message }));
});
