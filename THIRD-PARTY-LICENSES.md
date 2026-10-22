# Licenze di terze parti

Giacenze Crypto è distribuito sotto licenza MIT (vedi `LICENSE`). Il jar
eseguibile (`jar-with-dependencies`) include però anche il codice delle
librerie di terze parti elencate qui sotto, ciascuna con la propria licenza
originale, che resta invariata indipendentemente dalla licenza scelta per il
codice proprio dell'applicazione.

Elenco verificato sui metadati POM ufficiali in data 2026-07-24 (versione
1.0.58) — dettagli e metodo di verifica in
`Documentazione/Analisi_Compatibilita_GPL.md`.

## Dipendenze dirette

| Libreria | Versione | Licenza |
|---|---|---|
| okhttp (Square) | 4.9.3 | Apache License 2.0 |
| flatlaf / flatlaf-extras | 3.5.4 / 3.6 | Apache License 2.0 |
| gson (Google) | 2.12.1 | Apache License 2.0 |
| jcalendar (Toedter) | 1.4 | GNU Lesser General Public License 2.1 |
| json (org.json / JSON-java) | 20250107 | Public Domain |
| openpdf (LibrePDF) | 2.0.3 | GNU Lesser General Public License 2.1 |
| fastexcel (dhatim) | 0.18.4 | Apache License 2.0 |
| H2 Database | 2.2.224 | Mozilla Public License 2.0 (opzione scelta tra MPL 2.0 / EPL 1.0 dual-license) |
| jsoup | 1.13.1 | MIT License |

`org.junit.jupiter:junit-jupiter` (Eclipse Public License 2.0) è usato solo in
fase di test (`scope=test`) e non viene distribuito nel jar finale.
Lo stesso vale per `bcprov-jdk15to18` (Bouncy Castle Licence), che fino alla
versione 1.0.65 entrava nel jar insieme a bitcoinj-core: dal 2026-10-22
bitcoinj, guava, protobuf e le loro dipendenze non fanno più parte del
programma (gli indirizzi Bitcoin si ricavano con il solo JDK), e jdatepicker,
che non era usato, è stato tolto.

## Dipendenze transitive incluse nel jar-with-dependencies

| Libreria | Versione | Trascinata da | Licenza |
|---|---|---|---|
| okio (Square) | 2.8.0 | okhttp | Apache License 2.0 |
| kotlin-stdlib / kotlin-stdlib-common | 1.4.10 / 1.4.0 | okio | Apache License 2.0 |
| org.jetbrains:annotations | 13.0 | kotlin-stdlib | Apache License 2.0 |
| error_prone_annotations (Google) | 2.36.0 | gson | Apache License 2.0 |
| opczip (rzymek) | 1.2.0 | fastexcel | Apache License 2.0 |
| jsvg (weisj) | 1.4.0 | flatlaf-extras | MIT License |

## Componenti scaricati a runtime (non bundlati nel jar)

Al primo utilizzo delle funzioni CCXT, `CcxtInterop.java` scarica un binario
Node.js e installa il pacchetto npm `ccxt`, eseguendoli come processo esterno
separato (non linkati nel jar Java):

| Componente | Licenza |
|---|---|
| Node.js | MIT License |
| ccxt | MIT License |
