package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Le tre combo gruppo, wallet e sotto-wallet: la regola con cui si influenzano, su combo vere (senza finestra). */
class SelettoriWalletTest {

    private static final String G1 = "Wallet 1 ( uno )";
    private static final String G2 = "Wallet 2 ( due )";
    private static final Map<String, String> GRUPPO_DI = Map.of("Alfa", "Wallet 1", "Beta", "Wallet 2", "Gamma", "Wallet 1");
    private static final Map<String, List<String>> SOTTO = Map.of("Alfa", List.of("Principale", "Earn"), "Beta", List.of("Spot"));

    private JComboBox<String> gruppo;
    private JComboBox<String> wallet;
    private JComboBox<String> sotto;
    private SelettoriWallet sel;
    private int cambi;
    private List<String> noti;

    @BeforeEach
    void prepara() {
        gruppo = new JComboBox<>();
        wallet = new JComboBox<>();
        sotto = new JComboBox<>();
        noti = new ArrayList<>(List.of("Alfa", "Beta", "Gamma"));
        sel = new SelettoriWallet(gruppo, wallet, sotto, () -> noti, GRUPPO_DI::get, SOTTO::get);
        sel.ImpostaGruppi(List.of(G1, G2));
        sel.Applica("Tutti", "Tutti", "Tutti");
        cambi = 0;
        sel.setAlCambio(() -> cambi++);
    }

    private static List<String> voci(JComboBox<String> c) {
        List<String> r = new ArrayList<>();
        for (int i = 0; i < c.getItemCount(); i++) r.add(c.getItemAt(i));
        return r;
    }

    @Test
    void alPartireTuttoSuTuttiESottoWalletSpento() {
        assertEquals("Tutti", sel.Effettivo());
        assertEquals(List.of("Tutti", "Alfa", "Beta", "Gamma"), voci(wallet));
        assertFalse(sotto.isEnabled());
    }

    @Test
    void sceltoUnGruppoIWalletSiRestringono() {
        gruppo.setSelectedItem(G1);
        assertEquals(List.of("Tutti", "Alfa", "Gamma"), voci(wallet));
        assertEquals("Gruppo : " + G1, sel.Effettivo());
        assertEquals(1, cambi);
    }

    @Test
    void sceltoUnWalletIlGruppoSiSposta() {
        wallet.setSelectedItem("Beta");
        assertEquals(G2, gruppo.getSelectedItem());
        assertEquals("Beta", sel.Effettivo());
        assertTrue(sotto.isEnabled());
        assertEquals(List.of("Tutti", "Spot"), voci(sotto));
    }

    @Test
    void cambiandoGruppoUnWalletEstraneoTornaATuttiESpegneIlSottoWallet() {
        wallet.setSelectedItem("Beta");
        gruppo.setSelectedItem(G1);
        assertEquals("Tutti", wallet.getSelectedItem());
        assertFalse(sotto.isEnabled());
        assertEquals("Gruppo : " + G1, sel.Effettivo());
    }

    @Test
    void cambiandoWalletIlSottoWalletRiparteDaTutti() {
        wallet.setSelectedItem("Alfa");
        sotto.setSelectedItem("Earn");
        assertEquals("Earn", sel.SottoWalletScelto());
        wallet.setSelectedItem("Gamma");
        assertEquals("Tutti", sel.SottoWalletScelto());
    }

    @Test
    void ilSottoWalletCambiatoDallUtenteAvvisa() {
        wallet.setSelectedItem("Alfa");
        int prima = cambi;
        sotto.setSelectedItem("Earn");
        assertEquals(prima + 1, cambi);
    }

    @Test
    void unWalletScomparsoRestaComeVoceECosiIlFiltroNonSparisce() {
        sel.Applica("Tutti", "Delta", "Tutti");
        assertEquals("Delta", sel.Effettivo());
        assertTrue(String.valueOf(wallet.getSelectedItem()).contains("non più disponibile"));
        //riapplicare con la voce stessa non la duplica
        sel.Applica("Tutti", String.valueOf(wallet.getSelectedItem()), "Tutti");
        assertEquals("Delta", sel.Effettivo());
        assertEquals(5, wallet.getItemCount());
    }

    @Test
    void ImpostaDaFiltroAccettaTuttiGruppoEWallet() {
        sel.ImpostaDaFiltro("Gruppo : " + G2, "Tutti");
        assertEquals("Gruppo : " + G2, sel.Effettivo());
        assertEquals(List.of("Tutti", "Beta"), voci(wallet));

        sel.ImpostaDaFiltro("Alfa", "Earn");
        assertEquals("Alfa", sel.Effettivo());
        assertEquals(G1, gruppo.getSelectedItem());
        assertEquals("Earn", sel.SottoWalletScelto());

        sel.ImpostaDaFiltro("Tutti", "Tutti");
        assertEquals("Tutti", sel.Effettivo());
        assertFalse(sotto.isEnabled());
    }

    @Test
    void ungruppoCheNonCePiuRestaComeVoce() {
        sel.ImpostaDaFiltro("Gruppo : Wallet 9 ( nove )", "Tutti");
        assertEquals("Gruppo : Wallet 9 ( nove )", sel.Effettivo());
    }

    @Test
    void ilSottoWalletNonEsisteSenzaWallet() {
        sel.Applica("Tutti", "Tutti", "Earn");
        assertEquals("Tutti", sel.SottoWalletScelto());
        assertFalse(sotto.isEnabled());
    }
}
