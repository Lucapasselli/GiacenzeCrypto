"""Converte in HTML i .md della documentazione, per generare i PDF con LibreOffice.

Due cose che LibreOffice fa e che il CSS non governa, e che quindi sono risolte qui:

- **L'importazione HTML ignora `max-width` sulle immagini** e le inserisce alla dimensione
  nominale (96 dpi): uno screenshot da 2260 px diventa largo 60 cm su una pagina di 21 e
  finisce fuori dal margine, tagliato. Le dimensioni vanno scritte negli **attributi**
  `width`/`height` del tag, ed è per questo che qui si legge la dimensione reale del file
  con PIL e la si riduce a DPI_IMMAGINI, con un tetto di LARGH_MAX_CM x ALT_MAX_CM.
- **Un collegamento interno ha bisogno di `<a name="...">`**, non del solo `id=`: gli
  identificatori espliciti `{#ancora}` delle intestazioni servono ai collegamenti della
  pagina web, e finché venivano soltanto scartati i link dell'indice iniziale non avevano
  destinazione nel PDF. Qui si emettono entrambi.
"""
import sys, re, html, os

#tetti pensati per A4 con margini di 2 cm (area di testo 17 x 25,7 cm)
LARGH_MAX_CM = 16.0
ALT_MAX_CM = 20.0
#gli screenshot sono catturati su schermi ad alta densità: a 96 dpi risulterebbero enormi
DPI_IMMAGINI = 150.0

BASE = "."      # cartella del .md, per risolvere i percorsi relativi delle immagini

def dimensioni_img(percorso):
    """Restituisce (larghezza, altezza) in pixel da scrivere negli attributi del tag."""
    try:
        from PIL import Image
        with Image.open(os.path.join(BASE, percorso)) as im:
            px_l, px_a = im.size
    except Exception as e:
        print("Immagine non misurabile: %s (%s)" % (percorso, e), file=sys.stderr)
        return None
    l_cm = px_l * 2.54 / DPI_IMMAGINI
    a_cm = px_a * 2.54 / DPI_IMMAGINI
    fattore = min(1.0, LARGH_MAX_CM / l_cm, ALT_MAX_CM / a_cm)
    l_cm *= fattore
    a_cm *= fattore
    return (int(round(l_cm / 2.54 * 96)), int(round(a_cm / 2.54 * 96)))

def tag_img(m):
    alt, src = m.group(1), m.group(2)
    dim = dimensioni_img(src)
    misure = ' width="%d" height="%d"' % dim if dim else ""
    return '<img src="%s" alt="%s"%s/>' % (src, alt, misure)

def inline(s):
    s = html.escape(s, quote=False)
    s = re.sub(r"!\[([^\]]*)\]\(([^)]+)\)", tag_img, s)
    s = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r'<a href="\2">\1</a>', s)
    s = re.sub(r"`([^`]+)`", r"<code>\1</code>", s)
    s = re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", s)
    s = re.sub(r"(?<![\w*])\*([^*\n]+)\*(?![\w*])", r"<i>\1</i>", s)
    return s

def converti(md):
    md = re.sub(r"^---\n.*?\n---\n", "", md, flags=re.S)   # front matter
    out, i = [], 0
    righe = md.split("\n")
    lista = []       # stack di ("ul"/"ol", indent)
    def chiudi_liste(fino=-1):
        while lista and lista[-1][1] > fino:
            out.append("</%s>" % lista.pop()[0])
    while i < len(righe):
        r = righe[i]
        s = r.strip()
        if s.startswith("```"):
            chiudi_liste(); i += 1
            corpo = []
            while i < len(righe) and not righe[i].strip().startswith("```"):
                corpo.append(html.escape(righe[i])); i += 1
            i += 1
            out.append("<pre>" + "\n".join(corpo) + "</pre>")
            continue
        if s.startswith("|") and i + 1 < len(righe) and re.match(r"^\|[\s:|-]+\|$", righe[i+1].strip()):
            chiudi_liste()
            def celle(x): return [c.strip() for c in x.strip().strip("|").split("|")]
            #width="100%" è un attributo, non CSS: senza, una tabella di 12 colonne esce dal margine
            out.append('<table border="1" cellspacing="0" cellpadding="3" width="100%"><tr>' +
                       "".join("<th>%s</th>" % inline(c) for c in celle(r)) + "</tr>")
            i += 2
            while i < len(righe) and righe[i].strip().startswith("|"):
                out.append("<tr>" + "".join("<td>%s</td>" % inline(c) for c in celle(righe[i])) + "</tr>")
                i += 1
            out.append("</table>")
            continue
        m = re.match(r"^(#{1,6})\s+(.*)$", s)
        if m:
            chiudi_liste(); n = len(m.group(1))
            #gli identificatori espliciti "{#ancora}" servono ai collegamenti interni della pagina web:
            #qui diventano un segnalibro, altrimenti i link dell'indice iniziale non sono cliccabili
            testo = m.group(2)
            anc = re.search(r"\{#([^}]*)\}\s*$", testo)
            titolo = re.sub(r"\s*\{#[^}]*\}\s*$", "", testo)
            segnalibro = '<a name="%s"></a>' % html.escape(anc.group(1), quote=True) if anc else ""
            attr = ' id="%s"' % html.escape(anc.group(1), quote=True) if anc else ""
            out.append("<h%d%s>%s%s</h%d>" % (n, attr, segnalibro, inline(titolo), n)); i += 1; continue
        if s.startswith("> "):
            chiudi_liste()
            blocco = []
            while i < len(righe) and righe[i].strip().startswith(">"):
                blocco.append(righe[i].strip().lstrip(">").strip()); i += 1
            out.append("<p><i>%s</i></p>" % inline(" ".join(blocco))); continue
        m = re.match(r"^(\s*)([-*]|\d+\.)\s+(.*)$", r)
        if m:
            ind = len(m.group(1)); tipo = "ol" if m.group(2)[0].isdigit() else "ul"
            testo = [m.group(3)]; i += 1
            while i < len(righe) and righe[i].strip() and not re.match(r"^\s*([-*]|\d+\.)\s+", righe[i]) \
                    and not righe[i].strip().startswith(("#", "|", ">", "```")):
                testo.append(righe[i].strip()); i += 1
            while lista and lista[-1][1] > ind: out.append("</%s>" % lista.pop()[0])
            if not lista or lista[-1][1] < ind:
                lista.append((tipo, ind)); out.append("<%s>" % tipo)
            elif lista[-1][0] != tipo:
                out.append("</%s>" % lista.pop()[0]); lista.append((tipo, ind)); out.append("<%s>" % tipo)
            out.append("<li>%s</li>" % inline(" ".join(testo)))
            continue
        if s in ("---", "***", "___"):
            chiudi_liste(); out.append("<hr/>"); i += 1; continue
        if s.startswith("[Torna all'indice"):
            i += 1; continue
        if not s:
            i += 1; continue
        chiudi_liste()
        par = [s]; i += 1
        while i < len(righe) and righe[i].strip() and not re.match(r"^\s*([-*]|\d+\.)\s+", righe[i]) \
                and not righe[i].strip().startswith(("#", "|", ">", "```")):
            par.append(righe[i].strip()); i += 1
        testo = " ".join(par)
        if re.match(r"^!\[[^\]]*\]\([^)]+\)$", testo):
            out.append('<p align="center">%s</p>' % inline(testo))
        else:
            out.append("<p>%s</p>" % inline(testo))
    chiudi_liste()
    return "\n".join(out)

#le regole per elemento sono necessarie: LibreOffice non fa ereditare il font di "body" alle
#intestazioni e alle celle, che altrimenti tornano al Liberation Serif del suo stile predefinito
STILE = ("@page{size:21cm 29.7cm;margin:2cm;}"
         "body{font-family:'Noto Sans',sans-serif;font-size:10pt;}"
         "p,li,td,th{font-family:'Noto Sans',sans-serif;}"
         "h1,h2,h3,h4,h5,h6{font-family:'Noto Sans',sans-serif;color:#1a3b5c;}"
         "h1{font-size:18pt;}h2{font-size:14pt;}h3{font-size:12pt;}h4{font-size:11pt;}"
         "table{font-size:8pt;}th{background:#e8eef4;}"
         "code{font-family:monospace;font-size:9pt;}"
         "pre{font-family:monospace;font-size:8pt;background:#f4f4f4;}")

if __name__ == "__main__":
    sorgente = sys.argv[1]
    BASE = os.path.dirname(os.path.abspath(sorgente))
    titolo = os.path.basename(sorgente)
    md = open(sorgente, encoding="utf-8").read()
    m = re.search(r"^title:\s*(.+)$", md, flags=re.M)
    if m: titolo = m.group(1).strip()
    print("<html><head><meta charset='utf-8'/><title>%s</title><style>%s</style></head><body>%s</body></html>"
          % (html.escape(titolo), STILE, converti(md)))
