// npm i ccxt@latest
//
// Scarica i "bills" (movimenti contabili) di OKX, che sono l'equivalente API dell'export CSV
// gestito da Importazioni.Ex_OKX_Importa: ogni riga e' una variazione di saldo su una moneta.
//
// Vengono interrogati i due conti separatamente, esattamente come i due CSV Funding e Trading:
//   - Funding account -> /api/v5/asset/bills-history   (depositi, prelievi, trasferimenti, earn, reward)
//   - Trading account -> /api/v5/account/bills-archive (trade, conversioni, fee)
//
// I due conti hanno finestre storiche MOLTO diverse, per questo il limite non e' piu' unico:
//   - Funding: asset/bills-history restituisce l'intero storico. Verificato su dati reali
//     (03/08/2026): 100 movimenti dal 19/02/2025 al 28/07/2026, 524 giorni, con la pagina
//     successiva vuota. E' molto piu' capiente di asset/bills, che si fermava a ~1 mese e che era
//     l'endpoint usato prima. Costa pero' 10 unita' di rate limit: fra una pagina e l'altra
//     servono ~1,1 s, e a 300 ms si prende un 50011 "Too many requests".
//   - Trading: account/bills-archive copre AL MASSIMO 3 MESI. Per lo storico piu' vecchio serve
//     l'archivio trimestrale (OKX_Archivio.js) oppure l'import del CSV.
// Se startDate e' piu' vecchia del limite del Trading, per quel solo conto viene limitata.
//
// argv: exchangeId apiKey secret startDate(ms) tokens passphrase hostname
// Passphrase e hostname sono in CODA perche' tutti gli altri script destrutturano argv
// per posizione e non devono essere toccati.

const ccxt = require('ccxt');

// ======================= Costanti & Utils =======================

// Il solo conto Trading e' limitato a 3 mesi. Si tiene un margine di qualche giorno per non farsi
// rifiutare la richiesta al limite esatto della finestra. Il Funding non ha questo limite:
// asset/bills-history torna tutto lo storico, quindi la sua data di partenza non va toccata.
const FINESTRA_MAX_TRADING_MS = 88 * 24 * 60 * 60 * 1000;
const LIMIT = 100;        // massimo consentito da OKX per pagina
const MAX_PAGINE = 500;   // 50.000 movimenti

// Attesa fra due pagine. asset/bills-history costa 10 unita' di rate limit (~1,1 s con il
// moltiplicatore di 110 ms di CCXT): a 300 ms si prende un 50011 gia' alla seconda pagina.
const PAUSA_PAGINA_MS = 1500;

// OKX non ha un solo dominio: le entita' regionali (europea, statunitense, globale) sono separate
// e una chiave API creata su una di esse NON esiste sulle altre. Interrogare il dominio sbagliato
// risponde 50119 "API key doesn't exist" anche con credenziali perfettamente valide, ed e'
// l'errore in cui incappa chi si e' registrato in Europa: CCXT punta di default a www.okx.com.
// Le fonti pubbliche si contraddicono su quale sia il dominio europeo (my.okx.com o eea.okx.com),
// percio' il dominio giusto non si indovina: si prova, e quello che risponde viene memorizzato
// dal chiamante Java e ripassato qui alle esecuzioni successive.
const HOSTNAME_CANDIDATI = ['my.okx.com', 'eea.okx.com', 'www.okx.com', 'app.okx.com'];

// Errori che riguardano le credenziali: riprovare non ha alcun senso, vanno mostrati subito
// all'utente con una spiegazione di quale delle tre credenziali e' in discussione.
const ERRORI_CREDENZIALI = {
  '50119': "la chiave API non esiste su questo dominio di OKX",
  '50111': "chiave API non valida",
  '50105': "passphrase errata (e' quella scelta creando la chiave API, non la password dell'account)",
  '50113': "firma non valida: l'API secret non corrisponde alla chiave",
  '50110': "l'indirizzo IP di questo computer non e' fra quelli autorizzati per la chiave API"
};

// Gli unici due errori che dicono "la chiave non e' su QUESTO dominio", e per i quali ha quindi
// senso provare il dominio successivo. Tutti gli altri errori di credenziali dimostrano il
// contrario - la chiave e' stata riconosciuta, e' un'altra credenziale a non andare - e cambiare
// dominio non li risolverebbe: vanno riportati subito com'e', altrimenti l'utente si sente dire
// "provati quattro domini" senza scoprire che il problema e' la passphrase.
const CHIAVE_ALTROVE = ['50119', '50111'];

function logProg(msg) { console.error(`[PROG] ${msg}`); }
function log(msg) { console.error(`[Node-LOG] ${msg}`); }

function sleep(ms) { return new Promise(resolve => setTimeout(resolve, ms)); }

/** @return il codice di errore numerico di OKX contenuto nel messaggio, o null se non c'e' */
function codiceErroreOKX(e) {
  const trovato = String((e && e.message) || '').match(/"code"\s*:\s*"?(\d+)"?/);
  return trovato ? trovato[1] : null;
}

/**
 * Individua il dominio regionale OKX su cui la chiave API esiste davvero, provando i candidati
 * con una richiesta minima (una sola riga di bills del Funding).
 *
 * Solo i codici di {@link CHIAVE_ALTROVE} fanno passare al candidato successivo: ogni altro errore
 * di credenziali e' decisivo, perche' dimostra che la chiave su questo dominio esiste.
 *
 * @param hostnamePreferito dominio gia' riconosciuto in una esecuzione precedente, provato per primo
 * @return {{hostname: string}} in caso di successo, {{errore: string}} se la chiave non e' utilizzabile
 */
async function risolviHostname(exchange, hostnamePreferito) {
  const candidati = hostnamePreferito
    ? [hostnamePreferito, ...HOSTNAME_CANDIDATI.filter(h => h !== hostnamePreferito)]
    : [...HOSTNAME_CANDIDATI];

  const inconcludenti = [];
  let ultimoCredenziali;
  for (const host of candidati) {
    exchange.hostname = host;
    try {
      await exchange.privateGetAssetBills({ limit: '1' });
      log(`Dominio OKX riconosciuto: ${host}`);
      return { hostname: host };
    } catch (e) {
      const codice = codiceErroreOKX(e);
      if (codice && CHIAVE_ALTROVE.includes(codice)) {
        log(`${host}: ${ERRORI_CREDENZIALI[codice]}, provo il dominio successivo`);
        ultimoCredenziali = `${ERRORI_CREDENZIALI[codice]} (codice OKX ${codice})`;
        await sleep(300);
        continue;
      }
      if (codice && ERRORI_CREDENZIALI[codice]) {
        return { errore: `${ERRORI_CREDENZIALI[codice]} (codice OKX ${codice})`, hostname: host };
      }
      // Errore non riconducibile alle credenziali (rete, rate limit, manutenzione): non dice
      // nulla sul dominio, quindi non ci si accontenta di questo candidato.
      log(`${host}: ${e.message}`);
      inconcludenti.push(`${host}: ${e.message}`);
      await sleep(300);
    }
  }

  // Il motivo di credenziali, se c'e' stato, viene sempre riportato: e' l'informazione utile.
  // Gli errori inconcludenti (rete, rate limit) si aggiungono, non lo sostituiscono, altrimenti
  // un singolo timeout su un dominio nasconderebbe il fatto che la chiave non esiste sugli altri.
  let errore;
  if (ultimoCredenziali) {
    errore = `la chiave API non e' utilizzabile su nessuno dei domini OKX provati `
           + `(${candidati.join(', ')}): ${ultimoCredenziali}.\n`
           + `Verifica di aver copiato la chiave giusta e che sia ancora attiva.`;
  } else {
    errore = `nessun dominio OKX ha risposto correttamente.`;
  }
  if (inconcludenti.length > 0) errore = `${errore}\n${inconcludenti.join('\n')}`;
  return { errore };
}

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
        const codice = codiceErroreOKX(e);
        if (codice && ERRORI_CREDENZIALI[codice]) {
          //Un errore di credenziali non si risolve riprovando: inutile spendere 18 secondi di
          //attese prima di dire all'utente qualcosa che si sa gia' al primo tentativo.
          const errore = `${ERRORI_CREDENZIALI[codice]} (codice OKX ${codice})`;
          log(`${etichetta}: ${errore}`);
          return { bills: out, completo: false, errore };
        }
        tentativo++;
        if (tentativo >= 4) {
          log(`${etichetta} errore definitivo alla pagina ${pagina}: ${e.message}`);
          return { bills: out, completo: false };
        }
        //Il 50011 (troppe richieste) non e' un guasto: basta rallentare. Si attende di piu' che
        //per gli altri errori, perche' rientrare subito nella finestra lo ripresenterebbe.
        const attesa = (codice === '50011' ? 6000 : 3000) * tentativo;
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
    await sleep(PAUSA_PAGINA_MS);
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
  const [, , exchangeId, apiKey, secret, startDateArg = "0", tokensArg = "", passphrase = "", hostnameArg = ""] = process.argv;

  const endTime = Date.now();
  let startTime = Number(startDateArg);
  if (!Number.isFinite(startTime) || startTime <= 0) startTime = endTime - FINESTRA_MAX_TRADING_MS;

  //Il Funding parte dalla data richiesta senza alcun taglio: asset/bills-history torna tutto lo
  //storico, e limitarlo ai 3 mesi del Trading butterebbe via anni di movimenti realmente disponibili.
  const startTimeFunding = startTime;

  //Il Trading invece e' davvero limitato a 3 mesi: chiedere di piu' non fa fallire la richiesta,
  //restituisce solo meno righe, quindi il taglio va reso esplicito nel log.
  let startTimeTrading = startTime;
  const limiteTrading = endTime - FINESTRA_MAX_TRADING_MS;
  if (startTimeTrading < limiteTrading) {
    log(`Trading: data richiesta (${new Date(startTimeTrading).toISOString()}) oltre il limite di 3 mesi di account/bills-archive, limitata a ${new Date(limiteTrading).toISOString()}. Per lo storico precedente serve l'archivio trimestrale o l'import del CSV.`);
    startTimeTrading = limiteTrading;
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

  const risoluzione = await risolviHostname(exchange, hostnameArg.trim());
  if (risoluzione.errore) {
    console.log(JSON.stringify({
      okx_fundingBills: [], okx_tradingBills: [], okx_completo: false,
      //Se l'errore e' arrivato da un dominio che ha comunque riconosciuto la chiave (passphrase o
      //secret sbagliati), quel dominio e' quello giusto e vale la pena ricordarlo.
      okx_hostname: risoluzione.hostname || "", error: risoluzione.errore
    }));
    return;
  }
  exchange.hostname = risoluzione.hostname;

  const funding = await fetchBills(exchange, 'privateGetAssetBillsHistory', startTimeFunding, endTime, 'Funding')
    .catch(e => { log(`Funding fallito: ${e.message}`); return { bills: [], completo: false }; });

  const trading = await fetchBills(exchange, 'privateGetAccountBillsArchive', startTimeTrading, endTime, 'Trading')
    .catch(e => { log(`Trading fallito: ${e.message}`); return { bills: [], completo: false }; });

  const risultato = {
    okx_fundingBills: funding.bills,
    okx_tradingBills: trading.bills,
    okx_completo: funding.completo && trading.completo,
    okx_hostname: risoluzione.hostname
  };
  //Un errore di credenziali su uno dei due conti (tipicamente: chiave senza i permessi di lettura
  //sul conto Trading) va mostrato, non nascosto dietro un generico "scaricamento incompleto".
  const errore = funding.errore || trading.errore;
  if (errore) risultato.error = errore;
  console.log(JSON.stringify(risultato));
}

main().catch(err => {
  console.log(JSON.stringify({ okx_fundingBills: [], okx_tradingBills: [], okx_completo: false, okx_hostname: "", error: err.message }));
});
