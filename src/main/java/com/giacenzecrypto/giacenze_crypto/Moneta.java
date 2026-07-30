/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.util.Set;


/**
 *
 * @author luca.passelli
 */
public class Moneta {
  public String Moneta;
  public String NomeEsteso;
  public String Qta;
  public String MonetaAddress;
  public Prezzi.InfoPrezzo InfoPrezzo;
//  public String MonetaNomeCompleto;
 // public String IndirizzoNoWallet;
  public String CostoCarico;
  public String Prezzo;
  public String Tipo; //NFT, FIAT o CRYPTO
  public String Rete;//da gestire, anzi forse conviene gestire rete e address come tipologia multipla
  
  public String GruppoRW;//usato solamente per calcolo finare RW come Base di appoggio, in futuro sarà da sostituire
  
  
  
    /**
     * Imposta in blocco nome, quantità, indirizzo e tipologia della moneta.
     * @param Nome simbolo della moneta
     * @param Quantita quantità, come stringa decimale
     * @param Address indirizzo di contratto del token
     * @param Tipologia tipo della moneta ({@code NFT}, {@code FIAT} o {@code Crypto})
     */
    public void InserisciValori(String Nome, String Quantita, String Address, String Tipologia) {
        SetNome(Nome);
        Qta = Quantita;
        SetAddress(Address);
        Tipo = Tipologia;
    }

    /** @return la fonte del prezzo associato ({@link Prezzi.InfoPrezzo#Fonte}), oppure {@code null} se non è impostato alcun {@link Prezzi.InfoPrezzo} */
    public String getFontePrezzo(){
        if (this.InfoPrezzo!=null){
            return InfoPrezzo.Fonte;
        }
        return null;
    }
    
    /**
     * Imposta la fonte del prezzo, creando un {@link Prezzi.InfoPrezzo} vuoto se non ne esiste già uno.
     * @param fonte fonte del prezzo da impostare
     */
    public void setFontePrezzo(String fonte){
        if (InfoPrezzo==null){
            InfoPrezzo=new Prezzi.InfoPrezzo();
            InfoPrezzo.Fonte=fonte;
        }else InfoPrezzo.Fonte=fonte;
    }
    
    /**
     * Imposta nome, quantità e indirizzo della moneta, deducendo automaticamente la tipologia:
     * {@code FIAT} solo per {@code EUR}/{@code USD}, {@code Crypto} per tutto il resto.
     * @param Nome simbolo della moneta
     * @param Quantita quantità, come stringa decimale
     * @param Address indirizzo di contratto del token
     */
    public void InserisciMonetaNoTipo(String Nome, String Quantita, String Address) {
        //Individua solo USD e EUR come FIAT
        Moneta = Nome;
        Qta = Quantita;
        MonetaAddress = Address;

        if (Nome.equalsIgnoreCase("EUR") || Nome.equalsIgnoreCase("USD")) {
            Tipo = "FIAT";

        } else {
            Tipo = "Crypto";
        }

    }

/** @return il simbolo della moneta */
public String GetNome(){
    return Moneta;
}
/** @return la rete/blockchain della moneta */
public String GetRete(){
    return Rete;
}

/** @return il nome completo della moneta (usato per i token DeFi) */
public String GetNomeEsteso(){
    //Nome completo per le monete in defi
    return NomeEsteso;
}


/** @param Nome simbolo della moneta da impostare */
public void SetNome(String Nome){
    Moneta=Nome;
}
/** @param ReteT rete/blockchain da impostare */
public void SetRete(String ReteT){
    Rete=ReteT;
}

/** @param Nome nome completo della moneta da impostare (usato per i token DeFi) */
public void SetNomeEsteso(String Nome){
    //Nome completo per le monete in defi
    NomeEsteso=Nome;
}

/** @return la quantità, come stringa decimale */
public String GetQta(){
    return Qta;
}
/** @return la quantità come {@link BigDecimal}, oppure {@code null} se {@link #Qta} non è un numero valido */
public BigDecimal GetQtaBD(){
    if (Funzioni.isNumeric(Qta, false))return new BigDecimal(Qta);
    return null;
}

/**
 * Imposta la quantità, solo se {@code Quant} è un valore numerico valido (altrimenti non modifica {@link #Qta}).
 * @param Quant nuova quantità, come stringa decimale
 */
public void SetQta(String Quant){
    if (Funzioni.isNumeric(Quant, false)){
        Qta=Quant;
    }
}

/**
 * Imposta il prezzo totale della moneta, solo se {@code Prz} è un valore numerico valido: crea un
 * {@link Prezzi.InfoPrezzo} se non ne esiste già uno, e aggiorna anche {@link #Prezzo}.
 * @param Prz nuovo prezzo totale, come stringa decimale
 */
public void SetPrezzo(String Prz){
    if (Funzioni.isNumeric(Prz, false)){
        if (InfoPrezzo==null){
            InfoPrezzo=new Prezzi.InfoPrezzo();
            InfoPrezzo.prezzoQta=new BigDecimal(Prz).abs();
        }else 
        {
            InfoPrezzo.prezzoQta=new BigDecimal(Prz);
        }
        Prezzo=InfoPrezzo.prezzoQta.toPlainString();
    }
}

/**
 * Sostituisce l'intero {@link Prezzi.InfoPrezzo} associato, aggiornando anche {@link #Prezzo} se disponibile.
 * @param InfoPrz il nuovo {@link Prezzi.InfoPrezzo} da associare
 */
public void SetInfoPrezzo(Prezzi.InfoPrezzo InfoPrz){
    InfoPrezzo=InfoPrz;
    if (InfoPrezzo.prezzoQta!=null)Prezzo=InfoPrezzo.prezzoQta.toPlainString();
}

/** @param Address indirizzo di contratto da impostare */
public void SetAddress(String Address){
    MonetaAddress=Address;
}

/** @return l'indirizzo di contratto della moneta */
public String GetAddress(){
    return MonetaAddress;
}
/** @return la tipologia della moneta ({@code NFT}, {@code FIAT} o {@code Crypto}) */
public String GetTipologia(){
    return Tipo;
}

/**
 * Deduce e imposta automaticamente la tipologia della moneta: {@code FIAT} se il simbolo è tra
 * {@code EUR}/{@code USD}/{@code TRY}/{@code GBP}, altrimenti {@code Crypto}.
 */
public void AssegnaTipoAuto(){
    Set<String> valute = Set.of("EUR", "USD", "TRY","GBP");
    if (valute.contains(Moneta)) {
                Tipo = "FIAT";
            } else {
                Tipo = "Crypto";
            }
    /*if (Moneta.equals("EUR")||Moneta.equals("USD")){
        Tipo="FIAT";
    }
    else Tipo="Crypto";*/
}

/**
 * Inverte il segno di {@link #Qta}: se il valore è negativo lo rende positivo e viceversa.
 *
 * <p>Correzione <b>M7</b>: viene manipolato solo il segno iniziale. La versione precedente usava
 * {@code contains("-")} e {@code replace("-","")}, che su una quantità in notazione scientifica ne
 * cancellavano anche il segno dell'esponente: {@code 1.5E-8} diventava {@code 1.5E8}, cioè un valore
 * moltiplicato per 10^16, e lo stesso accadeva invertendo {@code -1.5E-8}.</p>
 */
public void InvertiQta(){
    if (Qta!=null&&!Qta.isBlank()){
        String Valore = Qta.trim();
        if (Valore.startsWith("-"))
            Qta = Valore.substring(1);
        else
            Qta = "-" + Valore;
    }
}

    /**
     * Crea una copia superficiale di questa moneta (simbolo, indirizzo, prezzo, quantità, rete, tipo).
     * Nota: {@link #NomeEsteso}, {@link #InfoPrezzo}, {@link #CostoCarico} e {@link #GruppoRW} non vengono copiati.
     * @return la nuova istanza clonata
     */
    public Moneta ClonaMoneta() {
        Moneta mo = new Moneta();
        mo.Moneta = Moneta;
        mo.MonetaAddress = MonetaAddress;
        mo.Prezzo = Prezzo;
        mo.Qta = Qta;
        mo.Rete = Rete;
        mo.Tipo = Tipo;
        return mo;
    } 
  
    
    /**
     * Costruisce le due monete (uscente ed entrante) coinvolte in un movimento, a partire dai suoi campi grezzi,
     * ricavando la rete tramite {@link Funzioni#TrovaReteDaID}.
     * @param v riga di movimento grezza
     * @return array di 2 elementi: {@code [0]} moneta uscente, {@code [1]} moneta entrante
     */
    public static Moneta[] RitornaMoneteDaMov(String[] v){
            
            String Rete = Funzioni.TrovaReteDaID(v[0]);
            Moneta Monete[] = new Moneta[2];//in questo array metto la moneta in entrata e quellain uscita
            //in paricolare la moneta in uscita nella posizione 0 e quella in entrata nella posizione 1
            Monete[0] = new Moneta();
            Monete[1] = new Moneta();
            Monete[0].MonetaAddress = v[26];
            Monete[1].MonetaAddress = v[28];
            //ovviamente gli address se non rispettano le 2 condizioni precedenti sono null
            Monete[0].Moneta = v[8];
            Monete[0].Tipo = v[9];
            Monete[0].Qta = v[10];
            Monete[0].CostoCarico = v[16];
            Monete[0].Rete = Rete;
            Monete[1].Moneta = v[11];
            Monete[1].Tipo = v[12];
            Monete[1].Qta = v[13];
            Monete[1].CostoCarico = v[17];
            Monete[1].Rete = Rete;
            
            return Monete;
    }
    
}


