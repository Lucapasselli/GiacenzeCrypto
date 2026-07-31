/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;

/**
 * Costruisce il testo della domanda da sottoporre a un chatbot esterno a partire da un singolo movimento
 * di {@link Principale#MappaCryptoWallet}.
 *
 * <p>Sono previsti due profili, che determinano quali dati escono dal computer dell'utente:</p>
 * <ul>
 *   <li>{@link Profilo#COMPLETO} - tutti i dati del movimento, compresi hash della transazione, indirizzi
 *       wallet e controparte, quantit&agrave; e controvalore. &Egrave; il profilo che permette al chatbot di
 *       leggere davvero la transazione sull'explorer, ma identifica in modo univoco il portafoglio.</li>
 *   <li>{@link Profilo#GENERICO} - causale, simboli delle monete, rete e nome della piattaforma (solo se
 *       non &egrave; un indirizzo, vedi {@link #PiattaformaAnonima}): nessun hash, nessun indirizzo, nessun
 *       importo, nessuna data. Serve a capire che cosa significa <i>quel tipo</i> di operazione senza
 *       esporre dati riconducibili all'utente.</li>
 * </ul>
 *
 * <p>La richiesta di inquadramento fiscale &egrave; opzionale: quando attiva, il prompt chiede al chatbot di
 * chiudere la risposta con una riga in formato fisso ({@code CATEGORIA CONSIGLIATA: ...}) scelta fra le
 * categorie fiscali generali (acquisto, vendita, permuta, trasferimento, reward, commissione). Sono
 * categorie di inquadramento, non le voci del combo di {@link GUI_ClassificazioneMovimento}, che classifica
 * i soli depositi e prelievi con un elenco pi&ugrave; specifico.</p>
 *
 * @author luca.passelli
 */
public class PromptIA {

    /** Profilo di riservatezza del prompt: quanti dati del movimento vengono inclusi nel testo. */
    public enum Profilo {
        /** Tutti i dati del movimento, hash e indirizzi compresi. */
        COMPLETO,
        /** Solo piattaforma, causale e simboli monete: nessun dato riconducibile all'utente. */
        GENERICO
    }

    /**
     * Costruisce il testo della domanda per un movimento.
     * @param ID identificativo del movimento in {@link Principale#MappaCryptoWallet}
     * @param profilo profilo di riservatezza da applicare
     * @param inquadramentoFiscale se {@code true} aggiunge la richiesta di inquadramento fiscale italiano
     * @return il testo pronto da inviare al chatbot, oppure {@code null} se il movimento non esiste
     */
    public static String Costruisci(String ID, Profilo profilo, boolean inquadramentoFiscale) {
        if (ID == null) return null;
        String[] v = Principale.MappaCryptoWallet.get(ID);
        if (v == null) return null;

        if (profilo == Profilo.GENERICO) return Generico(ID, v, inquadramentoFiscale);
        return Completo(ID, v, inquadramentoFiscale);
    }

    /**
     * Indica se per il movimento sono disponibili i dati che rendono utile il profilo completo su DeFi
     * (hash della transazione e rete). &Egrave; la stessa condizione con cui {@link GUI_DettaglioTransazione}
     * abilita il pulsante di apertura dell'explorer.
     * @param ID identificativo del movimento
     * @return {@code true} se il movimento ha sia hash sia rete
     */
    public static boolean isDefi(String ID) {
        String[] v = Principale.MappaCryptoWallet.get(ID);
        if (v == null) return false;
        return !Campo(v, 24).isEmpty() && Funzioni.TrovaReteDaID(ID) != null;
    }

    /**
     * Costruisce il prompt del profilo completo.
     * @param ID identificativo del movimento
     * @param v array dei dati del movimento
     * @param inquadramentoFiscale se {@code true} aggiunge la richiesta di inquadramento fiscale
     * @return il testo della domanda
     */
    private static String Completo(String ID, String[] v, boolean inquadramentoFiscale) {
        StringBuilder sb = new StringBuilder();

        String Rete = Funzioni.TrovaReteDaID(ID);
        String Hash = Campo(v, 24);
        String UrlExplorer = Funzioni_WalletDeFi.UrlExplorerTx(Rete, Hash);

        sb.append("Sei un esperto di transazioni on-chain e di fiscalità italiana delle criptoattività.\n");
        sb.append("Ti do i dati di UNA transazione dal mio software di rendicontazione:\n");
        sb.append(UrlExplorer != null
                ? "non so leggere gli explorer, spiegami cosa è successo.\n\n"
                : "aiutami a capire cosa rappresenta.\n\n");

        sb.append("DATI DELLA TRANSAZIONE\n");

        Riga(sb, "Data e ora", Campo(v, 1));
        Riga(sb, "Rete", Rete);
        //l'hash è già dentro il link: si ripete solo quando la rete non ha un explorer noto
        if (UrlExplorer == null) Riga(sb, "Hash", Hash);
        //sui movimenti DeFi piattaforma e wallet contengono lo stesso indirizzo: inutile ripeterlo
        if (!Campo(v, 3).equalsIgnoreCase(Campo(v, 4))) Riga(sb, "Piattaforma", Campo(v, 3));
        Riga(sb, "Wallet", Campo(v, 4));
        Riga(sb, "Controparte", Campo(v, 30));
        Riga(sb, "Uscita", Moneta(Campo(v, 10), Campo(v, 8), Campo(v, 25), Campo(v, 26)));
        Riga(sb, "Entrata", Moneta(Campo(v, 13), Campo(v, 11), Campo(v, 27), Campo(v, 28)));
        Riga(sb, "Valore stimato", ValoreEuro(Campo(v, 15)));
        Riga(sb, "Classificato dal programma", Tronca(Unisci(Campo(v, 5), Campo(v, 6)), 120));
        Riga(sb, "Causale originale", Tronca(Campo(v, 7), 80));
        Riga(sb, "Note mie", Tronca(Campo(v, 21), 100));
        Riga(sb, "Fonte dati", FonteDati(Campo(v, 39)));
        Riga(sb, "Explorer", UrlExplorer);

        sb.append("\nCOSA TI CHIEDO\n");
        int n = 1;
        //la menzione dell'explorer ha senso solo sui movimenti on-chain
        if (UrlExplorer != null) {
            sb.append(n++).append(". Cosa fa questa transazione, in italiano semplice, come se leggessi tu l'explorer.\n");
        } else {
            sb.append(n++).append(". Cosa rappresenta questa operazione, in italiano semplice.\n");
        }
        sb.append(n++).append(". Quale protocollo/dApp o contratto è coinvolto e che operazione è: swap, liquidità,\n");
        sb.append("   staking, claim, bridge, approve, mint NFT, invio/ricezione, airdrop.\n");
        sb.append(n++).append(". Se ci sono segnali di token spam/scam o airdrop non richiesto, e da cosa lo capisci.\n");
        sb.append(n++).append(". Se comporta commissioni di rete a mio carico e in quale token.\n");
        if (inquadramentoFiscale) {
            sb.append(n++).append(". In quale categoria fiscale italiana ricade: acquisto con fiat, vendita per fiat,\n");
            sb.append("   permuta tra criptoattività, trasferimento tra wallet miei, reward/staking/airdrop,\n");
            sb.append("   commissione, altro.\n");
            FontiNormative(sb);
            sb.append("   Chiudi con la riga: CATEGORIA CONSIGLIATA: <una categoria>\n");
        }
        sb.append(n++).append(". Quello che non è deducibile dai dati dichiaralo invece di ipotizzarlo, e dimmi\n");
        sb.append(UrlExplorer != null ? "   cosa dovrei cercare sull'explorer.\n" : "   cosa dovrei controllare.\n");

        sb.append("\nRispondi in italiano, sintetico. Non è consulenza fiscale: decido io.\n");

        return sb.toString();
    }

    /**
     * Costruisce il prompt del profilo generico, privo di dati riconducibili all'utente.
     * @param ID identificativo del movimento
     * @param v array dei dati del movimento
     * @param inquadramentoFiscale se {@code true} aggiunge la richiesta di inquadramento fiscale
     * @return il testo della domanda
     */
    private static String Generico(String ID, String[] v, boolean inquadramentoFiscale) {
        StringBuilder sb = new StringBuilder();

        sb.append("Sei un esperto di criptoattività e di fiscalità italiana.\n");
        sb.append("Nel mio software di rendicontazione ho un movimento di questo tipo e non sono\n");
        sb.append("sicuro di come interpretarlo. Ometto volutamente importi, indirizzi e hash.\n\n");

        String Rete = Funzioni.TrovaReteDaID(ID);
        Riga(sb, "Piattaforma", PiattaformaAnonima(Campo(v, 3), Rete));
        Riga(sb, "Causale esportata dalla piattaforma", Campo(v, 7));
        Riga(sb, "Classificato dal programma come", Campo(v, 5));
        Riga(sb, "Monete coinvolte", Monete(Campo(v, 8), Campo(v, 11)));
        Riga(sb, "Rete", Rete);

        sb.append("\nCOSA TI CHIEDO\n");
        int n = 1;
        sb.append(n++).append(". Che cosa significa questa causale su quella piattaforma: che operazione\n");
        sb.append("   rappresenta in concreto?\n");
        if (inquadramentoFiscale) {
            sb.append(n++).append(". Secondo la prassi fiscale italiana che tipo di evento è (acquisto, vendita,\n");
            sb.append("   permuta, trasferimento interno, reward, commissione, altro) e se di norma\n");
            sb.append("   genera una plusvalenza da dichiarare.\n");
            FontiNormative(sb);
        }
        sb.append(n++).append(". Cosa dovrei controllare per esserne sicuro.\n");

        sb.append("\nRispondi in italiano, sintetico. Non è consulenza fiscale.\n");

        return sb.toString();
    }

    /**
     * Aggiunge alla richiesta di inquadramento fiscale i riferimenti normativi su cui la risposta deve
     * fondarsi. Senza questo vincolo i chatbot rispondono sulla fiscalit&agrave; italiana delle criptoattivit&agrave;
     * in modo generico e spesso datato: ancorarli alle fonti e chiedere la citazione puntuale rende molto
     * pi&ugrave; visibile quando stanno improvvisando.
     * @param sb buffer del prompt in costruzione
     */
    private static void FontiNormative(StringBuilder sb) {
        sb.append("   Basati sulla Circolare 30/E del 27/10/2023 dell'Agenzia delle Entrate e sulle Leggi\n");
        sb.append("   di Bilancio 2023, 2025 e 2026, citando il punto preciso; se non sei certo di cosa\n");
        sb.append("   dicano, dillo invece di ipotizzare.\n");
    }

    /**
     * Aggiunge al prompt una riga {@code - etichetta: valore}, saltandola del tutto se il valore &egrave; vuoto:
     * le righe assenti sono preferibili a righe valorizzate a "vuoto", che porterebbero il chatbot a
     * ragionare su dati mancanti.
     * @param sb buffer del prompt in costruzione
     * @param Etichetta etichetta della riga
     * @param Valore valore della riga, eventualmente {@code null} o vuoto
     */
    private static void Riga(StringBuilder sb, String Etichetta, String Valore) {
        if (Valore == null || Valore.isBlank()) return;
        sb.append("- ").append(Etichetta).append(": ").append(Valore).append("\n");
    }

    /**
     * Legge in sicurezza un campo dell'array del movimento.
     * @param v array dei dati del movimento
     * @param i indice del campo
     * @return il valore del campo senza spazi iniziali/finali, oppure stringa vuota se assente
     */
    private static String Campo(String[] v, int i) {
        if (v == null || i < 0 || i >= v.length || v[i] == null) return "";
        return v[i].trim();
    }

    /**
     * Compone la descrizione di una moneta movimentata, con quantit&agrave;, nome esteso e contratto quando noti.
     * @param Qta quantit&agrave; movimentata
     * @param Simbolo simbolo della moneta
     * @param NomeToken nome esteso del token (campo DeFi), eventualmente vuoto
     * @param Address indirizzo del contratto (campo DeFi), eventualmente vuoto
     * @return la descrizione della moneta, o stringa vuota se non c'&egrave; nessuna moneta
     */
    private static String Moneta(String Qta, String Simbolo, String NomeToken, String Address) {
        if (Simbolo.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(Numero(Qta)).append(" ").append(Simbolo);
        if (!NomeToken.isBlank() || !Address.isBlank()) {
            sb.append(" (");
            if (!NomeToken.isBlank()) sb.append("token: ").append(NomeToken);
            if (!NomeToken.isBlank() && !Address.isBlank()) sb.append(" - ");
            if (!Address.isBlank()) sb.append("contratto: ").append(Address);
            sb.append(")");
        }
        return sb.toString().trim();
    }

    /**
     * Elenca i soli simboli delle monete coinvolte, usato dal profilo generico.
     * @param Uscita simbolo della moneta in uscita
     * @param Entrata simbolo della moneta in entrata
     * @return una descrizione testuale delle monete coinvolte, o stringa vuota se non ce ne sono
     */
    private static String Monete(String Uscita, String Entrata) {
        StringBuilder sb = new StringBuilder();
        if (!Uscita.isBlank()) sb.append("uscita ").append(Uscita);
        if (!Uscita.isBlank() && !Entrata.isBlank()) sb.append(", ");
        if (!Entrata.isBlank()) sb.append("entrata ").append(Entrata);
        return sb.toString();
    }

    /**
     * Filtra il nome della piattaforma per il profilo generico. Sui movimenti on-chain il campo
     * {@code [3]} non contiene il nome di un exchange ma l'<b>indirizzo del wallet</b> (spesso nella forma
     * {@code indirizzo (RETE)}, come conferma {@link Funzioni#Deprecato_RitornaReteDefi} che proprio da
     * l&igrave; ricava la rete): in un profilo che promette di non esporre dati riconducibili all'utente
     * il campo va quindi omesso.
     * @param Piattaforma valore del campo piattaforma/exchange
     * @param Rete rete del movimento, {@code null} per i movimenti che non sono on-chain
     * @return il nome della piattaforma se è sicuro esporlo, stringa vuota altrimenti
     */
    private static String PiattaformaAnonima(String Piattaforma, String Rete) {
        if (Piattaforma == null || Piattaforma.isBlank()) return "";
        if (Rete != null) return "";              //movimento on-chain: qui c'è il wallet, non un exchange
        if (sembraIndirizzo(Piattaforma)) return "";
        return Piattaforma;
    }

    /**
     * Riconosce in modo prudente i valori che sono (o contengono) un indirizzo di wallet.
     * @param Valore testo da valutare
     * @return {@code true} se il valore ha l'aspetto di un indirizzo o di un wallet {@code indirizzo (RETE)}
     */
    private static boolean sembraIndirizzo(String Valore) {
        String s = Valore.trim();
        if (s.startsWith("0x") && s.length() >= 20) return true;       //EVM
        if (s.contains("(") && s.contains(")")) return true;           //forma "indirizzo (RETE)"
        //indirizzi Bitcoin/Solana: stringa unica, lunga e senza spazi
        return !s.contains(" ") && s.length() >= 26;
    }

    /**
     * Tronca un testo libero, perché un campo note molto lungo gonfierebbe il prompt senza aggiungere
     * informazione utile alla comprensione della transazione.
     * @param Valore testo da troncare
     * @param Massimo lunghezza massima ammessa
     * @return il testo, eventualmente troncato e seguito da puntini di sospensione
     */
    private static String Tronca(String Valore, int Massimo) {
        if (Valore == null || Valore.length() <= Massimo) return Valore;
        return Valore.substring(0, Massimo).trim() + "...";
    }

    /**
     * Unisce due descrizioni nella forma {@code principale (secondaria)}, tollerando l'assenza di una delle due.
     * @param Principale testo principale
     * @param Secondaria testo tra parentesi
     * @return il testo unito, o stringa vuota se entrambi assenti
     */
    private static String Unisci(String Principale, String Secondaria) {
        if (Principale.isBlank()) return Secondaria;
        if (Secondaria.isBlank()) return Principale;
        return Principale + " (" + Secondaria + ")";
    }

    /**
     * Normalizza una quantit&agrave; togliendo gli zeri non significativi ed evitando la notazione scientifica,
     * che sui token con molti decimali renderebbe il dato illeggibile.
     * @param Valore quantit&agrave; come salvata nel movimento
     * @return la quantit&agrave; in notazione piana, o il valore originale se non numerico
     */
    private static String Numero(String Valore) {
        if (Valore == null || Valore.isBlank()) return "";
        try {
            return new BigDecimal(Valore).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e) {
            return Valore;
        }
    }

    /**
     * Formatta il controvalore in euro del movimento, omettendolo se nullo o non valorizzato.
     * @param Valore controvalore in euro come salvato nel movimento
     * @return il controvalore seguito da {@code EUR}, o stringa vuota
     */
    private static String ValoreEuro(String Valore) {
        String n = Numero(Valore);
        if (n.isBlank() || !Funzioni.isBigDecimalNonZero(Valore)) return "";
        return n + " EUR";
    }

    /**
     * Traduce in chiaro il codice della fonte dati del movimento (campo 39).
     * @param Codice codice della fonte dati
     * @return la descrizione della fonte, o stringa vuota se il codice non &egrave; riconosciuto
     */
    private static String FonteDati(String Codice) {
        if (Codice == null || Codice.isBlank()) return "";
        switch (Codice.trim().toUpperCase()) {
            case "A": return "API dell'exchange";
            case "B": return "API dell'explorer blockchain";
            case "C": return "file CSV dell'exchange";
            case "D": return "file CSV generico";
            case "E": return "inserimento manuale";
            default: return "";
        }
    }
}
