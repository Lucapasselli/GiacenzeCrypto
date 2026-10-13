package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Il registro unico dei file di {@code config/} tenuti in memoria e di cosa fare quando l'aggiornamento in background
 * ne porta una copia nuova.
 *
 * <p>Prima ogni file in memoria aveva il suo blocco {@code if} dentro il thread di aggiornamento di {@code Principale},
 * e dimenticarlo (alias dei prezzi fino al 2026-10-08) voleva dire una voce nuova inattiva fino al riavvio, con le
 * importazioni di quella sessione senza prezzo. Ora un file nuovo di {@code config/varie} o {@code config/importmappe}
 * deve comparire qui in uno dei due elenchi: {@link #VOCI} se resta in memoria, {@link #LETTI_A_OGNI_USO} se si rilegge
 * a ogni uso. {@code RicaricaConfigTest} scandisce le cartelle e fallisce per un file che non sta in nessuno dei due, così
 * la scelta non si può dimenticare.
 */
final class RicaricaConfig {

    private RicaricaConfig() {}

    /**
     * Un file tenuto in memoria.
     *
     * @param cartella    {@code varie} o {@code importmappe}, come nelle chiavi di
     *                    {@link Funzioni#AggiornamentoConfigDaRepositoryUnicaChiamata}
     * @param file        nome senza estensione
     * @param ricarica    rilegge il file e sostituisce i dati in memoria
     * @param descrizione per il messaggio di riepilogo
     */
    record Voce(String cartella, String file, Runnable ricarica, String descrizione) {}

    /** I file tenuti in memoria. */
    static final List<Voce> VOCI = List.of(
            new Voce("varie", AliasPrezziToken.NOME, AliasPrezziToken::Carica, "alias dei prezzi"),
            new Voce("varie", ListeSimboli.NOME, () -> {
                Prezzi.RicaricaSimboliPrioritari();
                ListeSimboli.RicaricaFiat();
            }, "liste di simboli"),
            new Voce("varie", StatiEsteri.NOME, StatiEsteri::Ricarica, "stati esteri"),
            new Voce("importmappe", DatiPredefinitiRW.NOME, Principale_GruppiWalletRW::RicaricaExchangeNoti, "exchange noti"));

    /** I file che si rileggono a ogni uso (nessuna cache): un file nuovo ha effetto da solo. Formato {@code cartella/nome}. */
    static final Set<String> LETTI_A_OGNI_USO = Set.of(
            "varie/" + NoteCompilazione.NOME,
            "varie/" + EmoneyToken.NOME,
            "importmappe/" + MappeCausali.BINANCE_OLD,
            "importmappe/" + MappeCausali.BINANCE_FINANCIAL_REPORT,
            "importmappe/" + MappeCausali.OKX,
            "importmappe/" + MappeCausali.CRYPTOCOM_APP,
            "importmappe/" + MappeCausali.CRYPTOCOM_EXCHANGE,
            "importmappe/" + MappeCausali.TATAX_OLD,
            "importmappe/" + TipiOKX.NOME);

    /**
     * Ricarica i file in memoria che compaiono fra quelli appena aggiornati. Un errore su una voce non ferma le altre.
     *
     * @param aggiornati cartella → file aggiornati (con estensione), come li rende l'aggiornamento
     * @return le descrizioni delle voci ricaricate
     */
    static List<String> Ricarica(Map<String, List<String>> aggiornati) {
        return Ricarica(aggiornati, VOCI);
    }

    static List<String> Ricarica(Map<String, List<String>> aggiornati, List<Voce> voci) {
        List<String> fatte = new ArrayList<>();
        if (aggiornati == null) return fatte;
        for (Voce v : voci) {
            List<String> file = aggiornati.get(v.cartella());
            if (file == null || !file.contains(v.file() + ".json")) continue;
            try {
                v.ricarica().run();
                fatte.add(v.descrizione());
                System.out.println("AggiornamentoConfig: " + v.descrizione() + " ricaricati");
            } catch (RuntimeException e) {
                LoggerGC.ScriviErrore("AggiornamentoConfig: ricarica di " + v.cartella() + "/" + v.file() + " non riuscita : " + e);
            }
        }
        return fatte;
    }
}
