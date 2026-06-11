/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.Window;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author luca.passelli
 */
public class F_Opzioni_Pulizie {

    public static boolean confermaECancellaWalletFIATeCARDPerIntervallo(
            String dataIniziale,
            String dataFinale,
            String TIPOWALLET,
            String FileDaPulire,
            Window win) {

        long timeStampIniziale = FunzioniDate.ConvertiDatainLong(dataIniziale);
        long timeStampFinale = FunzioniDate.ConvertiDatainLong(dataFinale) + 86400000;

        String messaggio = "Vuoi eliminare tutti i dati del Fiat Wallet nel periodo selezionato?";

        AppDialog.DialogResult result = AppDialog.builder(win)
                .windowTitle("Cancellazione "+TIPOWALLET)
                .bodyTitle("Eliminare i dati selezionati?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message(messaggio)
                .details("""
                Intervallo incluso: dal %s al %s.

                Verranno rimossi tutti i movimenti del %s compresi nel periodo selezionato.
                """.formatted(dataIniziale, dataFinale,TIPOWALLET))
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("delete-fiat-range", "Elimina dati")
                        .role(AppDialog.ActionRole.DANGER)
                        .build())
                .showDialog();

        if (result != null && result.isAction("delete-fiat-range")) {
            try {
                FileReader fire = new FileReader(FileDaPulire);
                BufferedReader bure = new BufferedReader(fire);
                String rigas;

                List<String> daMantenere = new ArrayList<>();

                while ((rigas = bure.readLine()) != null) {
                    long timeStampMovimento = FunzioniDate.ConvertiDatainLong(rigas.split(" ")[0]);

                    if (timeStampMovimento < timeStampIniziale || timeStampMovimento >= timeStampFinale) {
                        daMantenere.add(rigas);
                    }
                }

                bure.close();
                fire.close();

                FileWriter w = new FileWriter(FileDaPulire);
                BufferedWriter b = new BufferedWriter(w);

                Iterator<String> it = daMantenere.iterator();
                while (it.hasNext()) {
                    b.write(it.next() + "\n");
                }

                b.close();
                w.close();

            } catch (IOException ex) {

            }

            return true;
            /*CDC_FiatWallet_Mappa.clear();
            CDC_FiatWallet_Funzione_ImportaWallet(VarStatiche.getFile_CDCFiatWallet());
            CDC_FiatWallet_AggiornaDatisuGUI();

            Messaggi.SuccessMessage("Dati eliminati", "I dati del Fiat Wallet nel periodo selezionato sono stati eliminati correttamente.", win);*/
        }
        return false;
    }


    
    

}
