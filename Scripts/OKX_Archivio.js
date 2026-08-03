// npm i ccxt@latest
//
// Scarica l'ARCHIVIO STORICO dei bill del conto Trading di OKX, che copre periodi ben piu' vecchi
// dei 3 mesi di account/bills-archive usato da OKX_Bills.js.
//
// Il meccanismo e' asincrono e in due tempi:
//   POST /api/v5/account/bills-history-archive  {year, quarter}  -> chiede la generazione
//   GET  /api/v5/account/bills-history-archive  {year, quarter}  -> state + fileHref quando e' pronto
// Il file e' uno ZIP che contiene un solo CSV (bills_history_archive_<dal>_<al>.csv).
// Lo scaricamento avviene in TRE FASI, ed e' cosi' per una ragione misurata:
//   1. ricognizione: una GET per trimestre (costo 2, ~150 ms) dice quali file esistono gia';
//   2. richiesta: una POST per ogni trimestre mai chiesto, tutte di seguito;
//   3. attesa e ritiro: si scarica ciascun file appena diventa disponibile.
// La parte cara e' CHIEDERE, non ritirare: i file generati restano sul server e le esecuzioni
// successive se li riprendono gratis con la sola GET.
//
// LIMITI DELL'ENDPOINT:
//   - copre da febbraio 2021, ma NON il trimestre in corso;
//   - riguarda solo il conto Trading unificato, non il Funding (che pero' non ne ha bisogno:
//     asset/bills-history restituisce gia' tutto lo storico).
//
// NOTA SUL LIMITE DELLA POST: CCXT le assegna costo 72000, che sulla sua base di 110 ms si
// tradurrebbe in una richiesta ogni ~2h12m, e per questo la versione precedente ne inviava UNA SOLA
// per esecuzione. Misurato sul campo il 04/08/2026, quel limite NON e' applicato dal server: 15
// richieste consecutive a 1,5-3 s di distanza sono state tutte accettate, senza alcun 50011, e gli
// 11 file del secondo lotto erano tutti pronti entro 5 minuti (9 entro il primo). Il limitatore di
// CCXT resta quindi disattivato e le richieste si mandano tutte, ma un rifiuto va comunque gestito:
// non e' escluso che esista una soglia piu' alta, o diversa da account ad account. Al primo rifiuto
// per eccesso di richieste ci si ferma, si tiene quel che si e' ottenuto e lo si dice.
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

// Quanto si resta in attesa che OKX generi i file prima di rinunciare e invitare a ritentare.
// Misurato su 11 trimestri chiesti insieme: tutti pronti entro 5 minuti, 9 entro il primo. Dieci
// minuti danno quindi il doppio del margine osservato senza bloccare l'utente all'infinito.
const ATTESA_MAX_MS = 10 * 60 * 1000;
// Passo della ricognizione durante l'attesa. E' anche la latenza con cui il tasto Interrompi ha
// effetto: il lato Java uccide il processo quando lo script scrive una riga di log, quindi il
// passo non va allungato senza motivo.
const ATTESA_PASSO_MS = 15000;

// Codici con cui OKX dice "troppe richieste, riprova": al primo di questi si smette di chiedere.
// Non svuotano quel che si e' gia' ottenuto, spostano solo il resto alla prossima esecuzione.
const CODICI_TROPPE_RICHIESTE = ['50011', '50013', '50026'];

// Pausa fra due letture consecutive. CCXT assegna alla GET costo 2, cioe' ~220 ms di distanza: qui il
// limitatore e' disattivo e le letture partono alla velocita' della rete (95-233 ms misurati), che e'
// proprio sul filo. Contro una ricognizione ogni 15 secondi questa pausa non costa nulla e toglie di
// mezzo la causa piu' probabile di un rifiuto per frequenza.
const PAUSA_GET_MS = 250;

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
    //Solo le credenziali sono un guasto vero: con quelle sbagliate nessun trimestre potra' mai arrivare.
    //Tutto il resto (timeout, 5xx, troppe richieste) e' transitorio e va ritentato, non fatto pesare
    //sull'intero scaricamento.
    if (c && ERRORI_CREDENZIALI[c]) return { errore: `${ERRORI_CREDENZIALI[c]} (codice OKX ${c})`, fatale: true };
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

  // Esito per trimestre, nell'ordine in cui sono stati chiesti: e' cio' che l'utente vedra'.
  const periodi = new Map();
  for (const periodo of trimestri) periodi.set(periodo, { periodo, stato: 'da richiedere' });

  let bills = [];
  let errore;

  const valido = p => /^\d{4}Q[1-4]$/.test(p);
  const pezzi = p => [p.slice(0, 4), 'Q' + p.slice(5)];

  // ===================== FASE 1: ricognizione =====================
  // Una GET per trimestre. Costa 2 unita' di rate limit e risponde in ~150 ms, quindi si interrogano
  // tutti sempre: e' il modo per accorgersi gratis dei file gia' generati da esecuzioni precedenti.
  const daRichiedere = [];
  const pendenti = [];
  const pronti = [];

  for (const periodo of trimestri) {
    if (!valido(periodo)) { periodi.set(periodo, { periodo, stato: 'ignorato' }); continue; }
    const [anno, trimestre] = pezzi(periodo);

    logProg(`Archivio ${periodo}: verifica`);
    const esito = await leggiArchivio(exchange, anno, trimestre);
    await sleep(PAUSA_GET_MS);

    if (esito.errore) {
      log(`${periodo}: ${esito.errore}`);
      periodi.set(periodo, { periodo, stato: 'errore', dettaglio: esito.errore });
      //Solo le credenziali fermano tutto: vedi leggiArchivio
      if (esito.fatale) errore = errore || esito.errore;
    } else if (esito.href) {
      pronti.push({ periodo, href: esito.href });
    } else if (esito.daRichiedere) {
      daRichiedere.push(periodo);
    } else {
      //Generazione gia' in corso da un'esecuzione precedente: niente da chiedere, solo da attendere
      pendenti.push(periodo);
      periodi.set(periodo, { periodo, stato: 'in preparazione' });
    }
  }
  log(`Ricognizione: ${pronti.length} gia' pronti, ${pendenti.length} in preparazione, ${daRichiedere.length} da chiedere`);

  // ===================== FASE 2: richieste di generazione =====================
  // Si mandano tutte. Al primo rifiuto per eccesso di richieste ci si ferma: i trimestri rimasti
  // restano "da richiedere" e la prossima esecuzione ripartira' da li'.
  let fermatoPerLimite = false;
  for (const periodo of daRichiedere) {
    if (fermatoPerLimite) break;
    const [anno, trimestre] = pezzi(periodo);
    logProg(`Archivio ${periodo}: richiesta di generazione`);
    try {
      await exchange.privatePostAccountBillsHistoryArchive({ year: anno, quarter: trimestre });
      pendenti.push(periodo);
      periodi.set(periodo, { periodo, stato: 'in preparazione' });
    } catch (e) {
      const c = codiceErroreOKX(e);
      if (CODICI_TROPPE_RICHIESTE.includes(c)) {
        //Non e' un guasto: e' la quota. Ci si ferma qui e si tiene quel che si e' ottenuto.
        fermatoPerLimite = true;
        log(`${periodo}: limite di richieste raggiunto (codice OKX ${c}), i restanti alla prossima esecuzione`);
        periodi.set(periodo, { periodo, stato: 'da richiedere', dettaglio: 'limite di richieste raggiunto' });
        continue;
      }
      const d = (c && ERRORI_CREDENZIALI[c]) ? `${ERRORI_CREDENZIALI[c]} (codice OKX ${c})` : messaggioOKX(e);
      log(`${periodo}: richiesta rifiutata - ${d}`);
      periodi.set(periodo, { periodo, stato: 'errore', dettaglio: d });
      errore = errore || d;
    }
  }

  // ===================== FASE 3: attesa e ritiro =====================
  /** Scarica un file gia' disponibile e ne registra l'esito */
  async function ritira(periodo, href) {
    try {
      const righe = await scaricaRighe(href, `Archivio ${periodo}`);
      bills = bills.concat(righe);
      periodi.set(periodo, { periodo, stato: 'scaricato', righe: righe.length });
    } catch (e) {
      //Il singolo file non scaricato viene riportato fra i periodi, ma non e' un guasto dell'intera
      //operazione: gli altri trimestri restano validi e questo si ritenta alla prossima esecuzione.
      log(`${periodo}: ${e.message}`);
      periodi.set(periodo, { periodo, stato: 'errore', dettaglio: e.message });
    }
  }

  //Prima quelli gia' pronti dalla ricognizione: sono immediati e non c'e' motivo di farli attendere
  for (const p of pronti) await ritira(p.periodo, p.href);

  const inizio = Date.now();
  let daAttendere = pendenti.slice();
  while (daAttendere.length > 0) {
    const trascorsi = Math.round((Date.now() - inizio) / 1000);
    if (Date.now() - inizio >= ATTESA_MAX_MS) {
      log(`Attesa interrotta dopo ${trascorsi}s: ${daAttendere.length} trimestri ancora in preparazione`);
      break;
    }
    logProg(`Archivio: ${daAttendere.length} trimestri in preparazione da ${trascorsi}s`);
    await sleep(ATTESA_PASSO_MS);

    const restano = [];
    for (const periodo of daAttendere) {
      const [anno, trimestre] = pezzi(periodo);
      const esito = await leggiArchivio(exchange, anno, trimestre);
      await sleep(PAUSA_GET_MS);
      if (esito.href) {
        await ritira(periodo, esito.href);
      } else if (esito.fatale) {
        //Credenziali: da qui in poi non arrivera' nulla, inutile continuare a interrogare
        log(`${periodo}: ${esito.errore}`);
        periodi.set(periodo, { periodo, stato: 'errore', dettaglio: esito.errore });
        errore = errore || esito.errore;
      } else if (esito.errore) {
        //Errore passeggero: si RITENTA al giro successivo. Toglierlo dalla lista sarebbe la cosa
        //peggiore possibile, perche' un timeout su un trimestre non deve costare gli altri venti.
        log(`${periodo}: lettura fallita (${esito.errore}), ritento fra ${ATTESA_PASSO_MS / 1000}s`);
        restano.push(periodo);
      } else {
        restano.push(periodo);
      }
    }
    daAttendere = restano;
  }

  const risultato = { okx_archivioBills: bills, okx_periodi: [...periodi.values()], okx_hostname: exchange.hostname };
  //ATTENZIONE: 'error' e' fatale per il chiamante — CcxtInterop.fetchMovimento restituisce null appena lo
  //trova, buttando via TUTTI i bill raccolti. Ci va quindi soltanto cio' che rende inutile l'intera
  //operazione, cioe' le credenziali. Il destino dei singoli trimestri si legge in okx_periodi, da cui il
  //lato Java ricava gia' l'elenco di quelli da ritentare.
  if (errore) risultato.error = errore;
  console.log(JSON.stringify(risultato));
}

main().catch(err => {
  console.log(JSON.stringify({ okx_archivioBills: [], okx_periodi: [], okx_hostname: "", error: err.message }));
});
