/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.math.BigDecimal;
import javax.swing.JPanel;

/**
 * Grafico a linee, dipinto a mano (nessuna libreria esterna), usato dal pannello
 * "Dettagli x Tipologia" del quadro RT. Mostra due serie sui 12 mesi dell'anno:
 * la plusvalenza mensile della tipologia di movimento selezionata e, come
 * riferimento, la plusvalenza mensile totale (somma di tutte le tipologie).
 *
 * <p>È un pannello di sola consultazione: non ha stato proprio oltre ai dati
 * impostati con {@link #impostaDati}, e ridisegna tutto in {@code paintComponent}.
 * I colori seguono il tema corrente ({@link Principale#tema}).
 */
public class RT_GraficoPlusvalenze extends JPanel {

    private static final String[] MESI = {
        "Gen", "Feb", "Mar", "Apr", "Mag", "Giu",
        "Lug", "Ago", "Set", "Ott", "Nov", "Dic"
    };

    private BigDecimal[] serieTipologia;
    private BigDecimal[] serieTotale;
    private String nomeTipologia = "";

    public RT_GraficoPlusvalenze() {
        setOpaque(true);
        setPreferredSize(new Dimension(520, 320));
        setMinimumSize(new Dimension(260, 180));
    }

    /**
     * Imposta le due serie (12 valori ciascuna, indice 0 = gennaio) e ridisegna.
     * Un parametro {@code null} viene trattato come serie assente.
     *
     * @param nomeTipologia etichetta della tipologia evidenziata, mostrata in legenda
     * @param serieTipologia plusvalenza mensile della tipologia selezionata
     * @param serieTotale plusvalenza mensile totale (tutte le tipologie)
     */
    public void impostaDati(String nomeTipologia, BigDecimal[] serieTipologia, BigDecimal[] serieTotale) {
        this.nomeTipologia = nomeTipologia == null ? "" : nomeTipologia;
        this.serieTipologia = serieTipologia;
        this.serieTotale = serieTotale;
        repaint();
    }

    /** Svuota il grafico (nessuna tipologia selezionata). */
    public void pulisci() {
        this.nomeTipologia = "";
        this.serieTipologia = null;
        this.serieTotale = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            boolean scuro = Principale.tema != null && Principale.tema.equalsIgnoreCase("Scuro");
            Color sfondo = scuro ? new Color(60, 63, 65) : Color.WHITE;
            Color testo = scuro ? new Color(210, 210, 210) : new Color(40, 40, 40);
            Color assi = scuro ? new Color(120, 120, 120) : new Color(150, 150, 150);
            Color griglia = scuro ? new Color(80, 83, 85) : new Color(225, 225, 225);
            Color coloreTipologia = scuro ? new Color(120, 180, 255) : new Color(31, 119, 180);
            Color coloreTotale = scuro ? new Color(255, 180, 120) : new Color(214, 120, 40);

            int w = getWidth();
            int h = getHeight();
            g2.setColor(sfondo);
            g2.fillRect(0, 0, w, h);

            Font base = getFont();
            if (base == null) base = new Font("Noto Sans", Font.PLAIN, 12);
            g2.setFont(base);
            int fh = g2.getFontMetrics().getHeight();

            int margSx = 70;
            int margDx = 16;
            int margSu = 12 + fh;               // spazio per la legenda
            int margGiu = 16 + fh;              // spazio per le etichette dei mesi
            int plotW = w - margSx - margDx;
            int plotH = h - margSu - margGiu;

            if (plotW < 40 || plotH < 40) {
                return;
            }

            boolean haTip = contieneValori(serieTipologia);
            boolean haTot = contieneValori(serieTotale);

            if (!haTip && !haTot) {
                g2.setColor(testo);
                String msg = "Seleziona una tipologia per vedere il grafico";
                int mw = g2.getFontMetrics().stringWidth(msg);
                g2.drawString(msg, margSx + (plotW - mw) / 2, margSu + plotH / 2);
                return;
            }

            // Intervallo dei valori: sempre incluso lo zero
            double minV = 0;
            double maxV = 0;
            minV = Math.min(minV, minSerie(serieTipologia));
            maxV = Math.max(maxV, maxSerie(serieTipologia));
            minV = Math.min(minV, minSerie(serieTotale));
            maxV = Math.max(maxV, maxSerie(serieTotale));
            if (minV == maxV) {
                // tutto a zero o valore unico: apro un minimo di scala per non dividere per zero
                maxV = minV + 1;
                minV = minV - 1;
            }
            double span = maxV - minV;

            int x0 = margSx;
            int y0 = margSu;
            int yBase = y0 + plotH;

            // Griglia orizzontale + etichette valori (5 tacche)
            g2.setFont(base);
            for (int i = 0; i <= 5; i++) {
                int yy = y0 + (int) Math.round(plotH * i / 5.0);
                double val = maxV - span * i / 5.0;
                g2.setColor(griglia);
                g2.drawLine(x0, yy, x0 + plotW, yy);
                g2.setColor(testo);
                String et = formattaValore(val);
                int ew = g2.getFontMetrics().stringWidth(et);
                g2.drawString(et, x0 - 8 - ew, yy + g2.getFontMetrics().getAscent() / 2 - 2);
            }

            // Asse X (linea dello zero, evidenziata)
            int yZero = (int) Math.round(y0 + plotH * (maxV - 0) / span);
            g2.setColor(assi);
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawLine(x0, yZero, x0 + plotW, yZero);
            // Asse Y
            g2.drawLine(x0, y0, x0, yBase);

            // Etichette dei mesi
            g2.setColor(testo);
            for (int m = 0; m < 12; m++) {
                int xx = xMese(m, x0, plotW);
                String et = MESI[m];
                int ew = g2.getFontMetrics().stringWidth(et);
                g2.drawString(et, xx - ew / 2, yBase + g2.getFontMetrics().getAscent() + 2);
            }

            // Serie totale (riferimento) - tratteggiata
            Stroke strokeTratteggio = new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{6f, 5f}, 0f);
            disegnaSerie(g2, serieTotale, x0, plotW, y0, plotH, maxV, span, coloreTotale, strokeTratteggio, false);
            // Serie tipologia selezionata - piena, con marcatori
            disegnaSerie(g2, serieTipologia, x0, plotW, y0, plotH, maxV, span, coloreTipologia,
                    new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND), true);

            // Legenda
            int lx = x0 + 4;
            int ly = 4 + g2.getFontMetrics().getAscent();
            g2.setColor(coloreTipologia);
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawLine(lx, ly - 4, lx + 22, ly - 4);
            g2.setColor(testo);
            String etTip = nomeTipologia.isBlank() ? "Tipologia selezionata" : nomeTipologia;
            g2.drawString(etTip, lx + 28, ly);
            int off = lx + 28 + g2.getFontMetrics().stringWidth(etTip) + 24;
            g2.setColor(coloreTotale);
            g2.setStroke(strokeTratteggio);
            g2.drawLine(off, ly - 4, off + 22, ly - 4);
            g2.setColor(testo);
            g2.drawString("Totale (tutte le tipologie)", off + 28, ly);

        } finally {
            g2.dispose();
        }
    }

    private void disegnaSerie(Graphics2D g2, BigDecimal[] serie, int x0, int plotW, int y0, int plotH,
                              double maxV, double span, Color colore, Stroke stroke, boolean marcatori) {
        if (!contieneValori(serie)) return;
        g2.setColor(colore);
        g2.setStroke(stroke);
        int[] xs = new int[12];
        int[] ys = new int[12];
        for (int m = 0; m < 12; m++) {
            double val = serie[m] == null ? 0 : serie[m].doubleValue();
            xs[m] = xMese(m, x0, plotW);
            ys[m] = (int) Math.round(y0 + plotH * (maxV - val) / span);
        }
        for (int m = 0; m < 11; m++) {
            g2.drawLine(xs[m], ys[m], xs[m + 1], ys[m + 1]);
        }
        if (marcatori) {
            for (int m = 0; m < 12; m++) {
                g2.fillOval(xs[m] - 3, ys[m] - 3, 6, 6);
            }
        }
    }

    private static int xMese(int m, int x0, int plotW) {
        return x0 + (int) Math.round(plotW * (m + 0.5) / 12.0);
    }

    private static boolean contieneValori(BigDecimal[] serie) {
        if (serie == null) return false;
        for (BigDecimal b : serie) {
            if (b != null && b.signum() != 0) return true;
        }
        return false;
    }

    private static double minSerie(BigDecimal[] serie) {
        if (serie == null) return 0;
        double min = 0;
        for (BigDecimal b : serie) {
            if (b != null) min = Math.min(min, b.doubleValue());
        }
        return min;
    }

    private static double maxSerie(BigDecimal[] serie) {
        if (serie == null) return 0;
        double max = 0;
        for (BigDecimal b : serie) {
            if (b != null) max = Math.max(max, b.doubleValue());
        }
        return max;
    }

    private static String formattaValore(double v) {
        double abs = Math.abs(v);
        if (abs >= 1_000_000) return String.format("%.1fM", v / 1_000_000);
        if (abs >= 1_000) return String.format("%.1fk", v / 1_000);
        if (abs >= 1) return String.format("%.0f", v);
        return String.format("%.2f", v);
    }
}
