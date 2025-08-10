// npm i ccxt@latest
const ccxt = require('ccxt');

// ======================= Costanti & Utils =======================

const COMMON_QUOTES = new Set([
  'USDT','FDUSD','BUSD','USDC','TUSD','DAI',
  'BTC','BNB','ETH',
  'EUR','TRY','GBP','AUD','BRL','RUB','UAH'
]);
const PREFERRED_QUOTES = ['USDT','FDUSD','USDC','BUSD','BTC','BNB','ETH','EUR','TRY'];

// Tutto lo storico da 2017-01-01 (EPOCH ms)
const EPOCH_ALL_HISTORY = new Date('2017-01-01T00:00:00Z').getTime();

function pickAssetField(o) { return o?.asset || o?.coin || o?.token || o?.rewardAsset || o?.currency || null; }
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
    
    // Pausa minima tra richieste: 2 secondi
    const timeSinceLastRequest = now - this.lastRequest;
    if (timeSinceLastRequest < 2000) {
      const waitTime = 2000 - timeSinceLastRequest;
      console.error(`[RATE] Pausa inter-request: ${waitTime}ms`);
      await this.sleep(waitTime);
    }
    
    // Ogni 40 richieste, pausa lunga (per stare sotto 300/5min)
    if (this.requestCount % 40 === 0) {
      console.error(`[RATE] 🛑 Pausa lunga dopo ${this.requestCount} richieste (60s)`);
      await this.sleep(60000);
      this.startTime = Date.now(); // reset timer
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
      const waitTime = Math.min(30000 * (2 ** attempt), 300000); // max 5min
      console.error(`[RATE] ⚠️ Rate limit hit! Pausa: ${waitTime/1000}s (attempt ${attempt+1})`);
      await this.sleep(waitTime);
      return true;
    }
    return false;
  }
}

// ======================= RAW SAPI helpers (solo Binance) =======================

async function fetchAllDepositsRaw(exchange, rateLimiter, globalStartTime, globalEndTime) {
  const out = [];
  const seenIds = new Set(); // Per deduplicazione
  const limit = 1000;
  const WINDOW_MS = 90 * 24 * 60 * 60 * 1000; // 90 giorni in ms
  const MAX_PAGES_PER_WINDOW = 10; // Sicurezza anti-loop
  let currentStart = Math.max(globalStartTime, EPOCH_ALL_HISTORY);
  let windowIdx = 0;
  const totalWindows = Math.ceil((globalEndTime - currentStart) / WINDOW_MS);

  while (currentStart < globalEndTime) {
    windowIdx++;
    const currentEnd = Math.min(currentStart + WINDOW_MS, globalEndTime);
    console.error(`[Node-LOG] Depositi finestra ${windowIdx}/${totalWindows} (${new Date(currentStart).toISOString()} -> ${new Date(currentEnd).toISOString()})`);
    
    let page = 0;
    let hasMore = true;
    while (hasMore && page < MAX_PAGES_PER_WINDOW) {
      page++;
      await rateLimiter.waitForRateLimit();
      
      let attempt = 0;
      let pageData = [];
      while (attempt < 5) {
        try {
          pageData = await exchange.request('capital/deposit/hisrec', 'sapi', 'GET', { 
            limit, 
            startTime: currentStart, 
            endTime: currentEnd, 
            recvWindow: 60000 
          });
          console.error(`[DEBUG] Finestra ${windowIdx}, pagina ${page}: Richiesti ${limit}, Ricevuti ${pageData.length}`);
          break;
        } catch (e) {
          if (await rateLimiter.handleRateLimit(e, attempt)) {
            attempt++;
            continue;
          }
          console.error(`[Node-LOG] Deposits RAW errore (attempt ${attempt}): ${e.message}`);
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
      for (const d of pageData) {
        const id = d.id || d.txId || d.insertTime; // Usa ID unico
        if (seenIds.has(id)) {
          duplicates++;
          continue;
        }
        seenIds.add(id);
        out.push(d);
        newCount++;
      }
      logProg(`Depositi RAW - finestra ${windowIdx}, pagina ${page}: +${newCount} nuovi (duplicati skippati: ${duplicates}), tot=${out.length}`);
      
      if (pageData.length < limit) {
        console.error(`[DEBUG] Ricevuti < limit (${pageData.length} < ${limit}) -> fine finestra ${windowIdx}`);
        hasMore = false;
      }
    }
    
    if (page >= MAX_PAGES_PER_WINDOW) {
      console.error(`[WARN] Raggiunto max pagine (${MAX_PAGES_PER_WINDOW}) per finestra ${windowIdx} - possibile loop, procedo alla successiva`);
    }
    
    currentStart = currentEnd + 1;
  }
  
  // Filtro post-raccolta per range temporale (sicurezza)
  const filteredOut = out.filter(item => item.insertTime >= globalStartTime && item.insertTime <= globalEndTime);
  console.error(`[Node-LOG] Depositi totali dopo filtro temporale e dedup: ${filteredOut.length} (su ${out.length} grezzi)`);
  return filteredOut;
}

async function fetchAllWithdrawalsRaw(exchange, rateLimiter, globalStartTime, globalEndTime) {
  const out = [];
  const seenIds = new Set(); // Per deduplicazione
  const limit = 1000;
  const WINDOW_MS = 90 * 24 * 60 * 60 * 1000; // 90 giorni in ms
  const MAX_PAGES_PER_WINDOW = 10; // Sicurezza anti-loop
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
      await rateLimiter.waitForRateLimit();
      
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
          if (await rateLimiter.handleRateLimit(e, attempt)) {
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
        const id = w.id || w.txId || w.applyTime; // Usa ID unico
        if (seenIds.has(id)) {
          duplicates++;
          continue;
        }
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
    
    if (page >= MAX_PAGES_PER_WINDOW) {
      console.error(`[WARN] Raggiunto max pagine (${MAX_PAGES_PER_WINDOW}) per finestra ${windowIdx} - possibile loop, procedo alla successiva`);
    }
    
    currentStart = currentEnd + 1;
  }
  
  // Filtro post-raccolta per range temporale (sicurezza)
  const filteredOut = out.filter(item => {
    const ts = Date.parse(item.applyTime); // Converti string a ms
    return ts >= globalStartTime && ts <= globalEndTime;
  });
  console.error(`[Node-LOG] Prelievi totali dopo filtro temporale e dedup: ${filteredOut.length} (su ${out.length} grezzi)`);
  return filteredOut;
}

async function fetchAllSimpleEarnFlexibleRaw(exchange, rateLimiter, globalStartTime, globalEndTime) {
  const out = [];
  const seenIds = new Set();

  // Step 1: Fetch all flexible positions
  let positions = [];
  try {
    await rateLimiter.waitForRateLimit();
    const res = await exchange.request('simple-earn/flexible/position', 'sapi', 'GET', { recvWindow: 60000 });
    positions = res?.rows ?? (Array.isArray(res) ? res : []);
    console.error(`[DEBUG] Flexible positions ricevute: ${positions.length}`);
    if (positions.length === 0) {
      console.error(`[WARN] Nessuna position Flexible trovata - risposta raw: ${JSON.stringify(res)}`);
    }
  } catch (e) {
    console.error(`[Node-LOG] Errore fetch Flexible positions: ${e.message}`);
    return out;
  }

  // Step 2: For each position, fetch reward records with time range
  for (const pos of positions) {
    const positionId = pos.positionId;
    if (!positionId) continue;
    console.error(`[DEBUG] Fetch rewards per Flexible positionId: ${positionId}`);
    let currentStart = globalStartTime;
    while (currentStart < globalEndTime) {
      const currentEnd = Math.min(currentStart + (90 * 24 * 60 * 60 * 1000), globalEndTime);
      await rateLimiter.waitForRateLimit();
      let attempt = 0;
      while (attempt < 5) {
        try {
          const res = await exchange.request('simple-earn/flexible/history/rewardRecord', 'sapi', 'GET', {
            positionId,
            startTime: currentStart,
            endTime: currentEnd,
            limit: 500,
            recvWindow: 60000
          });
          const rows = res?.rows ?? (Array.isArray(res) ? res : []);
          for (const r of rows) {
            const id = `${r.time}-${r.asset}-${r.amount}`;
            if (!seenIds.has(id)) {
              seenIds.add(id);
              out.push(r);
            }
          }
          break;
        } catch (e) {
          if (await rateLimiter.handleRateLimit(e, attempt)) {
            attempt++;
            continue;
          }
          console.error(`[Node-LOG] Errore rewards Flexible per position ${positionId}: ${e.message}`);
          break;
        }
      }
      currentStart = currentEnd + 1;
    }
  }

  // Fallback if 0: Try general rewards without positionId
  if (out.length === 0) {
    console.error(`[WARN] 0 rewards da positions - provo fetch generale senza positionId`);
    let currentStart = globalStartTime;
    while (currentStart < globalEndTime) {
      const currentEnd = Math.min(currentStart + (90 * 24 * 60 * 60 * 1000), globalEndTime);
      await rateLimiter.waitForRateLimit();
      try {
        const res = await exchange.request('simple-earn/flexible/history/rewards', 'sapi', 'GET', {
          startTime: currentStart,
          endTime: currentEnd,
          limit: 500,
          recvWindow: 60000
        });
        const rows = res?.rows ?? (Array.isArray(res) ? res : []);
        for (const r of rows) {
          const id = `${r.time}-${r.asset}-${r.amount}`;
          if (!seenIds.has(id)) {
            seenIds.add(id);
            out.push(r);
          }
        }
      } catch (e) {
        console.error(`[Node-LOG] Errore fallback Flexible: ${e.message}`);
      }
      currentStart = currentEnd + 1;
    }
  }

  const filteredOut = out.filter(item => item.time >= globalStartTime && item.time <= globalEndTime);
  console.error(`[Node-LOG] SimpleEarn Flexible totali dopo filtro e dedup: ${filteredOut.length}`);
  return filteredOut;
}

async function fetchAllSimpleEarnLockedRaw(exchange, rateLimiter, globalStartTime, globalEndTime) {
  const out = [];
  const seenIds = new Set();

  // Step 1: Fetch all locked positions
  let positions = [];
  try {
    await rateLimiter.waitForRateLimit();
    const res = await exchange.request('simple-earn/locked/position', 'sapi', 'GET', { recvWindow: 60000 });
    positions = res?.rows ?? (Array.isArray(res) ? res : []);
    console.error(`[DEBUG] Locked positions ricevute: ${positions.length}`);
    if (positions.length === 0) {
      console.error(`[WARN] Nessuna position Locked trovata - risposta raw: ${JSON.stringify(res)}`);
    }
  } catch (e) {
    console.error(`[Node-LOG] Errore fetch Locked positions: ${e.message}`);
    return out;
  }

  // Step 2: For each position, fetch reward records with time range
  for (const pos of positions) {
    const positionId = pos.positionId;
    if (!positionId) continue;
    console.error(`[DEBUG] Fetch rewards per Locked positionId: ${positionId}`);
    let currentStart = globalStartTime;
    while (currentStart < globalEndTime) {
      const currentEnd = Math.min(currentStart + (90 * 24 * 60 * 60 * 1000), globalEndTime);
      await rateLimiter.waitForRateLimit();
      let attempt = 0;
      while (attempt < 5) {
        try {
          const res = await exchange.request('simple-earn/locked/history/rewardRecord', 'sapi', 'GET', {
            positionId,
            startTime: currentStart,
            endTime: currentEnd,
            limit: 500,
            recvWindow: 60000
          });
          const rows = res?.rows ?? (Array.isArray(res) ? res : []);
          for (const r of rows) {
            const id = `${r.time}-${r.asset}-${r.amount}`;
            if (!seenIds.has(id)) {
              seenIds.add(id);
              out.push(r);
            }
          }
          break;
        } catch (e) {
          if (await rateLimiter.handleRateLimit(e, attempt)) {
            attempt++;
            continue;
          }
          console.error(`[Node-LOG] Errore rewards Locked per position ${positionId}: ${e.message}`);
          break;
        }
      }
      currentStart = currentEnd + 1;
    }
  }

  // Fallback if 0: Try general rewards without positionId
  if (out.length === 0) {
    console.error(`[WARN] 0 rewards da positions - provo fetch generale senza positionId`);
    let currentStart = globalStartTime;
    while (currentStart < globalEndTime) {
      const currentEnd = Math.min(currentStart + (90 * 24 * 60 * 60 * 1000), globalEndTime);
      await rateLimiter.waitForRateLimit();
      try {
        const res = await exchange.request('simple-earn/locked/history/rewards', 'sapi', 'GET', {
          startTime: currentStart,
          endTime: currentEnd,
          limit: 500,
          recvWindow: 60000
        });
        const rows = res?.rows ?? (Array.isArray(res) ? res : []);
        for (const r of rows) {
          const id = `${r.time}-${r.asset}-${r.amount}`;
          if (!seenIds.has(id)) {
            seenIds.add(id);
            out.push(r);
          }
        }
      } catch (e) {
        console.error(`[Node-LOG] Errore fallback Locked: ${e.message}`);
      }
      currentStart = currentEnd + 1;
    }
  }

  const filteredOut = out.filter(item => item.time >= globalStartTime && item.time <= globalEndTime);
  console.error(`[Node-LOG] SimpleEarn Locked totali dopo filtro e dedup: ${filteredOut.length}`);
  return filteredOut;
}

async function fetchAllStakingInterestsRaw(exchange, product, rateLimiter, globalStartTime, globalEndTime) {
  const out = [];
  const seenIds = new Set(); // Per deduplicazione
  const limit = 500;
  const WINDOW_MS = 90 * 24 * 60 * 60 * 1000; // 90 giorni in ms
  const MAX_PAGES_PER_WINDOW = 10;
  let currentStart = Math.max(globalStartTime, EPOCH_ALL_HISTORY);
  let windowIdx = 0;
  const totalWindows = Math.ceil((globalEndTime - currentStart) / WINDOW_MS);

  while (currentStart < globalEndTime) {
    windowIdx++;
    const currentEnd = Math.min(currentStart + WINDOW_MS, globalEndTime);
    console.error(`[Node-LOG] Staking ${product} finestra ${windowIdx}/${totalWindows} (${new Date(currentStart).toISOString()} -> ${new Date(currentEnd).toISOString()})`);
    
    let page = 0;
    let hasMore = true;
    while (hasMore && page < MAX_PAGES_PER_WINDOW) {
      page++;
      await rateLimiter.waitForRateLimit();
      
      let attempt = 0;
      let rows = [];
      while (attempt < 5) {
        try {
          const res = await exchange.request('staking/stakingRecord', 'sapi', 'GET', { 
            txnType: 'INTEREST',
            product,
            limit,
            startTime: currentStart, 
            endTime: currentEnd, 
            recvWindow: 60000
          });
          rows = res?.rows ?? (Array.isArray(res) ? res : []);
          console.error(`[DEBUG] Finestra ${windowIdx}, pagina ${page}: Richiesti ${limit}, Ricevuti ${rows.length}`);
          break;
        } catch (e) {
          if (await rateLimiter.handleRateLimit(e, attempt)) {
            attempt++;
            continue;
          }
          console.error(`[Node-LOG] Staking ${product} errore (attempt ${attempt}): ${e.message}`);
          hasMore = false;
          break;
        }
      }
      
      if (rows.length === 0) {
        console.error(`[DEBUG] Pagina vuota (0 ricevuti) -> fine finestra ${windowIdx}`);
        hasMore = false;
        break;
      }

      let newCount = 0;
      let duplicates = 0;
      for (const r of rows) {
        const id = r.positionId || r.time || `${r.asset}-${r.amount}`; // ID unico approssimato
        if (seenIds.has(id)) {
          duplicates++;
          continue;
        }
        seenIds.add(id);
        out.push(r);
        newCount++;
      }
      logProg(`Staking ${product} - finestra ${windowIdx}, pagina ${page}: +${newCount} nuovi (duplicati skippati: ${duplicates}), tot=${out.length}`);
      
      if (rows.length < limit) {
        console.error(`[DEBUG] Ricevuti < limit (${rows.length} < ${limit}) -> fine finestra ${windowIdx}`);
        hasMore = false;
      }
    }
    
    currentStart = currentEnd + 1;
  }
  
  // Filtro post-raccolta
  const filteredOut = out.filter(item => item.time >= globalStartTime && item.time <= globalEndTime);
  console.error(`[Node-LOG] Staking ${product} totali dopo filtro e dedup: ${filteredOut.length}`);
  return filteredOut;
}

// ======================= Discovery simboli =======================

async function discoverLikelySymbolsFromHistoryBinance(exchange, { startTime, endTime }, rateLimiter) {
  await exchange.loadMarkets();
  console.error(`[Node-LOG] Mercati TRADING caricati: ${Object.keys(exchange.markets).length}`);

  const assets = new Set();

  // Balance
  try {
    await rateLimiter.waitForRateLimit();
    const balance = await exchange.fetchBalance();
    for (const a of Object.keys(balance.total || {})) {
      if (a) assets.add(a.toUpperCase());
    }
  } catch (e) {
    console.error(`[Node-LOG] fetchBalance errore: ${e.message}`);
  }

  // Depositi/Prelievi via SAPI raw (con windowing)
  try {
    const deps = await fetchAllDepositsRaw(exchange, rateLimiter, startTime, endTime);
    for (const d of deps) {
      const coin = (d.coin || d.asset || '').toUpperCase();
      if (coin) assets.add(coin);
    }
    console.error(`[Node-LOG] Depositi raccolti: ${deps.length}`);
  } catch {}
  
  try {
    const wds = await fetchAllWithdrawalsRaw(exchange, rateLimiter, startTime, endTime);
    for (const w of wds) {
      const coin = (w.coin || w.asset || '').toUpperCase();
      if (coin) assets.add(coin);
    }
    console.error(`[Node-LOG] Prelievi raccolti: ${wds.length}`);
  } catch {}

  // Earn (Flexible e Locked)
  try {
    const flex = await fetchAllSimpleEarnFlexibleRaw(exchange, rateLimiter, startTime, endTime);
    for (const r of flex) {
      const a = pickAssetField(r);
      if (a) assets.add(String(a).toUpperCase());
    }
    console.error(`[Node-LOG] SimpleEarn Flexible: ${flex.length}`);
  } catch {}
  
  try {
    const locked = await fetchAllSimpleEarnLockedRaw(exchange, rateLimiter, startTime, endTime);
    for (const r of locked) {
      const a = pickAssetField(r);
      if (a) assets.add(String(a).toUpperCase());
    }
    console.error(`[Node-LOG] SimpleEarn Locked: ${locked.length}`);
  } catch {}
  
  // Staking (tutti i product)
  try {
    const st1 = await fetchAllStakingInterestsRaw(exchange, 'STAKING', rateLimiter, startTime, endTime);
    const st2 = await fetchAllStakingInterestsRaw(exchange, 'L_DEFI', rateLimiter, startTime, endTime);
    const st3 = await fetchAllStakingInterestsRaw(exchange, 'F_DEFI', rateLimiter, startTime, endTime);
    for (const r of [...st1, ...st2, ...st3]) {
      const a = pickAssetField(r);
      if (a) assets.add(String(a).toUpperCase());
    }
    console.error(`[Node-LOG] Staking/DeFi: ${st1.length + st2.length + st3.length}`);
  } catch {}

  console.error(`[Node-LOG] Asset totali osservati: ${assets.size}`);

  const coreAssets = new Set();
  const seenQuotes = new Set();
  for (const a of assets) {
    if (COMMON_QUOTES.has(a)) seenQuotes.add(a);
    else coreAssets.add(a);
  }
  if (coreAssets.size === 0 && seenQuotes.size > 0) {
    for (const q of seenQuotes) coreAssets.add(q);
  }

  const result = new Set();
  const markets = exchange.markets;

  // 1) base core + quote preferite
  for (const symbol of Object.keys(markets)) {
    const m = markets[symbol];
    if (m.active === false) continue;
    const base = (m.base || '').toUpperCase();
    const quote = (m.quote || '').toUpperCase();
    if (coreAssets.has(base) && PREFERRED_QUOTES.includes(quote)) result.add(m.symbol);
  }

  // 2) coppie tra core assets
  for (const symbol of Object.keys(markets)) {
    const m = markets[symbol];
    if (m.active === false) continue;
    const base = (m.base || '').toUpperCase();
    const quote = (m.quote || '').toUpperCase();
    if (coreAssets.has(base) && coreAssets.has(quote)) result.add(m.symbol);
  }

  console.error(`[Node-LOG] Simboli candidati: ${result.size}`);
  return Array.from(result);
}

// Discovery generico (altri exchange)
async function discoverLikelySymbolsGeneric(exchange) {
  await exchange.loadMarkets();
  const assets = new Set();

  try {
    const balance = await exchange.fetchBalance();
    for (const a of Object.keys(balance.total || {})) {
      if (a) assets.add(a.toUpperCase());
    }
  } catch (e) {
    console.error(`[Node-LOG] fetchBalance errore: ${e.message}`);
  }

  try {
    if (exchange.has?.fetchDeposits) {
      const deps = await exchange.fetchDeposits();
      for (const d of deps || []) {
        const c = (d.currency || d.code || '').toUpperCase();
        if (c) assets.add(c);
      }
      await exchange.sleep(300);
    }
  } catch (e) { console.error(`[Node-LOG] fetchDeposits non disponibile: ${e.message}`); }

  console.error(`[Node-LOG] Asset osservati (generic): ${assets.size}`);

  const coreAssets = new Set();
  for (const a of assets) {
    if (!COMMON_QUOTES.has(a)) coreAssets.add(a);
  }

  const result = new Set();
  const markets = exchange.markets;

  for (const symbol of Object.keys(markets)) {
    const m = markets[symbol];
    if (m.active === false) continue;
    const base = (m.base || '').toUpperCase();
    const quote = (m.quote || '').toUpperCase();
    if (coreAssets.has(base) && PREFERRED_QUOTES.includes(quote)) result.add(m.symbol);
  }

  console.error(`[Node-LOG] Simboli candidati (generic): ${result.size}`);
  return Array.from(result);
}

// ======================= TRADES =======================

async function fetchAllTradesBinance(exchange, startTime, endTime, rateLimiter) {
  await exchange.loadMarkets();

  // Discovery simboli intelligente
  const symbols = await discoverLikelySymbolsFromHistoryBinance(exchange, { startTime, endTime }, rateLimiter);
  console.error(`[Node-LOG] 🎯 Interrogo ${symbols.length} simboli (filtrati).`);

  const allTrades = [];
  const seen = new Set();
  let idx = 0;

  for (const symbol of symbols) {
    idx++;
    console.error(`▶ [${idx}/${symbols.length}] Trades per ${symbol}...`);
    let since = startTime;
    let page = 0;

    while (true) {
      page++;
      await rateLimiter.waitForRateLimit();
      
      let attempt = 0;
      let trades;
      
      // Retry logic per gestire 429
      while (attempt < 3) {
        try {
          trades = await exchange.fetchMyTrades(symbol, since, 500, { recvWindow: 60000 });
          break;
        } catch (err) {
          if (await rateLimiter.handleRateLimit(err, attempt)) {
            attempt++;
            continue;
          }
          console.error(`⚠ [Binance] Errore su ${symbol} (pag ${page}): ${err.message}`);
          trades = null;
          break;
        }
      }
      
      if (!trades || trades.length === 0) {
        console.error(`   └─ pagina ${page}: 0 risultati -> stop`);
        break;
      }

      let newCount = 0;
      for (const t of trades) {
        const key = `${t.symbol}|${t.id || t.order || t.timestamp}`;
        if (!seen.has(key)) {
          seen.add(key);
          allTrades.push(t);
          newCount++;
        }
      }
      console.error(`   └─ pagina ${page}: +${newCount} (grezzi ${trades.length}), tot=${allTrades.length}`);

      since = trades[trades.length - 1].timestamp + 1;
      if (since > endTime) break;
    }
    
    // Pausa extra tra simboli
    console.error(`   💤 Pausa tra simboli (3s)...`);
    await rateLimiter.sleep(3000);
  }

  console.error(`[Node-LOG] 🎉 Trades totali raccolti: ${allTrades.length}`);
  return allTrades;
}

// Generico: AUTO (prova senza symbol, fallback per-simboli filtrati)
async function fetchAllTradesGenericAuto(exchange, startTime, endTime) {
  try {
    console.error(`[Node-LOG] Probe fetchMyTrades senza symbol...`);
    await exchange.fetchMyTrades(undefined, startTime, 1);
    console.error(`[Node-LOG] Probe OK: percorso diretto`);
    return await fetchAllTradesGeneric(exchange, startTime, endTime);
  } catch (e) {
    console.error(`[Node-LOG] Probe fallita: ${e.message} -> userò simboli filtrati`);
    return await fetchAllTradesGenericBySymbols(exchange, startTime, endTime);
  }
}

async function fetchAllTradesGeneric(exchange, startTime, endTime) {
  let allTrades = [];
  let since = startTime;

  while (true) {
    try {
      const trades = await exchange.fetchMyTrades(undefined, since, 500);
      if (!trades.length) break;
      allTrades.push(...trades);
      since = trades[trades.length - 1].timestamp + 1;
      if (since > endTime) break;
    } catch (err) {
      console.error(`⚠ Errore fetchMyTrades: ${err.message}`);
      break;
    }
    await exchange.sleep(300);
  }
  return allTrades;
}

async function fetchAllTradesGenericBySymbols(exchange, startTime, endTime) {
  await exchange.loadMarkets();
  const symbols = await discoverLikelySymbolsGeneric(exchange);
  console.error(`[Node-LOG] Interrogo (generic) ${symbols.length} simboli filtrati.`);
  const allTrades = [];
  const seen = new Set();
  let idx = 0;

  for (const symbol of symbols) {
    idx++;
    console.error(`▶ [${idx}/${symbols.length}] Trades per ${symbol}...`);
    let since = startTime;

    while (true) {
      try {
        const trades = await exchange.fetchMyTrades(symbol, since, 500);
        if (!trades.length) break;
        let newCount = 0;
        for (const t of trades) {
          const key = `${t.symbol}|${t.id || t.order || t.timestamp}`;
          if (!seen.has(key)) {
            seen.add(key);
            allTrades.push(t);
            newCount++;
          }
        }
        since = trades[trades.length - 1].timestamp + 1;
        if (since > endTime) break;
      } catch (err) {
        console.error(`⚠ [Generic] Errore su ${symbol}: ${err.message}`);
        break;
      }
      await exchange.sleep(500);
    }
    await exchange.sleep(200);
  }
  return allTrades;
}

// ======================= MAIN =======================

async function main() {
  const [,, exchangeId, apiKey, secret, startDateArg = "2017-01-01"] = process.argv; // Default a 2017 per storico completo
  let startTime = new Date(startDateArg).getTime();
  const endTime = Date.now();

  // Fallback se startTime troppo vecchio
  if (startTime < EPOCH_ALL_HISTORY) {
    console.error(`[Node-LOG] startTime troppo antico: impostato a 2017-01-01`);
    startTime = EPOCH_ALL_HISTORY;
  }

  console.error(`▶ Inizio fetch per ${exchangeId} da ${new Date(startTime).toISOString()} a ${new Date(endTime).toISOString()}`);

  const ExchangeClass = ccxt[exchangeId];
  const exchange = new ExchangeClass({ 
    apiKey, 
    secret, 
    enableRateLimit: false, // gestiamo noi
    options: { recvWindow: 60000 },
    timeout: 60000
  });

  // Rate limiter (solo per Binance)
  const rateLimiter = exchangeId === 'binance' ? new BinanceRateLimiter() : null;

  // Time sync nativo ccxt
  if (exchangeId === 'binance') {
    try {
      await exchange.loadTimeDifference();
      exchange.options.adjustForTimeDifference = true;
      console.error(`[Node-LOG] ✅ Time sync nativo completato`);
    } catch (e) {
      console.error(`[Node-LOG] loadTimeDifference errore: ${e.message}`);
    }
  }

  // Test permessi API
  if (exchangeId === 'binance') {
    try {
      await exchange.request('account', 'sapi', 'GET', { recvWindow: 60000 });
      console.error(`[Node-LOG] ✅ Key API ha permessi base`);
    } catch (e) {
      console.error(`[WARN] Errore test permessi API: ${e.message} - verifica key su Binance`);
    }
  }

  console.error("⏳ Scarico depositi e prelievi...");
  
  let deposits = [];
  let withdrawals = [];
  
  if (exchangeId === 'binance') {
    deposits = await fetchAllDepositsRaw(exchange, rateLimiter, startTime, endTime);
    withdrawals = await fetchAllWithdrawalsRaw(exchange, rateLimiter, startTime, endTime);
  } else {
    deposits = await exchange.fetchDeposits(undefined, startTime)
      .catch(err => { console.error(`⚠ Depositi: ${err.message}`); return []; });
    withdrawals = await exchange.fetchWithdrawals(undefined, startTime)
      .catch(err => { console.error(`⚠ Prelievi: ${err.message}`); return []; });
  }

  console.error("⏳ Scarico earn e staking (prima dei trades per ottimizzare discovery)...");
  
  let earnFlexible = [];
  let earnLocked = [];
  let stakingStaking = [];
  let stakingLDefi = [];
  let stakingFDefi = [];
  
  if (exchangeId === 'binance') {
    earnFlexible = await fetchAllSimpleEarnFlexibleRaw(exchange, rateLimiter, startTime, endTime);
    earnLocked = await fetchAllSimpleEarnLockedRaw(exchange, rateLimiter, startTime, endTime);
    stakingStaking = await fetchAllStakingInterestsRaw(exchange, 'STAKING', rateLimiter, startTime, endTime);
    stakingLDefi = await fetchAllStakingInterestsRaw(exchange, 'L_DEFI', rateLimiter, startTime, endTime);
    stakingFDefi = await fetchAllStakingInterestsRaw(exchange, 'F_DEFI', rateLimiter, startTime, endTime);
  }

  // Combina staking in un unico array (come da tuo output JSON)
  const staking = [...stakingStaking, ...stakingLDefi, ...stakingFDefi];

  console.error("⏳ Scarico trade (ultimo, con discovery ottimizzata da earn/staking)...");
  const trades = exchange.has['fetchMyTrades']
    ? exchangeId === 'binance'
      ? await fetchAllTradesBinance(exchange, startTime, endTime, rateLimiter)
      : await fetchAllTradesGenericAuto(exchange, startTime, endTime)
    : [];

  let conversions = [];
  if (exchangeId === 'binance') {
    console.error("⏳ Scarico conversioni...");
    await rateLimiter.waitForRateLimit();
    conversions = await exchange.request('convert/tradeFlow', 'sapi', 'GET', { 
      startTime, 
      endTime, 
      limit: 500,
      recvWindow: 60000 
    }).catch(err => { 
      console.error(`⚠ Conversioni: ${err.message}`); 
      return []; 
    });
  }

  let savings = [];
  if (exchangeId === 'binance') {
    console.error("⏳ Scarico savings...");
    await rateLimiter.waitForRateLimit();
    savings = await exchange.request('lending/union/transactions', 'sapi', 'GET', { 
      startTime, 
      endTime, 
      type: 'PURCHASE',
      recvWindow: 60000 
    }).catch(err => { 
      console.error(`⚠ Savings: ${err.message}`); 
      return []; 
    });
  }

  console.error("✅ Fetch completato, invio JSON finale.");
  console.log(JSON.stringify({ deposits, withdrawals, trades, conversions, savings, staking, error: null }));
}

main().catch(err => {
  console.error(`❌ Errore script JS: ${err.message}`);
  console.log(JSON.stringify({ deposits: [], withdrawals: [], trades: [], conversions: [], savings: [], staking: [], error: err.message }));
});