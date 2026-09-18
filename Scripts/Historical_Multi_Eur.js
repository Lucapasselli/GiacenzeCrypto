//#!/usr/bin/env node
const fs = require('fs');
const path = require('path');
const os = require('os');

// Utility functions
function logError(message) { console.error(`[ERROR] ${message}`); }
function logInfo(message) { console.error(`[INFO] ${message}`); }
function logDebug(message) { console.error(`[DEBUG] ${message}`); }

// In Node.js v15+ unhandled rejections terminate the process by default.
// CCXT 4.x creates Coinbase FUTURE/PERPETUAL promises before awaiting them; if the
// endpoint is unreachable, Node.js sees a brief "unhandled" rejection and crashes
// before CCXT's own try-catch can intercept it. Registering this handler keeps
// the process alive so CCXT can handle the rejection itself.
process.on('unhandledRejection', (reason) => {
    logError(`Unhandled rejection (ignorato, gestito da CCXT): ${reason}`);
});

// Trova il miglior simbolo disponibile per XXX/EUR
async function findBestPair(ex, baseSymbol) {
   // const markets = await ex.loadMarkets();
    const markets = await getMarketsSymbols(ex);
    
    // Se esiste direttamente la coppia con EUR
    if (`${baseSymbol}/EUR` in markets) {
       // logInfo(`Trovato corrispondenza per EUR su ${ex.id}`);
        return { pair: `${baseSymbol}/EUR`, invert: false, needsConversion: false };
    }

    // Se esiste con USD
    if (`${baseSymbol}/USD` in markets) {
       // logInfo(`Trovato corrispondenza per USD su ${ex.id}`);
        if ("EUR/USD" in markets) {
            return { pair: `${baseSymbol}/USD`, invert: false, needsConversion: true, conversionPair: "EUR/USD" };
        }
        if ("USD/EUR" in markets) {
            return { pair: `${baseSymbol}/USD`, invert: false, needsConversion: true, conversionPair: "USD/EUR" };
        }
    }

    // Se esiste con USDT
    if (`${baseSymbol}/USDT` in markets) {
       // logInfo(`Trovato corrispondenza per USDT su ${ex.id}`);
        if ("EUR/USDT" in markets) {
            return { pair: `${baseSymbol}/USDT`, invert: false, needsConversion: true, conversionPair: "EUR/USDT" };
        }
        if ("USDT/EUR" in markets) {
            return { pair: `${baseSymbol}/USDT`, invert: false, needsConversion: true, conversionPair: "USDT/EUR" };
        }
    }

    // Se esiste con USDC
    if (`${baseSymbol}/USDC` in markets) {
       // logInfo(`Trovato corrispondenza per USDC su ${ex.id}`);
        if ("EUR/USDC" in markets) {
            return { pair: `${baseSymbol}/USDC`, invert: false, needsConversion: true, conversionPair: "EUR/USDC" };
        }
        if ("USDC/EUR" in markets) {
            return { pair: `${baseSymbol}/USDC`, invert: false, needsConversion: true, conversionPair: "USDC/EUR" };
        }
    }

    // Direzione inversa: per quando baseSymbol è esso stesso una valuta di quotazione comune
    // (USDT, USDC) e l'exchange quota EUR/USD come BASE — es. Binance ha "EUR/USDT", mai
    // "USDT/EUR". Senza questo, cercare il prezzo di USDT/USDC stesse non trovava mai nulla: i
    // controlli sopra guardano solo ${baseSymbol}/X, mai X/${baseSymbol} (bug osservato il
    // 2026-09-18 valorizzando USDT come giacenza in RT).
    if (`EUR/${baseSymbol}` in markets) {
        return { pair: `EUR/${baseSymbol}`, invert: true, needsConversion: false };
    }
    if (`USD/${baseSymbol}` in markets) {
        if ("EUR/USD" in markets) {
            return { pair: `USD/${baseSymbol}`, invert: true, needsConversion: true, conversionPair: "EUR/USD" };
        }
        if ("USD/EUR" in markets) {
            return { pair: `USD/${baseSymbol}`, invert: true, needsConversion: true, conversionPair: "USD/EUR" };
        }
    }

    return null;
}



// Memoria in processo dei markets gia' letti, per id di exchange. La cache su file qui sotto evita
// di richiamare l'API, ma non di rileggere e ri-analizzare il JSON a ogni richiesta: quello di
// binance pesa 36 MB, e in modalita' lotto la stessa lettura si ripeterebbe per ogni (moneta, ora).
// Vive quanto il processo, quindi nella modalita' a richiesta singola non cambia nulla.
const marketsInMemoria = new Map();

// Tasso di cambio EUR/stablecoin condiviso da TUTTE le monete della stessa finestra, invece di uno
// per exchange: senza questa memoria, in un lotto con N monete che hanno tutte bisogno della stessa
// conversione USDT->EUR, la si cercherebbe (e si proverebbe a scalare da Binance agli altri exchange)
// N volte invece di una sola. Chiave `${quoteCcy}|${since}|${until}`, valore l'esito di
// trovaSerieConversione (anche `null` se nessun exchange l'ha, per non ritentare invano).
const serieConversioneMemo = new Map();

async function getMarketsSymbols(exchange) {
    if (marketsInMemoria.has(exchange.id)) {
        return marketsInMemoria.get(exchange.id);
    }
    const tempDir = path.join(os.tmpdir(), 'GiacenzeCrypto');
    if (!fs.existsSync(tempDir)) {
        fs.mkdirSync(tempDir, { recursive: true });
    }

    const marketsFile = path.join(tempDir, `markets_${exchange.id}.json`);
    const oneHourMs = 1 * 60 * 60 * 6000; // 6 ore
    let markets;

    const needReload = () => {
        if (!fs.existsSync(marketsFile)) return true;
        const age = Date.now() - fs.statSync(marketsFile).mtimeMs;
        return age > oneHourMs;
    };

    if (needReload()) {
        logInfo(`Ricarico markets da API per ${exchange.id}...`);
        markets = await exchange.loadMarkets();

        // Rimuove riferimenti circolari e valori non serializzabili
        const cleanMarkets = JSON.parse(JSON.stringify(markets));
        fs.writeFileSync(marketsFile, JSON.stringify(cleanMarkets, null, 2), "utf8");
    } else {
       // logInfo(`Carico markets da file locale ${marketsFile}`);
        const raw = fs.readFileSync(marketsFile, "utf8");
        markets = JSON.parse(raw);
    }

    marketsInMemoria.set(exchange.id, markets);
    return markets;
}






// La serie della coppia di conversione (es. EUR/USDT) e' identica per TUTTE le monete della
// stessa finestra: essendo lo script invocato una volta per moneta, senza questa memoria su file
// verrebbe riscaricata da zero a ogni moneta dello stesso giorno. Verificato su TIA e SHIB su
// binance (nessuna delle due ha la coppia diretta in EUR): due monete, una sola serie scaricata.
// Quante monete ne abbiano bisogno sull'intero archivio non e' stato misurato.
// La chiave e' la quadrupla esatta (exchange, coppia, since, until) e non il giorno: anche
// `Prezzi.RecuperaPrezziDaCCXT` chiede finestre arbitrarie (+-ore attorno a un movimento), e una
// chiave per giorno le confonderebbe fra loro. Le giornate intere condividono comunque la cache
// perche' `RecuperaPrezziDaCCXTGiornata` passa gli stessi estremi per ogni moneta di quel giorno.
// Convenzioni identiche a `getMarketsSymbols`: stessa cartella, JSON semplice, eta' da mtime.
function fileCacheConversione(exId, pair, since, until) {
    const tempDir = path.join(os.tmpdir(), 'GiacenzeCrypto');
    if (!fs.existsSync(tempDir)) fs.mkdirSync(tempDir, { recursive: true });
    // Il "v2" e' un token di versione del CONTENUTO, non del formato del file: una finestra
    // passata non scade mai (vedi `finestraChiusa` sotto), quindi un cambiamento di come
    // `fetchHistorical` scarica la serie lascerebbe servire per sempre le serie salvate da prima.
    // Chi modifica `fetchHistorical` deve incrementare questo token.
    // v1 -> v2 il 2026-09-17 con la correzione di A9 (limite per chiamata da 1000 a 60): le serie
    // salvate prima potevano essere troncate o sfasate proprio per quel bug.
    const nome = `conv_v2_${exId}_${pair.replace(/[^A-Za-z0-9]/g, '_')}_${since}_${until}.json`;
    return path.join(tempDir, nome);
}

async function fetchHistoricalConversione(ex, symbol, timeframe, since, until) {
    const file = fileCacheConversione(ex.id, symbol, since, until);
    // Una finestra che comprende "adesso" non e' chiusa: arrivano ancora candele nuove, quindi la
    // si rilegge solo entro pochi minuti. Una finestra interamente passata non cambia mai piu'.
    const finestraChiusa = until < Date.now();
    if (fs.existsSync(file)) {
        const eta = Date.now() - fs.statSync(file).mtimeMs;
        if (finestraChiusa || eta < 5 * 60 * 1000) {
            try {
                return JSON.parse(fs.readFileSync(file, 'utf8'));
            } catch (e) {
                // file troncato da una corsa interrotta: si riscarica invece di propagare l'errore
                logInfo(`Cache conversione illeggibile (${path.basename(file)}), riscarico: ${e.message}`);
            }
        }
    }
    const dati = await fetchHistorical(ex, symbol, timeframe, since, until);
    // Una serie vuota non si memorizza: sarebbe indistinguibile da "non ancora scaricata" e
    // renderebbe permanente un buco dovuto a un errore transitorio dell'exchange.
    if (dati.length) {
        try {
            fs.writeFileSync(file, JSON.stringify(dati), 'utf8');
        } catch (e) {
            logInfo(`Cache conversione non scrivibile (${path.basename(file)}): ${e.message}`);
        }
    }
    return dati;
}

// Il limite per chiamata deve stare sotto il tetto di candele di OGNI exchange interrogato, non
// sotto il piu' generoso: era 1000, ma sullo storico okx si fermava a 100, bitget a 200, coinbase e
// cryptocom a 300. Chiedendo piu' del tetto, ccxt interpreta il limite come AMPIEZZA della finestra
// e restituisce le ULTIME candele dell'intervallo invece delle prime: cadevano fuori da
// [since, until] e il filtro finale le scartava tutte, silenziosamente (bug A9 in
// Analisi_Bug_Criticita.md; bitget aveva 0 righe in PrezziNew da sempre).
// 60 = un'ora a 1m, sotto ogni tetto, e allineato alla direzione del passo 5 del piano
// (bucket orari). Su Binance una richiesta con limit <= 99 pesa 1 invece di 2, la fascia piu'
// economica, quindi il conto delle richieste sale ma il peso no in proporzione.
// Volutamente NON si usa la paginazione interna di ccxt (`paginate: true`): lancia BadRequest oltre
// 10 chiamate, pretende `until` dentro params, e kucoin/bitstamp non la implementano affatto.
async function fetchHistorical(ex, symbol, timeframe, since, until, limit = 60) {
    const all_ohlcv = [];
    let current_since = since;

    while (current_since < until) {
      //  logInfo(`scarico dati per ${ex.id} , ${symbol}`);
        const ohlcv = await ex.fetchOHLCV(symbol, timeframe, current_since, limit);
       // logInfo(`i dati sono ${ohlcv}`);
        if (!ohlcv.length) break;
        all_ohlcv.push(...ohlcv);
        const last_ts = ohlcv[ohlcv.length - 1][0];
        if (last_ts === current_since) break;
        current_since = last_ts + ex.parseTimeframe(timeframe) * 1000;
    }

    return all_ohlcv.filter(c => c[0] <= until);
}

/**
 * Cerca un tasso di cambio EUR/`quoteCcy` (USDT, USD o USDC) valido per la finestra indicata,
 * provando gli exchange in `ordineEsplorazione` UNO ALLA VOLTA e fermandosi al primo che risponde
 * con dati non vuoti. Il tasso di cambio è un fatto di mercato, non specifico dell'exchange da cui
 * arriva il prezzo della moneta: se crypto.com ha un buco sulla propria serie EUR/USDT per un'ora,
 * non c'è motivo di scartare il prezzo di CRO che crypto.com fornisce benissimo — si può convertire
 * con il tasso di un altro exchange che quell'ora ce l'ha (vedi CLAUDE.md e il caso CRO/crypto.com
 * del 2026-09-17).
 *
 * @return `{exchangeId, pair, dati}` del primo exchange trovato, o `null` se nessuno ha dati
 */
async function trovaSerieConversione(ccxtLib, istanze, ordineEsplorazione, quoteCcy, timeframe, since, until) {
    for (const exId of ordineEsplorazione) {
        const chiave = exId.toLowerCase();
        if (!ccxtLib[chiave]) continue;
        const ex = (istanze && istanze[chiave]) || new ccxtLib[chiave]();
        let markets;
        try {
            markets = await getMarketsSymbols(ex);
        } catch (e) {
            continue;
        }
        // Stessa preferenza di findBestPair: EUR/X prima di X/EUR.
        let pair = null;
        if (`EUR/${quoteCcy}` in markets) pair = `EUR/${quoteCcy}`;
        else if (`${quoteCcy}/EUR` in markets) pair = `${quoteCcy}/EUR`;
        if (!pair) continue;

        try {
            const dati = await fetchHistoricalConversione(ex, pair, timeframe, since, until);
            if (dati.length) return { exchangeId: exId, pair, dati };
        } catch (e) {
            continue;
        }
    }
    return null;
}

/** Wrapper di {@link trovaSerieConversione} con la memoria di modulo `serieConversioneMemo`: una
 * sola ricerca per (valuta, finestra), riusata da tutte le monete della stessa invocazione. */
async function serieConversioneCondivisa(ccxtLib, istanze, ordineEsplorazione, quoteCcy, timeframe, since, until) {
    const chiaveMemo = `${quoteCcy}|${since}|${until}`;
    if (serieConversioneMemo.has(chiaveMemo)) return serieConversioneMemo.get(chiaveMemo);
    const risultato = await trovaSerieConversione(ccxtLib, istanze, ordineEsplorazione, quoteCcy, timeframe, since, until);
    serieConversioneMemo.set(chiaveMemo, risultato);
    return risultato;
}

/**
 * Verifica se `datiOhlcv` copre l'istante richiesto (±5 minuti, la stessa tolleranza con cui
 * {@code DammiPrezzoDaDatabase} cerca in cache): se sì, la cascata di {@link eseguiRicerca} può
 * fermarsi qui senza interrogare altri exchange. Senza un istante preciso (percorso a richiesta
 * singola, mai in cascata) basta che l'exchange abbia risposto con qualcosa.
 */
function copreIstante(datiOhlcv, istante) {
    if (istante == null) return datiOhlcv.length > 0;
    const finestra = 5 * 60 * 1000;
    return datiOhlcv.some(c => Math.abs(c[0] - istante) <= finestra);
}

/** Implementazione condivisa da `cercaPrezziStorici` e `cercaPrezziStoriciConEsito` (sotto): fa il
 * lavoro vero, la seconda espone anche quali exchange sono falliti con un errore vero.
 *
 * @param cascata se true (solo dal percorso a lotti, {@link cercaPrezziStoriciLotto}), interroga UN
 *        exchange alla volta invece di tutti in parallelo — prima `exchangePreferito` (o Binance se
 *        assente), poi i restanti in ordine ALFABETICO, fermandosi al primo che copre `istanteEsatto`
 *        (vedi {@link copreIstante}). L'ordine alfabetico per il ripiego non è arbitrario: è lo stesso
 *        con cui `DammiPrezzoDaDatabase` sceglie a parità di distanza quando si interrogano tutti gli
 *        exchange insieme (vedi `PrezziOrdinePrioritaTest` e `Analisi_Prezzi_Scaricamento_Costi.md`
 *        §4b) — è l'unico ordine che garantisce lo stesso prezzo finale della ricerca completa.
 *        Ogni exchange REALMENTE interrogato (in entrambe le modalità) scarica comunque l'ORA
 *        INTERA, mai un sottoinsieme di minuti: il marcatore `PrezziOraCCXT` è per ora, non per
 *        minuto, e un download parziale lo renderebbe disonesto.
 * @param exchangePreferito primo exchange da provare in cascata, se noto (altrimenti Binance)
 * @param istanteEsatto l'istante che la cascata deve coprire per fermarsi; `null` fuori dalla cascata
 */
async function eseguiRicerca({ ccxtLib, exchangeIds, baseSymbol, timeframe, since, until, istanze,
        cascata, exchangePreferito, istanteEsatto }) {
    const results = {};
    const risultatiGrezzi = {};
    const falliti = [];
    const interrogati = [];
    // Binance prima (il più liquido), poi gli altri exchange di questa richiesta nell'ordine dato.
    const ordineConversione = [...new Set(['binance', ...exchangeIds])];

    async function safe_fetch(exchange_id) {
        try {
            if (!ccxtLib[exchange_id.toLowerCase()]) {
                return [exchange_id, [], null, [], false];
            }
            // Con `istanze` fornite (modalita' lotto) l'oggetto exchange e' condiviso da tutte le
            // richieste: e' cio' che rende autorevole il limitatore interno di ccxt, che tiene lo
            // stato nell'istanza e con un oggetto nuovo per richiesta ripartirebbe da zero ignorando
            // le chiamate precedenti. Senza `istanze` il comportamento e' identico a prima.
            const ex = (istanze && istanze[exchange_id.toLowerCase()])
                    || new ccxtLib[exchange_id.toLowerCase()]();

            const pairInfo = await findBestPair(ex, baseSymbol);
            if (!pairInfo) return [exchange_id, [], null, [], false];

            // OHLCV principale (es. BTC/USDT o BTC/EUR, oppure EUR/USDT o USD/USDT se pairInfo.invert)
            const baseDataGrezza = await fetchHistorical(ex, pairInfo.pair, timeframe, since, until);
            // pairInfo.invert: la coppia trovata ha baseSymbol in posizione di QUOTA (es. "EUR/USDT"
            // per baseSymbol="USDT"), quindi il prezzo grezzo è l'inverso di quello che serve — e
            // high/low si scambiano, essendo l'inversione decrescente sui valori positivi.
            const baseData = pairInfo.invert
                    ? baseDataGrezza.map(([ts, o, h, l, c, v]) => [ts, 1 / o, 1 / l, 1 / h, 1 / c, v])
                    : baseDataGrezza;
            const copertura = copreIstante(baseData, istanteEsatto);

            let finalData = baseData;
            const grezzi = [];

            // Conversione se serve (es. BTC/USDT -> BTC/EUR usando un tasso EUR/USDT condiviso)
            if (pairInfo.needsConversion) {
                // "CRO/USDT" -> "USDT" nel caso normale; "USD/USDT" (invert) -> "USD", perché dopo
                // l'inversione qui sopra baseData è già "USDT in USD", non "USDT in USDT".
                const quoteCcy = pairInfo.invert ? pairInfo.pair.split('/')[0] : pairInfo.pair.split('/')[1];
                const conv = await serieConversioneCondivisa(
                        ccxtLib, istanze, ordineConversione, quoteCcy, timeframe, since, until);

                // Mappa timestamp -> tasso EUR/quoteCcy, qualunque sia l'exchange che l'ha fornito e
                // qualunque sia la direzione della coppia trovata (EUR/X o X/EUR).
                const convMap = {};
                if (conv) {
                    const invertire = conv.pair.startsWith('EUR/');
                    for (const c of conv.dati) {
                        const chiusura = c[4]; // chiusura, come prima
                        convMap[c[0]] = invertire ? 1 / chiusura : chiusura;
                    }
                }

                finalData = [];
                for (const c of baseData) {
                    const [ts, o, h, l, close, v] = c;
                    const fattore = convMap[ts];
                    if (fattore) {
                        finalData.push([ts, o * fattore, h * fattore, l * fattore, close * fattore, v]);
                    } else {
                        // Nessun exchange (Binance compreso) ha un tasso di cambio per questo minuto
                        // esatto: il prezzo grezzo si passa comunque, la conversione la fa il programma
                        // (CoinMarketCap + Banca d'Italia) invece di perdere il dato.
                        grezzi.push({ ts, valore: o, denom: quoteCcy });
                    }
                }
            }

            return [exchange_id, finalData, null, grezzi, copertura];
        } catch (err) {
            // Un errore vero (rate limit, manutenzione, rete, ...) è diverso da "l'exchange non ha
            // questa coppia" (gestito sopra con un `return` pulito, non un'eccezione): qui va
            // segnalato come fallito, non confuso con "nessun dato" — vedi Analisi_VPS_Prezzi_Sito.md,
            // "Esclusioni: exchange falliti vs nessun dato".
            return [exchange_id, [], (err && err.message) || 'errore sconosciuto', [], false];
        }
    }

    function registra([ex_id, ohlcv_data, errore, grezzi]) {
        interrogati.push(ex_id);
        if (errore) falliti.push(ex_id);
        if (ohlcv_data.length) results[ex_id] = ohlcv_data;
        if (grezzi && grezzi.length) risultatiGrezzi[ex_id] = grezzi;
    }

    if (cascata) {
        const primo = exchangePreferito || 'binance';
        const restanti = exchangeIds.filter(id => id !== primo).sort();
        for (const ex_id of [primo, ...restanti]) {
            const esito = await safe_fetch(ex_id);
            registra(esito);
            if (esito[4]) break; // copertura raggiunta: nessun altro exchange da interrogare
        }
    } else {
        // Fetch parallelo (percorso a richiesta singola, mai in cascata)
        const fetched = await Promise.all(exchangeIds.map(ex_id => safe_fetch(ex_id)));
        for (const esito of fetched) registra(esito);
    }

    // Combino risultati
    const combined = {};
    for (const ex_id in results) {
        for (const entry of results[ex_id]) {
            const [ts, o] = entry;
            if (!combined[ts]) combined[ts] = {};
            combined[ts][ex_id] = o;
        }
    }
    const combinedGrezzi = {};
    for (const ex_id in risultatiGrezzi) {
        for (const g of risultatiGrezzi[ex_id]) {
            if (!combinedGrezzi[g.ts]) combinedGrezzi[g.ts] = {};
            combinedGrezzi[g.ts][ex_id] = { valore: g.valore, denom: g.denom };
        }
    }

    const sorted_timestamps = [...new Set([...Object.keys(combined), ...Object.keys(combinedGrezzi)])]
            .map(Number).sort((a, b) => a - b);
    const punti = sorted_timestamps.map(ts => {
        const punto = { timestamp: ts, prices: combined[ts] || {} };
        if (combinedGrezzi[ts]) punto.grezzi = combinedGrezzi[ts];
        return punto;
    });

    return { punti, falliti, interrogati };
}

/**
 * Interroga in parallelo gli exchange indicati e ne fonde i prezzi in EUR di `baseSymbol` nella
 * finestra [since, until]. Corpo di `main()` estratto così com'era (stesso algoritmo, stesso
 * comportamento) per essere riusabile anche da `ServizioPrezzi/` (Fase 1, VPS) senza duplicare la
 * logica di scelta della coppia/conversione — vedi CLAUDE.md e
 * `test/Documentazione/Analisi_VPS_Prezzi_Sito.md`. `ccxtLib` è iniettato invece di essere
 * richiesto qui: `ccxt` viene caricato solo dentro `main()` (unico punto che ne aveva bisogno
 * prima del refactor), così un `require()` di questo file per le sole funzioni esportate non
 * tocca mai il modulo `ccxt` — evita che `ServizioPrezzi/`, che ha una propria installazione di
 * ccxt in `ServizioPrezzi/node_modules`, debba dipendere dalla risoluzione dei moduli di
 * `Scripts/` (cartelle diverse, non annidate).
 *
 * Restituisce solo l'array di punti (contratto invariato: lo consuma anche il CLI di questo file
 * e, in futuro, il client Java via `Prezzi.java`). Chi ha bisogno di sapere quali exchange sono
 * falliti (non "nessun dato", un errore vero) usa `cercaPrezziStoriciConEsito`.
 */
async function cercaPrezziStorici(args) {
    const { punti } = await eseguiRicerca(args);
    return punti;
}

/** Come `cercaPrezziStorici`, ma espone anche `falliti` (gli id degli exchange il cui fetch ha
 * lanciato un errore in questa chiamata, distinto da "nessun dato trovato"). Usato dal servizio
 * prezzi per non confondere un blip transitorio con un'assenza di prezzo confermata — vedi
 * `ServizioPrezzi/src/esclusioni.js` e `Analisi_VPS_Prezzi_Sito.md`. */
async function cercaPrezziStoriciConEsito(args) {
    return eseguiRicerca(args);
}

/**
 * Serve MOLTE richieste (moneta, finestra) in una sola invocazione del processo.
 *
 * Perche' esiste: il costo fisso di un'invocazione e' ~0,7 s (di cui ~0,56 s il solo
 * `require('ccxt')`, quindici volte l'avvio di Node), e finora si pagava per ogni singola coppia
 * (moneta, ora). Qui si paga una volta per lotto. Gli oggetti exchange sono costruiti una volta e
 * condivisi: oltre a risparmiare la costruzione, e' cio' che rende autorevole il limitatore di ccxt
 * (`enableRateLimit` e' true di default), che vive nell'istanza.
 *
 * Le richieste vengono servite con concorrenza limitata: le latenze di rete si sovrappongono, mentre
 * ccxt serializza da se' le chiamate verso lo stesso exchange con l'attesa dovuta. Servirle in fila
 * farebbe risparmiare solo il costo fisso, cioe' poco.
 *
 * Ogni richiesta viene servita in CASCATA (vedi {@link eseguiRicerca}): prima `richieste[i].exchangePreferito`
 * (o Binance se assente), poi i restanti in ordine alfabetico, fermandosi al primo che copre
 * `richieste[i].istante`. È la differenza col percorso a richiesta singola (`cercaPrezziStorici`),
 * che interroga sempre tutti gli exchange — usato apposta dal pulsante "Riscarica tutti i prezzi
 * dalle fonti", dove si vuole il confronto completo, non la scorciatoia.
 *
 * @param richieste array di `{symbol, since, until, istante, exchangePreferito}` (gli ultimi due
 *        opzionali: `istante` assente equivale a "basta che risponda qualcosa", `exchangePreferito`
 *        assente equivale a "Binance")
 * @returns array parallelo a `richieste`, ogni voce `{symbol, since, until, punti, falliti, interrogati}`.
 *          `falliti` distingue "l'exchange ha risposto e non ha dati" da "la chiamata e' fallita",
 *          distinzione necessaria a chi marca le ore gia' interrogate: marcare un'ora saltata per un
 *          errore di rete congelerebbe un buco per sempre. `interrogati` è chi è stato REALMENTE
 *          interrogato in questa cascata (mai tutti gli otto, salvo il caso limite in cui nessuno
 *          copra l'istante): solo loro vanno marcati su `PrezziOraCCXT`, non l'intera `exchangeIds`.
 */
async function cercaPrezziStoriciLotto({ ccxtLib, exchangeIds, timeframe, richieste, concorrenza }) {
    const istanze = {};
    for (const id of exchangeIds) {
        const chiave = id.toLowerCase();
        if (ccxtLib[chiave]) istanze[chiave] = new ccxtLib[chiave]();
    }

    const esiti = new Array(richieste.length);
    const inVolo = Math.max(1, Math.min(concorrenza || 4, richieste.length || 1));
    let prossima = 0;

    async function lavoratore() {
        while (true) {
            const i = prossima++;
            if (i >= richieste.length) return;
            const r = richieste[i];
            try {
                const { punti, falliti, interrogati } = await eseguiRicerca({
                    ccxtLib,
                    exchangeIds,
                    baseSymbol: r.symbol,
                    timeframe: timeframe || '1m',
                    since: r.since,
                    until: r.until,
                    istanze,
                    cascata: true,
                    exchangePreferito: r.exchangePreferito || '',
                    istanteEsatto: r.istante != null ? r.istante : null,
                });
                // exchangePreferito in eco: serve a Java per tenere separata, nel dedup di sessione,
                // una richiesta con preferenza da una senza (o con preferenza diversa) sulla stessa
                // (moneta, ora) — altrimenti la seconda risulterebbe "già chiesta" e salterebbe
                // proprio l'exchange che le serve davvero.
                esiti[i] = { symbol: r.symbol, since: r.since, until: r.until, punti, falliti, interrogati,
                        exchangePreferito: r.exchangePreferito || '' };
            } catch (err) {
                //Una richiesta che esplode non deve abbattere il lotto: si segnala e si prosegue.
                //`interrogati` vuoto: non si sa cosa sia stato davvero chiesto, quindi non si marca
                //nulla su PrezziOraCCXT piuttosto che marcare (e quindi bloccare) exchange a caso.
                logError(`Richiesta ${r.symbol} ${r.since}-${r.until} fallita: ${(err && err.message) || err}`);
                esiti[i] = { symbol: r.symbol, since: r.since, until: r.until, punti: [], falliti: [], interrogati: [],
                        exchangePreferito: r.exchangePreferito || '' };
            }
        }
    }

    await Promise.all(Array.from({ length: inVolo }, () => lavoratore()));
    return esiti;
}

async function main() {
    try {
        const ccxt = require('ccxt');

        // Parametri CLI. "lotto" è un flag booleano, senza valore proprio: trattarlo come gli
        // altri (prendere args[i+1] come valore) gli fa inghiottire il *nome* del flag successivo
        // invece che il suo valore — con "--lotto --exchanges binance,cryptocom,..." risultava
        // params.exchanges MAI valorizzato, quindi ogni scaricamento a lotto interrogava solo
        // Binance (il ripiego di default), qualunque lista di exchange gli venisse passata. Bug
        // scoperto il 2026-09-18 da CRO (mai su Binance) che risultava "nessun exchange" anche con
        // crypto.com pienamente disponibile.
        const args = process.argv.slice(2);
        const params = {};
        for (let i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                const key = args[i].substring(2);
                if (key === "lotto") {
                    params.lotto = "true";
                    continue;
                }
                const value = args[i + 1];
                params[key] = value;
                i++;
            }
        }

        const exchanges = params.exchanges ? params.exchanges.split(",").map(e => e.trim()) : ["binance"];
        const baseSymbol = params.symbol || "BTC";
        const timeframe = params.timeframe || "1m";

        //Modalita' lotto: le richieste arrivano come JSON su stdin, `[{symbol, since, until}, ...]`.
        //Si passa da stdin e non da un argomento perche' un lotto di migliaia di richieste supererebbe
        //il limite della riga di comando. Il ramo a richiesta singola qui sotto resta identico.
        if (params.lotto !== undefined) {
            const testo = fs.readFileSync(0, 'utf8');
            const richieste = JSON.parse(testo);
            const esiti = await cercaPrezziStoriciLotto({
                ccxtLib: ccxt,
                exchangeIds: exchanges,
                timeframe,
                richieste,
                concorrenza: params.concorrenza ? parseInt(params.concorrenza) : undefined,
            });
            console.log(JSON.stringify(esiti));
            return;
        }
        const since = params.since ? parseInt(params.since) : new ccxt.binance().parse8601("2024-01-01T00:00:00Z");
        const until = params.until ? parseInt(params.until) : new ccxt.binance().milliseconds();

        const output = await cercaPrezziStorici({
            ccxtLib: ccxt,
            exchangeIds: exchanges,
            baseSymbol,
            timeframe,
            since,
            until,
        });

        console.log(JSON.stringify(output, null, 2));
    } catch (err) {
        console.error(JSON.stringify({ error: err.message }));
        process.exit(1);
    }
}

module.exports = { findBestPair, fetchHistorical, getMarketsSymbols, cercaPrezziStorici, cercaPrezziStoriciConEsito, cercaPrezziStoriciLotto };

if (require.main === module) {
    main();
}
