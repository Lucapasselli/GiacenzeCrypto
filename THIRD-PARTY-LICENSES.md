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
| jdatepicker | 1.3.4 | Simplified BSD License |
| flatlaf / flatlaf-extras | 3.5.4 / 3.6 | Apache License 2.0 |
| gson (Google) | 2.12.1 | Apache License 2.0 |
| jcalendar (Toedter) | 1.4 | GNU Lesser General Public License 2.1 |
| json (org.json / JSON-java) | 20250107 | Public Domain |
| openpdf (LibrePDF) | 2.0.3 | GNU Lesser General Public License 2.1 |
| fastexcel (dhatim) | 0.18.4 | Apache License 2.0 |
| H2 Database | 2.2.224 | Mozilla Public License 2.0 (opzione scelta tra MPL 2.0 / EPL 1.0 dual-license) |
| bitcoinj-core | 0.16.5 | Apache License 2.0 |
| jsoup | 1.13.1 | MIT License |

`org.junit.jupiter:junit-jupiter` (Eclipse Public License 2.0) è usato solo in
fase di test (`scope=test`) e non viene distribuito nel jar finale.

## Dipendenze transitive incluse nel jar-with-dependencies

| Libreria | Versione | Trascinata da | Licenza |
|---|---|---|---|
| okio (Square) | 2.8.0 | okhttp | Apache License 2.0 |
| kotlin-stdlib / kotlin-stdlib-common | 1.4.10 / 1.4.0 | okio | Apache License 2.0 |
| org.jetbrains:annotations | 13.0 | kotlin-stdlib | Apache License 2.0 |
| error_prone_annotations (Google) | 2.36.0 | gson | Apache License 2.0 |
| opczip (rzymek) | 1.2.0 | fastexcel | Apache License 2.0 |
| bcprov-jdk15to18 (Bouncy Castle) | 1.73 | bitcoinj-core | Bouncy Castle Licence (variante MIT-style) |
| guava (Google) | 31.0.1-android | bitcoinj-core | Apache License 2.0 |
| failureaccess / listenablefuture (Google) | 1.0.1 / — | guava | Apache License 2.0 |
| jsr305 (findbugs) | 3.0.2 | guava | Apache License 2.0 |
| checker-qual | 3.12.0 | guava | MIT License |
| checker-compat-qual | 2.5.5 | guava | GNU GPL v2 con Classpath Exception (non impone GPL a chi la usa) |
| j2objc-annotations (Google) | 1.3 | guava | Apache License 2.0 |
| protobuf-javalite (Google) | 3.18.0 | bitcoinj-core | BSD 3-Clause License |
| slf4j-api | 1.7.36 | bitcoinj-core | MIT License |
| jcip-annotations | 1.0 | bitcoinj-core | Creative Commons Attribution 2.5 |
| jsvg (weisj) | 1.4.0 | flatlaf-extras | MIT License |

## Componenti scaricati a runtime (non bundlati nel jar)

Al primo utilizzo delle funzioni CCXT, `CcxtInterop.java` scarica un binario
Node.js e installa il pacchetto npm `ccxt`, eseguendoli come processo esterno
separato (non linkati nel jar Java):

| Componente | Licenza |
|---|---|
| Node.js | MIT License |
| ccxt | MIT License |
