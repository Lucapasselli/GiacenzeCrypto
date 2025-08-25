// npm i ccxt@latest
const ccxt = require('ccxt');

// ======================= Costanti & Utils =======================

const EPOCH_ALL_HISTORY = new Date('2017-01-01T00:00:00Z').getTime();

function logProg(msg) { console.error(`[PROG] ${msg}`); }

// ======================= Rate Limiting =======================

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

  async sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

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

// ======================= Fetch Withdrawals =======================

async function fetchAllWithdrawalsRaw(exchange, rateLimiter, globalStartTime, globalEndTime) {
  const out = [];
  const seenIds = new Set();
  const limit = 1000;
  const WINDOW_MS = 90 * 24 * 60 * 60 * 1000;
  const MAX_PAGES_PER_WINDOW = 10;
  let currentStart = Math.max(globalStartTime, EPOCH_ALL_HISTORY);
  let windowIdx = 0;
  const totalWindows = Math.ceil((globalEndTime - currentStart) / WINDOW_MS);

  while (currentStart < globalEndTime) {
    windowIdx++;
    const currentEnd = Math.min(currentStart + WINDOW_MS, globalEndTime);
    console.error(`[Node-LOG] Prelievi finestra ${windowIdx}/${totalWindows} (${new Date(currentStart).toISOString()} -> ${new Date(currentEnd).toISOString()})`);
    
    let page = 0;
    let hasMore = true;
    while (hasMore && page < MAX_PAGES_PER_WINDOW) {
      page++;
      if (rateLimiter) await rateLimiter.waitForRateLimit();
      
      let attempt = 0;
      let pageData = [];
      while (attempt < 5) {
        try {
          pageData = await exchange.request('capital/withdraw/history', 'sapi', 'GET', { 
            limit, 
            startTime: currentStart, 
            endTime: currentEnd, 
            recvWindow: 60000 
          });
          console.error(`[DEBUG] Finestra ${windowIdx}, pagina ${page}: Richiesti ${limit}, Ricevuti ${pageData.length}`);
          break;
        } catch (e) {
          if (rateLimiter && await rateLimiter.handleRateLimit(e, attempt)) {
            attempt++;
            continue;
          }
          console.error(`[Node-LOG] Withdrawals RAW errore (attempt ${attempt}): ${e.message}`);
          hasMore = false;
          break;
        }
      }
      
      if (!Array.isArray(pageData) || pageData.length === 0) {
        console.error(`[DEBUG] Pagina vuota (0 ricevuti) -> fine finestra ${windowIdx}`);
        hasMore = false;
        break;
      }

      let newCount = 0;
      let duplicates = 0;
      for (const w of pageData) {
        const id = w.id || w.txId || w.applyTime;
        if (seenIds.has(id)) {
          duplicates++;
          continue;
        }
        // Converte la data in ms (creo i campi corretti per il json)
        //w.insertTime = Date.parse(w.applyTime);//Questo serve per rendere compatibile la cosa con l'output dei depositi
       // w.completeTime = Date.parse(w.completeTime);//Questo serve per rendere compatibile la cosa con l'output dei depositi
        w.insertTime = normalizeBinanceTime(w.applyTime);   // compatibile con i depositi
        w.completeTime = normalizeBinanceTime(w.completeTime);
        
        seenIds.add(id);
        out.push(w);
        newCount++;
      }
      logProg(`Prelievi RAW - finestra ${windowIdx}, pagina ${page}: +${newCount} nuovi (duplicati skippati: ${duplicates}), tot=${out.length}`);
      
      if (pageData.length < limit) {
        console.error(`[DEBUG] Ricevuti < limit (${pageData.length} < ${limit}) -> fine finestra ${windowIdx}`);
        hasMore = false;
      }
    }
    
    currentStart = currentEnd + 1;
  }
  
  const filteredOut = out.filter(item => {
    const ts = Date.parse(item.applyTime);
    return ts >= globalStartTime && ts <= globalEndTime;
  });
  console.error(`[Node-LOG] Prelievi totali dopo filtro: ${filteredOut.length}`);
  return filteredOut;
}



function normalizeBinanceTime(value) {
  if (!value) return null;

  // Se è già un numero (epoch in ms)
  if (!isNaN(value)) {
    return Number(value);
  }

  // Se è una stringa (es: "2025-08-25 09:00:00")
  if (typeof value === 'string') {
    // ISO già con Z o offset (+hh:mm / -hhmm)
    if (/T.*Z$/.test(value) || /[+-]\d\d:?(\d\d)?$/.test(value)) {
      return new Date(value).getTime();
    }
    // "YYYY-MM-DD HH:mm:ss" (documentato come UTC)
    const m = value.match(
      /^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2}):(\d{2})(?:\.\d+)?$/
    );
    if (m) {
      const [, y, mo, d, h, mi, se] = m;
      return Date.UTC(+y, +mo - 1, +d, +h, +mi, +se);
    }
    // fallback
    return Date.parse(value);
  }

  return null;
}


// ======================= MAIN =======================

async function main() {
  const [,, exchangeId, apiKey, secret, startDateArg = "1483228800000"] = process.argv;
  let startTime = Number(startDateArg);
  //let startTime = new Date(startDateArg).getTime();
  const endTime = Date.now();

  if (startTime < EPOCH_ALL_HISTORY) {
    startTime = EPOCH_ALL_HISTORY;
  }

  const ExchangeClass = ccxt[exchangeId];
  const exchange = new ExchangeClass({ apiKey, secret, enableRateLimit: true, options: { recvWindow: 60000 }, timeout: 60000 });

  const rateLimiter = exchangeId === 'binance' ? new BinanceRateLimiter() : null;

  if (exchangeId === 'binance') {
    try {
      await exchange.loadTimeDifference();
      exchange.options.adjustForTimeDifference = true;
    } catch (e) {}
  }

  const withdrawals = exchangeId === 'binance' ? await fetchAllWithdrawalsRaw(exchange, rateLimiter, startTime, endTime) : await exchange.fetchWithdrawals(undefined, startTime).catch(() => []);

  console.log(JSON.stringify({ withdrawals }));
}

main().catch(err => {
  console.log(JSON.stringify({ withdrawals: [], error: err.message }));
});