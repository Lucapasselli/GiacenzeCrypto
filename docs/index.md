---
layout: default
title: Giacenze Crypto
---

# Giacenze Crypto

Applicazione desktop per il calcolo delle giacenze e delle plusvalenze in criptovaluta ai fini della
dichiarazione dei redditi italiana (**Quadro RW** e **Quadro RT**), con metodo **LIFO**.

L'applicazione lavora **in locale**: i movimenti, i wallet e le quotazioni scaricate restano sul
computer di chi la usa.

- [Documentazione](documentazione/) — i manuali, consultabili dal browser o scaricabili in PDF
- [Novità delle versioni](documentazione/changelog.html) — cosa cambia a ogni rilascio
- [Informativa sulla privacy](privacy/) — Privacy policy (italiano / English)
- [Codice sorgente e rilasci su GitHub](https://github.com/Lucapasselli/GiacenzeCrypto)

## In breve {#in-breve}

- Importazione dei movimenti da **Binance**, **OKX** e **Crypto.com** (API o file CSV), da esportazioni
  di servizi come CoinTracking e Tatax, e da CSV qualsiasi tramite descrittori JSON.
- Importazione diretta da **blockchain**: Bitcoin, Solana e una ventina di reti EVM.
- **Classificazione dei movimenti** (acquisti, vendite, permute, trasferimenti, reward, prestiti,
  commissioni) con calcolo del costo di carico secondo il metodo LIFO.
- **Recupero delle quotazioni storiche** con cache locale dei prezzi.
- **Prospetti per il Quadro RT e il Quadro RW**, esportabili in PDF ed Excel.
- **Backup e ripristino** dell'intero archivio.

Il programma è uno strumento di supporto al calcolo: non sostituisce il parere di un professionista
abilitato. Si veda il [disclaimer](documentazione/disclaimer.html).
