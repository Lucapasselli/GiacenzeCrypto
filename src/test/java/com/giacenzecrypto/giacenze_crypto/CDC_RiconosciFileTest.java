package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.CDC_FiatECardWallet.TipoFileCDC;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link CDC_FiatECardWallet#RiconosciFile}: i tre export dell'app Crypto.com hanno la stessa intestazione,
 * e chi carica il Fiat Wallet o la carta sotto "Crypto.com - App CSV" si ritrovava solo causali sconosciute.
 * Righe sintetiche nella forma degli export veri (importi inventati).
 */
class CDC_RiconosciFileTest {

    @TempDir
    Path dir;

    private static final String INTESTAZIONE = "Timestamp (UTC),Transaction Description,Currency,Amount,To Currency,"
            + "To Amount,Native Currency,Native Amount,Native Amount (in USD),Transaction Kind,Transaction Hash";

    private File scrivi(String nome, String... righe) throws Exception {
        Path f = dir.resolve(nome);
        Files.writeString(f, "﻿" + INTESTAZIONE + "\n" + String.join("\n", righe) + "\n", StandardCharsets.UTF_8);
        return f.toFile();
    }

    private static final String[] APP = {
        "2024-01-10 10:00:00,Buy BTC,EUR,-100,BTC,0.002,EUR,100,108,viban_purchase,",
        "2024-01-11 10:00:00,CRO Stake Rewards,CRO,1.5,,,EUR,0.12,0.13,mco_stake_reward,",
        "2024-01-12 10:00:00,Card Cashback,CRO,2,,,EUR,0.16,0.17,referral_card_cashback,"};
    private static final String[] FIAT = {
        "2024-01-10 10:00:00,EUR Deposit (via SEPA),EUR,500.0,EUR,500.0,EUR,500.0,540.0,viban_deposit,",
        "2024-01-10 10:05:00,Top Up Card,EUR,500.0,EUR,500.0,EUR,500.0,540.0,viban_card_top_up,",
        "2024-01-11 10:00:00,Buy BTC,EUR,-100,BTC,0.002,EUR,100,108,viban_purchase,"};
    private static final String[] CARTA = {
        "2024-01-10 10:00:00,EUR Deposit,EUR,500,,,EUR,500,540,,",
        "2024-01-11 12:00:00,Crv*Supermercato,EUR,-35.20,,,EUR,35.20,38.00,,"};

    @Test
    void nomiDatiDaCryptoCom_decidonoPrimaDelContenuto() throws Exception {
        assertEquals(TipoFileCDC.FIAT_WALLET, CDC_FiatECardWallet.RiconosciFile(scrivi("fiat_transactions_record_20240201_101010.csv", APP)));
        assertEquals(TipoFileCDC.CARD_WALLET, CDC_FiatECardWallet.RiconosciFile(scrivi("card_transactions_record_20240201_101010.csv", APP)));
        assertEquals(TipoFileCDC.APP, CDC_FiatECardWallet.RiconosciFile(scrivi("crypto_transactions_record_20240201_101010.csv", FIAT)));
    }

    @Test
    void fileRinominati_siRiconosconoDalContenuto() throws Exception {
        assertEquals(TipoFileCDC.APP, CDC_FiatECardWallet.RiconosciFile(scrivi("app.csv", APP)));
        assertEquals(TipoFileCDC.FIAT_WALLET, CDC_FiatECardWallet.RiconosciFile(scrivi("fiat.csv", FIAT)));
        assertEquals(TipoFileCDC.CARD_WALLET, CDC_FiatECardWallet.RiconosciFile(scrivi("carta.csv", CARTA)));
    }

    @Test
    void soloAcquistiDalFiatWallet_restaApp() throws Exception {
        //viban_purchase compare anche fra le transazioni crypto: da solo non basta a dire Fiat Wallet
        assertEquals(TipoFileCDC.APP, CDC_FiatECardWallet.RiconosciFile(scrivi("acquisti.csv",
                "2024-01-11 10:00:00,Buy BTC,EUR,-100,BTC,0.002,EUR,100,108,viban_purchase,")));
    }

    @Test
    void soloIntestazione_restaApp() throws Exception {
        assertEquals(TipoFileCDC.APP, CDC_FiatECardWallet.RiconosciFile(scrivi("vuoto.csv")));
    }

    @Test
    void altroFormato_sconosciuto() throws Exception {
        Path f = dir.resolve("binance.csv");
        Files.writeString(f, "User_ID,UTC_Time,Account,Operation,Coin,Change,Remark\n1,2024-01-10 10:00:00,Spot,Deposit,BTC,1,\n");
        assertEquals(TipoFileCDC.SCONOSCIUTO, CDC_FiatECardWallet.RiconosciFile(f.toFile()));
    }
}
