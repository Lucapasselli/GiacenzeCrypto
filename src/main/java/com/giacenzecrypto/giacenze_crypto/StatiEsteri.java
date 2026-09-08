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

    private static final String SEP = "  —  "; // " — "

    /** Denominazione del codice, o {@code ""} se non in tabella. */
    public static String nome(String codice) {
        if (codice == null) {
            return "";
        }
        String c = codice.trim();
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
