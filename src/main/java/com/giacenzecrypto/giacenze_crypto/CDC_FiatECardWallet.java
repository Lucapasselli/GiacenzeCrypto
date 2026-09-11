/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author luca
 */
public class CDC_FiatECardWallet {
    
    /**
 * Calcola il saldo iniziale, il saldo finale e la giacenza media su un periodo specificato.
 *
 * <p>Itera sulla lista dei saldi (e facoltativamente quella con i picchi) per
 * determinare il saldo vigente a ogni data nel periodo, poi calcola la giacenza
 * media ponderata per il numero di giorni.</p>
 *
 * <p>Struttura dell'array restituito:</p>
 * <ul>
 *   <li>[0] - Saldo iniziale del periodo (include {@code saldoInizioPeriodo})</li>
 *   <li>[1] - Saldo finale del periodo (include {@code saldoInizioPeriodo})</li>
 *   <li>[2] - Giacenza media ponderata (include {@code saldoInizioPeriodo})</li>
 *   <li>[3] - Numero totale di giorni del periodo (come stringa)</li>
 * </ul>
 *
 * @param listaSaldi         Mappa con chiave 0 = lista saldi standard,
 *                           chiave 1 = lista saldi con picchi.
 *                           Ogni elemento è una stringa "data,valore".
 * @param dataInizialeS      Data di inizio del periodo nel formato atteso da
 *                           {@code FunzioniDate.convertiDataInLong}.
 * @param dataFinaleS        Data di fine del periodo nello stesso formato.
 * @param saldoInizioPeriodo Saldo di partenza da sommare al risultato finale
 *                           (offset fisso, es. saldo contabile iniziale).
 * @param mediaConPicchi     Se {@code true}, usa la lista dei picchi (chiave 1)
 *                           per il calcolo della giacenza media; altrimenti usa
 *                           la lista standard (chiave 0).
 * @return Array di 4 stringhe: saldo iniziale, saldo finale, giacenza media,
 *         numero di giorni.
 */
public static String[] calcolaSaldiEMedia(
        Map<Integer, List<String>> listaSaldi,
        String dataInizialeS,
        String dataFinaleS,
        String saldoInizioPeriodo,
        boolean mediaConPicchi) {

    // Risultato: [saldoIniziale, saldoFinale, giacenzaMedia, giorniTotali]
    String[] ritorno = new String[4];

    // Conversione delle date di confine in long per i confronti
    long longDataIniziale = FunzioniDate.ConvertiDatainLong(dataInizialeS);
    long longDataFinale   = FunzioniDate.ConvertiDatainLong(dataFinaleS);

    // Sceglie la lista da usare in base al flag mediaConPicchi
    List<String> listaCalcolo = mediaConPicchi
            ? listaSaldi.get(1)
            : listaSaldi.get(0);

    // -----------------------------------------------------------------------
    // Passata 1: calcolo saldo iniziale visuale (dalla lista standard, chiave 0)
    // Serve a determinare quale saldo era vigente *prima* del periodo, per
    // mostrarlo correttamente come "saldo iniziale" anche se nessuna voce
    // cade esattamente sulla data di inizio.
    // -----------------------------------------------------------------------
    BigDecimal ultimoValore    = BigDecimal.ZERO;
    String     saldoInizialeT  = "0";
    boolean    trovatoIniziale = false;

    for (String riga : listaSaldi.get(0)) {
        String[] parti = riga.split(",");
        if (!parti[0].isBlank()) {
            long longDataRiga = FunzioniDate.ConvertiDatainLong(parti[0]);
            BigDecimal valoreRiga = new BigDecimal(parti[1]);

            if (longDataIniziale > longDataRiga) {
                // La riga è *prima* del periodo: aggiorna l'ultimo valore noto
                ultimoValore = valoreRiga;
                saldoInizialeT = parti[1];
            } else if (longDataIniziale <= longDataRiga && longDataRiga <= longDataFinale) {
                // Prima voce *dentro* il periodo: cristallizza il saldo iniziale
                if (!trovatoIniziale) {
                    saldoInizialeT = ultimoValore.toString();
                    trovatoIniziale = true;
                }
                // ✅ Aggiorna ultimoValore anche DENTRO il periodo
                //  così alla fine del loop conterrà l'ultimo saldo vigente
                ultimoValore = valoreRiga;
            }
        }
    }

    // L'ultimo valore noto dalla lista standard è anche il saldo finale
    String saldoFinaleT = ultimoValore.toString();

    // -----------------------------------------------------------------------
    // Passata 2: calcolo giacenza media ponderata per giorni
    // Usa listaCalcolo (standard o con picchi secondo mediaConPicchi).
    // -----------------------------------------------------------------------
    String     dataInizialeCorrente = dataInizialeS; // avanza a ogni voce elaborata
    BigDecimal sommaGiacenza        = BigDecimal.ZERO;
    BigDecimal ultimoValoreCalcolo  = BigDecimal.ZERO;
    int        giorniTotali         = 0;

    for (String riga : listaCalcolo) {
        String[]   parti        = riga.split(",");
        if (!parti[0].isBlank()) {
        long       longDataRiga = FunzioniDate.ConvertiDatainLong(parti[0]);
        BigDecimal valoreRiga   = new BigDecimal(parti[1]);

        if (longDataIniziale > longDataRiga) {
            // Riga precedente al periodo: memorizza come ultimo valore noto
            ultimoValoreCalcolo = valoreRiga;

        } else if (longDataIniziale <= longDataRiga && longDataRiga <= longDataFinale) {
            // Riga nel periodo: calcola contributo ponderato
            long diffGiorni = FunzioniDate.DifferenzaDate(dataInizialeCorrente, parti[0]);
            giorniTotali  += (int) diffGiorni;
            sommaGiacenza  = ultimoValoreCalcolo
                    .multiply(new BigDecimal(diffGiorni))
                    .add(sommaGiacenza);

            // Avanza il cursore alla data corrente
            dataInizialeCorrente = parti[0];
            ultimoValoreCalcolo  = valoreRiga;
        }
        }
    }

    // Aggiunge i giorni rimanenti fino alla data finale (inclusiva)
    long diffFinale = FunzioniDate.DifferenzaDate(dataInizialeCorrente, dataFinaleS) + 1;
    giorniTotali += (int) diffFinale;
    sommaGiacenza = ultimoValoreCalcolo
            .multiply(new BigDecimal(diffFinale))
            .add(sommaGiacenza);

    // -----------------------------------------------------------------------
    // Calcolo giacenza media = somma ponderata / giorni totali + saldo offset
    // -----------------------------------------------------------------------
    BigDecimal offset        = new BigDecimal(saldoInizioPeriodo);
    BigDecimal giacenzaMedia = sommaGiacenza
            .divide(new BigDecimal(giorniTotali), 2, RoundingMode.HALF_UP)
            .add(offset);

    // Aggiunge l'offset anche ai saldi di inizio e fine
    saldoInizialeT = new BigDecimal(saldoInizialeT).add(offset).toString();
    saldoFinaleT   = new BigDecimal(saldoFinaleT).add(offset).toString();

    // Popola l'array di ritorno
    ritorno[0] = saldoInizialeT;
    ritorno[1] = saldoFinaleT;
    ritorno[2] = giacenzaMedia.toString();
    ritorno[3] = String.valueOf(giorniTotali); // sostituisce this.CDC_Text_Giorni.setText(...)

    return ritorno;
}
    
    
    

    // =======================================================================
    // Fiat Wallet Crypto.com : lettura del CSV e regole di riconoscimento
    // =======================================================================
    //
    // Queste regole hanno DUE consumatori: il tab "Crypto.com / Fiat Wallet"
    // (Principale.CDC_FiatWallet_Funzione_ImportaWallet + _CalcolaListaSaldi, che ci aggiunge sopra
    // la sola segnalazione degli errori a video) e la parte FIAT del quadro W/RW
    // (Calcoli_RW_Fiat, che per "Crypto.com App" legge di qui invece che dai movimenti crypto).
    // Stanno qui, in un posto solo, perche' il numero che finisce in RW e' proprio quello che
    // l'utente verifica sul tab: due implementazioni potrebbero divergere in silenzio.

    /**
     * Nome exchange (campo {@code [3]} del movimento) scritto dall'importatore Crypto.com App:
     * vedi i letterali in {@code Importazioni}. E' il discriminante con cui {@code Calcoli_RW_Fiat}
     * esclude le gambe FIAT che vanno invece prese dal Fiat Wallet — <b>non</b> il gruppo wallet,
     * perche' l'utente puo' aver raggruppato Crypto.com App insieme ad altri exchange e in quel caso
     * escludere per gruppo cancellerebbe anche le gambe FIAT degli altri.
     */
    public static final String NOME_EXCHANGE_CDC_APP = "Crypto.com App";

    /** Numero esatto di campi di una riga valida del CSV del Fiat Wallet (il file ha la virgola finale). */
    public static final int CAMPI_RIGA_FIATWALLET = 10;

    /** Valore reso da {@link #ColonnaEuro} quando la riga non ha nessuna colonna in EUR. */
    public static final int COLONNA_EURO_ASSENTE = 999;

    /** Tipi movimento transitori degli ordini limite: non sono movimenti, non vanno importati. */
    private static final Set<String> TIPI_FIATWALLET_TRANSITORI = Set.of(
            "trading.limit_order.fiat_wallet.purchase_unlock",
            "trading.limit_order.fiat_wallet.purchase_lock");

    /**
     * L'indice della colonna che porta l'importo in euro della riga: {@code 3}, {@code 5} o {@code 7}
     * secondo quale delle tre colonne valuta ({@code 2}, {@code 4}, {@code 6}) dice {@code EUR}.
     *
     * @return l'indice della colonna, oppure {@link #COLONNA_EURO_ASSENTE} se la riga non e' in euro
     *         — nel qual caso il movimento <b>non va contabilizzato</b> (ne' sul tab ne' in RW)
     */
    public static int ColonnaEuro(String riga) {
        if (riga == null) {
            return COLONNA_EURO_ASSENTE;
        }
        String[] splittata = riga.split(",");
        if (splittata.length != CAMPI_RIGA_FIATWALLET && splittata.length != CAMPI_RIGA_FIATWALLET - 1) {
            return COLONNA_EURO_ASSENTE;
        }
        if (splittata[2].trim().equalsIgnoreCase("EUR")) {
            return 3;
        }
        if (splittata[4].trim().equalsIgnoreCase("EUR")) {
            return 5;
        }
        if (splittata[6].trim().equalsIgnoreCase("EUR")) {
            return 7;
        }
        return COLONNA_EURO_ASSENTE;
    }

    /**
     * {@code true} se la riga del CSV va importata: esattamente {@link #CAMPI_RIGA_FIATWALLET} campi,
     * data valida in {@code [0]} e tipo movimento non transitorio.
     */
    public static boolean RigaFiatWalletDaImportare(String[] splittata) {
        return splittata != null
                && splittata.length == CAMPI_RIGA_FIATWALLET
                && !TIPI_FIATWALLET_TRANSITORI.contains(splittata[9])
                && dataValida(splittata[0]);
    }

    /**
     * Lo stesso controllo di data che l'importatore faceva con
     * {@code Principale.Funzioni_Date_ConvertiDatainLong}: {@code SimpleDateFormat("yyyy-MM-dd")},
     * lenient e sul fuso di default, quindi legge il prefisso di {@code "yyyy-MM-dd HH:mm:ss"} e
     * ignora il resto. E' ricopiato qui, e non richiamato di la', perche' questa classe la usa anche
     * il motore RW : dipendere da {@code Principale} vorrebbe dire caricare la finestra principale
     * (e con essa mezza GUI) per parsare una data.
     */
    private static boolean dataValida(String data) {
        if (data == null) {
            return false;
        }
        try {
            return SDF_FIATWALLET.get().parse(data).getTime() != 0;
        } catch (ParseException ex) {
            return false;
        }
    }

    private static final ThreadLocal<SimpleDateFormat> SDF_FIATWALLET =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    /**
     * Ricarica {@code VarCondivise.CDC_FiatWallet_MappaTipiMovimenti}: prima i tipi di default, poi
     * le eventuali personalizzazioni salvate su file, che li sovrascrivono.
     */
    public static void CaricaTipiMovimento() {
        Map<String, String> tipi = VarCondivise.CDC_FiatWallet_MappaTipiMovimenti;
        tipi.clear();
        tipi.put("crypto_viban", "crypto_viban;+;default;Vendita Crypto");
        tipi.put("viban_card_top_up", "viban_card_top_up;-;default;TopUp Carta");
        tipi.put("viban_deposit", "viban_deposit;+;default;Bonifico in Ingresso");
        tipi.put("viban_purchase", "viban_purchase;-;default;Acquisto Crypto");
        tipi.put("recurring_buy_order", "recurring_buy_order;-;default;Acquisto Crypto");
        tipi.put("viban_withdrawal", "viban_withdrawal;-;default;Bonifico su Conto Corrente");
        tipi.put("trading.limit_order.fiat_wallet.purchase_commit",
                "trading.limit_order.fiat_wallet.purchase_commit;-;default;Acquisto Crypto");
        tipi.put("trading.limit_order.fiat_wallet.sell_commit",
                "trading.limit_order.fiat_wallet.sell_commit;+;default;Vendita Crypto");
        try {
            File movPers = new File(VarStatiche.getFile_CDCFiatWallet_FileTipiMovimentiPers());
            if (!movPers.exists()) {
                movPers.createNewFile();
            }
            try (FileReader fires = new FileReader(movPers);
                    BufferedReader bures = new BufferedReader(fires)) {
                String riga;
                while ((riga = bures.readLine()) != null) {
                    String[] splittata = riga.split(";");
                    if (splittata.length == 4) {
                        tipi.put(splittata[0], riga);
                    }
                }
            }
        } catch (IOException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }

    /**
     * Il segno con cui il tipo movimento incide sul saldo del Fiat Wallet.
     *
     * <p>L'importatore toglie il segno dalla colonna {@code [3]} (che deve essere sempre positiva),
     * quindi il segno viene <b>solo</b> di qui: un tipo che non sta nella mappa e' un movimento
     * sconosciuto e va scartato-e-segnalato, mai firmato a caso.</p>
     *
     * <p>Il segno viene restituito <b>trimmato</b>. Fino al 2026-09-11 il confronto era
     * {@code tempo.split(";")[1].equalsIgnoreCase("+")} senza trim, mentre la chiave era gia'
     * trimmata : una riga personalizzata scritta {@code tipo; +;default;Desc} in
     * {@code crypto.com.fiatwallet.tipimovimentiPers.db} veniva letta come segno meno. Era una
     * svista, non una regola, ma la correzione sposta anche il saldo mostrato dal tab, non solo
     * quello di RW.</p>
     *
     * @return {@code "+"} o {@code "-"}, oppure {@code null} se il tipo non e' riconosciuto
     */
    public static String SegnoTipoMovimento(String tipo) {
        if (tipo == null) {
            return null;
        }
        if (VarCondivise.CDC_FiatWallet_MappaTipiMovimenti.isEmpty()) {
            CaricaTipiMovimento(); // il tab non e' ancora stato aperto (o siamo fuori dalla GUI)
        }
        for (String tempo : VarCondivise.CDC_FiatWallet_MappaTipiMovimenti.values()) {
            String[] parti = tempo.split(";");
            if (parti.length >= 2 && tipo.trim().equalsIgnoreCase(parti[0].trim())) {
                return parti[1].trim();
            }
        }
        return null;
    }

    /**
     * Legge il CSV del Fiat Wallet e ne rende le righe da importare, gia' normalizzate come le vuole
     * {@code CDC_FiatWallet_Mappa}: chiave {@code data + descrizione + tipo (+ importo in euro)},
     * valore la riga riunita con la virgola finale e con la colonna {@code [3]} resa positiva.
     *
     * <p>La mappa e' ordinata per chiave e la chiave comincia con la data in formato
     * {@code yyyy-MM-dd HH:mm:ss}: l'ordine di iterazione e' quindi gia' cronologico.</p>
     *
     * @param percorsoFile percorso del CSV; un file assente rende una mappa vuota, non un errore
     */
    public static Map<String, String> LeggiRigheFiatWallet(String percorsoFile) {
        Map<String, String> righe = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        File f = new File(percorsoFile);
        if (!f.exists()) {
            return righe;
        }
        try (FileReader fire = new FileReader(f);
                BufferedReader bure = new BufferedReader(fire)) {
            String riga;
            while ((riga = bure.readLine()) != null) {
                String[] splittata = riga.split(",");
                if (!RigaFiatWalletDaImportare(splittata)) {
                    continue;
                }
                int colonna = ColonnaEuro(riga);
                String idRiga = colonna == COLONNA_EURO_ASSENTE
                        ? splittata[0] + splittata[1] + splittata[9]
                        : splittata[0] + splittata[1] + splittata[9] + splittata[colonna];
                //la colonna 3 deve essere sempre positiva
                splittata[3] = splittata[3].replace("-", "");
                StringBuilder rigasistemata = new StringBuilder();
                for (String composta : splittata) {
                    rigasistemata.append(composta).append(",");
                }
                righe.put(idRiga, rigasistemata.toString());
            }
        } catch (IOException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return righe;
    }

    /** Un movimento in euro del Fiat Wallet Crypto.com, con l'importo gia' firmato. */
    public static final class MovimentoFiatWallet {

        /** Istante del movimento, {@code yyyy-MM-dd HH:mm:ss} come sta nel CSV. */
        public final String istante;
        /** Giornata del movimento, {@code yyyy-MM-dd}. */
        public final String giorno;
        /** Tipo movimento ({@code [9]} del CSV), gia' riconosciuto. */
        public final String tipo;
        /** Importo in euro, <b>firmato</b>: positivo = apporto, negativo = uscita. */
        public final BigDecimal importoEUR;

        MovimentoFiatWallet(String istante, String giorno, String tipo, BigDecimal importoEUR) {
            this.istante = istante;
            this.giorno = giorno;
            this.tipo = tipo;
            this.importoEUR = importoEUR;
        }
    }

    /**
     * Esito della lettura dei movimenti in euro del Fiat Wallet: i movimenti contabilizzati e il
     * conto di quelli scartati, distinti per motivo — gli stessi due che il tab segnala col pulsante
     * "errori", e per la stessa ragione: un movimento non contabilizzato sposta il saldo, quindi chi
     * usa questi dati deve poterlo dire.
     */
    public static final class EsitoFiatWallet {

        /** I movimenti in euro riconosciuti, in ordine cronologico. */
        public final List<MovimentoFiatWallet> movimenti;
        /** Righe scartate perche' non hanno nessuna colonna in euro. */
        public final int scartatiNonInEuro;
        /** Righe scartate perche' il tipo movimento non e' riconosciuto. */
        public final int scartatiTipoSconosciuto;

        EsitoFiatWallet(List<MovimentoFiatWallet> movimenti, int scartatiNonInEuro, int scartatiTipoSconosciuto) {
            this.movimenti = movimenti;
            this.scartatiNonInEuro = scartatiNonInEuro;
            this.scartatiTipoSconosciuto = scartatiTipoSconosciuto;
        }
    }

    /**
     * I movimenti in euro del Fiat Wallet Crypto.com letti dal CSV, con le stesse regole con cui li
     * contabilizza il tab: riga da importare ({@link #RigaFiatWalletDaImportare}), colonna euro
     * presente ({@link #ColonnaEuro}) e tipo movimento riconosciuto ({@link #SegnoTipoMovimento}).
     * Le righe che non superano gli ultimi due controlli sono <b>scartate</b>, non valorizzate a
     * zero, e vengono contate nell'esito.
     *
     * @param percorsoFile percorso del CSV del Fiat Wallet
     */
    public static EsitoFiatWallet MovimentiEuro(String percorsoFile) {
        List<MovimentoFiatWallet> movimenti = new ArrayList<>();
        int nonInEuro = 0;
        int tipoSconosciuto = 0;
        for (String value : LeggiRigheFiatWallet(percorsoFile).values()) {
            String[] splittata = value.split(",");
            if (splittata.length < CAMPI_RIGA_FIATWALLET) {
                continue;
            }
            int colonna = ColonnaEuro(value);
            if (colonna == COLONNA_EURO_ASSENTE) {
                nonInEuro++;
                continue;
            }
            String segno = SegnoTipoMovimento(splittata[9]);
            if (segno == null) {
                tipoSconosciuto++;
                continue;
            }
            BigDecimal importo;
            try {
                importo = new BigDecimal(splittata[colonna].trim()).abs();
            } catch (NumberFormatException ex) {
                nonInEuro++; // importo illeggibile: come una riga senza colonna in euro, non si contabilizza
                continue;
            }
            if ("-".equals(segno)) {
                importo = importo.negate();
            }
            String istante = splittata[0].trim();
            movimenti.add(new MovimentoFiatWallet(istante, istante.substring(0, 10), splittata[9].trim(), importo));
        }
        return new EsitoFiatWallet(movimenti, nonInEuro, tipoSconosciuto);
    }

}
