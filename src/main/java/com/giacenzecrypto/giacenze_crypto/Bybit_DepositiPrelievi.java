package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Arricchisce i depositi e i prelievi on-chain di Bybit già in archivio con i dati del file
 * "Withdraw &amp; Deposit History" ({@code assetHistory_withdrawDepositHistory_...csv}, colonne
 * {@code Uid,Date,Type,Asset,Chain,Amount,Tx ID,Status,Received Address}).
 *
 * <p>Il file non contiene nessun movimento nuovo: le stesse righe sono già negli "Asset Change Details"
 * del conto Spot ({@code userDeposit}/{@code userWithdrawal} nel 2022, Type vuoto per i prelievi del
 * 2023-2024) e del conto Funding ({@code Deposit}/{@code Withdrawal}), importati come depositi/prelievi da
 * classificare. Qui si aggiungono soltanto l'hash della transazione ({@code [24]}), l'indirizzo
 * ({@code [30]}) e la rete in nota ({@code [21]}). Nessuno dei tre entra in
 * {@code Calcoli_PlusvalenzeNew.Impronta()}: nessun ricalcolo, basta Salva.
 *
 * <p>La rete va in nota e <b>non</b> in {@code [34]}: quello è la rete del token, parte dell'identità
 * della moneta per prezzi e lotti, e un USDT di Bybit con {@code [34]=BSC} diventerebbe un'altra moneta.
 * I nomi di rete di Bybit sono anche incoerenti ({@code MATIC}/{@code Polygon PoS}), quindi restano testo.
 *
 * <p>Abbinamento: stesso exchange, stessa moneta, verso opposto a quello del movimento, causale grezza
 * ({@code [7]}) fra quelle dei depositi/prelievi on-chain, e <b>un solo</b> candidato, altrimenti la riga
 * resta non abbinata. La quantità è il vincolo che decide, il tempo solo una finestra: sui dati reali il
 * movimento più vicino nel tempo era a volte un giroconto fra conti di pari importo.
 * <ul>
 *   <li>deposito: quantità identica, accredito da poco prima a un'ora dopo la data del file (l'accredito
 *       arriva dopo le conferme della rete, fino a 27 minuti osservati);</li>
 *   <li>prelievo: entro due minuti; il file riporta l'importo netto, lo Spot il lordo (commissione di
 *       rete compresa), quindi il movimento può valere più della riga ma non meno.</li>
 * </ul>
 * La commissione di rete non si scorpora: la crea già la classificazione del trasferimento dalla
 * differenza fra prelievo e deposito, e farlo qui la conterebbe due volte.
 *
 * <p>Scrive solo campi vuoti: rilanciarlo sullo stesso file non cambia nulla.
 */
public class Bybit_DepositiPrelievi {

    static final String EXCHANGE = "Bybit";

    /**
     * Causali grezze ({@code [7]}) dei depositi/prelievi on-chain nelle config Bybit Spot e Funding. Le
     * altre causali mappate TRASFERIMENTO-CRYPTO (Launchpad, trading bot) non sono on-chain e non
     * devono ricevere l'hash di un prelievo di pari importo.
     */
    static final Set<String> CAUSALI = Set.of("userdeposit", "userwithdrawal", "", "deposit", "withdrawal", "withdraw");

    static final long DEPOSITO_PRIMA_MS = 2 * 60_000L;
    static final long DEPOSITO_DOPO_MS = 60 * 60_000L;
    static final long PRELIEVO_MS = 2 * 60_000L;

    static final String PREFISSO_NOTA = "Rete Bybit: ";

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Esito dell'arricchimento, per il resoconto mostrato all'utente. */
    public static class Esito {
        public int righeTotali = 0;
        /** Righe il cui movimento ha ricevuto almeno un dato nuovo */
        public int arricchiti = 0;
        /** Righe il cui movimento aveva già tutti i dati (file già letto) */
        public int giaArricchiti = 0;
        /** Depositi/prelievi non andati a buon fine: nessun movimento da cercare */
        public int nonCompletati = 0;
        public int ambigui = 0;
        public int nonTrovati = 0;
        /** Una riga per ogni riga non abbinata, per la diagnosi */
        public List<String> dettagli = new ArrayList<>();

        void Somma(Esito altro) {
            righeTotali += altro.righeTotali;
            arricchiti += altro.arricchiti;
            giaArricchiti += altro.giaArricchiti;
            nonCompletati += altro.nonCompletati;
            ambigui += altro.ambigui;
            nonTrovati += altro.nonTrovati;
            dettagli.addAll(altro.dettagli);
        }
    }

    /**
     * Arricchisce i movimenti con uno o più file "Withdraw &amp; Deposit History". Non ricalcola nulla.
     * @param files i CSV scaricati da Bybit, uno per anno
     * @return i conteggi sommati su tutti i file
     * @throws IOException se un file non è leggibile
     */
    public static Esito Arricchisci(File[] files) throws IOException {
        Esito totale = new Esito();
        Set<String> giaUsati = new HashSet<>();
        for (File f : files) {
            totale.Somma(Arricchisci(leggiRighe(f), giaUsati));
        }
        return totale;
    }

    /**
     * @param righe righe del file, già senza intestazione
     * @param giaUsati ID dei movimenti già abbinati a un'altra riga, aggiornato
     * @return i conteggi
     */
    static Esito Arricchisci(List<String[]> righe, Set<String> giaUsati) {
        Esito esito = new Esito();
        for (String[] c : righe) {
            if (c.length < 9) continue;
            esito.righeTotali++;
            String data = c[1].trim();
            String tipo = c[2].trim();
            String moneta = c[3].trim();
            String rete = c[4].trim();
            String hash = c[6].trim();
            String stato = c[7].trim();
            String indirizzo = c[8].trim();
            String descrizione = data + " " + tipo + " " + c[5].trim() + " " + moneta + " (" + rete + ")";

            if (!isCompletato(stato)) {
                esito.nonCompletati++;
                continue;
            }
            boolean deposito = tipo.equalsIgnoreCase("Deposit");
            if (!deposito && !tipo.equalsIgnoreCase("Withdraw") && !tipo.equalsIgnoreCase("Withdrawal")) {
                esito.nonTrovati++;
                esito.dettagli.add(descrizione + ": tipo non riconosciuto");
                continue;
            }
            BigDecimal quantita;
            long istante;
            try {
                quantita = new BigDecimal(c[5].trim()).abs();
                istante = LocalDateTime.parse(data, FORMATO_DATA).toEpochSecond(ZoneOffset.UTC) * 1000L;
            } catch (Exception ex) {
                esito.nonTrovati++;
                esito.dettagli.add(descrizione + ": data o importo non interpretabili");
                continue;
            }

            List<String> candidati = Candidati(moneta, quantita, istante, deposito, giaUsati);
            if (candidati.size() > 1) {
                esito.ambigui++;
                esito.dettagli.add(descrizione + ": più movimenti compatibili, lasciato com'è");
                continue;
            }
            if (candidati.isEmpty()) {
                esito.nonTrovati++;
                esito.dettagli.add(descrizione + ": movimento non trovato (importare prima gli Asset Change Details di Spot e Funding)");
                continue;
            }
            String id = candidati.get(0);
            giaUsati.add(id);
            if (ScriviDati(MappaCryptoWallet.get(id), hash, indirizzo, rete)) {
                esito.arricchiti++;
            } else {
                esito.giaArricchiti++;
            }
        }
        return esito;
    }

    /** Bybit scrive "Completed" sui depositi e "Transferred successfully" sui prelievi. */
    static boolean isCompletato(String stato) {
        String s = stato.toLowerCase();
        return s.equals("completed") || s.equals("success") || s.startsWith("transferred success");
    }

    /**
     * I movimenti compatibili con una riga del file.
     * @param istante data della riga, UTC, in millisecondi
     * @return gli ID, vuoto se nessuno
     */
    static List<String> Candidati(String moneta, BigDecimal quantita, long istante, boolean deposito, Set<String> giaUsati) {
        List<String> risultato = new ArrayList<>();
        for (String[] v : MappaCryptoWallet.values()) {
            if (v == null || v.length < Importazioni.ColonneTabella || giaUsati.contains(v[0])) continue;
            if (!EXCHANGE.equalsIgnoreCase(v[3].trim())) continue;
            if ("AU".equalsIgnoreCase(v[22])) continue;
            String[] parti = v[0].split("_");
            if (parti.length < 5 || parti[0].length() < 14) continue;
            if (!parti[4].equalsIgnoreCase(deposito ? "DC" : "PC")) continue;
            if (!CAUSALI.contains(v[7].trim().toLowerCase())) continue;
            String monetaMov = deposito ? v[11] : v[8];
            if (!moneta.equalsIgnoreCase(monetaMov.trim())) continue;
            BigDecimal qtaMov;
            try {
                qtaMov = new BigDecimal(deposito ? v[13] : v[10]).abs();
            } catch (Exception ex) {
                continue;
            }
            //Il prefisso dell'ID porta i secondi, [1] no
            long scarto = FunzioniDate.ConvertiDataIDinLong(parti[0].substring(0, 14)) - istante;
            if (deposito) {
                if (qtaMov.compareTo(quantita) != 0) continue;
                if (scarto < -DEPOSITO_PRIMA_MS || scarto > DEPOSITO_DOPO_MS) continue;
            } else {
                //Lordo almeno pari al netto, e la commissione non più grande dell'importo netto
                if (qtaMov.compareTo(quantita) < 0 || qtaMov.subtract(quantita).compareTo(quantita) >= 0) continue;
                if (Math.abs(scarto) > PRELIEVO_MS) continue;
            }
            risultato.add(v[0]);
        }
        return risultato;
    }

    /**
     * Scrive hash, indirizzo e rete nei campi vuoti del movimento.
     * @return {@code true} se è cambiato qualcosa
     */
    static boolean ScriviDati(String[] v, String hash, String indirizzo, String rete) {
        boolean cambiato = false;
        //Scrivi_Movimenti_Crypto cancella i ";", che nel file separano i campi
        hash = hash.replace(";", "");
        indirizzo = indirizzo.replace(";", "");
        rete = rete.replace(";", ",");
        if (!hash.isEmpty() && Funzioni.noData(v[24])) {
            v[24] = hash;
            cambiato = true;
        }
        if (!indirizzo.isEmpty() && Funzioni.noData(v[30])) {
            v[30] = indirizzo;
            cambiato = true;
        }
        String nota = v[21] == null ? "" : v[21];
        if (!rete.isEmpty() && !nota.contains(PREFISSO_NOTA)) {
            v[21] = nota.isBlank() ? PREFISSO_NOTA + rete : nota + "<br>" + PREFISSO_NOTA + rete;
            cambiato = true;
        }
        return cambiato;
    }

    /** Legge il CSV, saltando la riga "UID: ..." e l'intestazione. */
    static List<String[]> leggiRighe(File file) throws IOException {
        List<String[]> risultato = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String riga;
            while ((riga = br.readLine()) != null) {
                riga = riga.replace("﻿", "").trim();
                if (riga.isEmpty()) continue;
                String minuscola = riga.toLowerCase();
                if (minuscola.startsWith("uid:") || minuscola.startsWith("uid,date")) continue;
                risultato.add(riga.split(",", -1));
            }
        }
        return risultato;
    }
}
