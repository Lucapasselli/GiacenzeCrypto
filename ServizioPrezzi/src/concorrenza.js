'use strict';

/**
 * Limita quanti fetch dal vivo verso gli exchange possono essere in corso contemporaneamente,
 * across tutte le monete e tutti gli utenti. Il client oggi fa `TimeUnit.SECONDS.sleep(1)` prima
 * di ogni fetch (Prezzi.java) — disciplina calibrata per un solo utente. Qui il carico di più
 * utenti converge sullo stesso IP verso gli exchange, quindi serve un limite sulla concorrenza
 * effettiva, non sul singolo utente. Sopra il limite le richieste **non si accodano**: il
 * chiamante deve rispondere "occupato" e lasciare che il client ripieghi in locale.
 */
class Semaforo {
    constructor(max) {
        if (!Number.isInteger(max) || max < 1) throw new RangeError('max deve essere un intero >= 1');
        this.max = max;
        this.inUso = 0;
    }

    disponibile() {
        return this.inUso < this.max;
    }

    acquisisci() {
        this.inUso++;
    }

    rilascia() {
        this.inUso = Math.max(0, this.inUso - 1);
    }
}

module.exports = { Semaforo };
