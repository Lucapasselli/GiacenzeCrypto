#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Estrae da un file Akoma Ntoso di Normattiva un sottoinsieme di articoli o di
commi e lo riscrive in Markdown leggibile.

Il testo delle disposizioni NON viene alterato: si toccano solo impaginazione e
spaziatura. Restano quindi visibili le convenzioni tipografiche di Normattiva:

  * ``(( ))``  racchiude il testo introdotto o modificato da un atto successivo;
  * ``(12)``   sono i richiami alle note di aggiornamento dell'atto.

Uso:
  python3 akn_estrai.py FILE.xml --articoli 67,68,110            > estratto.md
  python3 akn_estrai.py FILE.xml --articolo 1 --commi 126-147    > estratto.md

Opzioni utili:
  --titolo TESTO      intestazione del documento prodotto
  --intestazione FILE file Markdown da anteporre (provenienza, avvertenze)
"""

import argparse
import hashlib
import os
import re
import sys
import xml.etree.ElementTree as ET

AKN = "http://docs.oasis-open.org/legaldocml/ns/akn/3.0"
NS = {"a": AKN}


def testo(el):
    """Testo di un elemento, con gli spazi normalizzati."""
    return re.sub(r"\s+", " ", "".join(el.itertext())).strip()


def sha256(percorso):
    h = hashlib.sha256()
    with open(percorso, "rb") as f:
        for b in iter(lambda: f.read(1 << 16), b""):
            h.update(b)
    return h.hexdigest()


def intervallo(spec):
    """"126-147,150" -> [126..147, 150]"""
    fuori = []
    for pezzo in spec.split(","):
        pezzo = pezzo.strip()
        if "-" in pezzo:
            a, b = pezzo.split("-", 1)
            fuori.extend(range(int(a), int(b) + 1))
        elif pezzo:
            fuori.append(int(pezzo))
    return fuori


def rendi_paragrafo(par, livello="###"):
    """Un <paragraph> in Markdown: numero in grassetto, lettere come elenco."""
    righe = []
    num = par.find("a:num", NS)
    etichetta = testo(num) if num is not None else ""
    marca = f"**{etichetta}** " if etichetta else ""
    lista = par.find("a:list", NS)
    if lista is not None:
        intro = lista.find("a:intro", NS)
        righe.append(f"{marca}{testo(intro) if intro is not None else ''}".strip())
        for punto in lista.findall("a:point", NS):
            pn = punto.find("a:num", NS)
            corpo = testo(punto.find("a:content", NS)) if punto.find("a:content", NS) is not None else testo(punto)
            righe.append(f"- **{testo(pn) if pn is not None else ''}** {corpo}".strip())
    else:
        corpo = par.find("a:content", NS)
        righe.append(f"{marca}{testo(corpo) if corpo is not None else testo(par)}".strip())
    return "\n\n".join(r for r in righe if r.strip(" *"))


def rendi_articolo(art, solo_commi=None):
    righe = []
    num = art.find("a:num", NS)
    tit = art.find("a:heading", NS)
    intestazione = testo(num) if num is not None else ""
    if tit is not None and testo(tit):
        intestazione += " — " + testo(tit)
    righe.append(f"### {intestazione}".strip())
    for par in art.findall("a:paragraph", NS):
        if solo_commi is not None:
            m = re.search(r"__para_(\d+)$", par.get("eId") or "")
            if not m or int(m.group(1)) not in solo_commi:
                continue
        blocco = rendi_paragrafo(par)
        if blocco.strip(" *"):
            righe.append(blocco)
    return "\n\n".join(righe)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("file")
    ap.add_argument("--articoli", help="elenco/intervallo di articoli, es. 67,68,110")
    ap.add_argument("--articolo", help="un solo articolo, da usare con --commi")
    ap.add_argument("--commi", help="elenco/intervallo di commi, es. 126-147")
    ap.add_argument("--titolo")
    ap.add_argument("--intestazione")
    args = ap.parse_args()

    radice = ET.parse(args.file).getroot()
    articoli = radice.findall(".//a:article", NS)

    if args.titolo:
        print(f"# {args.titolo}\n")
    if args.intestazione and os.path.exists(args.intestazione):
        with open(args.intestazione, encoding="utf-8") as f:
            print(f.read().rstrip() + "\n")

    print("> **Provenienza del testo riportato qui sotto**  ")
    print(f"> file di origine: `{os.path.basename(args.file)}`  ")
    print(f"> sha256: `{sha256(args.file)}`  ")
    print("> fonte: Normattiva (Istituto Poligrafico e Zecca dello Stato) — testo non modificato,")
    print("> riformattato in Markdown dallo strumento `Strumenti/akn_estrai.py`.\n")

    voluti = None
    if args.articoli:
        voluti = {f"art_{n}" for n in args.articoli.replace(" ", "").split(",")}
    elif args.articolo:
        voluti = {f"art_{args.articolo}"}
    commi = intervallo(args.commi) if args.commi else None
    commi = set(commi) if commi else None

    trovati = 0
    for art in articoli:
        eid = art.get("eId") or ""
        if voluti is not None and eid not in voluti:
            continue
        blocco = rendi_articolo(art, commi)
        if blocco.strip():
            print(blocco + "\n")
            trovati += 1
    if trovati == 0:
        print("*(nessun articolo corrispondente ai criteri richiesti)*", file=sys.stderr)
        sys.exit(2)


if __name__ == "__main__":
    main()
