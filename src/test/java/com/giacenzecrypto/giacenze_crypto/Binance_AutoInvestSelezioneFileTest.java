package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.Binance_AutoInvest.TipoFile;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Fissa il controllo che la finestra di importazione fa sui file Auto-Invest scelti, prima di abbinarli.
 * <p>Il file Rebalancing non dice il verso dei ribilanciamenti: lo si ricava da Subscription e Redemption letti nello
 * stesso passaggio, e un ribilanciamento creato non viene più rifatto. Per questo la finestra avvisa quando manca
 * uno dei due, e solo allora: Subscription e Redemption da soli sono una scelta legittima.
 */
public class Binance_AutoInvestSelezioneFileTest {

    @Test
    public void ilTipoSiRiconosceDallIntestazione(@TempDir File dir) throws Exception {
        assertEquals(TipoFile.SUBSCRIPTION, Binance_AutoInvest.RiconosciFile(
                Scrivi(dir, "a.csv", "﻿Time,Holding Coin,Amount Per Period,Trading Fee,Units,From,Status\n")));
        assertEquals(TipoFile.REBALANCING, Binance_AutoInvest.RiconosciFile(
                Scrivi(dir, "b.csv", "\n\nTime,Index Name,Rebalancing Coin,Rebalancing Amount,Total Rebalancing Fee\n")));
        assertEquals(TipoFile.REDEMPTION, Binance_AutoInvest.RiconosciFile(
                Scrivi(dir, "c.csv", "Time,Index Name,Coin,Redemption Amount,Redemption Fee\n")));
        assertEquals(TipoFile.SCONOSCIUTO, Binance_AutoInvest.RiconosciFile(
                Scrivi(dir, "d.csv", "User_ID,UTC_Time,Account,Operation,Coin,Change,Remark\n")));
        assertEquals(TipoFile.SCONOSCIUTO, Binance_AutoInvest.RiconosciFile(Scrivi(dir, "vuoto.csv", "")));
        assertEquals(TipoFile.SCONOSCIUTO, Binance_AutoInvest.RiconosciFile(new File(dir, "inesistente.csv")));
    }

    @Test
    public void senzaRebalancingNonMancaNulla() {
        assertTrue(Binance_AutoInvest.MancantiPerRibilanciamenti(EnumSet.of(TipoFile.SUBSCRIPTION)).isEmpty());
        assertTrue(Binance_AutoInvest.MancantiPerRibilanciamenti(EnumSet.of(TipoFile.REDEMPTION)).isEmpty());
        assertTrue(Binance_AutoInvest.MancantiPerRibilanciamenti(
                EnumSet.of(TipoFile.SUBSCRIPTION, TipoFile.REDEMPTION)).isEmpty());
        assertTrue(Binance_AutoInvest.MancantiPerRibilanciamenti(EnumSet.noneOf(TipoFile.class)).isEmpty());
    }

    @Test
    public void ilRebalancingVuoleGliAltriDue() {
        assertEquals(List.of(TipoFile.SUBSCRIPTION, TipoFile.REDEMPTION),
                Binance_AutoInvest.MancantiPerRibilanciamenti(EnumSet.of(TipoFile.REBALANCING)));
        assertEquals(List.of(TipoFile.REDEMPTION),
                Binance_AutoInvest.MancantiPerRibilanciamenti(EnumSet.of(TipoFile.REBALANCING, TipoFile.SUBSCRIPTION)));
        assertEquals(List.of(TipoFile.SUBSCRIPTION),
                Binance_AutoInvest.MancantiPerRibilanciamenti(EnumSet.of(TipoFile.REBALANCING, TipoFile.REDEMPTION)));
        assertTrue(Binance_AutoInvest.MancantiPerRibilanciamenti(
                EnumSet.of(TipoFile.REBALANCING, TipoFile.SUBSCRIPTION, TipoFile.REDEMPTION)).isEmpty());
    }

    private static File Scrivi(File dir, String nome, String contenuto) throws Exception {
        File f = new File(dir, nome);
        Files.writeString(f.toPath(), contenuto, StandardCharsets.UTF_8);
        return f;
    }
}
