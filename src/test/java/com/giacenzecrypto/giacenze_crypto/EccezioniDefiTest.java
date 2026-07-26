package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.TransazioneDefi.ValoriToken;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link EccezioniDefi#IndirizziDaEliminare}: sulla rete Gnosis alcune piattaforme non
 * aggiornate movimentano ancora il vecchio contratto EURe insieme a quello nuovo, per cui l'explorer
 * registra due movimenti identici quando in realtà è transitato dal wallet solo il token nuovo; il
 * movimento sul contratto vecchio va quindi eliminato.
 */
class EccezioniDefiTest {

    private static final String EURE_VECCHIO = "0xcB444e90D8198415266c6a2724b7900fb12FC56E";
    private static final String EURE_NUOVO = "0x420CA0f9B9b604cE0fd9C18EF134C705e5Fa3430";

    /** La mappa dei token di una transazione è case-insensitive come in {@link TransazioneDefi}. */
    private static Map<String, ValoriToken> mappa(String[]... token) {
        //ValoriToken è una inner class non statica: serve un'istanza di TransazioneDefi per crearla
        TransazioneDefi trans = new TransazioneDefi();
        Map<String, ValoriToken> mappa = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String[] t : token) {
            ValoriToken v = trans.new ValoriToken();
            v.Moneta = t[0];
            v.MonetaAddress = t[1];
            v.Qta = t[2];
            v.Tipo = "Crypto";
            mappa.put(v.MonetaAddress, v);
        }
        return mappa;
    }

    @Test
    void eureDoppioInEntrata_eliminaIlContrattoVecchio() {
        List<String> daEliminare = EccezioniDefi.IndirizziDaEliminare("GNOSIS",
                mappa(new String[]{"EURe", EURE_VECCHIO, "150"},
                      new String[]{"EURe", EURE_NUOVO, "150"}));

        assertEquals(List.of(EURE_VECCHIO), daEliminare);
    }

    @Test
    void eureDoppioInUscita_eliminaIlContrattoVecchio() {
        //L'eccezione EURe è configurata su ENTRAMBE le direzioni
        List<String> daEliminare = EccezioniDefi.IndirizziDaEliminare("GNOSIS",
                mappa(new String[]{"EURe", EURE_VECCHIO, "-150"},
                      new String[]{"EURe", EURE_NUOVO, "-150"}));

        assertEquals(List.of(EURE_VECCHIO), daEliminare);
    }

    @Test
    void quantitaConZeriNonSignificativi_consideratePariEQuindiEliminate() {
        List<String> daEliminare = EccezioniDefi.IndirizziDaEliminare("GNOSIS",
                mappa(new String[]{"EURe", EURE_VECCHIO, "150.00"},
                      new String[]{"EURe", EURE_NUOVO, "1.5E+2"}));

        assertEquals(List.of(EURE_VECCHIO), daEliminare);
    }

    @Test
    void indirizziConMaiuscoleDiverse_riconosciutiUgualmente() {
        List<String> daEliminare = EccezioniDefi.IndirizziDaEliminare("gnosis",
                mappa(new String[]{"EURe", EURE_VECCHIO.toLowerCase(), "150"},
                      new String[]{"EURe", EURE_NUOVO.toLowerCase(), "150"}));

        assertEquals(List.of(EURE_VECCHIO.toLowerCase()), daEliminare);
    }

    @Test
    void quantitaDiverse_nessunaEliminazione() {
        //Se le quantità non coincidono i due movimenti sono reali e distinti
        List<String> daEliminare = EccezioniDefi.IndirizziDaEliminare("GNOSIS",
                mappa(new String[]{"EURe", EURE_VECCHIO, "150"},
                      new String[]{"EURe", EURE_NUOVO, "120"}));

        assertTrue(daEliminare.isEmpty());
    }

    @Test
    void segniOpposti_nessunaEliminazione() {
        //Uscita del vecchio ed entrata del nuovo è una vera conversione, non un doppione
        List<String> daEliminare = EccezioniDefi.IndirizziDaEliminare("GNOSIS",
                mappa(new String[]{"EURe", EURE_VECCHIO, "-150"},
                      new String[]{"EURe", EURE_NUOVO, "150"}));

        assertTrue(daEliminare.isEmpty());
    }

    @Test
    void soloContrattoVecchio_nessunaEliminazione() {
        List<String> daEliminare = EccezioniDefi.IndirizziDaEliminare("GNOSIS",
                mappa(new String[]{"EURe", EURE_VECCHIO, "150"}));

        assertTrue(daEliminare.isEmpty());
    }

    @Test
    void reteDiversa_nessunaEliminazione() {
        List<String> daEliminare = EccezioniDefi.IndirizziDaEliminare("ETH",
                mappa(new String[]{"EURe", EURE_VECCHIO, "150"},
                      new String[]{"EURe", EURE_NUOVO, "150"}));

        assertTrue(daEliminare.isEmpty());
    }

    @Test
    void reteNullaOMappaVuota_nessunaEliminazione() {
        assertTrue(EccezioniDefi.IndirizziDaEliminare(null, mappa(new String[]{"EURe", EURE_VECCHIO, "150"})).isEmpty());
        assertTrue(EccezioniDefi.IndirizziDaEliminare("GNOSIS", null).isEmpty());
        assertTrue(EccezioniDefi.IndirizziDaEliminare("GNOSIS", mappa()).isEmpty());
    }
}
