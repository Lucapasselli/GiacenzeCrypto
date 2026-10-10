// Saldi ATTUALI di un account Binance, per la colonna di confronto di "Giacenze a data" (GiacenzeExchange.java).
//
// Binance non ha uno storico dei saldi: lo snapshot giornaliero (/sapi/v1/accountSnapshot) copre solo l'ultimo
// mese e solo il conto Spot. Qui si legge quindi soltanto la situazione di adesso, sommando i conti in cui
// l'archivio del programma tiene le monete di Binance (Spot ed Earn sono un unico insieme per l'import):
//   - Spot (free + locked), da /api/v3/account
//   - Funding (free + locked + freeze; "withdrawing" no: il prelievo in corso e' gia' nell'archivio)
//   - Simple Earn flessibile (totalAmount) e bloccato (amount)
// Restano fuori i contratti Dual Investment aperti, lo staking on-chain, margin e futures.
//
// Uso: node Binance_Saldi.js binance API_KEY SECRET
// stdout: {"spot":{asset:qta}, "funding":{...}, "earnFlessibile":{...}, "earnBloccato":{...}, "errori":{sezione:msg}}
// Una sezione che non risponde finisce in "errori": il programma allora non mostra il totale di nessuna moneta.

const ccxt = require('ccxt');

function logInfo(m) { console.error(`[INFO] ${m}`); }
function logError(m) { console.error(`[ERROR] ${m}`); }

const PAGINA = 100;

function somma(mappa, asset, ...valori) {
    let t = 0;
    for (const v of valori) {
        const n = parseFloat(v || '0');
        if (Number.isFinite(n)) t += n;
    }
    if (t === 0) return;
    // Stringa decimale esatta: si sommano le stringhe originali lato Java, qui si tiene la lista
    (mappa[asset] = mappa[asset] || []).push(...valori.filter(v => v && parseFloat(v) !== 0));
}

async function tutteLePagine(chiamata) {
    const righe = [];
    for (let p = 1; p <= 50; p++) {
        const r = await chiamata({ size: PAGINA, current: p });
        const lista = (r && r.rows) || [];
        righe.push(...lista);
        const totale = parseInt((r && r.total) || '0', 10);
        if (lista.length < PAGINA || righe.length >= totale) break;
    }
    return righe;
}

async function main() {
    const [, , exchangeId, apiKey, secret] = process.argv;
    const out = { spot: {}, funding: {}, earnFlessibile: {}, earnBloccato: {}, errori: {} };
    const ex = new ccxt[exchangeId || 'binance']({
        apiKey, secret, enableRateLimit: true,
        options: { recvWindow: 10000, adjustForTimeDifference: true },
    });
    try {
        await ex.loadTimeDifference();
    } catch (e) {
        logError(`Orario Binance: ${e.message}`);
    }

    try {
        const flex = await tutteLePagine(p => ex.sapiGetSimpleEarnFlexiblePosition(p));
        for (const r of flex) somma(out.earnFlessibile, r.asset, r.totalAmount);
        logInfo(`Simple Earn flessibile: ${flex.length} posizioni`);
    } catch (e) {
        out.errori.earnFlessibile = e.message;
        logError(`Simple Earn flessibile: ${e.message}`);
    }
    try {
        const bloccati = await tutteLePagine(p => ex.sapiGetSimpleEarnLockedPosition(p));
        for (const r of bloccati) somma(out.earnBloccato, r.asset, r.amount);
        logInfo(`Simple Earn bloccato: ${bloccati.length} posizioni`);
    } catch (e) {
        out.errori.earnBloccato = e.message;
        logError(`Simple Earn bloccato: ${e.message}`);
    }
    try {
        const conto = await ex.privateGetAccount({ omitZeroBalances: true });
        for (const b of (conto.balances || [])) {
            // I saldi "LDxxx" sono la rappresentazione nello Spot delle posizioni Earn flessibili, gia' contate
            // sopra: si saltano solo se "xxx" e' davvero fra le posizioni (LDO, Lido, e' un token vero)
            if (b.asset.startsWith('LD') && out.earnFlessibile[b.asset.substring(2)]) continue;
            somma(out.spot, b.asset, b.free, b.locked);
        }
    } catch (e) {
        out.errori.spot = e.message;
        logError(`Spot: ${e.message}`);
    }
    try {
        const fondi = await ex.sapiPostAssetGetFundingAsset({});
        for (const b of (fondi || [])) somma(out.funding, b.asset, b.free, b.locked, b.freeze);
    } catch (e) {
        out.errori.funding = e.message;
        logError(`Funding: ${e.message}`);
    }
    console.log(JSON.stringify(out));
}

main().catch(e => {
    console.log(JSON.stringify({ errori: { generale: e.message } }));
    process.exit(0);
});
