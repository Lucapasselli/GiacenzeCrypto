#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Costruisce i documenti di ``Normativa/Leggi/Consolidato``: per ogni disposizione
toccata dalle leggi di bilancio, il testo dell'articolo modificato COME ERA e
COME E' DIVENTATO, piu' l'elenco delle modifiche che Normattiva registra su
quell'articolo e il testo della novella che le ha prodotte.

Perche' e' fatto cosi'. Le leggi di bilancio non riscrivono l'articolo: scrivono
solo la variazione ("dopo la lettera c-quinquies) e' inserita la seguente..."),
quindi il testo integrato non sta in nessuno dei due atti. Ricostruirlo a mano
significherebbe scrivere un testo che nessuno ha promulgato. Qui invece si
accostano DUE TESTI UFFICIALI: la fotografia dell'articolo il giorno prima e
quella il giorno dopo, entrambe scaricate da Normattiva. L'unica parte derivata
e' l'accostamento — nessuna parola e' scritta da questo strumento.

Le modifiche vengono dal blocco ``<activeModifications>`` dell'Akoma Ntoso della
legge di bilancio: ogni ``<textualMod>`` ha una ``<destination>`` che indica in
modo preciso quale articolo di quale atto viene toccato, per esempio
``/akn/it/act/decreto_del_presidente_della_repubblica/stato/1986-12-22/917/...
/~art_67__para_1``. Il blocco ``<passiveModifications>`` dell'atto modificato NON
serve allo scopo: li' la ``<source>`` e' vuota (``#``) e il testo riportato e' il
testo gia' modificato, senza dire da chi.

Configurazione in ``consolidato.json``; i file prodotti sono derivati e vanno
rigenerati, non modificati a mano.
"""

import json
import os
import re
import xml.etree.ElementTree as ET
from datetime import date

AKN = "http://docs.oasis-open.org/legaldocml/ns/akn/3.0"
NS = {"a": AKN}
QUI = os.path.dirname(os.path.abspath(__file__))
RADICE = os.path.dirname(QUI)


def testo(el):
    return re.sub(r"\s+", " ", "".join(el.itertext())).strip()


def trova(radice, eid):
    for tag in ("article", "paragraph", "point"):
        for el in radice.findall(f".//a:{tag}", NS):
            if el.get("eId") == eid:
                return el
    return None


def ritaglia(radice, articolo, inizio, fine):
    """Ritaglia dal testo dell'articolo il tratto compreso fra due espressioni.

    Serve perche' Normattiva non struttura allo stesso modo tutte le fotografie:
    il testo appena inserito da una novella compare per un certo periodo dentro
    doppie parentesi tonde all'interno dell'elemento precedente, e diventa un
    elemento con un proprio eId solo a un consolidamento successivo. Cercare per
    eId, in quelle fotografie, farebbe concludere per errore che la disposizione
    non esiste ancora — quando invece c'e', e in vigore.
    """
    art = trova(radice, f"art_{articolo}")
    if art is None:
        return None
    intero = testo(art)
    a = re.search(inizio, intero)
    if not a:
        return None
    b = re.search(fine, intero[a.start():])
    if not b:
        return None
    return intero[a.start():a.start() + b.end()].strip()


def modifiche_verso(radice, filtro):
    """Le modifiche che l'atto letto dispone verso le destinazioni che corrispondono
    all'espressione ``filtro``. Restituisce (destinazione, descrizione)."""
    fuori, visti = [], set()
    for m in radice.findall(".//a:activeModifications/a:textualMod", NS):
        d = m.find("a:destination", NS)
        destinazione = (d.get("href") if d is not None else "") or ""
        if not re.search(filtro, destinazione):
            continue
        nuovo = m.find("a:new", NS)
        descrizione = testo(nuovo) if nuovo is not None else ""
        chiave = (destinazione, descrizione)
        if chiave in visti:
            continue
        visti.add(chiave)
        fuori.append((destinazione, descrizione))
    return fuori


def leggi(percorso):
    return ET.parse(os.path.join(RADICE, percorso)).getroot()


def main():
    with open(os.path.join(QUI, "consolidato.json"), encoding="utf-8") as f:
        voci = json.load(f)

    for v in voci:
        righe = [f"# {v['titolo']}", ""]
        righe += [
            "> ⚠ **DOCUMENTO DERIVATO — non ha valore ufficiale.**  ",
            "> Non e' un testo promulgato: e' l'accostamento di piu' fotografie del testo",
            "> ufficiale, scaricate da Normattiva alle date indicate qui sotto. Ogni singolo",
            "> testo riportato e' ufficiale; derivato e' soltanto il fatto di averli messi",
            "> uno accanto all'altro. In caso di dubbio fa fede il testo su Normattiva.  ",
            f"> Generato da `Strumenti/consolida.py` il {date.today().isoformat()}.",
            "",
        ]
        if v.get("premessa"):
            righe += ["## Di che cosa si tratta", "", v["premessa"], ""]

        # 1. le novelle, come sono scritte nelle leggi di bilancio
        if v.get("novelle"):
            righe += ["## La disposizione che ha modificato l'articolo", ""]
            for n in v["novelle"]:
                radice = leggi(n["file"])
                el = trova(radice, n["eId"])
                righe += [f"**{n['etichetta']}**", "",
                          (testo(el) if el is not None else "*(non trovato nel file indicato)*"), ""]

        # 2. il testo dell'articolo a ciascuna data
        righe += ["## Il testo dell'articolo, a ciascuna data", ""]
        precedente = None
        for f in v["fotografie"]:
            radice = leggi(f["file"])
            corpo = None
            if v.get("cerca"):
                corpo = ritaglia(radice, v["articolo"], v["cerca"]["inizio"], v["cerca"]["fine"])
            if corpo is None:
                el = trova(radice, v["eId"])
                corpo = testo(el) if el is not None else None
            righe.append(f"### {f['etichetta']}")
            righe.append("")
            if corpo is None:
                righe += ["*(a questa data la disposizione non compare nel corpo dell'atto: o non esiste "
                          "ancora, o Normattiva non ha ancora consolidato la modifica nel testo — si veda "
                          "l'avvertenza in testa al documento)*", ""]
            else:
                if precedente is not None and corpo == precedente:
                    righe += ["*(testo identico alla fotografia precedente)*", ""]
                else:
                    righe += [corpo, ""]
                precedente = corpo
            righe.append(f"<sub>fonte: `{f['file']}`</sub>")
            righe.append("")

        # 3. le modifiche disposte, lette dai metadati delle leggi che le hanno fatte
        filtro = v.get("filtro_destinazione") or rf"art_{re.escape(v['articolo'])}(__|\b)"
        mods = []
        for n in v.get("novelle", []):
            for destinazione, descrizione in modifiche_verso(leggi(n["file"]), filtro):
                mods.append((n["etichetta"], destinazione, descrizione))
        righe += ["## Le modifiche disposte, come le registra Normattiva", ""]
        if mods:
            righe += ["| disposizione | destinazione (identificativo Akoma Ntoso) | nota |", "|---|---|---|"]
            for etichetta, destinazione, descrizione in mods:
                nota = re.sub(r"\s+", " ", descrizione)[:300].replace("|", "/")
                righe.append(f"| {etichetta} | `{destinazione}` | {nota} |")
        else:
            righe.append("*(i metadati dell'atto non riportano modifiche verso questo articolo; "
                         "il confronto fra le fotografie qui sopra resta comunque valido)*")
        righe.append("")

        dest = os.path.join(RADICE, v["out"])
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        with open(dest, "w", encoding="utf-8") as f:
            f.write("\n".join(righe))
        print(f"-> {v['out']}  ({len(mods)} modifiche registrate)")


if __name__ == "__main__":
    main()
