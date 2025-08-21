
// npm i ccxt@latest

const ccxt = require('ccxt');

const EPOCH_ALL_HISTORY = new Date('2017-01-01T00:00:00Z').getTime();

function pickAssetField(o) { return o?.asset || o?.coin || o?.token || o?.rewardAsset || o?.currency || null; }

function logProg(msg) { console.error(`[PROG] ${msg}`); }

class BinanceRateLimiter {

  constructor() {

    this.requestCount = 0;

    this.startTime = Date.now();

    this.lastRequest = 0;

  }

  async waitForRateLimit() {

    this.requestCount++;

    const now = Date.now();

    const timeSinceLastRequest = now - this.lastRequest;

    if (timeSinceLastRequest < 2000) {

      const waitTime = 2000 - timeSinceLastRequest;

      console.error(`[RATE] Pausa inter-request: ${waitTime}ms`);

      await this.sleep(waitTime);

    }

    if (this.requestCount % 40 === 0) {

      console.error(`[RATE] 🛑 Pausa lunga dopo ${this.requestCount} richieste (60s)`);

      await this.sleep(60000);

      this.startTime = Date.now();

      console.error(`[RATE] ✅ Ripartito dopo pausa lunga`);

    }

    this.lastRequest = Date.now();

  }

  async sleep(ms) { return new Promise(resolve => setTimeout(resolve, ms)); }

  async handleRateLimit(error, attempt = 0) {

    const is429 = error.message && (

      error.message.includes('429') || 

      error.message.includes('Too many requests') ||

      error.message.includes('Request weight exceeds')

    );

    if (is429) {

      const waitTime = Math.min(30000 * (2 ** attempt), 300000);

      console.error(`[RATE] ⚠️ Rate limit hit! Pausa: ${waitTime/1000}s (attempt ${attempt+1})`);

      await this.sleep(waitTime);

      return true;

    }

    return false;

  }

}

// NUOVA FUNZIONE: Paginazione basata su timestamp cursor per endpoint che restituiscono max 10 record
async function fetchWithTimestampCursorAvanti(exchange, endpoint, params, rateLimiter, seenIds, out, idFn, maxRecords = 10000) {
  const { endTime, ...baseParams } = params;
  let currentStartTime = params.startTime;
  let totalFetched = 0;
  let iterationCount = 0;
  let consecutiveEmptyIterations = 0; // Contatore per cicli consecutivi senza nuovi record
  const maxIterations = Math.ceil(maxRecords / 10) + 100; // Safety margin
  const maxConsecutiveEmpty = 3; // Massimo numero di iterazioni consecutive senza nuovi record

  console.error(`[CURSOR] Inizio fetch con timestamp cursor per ${endpoint}, startTime: ${new Date(currentStartTime).toISOString()}`);

  while (totalFetched < maxRecords && iterationCount < maxIterations && consecutiveEmptyIterations < maxConsecutiveEmpty) {
    iterationCount++;
    
    if (rateLimiter) await rateLimiter.waitForRateLimit();

    const requestParams = { 
      ...baseParams, 
      startTime: currentStartTime
    };
    
    // Aggiungi endTime solo se specificato
    if (endTime) {
      requestParams.endTime = endTime;
    }

    let res;
    try {
      res = await exchange.request(endpoint, 'sapi', 'GET', requestParams);
    } catch (error) {
      console.error(`[CURSOR-ERROR] Errore API per startTime ${new Date(currentStartTime).toISOString()}: ${error.message}`);
      break;
    }

    console.error(`[API-RESPONSE] ${endpoint} cursor iter=${iterationCount} startTime=${new Date(currentStartTime).toISOString()}: ${JSON.stringify(res, null, 2)}`);

    const rows = res?.rows ?? (Array.isArray(res) ? res : []);

    // CONDIZIONE DI USCITA: Array vuoto significa che abbiamo recuperato tutti i dati
    if (!rows || rows.length === 0) {
      console.error(`[CURSOR] Completato: nessun record trovato all'iterazione ${iterationCount}. Totale recuperato: ${totalFetched}`);
      break;
    }

    // Filtra e aggiungi i nuovi record
    let newRecordsCount = 0;
    rows.forEach(r => {
      const id = idFn(r);
      if (!seenIds.has(id)) {
        seenIds.add(id);
        out.push(r);
        newRecordsCount++;
      }
    });

    totalFetched += newRecordsCount;
    console.error(`[CURSOR] Iterazione ${iterationCount}: ${rows.length} record ricevuti, ${newRecordsCount} nuovi. Totale: ${totalFetched}`);

    // Gestione cicli consecutivi senza nuovi record
    if (newRecordsCount === 0) {
      consecutiveEmptyIterations++;
      console.error(`[CURSOR-WARN] Nessun nuovo record nell'iterazione ${iterationCount}. Cicli consecutivi vuoti: ${consecutiveEmptyIterations}/${maxConsecutiveEmpty}`);
    } else {
      consecutiveEmptyIterations = 0; // Reset contatore se abbiamo trovato nuovi record
    }

    // Se abbiamo ricevuto meno di 10 record (il massimo per questi endpoint), 
    // probabilmente abbiamo raggiunto la fine
    if (rows.length < 10) {
      console.error(`[CURSOR] Completato: ricevuti solo ${rows.length} record (< 10). Totale recuperato: ${totalFetched}`);
      break;
    }

    // Ordina i record per timestamp per trovare il più recente
    const sortedRows = [...rows].sort((a, b) => {
      const timeA = Number(a.time || 0);
      const timeB = Number(b.time || 0);
      return timeB - timeA; // Ordinamento decrescente (più recente prima)
    });

    // Prepara il prossimo startTime: timestamp del record più recente + 1ms
    const mostRecentRecord = sortedRows[0]; // Il primo dopo l'ordinamento decrescente
    if (mostRecentRecord && mostRecentRecord.time) {
      // Conversione esplicita a numero per evitare concatenazione stringa
      const mostRecentTime = Number(mostRecentRecord.time);
      const newStartTime = mostRecentTime + 1;
      
      console.error(`[CURSOR-DEBUG] Record più recente: ${mostRecentRecord.time} (tipo: ${typeof mostRecentRecord.time}), convertito: ${mostRecentTime}, prossimo startTime: ${newStartTime}`);
      
      // Verifica che stiamo avanzando nel tempo
      if (newStartTime <= currentStartTime) {
        console.error(`[CURSOR-WARN] Il nuovo startTime (${newStartTime}) non è maggiore del precedente (${currentStartTime}). Rischio loop infinito. Esco.`);
        break;
      }
      
      currentStartTime = newStartTime;
      
      // Verifica che non superiamo l'endTime se specificato
      if (endTime && currentStartTime > endTime) {
        console.error(`[CURSOR] Completato: prossimo startTime (${new Date(currentStartTime).toISOString()}) supera endTime. Totale recuperato: ${totalFetched}`);
        break;
      }
    } else {
      console.error(`[CURSOR-WARN] Record senza timestamp trovato, impossibile continuare`);
      break;
    }
  }

  // Logging finale con motivo di uscita
  if (iterationCount >= maxIterations) {
    console.error(`[CURSOR-WARN] Raggiunto limite massimo iterazioni (${maxIterations}). Totale recuperato: ${totalFetched}`);
  } else if (consecutiveEmptyIterations >= maxConsecutiveEmpty) {
    console.error(`[CURSOR-WARN] Raggiunto limite cicli consecutivi senza nuovi record (${maxConsecutiveEmpty}). Totale recuperato: ${totalFetched}`);
  }

  console.error(`[CURSOR] Fetch completato per ${endpoint}. Totale record recuperati: ${totalFetched} in ${iterationCount} iterazioni`);
}

//Funzione che va all'indietro con gli indici, in questo caso l'end time
async function fetchWithTimestampCursor(exchange, endpoint, params, rateLimiter, seenIds, out, idFn, maxRecords = 10000) {
  const { startTime, ...baseParams } = params;
  let currentEndTime = params.endTime;
  let totalFetched = 0;
  let iterationCount = 0;
  let consecutiveEmptyIterations = 0;
  const maxIterations = Math.ceil(maxRecords / 10) + 100;
  const maxConsecutiveEmpty = 3;

  console.error(`[CURSOR] Inizio fetch con cursor endTime per ${endpoint}, endTime: ${new Date(currentEndTime).toISOString()} startTime: ${new Date(startTime).toISOString()}`);

  while (
    totalFetched < maxRecords &&
    iterationCount < maxIterations &&
    consecutiveEmptyIterations < maxConsecutiveEmpty &&
    (!startTime || currentEndTime > startTime)
  ) {
    iterationCount++;

    if (rateLimiter) await rateLimiter.waitForRateLimit();

    const requestParams = {
      ...baseParams,
      startTime: startTime,
      endTime: currentEndTime,
    };

    let res;
    try {
      res = await exchange.request(endpoint, 'sapi', 'GET', requestParams);
    } catch (error) {
      console.error(`[CURSOR-ERROR] Errore API per endTime ${new Date(currentEndTime).toISOString()}: ${error.message}`);
      break;
    }

    console.error(`[API-RESPONSE] ${endpoint} cursor iter=${iterationCount} endTime=${new Date(currentEndTime).toISOString()}: ${JSON.stringify(res, null, 2)}`);

    const rows = res?.rows ?? (Array.isArray(res) ? res : []);
    if (!rows || rows.length === 0) {
      console.error(`[CURSOR] Completato: nessun record trovato all'iterazione ${iterationCount}. Totale recuperato: ${totalFetched}`);
      break;
    }

    // Aggiunta record NON doppi
    let newRecordsCount = 0;
    rows.forEach(r => {
      const id = idFn(r);
      if (!seenIds.has(id)) {
        seenIds.add(id);
        out.push(r);
        newRecordsCount++;
      }
    });
    totalFetched += newRecordsCount;

    if (newRecordsCount === 0) {
      consecutiveEmptyIterations++;
      console.error(`[CURSOR-WARN] Nessun nuovo record iter ${iterationCount}. Cicli consecutivi vuoti: ${consecutiveEmptyIterations}/${maxConsecutiveEmpty}`);
    } else {
      consecutiveEmptyIterations = 0;
    }

    if (rows.length < 10) {
      console.error(`[CURSOR] Completato: ricevuti solo ${rows.length} record (< 10). Totale recuperato: ${totalFetched}`);
      break;
    }

    // Trovare il record più vecchio
    const sortedRows = [...rows].sort((a, b) => Number(a.time || 0) - Number(b.time || 0));
    const oldestRecord = sortedRows[0];
    if (oldestRecord && oldestRecord.time) {
      const oldestTime = Number(oldestRecord.time);
      if (oldestTime <= startTime) {
        console.error(`[CURSOR] Completato: raggiunto startTime (${new Date(startTime).toISOString()})`);
        break;
      }
      currentEndTime = oldestTime - 1;
      console.error(`[CURSOR-DEBUG] Vecchio record time: ${oldestTime}, prossimo endTime: ${currentEndTime}`);
    } else {
      console.error(`[CURSOR-WARN] Record senza timestamp trovato`);
      break;
    }
  }

  if (iterationCount >= maxIterations) {
    console.error(`[CURSOR-WARN] Raggiunto limite massimo iterazioni (${maxIterations}). Totale recuperato: ${totalFetched}`);
  } else if (consecutiveEmptyIterations >= maxConsecutiveEmpty) {
    console.error(`[CURSOR-WARN] Raggiunto limite cicli consecutivi senza nuovi record (${maxConsecutiveEmpty}).`);
  }

  console.error(`[CURSOR] Fetch BACKWARD completato. Totale record: ${totalFetched} in ${iterationCount} iterazioni`);
}

// STRATEGIA TEMPORALE OTTIMIZZATA con protezioni aggiuntive

async function fetchWithTemporalPagination(exchange, endpoint, params, rateLimiter, seenIds, out, idFn, maxDepth = 10) {

  const { startTime, endTime, ...otherParams } = params;

  // Se non ci sono range temporali, usa strategia semplice

  if (!startTime || !endTime) {

    if (rateLimiter) await rateLimiter.waitForRateLimit();

    const res = await exchange.request(endpoint, 'sapi', 'GET', params);

    console.error(`[API-RESPONSE] ${endpoint}: ${JSON.stringify(res, null, 2)}`);

    const rows = res?.rows ?? (Array.isArray(res) ? res : []);

    rows.forEach(r => {

      const id = idFn(r);

      if (!seenIds.has(id)) {

        seenIds.add(id);

        out.push(r);

      }

    });

    return;

  }

  // STRATEGIA TEMPORALE: suddividi in finestre piccole fino a ottenere tutti i record

  await fetchTemporalRange(exchange, endpoint, startTime, endTime, otherParams, rateLimiter, seenIds, out, idFn, 0, maxDepth);

}

async function fetchTemporalRange(exchange, endpoint, start, end, params, rateLimiter, seenIds, out, idFn, depth = 0, maxDepth = 10) {

  // Protezione da ricorsione infinita

  if (depth > maxDepth) {

    console.error(`[ERROR] Massima profondità ricorsione raggiunta (${maxDepth}). Range: ${new Date(start).toISOString()} -> ${new Date(end).toISOString()}`);

    return;

  }

  // Protezione da range temporali troppo piccoli (meno di 1 minuto)

  if (end - start < 60000) {

    console.error(`[WARN] Range temporale troppo piccolo (${(end-start)/1000}s). Salto il range.`);

    return;

  }

  if (rateLimiter) await rateLimiter.waitForRateLimit();

  const requestParams = { ...params, startTime: start, endTime: end };

  let res;

  try {

    res = await exchange.request(endpoint, 'sapi', 'GET', requestParams);

  } catch (error) {

    console.error(`[ERROR] Errore API per range ${new Date(start).toISOString()} -> ${new Date(end).toISOString()}: ${error.message}`);

    return;

  }

  console.error(`[API-RESPONSE] ${endpoint} depth=${depth} (${new Date(start).toISOString()} -> ${new Date(end).toISOString()}): ${JSON.stringify(res, null, 2)}`);

  const rows = res?.rows ?? (Array.isArray(res) ? res : []);

  const total = parseInt(res?.total ?? '0') || rows.length;

  // Conta quanti nuovi record stiamo per aggiungere

  const newRows = rows.filter(r => !seenIds.has(idFn(r)));

  // CONDIZIONI PER PROCESSARE DIRETTAMENTE (senza suddividere ulteriormente)

  const shouldProcess = (

    rows.length >= total ||  // Abbiamo tutti i record dichiarati

    rows.length <= 10 ||     // Pochi record, non c'è rischio di troncamento

    newRows.length === 0 ||  // Tutti duplicati, non serve dividere

    total <= 10 ||           // Total basso, probabilmente completo

    depth >= maxDepth - 1    // Vicini al limite di ricorsione

  );

  if (shouldProcess) {

    console.error(`[DEBUG] Processo range diretto: ${rows.length}/${total} record (${newRows.length} nuovi) depth=${depth}`);

    rows.forEach(r => {

      const id = idFn(r);

      if (!seenIds.has(id)) {

        seenIds.add(id);

        out.push(r);

      }

    });

    if (rows.length < total && depth < 3) {

      console.error(`[WARN] Possibili record mancanti: ${rows.length}/${total}. Profondità: ${depth}`);

    }

  } else {

    // CASO PROBLEMATICO: total > rows.length e abbiamo margine per dividere

    console.error(`[WARN] Range troppo grande: ${rows.length}/${total} (${newRows.length} nuovi). Suddivido... depth=${depth}`);

    const rangeDuration = end - start;

    const midPoint = start + Math.floor(rangeDuration / 2);

    if (midPoint <= start || midPoint >= end || rangeDuration < 120000) { // Minimo 2 minuti

      // Non possiamo dividere ulteriormente, prendiamo quello che abbiamo

      console.error(`[WARN] Impossibile suddividere range di ${rangeDuration/1000}s. Accetto ${rows.length}/${total} record.`);

      rows.forEach(r => {

        const id = idFn(r);

        if (!seenIds.has(id)) {

          seenIds.add(id);

          out.push(r);

        }

      });

    } else {

      // Ricorsione: dividi in due metà

      console.error(`[DEBUG] Divido range depth=${depth}: [${new Date(start).toISOString()} -> ${new Date(midPoint).toISOString()}] + [${new Date(midPoint + 1).toISOString()} -> ${new Date(end).toISOString()}]`);

      await fetchTemporalRange(exchange, endpoint, start, midPoint, params, rateLimiter, seenIds, out, idFn, depth + 1, maxDepth);

      await fetchTemporalRange(exchange, endpoint, midPoint + 1, end, params, rateLimiter, seenIds, out, idFn, depth + 1, maxDepth);

    }

  }

}

async function fetchAllSimpleEarnFlexibleRaw(exchange, rateLimiter, globalStartTime, globalEndTime) {

  const out = [];

  const seenIds = new Set();

  let positions = [];

  try {

    if (rateLimiter) await rateLimiter.waitForRateLimit();

    const res = await exchange.request('simple-earn/flexible/position', 'sapi', 'GET', { recvWindow: 60000 });

    console.error(`[API-RESPONSE] simple-earn/flexible/position: ${JSON.stringify(res, null, 2)}`);

    positions = res?.rows ?? (Array.isArray(res) ? res : []);

    console.error(`[DEBUG] Flexible positions ricevute: ${positions.length}`);

  } catch (e) {

    console.error(`[Node-LOG] Errore fetch Flexible positions: ${e.message}`);

  }

  // rewardRecord

    // 1. Ottieni tutti i productId dal subscriptionRecord
    const subs = [];
    await fetchWithTimestampCursor(
            exchange,
            'simple-earn/flexible/history/subscriptionRecord',
            {startTime: globalStartTime, endTime: globalEndTime, recvWindow: 60000},
            rateLimiter,
            new Set(), subs,
            r => r.purchaseId      // idFn per deduplicare
    );

    const uniqueProducts = [...new Set(subs.map(s => s.productId))];

// 2. Per ogni productId interroga rewardRecord
    for (const productId of uniqueProducts) {
        let currentStart = globalStartTime;
        while (currentStart < globalEndTime) {
            const currentEnd = Math.min(currentStart + 90 * 24 * 60 * 60 * 1000, globalEndTime);

            await fetchWithTemporalPagination(
                    exchange,
                    'simple-earn/flexible/history/rewardRecord',
                    {productId, startTime: currentStart, endTime: currentEnd, recvWindow: 60000},
                    rateLimiter, seenIds, out,
                    r => `${r.time}-${r.asset}-${r.amount}-${productId}`
            );

            currentStart = currentEnd + 1;
        }
    }

  // Fallback subscriptionRecord con timestamp cursor (sostituisce la logica a finestre temporali)

  if (out.length === 0) {

    console.error(`[WARN] 0 rewards - provo subscriptionRecord per Flexible con timestamp cursor`);

    await fetchWithTimestampCursor(exchange, 'simple-earn/flexible/history/subscriptionRecord',

      { startTime: globalStartTime, endTime: globalEndTime, recvWindow: 60000 },

      rateLimiter, seenIds, out, r => `${r.purchaseId || r.time}-${r.time}-${r.asset}-${r.amount}`);

  }

  const filteredOut = out.filter(item => item.time >= globalStartTime && item.time <= globalEndTime);

  console.error(`[Node-LOG] SimpleEarn Flexible totali: ${filteredOut.length}`);

  return filteredOut;

}

async function fetchAllSimpleEarnLockedRaw(exchange, rateLimiter, globalStartTime, globalEndTime) {

  const out = [];

  const seenIds = new Set();

  let positions = [];

  try {

    if (rateLimiter) await rateLimiter.waitForRateLimit();

    const res = await exchange.request('simple-earn/locked/position', 'sapi', 'GET', { recvWindow: 60000 });

    console.error(`[API-RESPONSE] simple-earn/locked/position: ${JSON.stringify(res, null, 2)}`);

    positions = res?.rows ?? (Array.isArray(res) ? res : []);

    console.error(`[DEBUG] Locked positions ricevute: ${positions.length}`);

  } catch (e) {

    console.error(`[Node-LOG] Errore fetch Locked positions: ${e.message}`);

  }

  // rewardRecord

  for (const pos of positions) {

    const positionId = pos.positionId || pos.productId;

    if (!positionId) continue;

    let currentStart = globalStartTime;

    while (currentStart < globalEndTime) {

      const currentEnd = Math.min(currentStart + (90 * 24 * 60 * 60 * 1000), globalEndTime);

      await fetchWithTemporalPagination(exchange, 'simple-earn/locked/history/rewardRecord',

        { positionId, startTime: currentStart, endTime: currentEnd, recvWindow: 60000 },

        rateLimiter, seenIds, out, r => `${r.time}-${r.asset}-${r.amount}-${r.positionId || ''}`);

      currentStart = currentEnd + 1;

    }

  }

  // Fallback subscriptionRecord con timestamp cursor (sostituisce la logica a finestre temporali)

  if (out.length === 0) {

    console.error(`[WARN] 0 rewards - provo subscriptionRecord per Locked con timestamp cursor`);

    await fetchWithTimestampCursor(exchange, 'simple-earn/locked/history/subscriptionRecord',

      { startTime: globalStartTime, endTime: globalEndTime, recvWindow: 60000 },

      rateLimiter, seenIds, out, r => `${r.purchaseId || r.time}-${r.time}-${r.asset}-${r.amount}`);

  }

  const filteredOut = out.filter(item => item.time >= globalStartTime && item.time <= globalEndTime);

  console.error(`[Node-LOG] SimpleEarn Locked totali: ${filteredOut.length}`);

  return filteredOut;

}

function logDebug(message) { console.error(`[DEBUG] ${new Date().toISOString()}: ${message}`); }


async function fetchAllStakingInterestsRaw(exchange, product, rateLimiter, globalStartTime, globalEndTime) {

  const out = [];

  const seenIds = new Set();

  const WINDOW_MS = 90 * 24 * 60 * 60 * 1000;

  let currentStart = Math.max(globalStartTime, EPOCH_ALL_HISTORY);

  let windowIdx = 0;

  const totalWindows = Math.ceil((globalEndTime - currentStart) / WINDOW_MS);

  while (currentStart < globalEndTime) {

    windowIdx++;

    const currentEnd = Math.min(currentStart + WINDOW_MS, globalEndTime);

    console.error(`[Node-LOG] Staking ${product} finestra ${windowIdx}/${totalWindows} (${new Date(currentStart).toISOString()} -> ${new Date(currentEnd).toISOString()})`);

    await fetchWithTemporalPagination(exchange, 'staking/stakingRecord',

      { txnType: 'INTEREST', product, startTime: currentStart, endTime: currentEnd, recvWindow: 60000 },

      rateLimiter, seenIds, out, r => r.positionId || `${r.time}-${r.asset}-${r.amount}`);

    currentStart = currentEnd + 1;

  }
  
  const filteredOut = out.filter(item => item.time >= globalStartTime && item.time <= globalEndTime);

  console.error(`[Node-LOG] Staking ${product} totali: ${filteredOut.length}`);

  return filteredOut;

}

async function main() {

  const [,, exchangeId, apiKey, secret, startDateArg = "2017-01-01"] = process.argv;

  let startTime = new Date(startDateArg).getTime();

  const endTime = Date.now();

  if (startTime < EPOCH_ALL_HISTORY) startTime = EPOCH_ALL_HISTORY;

  const ExchangeClass = ccxt[exchangeId];

  const exchange = new ExchangeClass({ apiKey, secret, enableRateLimit: false, options: { recvWindow: 60000 }, timeout: 60000 });

  const rateLimiter = exchangeId === 'binance' ? new BinanceRateLimiter() : null;

  if (exchangeId === 'binance') {

    try { await exchange.loadTimeDifference(); exchange.options.adjustForTimeDifference = true; } catch (e) {}

  }

  let earnFlexible = [], earnLocked = [], staking = [];

  if (exchangeId === 'binance') {

    //earnFlexible = await fetchAllSimpleEarnFlexibleRaw(exchange, rateLimiter, startTime, endTime);

    //earnLocked = await fetchAllSimpleEarnLockedRaw(exchange, rateLimiter, startTime, endTime);

    const stakingStaking = await fetchAllStakingInterestsRaw(exchange, 'STAKING', rateLimiter, startTime, endTime);

    const stakingLDefi = await fetchAllStakingInterestsRaw(exchange, 'L_DEFI', rateLimiter, startTime, endTime);

    const stakingFDefi = await fetchAllStakingInterestsRaw(exchange, 'F_DEFI', rateLimiter, startTime, endTime);

    staking = [...stakingStaking, ...stakingLDefi, ...stakingFDefi];

  }

  console.log(JSON.stringify({ earnFlexible, earnLocked, staking }));

}

main().catch(err => {

  console.log(JSON.stringify({ earnFlexible: [], earnLocked: [], staking: [], error: err.message }));

});

