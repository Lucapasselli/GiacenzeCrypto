package com.giacenzecrypto.giacenze_crypto;

import java.awt.Window;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

/**
 * Wallet Bitcoin composto da piu' indirizzi scelti dall'utente, per chi non ha (o non vuole usare) la
 * chiave estesa. Il wallet sta in {@code WALLETS} con un nome al posto dell'indirizzo, gli indirizzi in
 * {@code WALLET_BTC_INDIRIZZI}; allo scarico {@link Trans_Bitcoin#IndirizziDaScansionare} li usa come
 * fa con quelli derivati da una xpub, quindi resti e consolidamenti fra indirizzi propri restano
 * interni invece di diventare un prelievo e un deposito da abbinare a mano.
 *
 * <p><b>Cambiare l'elenco di un wallet gia' importato.</b> Aggiungere o togliere l'indirizzo N cambia il
 * significato soltanto delle transazioni in cui N compare (input o output): per tutte le altre
 * {@link Trans_Bitcoin#analizza} da' lo stesso risultato con l'elenco vecchio e con quello nuovo. Ogni
 * transazione che tocca N sta nella cronologia di N, quindi basta scaricare quella e rileggere ogni
 * transazione con i due elenchi: gli altri indirizzi non si riscaricano.
 *
 * <p>Si considerano solo le transazioni fino all'ultimo blocco gia' importato ({@code v[23]} piu'
 * alto del wallet): quelle successive le prende lo scarico incrementale normale, gia' con l'elenco
 * nuovo. Aggiungere una transazione piu' recente di quel blocco sposterebbe in avanti il punto da cui
 * riparte lo scarico, e le transazioni degli altri indirizzi rimaste in mezzo non verrebbero mai lette.
 */
public class WalletBtcMultiIndirizzo {

    static final String RETE = "BTC";

    /** Il campo wallet {@code [3]} dei movimenti del wallet */
    static String CampoWallet(String Nome) {
        return Nome + " (" + RETE + ")";
    }

    enum TipoVariazione { AGGIUNTA, SOSTITUITA, RIMOSSA }

    /** Cosa succede a una transazione quando cambia l'elenco degli indirizzi */
    static final class Variazione {
        final String Txid;
        final TipoVariazione Tipo;
        /** ID dei movimenti che la transazione ha oggi nel wallet (vuoto per un'aggiunta) */
        final List<String> IdVecchi;
        /**
         * La transazione in formato mempool.space, da rileggere con l'elenco nuovo in {@link #Applica}
         * ({@code null} per una rimozione). Non si costruisce qui il movimento perche' costruirlo vuol
         * dire gia' recuperare i prezzi, e il piano deve restare gratuito finche' l'utente non conferma
         */
        final JSONObject Tx;
        /** Almeno uno dei movimenti attuali e' stato ritoccato a mano, vedi {@link #ToccatoAMano} */
        final boolean ToccataAMano;

        Variazione(String Txid, TipoVariazione Tipo, List<String> IdVecchi, JSONObject Tx, boolean ToccataAMano) {
            this.Txid = Txid;
            this.Tipo = Tipo;
            this.IdVecchi = IdVecchi;
            this.Tx = Tx;
            this.ToccataAMano = ToccataAMano;
        }
    }

    /** Il risultato del confronto, da mostrare all'utente prima di applicarlo */
    static final class Piano {
        final String Nome;
        final Set<String> Nuovi;
        final List<Variazione> Variazioni = new ArrayList<>();
        /** Movimenti BRC-20/Runes degli indirizzi aggiunti non ancora presenti nel wallet */
        final Map<String, TransazioneDefi> TokenNuovi = new LinkedHashMap<>();
        /** Movimenti BRC-20/Runes gia' nel wallet: se si toglie un indirizzo vanno controllati a mano */
        int TokenEsistenti;
        int Invariate;

        long Conta(TipoVariazione Tipo) {
            return Variazioni.stream().filter(v -> v.Tipo == Tipo).count();
        }

        long ContaToccate() {
            return Variazioni.stream().filter(v -> v.ToccataAMano).count();
        }

        boolean isVuoto() {
            return Variazioni.isEmpty() && TokenNuovi.isEmpty();
        }

        Piano(String Nome, Set<String> Nuovi) {
            this.Nome = Nome;
            this.Nuovi = Nuovi;
        }
    }

    /**
     * L'elenco scritto dall'utente, un indirizzo per riga (vanno bene anche virgole e punti e virgola).
     * Gli indirizzi bech32 ({@code bc1...}) sono validi anche in maiuscolo, come escono spesso dai QR
     * code, ma mempool.space li restituisce in minuscolo e {@link Trans_Bitcoin#analizza} li confronta
     * esattamente: si portano in minuscolo qui. Mai gli indirizzi base58 ({@code 1...}, {@code 3...}),
     * dove maiuscole e minuscole sono caratteri diversi.
     */
    static Set<String> LeggiElenco(String Testo) {
        Set<String> Indirizzi = new LinkedHashSet<>();
        for (String Riga : Testo.split("[\\r\\n,;]+")) {
            String a = Riga.trim();
            if (a.isEmpty()) continue;
            if (a.toLowerCase().startsWith("bc1")) a = a.toLowerCase();
            Indirizzi.add(a);
        }
        return Indirizzi;
    }

    /**
     * Controlla l'elenco di indirizzi di un wallet prima di salvarlo.
     *
     * @param Nome nome del wallet che si sta modificando
     * @param Indirizzi elenco nuovo
     * @param WalletsTabella la tabella {@code WALLETS} come la restituisce
     *        {@link DatabaseH2#Pers_Wallets_LeggiTabella()}
     * @param IndirizziAltriWallet indirizzo → wallet multi-indirizzo che lo contiene, per tutti i wallet
     * @return {@code null} se l'elenco va bene, altrimenti il motivo
     */
    static String ValidaIndirizzi(String Nome, Set<String> Indirizzi, Map<String, String> WalletsTabella,
            Map<String, String> IndirizziAltriWallet) {
        for (String Indirizzo : Indirizzi) {
            if (Trans_Bitcoin.isExtendedKey(Indirizzo)) {
                return "\"" + Indirizzo + "\" e' una chiave estesa: va inserita come wallet a se', non dentro un elenco";
            }
            if (!Trans_Bitcoin.isValidBitcoinAddress(Indirizzo)) {
                return "\"" + Indirizzo + "\" non e' un indirizzo Bitcoin valido";
            }
            //Lo stesso indirizzo in due wallet farebbe scaricare due volte le stesse transazioni e
            //raddoppierebbe le giacenze
            if (WalletsTabella.get(Indirizzo + "_" + RETE) != null) {
                return "L'indirizzo " + Indirizzo + " e' gia' nella lista come wallet a se'.<br>"
                        + "Va eliminato prima quel wallet (con i suoi movimenti), poi l'indirizzo si puo' aggiungere qui";
            }
            String Altro = IndirizziAltriWallet.get(Indirizzo);
            if (Altro != null && !Altro.equals(Nome)) {
                return "L'indirizzo " + Indirizzo + " fa gia' parte del wallet \"" + Altro + "\"";
            }
        }
        return null;
    }

    /**
     * Un movimento che l'utente ha gia' ritoccato: modificato (lignaggio), abbinato ad altri movimenti,
     * classificato o con un prezzo scelto a mano. Sostituirlo in silenzio butterebbe via quel lavoro.
     */
    static boolean ToccatoAMano(String[] v) {
        return !Funzioni.noData(v[42]) || !Funzioni.noData(v[20]) || !Funzioni.noData(v[18])
                || (v[40] != null && v[40].contains("Personalizzato"));
    }

    /** Movimento del solo BTC (trasferimento o commissione), non BRC-20 ne' Runes */
    private static boolean SoloBtc(String[] v) {
        return (Funzioni.noData(v[8]) || v[8].equalsIgnoreCase(RETE))
                && (Funzioni.noData(v[11]) || v[11].equalsIgnoreCase(RETE));
    }

    /**
     * L'ultimo blocco importato del wallet, cioe' il punto da cui riparte lo scarico incrementale.
     * @return -1 se il wallet non ha movimenti
     */
    static int UltimoBloccoImportato(String Nome, Map<String, String[]> Movimenti) {
        String Campo = CampoWallet(Nome);
        int Max = -1;
        for (String[] v : Movimenti.values()) {
            if (v[3] != null && v[3].trim().equalsIgnoreCase(Campo) && Funzioni.isNumeric(v[23], false)) {
                try {
                    Max = Math.max(Max, Integer.parseInt(v[23].trim()));
                } catch (NumberFormatException e) {
                    //blocco non intero: non e' un movimento da explorer
                }
            }
        }
        return Max;
    }

    /**
     * Confronta ogni transazione letta con l'elenco vecchio e con quello nuovo. Non tocca nulla e non va
     * in rete.
     *
     * <ul>
     *   <li>movimenti presenti e analisi uguale: la transazione non cambia;</li>
     *   <li>movimenti presenti e nessun indirizzo nuovo coinvolto (o solo commissione, che lo scarico
     *       normale scarta): i movimenti vanno tolti;</li>
     *   <li>movimenti presenti e analisi diversa: vanno sostituiti;</li>
     *   <li>nessun movimento e transazione che non toccava l'elenco vecchio: va aggiunta;</li>
     *   <li>nessun movimento ma transazione che toccava gia' l'elenco vecchio: l'utente l'ha cancellata
     *       (o era di sola commissione), resta com'e'.</li>
     * </ul>
     *
     * @param Transazioni le transazioni degli indirizzi aggiunti e tolti, in formato mempool.space
     * @param BloccoMassimo ultimo blocco importato, vedi {@link #UltimoBloccoImportato}
     */
    static Piano CalcolaVariazioni(String Nome, Set<String> Vecchi, Set<String> Nuovi,
            Collection<JSONObject> Transazioni, Map<String, String[]> Movimenti, int BloccoMassimo) {
        Piano P = new Piano(Nome, Nuovi);
        String Campo = CampoWallet(Nome);

        Map<String, List<String>> IdPerTx = new HashMap<>();
        for (Map.Entry<String, String[]> e : Movimenti.entrySet()) {
            String[] v = e.getValue();
            if (v[3] == null || !v[3].trim().equalsIgnoreCase(Campo) || Funzioni.noData(v[24])) continue;
            if (SoloBtc(v)) {
                IdPerTx.computeIfAbsent(v[24].trim().toLowerCase(), k -> new ArrayList<>()).add(e.getKey());
            } else {
                P.TokenEsistenti++;
            }
        }

        Set<String> Visti = new LinkedHashSet<>();
        for (JSONObject tx : Transazioni) {
            String Txid = tx.optString("txid", "");
            if (Txid.isEmpty() || !Visti.add(Txid.toLowerCase())) continue;
            JSONObject Stato = tx.optJSONObject("status");
            int Blocco = Stato != null ? Stato.optInt("block_height", 0) : 0;
            if (Blocco <= 0 || Blocco > BloccoMassimo) continue;

            Trans_Bitcoin.Analisi Prima = Trans_Bitcoin.analizza(tx, Vecchi);
            Trans_Bitcoin.Analisi Dopo = Trans_Bitcoin.analizza(tx, Nuovi);
            List<String> Ids = IdPerTx.getOrDefault(Txid.toLowerCase(), List.of());

            if (Ids.isEmpty()) {
                if (Prima == null && Dopo != null && !Dopo.SoloCommissione()) {
                    P.Variazioni.add(new Variazione(Txid, TipoVariazione.AGGIUNTA, List.of(), tx, false));
                }
                continue;
            }

            if (Prima != null && Prima.equals(Dopo)) {
                P.Invariate++;
                continue;
            }
            boolean Toccata = Ids.stream().anyMatch(id -> ToccatoAMano(Movimenti.get(id)));
            if (Dopo == null || Dopo.SoloCommissione()) {
                P.Variazioni.add(new Variazione(Txid, TipoVariazione.RIMOSSA, Ids, null, Toccata));
            } else {
                P.Variazioni.add(new Variazione(Txid, TipoVariazione.SOSTITUITA, Ids, tx, Toccata));
            }
        }
        return P;
    }

    /**
     * Hash delle transazioni che hanno gia' movimenti BRC-20/Runes nel wallet, per non importarle due
     * volte. Limite noto: un trasferimento di token fra un indirizzo vecchio e uno aggiunto resta con
     * la sola gamba gia' importata, come succede oggi allo scarico da chiave estesa.
     */
    private static Set<String> HashTokenEsistenti(String Nome, Map<String, String[]> Movimenti) {
        String Campo = CampoWallet(Nome);
        Set<String> Hash = new java.util.HashSet<>();
        for (String[] v : Movimenti.values()) {
            if (v[3] == null || !v[3].trim().equalsIgnoreCase(Campo) || Funzioni.noData(v[24])) continue;
            if (!SoloBtc(v)) Hash.add(v[24].trim().toLowerCase());
        }
        return Hash;
    }

    /** Categoria di un movimento, l'ultimo segmento dell'ID */
    private static String Categoria(String Id) {
        String[] s = Id.split("_");
        return s[s.length - 1];
    }

    /**
     * Applica il piano a {@link Principale#MappaCryptoWallet}. Le righe nuove si costruiscono (e si
     * prezzano) tutte prima di toccare la mappa: un'interruzione o una caduta di rete a meta' lascia i
     * movimenti come erano.
     *
     * <p>Quando un movimento ritoccato a mano viene sostituito, il nuovo riceve il suo lignaggio e la
     * versione vecchia entra nello storico, come per qualunque altra modifica.
     *
     * @param AncheToccate se {@code false} le transazioni con movimenti ritoccati a mano restano come sono
     * @return numero di movimenti scritti, oppure -1 se l'operazione e' stata interrotta
     */
    static int Applica(Piano P, boolean AncheToccate, int IdDocumento, Download Progress) {
        //Fase 1: costruzione delle righe, con recupero dei prezzi
        Map<Variazione, List<String[]>> Righe = new LinkedHashMap<>();
        Progress.SetMassimo(P.Variazioni.size() + P.TokenNuovi.size());
        int Fatte = 0;
        for (Variazione V : P.Variazioni) {
            if (Progress.FineThread() || AttesaConnessione.Abortita()) return -1;
            Progress.SetAvanzamento(++Fatte);
            if (V.ToccataAMano && !AncheToccate) continue;
            TransazioneDefi Nuova = V.Tx == null ? null : Trans_Bitcoin.parseTransaction(V.Tx, P.Nome, P.Nuovi);
            Righe.put(V, Nuova == null || Nuova.isEmpty() ? List.of() : Nuova.RitornaRigheTabella());
        }
        List<String[]> RigheToken = new ArrayList<>();
        for (TransazioneDefi T : P.TokenNuovi.values()) {
            if (Progress.FineThread() || AttesaConnessione.Abortita()) return -1;
            Progress.SetAvanzamento(++Fatte);
            RigheToken.addAll(T.RitornaRigheTabella());
        }
        if (Progress.FineThread() || AttesaConnessione.Abortita()) return -1;

        //Fase 2: scrittura
        Map<String, String[]> Mappa = Principale.MappaCryptoWallet;
        int Scritti = 0;
        for (Map.Entry<Variazione, List<String[]>> e : Righe.entrySet()) {
            Variazione V = e.getKey();
            Map<String, String[]> Vecchi = new LinkedHashMap<>();
            for (String Id : V.IdVecchi) {
                String[] v = Mappa.get(Id);
                if (v != null) Vecchi.put(Id, v.clone());
            }
            for (String Id : Vecchi.keySet()) {
                Funzioni.RimuoviMovimentazioneXID(Id);
            }
            Set<String> LignaggiRiportati = new java.util.HashSet<>();
            for (String[] st : e.getValue()) {
                //Il lignaggio passa dal movimento vecchio della stessa categoria (trasferimento o
                //commissione): e' la chiave dello storico delle sue modifiche
                for (Map.Entry<String, String[]> Vecchio : Vecchi.entrySet()) {
                    String Lignaggio = Vecchio.getValue()[MovimentiStorico.CAMPO_LIGNAGGIO];
                    if (!Funzioni.noData(Lignaggio) && !LignaggiRiportati.contains(Lignaggio)
                            && Categoria(Vecchio.getKey()).equals(Categoria(st[0]))) {
                        st[MovimentiStorico.CAMPO_LIGNAGGIO] = Lignaggio;
                        LignaggiRiportati.add(Lignaggio);
                        break;
                    }
                }
                String IdNuovo = ScriviRiga(st, IdDocumento);
                Scritti++;
                if (!Funzioni.noData(st[MovimentiStorico.CAMPO_LIGNAGGIO])) {
                    for (Map.Entry<String, String[]> Vecchio : Vecchi.entrySet()) {
                        if (st[MovimentiStorico.CAMPO_LIGNAGGIO].equals(Vecchio.getValue()[MovimentiStorico.CAMPO_LIGNAGGIO])) {
                            MovimentiStorico.AccodaModifica(st[MovimentiStorico.CAMPO_LIGNAGGIO], IdNuovo, Vecchio.getKey(),
                                    Importazioni.SerializzaRiga(Vecchio.getValue()), "RielaboraIndirizziBTC");
                        }
                    }
                }
            }
            //I lignaggi rimasti senza un movimento nuovo: lo storico si ripulisce al salvataggio, se
            //nessun movimento vivo li porta piu'
            for (String[] v : Vecchi.values()) {
                String Lignaggio = v[MovimentiStorico.CAMPO_LIGNAGGIO];
                if (!Funzioni.noData(Lignaggio) && !LignaggiRiportati.contains(Lignaggio)) {
                    MovimentiStorico.AccodaCancellazione(Lignaggio);
                }
            }
        }
        for (String[] st : RigheToken) {
            ScriviRiga(st, IdDocumento);
            Scritti++;
        }
        return Scritti;
    }

    private static String ScriviRiga(String[] st, int IdDocumento) {
        Principale.Funzione_AggiornaMappaWallets(st);
        st[0] = MovimentiCrypto.getIDUnivoco(Principale.MappaCryptoWallet, st[0]);
        if (IdDocumento > 0 && st.length > 41 && Funzioni.noData(st[41])) st[41] = String.valueOf(IdDocumento);
        Importazioni.InserisciMovimentosuMappaCryptoWallet(st[0], st);
        return st[0];
    }

    /**
     * Salva il nuovo elenco di indirizzi di un wallet. Se il wallet ha gia' movimenti e l'elenco e'
     * cambiato, scarica la cronologia degli indirizzi aggiunti e tolti, mostra cosa cambierebbe e lo
     * applica solo se l'utente conferma; se non conferma, l'elenco non viene salvato, perche' elenco e
     * movimenti devono restare d'accordo.
     *
     * @return {@code true} se l'elenco e' stato salvato
     */
    public static boolean ModificaIndirizzi(String Nome, Set<String> Nuovi, Window Owner) {
        Set<String> Vecchi = DatabaseH2.Pers_WalletBtcIndirizzi_Leggi(Nome);
        if (Vecchi.equals(Nuovi)) return true;

        int BloccoMassimo = UltimoBloccoImportato(Nome, Principale.MappaCryptoWallet);
        if (BloccoMassimo < 0) {
            //Nessun movimento: lo scarico partira' dall'inizio con l'elenco nuovo
            return DatabaseH2.Pers_WalletBtcIndirizzi_Scrivi(Nome, Nuovi);
        }

        Set<String> Cambiati = new LinkedHashSet<>();
        for (String a : Nuovi) if (!Vecchi.contains(a)) Cambiati.add(a);
        Set<String> Aggiunti = new LinkedHashSet<>(Cambiati);
        for (String a : Vecchi) if (!Nuovi.contains(a)) Cambiati.add(a);
        boolean CiSonoRimossi = Cambiati.size() > Aggiunti.size();

        final boolean[] Salvato = {false};
        final int[] Scritti = {0};
        final String[] Errore = {null};
        Download Progress = new Download();
        Progress.setLocationRelativeTo(Owner);
        Progress.Titolo("Wallet BTC " + Nome + ": rielaborazione indirizzi");

        Thread T = new Thread(() -> {
            Importazioni.TransazioniAggiunte = 0;
            int IdDocumento = DocumentiFonte.ApriSessione("DeFi");
            Importazioni.DocumentoFonteCorrente = IdDocumento;
            AttesaConnessione.Apri(Progress);
            try {
                //1 - cronologia degli indirizzi cambiati
                List<JSONObject> Transazioni = new ArrayList<>();
                int n = 0;
                for (String Indirizzo : Cambiati) {
                    if (Progress.FineThread()) return;
                    Progress.SetLabel("Scaricamento " + (++n) + "/" + Cambiati.size() + ": " + Indirizzo);
                    Transazioni.addAll(Trans_Bitcoin.CronologiaIndirizzo(Indirizzo));
                }
                Piano P = CalcolaVariazioni(Nome, Vecchi, Nuovi, Transazioni, Principale.MappaCryptoWallet, BloccoMassimo);

                //2 - BRC-20 e Runes degli indirizzi aggiunti, se c'e' la chiave UniSat
                String ApiKeyUniSat = Funzioni.TrasformaNullinBlanc(DatabaseH2.Opzioni_Leggi("ApiKey_UniSat"));
                if (!ApiKeyUniSat.isBlank()) {
                    Set<String> Esistenti = HashTokenEsistenti(Nome, Principale.MappaCryptoWallet);
                    for (String Indirizzo : Aggiunti) {
                        if (Progress.FineThread()) return;
                        Progress.SetLabel("BRC-20 e Runes di " + Indirizzo);
                        for (Map.Entry<String, TransazioneDefi> e : Trans_Bitcoin.TokenIndirizzoFinoAlBlocco(
                                Indirizzo, Nome, BloccoMassimo, ApiKeyUniSat, Progress).entrySet()) {
                            if (!Esistenti.contains(e.getValue().HashTransazione.toLowerCase())) {
                                P.TokenNuovi.putIfAbsent(e.getKey(), e.getValue());
                            }
                        }
                    }
                }
                if (Progress.FineThread() || AttesaConnessione.Abortita()) return;

                //3 - conferma
                String Scelta = ChiediConferma(P, CiSonoRimossi, Owner);
                if (Scelta == null) return;
                if (!P.isVuoto()) {
                    Progress.SetLabel("Recupero prezzi e scrittura movimenti...");
                    int r = Applica(P, Scelta.equals("tutte"), IdDocumento, Progress);
                    if (r < 0) return;
                    Scritti[0] = r;
                }
                Salvato[0] = DatabaseH2.Pers_WalletBtcIndirizzi_Scrivi(Nome, Nuovi);
                Importazioni.TransazioniAggiunte = Scritti[0];
                if (Scritti[0] > 0 || P.Conta(TipoVariazione.RIMOSSA) > 0) Principale.TabellaCryptodaAggiornare = true;
            } catch (InterruptedException ex) {
                Errore[0] = "Operazione interrotta";
            } catch (Exception ex) {
                LoggerGC.ScriviErrore(ex);
                Errore[0] = "Scaricamento non riuscito: " + ex.getMessage();
            } finally {
                AttesaConnessione.Chiudi();
                Importazioni.DocumentoFonteCorrente = 0;
                DocumentiFonte.ChiudiSessione(IdDocumento);
                DocumentiFonte.ChiudiRegistrazione(new DocumentiFonte.Registrazione(IdDocumento, true), Scritti[0]);
                Progress.dispose();
            }
        });
        Progress.SetThread(T);
        T.start();
        Progress.setVisible(true);

        if (Errore[0] != null) {
            Messaggi.WarningMessage("Indirizzi non salvati", Errore[0] + "<br>L'elenco degli indirizzi non e' stato modificato.", Owner);
        } else if (Salvato[0] && Scritti[0] > 0) {
            Messaggi.SuccessMessage("Indirizzi salvati",
                    "Elenco salvato, movimenti scritti: " + Scritti[0]
                    + "<br>Ricordarsi di Salvare i movimenti: se le modifiche vengono annullate, l'elenco "
                    + "degli indirizzi resta quello nuovo mentre i movimenti tornano a quello vecchio.", Owner);
        }
        return Salvato[0];
    }

    /**
     * @return {@code "tutte"}, {@code "non_toccate"} oppure {@code null} se l'utente annulla
     */
    private static String ChiediConferma(Piano P, boolean CiSonoRimossi, Window Owner) {
        StringBuilder Testo = new StringBuilder();
        if (P.isVuoto()) {
            Testo.append("Nessun movimento gia' importato cambia con il nuovo elenco.");
        } else {
            Testo.append("Con il nuovo elenco di indirizzi:<br>")
                    .append("- transazioni da aggiungere: ").append(P.Conta(TipoVariazione.AGGIUNTA)).append("<br>")
                    .append("- transazioni i cui movimenti cambiano: ").append(P.Conta(TipoVariazione.SOSTITUITA)).append("<br>")
                    .append("- transazioni i cui movimenti vanno tolti: ").append(P.Conta(TipoVariazione.RIMOSSA)).append("<br>");
            if (!P.TokenNuovi.isEmpty()) {
                Testo.append("- movimenti BRC-20/Runes da aggiungere: ").append(P.TokenNuovi.size()).append("<br>");
            }
            Testo.append("(transazioni non toccate: ").append(P.Invariate).append(")");
            if (P.ContaToccate() > 0) {
                Testo.append("<br><br><b>").append(P.ContaToccate())
                        .append(" transazioni hanno movimenti modificati, classificati o abbinati a mano.</b><br>")
                        .append("Si possono lasciare come sono oppure sostituire anche quelle "
                                + "(la classificazione e gli abbinamenti andranno rifatti).");
            }
        }
        if (CiSonoRimossi && P.TokenEsistenti > 0) {
            Testo.append("<br><br>Il wallet ha ").append(P.TokenEsistenti)
                    .append(" movimenti BRC-20/Runes: quelli degli indirizzi tolti vanno controllati a mano.");
        }
        Testo.append("<br><br>I movimenti andranno poi salvati.");

        AppDialog.Builder B = AppDialog.builder(Owner)
                .windowTitle("Indirizzi del wallet BTC")
                .bodyTitle("Indirizzi del wallet BTC")
                .showTitleInBody(false)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message("")
                .details(Testo.toString())
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build());
        if (P.ContaToccate() > 0) {
            B = B.action(AppDialog.DialogAction.builder("tutte", "Sostituisci anche quelle")
                    .role(AppDialog.ActionRole.DANGER)
                    .build())
                 .action(AppDialog.DialogAction.builder("non_toccate", "Lascia quelle come sono")
                    .role(AppDialog.ActionRole.PRIMARY)
                    .build());
        } else {
            B = B.action(AppDialog.DialogAction.builder("tutte", "Applica")
                    .role(AppDialog.ActionRole.PRIMARY)
                    .build());
        }
        AppDialog.DialogResult R = B.showDialog();
        if (R == null || R.isAction("cancel")) return null;
        return R.isAction("tutte") ? "tutte" : R.isAction("non_toccate") ? "non_toccate" : null;
    }
}
