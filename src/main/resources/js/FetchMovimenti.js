const ccxt = require('ccxt');

async function main(exchangeId, apiKey, secret) {
    const exchangeClass = ccxt[exchangeId];
    const exchange = new exchangeClass({
        apiKey,
        secret,
        enableRateLimit: true,
    });

    if (exchangeId === 'binance') {
        // Sincronizza tempo con il server Binance
        await exchange.loadTimeDifference();
        exchange.options['adjustForTimeDifference'] = true;
    }

    // Ora le chiamate API useranno timestamp corretto
    const deposits = await exchange.fetchDeposits();
    const withdrawals = await exchange.fetchWithdrawals();

    return { deposits, withdrawals, error: null };
}

main(process.argv[2], process.argv[3], process.argv[4])
    .then(result => console.log(JSON.stringify(result)))
    .catch(error => console.log(JSON.stringify({ error: error.message })));