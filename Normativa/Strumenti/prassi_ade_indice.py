#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Percorre gli indici ufficiali della prassi dell'Agenzia delle entrate
(circolari, risoluzioni, risposte agli interpelli) e produce l'elenco dei
documenti che parlano di cripto-attivita'.

Come e' organizzato il sito, e perche' lo script naviga invece di costruire
gli indirizzi: la pagina annuale (``/portale/interpelli-2023``) elenca le
pagine mensili, ma i loro indirizzi non seguono uno schema affidabile — la
pagina di settembre 2023 e' pubblicata come ``settembre-2023-intepelli_``,
con un refuso. Gli indirizzi mensili vanno quindi letti dalla pagina annuale.

Ogni voce dell'elenco mensile ha questa forma:

    data-analytics-asset-title="Circolare n. 30 del 27/10/2023"
    ... <a href="...Circolare+criptoattivita....pdf">Trattamento fiscale delle
    cripto-attivita'. Articolo 1, commi da 126 a 147, ...</a>

cioe' numero e data nell'attributo, oggetto nel testo del collegamento.

Lo script NON scarica nulla: scrive ``candidati_prassi.json``, da rivedere a
mano prima di passare a ``scarica_prassi_ade.py``. La selezione automatica per
parole chiave e' un filtro, non un giudizio: alcune risposte parlano di
cripto-attivita' senza usare la parola (per esempio la n. 78/2025, che ha per
oggetto l'articolo 110, comma 3-bis, del TUIR).

Uso:  python3 prassi_ade_indice.py [anno_da anno_a]
"""

import html
import json
import os
import re
import sys
import time
import urllib.request

BASE = "https://www.agenziaentrate.gov.it"
UA = ("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/126.0 Safari/537.36")
QUI = os.path.dirname(os.path.abspath(__file__))
PAUSA = 1.0

TIPI = {"circolari": "Circolare", "risoluzioni": "Risoluzione",
        "interpelli": "Risposta a interpello"}

# Sezioni che non seguono lo schema ``<tipo>-<anno>``: si parte dalla pagina della
# sezione e dal suo archivio, e si seguono i collegamenti che nominano un anno.
SEZIONI = {
    # i principi di diritto cambiano schema a meta' strada: fino al 2023
    # ``principi-di-diritto-<anno>``, dal 2024 ``principi-di-diritto-anno-<anno>``.
    # Si parte dall'archivio, che li elenca tutti, invece di indovinare lo schema.
    "Principio di diritto": [
        "/portale/normativa-e-prassi/risposte-agli-interpelli/principi-di-diritto",
        "/portale/normativa-e-prassi/risposte-agli-interpelli/principi-di-diritto/"
        "archivio-principi-di-diritto",
    ],
    "Risposta a consulenza giuridica": [
        "/portale/normativa-e-prassi/risposte-agli-interpelli/"
        "risposte-alle-istanze-di-consulenza-giuridica",
        "/portale/normativa-e-prassi/risposte-agli-interpelli/"
        "risposte-alle-istanze-di-consulenza-giuridica/"
        "archivio-risposte-alle-istanze-di-consulenza-giuridica",
    ],
}

# Il filtro e' volutamente largo: meglio qualche falso positivo da scartare a
# mano che un documento perso.
CHIAVI = re.compile(
    r"cripto|criptovalut|cripto-attivit|valut[ae] virtual|bitcoin|blockchain|"
    r"registro distribuito|distributed ledger|\bdlt\b|\bnft\b|token|staking|"
    r"c-sexies|comma 9-bis|110,? comma 3-bis|3-bis.*110|dac ?8|\bcarf\b|"
    r"prestatori di servizi.*cripto", re.I)


def pagina(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=90) as r:
        return r.read().decode("utf-8", "replace")


def collegamenti(testo, filtro):
    return sorted({html.unescape(u) for u in re.findall(r'href="([^"]+)"', testo) if filtro(u)})


def voci_pagina(testo):
    """Estrae (titolo, oggetto, url) dalle schede di una pagina mensile."""
    fuori = []
    blocchi = re.finditer(
        r'data-analytics-asset-title="([^"]*)"(.*?)(?=data-analytics-asset-title=|\Z)', testo, re.S)
    for b in blocchi:
        titolo = html.unescape(b.group(1)).strip()
        a = re.search(r'<a[^>]+href="([^"]+)"[^>]*>(.*?)</a>', b.group(2), re.S)
        if not a:
            continue
        oggetto = html.unescape(re.sub(r"\s+", " ", re.sub(r"<[^>]+>", "", a.group(2)))).strip()
        fuori.append((titolo, oggetto, html.unescape(a.group(1))))
    return fuori


def main():
    anno_da, anno_a = (int(sys.argv[1]), int(sys.argv[2])) if len(sys.argv) > 2 else (2023, 2026)
    candidati, esaminati = [], 0

    for tipo, etichetta in TIPI.items():
        for anno in range(anno_da, anno_a + 1):
            annuale = f"{BASE}/portale/{tipo}-{anno}"
            try:
                testo = pagina(annuale)
            except Exception as e:                      # noqa: BLE001
                print(f"!! {annuale}: {e}")
                continue
            mensili = collegamenti(
                testo, lambda u, a=str(anno): "/portale/" in u and a in u
                and "/documents/" not in u and "archivio" not in u
                and not u.rstrip("/").endswith(f"{tipo}-{a}"))
            # la pagina annuale puo' gia' contenere le voci (anno in corso)
            pagine = [annuale] + [u if u.startswith("http") else BASE + u for u in mensili]
            print(f"== {etichetta} {anno}: {len(pagine)} pagine")
            for p in pagine:
                time.sleep(PAUSA)
                try:
                    t = pagina(p)
                except Exception as e:                  # noqa: BLE001
                    print(f"   !! {p}: {e}")
                    continue
                for titolo, oggetto, url in voci_pagina(t):
                    esaminati += 1
                    if CHIAVI.search(oggetto) or CHIAVI.search(titolo):
                        candidati.append({"tipo": etichetta, "anno": anno, "titolo": titolo,
                                          "oggetto": oggetto, "url": url, "indice": p})

    for etichetta, semi in SEZIONI.items():
        pagine = []
        for seme in semi:
            url = seme if seme.startswith("http") else BASE + seme
            try:
                t = pagina(url)
            except Exception as e:                      # noqa: BLE001
                print(f"!! {url}: {e}")
                continue
            pagine.append((url, t))
            for u in collegamenti(t, lambda u: "/portale/" in u and "/documents/" not in u
                                  and re.search(r"20(2[3-9])", u)):
                u = u if u.startswith("http") else BASE + u
                if u in [p for p, _ in pagine]:
                    continue
                time.sleep(PAUSA)
                try:
                    pagine.append((u, pagina(u)))
                except Exception as e:                  # noqa: BLE001
                    print(f"   !! {u}: {e}")
        print(f"== {etichetta}: {len(pagine)} pagine")
        for p, t in pagine:
            for titolo, oggetto, url in voci_pagina(t):
                esaminati += 1
                anno = int((re.search(r"20(2\d)", titolo) or re.search(r"20(2\d)", url)
                            or re.match(r"(\d{4})", "0000")).group(0)) if re.search(r"20\d\d", titolo + url) else 0
                if CHIAVI.search(oggetto) or CHIAVI.search(titolo):
                    candidati.append({"tipo": etichetta, "anno": anno, "titolo": titolo,
                                      "oggetto": oggetto, "url": url, "indice": p})

    # deduplica per url senza il parametro anti-cache ?t=
    visti, puliti = set(), []
    for c in candidati:
        chiave = c["url"].split("?t=")[0]
        if chiave in visti:
            continue
        visti.add(chiave)
        c["url_canonico"] = chiave
        puliti.append(c)

    with open(os.path.join(QUI, "candidati_prassi.json"), "w", encoding="utf-8") as f:
        json.dump(puliti, f, ensure_ascii=False, indent=1)
    print(f"\n{esaminati} documenti esaminati, {len(puliti)} candidati scritti in "
          f"Strumenti/candidati_prassi.json")
    for c in puliti:
        print(f"  [{c['anno']}] {c['titolo']} — {c['oggetto'][:90]}")


if __name__ == "__main__":
    main()
