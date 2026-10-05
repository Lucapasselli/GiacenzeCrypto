package com.giacenzecrypto.giacenze_crypto;

import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Estensione applicata a <b>tutte</b> le classi di test (registrata per autodetection, vedi
 * {@code META-INF/services/org.junit.jupiter.api.extension.Extension} e {@code junit-platform.properties}):
 * prima e dopo ogni classe azzera le connessioni H2 statiche di {@link DatabaseH2} che sono rimaste chiuse.
 *
 * <p>Le classi che aprono un database temporaneo lo chiudono nel loro {@code @AfterAll}, ma i campi statici
 * restavano valorizzati con la connessione chiusa, e la cartella di lavoro restava quella della classe
 * precedente, già cancellata da JUnit. Una classe successiva che non apre un database proprio
 * ({@code Principale_Movimenti_SeparaUnisciTest}) trovava così {@code connectionPrezzi != null}: il controllo
 * {@code == null} di {@code Prezzi.CercaPrezzoPreciso} non scattava, la ricerca del prezzo falliva sulla
 * cache chiusa e passava alla rete, scaricava Node (circa 237 MB) dentro la cartella cancellata e la ricreava.
 * Nessuno la cancellava più: una cartella {@code /tmp/junit*} in più a ogni esecuzione della suite, fino a
 * riempire {@code /tmp} (che è in RAM) e far fallire i test di importazione con "Spazio esaurito sul device".
 *
 * <p>Con le connessioni a {@code null} quella classe si comporta come quando gira da sola: nessuna ricerca
 * di prezzi, nessun accesso alla rete. Le connessioni ancora aperte non si toccano.
 */
public class IsolamentoConnessioniH2 implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        AzzeraConnessioniChiuse();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        AzzeraConnessioniChiuse();
    }

    static void AzzeraConnessioniChiuse() {
        if (Chiusa(DatabaseH2.connection)) DatabaseH2.connection = null;
        if (Chiusa(DatabaseH2.connectionPersonale)) DatabaseH2.connectionPersonale = null;
        if (Chiusa(DatabaseH2.connectionPrezzi)) DatabaseH2.connectionPrezzi = null;
    }

    private static boolean Chiusa(Connection c) {
        if (c == null) return false;
        try {
            return c.isClosed();
        } catch (SQLException ex) {
            return true;
        }
    }
}
