package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@link Principale_FiltriMovimenti#PeriodoCheComprendeTutto}: allarga il periodo, non lo restringe mai. */
class PeriodoCheComprendeTuttoTest {

    private static String[] mov(String data) {
        String[] m = new String[Importazioni.ColonneTabella];
        for (int i = 0; i < m.length; i++) m[i] = "";
        m[1] = data;
        return m;
    }

    @Test
    void movimentiDentroIlPeriodo_nonLoCambiano() {
        List<String[]> l = List.of(mov("2023-05-01 10:00"), mov("2025-12-31 23:59"));
        assertArrayEquals(new String[]{"2022-01-01", "2025-12-31"},
                Principale_FiltriMovimenti.PeriodoCheComprendeTutto(l, "2022-01-01", "2025-12-31"));
    }

    @Test
    void movimentiFuori_alzanoGliEstremi() {
        List<String[]> l = List.of(mov("2021-03-04 08:00"), mov("2023-05-01 10:00"), mov("2026-01-02 00:01"));
        assertArrayEquals(new String[]{"2021-03-04", "2026-01-02"},
                Principale_FiltriMovimenti.PeriodoCheComprendeTutto(l, "2022-01-01", "2025-12-31"));
    }

    @Test
    void dateIllegibiliEMovimentiVuoti_siIgnorano() {
        List<String[]> l = new ArrayList<>();
        l.add(mov(""));
        l.add(mov("non una data"));
        l.add(null);
        l.add(mov("2024-02-02 12:00"));
        assertArrayEquals(new String[]{"2022-01-01", "2025-12-31"},
                Principale_FiltriMovimenti.PeriodoCheComprendeTutto(l, "2022-01-01", "2025-12-31"));
    }
}
