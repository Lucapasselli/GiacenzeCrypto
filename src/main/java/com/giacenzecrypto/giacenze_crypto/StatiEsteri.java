package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;

/**
 * Tabella dei codici degli Stati / territori esteri usata dal quadro W/RW e dall'ISEE
 * (codice a 3 cifre + denominazione). Fonte : <b>Tabella 10 "Elenco dei Paesi e dei Territori
 * esteri"</b> delle istruzioni per la compilazione del modello Redditi PF (fascicolo 1),
 * edizione 2026 — la stessa tabella richiamata nei campi "Stato estero" dei quadri RW/RT.
 *
 * <p>Serve a far scegliere lo Stato da un elenco invece di doverne indovinare il numero : chi
 * conosce lo Stato non deve fare una ricerca a parte. L'elenco è fisso (cambia di rado, con
 * l'aggiornamento annuale della modulistica) : per questo è cablato qui e non in un file.</p>
 */
public final class StatiEsteri {

    private StatiEsteri() {
    }

    /** {codice a 3 cifre, denominazione (grafia della Tabella 10)}. Ordine : per codice numerico non è garantito. */
    public static final String[][] ELENCO = {
        {"238", "ABU DHABI"}, {"002", "AFGHANISTAN"}, {"239", "AJMAN"},
        {"292", "ALAND ISOLE"}, {"087", "ALBANIA"}, {"003", "ALGERIA"},
        {"148", "AMERICAN SAMOA"}, {"004", "ANDORRA"}, {"133", "ANGOLA"},
        {"209", "ANGUILLA"}, {"180", "ANTARTIDE"}, {"197", "ANTIGUA E BARBUDA"},
        {"005", "ARABIA SAUDITA"}, {"006", "ARGENTINA"}, {"266", "ARMENIA"},
        {"212", "ARUBA"}, {"227", "ASCENSION"}, {"007", "AUSTRALIA"},
        {"008", "AUSTRIA"}, {"268", "AZERBAIGIAN"}, {"234", "AZZORRE ISOLE"},
        {"160", "BAHAMAS"}, {"169", "BAHRAIN"}, {"130", "BANGLADESH"},
        {"118", "BARBADOS"}, {"009", "BELGIO"}, {"198", "BELIZE"},
        {"158", "BENIN"}, {"207", "BERMUDA"}, {"097", "BHUTAN"},
        {"264", "BIELORUSSIA"}, {"010", "BOLIVIA"}, {"295", "BONAIRE SAINT EUSTATIUS AND SABA"},
        {"274", "BOSNIA-ERZEGOVINA"}, {"098", "BOTSWANA"}, {"280", "BOUVET ISLAND"},
        {"011", "BRASILE"}, {"125", "BRUNEI DARUSSALAM"}, {"012", "BULGARIA"},
        {"142", "BURKINA FASO"}, {"025", "BURUNDI"}, {"135", "CAMBOGIA"},
        {"119", "CAMERUN"}, {"139", "CAMPIONE D'ITALIA"}, {"013", "CANADA"},
        {"100", "CANARIE ISOLE"}, {"188", "CAPO VERDE"}, {"256", "CAROLINE ISOLE"},
        {"211", "CAYMAN (ISOLE)"}, {"275", "CECA (REPUBBLICA)"}, {"143", "CENTROAFRICANA (REPUBBLICA)"},
        {"246", "CEUTA"}, {"230", "CHAFARINAS"}, {"255", "CHAGOS ISOLE"},
        {"282", "CHRISTMAS ISLAND"}, {"144", "CIAD"}, {"015", "CILE"},
        {"016", "CINA"}, {"101", "CIPRO"}, {"093", "CITTÀ DEL VATICANO"},
        {"223", "CLIPPERTON"}, {"281", "COCOS (KEELING) ISLAND"}, {"017", "COLOMBIA"},
        {"176", "COMORE, ISOLE"}, {"145", "CONGO"}, {"018", "CONGO (REP. DEMOCRATICA DEL)"},
        {"237", "COOK ISOLE"}, {"074", "COREA DEL NORD"}, {"084", "COREA DEL SUD"},
        {"146", "COSTA D'AVORIO"}, {"019", "COSTA RICA"}, {"261", "CROAZIA"},
        {"020", "CUBA"}, {"296", "CURACAO"}, {"021", "DANIMARCA"},
        {"192", "DOMINICA"}, {"063", "DOMINICANA (REPUBBLICA)"}, {"240", "DUBAI"},
        {"024", "ECUADOR"}, {"023", "EGITTO"}, {"277", "ERITREA"},
        {"257", "ESTONIA"}, {"026", "ETIOPIA"}, {"204", "FAEROER (ISOLE)"},
        {"190", "FALKLAND (ISOLE)"}, {"161", "FIJI, ISOLE"}, {"027", "FILIPPINE"},
        {"028", "FINLANDIA"}, {"029", "FRANCIA"}, {"241", "FUIJAYRAH"},
        {"157", "GABON"}, {"164", "GAMBIA"}, {"267", "GEORGIA"},
        {"094", "GERMANIA"}, {"112", "GHANA"}, {"082", "GIAMAICA"},
        {"088", "GIAPPONE"}, {"102", "GIBILTERRA"}, {"113", "GIBUTI"},
        {"122", "GIORDANIA"}, {"228", "GOUGH"}, {"032", "GRECIA"},
        {"156", "GRENADA"}, {"200", "GROENLANDIA"}, {"214", "GUADALUPA"},
        {"154", "GUAM"}, {"033", "GUATEMALA"}, {"123", "GUAYANA FRANCESE"},
        {"201", "GUERNSEY"}, {"137", "GUINEA"}, {"185", "GUINEA BISSAU"},
        {"167", "GUINEA EQUATORIALE"}, {"159", "GUYANA"}, {"034", "HAITI"},
        {"284", "HEARD AND MCDONALD ISLAND"}, {"035", "HONDURAS"}, {"103", "HONG KONG"},
        {"114", "INDIA"}, {"129", "INDONESIA"}, {"039", "IRAN"},
        {"038", "IRAQ"}, {"040", "IRLANDA"}, {"041", "ISLANDA"},
        {"252", "ISOLE AMERICANE DEL PACIFICO"}, {"182", "ISRAELE"}, {"202", "JERSEY C.I."},
        {"269", "KAZAKISTAN"}, {"116", "KENYA"}, {"270", "KIRGHIZISTAN"},
        {"194", "KIRIBATI"}, {"291", "KOSOVO"}, {"126", "KUWAIT"},
        {"136", "LAOS"}, {"089", "LESOTHO"}, {"258", "LETTONIA"},
        {"095", "LIBANO"}, {"044", "LIBERIA"}, {"045", "LIBIA"},
        {"090", "LIECHTENSTEIN"}, {"259", "LITUANIA"}, {"092", "LUSSEMBURGO"},
        {"059", "MACAO"}, {"278", "MACEDONIA"}, {"104", "MADAGASCAR"},
        {"235", "MADEIRA"}, {"056", "MALAWI"}, {"106", "MALAYSIA"},
        {"127", "MALDIVE"}, {"149", "MALI"}, {"105", "MALTA"},
        {"203", "MAN ISOLA"}, {"219", "MARIANNE SETTENTRIONALI (ISOLE)"}, {"107", "MAROCCO"},
        {"217", "MARSHALL (ISOLE)"}, {"213", "MARTINICA"}, {"141", "MAURITANIA"},
        {"128", "MAURITIUS"}, {"226", "MAYOTTE"}, {"231", "MELILLA"},
        {"046", "MESSICO"}, {"215", "MICRONESIA (STATI FEDERATI DI)"}, {"177", "MIDWAY ISOLE"},
        {"265", "MOLDOVIA"}, {"110", "MONGOLIA"}, {"290", "MONTENEGRO"},
        {"208", "MONTSERRAT"}, {"134", "MOZAMBICO"}, {"083", "MYANMAR"},
        {"206", "NAMIBIA"}, {"109", "NAURU"}, {"115", "NEPAL"},
        {"047", "NICARAGUA"}, {"150", "NIGER"}, {"117", "NIGERIA"},
        {"205", "NIUE"}, {"285", "NORFOLK ISLAND"}, {"048", "NORVEGIA"},
        {"253", "NUOVA CALEDONIA"}, {"049", "NUOVA ZELANDA"}, {"163", "OMAN"},
        {"050", "PAESI BASSI"}, {"036", "PAKISTAN"}, {"216", "PALAU"},
        {"279", "PALESTINA (TERRITORI AUTONOMI DI)"}, {"051", "PANAMA"}, {"186", "PAPUA NUOVA GUINEA"},
        {"052", "PARAGUAY"}, {"232", "PENON DE ALHUCEMAS"}, {"233", "PENON DE VELEZ DE LA GOMERA"},
        {"053", "PERÙ"}, {"175", "PITCAIRN"}, {"225", "POLINESIA FRANCESE"},
        {"054", "POLONIA"}, {"055", "PORTOGALLO"}, {"220", "PORTORICO"},
        {"091", "PRINCIPATO DI MONACO"}, {"168", "QATAR"}, {"242", "RAS EL KAIMAH"},
        {"031", "REGNO UNITO"}, {"247", "REUNION"}, {"061", "ROMANIA"},
        {"151", "RUANDA"}, {"262", "RUSSIA (FEDERAZIONE DI)"}, {"166", "SAHARA OCCIDENTALE"},
        {"293", "SAINT BARTHELEMY"}, {"195", "SAINT KITTS E NEVIS"}, {"199", "SAINT LUCIA"},
        {"222", "SAINT MARTIN SETTENTRIONALE"}, {"248", "SAINT-PIERRE E MIQUELON"}, {"191", "SALOMONE ISOLE"},
        {"064", "SALVADOR"}, {"131", "SAMOA"}, {"037", "SAN MARINO"},
        {"187", "SAO TOME E PRINCIPE"}, {"152", "SENEGAL"}, {"289", "SERBIA"},
        {"189", "SEYCHELLES"}, {"243", "SHARJAH"}, {"153", "SIERRA LEONE"},
        {"147", "SINGAPORE"}, {"294", "SINT MAARTEN"}, {"065", "SIRIA"},
        {"276", "SLOVACCA REPUBBLICA"}, {"260", "SLOVENIA"}, {"066", "SOMALIA"},
        {"283", "SOUTH GEORGIA AND SOUTH SANDWICH"}, {"067", "SPAGNA"}, {"085", "SRI LANKA"},
        {"254", "ST. HELENA"}, {"196", "ST. VINCENTE E LE GRENADINE"}, {"069", "STATI UNITI D'AMERICA"},
        {"297", "SUD SUDAN"}, {"078", "SUDAFRICANA REPUBBLICA"}, {"070", "SUDAN"},
        {"124", "SURINAM"}, {"286", "SVALBARD AND JAN MAYEN ISLANDS"}, {"068", "SVEZIA"},
        {"071", "SVIZZERA"}, {"138", "SWAZILAND"}, {"272", "TAGIKISTAN"},
        {"022", "TAIWAN"}, {"057", "TANZANIA"}, {"183", "TERRITORI FRANCESI DEL SUD"},
        {"245", "TERRITORIO BRIT. OCEANO INDIANO"}, {"072", "THAILANDIA"}, {"287", "TIMOR EST"},
        {"155", "TOGO"}, {"236", "TOKELAU"}, {"162", "TONGA"},
        {"120", "TRINIDAD E TOBAGO"}, {"229", "TRISTAN DA CUNHA"}, {"075", "TUNISIA"},
        {"273", "TURKMENISTAN"}, {"210", "TURKS E CAICOS (ISOLE)"}, {"193", "TUVALU"},
        {"076", "TÜRKIYE"}, {"263", "UCRAINA"}, {"132", "UGANDA"},
        {"244", "UMM AL QAIWAIN"}, {"077", "UNGHERIA"}, {"080", "URUGUAY"},
        {"271", "UZBEKISTAN"}, {"121", "VANUATU"}, {"081", "VENEZUELA"},
        {"221", "VERGINI AMERICANE (ISOLE)"}, {"249", "VERGINI BRITANNICHE (ISOLE)"}, {"062", "VIETNAM"},
        {"178", "WAKE ISOLE"}, {"218", "WALLIS E FUTUNA"}, {"042", "YEMEN"},
        {"058", "ZAMBIA"}, {"073", "ZIMBABWE"},
    };

    /** Voce vuota in testa alla combo (nessuno Stato indicato). */
    public static final String VOCE_VUOTA = "(nessuno)";

    /**
     * Valore sentinella per "conto detenuto in Italia". L'Italia <b>non</b> ha un codice nella
     * Tabella 10 "Elenco Paesi e Territori esteri" (verificato sulle istruzioni Redditi PF 2026 :
     * 085 = Sri Lanka, 087 = Albania, nessun 086), perché il quadro RW monitora le sole attività
     * <i>estere</i>. Qui è una voce speciale del campo Stato estero : quando è selezionata, la parte
     * FIAT del quadro W/RW per quel periodo non va compilata — {@code Calcoli_RW_Fiat.generaRighiFiat}
     * non emette righi per i tratti che ricadono in un periodo con questo valore. È
     * l'ISO&#8209;3166 alpha&#8209;2 {@code "IT"} : due lettere, non confondibile con un codice a
     * 3 cifre della Tabella 10 e sta nella colonna {@code StatoEstero VARCHAR(3)} del DB.
     */
    public static final String CODICE_ITALIA = "IT";

    private static final String SEP = "  —  "; // " — "

    /**
     * Codici della Tabella 10 corrispondenti agli Stati e territori dell'<b>elenco del D.M. 4 maggio
     * 1999</b> (e successive modificazioni), quello richiamato dall'art. 19 comma 20-bis del D.L.
     * 201/2011 per l'IVAFE in misura maggiorata e dall'art. 5 comma 2 del D.L. 167/1990 per le
     * sanzioni sul monitoraggio. L'elenco è quello pubblicato in appendice al fascicolo 2 delle
     * istruzioni di Redditi PF, 55 voci.
     *
     * <p><b>Una sola lista basta per tutti gli anni in cui la maggiorazione si applica, e non è una
     * semplificazione.</b> L'unica variazione recente dell'elenco è l'uscita della <b>Svizzera</b> :
     * c'è nel modello Redditi PF 2024 (anno d'imposta 2023) e non c'è più dal modello 2025 (anno
     * 2024). La misura del 4 per mille decorre <b>dall'anno 2024</b>, cioè esattamente da quando la
     * Svizzera non è più nell'elenco : negli anni in cui la maggiorazione può essere dovuta l'elenco
     * è quello qui sotto. Una tabella per anno non aggiungerebbe nulla.</p>
     *
     * <p><b>Sono 58 codici per 55 voci pubblicate, e il conto torna.</b> Tre voci dell'elenco —
     * <i>Alderney</i>, <i>Sark</i> e <i>Antille Olandesi</i> — <b>non hanno un codice nella Tabella
     * 10</b> (verificato sull'elenco ufficiale in appendice al fascicolo 1), quindi non sono
     * selezionabili come Stato estero e non possono comparire su nessun rigo. In pratica Alderney e
     * Sark si dichiarano sotto Guernsey (201) e le Antille Olandesi sono state sciolte nel 2010
     * (Curaçao 296, Bonaire/Saint Eustatius/Saba 295, Aruba 212 — quest'ultima già nell'elenco per
     * conto proprio). L'insieme è quindi <b>completo rispetto a ogni codice che il programma può
     * memorizzare</b>. Restano 52 voci mappabili, e gli Emirati Arabi Uniti ne valgono 7 invece di
     * 1 : 52 - 1 + 7 = 58 codici.</p>
     *
     * <p>Gli <b>Emirati Arabi Uniti</b> valgono <b>sette</b> codici, non uno : la Tabella 10 li
     * elenca emirato per emirato (238 Abu Dhabi, 239 Ajman, 240 Dubai, 241 Fuijayrah — grafia della
     * Tabella, 242 Ras El Kaimah, 243 Sharjah, 244 Umm Al Qaiwain).</p>
     */
    private static final java.util.Set<String> PRIVILEGIATI = java.util.Set.of(
            "004", // ANDORRA
            "209", // ANGUILLA
            "197", // ANTIGUA E BARBUDA
            "212", // ARUBA
            "160", // BAHAMAS
            "169", // BAHRAIN
            "118", // BARBADOS
            "198", // BELIZE
            "207", // BERMUDA
            "125", // BRUNEI DARUSSALAM
            "019", // COSTA RICA
            "192", // DOMINICA
            "024", // ECUADOR
            "238", "239", "240", "241", "242", "243", "244", // EMIRATI ARABI UNITI (i sette emirati)
            "027", // FILIPPINE
            "102", // GIBILTERRA
            "113", // GIBUTI
            "156", // GRENADA
            "201", // GUERNSEY
            "103", // HONG KONG
            "203", // MAN ISOLA
            "211", // CAYMAN (ISOLE)
            "237", // COOK ISOLE
            "217", // MARSHALL (ISOLE)
            "249", // VERGINI BRITANNICHE (ISOLE)
            "202", // JERSEY C.I.
            "095", // LIBANO
            "044", // LIBERIA
            "090", // LIECHTENSTEIN
            "059", // MACAO
            "106", // MALAYSIA
            "127", // MALDIVE
            "128", // MAURITIUS
            "091", // PRINCIPATO DI MONACO
            "208", // MONTSERRAT
            "109", // NAURU
            "205", // NIUE
            "163", // OMAN
            "051", // PANAMA
            "225", // POLINESIA FRANCESE
            "195", // SAINT KITTS E NEVIS
            "199", // SAINT LUCIA
            "196", // ST. VINCENTE E LE GRENADINE
            "131", // SAMOA
            "189", // SEYCHELLES
            "147", // SINGAPORE
            "022", // TAIWAN
            "162", // TONGA
            "210", // TURKS E CAICOS (ISOLE)
            "193", // TUVALU
            "080", // URUGUAY
            "121"  // VANUATU
    );

    /**
     * {@code true} se il codice Stato estero è fra quelli dell'elenco del D.M. 4 maggio 1999
     * ({@link #PRIVILEGIATI}). Un codice vuoto o sconosciuto è {@code false} : non si presume la
     * fiscalità privilegiata di uno Stato che l'utente non ha indicato.
     */
    public static boolean isPrivilegiato(String codice) {
        return codice != null && PRIVILEGIATI.contains(codice.trim());
    }

    /** I codici dell'elenco del D.M. 4 maggio 1999, per i test e le diagnostiche. */
    public static java.util.Set<String> codiciPrivilegiati() {
        return PRIVILEGIATI;
    }

    /** {@code true} se il codice è il valore sentinella {@link #CODICE_ITALIA} ("conto in Italia"). */
    public static boolean isItalia(String codice) {
        return CODICE_ITALIA.equals(codice == null ? "" : codice.trim());
    }

    /** Denominazione del codice, o {@code ""} se non in tabella. */
    public static String nome(String codice) {
        if (codice == null) {
            return "";
        }
        String c = codice.trim();
        if (isItalia(c)) {
            return "conto in Italia (nessun rigo RW valuta)";
        }
        for (String[] r : ELENCO) {
            if (r[0].equals(c)) {
                return r[1];
            }
        }
        return "";
    }

    /** {@code "092  —  LUSSEMBURGO"} ; {@link #VOCE_VUOTA} se {@code codice} è vuoto ; {@code "<codice> (non in elenco)"} se sconosciuto. */
    public static String etichetta(String codice) {
        String c = codice == null ? "" : codice.trim();
        if (c.isEmpty()) {
            return VOCE_VUOTA;
        }
        String n = nome(c);
        return n.isEmpty() ? c + " (non in elenco)" : c + SEP + n;
    }

    /** Il codice a 3 cifre estratto da una etichetta della combo ; {@code ""} per {@link #VOCE_VUOTA}. */
    public static String codiceDaEtichetta(String etichetta) {
        if (etichetta == null) {
            return "";
        }
        String e = etichetta.trim();
        if (e.isEmpty() || VOCE_VUOTA.equals(e)) {
            return "";
        }
        int i = e.indexOf(SEP);
        String testa = (i >= 0 ? e.substring(0, i) : e).trim();
        // "092 (non in elenco)" -> "092"
        int sp = testa.indexOf(' ');
        return sp > 0 ? testa.substring(0, sp) : testa;
    }

    /**
     * Le voci per una {@code JComboBox} del campo "Stato estero", ordinate per denominazione, con la
     * {@link #VOCE_VUOTA} in testa. Se {@code codiceCorrente} non è in tabella (valore legacy digitato
     * a mano) viene aggiunto come ultima voce così che la selezione corrente sopravviva.
     */
    public static String[] etichetteCombo(String codiceCorrente) {
        List<String[]> ordinato = new ArrayList<>(List.of(ELENCO));
        ordinato.sort((a, b) -> a[1].compareToIgnoreCase(b[1]));
        List<String> voci = new ArrayList<>();
        voci.add(VOCE_VUOTA);
        voci.add(CODICE_ITALIA + SEP + nome(CODICE_ITALIA)); // voce speciale : conto in Italia
        for (String[] r : ordinato) {
            voci.add(r[0] + SEP + r[1]);
        }
        String c = codiceCorrente == null ? "" : codiceCorrente.trim();
        if (!c.isEmpty() && nome(c).isEmpty()) {
            voci.add(c + " (non in elenco)");
        }
        return voci.toArray(new String[0]);
    }
}
