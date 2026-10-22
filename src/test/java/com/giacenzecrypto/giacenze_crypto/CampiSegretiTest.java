package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le credenziali (chiavi API, secret, passphrase) non si vedono a prima vista: {@link CampiSegreti}.
 */
class CampiSegretiTest {

    private static final Path SORGENTI = Path.of("src/main/java/com/giacenzecrypto/giacenze_crypto");

    /** I nomi di componente che contengono una credenziale. */
    private static final Pattern NOME_CREDENZIALE = Pattern.compile("(?i)apikey|apisecret|passphrase");

    private static final Pattern COMPONENTE =
            Pattern.compile("<Component class=\"([\\w.]+)\" name=\"(\\w+)\">");

    /**
     * Nessuna casella di credenziale torna una {@code JTextField} in chiaro: è il modo in cui la
     * protezione si perde senza che nulla lo segnali, rigenerando o copiando un .form.
     */
    @Test
    void leCaselleDelleCredenzialiSonoCampiPassword() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(SORGENTI), "sorgenti non disponibili");
        List<String> inChiaro = new ArrayList<>();
        int trovate = 0;
        try (Stream<Path> forms = Files.list(SORGENTI)) {
            for (Path form : forms.filter(p -> p.toString().endsWith(".form")).toList()) {
                Matcher m = COMPONENTE.matcher(Files.readString(form, StandardCharsets.UTF_8));
                while (m.find()) {
                    String classe = m.group(1);
                    String nome = m.group(2);
                    if (!NOME_CREDENZIALE.matcher(nome).find()) continue;
                    if (classe.equals("javax.swing.JPasswordField")) trovate++;
                    else if (classe.equals("javax.swing.JTextField")) inChiaro.add(form.getFileName() + ": " + nome);
                }
            }
        }
        assertTrue(inChiaro.isEmpty(), "Caselle di credenziali in chiaro: " + inChiaro);
        //3 in Exchange API, 8 nelle Opzioni: se il conto scende il pattern non trova più i nomi
        assertTrue(trovate >= 11, "trovate solo " + trovate + " caselle di credenziali");
    }

    @Test
    void laMascheraNonRivelaNeContenutoNeLunghezza() {
        assertEquals("", CampiSegreti.Mascherata(null));
        assertEquals("", CampiSegreti.Mascherata("  "));
        String corta = CampiSegreti.Mascherata("abc");
        String lunga = CampiSegreti.Mascherata("abcdefghijklmnopqrstuvwxyz0123456789");
        assertEquals(CampiSegreti.LUNGHEZZA_MASCHERA, corta.length());
        assertEquals(corta, lunga);
        assertFalse(lunga.contains("a"));
    }

    @Test
    void laTabellaNascondeSoloIlDisegnoNonIlModello() {
        DefaultTableModel modello = new DefaultTableModel(new Object[][]{{"Binance", "chiave-vera", ""}},
                new String[]{"Exchange", "API Key", "Passphrase"});
        JTable tabella = new JTable(modello);
        boolean[] mostra = {false};
        CampiSegreti.NascondiColonne(tabella, () -> mostra[0], 1, 2);

        assertEquals(CampiSegreti.Mascherata("x"), testoCella(tabella, 0, 1));
        assertEquals("", testoCella(tabella, 0, 2), "una passphrase assente resta vuota");
        assertEquals("Binance", testoCella(tabella, 0, 0), "le altre colonne non cambiano");
        assertEquals("chiave-vera", modello.getValueAt(0, 1), "il modello tiene la chiave vera");

        mostra[0] = true;
        assertEquals("chiave-vera", testoCella(tabella, 0, 1));
    }

    @Test
    void ilCampoPasswordRestituisceLaChiaveIntera() {
        //Il resto del programma legge le caselle con getText(): con JPasswordField deve dare lo stesso testo
        JTextField campo = new JPasswordField();
        CampiSegreti.AggiungiOcchio((JPasswordField) campo);
        campo.setText(" chiave ");
        assertEquals(" chiave ", campo.getText());
    }

    private static String testoCella(JTable tabella, int riga, int colonna) {
        return ((javax.swing.JLabel) tabella.prepareRenderer(tabella.getCellRenderer(riga, colonna), riga, colonna)).getText();
    }
}
