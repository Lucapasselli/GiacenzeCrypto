#!/usr/bin/env python3
"""Genera l'indice di ricerca client-side per la sezione Normative del sito.

Legge Normativa/fonti.csv, estrae il testo di ogni documento (pdftotext per i
PDF, strip dei tag per gli XML Akoma Ntoso, lettura diretta per i .md) e
produce due file JSON sotto Sito/normative/dati/:

- documenti.json  metadati per documento (titolo, autorita, date, url, ...),
  usati per mostrare i risultati.
- indice.json     indice invertito termine -> [[docId, peso], ...], usato per
  il filtro di ricerca. Non contiene il testo integrale: incorporare il testo
  grezzo di tutti i documenti nello stesso file (decine di MB una volta
  estratto dai PDF) sarebbe troppo pesante da scaricare a ogni visita della
  pagina, un indice invertito su un vocabolario di poche migliaia di termini
  e' ordini di grandezza piu' piccolo.
- testo/<id>.txt  testo integrale di ogni documento, un file per documento.
  Non scaricato in blocco: il client lo recupera al volo, solo per i
  documenti effettivamente mostrati in una pagina di risultati, per costruire
  gli estratti "trovato nel testo" con il termine cercato in contesto.
- leggibile/<id>.html  solo per i documenti XML (Akoma Ntoso): una pagina
  HTML con il testo reso leggibile (tag rimossi, un a capo per elemento, la
  stessa euristica "un elemento, una riga" usata dall'app desktop in
  Principale_Normativa.ApriCopia/EstraiXmlLeggibile — l'XML grezzo non e'
  fruibile da una persona comune). E' la pagina a cui punta "Apri copia
  archiviata" per un XML; il file originale resta comunque raggiungibile da
  un link secondario nella stessa pagina.

L'intera cartella Sito/normative/dati/ e' un artefatto di build (in
.gitignore, non versionato): va rigenerata a ogni pubblicazione del sito
dopo un aggiornamento di Normativa/, non e' incrementale.
"""
import csv
import html
import json
import re
import subprocess
import sys
import unicodedata
import xml.etree.ElementTree as ET
from pathlib import Path
from urllib.parse import quote

RADICE = Path(__file__).resolve().parent.parent.parent
NORMATIVA = RADICE / "Normativa"
FONTI_CSV = NORMATIVA / "fonti.csv"
OUT_DIR = RADICE / "Sito" / "normative" / "dati"
OUT_TESTO_DIR = OUT_DIR / "testo"
OUT_LEGGIBILE_DIR = OUT_DIR / "leggibile"

STOPWORD = set("""
il lo la i gli le un uno una di del dello della dei degli delle a al allo
alla ai agli alle da dal dallo dalla dai dagli dalle in nel nello nella nei
negli nelle con col coi su sul sullo sulla sui sugli sulle per tra fra e o
ma se che chi cui non si e' e' ne ci vi lui lei noi voi loro questo questa
questi queste quello quella quelli quelle come piu' anche gia' cosi' quando
dove essere sono e' sara' sono stati stato stata state come ogni ogni loro
articolo comma commi lettera lettere numero art
""".split())

TAG_RE = re.compile(r"<[^>]+>")
TOKEN_RE = re.compile(r"[a-zA-Zàèéìòù0-9]+")


def normalizza(testo: str) -> str:
    testo = unicodedata.normalize("NFKD", testo)
    testo = "".join(c for c in testo if not unicodedata.combining(c))
    return testo.lower()


def tokenizza(testo: str) -> list[str]:
    testo = normalizza(testo)
    out = []
    for tok in TOKEN_RE.findall(testo):
        if tok.isdigit() or (len(tok) >= 3 and tok not in STOPWORD):
            out.append(tok)
    return out


def estrai_testo(path: Path) -> str:
    suffisso = path.suffix.lower()
    if suffisso == ".pdf":
        try:
            r = subprocess.run(
                ["pdftotext", "-layout", str(path), "-"],
                capture_output=True, timeout=120,
            )
            return r.stdout.decode("utf-8", errors="ignore")
        except Exception as e:
            print(f"  ! estrazione PDF fallita per {path.name}: {e}", file=sys.stderr)
            return ""
    if suffisso == ".xml":
        grezzo = path.read_text(encoding="utf-8", errors="ignore")
        senza_tag = TAG_RE.sub(" ", grezzo)
        return html.unescape(senza_tag)
    if suffisso in (".md", ".txt"):
        return path.read_text(encoding="utf-8", errors="ignore")
    return ""


def _nome_locale(tag: str) -> str:
    return tag.split("}", 1)[-1] if "}" in tag else tag


def _pulisci_spazi(testo: str) -> str:
    testo = re.sub(r"[ \t]*\n[ \t]*", "\n", testo)
    testo = re.sub(r"\n{2,}", "\n", testo)
    testo = re.sub(r"[ \t]+", " ", testo)
    return testo.strip()


def _testo_diretto(el) -> str:
    return _pulisci_spazi(el.text or "") if el is not None else ""


def _testo_completo(el) -> str:
    return _pulisci_spazi("".join(el.itertext())) if el is not None else ""


def _figlio(el, nome):
    trovato = el.find(nome)
    return trovato if trovato is not None else el.find(f".//{nome}")


def _render_figli(el, escludi=()):
    return "".join(_render_nodo(f) for f in el if _nome_locale(f.tag) not in escludi)


def _render_nodo(el) -> str:
    tag = _nome_locale(el.tag)
    NS = "{http://docs.oasis-open.org/legaldocml/ns/akn/3.0}"

    if tag == "meta":
        # Storico delle modifiche e metadati FRBR/RDF: nessun testo utile a
        # una lettura corrente, e voluminoso (centinaia di KB su un testo
        # come il TUIR). Va escluso, non solo spostato in fondo.
        return ""

    if tag in ("akomaNtoso", "act", "bill", "doc"):
        return _render_figli(el)

    if tag == "preface":
        docType = _figlio(el, f"{NS}docType")
        docNumber = _figlio(el, f"{NS}docNumber")
        docDate = _figlio(el, f"{NS}docDate")
        docTitle = _figlio(el, f"{NS}docTitle")
        etichetta = " ".join(filter(None, [
            _testo_diretto(docType),
            _testo_diretto(docDate),
            f"n. {_testo_diretto(docNumber)}" if _testo_diretto(docNumber) else "",
        ]))
        titolo = _testo_completo(docTitle)
        out = '<div class="intestazione-atto">'
        if etichetta:
            out += f'<p class="tipo-atto">{escape_html(etichetta)}</p>'
        if titolo:
            out += f'<p class="titolo-atto">{escape_html(titolo)}</p>'
        out += "</div>"
        for nota in el.iter(f"{NS}authorialNote"):
            out += _render_nodo(nota)
        return out

    if tag == "preamble":
        return "".join(
            f'<p class="formula-preambolo">{escape_html(_testo_completo(f))}</p>'
            for f in el.iter(f"{NS}formula") if _testo_completo(f)
        )

    if tag == "body":
        return _render_figli(el)

    if tag in ("part", "book", "title", "chapter", "section", "subsection"):
        num = _testo_diretto(el.find(f"{NS}num"))
        heading = _testo_completo(el.find(f"{NS}heading"))
        titolo = " — ".join(filter(None, [num, heading]))
        out = f'<h2 class="titolo-struttura">{escape_html(titolo)}</h2>' if titolo else ""
        return out + _render_figli(el, escludi=("num", "heading"))

    if tag == "article":
        num = _testo_diretto(el.find(f"{NS}num"))
        heading = _testo_completo(el.find(f"{NS}heading"))
        titolo = " ".join(filter(None, [num, heading]))
        out = '<div class="articolo">'
        if titolo:
            out += f'<h3 class="titolo-articolo">{escape_html(titolo)}</h3>'
        out += _render_figli(el, escludi=("num", "heading"))
        out += "</div>"
        return out

    if tag in ("paragraph", "alinea"):
        num = _testo_diretto(el.find(f"{NS}num"))
        corpo = _render_figli(el, escludi=("num",))
        if not corpo.strip():
            return ""
        prefisso = f'<span class="numero-comma">{escape_html(num)}</span>' if num else ""
        return f'<div class="comma">{prefisso}{corpo}</div>'

    if tag == "point":
        num = _testo_diretto(el.find(f"{NS}num"))
        corpo = _render_figli(el, escludi=("num",))
        if not corpo.strip():
            return ""
        prefisso = f'<span class="numero-punto">{escape_html(num)}</span>' if num else ""
        return f'<div class="punto-elenco">{prefisso}{corpo}</div>'

    if tag == "intro":
        return _render_figli(el)

    if tag == "list":
        return f'<div class="elenco-normativo">{_render_figli(el)}</div>'

    if tag == "content":
        return _render_figli(el)

    if tag == "authorialNote":
        testo = _testo_completo(el)
        return f'<p class="nota-atto">{escape_html(testo)}</p>' if testo else ""

    if tag == "p":
        testo = _testo_completo(el)
        return f"<p>{escape_html(testo)}</p>" if testo else ""

    if tag in ("num", "heading"):
        return ""  # gestiti dal genitore; se raggiunti qui non c'e' niente da fare

    # Fallback per tag non censiti sopra (varianti non ancora viste): si
    # scende comunque nei figli, cosi' il testo non sparisce mai — solo la
    # formattazione speciale manca finche' non viene aggiunto un caso sopra.
    return _render_figli(el)


def xml_leggibile(grezzo: str) -> str:
    """Rende un Akoma Ntoso leggibile usando la struttura reale del
    documento (docType/docTitle nell'intestazione, capitoli e articoli come
    titoli, i commi come paragrafi numerati) invece di appiattire ogni
    elemento su una riga. Se il parsing fallisce (XML malformato o variante
    imprevista) ripiega sull'euristica "un elemento, una riga" dell'app
    desktop (EstraiXmlLeggibile), che non richiede capire lo schema.
    """
    try:
        radice = ET.fromstring(grezzo)
        html_reso = _render_nodo(radice)
        if html_reso.strip():
            return html_reso
    except ET.ParseError as e:
        print(f"  ! XML non valido, ripiego sul testo piatto: {e}", file=sys.stderr)

    con_a_capo = re.sub(r">\s*<", ">\n<", grezzo)
    senza_tag = TAG_RE.sub("", con_a_capo)
    testo = html.unescape(senza_tag)
    righe = [r.strip() for r in testo.split("\n")]
    righe = [r for r in righe if r]
    return "".join(f"<p>{escape_html(r)}</p>" for r in righe)


def escape_html(s: str) -> str:
    return (s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
             .replace('"', "&quot;").replace("'", "&#39;"))


def scrivi_pagina_leggibile(doc_id: int, meta: dict, html_leggibile: str, file_rel: str) -> None:
    link_archivio = "../../archivio/" + "/".join(quote(p) for p in Path(file_rel).parts)
    fonte = (
        f'<p><a href="{escape_html(meta["url"])}" target="_blank" rel="noopener">Apri fonte ufficiale ↗</a></p>'
        if meta.get("url") else ""
    )
    pagina = f"""<!DOCTYPE html>
<html lang="it">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{escape_html(meta['titolo'])} — Giacenze Crypto</title>
<link rel="icon" type="image/png" sizes="32x32" href="../../../assets/img/favicon-32.png">
<link rel="stylesheet" href="../../../assets/css/stile.css">
<style>
  .pagina-leggibile {{ max-width: 760px; margin: 0 auto; padding: 40px 24px 80px; }}
  .pagina-leggibile .torna {{ display: inline-block; margin-bottom: 20px; font-size: 0.9rem; }}
  .pagina-leggibile h1 {{ font-size: 1.5rem; margin: 0 0 10px; }}
  .pagina-leggibile .meta-doc {{ color: var(--testo-attenuato); font-size: 0.9rem; margin: 0 0 6px; }}
  .pagina-leggibile .fonti {{ display: flex; gap: 18px; margin: 16px 0 28px; font-size: 0.9rem; flex-wrap: wrap; }}
  .pagina-leggibile .avviso-conversione {{ background: var(--bg-elevata); border: 1px solid var(--bordo); border-left: 4px solid var(--accento); border-radius: var(--raggio); padding: 14px 18px; font-size: 0.85rem; color: var(--testo-attenuato); margin-bottom: 28px; }}
  .corpo-testo {{ font-size: 0.98rem; line-height: 1.7; }}
  .corpo-testo p {{ white-space: pre-line; margin: 0 0 14px; }}
  .intestazione-atto {{ margin-bottom: 32px; padding-bottom: 20px; border-bottom: 1px solid var(--bordo); }}
  .intestazione-atto .tipo-atto {{ margin: 0 0 6px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.03em; font-size: 0.85rem; color: var(--accento-forte); }}
  .intestazione-atto .titolo-atto {{ margin: 0; font-size: 1.1rem; color: var(--testo-attenuato); font-style: italic; }}
  .formula-preambolo {{ text-align: center; font-style: italic; color: var(--testo-attenuato); margin: 0 0 10px; }}
  .titolo-struttura {{ font-size: 1.25rem; color: var(--accento-forte); margin: 36px 0 16px; padding-bottom: 8px; border-bottom: 2px solid var(--bordo); }}
  .articolo {{ margin: 28px 0; padding-top: 20px; border-top: 1px solid var(--bordo); }}
  .articolo:first-child {{ border-top: none; padding-top: 0; }}
  .titolo-articolo {{ font-size: 1.05rem; color: var(--testo); margin: 0 0 14px; font-weight: 700; }}
  .comma {{ display: flex; gap: 10px; margin: 0 0 12px; align-items: baseline; }}
  .comma .numero-comma {{ flex: 0 0 auto; font-weight: 700; color: var(--accento-forte); font-size: 0.88rem; min-width: 1.6em; }}
  .comma > *:not(.numero-comma) {{ flex: 1 1 auto; min-width: 0; }}
  .comma p {{ margin: 0 0 8px; }}
  .comma p:last-child {{ margin-bottom: 0; }}
  .elenco-normativo {{ margin: 8px 0 12px 1.8em; }}
  .punto-elenco {{ display: flex; gap: 10px; margin: 0 0 8px; align-items: baseline; }}
  .punto-elenco .numero-punto {{ flex: 0 0 auto; font-weight: 700; color: var(--testo-attenuato); min-width: 1.6em; }}
  .punto-elenco p {{ margin: 0; }}
  .nota-atto {{ font-size: 0.85rem; color: var(--testo-attenuato); background: var(--bg-elevata); border-left: 3px solid var(--bordo); padding: 10px 14px; margin: 0 0 20px; }}
</style>
<script>try{{var t=localStorage.getItem("gc-tema");if(t==="chiaro"||t==="scuro")document.documentElement.setAttribute("data-tema",t);}}catch(e){{}}</script>
</head>
<body>
<div class="pagina-leggibile">
  <a class="torna" href="../../index.html">← Torna alla ricerca</a>
  <h1>{escape_html(meta['titolo'])}</h1>
  <p class="meta-doc">{escape_html(meta.get('autorita') or '')}</p>
  <p class="meta-doc">{escape_html(meta.get('identificativo') or '')}</p>
  <div class="fonti">
    {fonte}
    <p><a href="{escape_html(link_archivio)}" target="_blank" rel="noopener">Apri XML originale ↗</a></p>
  </div>
  <div class="avviso-conversione">Versione resa leggibile dal documento XML originale (struttura
  di articoli e commi ricostruita dai tag) — a scopo di consultazione, non sostituisce il testo
  ufficiale collegato sopra.</div>
  <div class="corpo-testo">{html_leggibile}</div>
</div>
<script src="../../../assets/js/tema.js"></script>
</body>
</html>
"""
    (OUT_LEGGIBILE_DIR / f"{doc_id}.html").write_text(pagina, encoding="utf-8")


def categoria(file_rel: str) -> str:
    parti = Path(file_rel).parts
    if parti[0] == "Leggi" and len(parti) > 1:
        return f"Leggi/{parti[1]}"
    return parti[0]


def estratto_breve(testo: str, max_car: int = 320) -> str:
    testo = re.sub(r"\s+", " ", testo).strip()
    if len(testo) <= max_car:
        return testo
    taglio = testo.rfind(" ", 0, max_car)
    return testo[: taglio if taglio > 0 else max_car] + "…"


def main() -> None:
    if not FONTI_CSV.exists():
        print(f"Non trovo {FONTI_CSV}", file=sys.stderr)
        sys.exit(1)

    with open(FONTI_CSV, encoding="utf-8", newline="") as f:
        righe = list(csv.DictReader(f, delimiter=";"))

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    OUT_TESTO_DIR.mkdir(parents=True, exist_ok=True)
    OUT_LEGGIBILE_DIR.mkdir(parents=True, exist_ok=True)

    documenti = []
    postings: dict[str, dict[int, int]] = {}

    for doc_id, riga in enumerate(righe):
        file_rel = riga["file"]
        path = NORMATIVA / file_rel
        if not path.exists():
            print(f"  ! file mancante, salto: {file_rel}", file=sys.stderr)
            continue

        titolo = riga.get("titolo", "") or ""
        autorita = riga.get("autorita", "") or ""
        argomenti_raw = riga.get("argomenti", "") or ""
        argomenti = [a.strip() for a in argomenti_raw.split(",") if a.strip()]

        testo = estrai_testo(path)
        # Tutti gli spazi bianchi (spazi, tabulazioni, a capo) diventano uno
        # spazio singolo: una ricerca per frase deve trovare "moneta
        # elettronica" anche se nel PDF le due parole sono su righe diverse
        # per via dell'impaginazione, non solo quando sono sulla stessa riga.
        # Non e' una perdita per la visualizzazione: il browser collassa
        # comunque gli a capo in un paragrafo HTML.
        testo_pulito = re.sub(r"\s+", " ", testo).strip()

        documenti.append({
            "id": doc_id,
            "file": file_rel,
            "categoria": categoria(file_rel),
            "tipo": riga.get("tipo", ""),
            "titolo": titolo,
            "autorita": autorita,
            "identificativo": riga.get("identificativo", ""),
            "data": riga.get("data_documento", "") or None,
            "argomenti": argomenti,
            "url": riga.get("url", ""),
            "estratto": estratto_breve(testo) if testo else "",
        })

        if testo_pulito:
            (OUT_TESTO_DIR / f"{doc_id}.txt").write_text(testo_pulito, encoding="utf-8")

        if path.suffix.lower() == ".xml":
            grezzo = path.read_text(encoding="utf-8", errors="ignore")
            scrivi_pagina_leggibile(
                doc_id,
                {"titolo": titolo, "autorita": autorita,
                 "identificativo": riga.get("identificativo", ""), "url": riga.get("url", "")},
                xml_leggibile(grezzo),
                file_rel,
            )

        pesi_doc: dict[str, int] = {}
        for tok in tokenizza(titolo):
            pesi_doc[tok] = pesi_doc.get(tok, 0) + 6
        for tok in tokenizza(autorita + " " + " ".join(argomenti)):
            pesi_doc[tok] = pesi_doc.get(tok, 0) + 3
        conteggio_corpo: dict[str, int] = {}
        for tok in tokenizza(testo):
            conteggio_corpo[tok] = conteggio_corpo.get(tok, 0) + 1
        for tok, n in conteggio_corpo.items():
            pesi_doc[tok] = pesi_doc.get(tok, 0) + min(n, 8)

        for tok, peso in pesi_doc.items():
            postings.setdefault(tok, {})[doc_id] = peso

        print(f"  [{doc_id + 1}/{len(righe)}] {file_rel} — {len(pesi_doc)} termini")

    postings_compatte = {
        term: [[d, w] for d, w in sorted(diz.items(), key=lambda kv: -kv[1])]
        for term, diz in postings.items()
    }

    (OUT_DIR / "documenti.json").write_text(
        json.dumps(documenti, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )
    (OUT_DIR / "indice.json").write_text(
        json.dumps(postings_compatte, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )

    dim_doc = (OUT_DIR / "documenti.json").stat().st_size
    dim_idx = (OUT_DIR / "indice.json").stat().st_size
    dim_testo = sum(p.stat().st_size for p in OUT_TESTO_DIR.glob("*.txt"))
    print(f"\n{len(documenti)} documenti indicizzati, {len(postings)} termini unici")
    print(f"documenti.json: {dim_doc / 1024:.0f} KB")
    print(f"indice.json:    {dim_idx / 1024:.0f} KB")
    print(f"testo/*.txt:    {dim_testo / 1024 / 1024:.1f} MB totali (recuperati on-demand, non in blocco)")


if __name__ == "__main__":
    main()
