#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Scarica da Normattiva (portale ufficiale dello Stato italiano) il testo degli atti
elencati in ``atti.json``, in formato Akoma Ntoso (XML strutturato ufficiale).

Per ogni atto vengono scaricate una o piu' "vigenze", cioe' fotografie del testo
a una data precisa:

  * ``originario`` -> il testo come pubblicato in Gazzetta Ufficiale;
  * ``vigente``    -> il testo coordinato con tutte le modifiche successive.

Note operative (verificate sul campo, non ovvie):

  * Normattiva richiede una **sessione**: la chiamata a ``/do/atto/caricaAKN``
    risponde con una pagina di errore HTML se non e' preceduta, nella stessa
    sessione e con lo stesso cookie, dalla risoluzione dell'URN dell'atto.
    Per questo la sessione viene ri-innescata **prima di ogni** download.
  * Il parametro ``dataVigenza`` (formato AAAAMMGG) e' quello che seleziona la
    fotografia del testo; con date diverse si ottengono file diversi.
  * La risposta valida ha ``Content-Type: text/xml`` e comincia con ``<?xml``.
    Una risposta ``text/html`` e' sempre un errore, anche con HTTP 200.

Ogni riga di manifest scrive anche ``data_documento`` (la data della vigenza appena
scaricata, non la data di scarico: per una fotografia a data fissa e' proprio quella la
data che la distingue dalle altre - vuota per "vigente", che non ha una data propria: e'
un "come e' oggi" che cambia a ogni scarico, non un dato del documento) e ``argomenti``
(etichette tematiche, lette dal campo opzionale ``argomenti`` di ``atti.json`` e valide
per tutte le vigenze dello stesso atto). Scrive anche ``url``, ma come pagina pubblica
dell'atto (apribile in un browser), non come indirizzo di scarico: l'indirizzo
``caricaAKN`` usato per scaricare l'XML richiede la sessione aperta da ``risolvi_urn``
e risponde con una pagina di errore, non con il testo, se aperto a freddo.

Uso:  python3 scarica_normattiva.py [id_atto ...]
      (senza argomenti scarica tutti gli atti elencati in atti.json)
"""

import hashlib
import http.cookiejar
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request
from datetime import date

BASE = "https://www.normattiva.it"
UA = ("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/126.0 Safari/537.36")
PAUSA = 2.5          # secondi fra una richiesta e l'altra: il portale limita
TIMEOUT = 180

QUI = os.path.dirname(os.path.abspath(__file__))
RADICE = os.path.dirname(QUI)


def apri_sessione():
    jar = http.cookiejar.CookieJar()
    op = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
    op.addheaders = [("User-Agent", UA),
                     ("Accept-Language", "it-IT,it;q=0.9")]
    return op


def scarica(op, url, referer=None):
    req = urllib.request.Request(url)
    if referer:
        req.add_header("Referer", referer)
    with op.open(req, timeout=TIMEOUT) as r:
        return r.read(), r.headers.get("Content-Type", "")


def risolvi_urn(op, urn):
    """Dall'URN ricava codice redazionale e data di pubblicazione in G.U."""
    url = BASE + "/uri-res/N2Ls?" + urn
    html, _ = scarica(op, url)
    testo = html.decode("utf-8", "replace")
    m = re.search(r"caricaAKN\?dataGU=(\d{8})&(?:amp;)?codiceRedaz=([A-Za-z0-9]+)", testo)
    if not m:
        raise RuntimeError("URN non risolto o pagina inattesa: " + urn)
    return m.group(1), m.group(2), url


def sha256(percorso):
    h = hashlib.sha256()
    with open(percorso, "rb") as f:
        for blocco in iter(lambda: f.read(1 << 16), b""):
            h.update(blocco)
    return h.hexdigest()


def main():
    with open(os.path.join(QUI, "atti.json"), encoding="utf-8") as f:
        atti = json.load(f)

    voluti = set(sys.argv[1:])
    oggi = date.today().strftime("%Y%m%d")
    op = apri_sessione()
    righe_manifest = []

    for atto in atti:
        if voluti and atto["id"] not in voluti:
            continue
        cartella = os.path.join(RADICE, atto.get("cartella", "Leggi/Originale"))
        os.makedirs(cartella, exist_ok=True)
        try:
            dataGU, codice, urn_url = risolvi_urn(op, atto["urn"])
        except Exception as e:                      # noqa: BLE001
            print(f"!! {atto['id']}: {e}")
            continue
        print(f"== {atto['id']}  GU {dataGU} cod. {codice}")

        for etichetta, quando in atto["vigenze"].items():
            vig = oggi if quando in ("OGGI", "oggi") else quando.replace("-", "")
            nome = f"{atto['id']}_{etichetta}.akn.xml"
            dest = os.path.join(cartella, nome)
            time.sleep(PAUSA)
            # la sessione va ri-innescata prima di ogni caricaAKN
            risolvi_urn(op, atto["urn"])
            time.sleep(PAUSA)
            url = (f"{BASE}/do/atto/caricaAKN?dataGU={dataGU}"
                   f"&codiceRedaz={codice}&dataVigenza={vig}")
            dati, tipo = scarica(op, url, referer=urn_url)
            if "xml" not in tipo or not dati.lstrip().startswith(b"<?xml"):
                print(f"   !! {etichetta}: risposta non XML ({tipo}, {len(dati)} byte)")
                continue
            with open(dest, "wb") as f:
                f.write(dati)
            print(f"   -> {nome}  {len(dati)} byte")
            # "vig" e' la data che la vigenza scaricata rappresenta per le fotografie a data
            # fissa (es. "2023-01-01_post_bilancio2023") - quella e' proprio la data del
            # documento. Per "vigente" (quando == OGGI) "vig" e' invece la data di *scarico*
            # travestita da vigenza: un testo "come e' oggi" non ha una data propria, e
            # scriverla in data_documento produrrebbe una data che cambia a ogni scarico e
            # che l'utente legge come "la data di questo documento" mentre e' solo il giorno
            # in cui e' stata presa la fotografia (verificato: e' cosi' che e' stata segnalata
            # la confusione). Vuota, come per le altre date non ricostruibili.
            data_documento = "" if quando in ("OGGI", "oggi") else f"{vig[0:4]}-{vig[4:6]}-{vig[6:8]}"
            # Stesso ragionamento del commento sopra, applicato al titolo: scrivere "vigenza
            # 20260821" nel titolo di un testo "vigente" e' la stessa bugia travestita da
            # promemoria - e il titolo e' la colonna che l'utente legge per prima nella
            # tabella, quindi ci e' cascato anche svuotando solo data_documento (bug
            # segnalato: troppi documenti con "data di ieri", ma nel titolo, non nella
            # colonna Data). Per le fotografie a data fissa la vigenza resta nel titolo:
            # li' e' un dato vero, non un travestimento della data di scarico.
            etichetta_titolo = f"({etichetta})" if quando in ("OGGI", "oggi") else f"({etichetta}, vigenza {vig})"
            righe_manifest.append({
                "file": os.path.relpath(dest, RADICE),
                "titolo": f"{atto['titolo']} {etichetta_titolo}",
                "autorita": "Normattiva - Istituto Poligrafico e Zecca dello Stato",
                "identificativo": f"{atto['urn']} | G.U. {dataGU} cod. {codice}",
                "data_documento": data_documento,
                "argomenti": ", ".join(atto.get("argomenti", [])),
                # "url" e' pensato per essere aperto in un browser (confronto col testo
                # ufficiale), non per essere ri-scaricato da uno script: "url" sopra
                # (caricaAKN) risponde 200 ma con una pagina di errore HTML se non e'
                # preceduta, nella stessa sessione, dalla risoluzione dell'urn (vedi il
                # docstring del modulo) - verificato che un browser che apre quel link a
                # freddo vede "Normattiva - Errore". "urn_url" e' invece la pagina pubblica
                # dell'atto (lo stesso indirizzo che risolve la citazione dell'URN) e funziona
                # sempre, aperta cosi' com'e'.
                "url": urn_url,
                "scaricato_il": date.today().isoformat(),
                "sha256": sha256(dest),
            })

    if righe_manifest:
        # il manifest e' cumulativo: uno scarico parziale non deve cancellare la
        # provenienza dei file scaricati nelle esecuzioni precedenti
        percorso = os.path.join(QUI, "manifest_normattiva.json")
        precedenti = {}
        if os.path.exists(percorso):
            with open(percorso, encoding="utf-8") as f:
                precedenti = {r["file"]: r for r in json.load(f)}
        for r in righe_manifest:
            precedenti[r["file"]] = r
        with open(percorso, "w", encoding="utf-8") as f:
            json.dump(sorted(precedenti.values(), key=lambda r: r["file"]), f,
                      ensure_ascii=False, indent=1)
        print(f"\n{len(righe_manifest)} file scaricati; "
              f"{len(precedenti)} registrati in Strumenti/manifest_normattiva.json")


if __name__ == "__main__":
    main()
