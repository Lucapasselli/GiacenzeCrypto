#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Scarica i documenti (PDF e allegati) elencati in un file JSON e ne registra la
provenienza.

Il file JSON e' una lista di oggetti con questi campi:

  nome        nome con cui salvare il file (senza percorso)
  cartella    cartella di destinazione, relativa a Normativa/
  titolo      titolo per esteso, come lo chiama l'ente che lo pubblica
  autorita    chi lo pubblica (Agenzia delle entrate, INPS, ...)
  identificativo  numero e data ufficiali (es. "Circolare n. 30/E del 27/10/2023")
  url         indirizzo da cui scaricarlo
  accept      (facoltativo) header Accept, per i repository che negoziano il formato
  lingua      (facoltativo) header Accept-Language, per quelli che negoziano la lingua
  data        (facoltativo) data del documento, ISO AAAA-MM-GG. Assente quando il
              giorno esatto non e' ricostruibile (es. le istruzioni annuali dei
              modelli dichiarativi, note solo per anno) - resta vuota, non va
              indovinata.
  argomenti   (facoltativo) elenco di etichette tematiche (es. ["plusvalenze",
              "quadro RW"]), per capire di cosa tratta un documento senza aprirlo.
              Classificazione per argomento, non interpretazione del contenuto
              normativo: usa un vocabolario ristretto e riusato fra le voci,
              invece di testo libero per ognuna.

Il campo ``url`` delle pagine dell'Agenzia delle entrate contiene spesso un
parametro ``?t=<numero>`` che serve solo a evitare la cache del browser: viene
tolto per ottenere l'indirizzo canonico registrato nel manifest, ma lo scarico
usa l'indirizzo cosi' com'e', perche' e' quello che si e' visto funzionare.

Uso:  python3 scarica_documenti.py elenco.json
"""

import hashlib
import json
import os
import sys
import time
import urllib.request
from datetime import date

UA = ("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/126.0 Safari/537.36")
QUI = os.path.dirname(os.path.abspath(__file__))
RADICE = os.path.dirname(QUI)
PAUSA = 1.5


def sha256(percorso):
    h = hashlib.sha256()
    with open(percorso, "rb") as f:
        for b in iter(lambda: f.read(1 << 16), b""):
            h.update(b)
    return h.hexdigest()


def main():
    elenco = sys.argv[1] if len(sys.argv) > 1 else os.path.join(QUI, "elenco_documenti.json")
    with open(elenco, encoding="utf-8") as f:
        voci = json.load(f)

    manifest, errori = [], 0
    for v in voci:
        cartella = os.path.join(RADICE, v["cartella"])
        os.makedirs(cartella, exist_ok=True)
        dest = os.path.join(cartella, v["nome"])
        if os.path.exists(dest) and not os.environ.get("RISCARICA"):
            print(f"   (gia' presente) {v['nome']}")
        else:
            time.sleep(PAUSA)
            try:
                intestazioni = {"User-Agent": UA}
                # il repository dell'Ufficio delle pubblicazioni dell'Unione europea
                # sceglie formato e lingua dagli header, non dall'indirizzo
                if v.get("accept"):
                    intestazioni["Accept"] = v["accept"]
                if v.get("lingua"):
                    intestazioni["Accept-Language"] = v["lingua"]
                req = urllib.request.Request(v["url"], headers=intestazioni)
                with urllib.request.urlopen(req, timeout=180) as r:
                    dati = r.read()
            except Exception as e:                      # noqa: BLE001
                print(f"!! {v['nome']}: {e}")
                errori += 1
                continue
            if dest.lower().endswith(".pdf") and not dati.startswith(b"%PDF"):
                print(f"!! {v['nome']}: la risposta non e' un PDF ({len(dati)} byte)")
                errori += 1
                continue
            with open(dest, "wb") as f:
                f.write(dati)
            print(f"-> {v['cartella']}/{v['nome']}  {len(dati)} byte")
        manifest.append({
            "file": os.path.join(v["cartella"], v["nome"]),
            "titolo": v["titolo"],
            "autorita": v.get("autorita", ""),
            "identificativo": v.get("identificativo", ""),
            "data_documento": v.get("data", ""),
            "argomenti": ", ".join(v.get("argomenti", [])),
            "url": v["url"].split("?t=")[0],
            "scaricato_il": date.today().isoformat(),
            "sha256": sha256(dest) if os.path.exists(dest) else "",
        })

    nome_manifest = os.path.splitext(os.path.basename(elenco))[0] + "_manifest.json"
    with open(os.path.join(QUI, nome_manifest), "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=1)
    print(f"\n{len(manifest)} file registrati in Strumenti/{nome_manifest}"
          + (f" ({errori} errori)" if errori else ""))
    return 1 if errori else 0


if __name__ == "__main__":
    sys.exit(main())
