/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Elenco dei chatbot disponibili per la funzione "Chiedi a IA" e costruzione dell'URL con cui aprirli.
 *
 * <p>L'elenco &egrave; volutamente <b>dato e non codice</b>: sta nel file {@code ChatbotIA.json} della
 * directory di lavoro ({@link VarStatiche#getFile_ChatbotIA()}), creato con i valori predefiniti al primo
 * avvio. I parametri di prefill degli URL ({@code ?q=...}) non sono documentati dai fornitori e cambiano
 * senza preavviso: quando uno smette di funzionare basta correggere il JSON, senza ricompilare. Un chatbot
 * con {@code paramQuery} vuoto viene semplicemente aperto sulla sua pagina, con la domanda negli appunti.</p>
 *
 * @author luca.passelli
 */
public class ChatbotIA {

    /** Configurazione di un singolo chatbot. */
    public static class Bot {
        /** Nome mostrato nel menu e nel dialogo. */
        public String Nome;
        /** Indirizzo della chat da aprire nel browser. */
        public String Url;
        /** Nome del parametro di query che precompila la domanda, vuoto se il chatbot non lo supporta. */
        public String ParamQuery;
        /** Lunghezza massima dell'URL completo oltre la quale si ripiega sugli appunti. */
        public int LunghezzaMax;

        /**
         * @param Nome nome del chatbot
         * @param Url indirizzo della chat
         * @param ParamQuery parametro di query per il prefill, eventualmente vuoto
         * @param LunghezzaMax lunghezza massima dell'URL completo
         */
        public Bot(String Nome, String Url, String ParamQuery, int LunghezzaMax) {
            this.Nome = Nome;
            this.Url = Url;
            this.ParamQuery = ParamQuery;
            this.LunghezzaMax = LunghezzaMax;
        }

        /** @return {@code true} se il chatbot pu&ograve; ricevere la domanda direttamente nell'URL */
        public boolean SupportaPrefill() {
            return ParamQuery != null && !ParamQuery.isBlank();
        }

        /** @return il nome del chatbot, usato dalla combo del dialogo */
        @Override
        public String toString() {
            return Nome;
        }
    }

    /** Configurazione predefinita, scritta nel file alla prima esecuzione. */
    private static final String JSON_PREDEFINITO
            = "{\n"
            + "  \"_nota\": \"Elenco dei chatbot della funzione 'Chiedi a IA'. paramQuery e' il parametro che precompila la domanda nell'URL: non e' documentato dai fornitori e puo' cambiare senza preavviso. Lascialo vuoto per aprire solo la pagina della chat e incollare la domanda dagli appunti. lunghezzaMax e' la lunghezza massima dell'URL completo oltre la quale si passa comunque agli appunti.\",\n"
            + "  \"chatbot\": [\n"
            + "    { \"nome\": \"ChatGPT\",           \"url\": \"https://chatgpt.com/\",           \"paramQuery\": \"q\", \"lunghezzaMax\": 1800 },\n"
            + "    { \"nome\": \"Claude\",            \"url\": \"https://claude.ai/new\",          \"paramQuery\": \"q\", \"lunghezzaMax\": 1800 },\n"
            + "    { \"nome\": \"Perplexity\",        \"url\": \"https://www.perplexity.ai/search\",\"paramQuery\": \"q\", \"lunghezzaMax\": 1800 },\n"
            + "    { \"nome\": \"Copilot\",           \"url\": \"https://copilot.microsoft.com/\",  \"paramQuery\": \"q\", \"lunghezzaMax\": 1800 },\n"
            + "    { \"nome\": \"Grok\",              \"url\": \"https://grok.com/\",               \"paramQuery\": \"q\", \"lunghezzaMax\": 1800 },\n"
            + "    { \"nome\": \"Gemini\",            \"url\": \"https://gemini.google.com/app\",   \"paramQuery\": \"\",  \"lunghezzaMax\": 0 },\n"
            + "    { \"nome\": \"Le Chat (Mistral)\", \"url\": \"https://chat.mistral.ai/chat\",    \"paramQuery\": \"\",  \"lunghezzaMax\": 0 },\n"
            + "    { \"nome\": \"DeepSeek\",          \"url\": \"https://chat.deepseek.com/\",      \"paramQuery\": \"\",  \"lunghezzaMax\": 0 }\n"
            + "  ]\n"
            + "}\n";

    private static List<Bot> ListaCache = null;

    /**
     * Restituisce l'elenco dei chatbot configurati, leggendolo dal file alla prima chiamata e creandolo con
     * i valori predefiniti se assente. In caso di file illeggibile o malformato ripiega sui valori
     * predefiniti, in modo che la funzione resti utilizzabile.
     * @return l'elenco dei chatbot, mai vuoto
     */
    public static synchronized List<Bot> Lista() {
        if (ListaCache != null) return ListaCache;

        String contenuto = null;
        try {
            File f = new File(VarStatiche.getFile_ChatbotIA());
            if (!f.exists()) {
                if (f.getParentFile() != null) f.getParentFile().mkdirs();
                Files.write(Paths.get(f.getAbsolutePath()), JSON_PREDEFINITO.getBytes(StandardCharsets.UTF_8));
                System.out.println("ChatbotIA: creato il file di configurazione " + f.getAbsolutePath());
            }
            contenuto = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath())), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LoggerGC.ScriviErrore(e);
        }

        ListaCache = Interpreta(contenuto);
        if (ListaCache.isEmpty()) {
            System.out.println("ChatbotIA: configurazione assente o non valida, uso i valori predefiniti");
            ListaCache = Interpreta(JSON_PREDEFINITO);
        }
        return ListaCache;
    }

    /**
     * Interpreta il JSON di configurazione dei chatbot.
     * @param contenuto testo JSON, eventualmente {@code null}
     * @return l'elenco dei chatbot descritti, vuoto se il contenuto non &egrave; utilizzabile
     */
    private static List<Bot> Interpreta(String contenuto) {
        List<Bot> risultato = new ArrayList<>();
        if (contenuto == null || contenuto.isBlank()) return risultato;
        try {
            JSONArray bots = new JSONObject(contenuto).getJSONArray("chatbot");
            for (int i = 0; i < bots.length(); i++) {
                JSONObject b = bots.getJSONObject(i);
                String nome = b.optString("nome", "").trim();
                String url = b.optString("url", "").trim();
                if (nome.isEmpty() || url.isEmpty()) continue;
                risultato.add(new Bot(nome, url, b.optString("paramQuery", "").trim(), b.optInt("lunghezzaMax", 0)));
            }
        } catch (Exception e) {
            LoggerGC.ScriviErrore(e);
        }
        return risultato;
    }

    /**
     * Cerca un chatbot per nome.
     * @param Nome nome del chatbot da cercare
     * @return il chatbot con quel nome, oppure il primo dell'elenco se non trovato
     */
    public static Bot CercaPerNome(String Nome) {
        List<Bot> lista = Lista();
        if (Nome != null) {
            for (Bot b : lista) {
                if (b.Nome.equalsIgnoreCase(Nome)) return b;
            }
        }
        return lista.get(0);
    }

    /**
     * Costruisce l'URL che apre il chatbot con la domanda gi&agrave; inserita.
     * @param b chatbot di destinazione
     * @param Domanda testo della domanda
     * @return l'URL con la domanda in query string, oppure {@code null} se il chatbot non supporta il
     *         prefill o se l'URL risultante supererebbe la lunghezza massima consentita (nel qual caso la
     *         domanda va passata dagli appunti)
     */
    public static String UrlConDomanda(Bot b, String Domanda) {
        if (b == null || Domanda == null || !b.SupportaPrefill()) return null;
        String codificata = URLEncoder.encode(Domanda, StandardCharsets.UTF_8).replace("+", "%20");
        String url = b.Url + (b.Url.contains("?") ? "&" : "?") + b.ParamQuery + "=" + codificata;
        if (b.LunghezzaMax > 0 && url.length() > b.LunghezzaMax) return null;
        return url;
    }

    /**
     * Popola il sottomenu "Chiedi a IA" del menu contestuale con una voce per ogni chatbot configurato.
     * Il sottomenu viene creato vuoto dal Designer e riempito qui, cos&igrave; l'elenco resta modificabile
     * dal solo file JSON.
     * @param Menu il sottomenu da riempire
     * @param Proprietario finestra da usare come parent del dialogo
     */
    public static void PopolaMenu(JMenu Menu, Window Proprietario) {
        if (Menu == null) return;
        Menu.removeAll();
        for (Bot b : Lista()) {
            JMenuItem voce = new JMenuItem(b.Nome);
            voce.addActionListener(evt -> GUI_ChiediIA.Mostra(Principale.PopUp_IDTrans, b, Proprietario));
            Menu.add(voce);
        }
    }
}
