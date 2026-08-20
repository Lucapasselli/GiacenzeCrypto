#!/bin/sh
# Rigenera i PDF di docs/documentazione/ a partire dalle pagine Markdown.
#
# I PDF non sono la fonte: la fonte è il Markdown. Restano pubblicati perché le versioni del
# programma precedenti alla 1.0.62 aprono i manuali all'indirizzo .../documentazione/<nome>.pdf,
# e quei collegamenti non devono diventare 404 (né mostrare testo vecchio).
#
# Serve LibreOffice (soffice), python3 e Pillow (python3-pillow): senza Pillow le immagini
# tornerebbero a essere inserite alla dimensione nominale, cioè enormi e fuori dal margine,
# ed è per questo che qui la sua assenza è un errore e non un avviso.
# Da eseguire dalla radice del repository:
#   sh docs/strumenti/genera-pdf.sh
set -e
python3 -c 'import PIL' 2>/dev/null || { echo "Manca Pillow: installare python3-pillow" >&2; exit 1; }
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
