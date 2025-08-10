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
  // (stesso codice completo come in withdrawals.js)
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

// ======================= Discovery simboli =======================

async function discoverLikelySymbolsFromHistoryBinance(exchange, { startTime, endTime }, rateLimiter) {
  await exchange.loadMarkets();
  console.error(`[Node-LOG] Mercati TRADING caricati: ${Object.keys(exchange.markets).length}`);

  const assets = new Set();

  // Balance
  try {
    if (rateLimiter) await rateLimiter.waitForRateLimit();
    const balance = await exchange.fetchBalance();
    for (const a of Object.keys(balance.total || {})) {
      if (a) assets.add(a.toUpperCase());
    }
  } catch (e) {
    console.error(`[Node-LOG] fetchBalance errore: ${e.message}`);
  }

  // (Per full discovery, combina con output da altri file manualmente)

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

  for (const symbol of Object.keys(markets)) {
    const m = markets[symbol];
    if (m.active === false) continue;
    const base = (m.base || '').toUpperCase();
    const quote = (m.quote || '').toUpperCase();
    if (coreAssets.has(base) && PREFERRED_QUOTES.includes(quote)) result.add(m.symbol);
    if (coreAssets.has(base) && coreAssets.has(quote)) result.add(m.symbol);
  }

  console.error(`[Node-LOG] Simboli candidati: ${result.size}`);
  return Array.from(result);
}

// ======================= Fetch Trades =======================

async function fetchAllTradesBinance(exchange, startTime, endTime, rateLimiter) {
  await exchange.loadMarkets();

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
      if (rateLimiter) await rateLimiter.waitForRateLimit();
      
      let attempt = 0;
      let trades;
      
      while (attempt < 3) {
        try {
          trades = await exchange.fetchMyTrades(symbol, since, 500, { recvWindow: 60000 });
          break;
        } catch (err) {
          if (rateLimiter && await rateLimiter.handleRateLimit(err, attempt)) {
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
    
    if (rateLimiter) await rateLimiter.sleep(3000);
  }

  console.error(`[Node-LOG] 🎉 Trades totali raccolti: ${allTrades.length}`);
  return allTrades;
}

// (Aggiungi funzioni generiche se necessario, come fetchAllTradesGenericAuto ecc.)

// ======================= MAIN =======================

async function main() {
  const [,, exchangeId, apiKey, secret, startDateArg = "2017-01-01"] = process.argv;
  let startTime = new Date(startDateArg).getTime();
  const endTime = Date.now();

  if (startTime < EPOCH_ALL_HISTORY) {
    startTime = EPOCH_ALL_HISTORY;
  }

  const ExchangeClass = ccxt[exchangeId];
  const exchange = new ExchangeClass({ apiKey, secret, enableRateLimit: false, options: { recvWindow: 60000 }, timeout: 60000 });

  const rateLimiter = exchangeId === 'binance' ? new BinanceRateLimiter() : null;

  if (exchangeId === 'binance') {
    try {
      await exchange.loadTimeDifference();
      exchange.options.adjustForTimeDifference = true;
    } catch (e) {}
  }

  const trades = exchange.has['fetchMyTrades']
    ? exchangeId === 'binance'
      ? await fetchAllTradesBinance(exchange, startTime, endTime, rateLimiter)
      : await fetchAllTradesGenericAuto(exchange, startTime, endTime)
    : [];

  console.log(JSON.stringify({ trades }));
}

main().catch(err => {
  console.log(JSON.stringify({ trades: [], error: err.message }));
});