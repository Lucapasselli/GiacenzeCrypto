package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Le tolleranze dell'abbinamento automatico di depositi e prelievi ({@link ParametriAbbinamento}). */
class ParametriAbbinamentoTest {

    private static String blocco(String corpo) {
        return "{\"abbinamentoDepositiPrelievi\":{" + corpo + "}}";
    }

    @Test
    void ilFileDistribuitoHaLeTolleranzeCheEranoNelCodice() throws Exception {
        //Caratterizzazione: sono i valori che Principale usava prima dello spostamento (un'ora, 2%, quantita'/10000).
        //Cambiarli nel file distribuito cambia quali giroconti si abbinano da soli: aggiornare qui di proposito.
        String contenuto = Files.readString(Path.of("config/varie/" + ParametriAbbinamento.NOME + ".json"));
        ParametriAbbinamento.Parametri p = ParametriAbbinamento.Interpreta(contenuto, ParametriAbbinamento.NOME);
        assertNotNull(p);
        assertEquals(3600, p.finestraSecondi());
        assertEquals(0, new BigDecimal(2).compareTo(p.differenzaMassimaPercentuale()));
        assertEquals(0, new BigDecimal("0.0001").compareTo(p.tolleranzaInversa(BigDecimal.ONE)));
        assertEquals(0, new BigDecimal("0.0001").compareTo(p.tolleranzaInversa(new BigDecimal("-1"))), "e' sul valore assoluto");
        assertTrue(p.perExchange().isEmpty(), "nessuna eccezione di default");
        assertTrue(Arrays.asList(MappeCausali.FILE_VARIE).contains(ParametriAbbinamento.NOME));
        assertFalse(contenuto.contains("\"centralizzato\""), "un file centralizzato non ancora pubblicato verrebbe cancellato all'avvio");
    }

    @Test
    void unaEccezionePerExchangeAllargaLaFinestraSoloPerLui() {
        ParametriAbbinamento.Parametri p = ParametriAbbinamento.Interpreta(blocco(
                "\"finestraSecondi\":3600,\"differenzaMassimaPercentuale\":2,\"tolleranzaInversaDivisore\":10000,"
                + "\"perExchange\":[{\"exchange\":\"Nexo\",\"finestraSecondi\":3660}]"), "x");
        assertNotNull(p);
        assertEquals(3660, p.finestraPer("Nexo", "Binance"));
        assertEquals(3660, p.finestraPer("Binance", " NEXO "), "maiuscole e spazi non contano, vale uno dei due");
        assertEquals(3600, p.finestraPer("Binance", "OKX"));
        assertEquals(3600, p.finestraPer(null, null));
    }

    @Test
    void unaEccezionePiuStrettaDellaFinestraGeneraleNonLaRestringe() {
        ParametriAbbinamento.Parametri p = ParametriAbbinamento.Interpreta(blocco(
                "\"finestraSecondi\":3600,\"differenzaMassimaPercentuale\":2,\"tolleranzaInversaDivisore\":10000,"
                + "\"perExchange\":[{\"exchange\":\"Nexo\",\"finestraSecondi\":60}]"), "x");
        assertEquals(3600, p.finestraPer("Nexo", "Binance"));
    }

    @Test
    void valoriNonValidiNonSiInterpretano() {
        assertNull(ParametriAbbinamento.Interpreta(null, "x"));
        assertNull(ParametriAbbinamento.Interpreta("non json", "x"));
        assertNull(ParametriAbbinamento.Interpreta("{}", "x"));
        assertNull(ParametriAbbinamento.Interpreta(blocco("\"finestraSecondi\":0,\"differenzaMassimaPercentuale\":2,\"tolleranzaInversaDivisore\":10000"), "x"));
        assertNull(ParametriAbbinamento.Interpreta(blocco("\"finestraSecondi\":60,\"differenzaMassimaPercentuale\":-1,\"tolleranzaInversaDivisore\":10000"), "x"));
        assertNull(ParametriAbbinamento.Interpreta(blocco("\"finestraSecondi\":60,\"differenzaMassimaPercentuale\":2,\"tolleranzaInversaDivisore\":0"), "x"));
        assertNull(ParametriAbbinamento.Interpreta(blocco("\"finestraSecondi\":60,\"differenzaMassimaPercentuale\":2,\"tolleranzaInversaDivisore\":10000,"
                + "\"perExchange\":[{\"exchange\":\"\",\"finestraSecondi\":60}]"), "x"));
    }

    @Test
    void unDivisoreNonDecimaleNonLanciaEccezioni() {
        ParametriAbbinamento.Parametri p = ParametriAbbinamento.Interpreta(blocco(
                "\"finestraSecondi\":60,\"differenzaMassimaPercentuale\":2,\"tolleranzaInversaDivisore\":3"), "x");
        assertNotNull(p.tolleranzaInversa(BigDecimal.ONE));
    }
}
