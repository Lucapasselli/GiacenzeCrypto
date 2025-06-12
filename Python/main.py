from fastapi import FastAPI, Request, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import ccxt
from typing import Union
from concurrent.futures import ThreadPoolExecutor
import time
import json
import os

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# File dove salvo la cache dei token supportati
SUPPORTED_TOKENS_FILE = "supported_tokens_cache.json"

# TTL 24 ore in ms
supported_tokens_ttl = 24 * 3600 * 1000

# Cache caricata da file all'avvio
if os.path.exists(SUPPORTED_TOKENS_FILE):
    with open(SUPPORTED_TOKENS_FILE, "r") as f:
        supported_tokens_cache = json.load(f)
        # json non salva set, converto simboli in set
        for ex_id in supported_tokens_cache:
            supported_tokens_cache[ex_id]["symbols"] = set(supported_tokens_cache[ex_id]["symbols"])
else:
    supported_tokens_cache = {}

def save_supported_tokens_cache():
    # Converte set in lista per json e salva su file
    to_save = {}
    for ex_id in supported_tokens_cache:
        to_save[ex_id] = {
            "timestamp": supported_tokens_cache[ex_id]["timestamp"],
            "symbols": list(supported_tokens_cache[ex_id]["symbols"])
        }
    with open(SUPPORTED_TOKENS_FILE, "w") as f:
        json.dump(to_save, f, indent=4)

def get_exchange(exchange_id: str, request: Request):
    params = {}
    prefix = exchange_id.lower() + "_"
    for key, value in request.query_params.items():
        if key.startswith(prefix):
            param_name = key[len(prefix):]
            params[param_name] = value
    try:
        exchange_class = getattr(ccxt, exchange_id.lower())
    except AttributeError:
        raise HTTPException(status_code=400, detail=f"Exchange '{exchange_id}' non supportato da ccxt")
    return exchange_class(params)

def update_supported_tokens(exchange_id: str, exchange) -> set:
    now = int(time.time() * 1000)
    cache = supported_tokens_cache.get(exchange_id)
    if cache is None or now - cache["timestamp"] > supported_tokens_ttl:
        try:
            markets = exchange.load_markets()
            symbols = set(markets.keys())
            supported_tokens_cache[exchange_id] = {
                "timestamp": now,
                "symbols": symbols
            }
            save_supported_tokens_cache()
        except Exception:
            # Se errore mantiene cache vecchia o vuota
            if cache is None:
                supported_tokens_cache[exchange_id] = {
                    "timestamp": now,
                    "symbols": set()
                }
    return supported_tokens_cache[exchange_id]["symbols"]

@app.get("/alltrades")
def all_trades(exchange: str, symbol: str, request: Request):
    ex = get_exchange(exchange, request)
    symbols_supported = update_supported_tokens(exchange, ex)
    if symbol not in symbols_supported:
        raise HTTPException(status_code=400, detail=f"Symbol '{symbol}' non supportato su {exchange}")
    try:
        trades = ex.fetch_trades(symbol)
        return trades
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/deposit")
def deposits(exchange: str, request: Request):
    ex = get_exchange(exchange, request)
    try:
        deposits = ex.fetch_deposits()
        return deposits
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/balances")
def balances(exchange: str, request: Request):
    ex = get_exchange(exchange, request)
    try:
        balance = ex.fetch_balance()
        return balance
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/historical_full")
def historical_full(
    exchange: str,
    symbol: str,
    timeframe: str = "1m",
    since: Union[str, int] = "2024-01-01T00:00:00Z",
    until: Union[str, int] = None,
    request: Request = None
):
    try:
        ex = get_exchange(exchange, request)
    except HTTPException as e:
        raise e

    symbols_supported = update_supported_tokens(exchange, ex)
    if symbol not in symbols_supported:
        raise HTTPException(status_code=400, detail=f"Symbol '{symbol}' non supportato su {exchange}")

    if isinstance(since, int):
        since_ts = since
    else:
        try:
            since_ts = ex.parse8601(since)
        except Exception:
            raise HTTPException(status_code=400, detail=f"Formato 'since' non valido: {since}")

    if until is not None:
        if isinstance(until, int):
            until_ts = until
        else:
            try:
                until_ts = ex.parse8601(until)
            except Exception:
                raise HTTPException(status_code=400, detail=f"Formato 'until' non valido: {until}")
    else:
        until_ts = ex.milliseconds()

    all_ohlcv = []
    limit = 1000
    current_since = since_ts

    while current_since < until_ts:
        try:
            ohlcv = ex.fetch_ohlcv(symbol, timeframe, current_since, limit)
            if not ohlcv:
                break
            all_ohlcv.extend(ohlcv)
            last_ts = ohlcv[-1][0]
            if last_ts == current_since:
                break
            current_since = last_ts + ex.parse_timeframe(timeframe) * 1000
        except Exception as e:
            break

    filtered = [c for c in all_ohlcv if c[0] < until_ts]
    return filtered

@app.get("/historical_multi")
def historical_multi(
    exchanges: str = "binance,kraken",
    symbol: str = "BTC/USDT",
    timeframe: str = "1m",
    since: Union[str, int] = "2024-01-01T00:00:00Z",
    until: Union[str, int] = None,
    request: Request = None
):
    try:
        ex_sample = ccxt.binance()
    except Exception:
        raise HTTPException(status_code=500, detail="Impossibile inizializzare exchange di esempio")

    if isinstance(since, int):
        since_ts = since
    else:
        try:
            since_ts = ex_sample.parse8601(since)
        except Exception:
            raise HTTPException(status_code=400, detail=f"Formato 'since' non valido: {since}")

    if until is not None:
        if isinstance(until, int):
            until_ts = until
        else:
            try:
                until_ts = ex_sample.parse8601(until)
            except Exception:
                raise HTTPException(status_code=400, detail=f"Formato 'until' non valido: {until}")
    else:
        until_ts = ex_sample.milliseconds()

    exchange_list = [e.strip() for e in exchanges.split(",") if e.strip()]
    if not exchange_list:
        raise HTTPException(status_code=400, detail="Devi specificare almeno un exchange")

    results = {}

    def safe_fetch(exchange_id):
        try:
            ex = get_exchange(exchange_id, request)
            symbols_supported = update_supported_tokens(exchange_id, ex)
            if symbol in symbols_supported:
                target_symbol = symbol
                invert_price = False
            else:
                parts = symbol.split('/')
                if len(parts) == 2:
                    inverted_symbol = f"{parts[1]}/{parts[0]}"
                    if inverted_symbol in symbols_supported:
                        target_symbol = inverted_symbol
                        invert_price = True
                    else:
                        return exchange_id, []
                else:
                    return exchange_id, []

            all_ohlcv = []
            limit = 1000
            current_since = since_ts
            while current_since < until_ts:
                try:
                    ohlcv = ex.fetch_ohlcv(target_symbol, timeframe, current_since, limit)
                    if not ohlcv:
                        break
                    all_ohlcv.extend(ohlcv)
                    last_ts = ohlcv[-1][0]
                    if last_ts == current_since:
                        break
                    current_since = last_ts + ex.parse_timeframe(timeframe) * 1000
                except Exception:
                    break
            filtered = [c for c in all_ohlcv if c[0] < until_ts]

            if invert_price:
                inverted_filtered = []
                for c in filtered:
                    ts, o, h, l, c_, v = c
                    if o == 0:
                        inv_o = 0
                    else:
                        inv_o = 1 / o
                    inverted_filtered.append([ts, inv_o, h, l, c_, v])
                filtered = inverted_filtered

            return exchange_id, filtered

        except Exception:
            return exchange_id, []

    with ThreadPoolExecutor(max_workers=len(exchange_list)) as executor:
        futures = [executor.submit(safe_fetch, ex_id) for ex_id in exchange_list]
        for future in futures:
            ex_id, ohlcv_data = future.result()
            if ohlcv_data:
                results[ex_id] = ohlcv_data

    combined = {}
    for ex_id, ohlcv_list in results.items():
        for entry in ohlcv_list:
            ts, o, h, l, c, v = entry
            if ts not in combined:
                combined[ts] = {}
            combined[ts][ex_id] = o

    sorted_timestamps = sorted(combined.keys())
    output = []
    for ts in sorted_timestamps:
        output.append({
            "timestamp": ts,
            "prices": combined[ts]
        })

    return output

@app.get("/supported_pairs")
def supported_pairs(exchange: str, request: Request):
    try:
        ex = get_exchange(exchange, request)
    except HTTPException as e:
        raise e
    symbols_supported = update_supported_tokens(exchange, ex)
    return sorted(list(symbols_supported))

