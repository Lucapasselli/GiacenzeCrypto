# packaging/winget/ — snapshot di bootstrap per winget-pkgs

Questi 4 file sono l'input **solo per la primissima submission** a
`microsoft/winget-pkgs` (la PR che crea il pacchetto `LucaPasselli.GiacenzeCrypto`).
Vanno copiati in un fork di quel repository sotto:

```
manifests/l/LucaPasselli/GiacenzeCrypto/1.0.63/
```

(prima lettera del publisher minuscola; la cartella `LucaPasselli` deve combaciare
carattere per carattere con il `PackageIdentifier`).

**Dopo che la prima PR è stata mergiata, l'autorità è winget-pkgs, non questa cartella.**
Le versioni successive NON si aggiornano qui a mano: le apre `.github/workflows/winget.yml`
(azione `microsoft/winget-releaser`), che parte dal manifest già presente su winget-pkgs e
si limita a cambiare versione, URL e SHA256. Quindi questi file restano fermi a 1.0.63 anche
quando è pubblica una versione più recente — sono un punto di partenza storico, non lo stato
corrente.

Procedura completa (comandi, CLA, gestione degli errori della pipeline):
`nocommit/Documentazione/Pubblicazione_Winget.md`.
