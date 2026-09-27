#!/bin/sh
# Rigenera i PDF di docs/documentazione/ a partire dalle pagine Markdown.
#
# I PDF non sono la fonte: la fonte è il Markdown. Restano pubblicati perché le versioni del
# programma precedenti alla 1.0.62 aprono i manuali all'indirizzo .../documentazione/<nome>.pdf,
# e quei collegamenti non devono diventare 404 (né mostrare testo vecchio).
#
# Dal 2026-09-27 i PDF sono prodotti dal programma stesso (GeneraPdfDocumentazione), con la stessa
# veste grafica delle stampe dei quadri W/RW: copertina, testata col logo, fascia nel margine,
# piè di pagina numerato, Noto Sans incorporato. Serve solo il JDK; LibreOffice non serve più.
# Da eseguire dalla radice del repository:
#   sh docs/strumenti/genera-pdf.sh
set -e
RADICE=$(cd "$(dirname "$0")/../.." && pwd)
cd "$RADICE"
./mvnw -q compile
CP_FILE="$RADICE/target/classpath-documentazione.txt"
./mvnw -q dependency:build-classpath -Dmdep.outputFile="$CP_FILE" >/dev/null
java -Djava.awt.headless=true -cp "target/classes:$(cat "$CP_FILE")" \
    com.giacenzecrypto.giacenze_crypto.GeneraPdfDocumentazione docs/documentazione
echo "PDF rigenerati in $RADICE/docs/documentazione"
