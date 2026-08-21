#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Genera i file di ``Normativa/Leggi/Estratti`` a partire dagli XML ufficiali
scaricati in ``Normativa/Leggi/Originale``, seguendo l'elenco in ``estratti.json``.

Ogni voce dell'elenco descrive: quale file XML leggere, quali articoli o commi
prendere, dove scrivere il Markdown e con quale titolo. Rilanciare lo script
rigenera tutti gli estratti: sono file derivati, non vanno modificati a mano.
"""

import json
import os
import subprocess
import sys

QUI = os.path.dirname(os.path.abspath(__file__))
RADICE = os.path.dirname(QUI)


def main():
    with open(os.path.join(QUI, "estratti.json"), encoding="utf-8") as f:
        voci = json.load(f)

    errori = 0
    for v in voci:
        sorgente = os.path.join(RADICE, v["file"])
        destinazione = os.path.join(RADICE, v["out"])
        if not os.path.exists(sorgente):
            print(f"!! manca il file di origine: {v['file']}")
            errori += 1
            continue
        os.makedirs(os.path.dirname(destinazione), exist_ok=True)
        cmd = [sys.executable, os.path.join(QUI, "akn_estrai.py"), sorgente,
               "--titolo", v["titolo"]]
        if v.get("articoli"):
            cmd += ["--articoli", v["articoli"]]
        if v.get("articolo"):
            cmd += ["--articolo", v["articolo"]]
        if v.get("commi"):
            cmd += ["--commi", v["commi"]]
        esito = subprocess.run(cmd, capture_output=True, text=True)
        if esito.returncode != 0:
            print(f"!! {v['out']}: {esito.stderr.strip()}")
            errori += 1
            continue
        testo = esito.stdout
        if v.get("nota"):
            righe = testo.split("\n")
            # la nota va subito sotto il titolo di primo livello
            righe.insert(2, "> **Nota di lettura** " + v["nota"] + "\n")
            testo = "\n".join(righe)
        with open(destinazione, "w", encoding="utf-8") as f:
            f.write(testo)
        print(f"-> {v['out']}  ({len(testo)} caratteri)")

    print(f"\n{len(voci) - errori}/{len(voci)} estratti generati.")
    return 1 if errori else 0


if __name__ == "__main__":
    sys.exit(main())
