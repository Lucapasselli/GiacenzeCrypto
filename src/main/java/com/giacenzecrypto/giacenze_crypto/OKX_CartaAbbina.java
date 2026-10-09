package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collega i movimenti della carta OKX a quelli dell'exchange, dopo ogni importazione che può portarne di nuovi
 * (bill OKX via API o recupero degli scarti, wallet della carta su X Layer). Passata unica su tutto l'archivio,
 * rifatta ogni volta: l'ordine in cui l'utente importa le due fonti non conta.
 *
 * <ul>
 *   <li><b>Ricarica.</b> Un prelievo OKX del bill 325 ("Transfer to smart wallet") e un deposito della stessa moneta
 *       e quantità sul wallet della carta, da 2 minuti prima a 15 dopo il bill (le stesse tolleranze con cui
 *       {@link OKX_WalletCarta} trova il wallet), diventano un trasferimento fra wallet propri (PTW/DTW).</li>
 *   <li><b>Cashback.</b> Un deposito sul wallet della carta che arriva dallo stesso indirizzo di una ricarica già
 *       abbinata (il wallet caldo di OKX), senza un bill 325, è il cashback della carta. Lo si decide solo quando
 *       l'archivio contiene movimenti OKX oltre quell'istante: prima, il 325 potrebbe non essere ancora scaricato e
 *       la ricarica verrebbe presa per un cashback. Limite noto: un rimborso dalla stessa fonte sarebbe preso per
 *       un cashback (nessun rimborso visto finora).</li>
 * </ul>
 */
final class OKX_CartaAbbina {

    private OKX_CartaAbbina() {}

    /** Inizio della causale ({@code [7]}) dei bill 325 importati: l'etichetta di {@code OKX_Tipi.json}. */
    static final String CAUSALE_325 = "Transfer to smart wallet";
    /** Campo 18 del cashback, lo stesso della classificazione manuale ({@code GUI_ClassificazioneMovimento.Crea_Reward}). */
    static final String DETTAGLIO_CASHBACK = "DAI - Airdrop,Cashback,Rewards etc.. ";

    record Esito(int ricariche, int cashback) {}

    /**
     * Abbina sull'archivio in memoria, col wallet della carta salvato nelle opzioni. Se non è ancora stato
     * individuato lo cerca dai bill 325 dell'archivio: lo scaricamento via API lo fa solo sui bill che scarica, e un
     * 325 arrivato dal recupero degli scarti (o scaricato prima che il programma lo cercasse) non passa di lì.
     */
    static Esito Abbina() {
        try {
            if (DatabaseH2.connectionPersonale == null) return new Esito(0, 0);
            String w = DatabaseH2.Pers_Opzioni_Leggi(OKX_WalletCarta.OPZIONE_WALLET);
            if (w == null || w.isBlank()) {
                w = TrovaWalletDaArchivio(MappaCryptoWallet, OKX_WalletCarta.RpcXLayer());
                if (w == null) return new Esito(0, 0);
            }
            Esito e = Abbina(MappaCryptoWallet, w.trim());
            if (e.ricariche() + e.cashback() > 0) Principale.TabellaCryptodaAggiornare = true;
            return e;
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return new Esito(0, 0);
        }
    }

    /** I bill 325 dell'archivio non ancora abbinati, nella forma che {@link OKX_WalletCarta#Trova} si aspetta. */
    static List<OKX_WalletCarta.Trasferimento> TrasferimentiDaArchivio(Map<String, String[]> archivio) {
        List<OKX_WalletCarta.Trasferimento> lista = new ArrayList<>();
        for (String[] v : archivio.values()) {
            if (v == null || v.length <= 30 || !"OKX".equalsIgnoreCase(v[3].trim()) || !"PC".equals(Categoria(v))
                    || !v[18].isBlank() || !v[7].startsWith(CAUSALE_325)) continue;
            try {
                lista.add(new OKX_WalletCarta.Trasferimento(Istante(v), v[8].trim().toUpperCase(), new BigDecimal(v[10]).abs()));
            } catch (Exception e) {
                //quantita' non numerica: il bill non serve a riconoscere il wallet
            }
        }
        return lista;
    }

    /** Cerca il wallet della carta dai bill 325 dell'archivio; se lo trova lo salva e lo registra fra i wallet DeFi. */
    private static String TrovaWalletDaArchivio(Map<String, String[]> archivio, OKX_WalletCarta.Rpc rpc) {
        List<OKX_WalletCarta.Trasferimento> versoCarta = TrasferimentiDaArchivio(archivio);
        if (versoCarta.isEmpty()) return null;
        OKX_WalletCarta.Esito carta = OKX_WalletCarta.Trova(versoCarta, rpc);
        for (String nota : carta.note()) System.out.println("Wallet carta OKX: " + nota);
        if (carta.indirizzo() == null) return null;
        System.out.println("Wallet carta OKX su X Layer (dall'archivio): " + carta.indirizzo());
        DatabaseH2.Pers_Opzioni_Scrivi(OKX_WalletCarta.OPZIONE_WALLET, carta.indirizzo());
        OKX_WalletCarta.Registra(carta.indirizzo());
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            javax.swing.JOptionPane.showConfirmDialog(null, OKX_WalletCarta.TestoAvviso(carta.indirizzo()), "Carta OKX",
                    javax.swing.JOptionPane.DEFAULT_OPTION, javax.swing.JOptionPane.INFORMATION_MESSAGE, null);
        }
        return carta.indirizzo();
    }

    static Esito Abbina(Map<String, String[]> archivio, String walletCarta) {
        String nomeWallet = walletCarta + " (" + Trans_XLayer.RETE + ")";
        List<String[]> bill325 = new ArrayList<>();
        List<String[]> depositiCarta = new ArrayList<>();
        Set<String> mittentiRicariche = new HashSet<>();
        long ultimoOKX = Long.MIN_VALUE;
        for (String[] v : archivio.values()) {
            if (v == null || v.length <= 30) continue;
            boolean okx = "OKX".equalsIgnoreCase(v[3].trim());
            if (okx) ultimoOKX = Math.max(ultimoOKX, Istante(v));
            if (okx && "PC".equals(Categoria(v)) && v[18].isBlank() && v[7].startsWith(CAUSALE_325)) {
                bill325.add(v);
            } else if (v[3].trim().equalsIgnoreCase(nomeWallet) && "DC".equals(Categoria(v))) {
                if (v[18].isBlank() && !"AU".equals(v[22].trim())) depositiCarta.add(v);
                else if (v[18].startsWith("DTW") && !v[30].isBlank()) mittentiRicariche.add(v[30].trim().toLowerCase());
            }
        }

        int ricariche = 0;
        List<String[]> abbinati = new ArrayList<>();
        for (String[] p : bill325) {
            long t = Istante(p);
            String[] migliore = null;
            long scartoMigliore = Long.MAX_VALUE;
            for (String[] d : depositiCarta) {
                if (abbinati.contains(d) || !d[11].equalsIgnoreCase(p[8]) || !StessaQuantita(p[10], d[13])) continue;
                long scarto = Istante(d) - t;
                if (scarto < -OKX_WalletCarta.ANTICIPO_S * 1000 || scarto > OKX_WalletCarta.RITARDO_S * 1000) continue;
                if (Math.abs(scarto) < scartoMigliore) {
                    migliore = d;
                    scartoMigliore = Math.abs(scarto);
                }
            }
            if (migliore != null) {
                abbinati.add(migliore);
                if (!migliore[30].isBlank()) mittentiRicariche.add(migliore[30].trim().toLowerCase());
                GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(p[0], migliore[0]);
                ricariche++;
            }
        }

        int cashback = 0;
        for (String[] d : depositiCarta) {
            if (abbinati.contains(d) || d[30].isBlank() || !mittentiRicariche.contains(d[30].trim().toLowerCase())) continue;
            //Il 325 della ricarica arriva fino a 2 minuti dopo il deposito (orologi): oltre, l'archivio OKX lo avrebbe
            if (ultimoOKX < Istante(d) + OKX_WalletCarta.ANTICIPO_S * 1000) continue;
            d[5] = "CASHBACK";
            d[17] = "";
            d[18] = DETTAGLIO_CASHBACK;
            d[19] = "";
            d[20] = "";
            cashback++;
        }
        return new Esito(ricariche, cashback);
    }

    private static String Categoria(String[] v) {
        String[] parti = v[0].split("_");
        return parti[parti.length - 1].toUpperCase();
    }

    /** Istante del movimento dall'ID ({@code yyyyMMddHHmmss}), al secondo. */
    private static long Istante(String[] v) {
        return FunzioniDate.ConvertiDataIDinLong(v[0].split("_")[0]);
    }

    private static boolean StessaQuantita(String prelievo, String deposito) {
        try {
            return new BigDecimal(prelievo).abs().compareTo(new BigDecimal(deposito).abs()) == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
