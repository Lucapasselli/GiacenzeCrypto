package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Riconoscimento del wallet della carta OKX su X Layer ({@link OKX_WalletCarta}) su un nodo simulato: un blocco al
 * secondo, trasferimenti di token e risposte di {@code entryPoint()}.
 */
class OKX_WalletCartaTest {

    private static final long T0 = 1_791_100_000L;          //timestamp del blocco 0
    private static final long ULTIMO = 20_000;
    private static final String USDG = OKX_WalletCarta.TOKEN_XLAYER.get("USDG").get(0);
    private static final String HOT = "0xca2e000000000000000000000000000000000001";
    private static final String CARTA = "0xf44e000000000000000000000000000000000002";
    private static final String ALTRO_AA = "0x9999000000000000000000000000000000000003";
    private static final String EOA = "0x1234000000000000000000000000000000000004";

    /** Nodo finto: blocco n al secondo T0 + n. */
    private static final class Nodo implements OKX_WalletCarta.Rpc {
        final List<String[]> logs = new ArrayList<>();   //blocco, token, da, a, valore esadecimale
        final Map<String, String> entryPoint = new HashMap<>();
        int chiamate;

        void trasferimento(long blocco, String token, String da, String a, String qta) {
            String valore = new BigDecimal(qta).movePointRight(6).toBigIntegerExact().toString(16);
            logs.add(new String[]{String.valueOf(blocco), token, da, a, "0x" + valore});
        }

        @Override
        public JsonElement chiama(String metodo, JsonArray p) {
            chiamate++;
            switch (metodo) {
                case "eth_blockNumber":
                    return new JsonPrimitive("0x" + Long.toHexString(ULTIMO));
                case "eth_getBlockByNumber": {
                    long n = Long.decode(p.get(0).getAsString());
                    JsonObject b = new JsonObject();
                    b.addProperty("timestamp", "0x" + Long.toHexString(T0 + n));
                    return b;
                }
                case "eth_getLogs": {
                    JsonObject f = p.get(0).getAsJsonObject();
                    long da = Long.decode(f.get("fromBlock").getAsString()), a = Long.decode(f.get("toBlock").getAsString());
                    assertTrue(a - da < OKX_WalletCarta.BLOCCHI_PER_RICHIESTA, "intervallo oltre il limite del nodo");
                    List<String> token = new ArrayList<>();
                    f.getAsJsonArray("address").forEach(e -> token.add(e.getAsString()));
                    JsonArray out = new JsonArray();
                    for (String[] l : logs) {
                        long b = Long.parseLong(l[0]);
                        if (b < da || b > a || !token.contains(l[1])) continue;
                        JsonObject log = new JsonObject();
                        JsonArray t = new JsonArray();
                        t.add(OKX_WalletCarta.TOPIC_TRANSFER);
                        t.add("0x000000000000000000000000" + l[2].substring(2));
                        t.add("0x000000000000000000000000" + l[3].substring(2));
                        log.add("topics", t);
                        log.addProperty("data", l[4]);
                        out.add(log);
                    }
                    return out;
                }
                case "eth_call": {
                    String to = p.get(0).getAsJsonObject().get("to").getAsString();
                    String ep = entryPoint.get(to);
                    if (ep == null) throw new RuntimeException("execution reverted");
                    return new JsonPrimitive("0x000000000000000000000000" + ep.substring(2));
                }
                default:
                    throw new IllegalArgumentException(metodo);
            }
        }
    }

    private static OKX_WalletCarta.Trasferimento bill(long blocco, String qta) {
        return new OKX_WalletCarta.Trasferimento((T0 + blocco) * 1000, "USDG", new BigDecimal(qta));
    }

    private static Nodo nodoConCarta() {
        Nodo n = new Nodo();
        n.entryPoint.put(CARTA, "0x0000000071727de22e5e9d8baf0edac6f37da032");
        n.entryPoint.put(ALTRO_AA, "0x0000000071727de22e5e9d8baf0edac6f37da032");
        return n;
    }

    /** Caso reale: il trasferimento arriva 34 secondi dopo il bill; la stessa cifra verso un non-wallet non conta. */
    @Test
    void trovaIlWalletDellaCartaDopoIlBill() {
        Nodo n = nodoConCarta();
        n.trasferimento(10_034, USDG, HOT, CARTA, "111.59");
        n.trasferimento(10_050, USDG, HOT, EOA, "111.59");             //stesso importo, ma non e' un wallet ERC-4337
        n.trasferimento(10_040, USDG, HOT, ALTRO_AA, "50");            //wallet ERC-4337, importo diverso
        var esito = OKX_WalletCarta.Trova(List.of(bill(10_000, "111.59")), n);
        assertEquals(CARTA, esito.indirizzo(), esito.note().toString());
        assertTrue(n.chiamate < 80, "richieste al nodo: " + n.chiamate);
    }

    /** Piu' bill: devono portare tutti allo stesso wallet, altrimenti non si sceglie. */
    @Test
    void piuBillDevonoPortareAlloStessoWallet() {
        Nodo n = nodoConCarta();
        n.trasferimento(10_034, USDG, HOT, CARTA, "111.59");
        n.trasferimento(15_020, USDG, HOT, CARTA, "20");
        assertEquals(CARTA, OKX_WalletCarta.Trova(List.of(bill(10_000, "111.59"), bill(15_000, "20")), n).indirizzo());

        Nodo diversi = nodoConCarta();
        diversi.trasferimento(10_034, USDG, HOT, CARTA, "111.59");
        diversi.trasferimento(15_020, USDG, HOT, ALTRO_AA, "20");
        var esito = OKX_WalletCarta.Trova(List.of(bill(10_000, "111.59"), bill(15_000, "20")), diversi);
        assertNull(esito.indirizzo());
        assertFalse(esito.note().isEmpty());
    }

    /** Nessun trasferimento, o due wallet ERC-4337 con lo stesso importo: nessun indirizzo, e una nota. */
    @Test
    void senzaUnCandidatoUnicoNonSiSceglie() {
        var vuoto = OKX_WalletCarta.Trova(List.of(bill(10_000, "111.59")), nodoConCarta());
        assertNull(vuoto.indirizzo());
        assertEquals(1, vuoto.note().size());

        Nodo ambiguo = nodoConCarta();
        ambiguo.trasferimento(10_034, USDG, HOT, CARTA, "111.59");
        ambiguo.trasferimento(10_100, USDG, HOT, ALTRO_AA, "111.59");
        assertNull(OKX_WalletCarta.Trova(List.of(bill(10_000, "111.59")), ambiguo).indirizzo());

        //Un trasferimento oltre la finestra (15 minuti) non e' quello del bill
        Nodo tardi = nodoConCarta();
        tardi.trasferimento(10_000 + OKX_WalletCarta.RITARDO_S + 60, USDG, HOT, CARTA, "111.59");
        assertNull(OKX_WalletCarta.Trova(List.of(bill(10_000, "111.59")), tardi).indirizzo());
    }

    /** Una moneta che la carta non usa viene ignorata con una nota. */
    @Test
    void unaMonetaNonPrevistaVieneIgnorata() {
        var esito = OKX_WalletCarta.Trova(List.of(new OKX_WalletCarta.Trasferimento(T0 * 1000, "BTC", BigDecimal.ONE)), nodoConCarta());
        assertNull(esito.indirizzo());
        assertTrue(esito.note().get(0).startsWith("BTC"));
    }

    /** Dai bill Funding si prendono solo i 325, con la quantita' uscita dall'exchange. */
    @Test
    void iTrasferimentiVersoLaCartaSonoIBill325() {
        JsonArray bills = JsonParser.parseString("""
                [ {"billId":"1","ccy":"USDG","balChg":"-111.59","bal":"0","notes":"Transfer from exchange to smart wallet","ts":"1791109002000","type":"325"},
                  {"billId":"2","ccy":"USDG","balChg":"111.59","bal":"111.59","notes":"Received from trading account","ts":"1791109002000","type":"130"} ]
                """).getAsJsonArray();
        var lista = OKX_WalletCarta.TrasferimentiVersoCarta(bills);
        assertEquals(1, lista.size());
        assertEquals("USDG", lista.get(0).moneta());
        assertEquals(0, lista.get(0).qta().compareTo(new BigDecimal("111.59")));
        assertEquals(1791109002000L, lista.get(0).ts());
    }

    /** Il blocco di un istante e' il primo con timestamp uguale o successivo. */
    @Test
    void ilBloccoDiUnIstante() throws Exception {
        Nodo n = new Nodo();
        assertEquals(12_345, OKX_WalletCarta.BloccoAlTempo(T0 + 12_345, n));
        assertEquals(ULTIMO, OKX_WalletCarta.BloccoAlTempo(T0 + ULTIMO + 500, n));
        assertEquals(0, OKX_WalletCarta.BloccoAlTempo(T0 - 10, n));
    }
}
