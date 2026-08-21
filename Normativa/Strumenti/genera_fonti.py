#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Genera ``Normativa/fonti.csv``: l'indice di provenienza di ogni file dell'archivio.

E' quello che rende verificabile la promessa "solo documenti ufficiali": per ogni
file riporta chi lo pubblica, con quale identificativo ufficiale, da quale
indirizzo e' stato preso, quando, e l'impronta SHA-256 del file cosi' com'e' oggi.
Chi legge puo' ricalcolare l'impronta e riscaricare dall'indirizzo indicato.

I file sono di due tipi, tenuti distinti nella colonna ``tipo``:

  ufficiale  scaricato tale e quale dalla fonte che lo pubblica;
  derivato   prodotto dagli strumenti di questa cartella a partire dai file
             ufficiali (estratti in Markdown, testi accostati). Non ha valore
             ufficiale e nella colonna ``url`` porta il file di partenza.

Le informazioni vengono dai manifest scritti dagli strumenti di scarico
(``manifest_normattiva.json``, ``elenco_*_manifest.json``) e dalle configurazioni
degli strumenti di derivazione (``estratti.json``, ``consolidato.json``), piu'
``documenti_redazionali.json`` per le poche schede scritte a mano.
Un file presente sul disco ma non descritto da nessuno di questi finisce comunque
in ``fonti.csv``, con tipo ``SCONOSCIUTO``: e' un errore da correggere, non una
riga da ignorare.

Uso:  python3 genera_fonti.py
"""

import csv
import glob
import hashlib
import json
import os

QUI = os.path.dirname(os.path.abspath(__file__))
RADICE = os.path.dirname(QUI)
CARTELLE = ["Leggi", "Prassi_AgenziaEntrate", "Istruzioni_Dichiarazioni", "ISEE_DSU"]
INTESTAZIONE = ["file", "tipo", "titolo", "autorita", "identificativo", "url",
                "scaricato_il", "sha256", "byte"]


def sha256(percorso):
    h = hashlib.sha256()
    with open(percorso, "rb") as f:
        for b in iter(lambda: f.read(1 << 16), b""):
            h.update(b)
    return h.hexdigest()


def carica(nome):
    percorso = os.path.join(QUI, nome)
    if not os.path.exists(percorso):
        return []
    with open(percorso, encoding="utf-8") as f:
        return json.load(f)


def main():
    descrizioni = {}

    # file ufficiali: dai manifest degli strumenti di scarico
    manifest = list(carica("manifest_normattiva.json"))
    for nome in sorted(glob.glob(os.path.join(QUI, "elenco_*_manifest.json"))):
        with open(nome, encoding="utf-8") as f:
            manifest.extend(json.load(f))
    for r in manifest:
        descrizioni[r["file"].replace(os.sep, "/")] = dict(
            tipo="ufficiale", titolo=r.get("titolo", ""), autorita=r.get("autorita", ""),
            identificativo=r.get("identificativo", ""), url=r.get("url", ""),
            scaricato_il=r.get("scaricato_il", ""))

    # file derivati: dalle configurazioni degli strumenti che li producono
    for v in carica("estratti.json"):
        descrizioni[v["out"]] = dict(
            tipo="derivato", titolo=v["titolo"],
            autorita="estratto da testo ufficiale (Strumenti/akn_estrai.py)",
            identificativo="riformattazione in Markdown, testo non alterato",
            url=v["file"], scaricato_il="")
    # schede scritte a mano (esiti di ricerca, note di lettura)
    for v in carica("documenti_redazionali.json"):
        descrizioni[v["file"]] = dict(
            tipo="derivato", titolo=v["titolo"],
            autorita=v.get("autorita", "scheda redazionale di questo archivio"),
            identificativo=v.get("identificativo", "NON UFFICIALE: testo scritto per questo archivio"),
            url=v.get("url", ""), scaricato_il="")

    for v in carica("consolidato.json"):
        origini = "; ".join(f["file"] for f in v["fotografie"])
        descrizioni[v["out"]] = dict(
            tipo="derivato", titolo=v["titolo"],
            autorita="accostamento di testi ufficiali (Strumenti/consolida.py)",
            identificativo="NON UFFICIALE: nessun testo promulgato in questa forma",
            url=origini, scaricato_il="")

    righe, sconosciuti = [], 0
    for cartella in CARTELLE:
        base = os.path.join(RADICE, cartella)
        for radice, _, file in os.walk(base):
            for nome in sorted(file):
                percorso = os.path.join(radice, nome)
                relativo = os.path.relpath(percorso, RADICE).replace(os.sep, "/")
                d = descrizioni.get(relativo)
                if d is None:
                    d = dict(tipo="SCONOSCIUTO", titolo="", autorita="",
                             identificativo="", url="", scaricato_il="")
                    sconosciuti += 1
                righe.append([relativo, d["tipo"], d["titolo"], d["autorita"],
                              d["identificativo"], d["url"], d["scaricato_il"],
                              sha256(percorso), os.path.getsize(percorso)])

    righe.sort(key=lambda r: r[0])
    with open(os.path.join(RADICE, "fonti.csv"), "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, delimiter=";", quoting=csv.QUOTE_MINIMAL)
        w.writerow(INTESTAZIONE)
        w.writerows(righe)

    ufficiali = sum(1 for r in righe if r[1] == "ufficiale")
    derivati = sum(1 for r in righe if r[1] == "derivato")
    print(f"fonti.csv: {len(righe)} file — {ufficiali} ufficiali, {derivati} derivati, "
          f"{sconosciuti} senza provenienza")
    if sconosciuti:
        print("!! file senza provenienza (da sistemare):")
        for r in righe:
            if r[1] == "SCONOSCIUTO":
                print("   ", r[0])
    return 1 if sconosciuti else 0


if __name__ == "__main__":
    raise SystemExit(main())
