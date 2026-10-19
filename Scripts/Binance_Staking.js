// Scarica da Binance le operazioni che cambiano moneta senza passare dagli scambi spot:
//  - staking SOL (SOL -> BNSOL) e riscatti (BNSOL -> SOL, accreditato giorni dopo)   sapi sol-staking
//  - staking ETH (ETH -> WBETH) e riscatti (WBETH -> ETH)                            sapi eth-staking
//  - sottoscrizioni e riscatti di RWUSD (USDC <-> RWUSD)                             sapi rwusd (non censito da ccxt)
//  - pagamenti Binance Pay (oggi se ne importano solo i "Tax Payment", orderType TAX) sapi pay/transactions
// Output su stdout: { "Binance_Staking": { inizio, solStaking, solRiscatti, ethStaking, ethRiscatti, rwusdSottoscrizioni,
//                     rwusdRiscatti, pay, nonDisponibili: { endpoint: messaggio } } }
//
// NON si scaricano i "rewards" di BNSOL e WBETH (bnsolRewardsHistory, wbethRewardsHistory): sono la crescita del
// cambio del token, nessuna moneta viene accreditata, e importarli come premi conterebbe un reddito che si
// realizza solo al riscatto. Le distribuzioni di RWUSD arrivano gia' da asset/assetDividend. Gli interessi del
// vecchio staking (staking/stakingRecord, 2022-2023) arrivano gia' da simple-earn/locked/history/rewardsRecord.
//
// Un endpoint che Binance dichiara non disponibile per il conto (prodotto non attivo nel paese, chiave senza quel
// permesso: ccxt PermissionDenied/AccountNotEnabled) non ferma lo scaricamento: finisce in nonDisponibili e Java lo
// segnala. Ogni altro errore ferma tutto, come negli altri script.
const ccxt = require('ccxt');
const { creaExchange, sincronizzaOrario, chiama, esciConErrore, logInfo } = require('./Binance_Comune.js');

const GIORNO_MS = 24 * 60 * 60 * 1000;
const GIORNI_FINESTRA = 90;  // intervallo massimo accettato da questi endpoint
const RIGHE = 100;
// Prima di queste date i prodotti non esistevano: inutile spendere richieste da 150 di peso.
const INIZIO_ETH = Date.UTC(2020, 11, 1);
const INIZIO_SOL = Date.UTC(2024, 7, 1);
const INIZIO_RWUSD = Date.UTC(2025, 0, 1);
const STORIA_PAY_GIORNI = 540;  // Binance Pay conserva circa 18 mesi
const COSTO_RWUSD = 15;         // peso 150 come gli altri storici Earn

async function storico(ex, endpoint, inizio, fine, costo = undefined) {
    const out = [];
    for (let a = inizio; a < fine; ) {
        const b = Math.min(a + GIORNI_FINESTRA * GIORNO_MS, fine);
        out.push(...await finestra(ex, endpoint, a, b, costo));
        a = b + 1;
    }
    return out;
}

/**
 * Tutte le pagine di un intervallo. Su "Query time range too large" (-6021) l'intervallo si divide a meta': i
 * riscatti l'hanno restituito anche per finestre di 90 giorni accettate negli stessi giorni dagli altri endpoint, e
 * il limite vero non e' documentato.
 */
async function finestra(ex, endpoint, a, b, costo) {
    const out = [];
    try {
        for (let current = 1; current <= 1000; current++) {
            const r = await chiama(ex, endpoint, { startTime: a, endTime: b, current, size: RIGHE }, 'sapi', costo);
            const righe = Array.isArray(r) ? r : (r?.rows || []);
            out.push(...righe);
            if (righe.length < RIGHE) break;
        }
    } catch (e) {
        if (!String(e.message).includes('-6021') || b - a < GIORNO_MS) throw e;
        const meta = Math.floor((a + b) / 2);
        logInfo(`${endpoint}: intervallo troppo ampio, divido ${new Date(a).toISOString()} - ${new Date(b).toISOString()}`);
        return [...await finestra(ex, endpoint, a, meta, costo), ...await finestra(ex, endpoint, meta + 1, b, costo)];
    }
    return out;
}

async function pagamentiPay(ex, inizio, fine) {
    const out = [];
    for (let a = Math.max(inizio, fine - STORIA_PAY_GIORNI * GIORNO_MS); a < fine; ) {
        const b = Math.min(a + GIORNI_FINESTRA * GIORNO_MS, fine);
        const r = await chiama(ex, 'pay/transactions', { startTime: a, endTime: b, limit: RIGHE });
        const righe = r?.data || [];
        // Senza paginazione: oltre 100 pagamenti in 90 giorni si divide la finestra a meta'
        if (righe.length >= RIGHE && b - a > 60000) {
            const meta = Math.floor((a + b) / 2);
            out.push(...await pagamentiPay(ex, a, meta), ...await pagamentiPay(ex, meta + 1, b));
        } else {
            out.push(...righe);
        }
        a = b + 1;
    }
    return out;
}

async function main() {
    const [, , exchangeId, apiKey, secret, startDate] = process.argv;
    const inizio = Number(startDate);
    const fine = Date.now();
    const ex = creaExchange(exchangeId, apiKey, secret);
    await sincronizzaOrario(ex);

    // inizio: a Java serve per importare un riscatto solo se il suo accredito e' nuovo (vedi daRiscatti)
    const risultato = { inizio, nonDisponibili: {} };
    const leggi = async (chiave, endpoint, fn) => {
        try {
            risultato[chiave] = await fn();
            logInfo(`${endpoint}: ${risultato[chiave].length}`);
        } catch (e) {
            // Prodotto inesistente per il conto, non disponibile nel paese, chiave senza quel permesso: l'utente non
            // puo' averlo usato, si segnala e si prosegue. Ogni altro errore (rete dopo le ripetizioni, parametro
            // rifiutato, percorso cambiato) ferma l'importazione: saltare l'endpoint lascerebbe un buco che gli
            // scaricamenti successivi, partendo dall'ultimo movimento, non colmano (scelta dell'utente, 2026-10-18).
            if (!(e instanceof ccxt.PermissionDenied || e instanceof ccxt.AccountNotEnabled)) throw e;
            risultato[chiave] = [];
            risultato.nonDisponibili[endpoint] = e.message;
            logInfo(`${endpoint}: non disponibile (${e.message})`);
        }
    };
    const da = (minimo) => Math.max(inizio, minimo);
    // I riscatti si cercano per data di richiesta, ma l'accredito arriva giorni dopo: si riparte 30 giorni prima,
    // cosi' un riscatto ancora in corso all'ultimo scaricamento viene ripreso. Java importa solo i riscatti completati
    // con l'accredito da inizio in poi: quelli accreditati prima sono gia' stati importati, e reimportarli (il ramo
    // Binance sovrascrive) annullerebbe lo scambio differito che l'utente ne ha fatto.
    const daRiscatti = (minimo) => Math.max(inizio - 30 * GIORNO_MS, minimo);
    await leggi('solStaking', 'sol-staking/sol/history/stakingHistory',
        () => storico(ex, 'sol-staking/sol/history/stakingHistory', da(INIZIO_SOL), fine));
    await leggi('solRiscatti', 'sol-staking/sol/history/redemptionHistory',
        () => storico(ex, 'sol-staking/sol/history/redemptionHistory', daRiscatti(INIZIO_SOL), fine));
    await leggi('ethStaking', 'eth-staking/eth/history/stakingHistory',
        () => storico(ex, 'eth-staking/eth/history/stakingHistory', da(INIZIO_ETH), fine));
    await leggi('ethRiscatti', 'eth-staking/eth/history/redemptionHistory',
        () => storico(ex, 'eth-staking/eth/history/redemptionHistory', daRiscatti(INIZIO_ETH), fine));
    await leggi('rwusdSottoscrizioni', 'rwusd/history/subscriptionHistory',
        () => storico(ex, 'rwusd/history/subscriptionHistory', da(INIZIO_RWUSD), fine, COSTO_RWUSD));
    await leggi('rwusdRiscatti', 'rwusd/history/redemptionHistory',
        () => storico(ex, 'rwusd/history/redemptionHistory', daRiscatti(INIZIO_RWUSD), fine, COSTO_RWUSD));
    await leggi('pay', 'pay/transactions', () => pagamentiPay(ex, inizio, fine));

    console.log(JSON.stringify({ Binance_Staking: risultato }));
}

main().catch(esciConErrore);
