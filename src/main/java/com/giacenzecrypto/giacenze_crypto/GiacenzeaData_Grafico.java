package com.giacenzecrypto.giacenze_crypto;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Path2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

/**
 * Grafico della scheda "Giacenze a data", dipinto a mano come {@link RT_GraficoPlusvalenze}: la giacenza della
 * moneta selezionata nel tempo (a gradini, asse di sinistra) e il suo valore in euro (asse di destra), con un
 * marcatore su ogni variazione della giacenza. Il mouse su un marcatore mostra il movimento (o i movimenti, se
 * cadono sullo stesso punto) che ha causato la variazione; su un punto della linea del valore, giacenza, prezzo e
 * valore di quel giorno. Rotella: zoom sul tempo attorno al mouse; trascinamento: spostamento; doppio clic: tutto il
 * periodo; clic su un marcatore: il movimento nella tabella ({@link #setAlClicMovimento}).
 *
 * <p>Solo disegno: i dati arrivano già pronti da {@link GraficoGiacenze}.
 */
public class GiacenzeaData_Grafico extends JPanel {

    private static final DateTimeFormatter DATA_ORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale IT = Locale.ITALY;
    private static final long ORA_MS = 3_600_000L;
    private static final int RAGGIO_MARCATORE = 4;
    private static final int TOLLERANZA_PX = 6;
    private static final int MAX_MOVIMENTI_TOOLTIP = 6;

    private GraficoGiacenze.Serie serie;
    private String moneta = "";
    private String stato = "Seleziona una moneta nella tabella sopra per vedere il grafico";

    /** Intervallo di tempo visibile; {@code vistaDa == vistaA} vuol dire "tutto". */
    private long vistaDa;
    private long vistaA;

    /** Geometria dell'ultimo disegno, per il mouse. */
    private int x0, plotW, y0, plotH;
    private double minQ, maxQ, minV, maxV;

    private int mouseX = -1;
    private Point inizioTrascinamento;
    private long trascinamentoDa, trascinamentoA;
    private Consumer<String> alClicMovimento;

    public GiacenzeaData_Grafico() {
        setOpaque(true);
        setPreferredSize(new Dimension(600, 300));
        setMinimumSize(new Dimension(260, 160));
        ToolTipManager.sharedInstance().registerComponent(this);
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouseX = -1;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                inizioTrascinamento = e.getPoint();
                trascinamentoDa = da();
                trascinamentoA = a();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                inizioTrascinamento = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (inizioTrascinamento == null || serie == null || plotW <= 0) return;
                long ampiezza = trascinamentoA - trascinamentoDa;
                long spostamento = (long) ((double) (inizioTrascinamento.x - e.getX()) / plotW * ampiezza);
                imposta(trascinamentoDa + spostamento, trascinamentoA + spostamento);
                mouseX = e.getX();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (serie == null) return;
                if (e.getClickCount() == 2) {
                    vistaDa = vistaA = 0;
                    repaint();
                    return;
                }
                List<GraficoGiacenze.Variazione> sotto = variazioniSotto(e.getX(), e.getY());
                if (!sotto.isEmpty() && alClicMovimento != null) alClicMovimento.accept(sotto.get(0).ID());
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (serie == null || plotW <= 0) return;
                long da = da(), a = a();
                double fattore = e.getWheelRotation() < 0 ? 0.8 : 1.25;
                double frazione = Math.max(0, Math.min(1, (double) (e.getX() - x0) / plotW));
                long centro = da + (long) ((a - da) * frazione);
                long ampiezza = Math.max(ORA_MS, (long) ((a - da) * fattore));
                imposta(centro - (long) (ampiezza * frazione), centro + (long) (ampiezza * (1 - frazione)));
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
        addMouseWheelListener(m);
    }

    /** Cosa fare al clic su un marcatore: riceve l'ID del movimento. */
    public void setAlClicMovimento(Consumer<String> azione) {
        this.alClicMovimento = azione;
    }

    /**
     * Mostra una serie. La vista torna a tutto il periodo solo se cambia la moneta o il periodo, così l'arrivo dei
     * prezzi non perde lo zoom.
     *
     * @param stato riga di testo sotto la legenda (prezzi in arrivo, giorni senza prezzo), vuota per nessuna
     */
    public void impostaSerie(GraficoGiacenze.Serie serie, String moneta, String stato) {
        boolean stessa = this.serie != null && serie != null && this.moneta.equals(moneta)
                && this.serie.Inizio() == serie.Inizio() && this.serie.Fine() == serie.Fine();
        this.serie = serie;
        this.moneta = moneta == null ? "" : moneta;
        this.stato = stato == null ? "" : stato;
        if (!stessa) vistaDa = vistaA = 0;
        repaint();
    }

    /** Svuota il grafico lasciando un messaggio. */
    public void pulisci(String messaggio) {
        this.serie = null;
        this.moneta = "";
        this.stato = messaggio == null ? "" : messaggio;
        vistaDa = vistaA = 0;
        repaint();
    }

    private long inizioSerie() {
        return serie.Inizio();
    }

    private long fineSerie() {
        return Math.max(serie.Fine(), serie.Inizio() + ORA_MS);
    }

    private long da() {
        return vistaDa == vistaA ? inizioSerie() : vistaDa;
    }

    private long a() {
        return vistaDa == vistaA ? fineSerie() : vistaA;
    }

    /** Imposta la vista dentro i limiti della serie, senza cambiarne l'ampiezza se si sposta. */
    private void imposta(long da, long a) {
        long min = inizioSerie(), max = fineSerie();
        long ampiezza = Math.min(a - da, max - min);
        if (da < min) {
            da = min;
            a = da + ampiezza;
        }
        if (a > max) {
            a = max;
            da = a - ampiezza;
        }
        if (da <= min && a >= max) {
            vistaDa = vistaA = 0;
        } else {
            vistaDa = da;
            vistaA = a;
        }
        repaint();
    }

    //Le linee si disegnano con le coordinate esatte: arrotondate al pixel, un valore piccolo su un asse che parte da
    //zero diventava una scala di gradini. Gli interi servono a marcatori e mouse.
    private double xd(long t) {
        long da = da(), a = a();
        return x0 + (double) (t - da) / (a - da) * plotW;
    }

    private double yQd(double q) {
        return y0 + plotH * (maxQ - q) / (maxQ - minQ);
    }

    private double yVd(double v) {
        return y0 + plotH * (maxV - v) / (maxV - minV);
    }

    private int x(long t) {
        return (int) Math.round(xd(t));
    }

    private int yQ(double q) {
        return (int) Math.round(yQd(q));
    }

    private int yV(double v) {
        return (int) Math.round(yVd(v));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Colori c = new Colori();
            int w = getWidth(), h = getHeight();
            g2.setColor(c.sfondo);
            g2.fillRect(0, 0, w, h);
            Font base = getFont() != null ? getFont() : new Font("Noto Sans", Font.PLAIN, 12);
            g2.setFont(base);
            FontMetrics fm = g2.getFontMetrics();
            int fh = fm.getHeight();

            if (serie == null || serie.Variazioni().isEmpty()) {
                g2.setColor(c.testo);
                String msg = serie == null ? stato : "Nessun movimento della moneta prima della data scelta";
                g2.drawString(msg, Math.max(8, (w - fm.stringWidth(msg)) / 2), h / 2);
                return;
            }

            x0 = 86;
            int margDx = 90;
            y0 = 10 + fh * 2 + 6;
            plotW = w - x0 - margDx;
            plotH = h - y0 - fh - 14;
            if (plotW < 60 || plotH < 40) return;

            long da = da(), a = a();
            List<GraficoGiacenze.Variazione> var = serie.Variazioni();
            List<GraficoGiacenze.PuntoValore> val = serie.Valori();
            calcolaScale(var, val, da, a);

            //Griglia e scale
            for (int i = 0; i <= 5; i++) {
                int yy = y0 + (int) Math.round(plotH * i / 5.0);
                g2.setColor(c.griglia);
                g2.drawLine(x0, yy, x0 + plotW, yy);
                g2.setColor(c.qta);
                String et = formattaNumero(maxQ - (maxQ - minQ) * i / 5.0);
                g2.drawString(et, x0 - 6 - fm.stringWidth(et), yy + fm.getAscent() / 2 - 2);
                if (serie.HaValori()) {
                    g2.setColor(c.valore);
                    g2.drawString(formattaEuroBreve(maxV - (maxV - minV) * i / 5.0), x0 + plotW + 6, yy + fm.getAscent() / 2 - 2);
                }
            }
            disegnaAsseTempo(g2, fm, c, da, a);
            g2.setColor(c.assi);
            g2.drawRect(x0, y0, plotW, plotH);
            if (minQ < 0 && maxQ > 0) {
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawLine(x0, yQ(0), x0 + plotW, yQ(0));
            }

            g2.setClip(x0, y0 - RAGGIO_MARCATORE - 1, plotW + 1, plotH + 2 * RAGGIO_MARCATORE + 2);
            disegnaValore(g2, c, val);
            disegnaGiacenza(g2, c, var, da, a);
            g2.setClip(null);

            //Linea verticale del mouse
            if (mouseX >= x0 && mouseX <= x0 + plotW) {
                g2.setColor(c.cursore);
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{3f, 3f}, 0f));
                g2.drawLine(mouseX, y0, mouseX, y0 + plotH);
                String quando = DATA.format(Instant.ofEpochMilli(tempo(mouseX)).atZone(GraficoGiacenze.FUSO));
                g2.setColor(c.testo);
                int lw = fm.stringWidth(quando);
                g2.drawString(quando, Math.min(Math.max(x0, mouseX - lw / 2), x0 + plotW - lw), y0 - 3);
            }

            disegnaLegenda(g2, fm, c);
        } finally {
            g2.dispose();
        }
    }

    private long tempo(int px) {
        long da = da(), a = a();
        return da + (long) ((double) (px - x0) / plotW * (a - da));
    }

    private void calcolaScale(List<GraficoGiacenze.Variazione> var, List<GraficoGiacenze.PuntoValore> val, long da, long a) {
        double qPrima = 0;
        minQ = 0;
        maxQ = 0;
        boolean primo = true;
        for (GraficoGiacenze.Variazione v : var) {
            double q = v.QtaDopo().doubleValue();
            if (v.Istante() < da) {
                qPrima = q;
                continue;
            }
            if (primo) {
                minQ = Math.min(minQ, qPrima);
                maxQ = Math.max(maxQ, qPrima);
                primo = false;
            }
            if (v.Istante() > a) break;
            minQ = Math.min(minQ, q);
            maxQ = Math.max(maxQ, q);
        }
        if (primo) {
            minQ = Math.min(minQ, qPrima);
            maxQ = Math.max(maxQ, qPrima);
        }
        if (maxQ == minQ) maxQ = minQ + 1;
        double margine = (maxQ - minQ) * 0.05;
        maxQ += margine;
        if (minQ < 0) minQ -= margine;

        minV = 0;
        maxV = 0;
        for (GraficoGiacenze.PuntoValore p : val) {
            if (p.Valore() == null || p.Istante() < da || p.Istante() > a) continue;
            minV = Math.min(minV, p.Valore().doubleValue());
            maxV = Math.max(maxV, p.Valore().doubleValue());
        }
        if (maxV == minV) maxV = minV + 1;
        margine = (maxV - minV) * 0.05;
        maxV += margine;
        if (minV < 0) minV -= margine;
    }

    private void disegnaValore(Graphics2D g2, Colori c, List<GraficoGiacenze.PuntoValore> val) {
        if (!serie.HaValori()) return;
        g2.setColor(c.valore);
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double linea = new Path2D.Double();
        boolean aperta = false;
        long da = da(), a = a();
        for (GraficoGiacenze.PuntoValore p : val) {
            if (p.Valore() == null) {
                aperta = false;
                continue;
            }
            //Un punto fuori vista per lato basta a far entrare la linea dal bordo
            if (p.Istante() < da - (a - da) || p.Istante() > a + (a - da)) continue;
            double px = xd(p.Istante()), py = yVd(p.Valore().doubleValue());
            if (aperta) linea.lineTo(px, py);
            else linea.moveTo(px, py);
            aperta = true;
        }
        g2.draw(linea);
    }

    private void disegnaGiacenza(Graphics2D g2, Colori c, List<GraficoGiacenze.Variazione> var, long da, long a) {
        g2.setColor(c.qta);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double gradini = new Path2D.Double();
        double q = 0;
        boolean iniziata = false;
        for (GraficoGiacenze.Variazione v : var) {
            double px = xd(v.Istante());
            if (!iniziata) {
                gradini.moveTo(px, yQd(q));
                iniziata = true;
            }
            gradini.lineTo(px, yQd(q));
            q = v.QtaDopo().doubleValue();
            gradini.lineTo(px, yQd(q));
        }
        gradini.lineTo(xd(fineSerie()), yQd(q));
        g2.draw(gradini);

        //Marcatori: uno per pixel di ascissa, l'ultimo movimento di quel pixel decide il colore
        int ultimoX = Integer.MIN_VALUE;
        for (int i = 0; i < var.size(); i++) {
            GraficoGiacenze.Variazione v = var.get(i);
            if (v.Istante() < da || v.Istante() > a) continue;
            int px = x(v.Istante());
            boolean ultimoDelPixel = i == var.size() - 1 || x(var.get(i + 1).Istante()) != px;
            if (!ultimoDelPixel || px == ultimoX) continue;
            ultimoX = px;
            disegnaMarcatore(g2, c, px, yQ(v.QtaDopo().doubleValue()), v.Qta().signum() >= 0);
        }
    }

    private static void disegnaMarcatore(Graphics2D g2, Colori c, int px, int py, boolean entrata) {
        int r = RAGGIO_MARCATORE;
        Polygon p = new Polygon();
        if (entrata) {
            p.addPoint(px, py - r - 1);
            p.addPoint(px - r, py + r - 1);
            p.addPoint(px + r, py + r - 1);
        } else {
            p.addPoint(px, py + r + 1);
            p.addPoint(px - r, py - r + 1);
            p.addPoint(px + r, py - r + 1);
        }
        g2.setColor(entrata ? c.entrata : c.uscita);
        g2.fillPolygon(p);
    }

    private void disegnaAsseTempo(Graphics2D g2, FontMetrics fm, Colori c, long da, long a) {
        int maxEtichette = Math.max(2, plotW / 90);
        long ampiezzaGiorni = Math.max(1, (a - da) / 86_400_000L);
        int[] mesiPasso = {1, 2, 3, 6, 12, 24, 60};
        int[] giorniPasso = {1, 2, 7, 14};
        LocalDate inizio = Instant.ofEpochMilli(da).atZone(GraficoGiacenze.FUSO).toLocalDate();
        LocalDate fine = Instant.ofEpochMilli(a).atZone(GraficoGiacenze.FUSO).toLocalDate();
        List<LocalDate> tacche = new ArrayList<>();
        DateTimeFormatter formato;
        int passoGiorni = 0;
        for (int p : giorniPasso) {
            if (ampiezzaGiorni / p <= maxEtichette) {
                passoGiorni = p;
                break;
            }
        }
        if (passoGiorni > 0) {
            formato = DateTimeFormatter.ofPattern("dd/MM/yy");
            for (LocalDate d = inizio.plusDays(1); !d.isAfter(fine); d = d.plusDays(passoGiorni)) tacche.add(d);
        } else {
            int passoMesi = 120;
            for (int p : mesiPasso) {
                if (ampiezzaGiorni / 30 / p <= maxEtichette) {
                    passoMesi = p;
                    break;
                }
            }
            formato = passoMesi >= 12 ? DateTimeFormatter.ofPattern("yyyy") : DateTimeFormatter.ofPattern("MMM yy", IT);
            LocalDate d = inizio.withDayOfMonth(1).plusMonths(1);
            if (passoMesi >= 12) d = inizio.withDayOfYear(1).plusYears(1);
            while (passoMesi < 12 && (d.getMonthValue() - 1) % passoMesi != 0) d = d.plusMonths(1);
            for (; !d.isAfter(fine); d = d.plusMonths(passoMesi)) tacche.add(d);
        }
        for (LocalDate d : tacche) {
            long t = d.atStartOfDay(GraficoGiacenze.FUSO).toInstant().toEpochMilli();
            if (t < da || t > a) continue;
            int px = x(t);
            g2.setColor(c.griglia);
            g2.drawLine(px, y0, px, y0 + plotH);
            g2.setColor(c.testo);
            String et = formato.format(d);
            g2.drawString(et, px - fm.stringWidth(et) / 2, y0 + plotH + fm.getAscent() + 3);
        }
    }

    private void disegnaLegenda(Graphics2D g2, FontMetrics fm, Colori c) {
        int lx = x0;
        int ly = 6 + fm.getAscent();
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(c.qta);
        g2.drawLine(lx, ly - 4, lx + 20, ly - 4);
        g2.setColor(c.testo);
        String et = "Giacenza " + moneta + " (asse sinistro)";
        g2.drawString(et, lx + 26, ly);
        lx += 26 + fm.stringWidth(et) + 20;
        g2.setColor(c.valore);
        g2.drawLine(lx, ly - 4, lx + 20, ly - 4);
        g2.setColor(c.testo);
        et = "Valore in euro (asse destro)";
        g2.drawString(et, lx + 26, ly);
        lx += 26 + fm.stringWidth(et) + 20;
        disegnaMarcatore(g2, c, lx + 5, ly - 4, true);
        disegnaMarcatore(g2, c, lx + 17, ly - 4, false);
        g2.setColor(c.testo);
        g2.drawString("Movimenti", lx + 28, ly);

        int ly2 = ly + fm.getHeight();
        String guida = "Rotella: zoom   Trascina: sposta   Doppio clic: tutto il periodo";
        g2.setColor(c.secondario);
        g2.drawString(guida, getWidth() - 10 - fm.stringWidth(guida), ly2);
        if (!stato.isBlank()) {
            g2.setColor(c.stato);
            g2.drawString(stato, x0, ly2);
        }
    }

    /** Le variazioni il cui marcatore sta sotto il punto, nell'ordine del tempo. */
    private List<GraficoGiacenze.Variazione> variazioniSotto(int mx, int my) {
        List<GraficoGiacenze.Variazione> ris = new ArrayList<>();
        if (serie == null || plotW <= 0) return ris;
        long da = da(), a = a();
        for (GraficoGiacenze.Variazione v : serie.Variazioni()) {
            if (v.Istante() < da || v.Istante() > a) continue;
            int px = x(v.Istante());
            if (Math.abs(px - mx) > TOLLERANZA_PX) continue;
            int py = yQ(v.QtaDopo().doubleValue());
            //Il gradino verticale va dalla giacenza prima a quella dopo: vale tutto il tratto, non solo il marcatore
            int pyPrima = yQ(v.QtaDopo().subtract(v.Qta()).doubleValue());
            int alto = Math.min(py, pyPrima) - TOLLERANZA_PX - RAGGIO_MARCATORE;
            int basso = Math.max(py, pyPrima) + TOLLERANZA_PX + RAGGIO_MARCATORE;
            if (my >= alto && my <= basso) ris.add(v);
        }
        return ris;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (serie == null || serie.Variazioni().isEmpty() || plotW <= 0) return null;
        List<GraficoGiacenze.Variazione> sotto = variazioniSotto(e.getX(), e.getY());
        if (!sotto.isEmpty()) return tooltipMovimenti(sotto);
        //Punto della linea del valore più vicino all'ascissa del mouse
        GraficoGiacenze.PuntoValore migliore = null;
        int distanza = Integer.MAX_VALUE;
        for (GraficoGiacenze.PuntoValore p : serie.Valori()) {
            if (p.Valore() == null) continue;
            int d = Math.abs(x(p.Istante()) - e.getX());
            if (d < distanza) {
                distanza = d;
                migliore = p;
            }
        }
        if (migliore == null || distanza > TOLLERANZA_PX || Math.abs(yV(migliore.Valore().doubleValue()) - e.getY()) > 2 * TOLLERANZA_PX) {
            return null;
        }
        StringBuilder sb = new StringBuilder("<html>");
        ZonedDateTime quando = Instant.ofEpochMilli(migliore.Istante()).atZone(GraficoGiacenze.FUSO);
        if (migliore.Variazione() == null) {
            sb.append("<b>Fine giornata ").append(DATA.format(quando)).append("</b><br>");
        } else {
            sb.append("<b>Dopo il movimento del ").append(DATA_ORA.format(quando)).append("</b><br>");
        }
        sb.append("Giacenza: ").append(formattaQta(migliore.Qta())).append(' ').append(html(moneta)).append("<br>");
        if (migliore.Prezzo() != null) sb.append("Prezzo: ").append(formattaPrezzo(migliore.Prezzo())).append("<br>");
        sb.append("Valore: ").append(formattaEuro(migliore.Valore())).append("</html>");
        return sb.toString();
    }

    private String tooltipMovimenti(List<GraficoGiacenze.Variazione> sotto) {
        StringBuilder sb = new StringBuilder("<html>");
        int n = 0;
        for (GraficoGiacenze.Variazione v : sotto) {
            if (n == MAX_MOVIMENTI_TOOLTIP) break;
            if (n > 0) sb.append("<hr>");
            sb.append("<b>").append(html(v.Data())).append("</b> - ").append(html(v.Tipo())).append("<br>");
            sb.append("Wallet: ").append(html(v.Wallet())).append("<br>");
            String segno = v.Qta().signum() > 0 ? "+" : "";
            sb.append("Quantità: <b>").append(segno).append(formattaQta(v.Qta())).append("</b> ").append(html(moneta)).append("<br>");
            sb.append("Giacenza dopo: ").append(formattaQta(v.QtaDopo()));
            if (v.ValoreDopo() != null) sb.append(" (").append(formattaEuro(v.ValoreDopo())).append(')');
            n++;
        }
        if (sotto.size() > n) sb.append("<hr>+").append(sotto.size() - n).append(" altri movimenti");
        if (alClicMovimento != null) sb.append("<br><i>Clic: mostra il movimento nella tabella</i>");
        return sb.append("</html>").toString();
    }

    private static String html(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static String formattaQta(BigDecimal q) {
        if (q == null) return "";
        BigDecimal a = q.abs();
        if (a.compareTo(new BigDecimal("1000")) >= 0) return String.format(IT, "%,.2f", q);
        if (a.signum() == 0) return "0";
        BigDecimal r = q.round(new java.math.MathContext(8, RoundingMode.HALF_UP)).stripTrailingZeros();
        if (r.scale() < 0) r = r.setScale(0);
        return r.toPlainString().replace('.', ',');
    }

    private static String formattaEuro(BigDecimal v) {
        return v == null ? "non disponibile" : "€ " + String.format(IT, "%,.2f", v);
    }

    private static String formattaPrezzo(BigDecimal p) {
        if (p.abs().compareTo(BigDecimal.ONE) >= 0) return "€ " + String.format(IT, "%,.2f", p);
        return "€ " + p.round(new java.math.MathContext(6, RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String formattaNumero(double v) {
        double a = Math.abs(v);
        if (a >= 1_000_000) return String.format(IT, "%.2fM", v / 1_000_000);
        if (a >= 10_000) return String.format(IT, "%.1fk", v / 1_000);
        if (a >= 100) return String.format(IT, "%.0f", v);
        if (a >= 1) return String.format(IT, "%.2f", v);
        if (a == 0) return "0";
        return String.format(IT, "%.4g", v);
    }

    private static String formattaEuroBreve(double v) {
        return "€ " + formattaNumero(v);
    }

    /** I colori del tema corrente, come in {@link RT_GraficoPlusvalenze}. */
    private static final class Colori {
        final boolean scuro = Principale.tema != null && Principale.tema.equalsIgnoreCase("Scuro");
        final Color sfondo = scuro ? new Color(60, 63, 65) : Color.WHITE;
        final Color testo = scuro ? new Color(210, 210, 210) : new Color(40, 40, 40);
        final Color secondario = scuro ? new Color(150, 150, 150) : new Color(120, 120, 120);
        final Color assi = scuro ? new Color(120, 120, 120) : new Color(150, 150, 150);
        final Color griglia = scuro ? new Color(80, 83, 85) : new Color(230, 230, 230);
        final Color qta = scuro ? new Color(120, 180, 255) : new Color(31, 119, 180);
        final Color valore = scuro ? new Color(255, 180, 120) : new Color(214, 120, 40);
        final Color entrata = scuro ? new Color(110, 200, 120) : new Color(30, 150, 60);
        final Color uscita = scuro ? new Color(240, 110, 110) : new Color(200, 40, 40);
        final Color cursore = scuro ? new Color(160, 160, 160) : new Color(140, 140, 140);
        final Color stato = scuro ? new Color(230, 200, 120) : new Color(150, 100, 0);
    }
}
