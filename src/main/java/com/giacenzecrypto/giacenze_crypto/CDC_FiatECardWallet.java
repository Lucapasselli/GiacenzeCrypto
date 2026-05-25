/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

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
        String[]   parti         = riga.split(",");
        long       longDataRiga  = FunzioniDate.ConvertiDatainLong(parti[0]);
        BigDecimal valoreRiga    = new BigDecimal(parti[1]);

        if (longDataIniziale > longDataRiga) {
            // La riga è *prima* del periodo: aggiorna l'ultimo valore noto
            ultimoValore   = valoreRiga;
            saldoInizialeT = parti[1];
        } else if (longDataIniziale <= longDataRiga && longDataRiga <= longDataFinale) {
            // Prima voce *dentro* il periodo: cristallizza il saldo iniziale
            if (!trovatoIniziale) {
                saldoInizialeT  = ultimoValore.toString();
                trovatoIniziale = true;
            }
            // ✅ Aggiorna ultimoValore anche DENTRO il periodo
            //  così alla fine del loop conterrà l'ultimo saldo vigente
            ultimoValore = valoreRiga;
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
    
    
    
}
