(() => {
  "use strict";

  const ETICHETTE_CATEGORIA = {
    "ISEE_DSU": "ISEE / DSU",
    "Istruzioni_Dichiarazioni": "Istruzioni dichiarazioni",
    "Leggi/Consolidato": "Leggi — consolidato",
    "Leggi/Estratti": "Leggi — estratti",
    "Leggi/Originale": "Leggi — testo originale",
    "Prassi_AgenziaEntrate": "Prassi Agenzia delle Entrate",
  };

  // Numero massimo di risultati per cui si va a recuperare il testo integrale
  // per mostrare gli estratti di contesto (ricerche senza frasi), per non
  // moltiplicare le richieste di rete a ogni ricerca.
  const MAX_SNIPPET_FETCH = 25;
  // Quando la ricerca contiene una frase (spazi), il testo va comunque
  // scaricato per verificare che le parole siano adiacenti, non solo tutte
  // presenti: limite ai candidati pre-filtrati da verificare.
  const MAX_VERIFICA_FRASE = 150;
  const MAX_RISULTATI_MOSTRATI = 200;

  const elencoEl = document.getElementById("elenco-risultati");
  const statoEl = document.getElementById("stato-ricerca");
  const campoRicerca = document.getElementById("campo-ricerca");
  const filtroCategoria = document.getElementById("filtro-categoria");
  const filtroAnno = document.getElementById("filtro-anno");

  let documenti = [];
  let indice = {};
  let terminiIndice = [];

  // Parole troppo comuni per restringere utilmente la pre-selezione di una
  // frase: usate solo per scegliere quali parole della frase interrogare
  // sull'indice prima di verificare l'adiacenza esatta nel testo — non
  // influenzano mai l'esito finale, solo quanti documenti vanno scaricati
  // per la verifica.
  const PAROLE_COMUNI = new Set([
    "il", "lo", "la", "i", "gli", "le", "un", "uno", "una",
    "di", "del", "dello", "della", "dei", "degli", "delle",
    "a", "al", "allo", "alla", "ai", "agli", "alle",
    "da", "dal", "dallo", "dalla", "dai", "dagli", "dalle",
    "in", "nel", "nello", "nella", "nei", "negli", "nelle",
    "con", "col", "coi", "su", "sul", "sullo", "sulla", "sui", "sugli", "sulle",
    "per", "tra", "fra", "che", "chi", "cui", "non", "ne", "ci", "vi",
    "lui", "lei", "noi", "voi", "loro",
    "questo", "questa", "questi", "queste", "quello", "quella", "quelli", "quelle",
    "come", "anche", "quando", "dove", "essere", "sono",
    "stati", "stato", "stata", "state", "ogni",
  ]);

  // Mappa di piegatura accenti che preserva la lunghezza carattere per
  // carattere: a differenza di String.normalize("NFKD") (usata per
  // tokenizzare l'indice) qui serve poter risalire dalla posizione trovata
  // nel testo "piegato" alla stessa posizione nel testo originale, per
  // ritagliare l'estratto di contesto e per confrontare una frase intera
  // (spazi compresi) con il testo del documento.
  const MAPPA_ACCENTI = {
    "à": "a", "á": "a", "â": "a", "ä": "a", "ã": "a",
    "è": "e", "é": "e", "ê": "e", "ë": "e",
    "ì": "i", "í": "i", "î": "i", "ï": "i",
    "ò": "o", "ó": "o", "ô": "o", "ö": "o", "õ": "o",
    "ù": "u", "ú": "u", "û": "u", "ü": "u",
    "ç": "c", "ñ": "n",
  };

  function chiaveAllineata(testo) {
    let out = "";
    for (let i = 0; i < testo.length; i++) {
      const ch = testo[i].toLowerCase();
      out += MAPPA_ACCENTI[ch] || ch;
    }
    return out;
  }

  function normalizza(testo) {
    return testo.normalize("NFKD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
  }

  function tokenizza(testo) {
    return normalizza(testo).match(/[a-z0-9]+/g) || [];
  }

  function escapeHtml(s) {
    return s.replace(/[&<>"']/g, (c) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
    }[c]));
  }

  function formattaData(iso) {
    if (!iso) return null;
    const [a, m, g] = iso.split("-");
    return `${g}/${m}/${a}`;
  }

  function popolaFiltri() {
    const categorie = [...new Set(documenti.map((d) => d.categoria))].sort();
    for (const cat of categorie) {
      const opt = document.createElement("option");
      opt.value = cat;
      opt.textContent = ETICHETTE_CATEGORIA[cat] || cat;
      filtroCategoria.appendChild(opt);
    }
    const anni = [...new Set(documenti.map((d) => (d.data || "").slice(0, 4)).filter(Boolean))]
      .sort()
      .reverse();
    for (const anno of anni) {
      const opt = document.createElement("option");
      opt.value = anno;
      opt.textContent = anno;
      filtroAnno.appendChild(opt);
    }
  }

  // Documenti (docId -> punteggio) il cui vocabolario contiene, come
  // sottostringa, OGNUNA delle parole date (intersezione = AND).
  function candidatiAND(parole) {
    let intersezione = null;
    for (const parola of parole) {
      const puntiPerDoc = new Map();
      for (const chiave of terminiIndice) {
        if (!chiave.includes(parola)) continue;
        for (const [docId, peso] of indice[chiave]) {
          puntiPerDoc.set(docId, Math.max(puntiPerDoc.get(docId) || 0, peso));
        }
      }
      if (intersezione === null) {
        intersezione = puntiPerDoc;
      } else {
        const nuova = new Map();
        for (const [docId, punti] of intersezione) {
          if (puntiPerDoc.has(docId)) nuova.set(docId, punti + puntiPerDoc.get(docId));
        }
        intersezione = nuova;
      }
      if (intersezione.size === 0) break;
    }
    return intersezione || new Map();
  }

  function interseca(mappe) {
    if (mappe.length === 0) return new Map();
    let esito = mappe[0];
    for (let i = 1; i < mappe.length; i++) {
      const nuova = new Map();
      for (const [docId, punti] of esito) {
        if (mappe[i].has(docId)) nuova.set(docId, punti + mappe[i].get(docId));
      }
      esito = nuova;
      if (esito.size === 0) break;
    }
    return esito;
  }

  // "+" combina piu' ricerche in AND. Lo spazio NON separa piu' parole: un
  // gruppo con uno spazio e' una frase, cercata cosi' come e' scritta
  // (adiacente, verificata nel testo integrale del documento), non come
  // parole sparse da trovare indipendentemente.
  function analizzaQuery(query) {
    const gruppi = query.split("+").map((g) => g.trim()).filter(Boolean);
    if (gruppi.length === 0) return null;

    return gruppi.map((gruppo) => {
      const eFrase = /\s/.test(gruppo);
      if (eFrase) {
        const tokenFrase = tokenizza(gruppo);
        const significative = tokenFrase.filter((t) => !PAROLE_COMUNI.has(t));
        return {
          frase: true,
          fold: chiaveAllineata(gruppo.replace(/\s+/g, " ").trim()),
          candidati: candidatiAND(significative.length ? significative : tokenFrase),
        };
      }
      const token = tokenizza(gruppo);
      return { frase: false, termini: token, candidati: candidatiAND(token) };
    });
  }

  function costruisciSnippet(testo, termini) {
    const chiave = chiaveAllineata(testo);
    const trovati = [];
    for (const termine of termini) {
      if (trovati.length >= 2) break;
      const idx = chiave.indexOf(termine);
      if (idx === -1) continue;
      const inizio = Math.max(0, idx - 70);
      const fine = Math.min(testo.length, idx + termine.length + 70);
      trovati.push({
        pre: (inizio > 0 ? "…" : "") + testo.slice(inizio, idx),
        match: testo.slice(idx, idx + termine.length),
        post: testo.slice(idx + termine.length, fine) + (fine < testo.length ? "…" : ""),
      });
    }
    return trovati;
  }

  function renderEstratti(contenitore, snippet) {
    if (!snippet || snippet.length === 0) { contenitore.remove(); return; }
    contenitore.innerHTML =
      `<div class="etichetta-estratti">Trovato nel testo</div>` +
      snippet.map((s) => `<p class="estratto-testo">${escapeHtml(s.pre)}<mark>${escapeHtml(s.match)}</mark>${escapeHtml(s.post)}</p>`).join("");
  }

  function caricaSnippetSemplice(contenitore, doc, termini) {
    contenitore.textContent = "Ricerca nel testo…";
    fetch(`dati/testo/${doc.id}.txt`)
      .then((r) => (r.ok ? r.text() : ""))
      .then((testo) => renderEstratti(contenitore, testo ? costruisciSnippet(testo, termini) : []))
      .catch(() => contenitore.remove());
  }

  function renderRisultato(doc) {
    const dataFmt = formattaData(doc.data);
    const etichettaCategoria = ETICHETTE_CATEGORIA[doc.categoria] || doc.categoria;
    const meta = [doc.autorita, dataFmt, etichettaCategoria].filter(Boolean).join(" · ");
    const tag = doc.argomenti.length
      ? `<div class="tag-argomenti">${doc.argomenti.map((a) => `<span>${escapeHtml(a)}</span>`).join("")}</div>`
      : "";
    const eXml = doc.file.toLowerCase().endsWith(".xml");
    const linkCopia = eXml
      ? `dati/leggibile/${doc.id}.html`
      : `archivio/${doc.file.split("/").map(encodeURIComponent).join("/")}`;
    const azioni = [];
    if (doc.url) azioni.push(`<a href="${escapeHtml(doc.url)}" target="_blank" rel="noopener">Apri fonte ufficiale ↗</a>`);
    azioni.push(`<a href="${linkCopia}" target="_blank" rel="noopener">Apri copia archiviata${eXml ? " (leggibile)" : ""}</a>`);

    const div = document.createElement("article");
    div.className = "risultato";
    div.innerHTML = `
      <div class="intestazione-risultato">
        <h3>${escapeHtml(doc.titolo)}</h3>
        <span class="etichetta">${escapeHtml(doc.tipo)}</span>
      </div>
      <p class="meta">${escapeHtml(meta)}</p>
      ${tag}
      ${doc.estratto ? `<p class="estratto">${escapeHtml(doc.estratto)}</p>` : ""}
      <div class="azioni-risultato">${azioni.join("")}</div>
      <div class="estratti-testo"></div>
    `;
    return div;
  }

  function applicaFiltri(elencoBase) {
    const cat = filtroCategoria.value;
    const anno = filtroAnno.value;
    return elencoBase.filter((doc) => {
      if (cat && doc.categoria !== cat) return false;
      if (anno && (doc.data || "").slice(0, 4) !== anno) return false;
      return true;
    });
  }

  function elencaOrdinati(mappaPunti) {
    return [...mappaPunti.entries()].sort((a, b) => b[1] - a[1]).map(([id]) => id);
  }

  function mostraRisultati(idOrdinati, statoMessaggioVuoto, termini, snippetPerDoc) {
    const perId = new Map(documenti.map((d) => [d.id, d]));
    let risultati = idOrdinati.map((id) => perId.get(id)).filter(Boolean);
    risultati = applicaFiltri(risultati);

    elencoEl.innerHTML = "";
    if (risultati.length === 0) {
      statoEl.textContent = statoMessaggioVuoto;
      return;
    }
    statoEl.textContent = `${risultati.length} document${risultati.length === 1 ? "o" : "i"} trovat${risultati.length === 1 ? "o" : "i"}`;

    const frammento = document.createDocumentFragment();
    const daMostrare = risultati.slice(0, MAX_RISULTATI_MOSTRATI);
    daMostrare.forEach((doc, i) => {
      const el = renderRisultato(doc);
      frammento.appendChild(el);
      const contenitore = el.querySelector(".estratti-testo");
      if (snippetPerDoc && snippetPerDoc.has(doc.id)) {
        renderEstratti(contenitore, snippetPerDoc.get(doc.id));
      } else if (!snippetPerDoc && termini.length > 0 && i < MAX_SNIPPET_FETCH) {
        caricaSnippetSemplice(contenitore, doc, termini);
      } else {
        contenitore.remove();
      }
    });
    elencoEl.appendChild(frammento);
  }

  let richiestaCorrente = 0;

  function aggiorna() {
    const idRichiesta = ++richiestaCorrente;
    const query = campoRicerca.value.trim();

    if (!query) {
      const tutti = [...documenti].sort((a, b) => (b.data || "").localeCompare(a.data || ""));
      mostraRisultati(tutti.map((d) => d.id), "Nessun documento trovato.", [], null);
      return;
    }

    const gruppi = analizzaQuery(query);
    if (!gruppi) {
      const tutti = [...documenti].sort((a, b) => (b.data || "").localeCompare(a.data || ""));
      mostraRisultati(tutti.map((d) => d.id), "Nessun documento trovato.", [], null);
      return;
    }
    const preFiltro = interseca(gruppi.map((g) => g.candidati));

    if (preFiltro.size === 0) {
      mostraRisultati([], "Nessun documento contiene tutti i termini cercati.", [], null);
      return;
    }

    const frasi = gruppi.filter((g) => g.frase);
    const terminiSemplici = gruppi.filter((g) => !g.frase).flatMap((g) => g.termini);

    if (frasi.length === 0) {
      mostraRisultati(elencaOrdinati(preFiltro), "Nessun documento contiene tutti i termini cercati.", terminiSemplici, null);
      return;
    }

    // Una o piu' frasi: la presenza di tutte le parole non basta, va
    // verificata l'adiacenza esatta nel testo del documento.
    statoEl.textContent = "Ricerca nel testo dei documenti…";
    elencoEl.innerHTML = "";

    const candidatiOrdinati = elencaOrdinati(preFiltro).slice(0, MAX_VERIFICA_FRASE);
    const richiesteTesto = candidatiOrdinati.map((id) =>
      fetch(`dati/testo/${id}.txt`)
        .then((r) => (r.ok ? r.text() : ""))
        .catch(() => "")
        .then((testo) => ({ id, testo }))
    );

    Promise.all(richiesteTesto).then((esiti) => {
      if (idRichiesta !== richiestaCorrente) return; // superata da una ricerca piu' recente

      const superstiti = new Map(); // docId -> punteggio
      const snippetPerDoc = new Map();

      for (const { id, testo } of esiti) {
        if (!testo) continue;
        const chiave = chiaveAllineata(testo);
        const tutteTrovate = frasi.every((f) => chiave.includes(f.fold));
        if (!tutteTrovate) continue;

        superstiti.set(id, preFiltro.get(id));
        const terminiSnippet = [...frasi.map((f) => f.fold), ...terminiSemplici];
        snippetPerDoc.set(id, costruisciSnippet(testo, terminiSnippet));
      }

      mostraRisultati(
        elencaOrdinati(superstiti),
        "Nessun documento contiene la frase cercata.",
        [],
        snippetPerDoc
      );
    });
  }

  let timerInput = null;
  campoRicerca.addEventListener("input", () => {
    clearTimeout(timerInput);
    timerInput = setTimeout(aggiorna, 150);
  });
  filtroCategoria.addEventListener("change", aggiorna);
  filtroAnno.addEventListener("change", aggiorna);

  Promise.all([
    fetch("dati/documenti.json").then((r) => r.json()),
    fetch("dati/indice.json").then((r) => r.json()),
  ])
    .then(([docs, idx]) => {
      documenti = docs;
      indice = idx;
      terminiIndice = Object.keys(idx);
      popolaFiltri();
      aggiorna();
    })
    .catch((err) => {
      statoEl.textContent = "Impossibile caricare l'archivio normative. Riprova più tardi.";
      console.error(err);
    });
})();
