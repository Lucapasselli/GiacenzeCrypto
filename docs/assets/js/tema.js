(() => {
  "use strict";
  const CHIAVE = "gc-tema";

  function applica(tema) {
    if (tema === "chiaro" || tema === "scuro") {
      document.documentElement.setAttribute("data-tema", tema);
    } else {
      document.documentElement.removeAttribute("data-tema");
    }
  }

  function correntementeScuro() {
    const salvato = localStorage.getItem(CHIAVE);
    if (salvato === "chiaro") return false;
    if (salvato === "scuro") return true;
    return !window.matchMedia("(prefers-color-scheme: light)").matches;
  }

  document.addEventListener("DOMContentLoaded", () => {
    const bottone = document.querySelector(".selettore-tema");
    if (!bottone) return;
    bottone.addEventListener("click", () => {
      const nuovo = correntementeScuro() ? "chiaro" : "scuro";
      localStorage.setItem(CHIAVE, nuovo);
      applica(nuovo);
    });
  });
})();
