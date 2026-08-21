#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Costruisce l'elenco delle istruzioni di compilazione dei modelli dichiarativi
(Redditi persone fisiche e 730) pubblicate dall'Agenzia delle entrate, anno per
anno, leggendo le pagine ufficiali "Modello e istruzioni".

Gli indirizzi delle pagine seguono uno schema stabile:

    /portale/modello-redditi-persone-fisiche-<anno>/modello-e-istruzioni
    /portale/redditi-persone-fisiche-<anno>/modello-e-istruzioni
    /portale/730-<anno>/modello-e-istruzioni

L'Agenzia ha cambiato lo schema fra un anno e l'altro (il 2026 usa
``modello-redditi-persone-fisiche-2026``, gli anni precedenti
``redditi-persone-fisiche-<anno>``, il 2023 aggiunge il suffisso ``-pf2023`` e il
2024 e' una pagina a se': ``modello-e-istruzioni-redditi-persone-fisiche-2024``).
Lo script prova le varianti note e tiene la prima che risponde.

mentre gli indirizzi dei PDF no: contengono la data dell'ultimo aggiornamento
(``pf1_istruzioni_2026_agg-28-05-2026``), che cambia a ogni ristampa. Vanno
quindi letti dalla pagina, non costruiti.

Di Redditi PF servono tutti e tre i fascicoli: il quadro RW (monitoraggio e
imposta sul valore delle cripto-attivita') e il quadro RT (plusvalenze) stanno
nel FASCICOLO 2, non nel primo.

Scrive ``elenco_istruzioni.json``, pronto per ``scarica_documenti.py``.

Uso:  python3 istruzioni_ade_indice.py [anno_da anno_a]
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
CARTELLA = "Istruzioni_Dichiarazioni"
A = "Agenzia delle entrate"


def pagina(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=90) as r:
        return r.read().decode("utf-8", "replace")


def collegamenti_documento(testo):
    for m in re.finditer(r'<a[^>]+href="([^"]*/documents/[^"]*)"[^>]*>(.*?)</a>', testo, re.S):
        etichetta = html.unescape(re.sub(r"\s+", " ", re.sub(r"<[^>]+>", "", m.group(2)))).strip()
        if etichetta:
            yield etichetta, html.unescape(m.group(1))


def pulisci(s):
    s = re.sub(r"\s*-\s*pdf$", "", s, flags=re.I)
    s = re.sub(r"[^0-9A-Za-zàèéìòù]+", "_", s).strip("_")
    return s[:90]


def main():
    anno_da, anno_a = (int(sys.argv[1]), int(sys.argv[2])) if len(sys.argv) > 2 else (2023, 2026)
    voci = []
    for anno in range(anno_da, anno_a + 1):
        for chiave, varianti, modello in (
                ("Redditi PF",
                 [f"{BASE}/portale/modello-redditi-persone-fisiche-{anno}/modello-e-istruzioni",
                  f"{BASE}/portale/redditi-persone-fisiche-{anno}/modello-e-istruzioni",
                  f"{BASE}/portale/redditi-persone-fisiche-{anno}/modello-e-istruzioni-pf{anno}",
                  f"{BASE}/portale/modello-e-istruzioni-redditi-persone-fisiche-{anno}"],
                 "Redditi Persone fisiche"),
                ("730",
                 [f"{BASE}/portale/730-{anno}/modello-e-istruzioni",
                  f"{BASE}/portale/730-{anno}/modello-e-istruzioni-730-{anno}"],
                 "730")):
            testo, indirizzo = None, None
            for tentativo in varianti:
                try:
                    testo, indirizzo = pagina(tentativo), tentativo
                    break
                except Exception as e:                  # noqa: BLE001
                    print(f"   (non disponibile) {tentativo}: {e}")
            if testo is None:
                continue
            trovati = 0
            for etichetta, url in collegamenti_documento(testo):
                if not re.search(r"istruzioni", etichetta, re.I):
                    continue
                if url.startswith("/"):
                    url = BASE + url
                nome = f"{anno}_{modello.replace(' ', '')}_{pulisci(etichetta)}.pdf"
                voci.append({
                    "nome": nome, "cartella": CARTELLA,
                    "titolo": f"Modello {modello} {anno} — {etichetta}",
                    "identificativo": f"Modello {modello} {anno} (redditi {anno - 1})",
                    "autorita": A, "url": url,
                })
                trovati += 1
            print(f"== {chiave} {anno}: {trovati} documenti di istruzioni")
            time.sleep(1.0)

    with open(os.path.join(QUI, "elenco_istruzioni.json"), "w", encoding="utf-8") as f:
        json.dump(voci, f, ensure_ascii=False, indent=1)
    print(f"\n{len(voci)} voci in Strumenti/elenco_istruzioni.json")
    for v in voci:
        print("  ", v["nome"])


if __name__ == "__main__":
    main()
