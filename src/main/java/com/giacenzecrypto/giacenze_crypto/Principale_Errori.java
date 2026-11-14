package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;

/**
 * Le voci del pulsante <b>Errori</b>: quali tipi di errore ci sono, con che conteggio, in che ordine e con che testo. Senza Swing:
 * il dialogo che le mostra è {@link GUI_Errori}, e la correzione di ciascuna sta in {@code Principale.Funzioni_CorrezioneErroriPrincipali}.
 *
 * <p>Un errore nuovo è <b>una voce in più</b> in {@link #Voci}, un contatore in {@link Contatori} e un ramo della correzione: il dialogo
 * non cambia (prima c'era un pulsante per tipo e un metodo con un parametro per contatore, e a sei tipi non entravano più).</p>
 *
 * <p>Regole del testo, come per gli altri avvisi del programma: dice <b>che cosa</b> è il problema e <b>che effetto</b> ha sui calcoli, mai
 * come trattarlo fiscalmente. Le voci con conteggio zero non si mostrano (si elencano a parte come "nessun problema su").</p>
 */
public final class Principale_Errori {

    private Principale_Errori() {
    }

    /** Gli identificativi delle voci: sono gli stessi che il dialogo restituisce e su cui si decide la correzione. */
    public static final String ID_NON_CLASSIFICATI = "nonClassificati";
    public static final String ID_SENZA_PREZZO = "PrezzoMancante";
    public static final String ID_LIFO_MANCANTE = "LifoMancante";
    public static final String ID_GIACENZE_NEGATIVE = "GiacenzeNegative";
    public static final String ID_MOVIMENTI_COLLEGATI = "MovimentiCollegati";
    public static final String ID_TRASFERIMENTI_SENZA_COLLEGAMENTO = "TrasferimentiSenzaCollegamento";

    /** Una voce da mostrare: un tipo di errore con il suo conteggio. */
    public record Voce(String id, String titolo, int quanti, String effetto, String descrizione) {
    }

    /**
     * I contatori dei tipi di errore al momento di aprire il dialogo.
     * @param plusPerWallet vero se le plusvalenze sono divise per gruppo wallet (cambia l'effetto dei trasferimenti senza collegamento)
     */
    public record Contatori(int nonClassificati, int senzaPrezzo, int lifoMancante, int giacenzeNegative, int movimentiCollegati,
            int trasferimentiSenzaCollegamento, boolean plusPerWallet) {

        /** Il numero sul pulsante: la somma di tutti i tipi. */
        public int totale() {
            return nonClassificati + senzaPrezzo + lifoMancante + giacenzeNegative + movimentiCollegati + trasferimentiSenzaCollegamento;
        }
    }

    /**
     * Le voci con almeno un errore, nell'ordine in cui conviene correggerle: prima ciò che cambia la forma dei dati (classificare, collegare),
     * poi i valori (prezzi, costi di carico), infine i controlli di coerenza.
     */
    public static List<Voce> Voci(Contatori c) {
        List<Voce> tutte = new ArrayList<>();
        tutte.add(new Voce(ID_NON_CLASSIFICATI, "Movimenti non classificati", c.nonClassificati(),
                "Depositi a costo zero, prelievi come cessioni",
                "Depositi e prelievi che non sono ancora stati classificati (trasferimento fra wallet, reward, scambio e così via). "
                + "Ai fini della plusvalenza i depositi non classificati sono considerati a costo di carico zero e i prelievi non classificati "
                + "come una cessione."));
        tutte.add(new Voce(ID_MOVIMENTI_COLLEGATI, "Movimenti collegati incoerenti", c.movimentiCollegati(),
                "Gruppi classificati non più coerenti",
                "Movimenti classificati insieme (trasferimenti, scambi differiti) che citano un movimento che non esiste più oppure che non li "
                + "cita a loro volta. Fra questi ci sono gli scambi differiti sovrascritti da una versione precedente, che il programma può "
                + "ricostruire."));
        tutte.add(new Voce(ID_TRASFERIMENTI_SENZA_COLLEGAMENTO, "Trasferimenti senza collegamento", c.trasferimentiSenzaCollegamento(),
                c.plusPerWallet() ? "Il costo di carico non passa fra i gruppi wallet" : "Nessuno finché le plusvalenze per wallet sono spente",
                "Prelievi o depositi classificati come trasferimento fra wallet ma senza il movimento della controparte collegato. "
                + (c.plusPerWallet()
                    ? "Con le plusvalenze divise per gruppo wallet il costo di carico non passa dal gruppo che invia a quello che riceve. "
                      + "Se i due wallet sono in gruppi diversi, la vendita del gruppo di arrivo può non trovare il lotto (parte del LIFO "
                      + "mancante) e il lotto resta nella pila del gruppo di partenza."
                    : "Le plusvalenze per wallet sono spente, quindi il calcolo non cambia. Il trasferimento però resta scollegato e se si "
                      + "attivano le plusvalenze per wallet il costo di carico non passerà da un gruppo all'altro.")));
        tutte.add(new Voce(ID_SENZA_PREZZO, "Transazioni senza prezzo", c.senzaPrezzo(),
                "Valore in euro mancante",
                "Movimenti per cui non è stato trovato il controvalore in euro. Senza prezzo il valore della transazione manca e i calcoli "
                + "che lo usano (costo di carico, corrispettivo, plusvalenza, valore a fine anno) ne risentono. I token già dichiarati SCAM non "
                + "si contano."));
        tutte.add(new Voce(ID_LIFO_MANCANTE, "Parte del LIFO mancante", c.lifoMancante(),
                "Costo di carico mancante, plusvalenza più alta",
                "Movimenti in uscita per cui la pila LIFO non contiene tutta la quantità ceduta. La parte mancante ha costo di carico zero, "
                + "quindi la plusvalenza risulta più alta."));
        tutte.add(new Voce(ID_GIACENZE_NEGATIVE, "Giacenze negative", c.giacenzeNegative(),
                "Un wallet ha speso più di quanto ricevuto",
                "Un wallet o sotto-wallet ha registrato, almeno una volta nello storico, una giacenza negativa: ne è uscita più moneta di quanta ne "
                + "sia entrata. Le cause tipiche sono un movimento in entrata mai importato oppure il rendimento di un comparto (Earn, Dual "
                + "Investment e simili) non ancora registrato come reward."));
        List<Voce> presenti = new ArrayList<>();
        for (Voce v : tutte) if (v.quanti() > 0) presenti.add(v);
        return presenti;
    }

    /** I titoli dei tipi di errore controllati e senza nessun problema, nello stesso ordine di {@link #Voci}. */
    public static List<String> TitoliSenzaProblemi(Contatori c) {
        List<String> ris = new ArrayList<>();
        int[] quanti = {c.nonClassificati(), c.movimentiCollegati(), c.trasferimentiSenzaCollegamento(), c.senzaPrezzo(), c.lifoMancante(),
            c.giacenzeNegative()};
        String[] titoli = {"movimenti non classificati", "movimenti collegati", "trasferimenti senza collegamento", "transazioni senza prezzo",
            "parte del LIFO mancante", "giacenze negative"};
        for (int i = 0; i < quanti.length; i++) if (quanti[i] == 0) ris.add(titoli[i]);
        return ris;
    }
}
