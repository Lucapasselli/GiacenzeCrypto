---
layout: default
title: Costo medio netto in Giacenze a data
---

# Costo medio netto in Giacenze a data

- [A cosa serve](#a-cosa-serve)
- [Dove si trova](#dove-si-trova)
- [Le colonne](#le-colonne)
- [Come si calcola](#come-si-calcola)
- [Un esempio completo in cinque passi](#esempio)
- [Confronto con il costo di carico LIFO](#confronto-lifo)
- [Quali movimenti contano e come](#movimenti)
- [Quando le colonne restano vuote e cosa dicono le note](#colonne-vuote)
- [Come confrontare i numeri con altri strumenti](#confronto-strumenti)
- [Domande frequenti](#domande)
- [Limiti e cose da sapere](#limiti)

## A cosa serve {#a-cosa-serve}

La tabella *Giacenze a data* mostra, per ogni token, la quantità posseduta, il suo valore alla data scelta e il
**costo di carico** calcolato con il metodo LIFO. Il costo di carico risponde alla domanda «quanto è costato
quello che mi resta», secondo le regole del calcolo delle plusvalenze.

Il **costo medio netto** risponde a un'altra domanda, più semplice e più vicina all'esperienza di chi guarda il
proprio portafoglio: **quanto denaro ho messo in questo token e quanto ne ho tolto, e a che prezzo dovrei vendere
quello che mi resta per essere in pareggio?**

Le colonne nuove permettono di:

- vedere **quanto hai speso davvero** per acquistare un token (*Costo totale*) e **quanto hai incassato** cedendolo
  (*Vendite*), sommando tutte le piattaforme e tutti i wallet,
- conoscere il **prezzo di pareggio** (*Costo medio netto*): sopra quel prezzo il token, considerando tutto quello
  che hai già incassato, è in utile, sotto è in perdita,
- leggere il **risultato complessivo** del token (*Profitti/Perdite totali*), che somma la parte già incassata e il
  valore di quello che possiedi ancora,
- fare un **confronto** con il metodo LIFO, o con strumenti esterni che mostrano gli stessi numeri, per esempio la
  scheda *My Portfolio* di CoinGecko.

> **Non è un calcolo fiscale.** Le colonne sono solo informative: non cambiano nessun movimento, non entrano nel
> calcolo delle plusvalenze (quadro RT) né nel quadro W/RW. Il costo di carico LIFO, le plusvalenze e le loro opzioni
> restano quelli di sempre e non vengono toccati. Il manuale descrive cosa calcola il programma, le valutazioni
> sulla propria dichiarazione restano dell'utente.

## Dove si trova {#dove-si-trova}

Nella scheda **Analisi Crypto – Giacenze a Data**, sotto la tabella dei token, accanto a *Mostra Giacenze a Zero* e a
*Nascondi Token Scam*, c'è la casella **Mostra costi netti**. Il manuale dell'intera scheda è **[Giacenze a data](giacenze-a-data.html)**,
raggiungibile anche dal pulsante *Guida...* della scheda stessa.

- Le colonne sono **nascoste di default**: spuntando la casella compaiono, togliendo la spunta spariscono.
- La scelta viene **ricordata** fra una sessione e l'altra.
- Le colonne sono calcolate **sempre**, anche quando sono nascoste: mostrarle o nasconderle è immediato e non
  ricarica la tabella.
- Il calcolo considera i movimenti **anteriori alla data** scelta in alto a destra, come il resto della tabella.

## Le colonne {#le-colonne}

Le cinque colonne si trovano in fondo alla tabella, dopo *Qta Blockchain*. Gli esempi di questo manuale usano i dati
inventati degli screenshot di *Giacenze a data* (tre wallet di fantasia, prezzi di mercato veri al 31/12/2025).

![La tabella con le colonne dei costi netti: Costo totale, Vendite, Costo medio netto, Profitti/Perdite totali e Note](immagini/costo-medio-netto/01.png)

| Colonna | Che cosa mostra |
|---|---|
| **Costo totale (in Euro)** | la somma di quanto è stato speso per ottenere il token: acquisti, scambi nei quali il token è stato ricevuto, commissioni in euro collegate all'acquisto |
| **Vendite (in Euro)** | la somma di quanto è stato incassato cedendo il token: vendite, scambi nei quali il token è stato dato, prelievi classificati come cashout |
| **Costo medio netto (tutti i wallet)** | (Costo totale − Vendite) diviso la quantità posseduta. È il prezzo di pareggio |
| **Profitti/Perdite totali (tutti i wallet)** | quantità × prezzo alla data + Vendite − Costo totale. Il risultato complessivo del token, realizzato e latente insieme |
| **Note costo netto** | cosa rende incerto il numero, in rosso. Vuota se non c'è nulla da segnalare |

L'intestazione dice «tutti i wallet» perché il calcolo ignora il wallet scelto in alto: vedi [Limiti](#limiti).
Nell'intestazione non compare la somma delle colonne, perché lo stesso token può comparire su più righe di rete
con lo stesso valore e la somma lo conterebbe più volte.

## Come si calcola {#come-si-calcola}

Per ogni token il programma tiene **due conti separati**.

**1. La quantità.** È la somma con segno di tutte le entrate e le uscite del token, di tutti i movimenti anteriori
alla data, di tutti i wallet, **trasferimenti compresi**. È la stessa quantità che la tabella mostra con la selezione
«Tutti». Quello che entra aumenta la quantità, quello che esce la riduce, qualunque sia il motivo (vendita,
commissione, trasferimento con commissione di rete trattenuta, prelievo non classificato).

**2. Il denaro.** Due totali, in euro: quanto è stato **speso** per avere il token e quanto è stato **incassato**
cedendolo. Cosa viene considerato speso o incassato lo decide il tipo di movimento (vedi [la tabella](#movimenti)),
usando il **valore in euro** che il movimento ha già nella tabella dei movimenti, cioè il prezzo del momento.

Da questi due conti nascono le colonne:

- **Costo medio netto** = (speso − incassato) / quantità posseduta
- **Profitti/Perdite totali** = quantità × prezzo alla data + incassato − speso

Due proprietà utili:

- il costo medio netto è il **prezzo di pareggio**: se il prezzo fosse esattamente quel valore, i profitti/perdite
  totali sarebbero zero,
- i profitti/perdite totali dipendono **solo da cosa è entrato e cosa è uscito**, non dall'ordine dei movimenti né da
  come si abbina il costo alle vendite.

Il token è identificato dal **simbolo e dal tipo** (per esempio *ETH Crypto*), senza guardare indirizzo di contratto e
rete: lo stesso simbolo su due reti è un token solo. Così un bridge fra reti non sposta nessun costo.

## Un esempio completo in cinque passi {#esempio}

Il token XYZ, tutti i valori in euro, prezzo di mercato alla data scelta 6 €. Ogni riga della tabella è la situazione
**dopo** il movimento indicato.

| Passo | Movimento | Quantità | Speso (totale) | Incassato (totale) | Costo medio netto |
|---|---|---|---|---|---|
| 1 | Acquisto di 100 XYZ pagati 100 € (1 € l'uno) | 100 | 100 | 0 | (100 − 0) / 100 = **1,00** |
| 2 | Acquisto di 100 XYZ pagati 300 € (3 € l'uno) | 200 | 400 | 0 | (400 − 0) / 200 = **2,00** |
| 3 | Vendita di 100 XYZ per 400 € (4 € l'uno) | 100 | 400 | 400 | (400 − 400) / 100 = **0,00** |
| 4 | Vendita di 50 XYZ per 250 € (5 € l'uno) | 50 | 400 | 650 | (400 − 650) / 50 = **−5,00** |
| 5 | Acquisto di 100 XYZ pagati 200 € (2 € l'uno) | 150 | 600 | 650 | (600 − 650) / 150 = **−0,33** |

Come si leggono i passi:

- **Passi 1 e 2.** Finché non ci sono vendite, il costo medio netto coincide con il prezzo medio di acquisto.
- **Passo 3.** Hai incassato 400 € e ne avevi spesi 400. Le 100 unità che restano sono «gratis»: il pareggio è a 0.
- **Passo 4.** Hai incassato 250 € in più di quanto speso. Il valore negativo dice che i soldi sono già rientrati e che
  c'è un utile in tasca, anche se il token scendesse a zero.
- **Passo 5.** Hai ricomprato a 2 €, ma il conto complessivo resta sotto zero: hai speso 600 € e ne hai già incassati 650.

Alla fine, con il prezzo a 6 € e 150 XYZ in giacenza:

| Colonna | Valore |
|---|---|
| Costo totale | 600,00 |
| Vendite | 650,00 |
| Costo medio netto | −0,33 |
| Profitti/Perdite totali | 150 × 6 + 650 − 600 = **950,00** |

Prova del pareggio al passo 2: con 200 XYZ, speso 400 e incassato 0, il costo medio netto è 2,00 e a un prezzo di
2 € i profitti/perdite sarebbero 200 × 2 + 0 − 400 = 0.

## Confronto con il costo di carico LIFO {#confronto-lifo}

Il costo di carico LIFO e il costo medio netto non devono coincidere, perché misurano cose diverse. Il LIFO
abbina ogni vendita ai lotti acquistati più di recente e dice quanto è costato quello che resta. Il costo medio netto
guarda solo il denaro entrato e uscito.

Esempio con quattro movimenti semplici (acquisti e vendite per euro, prezzo alla data 6 €):

| Passo | Movimento |
|---|---|
| 1 | Acquisto di 100 XYZ pagati 100 € |
| 2 | Acquisto di 100 XYZ pagati 300 € |
| 3 | Vendita di 100 XYZ per 400 € |
| 4 | Acquisto di 100 XYZ pagati 500 € |

Alla fine si hanno 200 XYZ, che valgono 200 × 6 = 1.200 €.

| | Costo medio netto | Costo di carico LIFO |
|---|---|---|
| Come si ottiene | (speso 900 − incassato 400) / 200 | la vendita del passo 3 scarica il lotto più recente (quello da 3 €), restano il lotto da 1 € e quello da 5 € |
| Risultato | **2,50 € per unità** | 100 + 500 = 600 €, cioè **3,00 € per unità** |
| Parte già realizzata | non distingue | 400 − 300 = 100 € |
| Parte latente | non distingue | 1.200 − 600 = 600 € |
| Risultato complessivo | 1.200 + 400 − 900 = **700 €** | 100 + 600 = **700 €** |

Il **risultato complessivo è lo stesso** con qualunque metodo, perché dipende solo da quanto è entrato e uscito. Cambia
solo come lo si divide fra utile già realizzato e utile ancora latente, e il prezzo per unità del residuo: 2,50 contro 3,00.

Una media ponderata classica darebbe un terzo numero ancora (3,50 in questo esempio): il programma non la calcola. Il costo medio netto non è
una media del costo di acquisto ma un prezzo di pareggio, e per questo può scendere sotto zero o valere zero, cosa che
una media del costo non fa mai.

## Quali movimenti contano e come {#movimenti}

Il programma classifica ogni movimento e decide cosa aggiunge a «speso» e a «incassato». Il **valore** usato è sempre
il *Valore transazione in EURO* del movimento.

| Movimento | Effetto sul token | Speso | Incassato |
|---|---|---|---|
| **Acquisto con euro** (o con un'altra valuta FIAT) | entra il token | valore dell'acquisto | |
| **Vendita per euro** | esce il token | | valore della vendita |
| **Scambio fra due cripto** | esce A ed entra B | B: valore dello scambio | A: valore dello scambio |
| **Acquisto o vendita di un NFT** pagato con cripto | la cripto esce o entra | se entra: valore | se esce: valore |
| **Deposito classificato come acquisto** (acquisto tramite contanti o servizi esterni) | entra il token | valore | |
| **Reward, staking, airdrop, earn, cashback** | entra il token | **zero** | |
| **Deposito a costo zero, donazione ricevuta, rimborso oltre il credito di un fallimento** | entra il token | **zero** | |
| **Deposito non classificato** | entra il token | **zero**, con nota | |
| **Prelievo non classificato** | esce il token | | **zero**, con nota |
| **Cashout o pagamento di un servizio** (prelievo classificato come cashout) | esce il token | | valore |
| **Commissione pagata in cripto** | esce il token | | **zero** |
| **Rettifica di giacenza, rimborso di una reward** | esce il token | | **zero** |
| **Commissione in euro di un acquisto** | nessuno | si somma al token acquistato, se collegata | |
| **Trasferimento fra i propri wallet** | entra ed esce la stessa quantità | non cambia | non cambia |
| **Deposito o prelievo in euro**, scambio fra valute FIAT | nessuno sul token | | |

Alcuni casi meritano un esempio. Negli esempi parte sempre da 100 XYZ acquistati con 100 € (speso 100 €, nessun
incasso), salvo diversa indicazione.

**Scambio fra due cripto.** Scambi 50 XYZ con 20 ABC e il valore dello scambio è 100 €. Per il programma è una
**vendita di XYZ e un acquisto di ABC**, entrambi a 100 €, anche se ai fini del calcolo delle plusvalenze lo scambio
può essere neutro. XYZ: incassato 100, quantità 50, costo medio netto (100 − 100) / 50 = **0,00**. ABC: speso 100,
quantità 20, costo medio netto 100 / 20 = **5,00**.

**Reward.** Ricevi 10 XYZ di staking, che in quel momento valgono 15 €. Entrano a **costo zero**: speso resta 100,
quantità 110, costo medio netto 100 / 110 = **0,909**. Il costo netto conta solo i soldi usciti davvero dal tuo
portafoglio, quindi tutta la reward è utile. Con il prezzo a 2 € i profitti/perdite sono 110 × 2 + 0 − 100 = **120**.
Questa scelta non segue le opzioni del calcolo delle plusvalenze sulle reward.

**Deposito non classificato.** Arrivano 50 XYZ da una provenienza che il programma non conosce: un wallet tuo non
importato, un regalo, un acquisto fatto altrove. Entrano a **costo zero** e la nota lo segnala: speso 100, quantità 150,
costo medio netto 100 / 150 = **0,667**. Se erano tuoi e arrivavano da un wallet non importato, il numero è
ottimistico: classificare il movimento (per esempio come trasferimento) lo corregge.

**Prelievo non classificato.** Escono 50 XYZ verso una destinazione sconosciuta. Escono **senza incasso**, e la nota lo
segnala: speso 100, incassato 0, quantità 50, costo medio netto 100 / 50 = **2,00**. Non viene contato come una vendita
perché potrebbe essere un trasferimento verso un tuo wallet non importato, e contarlo come vendita accrediterebbe un
incasso mai avvenuto.

**Commissione in cripto.** Paghi una commissione di 1 XYZ, che vale 2 €. Esce senza incasso: speso 100, incassato 0,
quantità 99, costo medio netto 100 / 99 = **1,0101**. Con il prezzo a 2 € i profitti/perdite sono 99 × 2 + 0 − 100 = **98**:
la commissione è un costo vero e pesa sul risultato.

**Commissione in euro su un acquisto.** Compri 100 XYZ con un addebito totale di 105 €, cioè acquisto 100 € più
commissione 5 € (alcune piattaforme la registrano come movimento a parte). La commissione si **somma allo speso**:
speso 105, costo medio netto 105 / 100 = **1,05**, e con il prezzo a 2 € i profitti/perdite sono 200 − 105 = **95**.
Funziona solo se il programma ha **collegato** la commissione a quell'acquisto e se in quel gruppo c'è **un solo**
movimento che fa entrare un token. In ogni altro caso la commissione non viene attribuita a nessun token.

**Trasferimento fra i propri wallet con commissione di rete.** Sposti 40 XYZ su un altro tuo wallet e ne arrivano 39,5.
Il trasferimento **non sposta denaro**, ma i 0,5 XYZ trattenuti come commissione di rete escono dalla quantità senza
incasso: quantità 99,5, speso 100, costo medio netto 100 / 99,5 = **1,005**.

**Bridge fra reti.** Compri 100 ETH per 100 € e ne sposti 40 su un'altra rete. Il token è lo stesso, quindi entrambe le
righe mostrano costo totale 100, quantità 100 e costo medio netto **1,00**.

**Fallimenti di piattaforme.** Il blocco del credito, la restituzione in natura e i rimborsi sono scambi fra cripto e
si trattano come tali. Esempio: 1 BTC acquistato per 50.000 € viene bloccato in una procedura quando vale 30.000 €. Il
BTC ha costo totale 50.000, vendite 30.000 e quantità 0, quindi il costo medio netto resta vuoto. Il token credito ha
costo totale 30.000 e quantità 1: costo medio netto **30.000**.

Cosa non entra mai nel pool: i token **FIAT** (euro, dollari), gli **NFT**, e le monete che non hanno alcun movimento
anteriore alla data.

## Quando le colonne restano vuote e cosa dicono le note {#colonne-vuote}

Il programma preferisce **lasciare vuoto un numero** piuttosto che mostrarne uno sbagliato.

| Situazione | Costo totale e Vendite | Costo medio netto | Profitti/Perdite |
|---|---|---|---|
| Tutto regolare | mostrati | mostrato | mostrato |
| Quantità nulla o negativa (venduto tutto, o più di quanto posseduto) | mostrati | vuoto | vuoto |
| Il token non ha un prezzo alla data | mostrati | mostrato | vuoto |
| Un movimento che porta denaro non ha il valore in euro | mostrati (parziali) | vuoto | vuoto |
| Token FIAT o NFT | vuoti | vuoto | vuoto |

La colonna **Note costo netto**, in rosso, dice cosa rende il numero incerto. I testi possibili sono:

- *N movimenti senza prezzo*: la riga ha almeno un movimento che porta denaro (acquisto, vendita, scambio, cashout)
  senza valore in euro. Per ritrovare il numero basta assegnare il prezzo al movimento dalla tabella dei movimenti.
  Un valore scritto `0,00` è invece un valore valido, per esempio un importo sotto il centesimo o un movimento il cui
  prezzo è davvero zero.
- *N depositi non classificati a zero*: ci sono depositi che il programma ha contato a costo zero perché non sa da dove
  arrivano. Il costo medio netto è da prendere con cautela finché non vengono classificati.
- *N prelievi non classificati senza incasso*: ci sono prelievi contati come uscite senza incasso.

Le tre segnalazioni possono comparire insieme, separate da virgole.

## Come confrontare i numeri con altri strumenti {#confronto-strumenti}

Schede come *My Portfolio* di CoinGecko mostrano gli stessi concetti con altri nomi: *Costo totale* (quanto è stato
speso), le vendite registrate, *Costo medio netto* e *Profitti/perdite totali*. Il calcolo è lo stesso, quindi i numeri
**tornano** quando gli stessi movimenti sono registrati nello stesso modo.

Se non tornano, di solito la differenza viene da come sono trattati alcuni movimenti:

- le **reward** qui hanno costo zero, in una scheda manuale dipende da cosa l'utente ha inserito,
- gli **scambi fra cripto** qui sono sempre una vendita e un acquisto al valore del momento,
- le **commissioni** in cripto escono senza incasso,
- i movimenti che nello strumento esterno **mancano** (trasferimenti da altre piattaforme, depositi non classificati),
- il **prezzo** usato per il valore del movimento, che può essere di un'altra fonte.

Per verificare un numero conviene guardare prima *Costo totale* e *Vendite*: se coincidono, il resto del calcolo è
aritmetica.

## Domande frequenti {#domande}

**Perché il costo medio netto è negativo?** Perché hai già incassato più di quanto hai speso su quel token. Non è un
errore: significa che anche se il token valesse zero resteresti in utile (passi 4 e 5 dell'[esempio](#esempio)).

**Perché è zero?** Perché le vendite hanno restituito esattamente quanto speso: le unità che restano sono a costo zero.

**Perché la cella è vuota?** Vedi [la tabella](#colonne-vuote) e leggi la colonna *Note costo netto*.

**Perché ogni riga dello stesso token ha lo stesso costo medio netto?** Perché il calcolo è per simbolo e tipo, sommando
tutte le reti.

**Perché non cambia se scelgo un solo wallet?** Perché il denaro speso e incassato non si può attribuire a un singolo
wallet: i trasferimenti fra wallet non spostano denaro. Con un wallet selezionato la tabella mostra la quantità di
quel wallet e il costo medio netto del token su tutti i wallet.

**Perché non coincide con il costo di carico?** Perché sono due conteggi diversi: vedi
[il confronto](#confronto-lifo). Il costo di carico segue le regole del calcolo delle plusvalenze, il costo medio netto
no.

**Se classifico un movimento, i numeri cambiano?** Sì, alla prossima apertura della tabella: le colonne si ricalcolano
a ogni calcolo della tabella.

## Limiti e cose da sapere {#limiti}

- **Solo informativo.** Non entra in nessun quadro della dichiarazione e non modifica nessun movimento.
- **Globale.** Ignora il wallet e il gruppo selezionati. Il profitto/perdita di una riga usa la quantità totale del
  token e il prezzo della riga, non la quantità del singolo wallet.
- **Per simbolo e tipo.** Due token diversi con lo stesso simbolo (per esempio due contratti con lo stesso nome) vengono
  fusi.
- **Il valore è quello del movimento.** Prezzi mancanti o sbagliati entrano nel calcolo così come sono.
- **Lo `0,00` è un valore valido.** Un movimento il cui valore è realmente `0,00` e uno che non ha trovato un prezzo
  possono sembrare uguali: lo `0,00` non viene segnalato.
- **Movimenti non classificati.** Più ce ne sono, meno il numero è affidabile: la nota lo ricorda.
- **Commissioni in euro.** Entrano nello speso solo se il programma le ha collegate a un unico acquisto.
- **Prezzo alla data.** I profitti/perdite usano il prezzo che la tabella mostra per la data scelta. Con la data di oggi
  il prezzo è quello attuale. Se il token non ha prezzo, la colonna resta vuota.
- **Nessuna distinzione fra periodi.** Il calcolo parte dal primo movimento dell'archivio, a prescindere dall'anno.

Per le altre funzioni della scheda (dettaglio dei movimenti, grafico, rettifica delle giacenze, confronto con blockchain ed
exchange) vedi il manuale **[Giacenze a data](giacenze-a-data.html)**.

[Torna all'elenco dei manuali](index.html)
