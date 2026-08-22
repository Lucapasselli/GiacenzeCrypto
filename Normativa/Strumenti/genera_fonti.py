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

Due colonne in piu' rispetto ai campi scritti a mano nelle configurazioni:

  data_documento  data ISO (AAAA-MM-GG) del documento, quando ricostruibile.
                  Per gli ufficiali viene dai manifest (per Normattiva e' la data
                  della vigenza scaricata, non la data di scarico - vuota per
                  "vigente", che non ha una data propria: e' un "come e' oggi"
                  che cambia a ogni scarico, non un dato del documento). Per gli
                  estratti si eredita dal file ufficiale di origine - un estratto
                  non ha una data propria. Per i consolidati si prende la piu'
                  recente fra le leggi che hanno modificato la disposizione (le
                  "novelle"): e' quella che risponde a "quando e' cambiata
                  l'ultima volta questa regola", il dato utile per un documento
                  che per natura accosta piu' fotografie. Vuota quando nessuna di
                  queste fonti la offre (es. le istruzioni annuali dei modelli
                  dichiarativi, note solo per anno): non va indovinata.
  argomenti       etichette tematiche separate da virgola (es. "plusvalenze,
                  quadro RW"), lette dal campo omonimo delle configurazioni.
                  Classificazione per argomento, non interpretazione del testo
                  normativo - la stessa distinzione che vale per titolo/autorita'.

Uso:  python3 genera_fonti.py
"""

import csv
import glob
import hashlib
import json
import os
import re

QUI = os.path.dirname(os.path.abspath(__file__))
RADICE = os.path.dirname(QUI)
CARTELLE = ["Leggi", "Prassi_AgenziaEntrate", "Istruzioni_Dichiarazioni", "ISEE_DSU"]
INTESTAZIONE = ["file", "tipo", "titolo", "autorita", "identificativo",
                "data_documento", "argomenti", "url", "scaricato_il", "sha256", "byte"]

# Riconosce la data di promulgazione incorporata negli id delle leggi/norme scaricate
# da Normattiva (es. "L_2024-12-30_207_bilancio2025_originario.akn.xml"): e' un
# identificativo che questo stesso archivio controlla (atti.json), non un'euristica
# su un nome file qualsiasi.
RE_DATA_ID_LEGGE = re.compile(r"^[A-Z]+_(\d{4}-\d{2}-\d{2})_")


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
            scaricato_il=r.get("scaricato_il", ""),
            data_documento=r.get("data_documento", ""), argomenti=r.get("argomenti", ""))

    # file derivati: dalle configurazioni degli strumenti che li producono
    for v in carica("estratti.json"):
        # un estratto non ha una data propria: eredita quella del file ufficiale da cui e' tratto
        data_origine = descrizioni.get(v["file"], {}).get("data_documento", "")
        descrizioni[v["out"]] = dict(
            tipo="derivato", titolo=v["titolo"],
            autorita="estratto da testo ufficiale (Strumenti/akn_estrai.py)",
            identificativo="riformattazione in Markdown, testo non alterato",
            url=v["file"], scaricato_il="",
            data_documento=data_origine, argomenti=", ".join(v.get("argomenti", [])))
    # schede scritte a mano (esiti di ricerca, note di lettura)
    for v in carica("documenti_redazionali.json"):
        descrizioni[v["file"]] = dict(
            tipo="derivato", titolo=v["titolo"],
            autorita=v.get("autorita", "scheda redazionale di questo archivio"),
            identificativo=v.get("identificativo", "NON UFFICIALE: testo scritto per questo archivio"),
            url=v.get("url", ""), scaricato_il="",
            data_documento=v.get("data", ""), argomenti=", ".join(v.get("argomenti", [])))

    for v in carica("consolidato.json"):
        origini = "; ".join(f["file"] for f in v["fotografie"])
        # data del consolidato = la piu' recente fra le leggi che hanno modificato la
        # disposizione (le "novelle"): un accostamento di piu' fotografie non ha una
        # sola data propria, ma "quando e' cambiata l'ultima volta" e' un fatto preciso,
        # ricavabile dall'id della legge stessa (RE_DATA_ID_LEGGE), senza indovinare nulla.
        date_novelle = []
        for n in v.get("novelle", []):
            m = RE_DATA_ID_LEGGE.match(os.path.basename(n["file"]))
            if m:
                date_novelle.append(m.group(1))
        descrizioni[v["out"]] = dict(
            tipo="derivato", titolo=v["titolo"],
            autorita="accostamento di testi ufficiali (Strumenti/consolida.py)",
            identificativo="NON UFFICIALE: nessun testo promulgato in questa forma",
            url=origini, scaricato_il="",
            data_documento=max(date_novelle) if date_novelle else "",
            argomenti=", ".join(v.get("argomenti", [])))

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
                             identificativo="", url="", scaricato_il="",
                             data_documento="", argomenti="")
                    sconosciuti += 1
                righe.append([relativo, d["tipo"], d["titolo"], d["autorita"],
                              d["identificativo"], d["data_documento"], d["argomenti"],
                              d["url"], d["scaricato_il"],
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
