/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.Window;
import javax.swing.JTable;

/**
 *
 * @author luca.passelli
 */
public class Messaggi {
    
    public static boolean ConfermaTokenSenzaPrezzo(String Moneta, String Qta,Window w) {
         AppDialog.DialogResult result = AppDialog.builder(w)
                        .windowTitle("Token senza prezzo")
                        .bodyTitle("Token senza prezzo")
                        .showTitleInBody(true)
                        .theme()
                        .type(AppDialog.DialogType.WARNING)
                        .message("Il token " + Moneta + " attualmente non ha un prezzo disponibile.")
                        .details("Vuoi assegnare il valore 0 ai " + Qta + " " + Moneta + "?")
                        .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                                .role(AppDialog.ActionRole.SECONDARY)
                                .build())
                        .action(AppDialog.DialogAction.builder("assign-zero", "Assegna zero")
                                .role(AppDialog.ActionRole.DANGER)
                                .build())
                        .showDialog();

                return result.isAction("assign-zero");
        
    }
    
        public static boolean ConfermaMovimentoSenzaPrezzo(Window w) {
         AppDialog.DialogResult result = AppDialog.builder(w)
                        .windowTitle("Movimento senza prezzo")
                        .bodyTitle("Movimento senza prezzo")
                        .showTitleInBody(true)
                        .theme()
                        .type(AppDialog.DialogType.WARNING)
                        .message("Il movimento attualmente non ha prezzo o è valorizzato a 0.00")
                        .details("Vuoi confermare il valore di € 0.00 al movimento?")
                        .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                                .role(AppDialog.ActionRole.SECONDARY)
                                .build())
                        .action(AppDialog.DialogAction.builder("assign-zero", "Assegna zero")
                                .role(AppDialog.ActionRole.DANGER)
                                .build())
                        .showDialog();

                return result.isAction("assign-zero");
        
    }
        
        public static void WarningMessage(String Titolo,String Dettaglio,Window w){
        AppDialog.builder(w)
                            .title(Titolo)
                            .showTitleInBody(true)
                            .theme()
                            .type(AppDialog.DialogType.WARNING)
                            .details(Dettaglio)
                            .primaryAction("ok", "OK")
                            .showDialog();
        }
        public static void WarningMessage(String Titolo,String Messaggio,String Dettaglio,Window w){
        AppDialog.builder(w)
                            .title(Titolo)
                            .showTitleInBody(true)
                            .theme()
                            .type(AppDialog.DialogType.WARNING)
                            .message(Messaggio)
                            .details(Dettaglio)
                            .primaryAction("ok", "OK")
                            .showDialog();
        }
        public static void InfoMessage(String Titolo,String Dettaglio,Window w){
        AppDialog.builder(w)
                            .title(Titolo)
                            .showTitleInBody(true)
                            .theme()
                            .type(AppDialog.DialogType.INFO)
                            .details(Dettaglio)
                            .primaryAction("ok", "OK")
                            .showDialog();
        }
        public static void InfoMessage(String Titolo,String Messaggio,String Dettaglio,Window w){
        AppDialog.builder(w)
                            .title(Titolo)
                            .showTitleInBody(true)
                            .theme()
                            .type(AppDialog.DialogType.INFO)
                            .message(Messaggio)
                            .details(Dettaglio)
                            .primaryAction("ok", "OK")
                            .showDialog();
        }
        public static void SuccessMessage(String Titolo,String Dettaglio,Window w){
        AppDialog.builder(w)
                            .title(Titolo)
                            .showTitleInBody(true)
                            .theme()
                            .type(AppDialog.DialogType.SUCCESS)
                            .details(Dettaglio)
                            .primaryAction("ok", "OK")
                            .showDialog();
        }
        public static void SuccessMessage(String Titolo,String Messaggio,String Dettaglio,Window w){
        AppDialog.builder(w)
                            .title(Titolo)
                            .showTitleInBody(true)
                            .theme()
                            .type(AppDialog.DialogType.SUCCESS)
                            .message(Messaggio)
                            .details(Dettaglio)
                            .primaryAction("ok", "OK")
                            .showDialog();
        }
    
}
