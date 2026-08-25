package com.giacenzecrypto.giacenze_crypto;

/**
 * Criteri di filtro della tabella dei movimenti crypto, e la decisione se un movimento li superi.
 *
 * <p>Sono i filtri <b>di caricamento</b>: quelli che decidono quali righe la tabella contiene, e che
 * quindi richiedono di ricostruirla quando cambiano. Non vanno confusi con i filtri <b>di riga</b>
 * ({@code Tabelle.tableFilters} e il campo di ricerca), che agiscono su un modello già costruito e sono
 * ricomposti a ogni caricamento da {@code Tabelle_FiltroColonne}. La distinzione è raccontata per esteso in
 * {@code nocommit/Documentazione/Analisi_Filtri_Movimenti.md}.
 *
 * <p>Fino al 2026-08-13 questi sette criteri erano un {@code if} a sette condizioni dentro
 * {@code Principale.TransazioniCrypto_Funzioni_CaricaTabellaCryptoDaMappa}, letti direttamente dalle
 * caselle e dalle combo della barra. Erano perciò <b>non verificabili senza schermo</b>, pur decidendo
 * cosa l'utente vede. Qui sono un record e una funzione pura, e la GUI si limita a comporre il record.
 *
 * <p><b>{@link FiltriMovimenti#Passa} non legge nulla per conto proprio</b>: data convertita, gruppo
 * wallet, presenza del prezzo e LiFo mancante arrivano dal chiamante perché il ciclo di caricamento li
 * calcola già per altri motivi (conteggio errori, totali), e ricalcolarli qui significherebbe una query
 * su {@code personale.mv.db} e una scansione dei prezzi per ogni movimento.
 */
public class Principale_FiltriMovimenti {

    /** Voce delle combo che vale "nessun filtro". */
    public static final String TUTTI = "Tutti";

    /** Filtro documento: nessun filtro. */
    public static final String DOC_TUTTI = "";
    /** Filtro documento: solo i movimenti che hanno un documento di origine, quale che sia. */
    public static final String DOC_CON = "CON";
    /** Filtro documento: solo i movimenti che <b>non</b> hanno un documento di origine. */
    public static final String DOC_SENZA = "SENZA";

    /**
     * I criteri attivi sulla tabella dei movimenti.
     *
     * @param Wallet voce scelta nella combo dei wallet: {@link #TUTTI}, un nome di wallet, oppure la
     *               forma {@code "Wallet : Gruppo (n)"} che seleziona l'intero gruppo
     * @param Token simbolo della moneta, oppure {@link #TUTTI}; combacia sia in entrata sia in uscita
     * @param Documento {@link #DOC_TUTTI}, {@link #DOC_CON}, {@link #DOC_SENZA} oppure l'id di un
     *                  documento di origine ({@code "3"}), confrontato con il campo {@code [41]}
     * @param DataInizio estremo inferiore, nella forma numerica di {@code Funzioni_Date_ConvertiDatainLong}
     * @param DataFine estremo superiore, stessa forma
     * @param NascondiTrasferimentiInterni toglie i movimenti di tipo "Trasferimento Interno"
     * @param NascondiTokenScam toglie i movimenti di soli token marcati SCAM
     * @param SoloSenzaPrezzo tiene i soli movimenti non valorizzati
     * @param SoloLifoMancante tiene i soli movimenti con giacenza insufficiente nel LiFo
     */
    public record FiltriMovimenti(
            String Wallet,
            String Token,
            String Documento,
            long DataInizio,
            long DataFine,
            boolean NascondiTrasferimentiInterni,
            boolean NascondiTokenScam,
            boolean SoloSenzaPrezzo,
            boolean SoloLifoMancante) {

        /** Costruttore compatto: normalizza i {@code null} così che {@link #Passa} non debba difendersene. */
        public FiltriMovimenti {
            if (Wallet == null || Wallet.isBlank()) Wallet = TUTTI;
            if (Token == null || Token.isBlank()) Token = TUTTI;
            if (Documento == null) Documento = DOC_TUTTI;
        }

        /**
         * Decide se un movimento debba comparire in tabella.
         *
         * @param v il movimento, nel formato a {@code Importazioni.ColonneTabella} campi
         * @param dataMovimento data del movimento, già convertita dal chiamante
         * @param gruppoWallet gruppo di appartenenza del wallet del movimento, già letto dal chiamante
         * @param haPrezzo esito di {@code Prezzi.isMovimentoPrezzato}, già calcolato dal chiamante
         * @param lifoMancante {@code true} se il campo {@code [38]} segnala giacenza insufficiente
         * @return {@code true} se il movimento supera tutti i criteri
         */
        public boolean Passa(String[] v, long dataMovimento, String gruppoWallet,
                             boolean haPrezzo, boolean lifoMancante) {

            if (SoloLifoMancante && !lifoMancante) return false;
            if (SoloSenzaPrezzo && haPrezzo) return false;

            if (NascondiTrasferimentiInterni && v[5].trim().equalsIgnoreCase("Trasferimento Interno")) return false;

            if (dataMovimento < DataInizio || dataMovimento > DataFine) return false;

            if (!Wallet.equalsIgnoreCase(TUTTI)) {
                String gruppoVoluto = GruppoDaVoceWallet(Wallet);
                if (gruppoWallet == null) gruppoWallet = "";
                if (!v[3].equalsIgnoreCase(Wallet) && !gruppoWallet.equalsIgnoreCase(gruppoVoluto)) return false;
            }

            if (!Token.equalsIgnoreCase(TUTTI) && !v[11].equals(Token) && !v[8].equals(Token)) return false;

            //Un movimento è scartato come SCAM solo quando il token marcato è l'unico che muove: uno
            //scambio fra un token buono e uno SCAM resta visibile, altrimenti sparirebbe anche la gamba
            //buona. È la regola che c'era prima ed è stata riportata tale e quale.
            if (NascondiTokenScam
                    && ((Funzioni.isSCAM(v[11]) && v[8].isBlank()) || (Funzioni.isSCAM(v[8]) && v[11].isBlank()))) {
                return false;
            }

            return PassaDocumento(Documento, v.length > 41 ? v[41] : "");
        }

        /**
         * Quanti criteri sono attivi, cioè diversi dal loro valore neutro. È il numero mostrato sul
         * pulsante dei filtri: senza, chiudendo i criteri in un dialogo si perderebbe il colpo d'occhio
         * su "sto guardando tutto oppure una fetta?" che oggi danno le caselle sempre visibili.
         *
         * <p>Le date <b>non</b> contano come criterio: sono sempre valorizzate (le imposta la scheda
         * Crypto.com) e conteggiarle mostrerebbe un filtro attivo anche quando non si è filtrato nulla.
         *
         * @return il numero di criteri attivi, da 0 a 7
         */
        public int Attivi() {
            int n = 0;
            if (!Wallet.equalsIgnoreCase(TUTTI)) n++;
            if (!Token.equalsIgnoreCase(TUTTI)) n++;
            if (!Documento.equals(DOC_TUTTI)) n++;
            if (NascondiTrasferimentiInterni) n++;
            if (NascondiTokenScam) n++;
            if (SoloSenzaPrezzo) n++;
            if (SoloLifoMancante) n++;
            return n;
        }

        /**
         * I criteri attivi scritti per esteso, una riga ciascuno, nello stesso ordine di {@link #Attivi()}.
         *
         * <p>&Egrave; l'elenco mostrato nel tooltip del pulsante "Filtri...": il numero da solo dice
         * <i>quanti</i> criteri sono accesi ma non <i>quali</i>, e i criteri non sono pi&ugrave; in vista da
         * nessuna parte finch&eacute; non si riapre il dialogo.
         *
         * <p><b>La lista ha sempre {@code Attivi()} elementi</b> — il pulsante mostra quel numero e il
         * tooltip questo elenco: se i due divergessero il tooltip sarebbe peggio che inutile.
         * {@code Principale_FiltriMovimentiTest} lo verifica.
         *
         * <p>Le date non compaiono qui per lo stesso motivo per cui {@link #Attivi()} non le conta: sono
         * sempre valorizzate e non sono un filtro che l'utente ha scelto. Chi vuole mostrarle le passa a
         * parte a {@link Principale_FiltriMovimenti#Tooltip}.
         *
         * @param NomeDocumento risolutore facoltativo da id di documento a nome leggibile ({@code null}
         *                      per mostrare il solo id); viene interrogato <b>solo</b> se il criterio sul
         *                      documento &egrave; un id, cos&igrave; la lettura del registro non pesa sui casi
         *                      in cui non serve
         * @return le righe dei soli criteri attivi, lista vuota se non ce n'&egrave; nessuno
         */
        public java.util.List<String> Descrizione(java.util.function.UnaryOperator<String> NomeDocumento) {
            java.util.List<String> righe = new java.util.ArrayList<>();

            if (!Wallet.equalsIgnoreCase(TUTTI)) {
                //La voce della combo ha due forme: un nome di wallet, oppure "Wallet : Gruppo (n)" che
                //seleziona l'intero gruppo. Ripetere la voce grezza non sarebbe "scritto bene".
                String gruppo = GruppoDaVoceWallet(Wallet);
                righe.add(gruppo.isEmpty() ? "Wallet : " + Wallet : "Gruppo di wallet : " + gruppo);
            }
            if (!Token.equalsIgnoreCase(TUTTI)) righe.add("Moneta : " + Token);
            if (!Documento.equals(DOC_TUTTI)) {
                righe.add("Documento di origine : " + DescrizioneDocumento(Documento, NomeDocumento));
            }
            if (NascondiTrasferimentiInterni) righe.add("Trasferimenti interni nascosti");
            if (NascondiTokenScam) righe.add("Token SCAM nascosti");
            if (SoloSenzaPrezzo) righe.add("Solo i movimenti senza prezzo");
            if (SoloLifoMancante) righe.add("Solo i movimenti con LiFo mancante");

            return righe;
        }

        /** @return come {@link #Descrizione(java.util.function.UnaryOperator)}, con il solo id del documento */
        public java.util.List<String> Descrizione() {
            return Descrizione(null);
        }
    }

    /**
     * Rende leggibile il criterio sul documento di origine.
     *
     * @param Criterio {@link #DOC_TUTTI}, {@link #DOC_CON}, {@link #DOC_SENZA} o un id
     * @param NomeDocumento risolutore da id a nome, oppure {@code null}; se solleva un'eccezione (registro
     *                      illeggibile) si ripiega sul solo id, come fa la combo del dialogo
     * @return la descrizione del criterio
     */
    public static String DescrizioneDocumento(String Criterio, java.util.function.UnaryOperator<String> NomeDocumento) {
        if (Criterio == null || Criterio.equals(DOC_TUTTI)) return "tutti";
        if (Criterio.equals(DOC_CON)) return "solo i movimenti che ne hanno uno";
        if (Criterio.equals(DOC_SENZA)) return "solo i movimenti che non ne hanno";

        String nome = null;
        if (NomeDocumento != null) {
            try {
                nome = NomeDocumento.apply(Criterio);
            } catch (Exception e) {
                nome = null;
            }
        }
        return (nome == null || nome.isBlank()) ? "n. " + Criterio : "n. " + Criterio + " — " + nome;
    }

    /**
     * Compone il tooltip del pulsante "Filtri...": la frase fissa che spiega cosa fa il pulsante e sotto
     * l'elenco dei criteri attivi in questo momento.
     *
     * <p>&Egrave; HTML perch&eacute; un tooltip Swing su pi&ugrave; righe non pu&ograve; essere altro, e per la stessa
     * ragione i valori (nomi di wallet, di monete e di file) vanno passati per {@link #EscapeHtml}: una
     * {@code &} in un nome di file troncherebbe il tooltip senza dire nulla.
     *
     * @param Testa la frase fissa del pulsante, mostrata sempre
     * @param F i criteri correnti
     * @param NomeDocumento risolutore da id di documento a nome, oppure {@code null}
     * @param Periodo intervallo di date gi&agrave; formattato, mostrato in fondo e fuori dall'elenco perch&eacute;
     *                non &egrave; un criterio scelto dall'utente; {@code null} o vuoto per ometterlo
     * @param AltriFiltri righe gi&agrave; formattate che descrivono i filtri <b>di riga</b> attivi (campo di
     *                    ricerca, filtri per colonna): non sono criteri di caricamento e stanno perci&ograve;
     *                    in una sezione a parte, ma vanno mostrati o il tooltip direbbe "nessun filtro"
     *                    con una colonna filtrata; {@code null} o vuota per ometterli
     * @return il testo HTML del tooltip
     */
    public static String Tooltip(String Testa, FiltriMovimenti F,
                                 java.util.function.UnaryOperator<String> NomeDocumento, String Periodo,
                                 java.util.List<String> AltriFiltri) {
        StringBuilder s = new StringBuilder("<html>");
        if (Testa != null && !Testa.isBlank()) s.append(EscapeHtml(Testa)).append("<br><br>");

        java.util.List<String> righe = (F == null) ? java.util.List.of() : F.Descrizione(NomeDocumento);
        if (righe.isEmpty()) {
            //"Nessun filtro" senza specificare quali sarebbe falso: i filtri di riga qui sotto possono
            //nascondere righe pur lasciando questo record neutro.
            s.append("<i>Nessun filtro di caricamento attivo.</i>");
        } else {
            s.append("<b>Filtri attivi (").append(righe.size()).append(") :</b>");
            s.append("<ul>");
            for (String r : righe) s.append("<li>").append(EscapeHtml(r)).append("</li>");
            s.append("</ul>");
        }

        if (Periodo != null && !Periodo.isBlank()) {
            if (righe.isEmpty()) s.append("<br>");
            s.append("Periodo : ").append(EscapeHtml(Periodo));
        }

        if (AltriFiltri != null && !AltriFiltri.isEmpty()) {
            s.append("<br><br><b>Sulle righe gi&agrave; caricate :</b><ul>");
            for (String r : AltriFiltri) s.append("<li>").append(EscapeHtml(r)).append("</li>");
            s.append("</ul>");
        }

        return s.append("</html>").toString();
    }

    /** Neutralizza i caratteri che romperebbero un tooltip HTML. */
    public static String EscapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Criteri che non filtrano nulla, con l'intervallo di date passato dal chiamante.
     * @param dataInizio estremo inferiore
     * @param dataFine estremo superiore
     * @return un record neutro
     */
    public static FiltriMovimenti Nessuno(long dataInizio, long dataFine) {
        return new FiltriMovimenti(TUTTI, TUTTI, DOC_TUTTI, dataInizio, dataFine,
                false, false, false, false);
    }

    /**
     * Estrae il nome del gruppo dalla voce della combo dei wallet, che nella forma
     * {@code "Wallet : Gruppo (12)"} indica un gruppo e non un singolo wallet.
     *
     * @param voce la voce selezionata nella combo
     * @return il nome del gruppo, oppure stringa vuota se la voce non ne indica uno
     */
    public static String GruppoDaVoceWallet(String voce) {
        if (voce == null || !voce.contains(":")) return "";
        String[] p = voce.split(" : ");
        if (p.length < 2) return "";
        return p[1].split("\\(")[0].trim();
    }

    /**
     * Applica il criterio sul documento di origine al campo {@code [41]} di un movimento.
     *
     * <p>Il campo contiene <b>solo un id</b>, mai un percorso (vedi {@code DocumentiFonte}), quindi il
     * confronto con un id scelto è un'uguaglianza secca. Vuoto è lo stato legittimo "nessun documento":
     * lo hanno i movimenti inseriti a mano, le rettifiche di giacenza DeFi e tutto ciò che era già in
     * archivio prima che la funzione esistesse.
     *
     * @param criterio {@link #DOC_TUTTI}, {@link #DOC_CON}, {@link #DOC_SENZA} o un id
     * @param campo41 contenuto del campo {@code [41]} del movimento
     * @return {@code true} se il movimento supera il criterio
     */
    public static boolean PassaDocumento(String criterio, String campo41) {
        if (criterio == null || criterio.equals(DOC_TUTTI)) return true;
        boolean haDocumento = campo41 != null && !campo41.trim().isEmpty();
        if (criterio.equals(DOC_CON)) return haDocumento;
        if (criterio.equals(DOC_SENZA)) return !haDocumento;
        return haDocumento && campo41.trim().equals(criterio.trim());
    }
}
