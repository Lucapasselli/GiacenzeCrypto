package com.giacenzecrypto.giacenze_crypto;

import java.time.LocalDate;
import java.util.List;

/**
 * Una riga di {@code Normativa/fonti.csv}, letta da {@link Principale_Normativa#LeggiCatalogo()}.
 *
 * @param percorso path relativo del file sotto {@code Normativa/} (es.
 *        {@code "Prassi_AgenziaEntrate/2023-10-27_Circolare_30E_trattamento_fiscale_cripto-attivita.pdf"})
 * @param tipo {@code "ufficiale"}, {@code "derivato"} o {@code "SCONOSCIUTO"} (vedi {@code genera_fonti.py})
 * @param titolo titolo per esteso del documento
 * @param autorita chi lo pubblica
 * @param identificativo numero e data ufficiali, o nota di non ufficialità per i derivati
 * @param dataDocumento data del documento, quando ricostruibile; {@code null} altrimenti (vedi
 *        {@code Normativa/Strumenti/genera_fonti.py} per come viene derivata caso per caso)
 * @param argomenti etichette tematiche, classificazione e non interpretazione del testo normativo
 * @param url indirizzo di provenienza (ufficiali) o dei file di partenza (derivati)
 * @param sha256 impronta del file, usata come chiave di validità della cache di estrazione testo
 * @param categoria etichetta leggibile della cartella di primo livello, per il filtro categoria
 */
public record DocumentoNormativa(
        String percorso,
        String tipo,
        String titolo,
        String autorita,
        String identificativo,
        LocalDate dataDocumento,
        List<String> argomenti,
        String url,
        String sha256,
        String categoria) {

    /** @return {@code true} se il documento è un testo ufficiale (non un estratto/consolidato derivato) */
    public boolean ufficiale() {
        return "ufficiale".equals(tipo);
    }
}
