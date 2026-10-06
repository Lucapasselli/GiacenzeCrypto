package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

/**
 * Le forme in cui un movimento dell'archivio può ricomparire in un file che si reimporta, e la ricerca del movimento da
 * sostituire con «sovrascrivi esistenti».
 *
 * <p>Un reimport riconosce i movimenti già presenti in due modi: senza «sovrascrivi» per chiave logica (istante al
 * secondo, exchange, monete e quantità: {@link Importazioni#F_buildKeyMovimento}), con «sovrascrivi» per ID. Tutti e due
 * falliscono quando il movimento in archivio non ha più la forma del file:</p>
 * <ul>
 * <li>un prelievo classificato come trasferimento ha la quantità ridotta della commissione generata, o aumentata della
 * reward ({@link GUI_ClassificazioneMovimento#PrelievoPrimaDellaClassificazione});</li>
 * <li>un movimento spostato con Trasla Orario o modificato a mano ha un altro istante e un altro ID, o un'altra quantità:
 * la forma del file è fra le sue versioni precedenti nello storico modifiche ({@link MovimentiStorico#Versioni}). Il
 * lignaggio che lega le versioni esiste dal 2026-09-16: un movimento modificato prima non ce l'ha e resta
 * irriconoscibile (sull'archivio 2026 dell'utente 555 movimenti {@code M}, nessuno con lignaggio);</li>
 * <li>con «sovrascrivi», un movimento importato da una versione precedente del programma può avere un ID in un altro
 * formato ({@code _1_1_RW} invece di {@code _001_001_RW}): l'ID non si trova, la chiave sì.</li>
 * </ul>
 *
 * <p>Senza «sovrascrivi» basta aggiungere le chiavi delle forme precedenti all'insieme dei già presenti
 * ({@link #FormePrecedenti}). Con «sovrascrivi» serve sapere <b>quale</b> movimento sostituire, e due righe identiche del
 * file (un ordine eseguito a pezzi) non devono prendersi lo stesso movimento: lo fa {@link Indice}, che dà ogni movimento
 * dell'archivio a una riga sola. Sovrascrivere sostituisce sempre tutto, anche lo spostamento o la correzione fatti a
 * mano: è la scelta dell'utente (2026-10-06).</p>
 */
final class FormeImportate {

    private FormeImportate() {
    }

    /**
     * Le forme che il movimento aveva prima di essere modificato nel programma: il prelievo prima della classificazione
     * del trasferimento e ogni versione precedente dello storico modifiche, buffer non salvato compreso. Non comprende
     * la forma attuale.
     * @param v un movimento dell'archivio
     * @return le forme, mai {@code null}
     */
    static List<String[]> FormePrecedenti(String[] v) {
        List<String[]> Forme = new ArrayList<>();
        String[] Prelievo = GUI_ClassificazioneMovimento.PrelievoPrimaDellaClassificazione(v);
        if (Prelievo != null) Forme.add(Prelievo);
        if (v != null && v.length > MovimentiStorico.CAMPO_LIGNAGGIO && !Funzioni.noData(v[MovimentiStorico.CAMPO_LIGNAGGIO])) {
            try {
                for (String[] Versione : MovimentiStorico.Versioni(v[MovimentiStorico.CAMPO_LIGNAGGIO])) {
                    if (!Funzioni.noData(Versione[2])) Forme.add(Importazioni.DeserializzaRiga(Versione[2]));
                }
            } catch (RuntimeException ex) {
                //Storico non leggibile (database chiuso): si riconosce il movimento solo nelle altre forme
                LoggerGC.ScriviErrore(ex);
            }
        }
        return Forme;
    }

    /**
     * Costruisce l'indice per un'importazione con «sovrascrivi».
     * @param RigheDelFile le righe che si stanno per scrivere, con l'ID definitivo
     */
    static Indice Indice(Collection<String[]> RigheDelFile) {
        return new Indice(RigheDelFile);
    }

    /**
     * I movimenti dell'archivio che una riga del file può sostituire quando il suo ID non c'è: per ID precedente (storico)
     * e per chiave logica di ogni forma. Si costruisce una volta per importazione, all'inizio, e ogni movimento va a una
     * riga sola. Restano fuori i movimenti generati ({@code AU}), che spariscono con l'annullamento della classificazione
     * del loro gruppo, e quelli che una riga del file ritrova già col proprio ID, che le righe senza ID non devono
     * portare via.
     */
    static final class Indice {

        private final Map<String, String[]> PerIDPrecedente = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private final Map<String, ArrayDeque<String[]>> PerChiave = new HashMap<>();
        private final Set<String[]> Usati = Collections.newSetFromMap(new IdentityHashMap<>());

        private Indice(Collection<String[]> RigheDelFile) {
            Set<String[]> Ritrovati = Collections.newSetFromMap(new IdentityHashMap<>());
            for (String[] R : RigheDelFile) {
                if (R == null || R[0] == null) continue;
                String[] v = MappaCryptoWallet.get(R[0]);
                if (v == null) {
                    String Rinominato = Importazioni.IDRinominatoScambioDifferito(R[0]);
                    if (Rinominato != null) v = MappaCryptoWallet.get(Rinominato);
                }
                if (v != null) Ritrovati.add(v);
            }
            for (String[] v : MappaCryptoWallet.values()) {
                if (v == null || "AU".equalsIgnoreCase(v[22]) || Ritrovati.contains(v)) continue;
                Aggiungi(Importazioni.F_buildKeyMovimento(v), v);
                for (String[] Forma : FormePrecedenti(v)) {
                    Aggiungi(Importazioni.F_buildKeyMovimento(Forma), v);
                    if (!Funzioni.noData(Forma[0]) && !Forma[0].equalsIgnoreCase(v[0])) PerIDPrecedente.putIfAbsent(Forma[0], v);
                }
            }
        }

        private void Aggiungi(String Chiave, String[] v) {
            ArrayDeque<String[]> Coda = PerChiave.computeIfAbsent(Chiave, k -> new ArrayDeque<>());
            //Più forme dello stesso movimento possono dare la stessa chiave
            if (Coda.peekLast() != v) Coda.addLast(v);
        }

        /**
         * Il movimento dell'archivio che la riga del file deve sostituire, da chiamare solo quando l'ID della riga non
         * c'è in archivio. Il movimento trovato non sarà più dato a un'altra riga.
         * @param Riga la riga del file
         * @return il movimento da sostituire, o {@code null} se la riga è un movimento nuovo
         */
        String[] Trova(String[] Riga) {
            String[] v = PerIDPrecedente.get(Riga[0]);
            if (Disponibile(v)) return Prendi(v);
            //Difesa: una riga più corta dei campi della chiave non ha una chiave
            if (Riga.length < Importazioni.ColonneTabella) return null;
            ArrayDeque<String[]> Coda = PerChiave.get(Importazioni.F_buildKeyMovimento(Riga));
            while (Coda != null && !Coda.isEmpty()) {
                v = Coda.pollFirst();
                if (Disponibile(v)) return Prendi(v);
            }
            return null;
        }

        /** Non ancora dato a una riga, e ancora in archivio con lo stesso ID (non sostituito né rinominato nel frattempo). */
        private boolean Disponibile(String[] v) {
            return v != null && !Usati.contains(v) && MappaCryptoWallet.get(v[0]) == v;
        }

        private String[] Prendi(String[] v) {
            Usati.add(v);
            return v;
        }
    }
}
