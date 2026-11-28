package com.giacenzecrypto.giacenze_crypto;

import com.formdev.flatlaf.FlatLightLaf;

/**
 * Terzo tema dell'applicazione ("Blu"): una variante <b>chiara</b> con una tavolozza blu notte, azzurro,
 * fondo grigio-azzurro e schede bianche.
 *
 * <p>È una sottoclasse di {@link FlatLightLaf} solo per un motivo: FlatLaf cerca i valori personalizzati in
 * {@code Temi/<NomeClasse>.properties}, quindi questo tema ha il suo file
 * ({@code Temi/FlatTemaBluLaf.properties}) e il tema "Chiaro" di serie, che non ne ha uno, resta
 * identico. Non cambia nessun componente: sono solo valori di {@code UIManager}.</p>
 *
 * <p>Per tutto il resto del programma è un tema chiaro: il controllo è sempre
 * {@code tema.equalsIgnoreCase("Scuro")}, quindi icone nere, colori delle righe e dialoghi lo trattano
 * come il chiaro, e non serve toccare nessuno dei quaranta punti che leggono {@link Principale#tema}.
 * Lo stesso vale al contrario: una nuova stringa di tema non deve mai essere confrontata con "Chiaro".</p>
 */
public class FlatTemaBluLaf extends FlatLightLaf {

    /** Valore salvato nell'opzione {@code Tema} di {@code database.mv.db}. */
    public static final String NOME_TEMA = "Blu";

    public static boolean setup() {
        return setup(new FlatTemaBluLaf());
    }

    @Override
    public String getName() {
        return "FlatTemaBluLaf";
    }

    @Override
    public String getDescription() {
        return "Tema chiaro con la tavolozza blu notte";
    }

    /** true se il tema salvato è questo (confronto senza maiuscole, come per "Scuro"). */
    public static boolean is(String tema) {
        return tema != null && tema.equalsIgnoreCase(NOME_TEMA);
    }

    /**
     * Colori che le tabelle e i testi HTML leggono da campi statici di {@link Tabelle} e non da
     * {@code UIManager}: vanno impostati insieme al Look&amp;Feel, all'avvio, come fa il ramo del tema scuro.
     * Stanno qui, e non dentro {@code Giacenze_Crypto.main}, perché siano provabili e perché il cambio
     * tema a caldo di {@code Principale} li usi senza copiarli.
     */
    public static void ImpostaColoriTabelle() {
        SalvaOriginali();
        // Verde e rosso della dashboard (positivo / negativo), leggibili su fondo bianco
        Tabelle.verdeScuro = new java.awt.Color(22, 109, 63);
        Tabelle.rosso = new java.awt.Color(163, 36, 47);
        Tabelle.Rosso = "A3242F";
        Tabelle.Verde = "166D3F";
        // Riga pari delle tabelle (la dispari è bianca): lo stesso azzurro della riga dei totali del riepilogo annuale
        Tabelle.grigioChiaro = new java.awt.Color(238, 243, 250);
        // Selezione: il blu della voce attiva della barra laterale, con due sfumature per la riga pari/dispari
        Tabelle.selezione = new java.awt.Color(38, 103, 188);
        Tabelle.selezioneAlt = new java.awt.Color(58, 120, 202);
    }

    // I valori che questo tema sovrascrive in Tabelle, presi prima della prima sovrascrittura: servono solo
    // al cambio tema a caldo, per tornare al chiaro di serie senza ripetere qui i suoi numeri.
    private static java.awt.Color origGrigioChiaro, origSelezione, origSelezioneAlt;

    private static void SalvaOriginali() {
        if (origGrigioChiaro != null) return;
        origGrigioChiaro = Tabelle.grigioChiaro;
        origSelezione = Tabelle.selezione;
        origSelezioneAlt = Tabelle.selezioneAlt;
    }

    /**
     * Rimette i colori di riga e di selezione del tema chiaro di serie, dopo un cambio a caldo da questo tema.
     * Non tocca verde e rosso: li reimposta il chiamante, come per ogni altro tema.
     */
    public static void RipristinaColoriTabelle() {
        if (origGrigioChiaro == null) return;
        Tabelle.grigioChiaro = origGrigioChiaro;
        Tabelle.selezione = origSelezione;
        Tabelle.selezioneAlt = origSelezioneAlt;
    }
}
