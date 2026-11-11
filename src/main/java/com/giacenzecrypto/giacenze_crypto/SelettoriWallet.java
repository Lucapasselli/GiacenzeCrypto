package com.giacenzecrypto.giacenze_crypto;

import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

/**
 * Le tre combo gruppo wallet, wallet e sotto-wallet con la loro regola, in un posto solo.
 * <p>
 * Le usano "Giacenze a data" e la finestra Filtri dei movimenti. Un gruppo scelto restringe i wallet ai suoi, un
 * wallet scelto porta il gruppo su quello che lo contiene, il sotto-wallet si sceglie solo con un wallet. La classe
 * possiede i listener delle tre combo (il chiamante non ne aggiunge) e dopo ogni cambio, anche fatto dal codice,
 * esegue {@link #setAlCambio}: da lì il chiamante aggiorna quello che dipende dalla scelta. I dati arrivano da chi la
 * crea, così non conosce né {@code Principale} né il database.
 * <p>
 * La decisione pura (stringa unica, restringimento, voci) è in {@link Principale_GiacenzeaData_FiltroWallet}.
 */
public final class SelettoriWallet {

    private static final String TUTTI = Principale_GiacenzeaData_FiltroWallet.TUTTI;

    private final JComboBox<String> Gruppo;
    private final JComboBox<String> Wallet;
    private final JComboBox<String> SottoWallet;
    private final Supplier<? extends Collection<String>> WalletNoti;
    private final Function<String, String> GruppoDi;
    private final Function<String, List<String>> SottoWalletDi;

    private Runnable AlCambio = () -> { };

    /** Vero mentre il codice sistema le combo: i loro listener non devono rispondere ai valori intermedi. */
    private boolean Aggiornando = false;

    /**
     * @param Gruppo combo dei gruppi, con voci {@code "Wallet 1 ( alias )"}
     * @param Wallet combo dei wallet
     * @param SottoWallet combo dei sotto-wallet, spenta finché non c'è un wallet
     * @param WalletNoti i wallet esistenti, letti a ogni uso (l'elenco può cambiare)
     * @param GruppoDi identificativo del gruppo di un wallet ({@code "Wallet 1"})
     * @param SottoWalletDi i sotto-wallet di un wallet, oppure {@code null}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public SelettoriWallet(JComboBox Gruppo, JComboBox Wallet, JComboBox SottoWallet,
            Supplier<? extends Collection<String>> WalletNoti, Function<String, String> GruppoDi,
            Function<String, List<String>> SottoWalletDi) {
        this.Gruppo = Gruppo;
        this.Wallet = Wallet;
        this.SottoWallet = SottoWallet;
        this.WalletNoti = WalletNoti;
        this.GruppoDi = GruppoDi;
        this.SottoWalletDi = SottoWalletDi;
        Gruppo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) GruppoCambiato();
        });
        Wallet.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) WalletCambiato();
        });
        SottoWallet.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && !Aggiornando) AlCambio.run();
        });
    }

    /** Cosa fare dopo ogni cambio di selezione, dell'utente o del codice. */
    public void setAlCambio(Runnable AlCambio) {
        this.AlCambio = AlCambio == null ? () -> { } : AlCambio;
    }

    /** Rifà la combo dei gruppi (con "Tutti" in testa). Dopo, chiamare {@link #Applica} per riposizionare le altre. */
    public void ImpostaGruppi(Collection<String> voci) {
        List<String> l = new ArrayList<>();
        l.add(TUTTI);
        l.addAll(voci);
        boolean precedente = Aggiornando;
        Aggiornando = true;
        try {
            Gruppo.setModel(new DefaultComboBoxModel<>(l.toArray(String[]::new)));
        } finally {
            Aggiornando = precedente;
        }
    }

    /** La stringa unica che il calcolo si aspetta: il wallet, altrimenti {@code "Gruppo : X ( alias )"}, altrimenti "Tutti". */
    public String Effettivo() {
        return Principale_GiacenzeaData_FiltroWallet.Effettivo(
                Principale_GiacenzeaData_FiltroWallet.NomeWallet(Scelta(Wallet)), Scelta(Gruppo));
    }

    /** Il sotto-wallet scelto, "Tutti" se la combo è spenta. */
    public String SottoWalletScelto() {
        return SottoWallet.isEnabled() ? Scelta(SottoWallet) : TUTTI;
    }

    /**
     * Porta le tre combo in uno stato coerente.
     *
     * @param Gruppo voce o identificativo del gruppo, "Tutti" per nessuno
     * @param Wallet wallet da selezionare: se non fa parte del gruppo si torna a "Tutti", se non esiste più compare
     *               con l'etichetta "non più disponibile" e il filtro non viene tolto da solo
     * @param SottoWalletVoce sotto-wallet da riselezionare (resta anche se non si trova più, per lo stesso motivo)
     */
    public void Applica(String Gruppo, String Wallet, String SottoWalletVoce) {
        String wallet = Principale_GiacenzeaData_FiltroWallet.NomeWallet(Wallet);
        boolean precedente = Aggiornando;
        Aggiornando = true;
        try {
            List<String> voci = new ArrayList<>();
            for (int i = 0; i < this.Gruppo.getItemCount(); i++) voci.add(this.Gruppo.getItemAt(i));
            String voce = Principale_GiacenzeaData_FiltroWallet.TrovaVoceGruppo(voci, Gruppo);
            this.Gruppo.setSelectedItem(voce == null ? TUTTI : voce);

            Collection<String> noti = WalletNoti.get();
            List<String> elenco = Principale_GiacenzeaData_FiltroWallet.WalletDelGruppo(noti, voce, GruppoDi);
            boolean scomparso = !Principale_GiacenzeaData_FiltroWallet.IsTutti(wallet) && !noti.contains(wallet);
            String etichetta = Principale_GiacenzeaData_FiltroWallet.EtichettaWalletMancante(wallet);
            if (scomparso) elenco.add(etichetta);
            this.Wallet.setModel(new DefaultComboBoxModel<>(elenco.toArray(String[]::new)));
            this.Wallet.setSelectedItem(scomparso ? etichetta : elenco.contains(wallet) ? wallet : TUTTI);
            RiempiSottoWallet(SottoWalletVoce);
        } finally {
            Aggiornando = precedente;
        }
        AlCambio.run();
    }

    /**
     * Mette le combo sullo stato di un filtro esistente.
     *
     * @param scelta {@code "Tutti"}, il nome di un wallet o {@code "Gruppo : X ( alias )"}
     * @param sottoWallet il sotto-wallet, che ha senso solo con un wallet
     */
    public void ImpostaDaFiltro(String scelta, String sottoWallet) {
        String prefisso = Principale_GiacenzeaData_FiltroWallet.PREFISSO_GRUPPO;
        if (Principale_GiacenzeaData_FiltroWallet.IsTutti(scelta)) {
            Applica(TUTTI, TUTTI, TUTTI);
        } else if (scelta.startsWith(prefisso)) {
            //Un gruppo che non c'è più resta come voce: il filtro non deve sparire da solo
            List<String> voci = new ArrayList<>();
            for (int i = 1; i < Gruppo.getItemCount(); i++) voci.add(Gruppo.getItemAt(i));
            if (Principale_GiacenzeaData_FiltroWallet.TrovaVoceGruppo(voci, scelta) == null) {
                voci.add(scelta.substring(prefisso.length()));
                ImpostaGruppi(voci);
            }
            Applica(scelta, TUTTI, TUTTI);
        } else {
            Applica(WalletNoti.get().contains(scelta) ? GruppoDi.apply(scelta) : TUTTI, scelta, sottoWallet);
        }
    }

    private void GruppoCambiato() {
        if (Aggiornando) return;
        Applica(Scelta(Gruppo), Scelta(Wallet), Scelta(SottoWallet));
    }

    /** Scelto un wallet: il gruppo si posiziona su quello che lo contiene, il sotto-wallet riparte da "Tutti". */
    private void WalletCambiato() {
        if (Aggiornando) return;
        String wallet = Principale_GiacenzeaData_FiltroWallet.NomeWallet(Scelta(Wallet));
        String gruppo = Scelta(Gruppo);
        if (!Principale_GiacenzeaData_FiltroWallet.IsTutti(wallet) && WalletNoti.get().contains(wallet)) {
            gruppo = GruppoDi.apply(wallet);
        }
        Applica(gruppo, wallet, TUTTI);
    }

    /** Riempie i sotto-wallet del wallet scelto; la combo si accende solo con un wallet. */
    private void RiempiSottoWallet(String vecchio) {
        String wallet = Principale_GiacenzeaData_FiltroWallet.NomeWallet(Scelta(Wallet));
        boolean conWallet = !Principale_GiacenzeaData_FiltroWallet.IsTutti(wallet);
        List<String> voci = new ArrayList<>();
        voci.add(TUTTI);
        List<String> noti = conWallet ? SottoWalletDi.apply(wallet) : null;
        if (noti != null) voci.addAll(noti);
        if (conWallet && !Principale_GiacenzeaData_FiltroWallet.IsTutti(vecchio) && !voci.contains(vecchio)) {
            voci.add(vecchio);
        }
        boolean precedente = Aggiornando;
        Aggiornando = true;
        try {
            SottoWallet.setModel(new DefaultComboBoxModel<>(voci.toArray(String[]::new)));
            SottoWallet.setSelectedItem(conWallet && voci.contains(vecchio) ? vecchio : TUTTI);
        } finally {
            Aggiornando = precedente;
        }
        SottoWallet.setEnabled(conWallet);
    }

    private static String Scelta(JComboBox<String> combo) {
        Object o = combo.getSelectedItem();
        return o == null ? "" : o.toString();
    }
}
