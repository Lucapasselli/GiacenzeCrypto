#!/bin/sh
# Rigenera i PDF di docs/documentazione/ a partire dalle pagine Markdown.
#
# I PDF non sono la fonte: la fonte è il Markdown. Restano pubblicati perché le versioni del
# programma precedenti alla 1.0.62 aprono i manuali all'indirizzo .../documentazione/<nome>.pdf,
# e quei collegamenti non devono diventare 404 (né mostrare testo vecchio).
#
# Serve LibreOffice (soffice) e python3. Da eseguire dalla radice del repository:
#   sh docs/strumenti/genera-pdf.sh
set -e
RADICE=$(cd "$(dirname "$0")/../.." && pwd)
DOC="$RADICE/docs/documentazione"
LAVORO=$(mktemp -d)
trap 'rm -rf "$LAVORO"' EXIT

for f in "$DOC"/*.md; do
    nome=$(basename "$f" .md)
    [ "$nome" = "index" ] && continue
    [ "$nome" = "changelog" ] && continue   # solo pagina web, non ha mai avuto un PDF
    python3 "$RADICE/docs/strumenti/md2html.py" "$f" > "$LAVORO/$nome.html"
done
cp -r "$DOC/immagini" "$LAVORO/"

# --convert-to va lanciato con la cartella di lavoro accanto alle immagini, i cui percorsi sono relativi
(cd "$LAVORO" && soffice --headless --convert-to pdf ./*.html >/dev/null)
cp "$LAVORO"/*.pdf "$DOC/"
echo "PDF rigenerati in $DOC"
