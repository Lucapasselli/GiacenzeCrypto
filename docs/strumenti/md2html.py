"""Converte in HTML i .md della documentazione, per generare i PDF con LibreOffice."""
import sys, re, html, os

def inline(s):
    s = html.escape(s, quote=False)
    s = re.sub(r"!\[([^\]]*)\]\(([^)]+)\)", r'<img src="\2" alt="\1"/>', s)
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
            out.append('<table border="1" cellspacing="0" cellpadding="3"><tr>' +
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
            #gli identificatori espliciti "{#ancora}" servono ai collegamenti interni della pagina web
            titolo = re.sub(r"\s*\{#[^}]*\}\s*$", "", m.group(2))
            out.append("<h%d>%s</h%d>" % (n, inline(titolo), n)); i += 1; continue
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

if __name__ == "__main__":
    sorgente = sys.argv[1]
    titolo = os.path.basename(sorgente)
    md = open(sorgente, encoding="utf-8").read()
    m = re.search(r"^title:\s*(.+)$", md, flags=re.M)
    if m: titolo = m.group(1).strip()
    stile = ("body{font-family:'Noto Sans',sans-serif;font-size:10pt;}"
             "h1{font-size:18pt;}h2{font-size:14pt;}h3{font-size:12pt;}"
             "img{max-width:16cm;}table{font-size:8pt;}"
             "pre{font-family:monospace;font-size:8pt;background:#f4f4f4;}")
    print("<html><head><meta charset='utf-8'/><title>%s</title><style>%s</style></head><body>%s</body></html>"
          % (html.escape(titolo), stile, converti(md)))
