package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** I tre selettori di "Giacenze a data": la stringa unica per il calcolo e il restringimento dei wallet. */
class Principale_GiacenzeaData_FiltroWalletTest {

    private static final Map<String, String> GRUPPO_DI = new LinkedHashMap<>();

    static {
        GRUPPO_DI.put("Alfa", "Wallet 1");
        GRUPPO_DI.put("Beta", "Wallet 2");
        GRUPPO_DI.put("Gamma", "Wallet 1");
    }

    @Test
    void effettivoPreferisceIlWalletAlGruppo() {
        assertEquals("Alfa", Principale_GiacenzeaData_FiltroWallet.Effettivo("Alfa", "Wallet 1 ( uno )"));
    }

    @Test
    void effettivoConSoloIlGruppoUsaLaFormaDelCalcolo() {
        assertEquals("Gruppo : Wallet 1 ( uno )", Principale_GiacenzeaData_FiltroWallet.Effettivo("Tutti", "Wallet 1 ( uno )"));
    }

    @Test
    void effettivoSenzaNullaETutti() {
        assertEquals("Tutti", Principale_GiacenzeaData_FiltroWallet.Effettivo("Tutti", "Tutti"));
        assertEquals("Tutti", Principale_GiacenzeaData_FiltroWallet.Effettivo(null, " tutti "));
    }

    @Test
    void idGruppoAccettaVoceEForma() {
        assertEquals("Wallet 1", Principale_GiacenzeaData_FiltroWallet.IdGruppo("Wallet 1 ( uno )"));
        assertEquals("Wallet 1", Principale_GiacenzeaData_FiltroWallet.IdGruppo("Gruppo : Wallet 1 ( uno )"));
        assertEquals("Wallet 1", Principale_GiacenzeaData_FiltroWallet.IdGruppo("Gruppo : Wallet 1"));
    }

    @Test
    void walletDelGruppoRestringeEMantieneLOrdine() {
        List<String> r = Principale_GiacenzeaData_FiltroWallet.WalletDelGruppo(GRUPPO_DI.keySet(), "Wallet 1 ( uno )", GRUPPO_DI::get);
        assertEquals(List.of("Tutti", "Alfa", "Gamma"), r);
    }

    @Test
    void walletDelGruppoConTuttiNonRestringe() {
        List<String> r = Principale_GiacenzeaData_FiltroWallet.WalletDelGruppo(GRUPPO_DI.keySet(), "Tutti", GRUPPO_DI::get);
        assertEquals(List.of("Tutti", "Alfa", "Beta", "Gamma"), r);
    }

    @Test
    void trovaVoceGruppoIgnoraLAliasCambiato() {
        List<String> voci = List.of("Tutti", "Wallet 1 ( nuovo alias )", "Wallet 2 ( due )");
        assertEquals("Wallet 1 ( nuovo alias )", Principale_GiacenzeaData_FiltroWallet.TrovaVoceGruppo(voci, "Gruppo : Wallet 1 ( vecchio )"));
        assertNull(Principale_GiacenzeaData_FiltroWallet.TrovaVoceGruppo(voci, "Wallet 9"));
        assertNull(Principale_GiacenzeaData_FiltroWallet.TrovaVoceGruppo(voci, "Tutti"));
    }

    @Test
    void isTutti() {
        assertTrue(Principale_GiacenzeaData_FiltroWallet.IsTutti("Tutti"));
        assertTrue(Principale_GiacenzeaData_FiltroWallet.IsTutti(""));
        assertFalse(Principale_GiacenzeaData_FiltroWallet.IsTutti("Alfa"));
    }

    @Test
    void nomeWalletTogliEtichettaDelMancante() {
        String et = Principale_GiacenzeaData_FiltroWallet.EtichettaWalletMancante("Alfa");
        assertTrue(et.contains("non più disponibile"));
        assertEquals("Alfa", Principale_GiacenzeaData_FiltroWallet.NomeWallet(et));
        assertEquals("Alfa", Principale_GiacenzeaData_FiltroWallet.NomeWallet("Alfa"));
        assertNull(Principale_GiacenzeaData_FiltroWallet.NomeWallet(null));
    }
}
