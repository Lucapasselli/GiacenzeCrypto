/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;


/**
 *
 * @author luca.passelli
 */
public class Moneta {
  public String Moneta;
  public String Qta;
  public String MonetaAddress;
//  public String MonetaNomeCompleto;
 // public String IndirizzoNoWallet;
  public String Prezzo;
  public String Tipo; //NFT, FIAT o CRYPTO
  public String Rete;//da gestire, anzi forse conviene gestire rete e address come tipologia multipla
  
  public String GruppoRW;//usato solamente per calcolo finare RW come Base di appoggio, in futuro sarà da sostituire
  
  
  
public void InserisciValori(String Nome,String Quantita,String Address,String Tipologia){
    Moneta=Nome;
    Qta=Quantita;
    MonetaAddress=Address;
    Tipo=Tipologia;
}

public String GetNome(){
    return Moneta;
}
public String GetQta(){
    return Qta;
}
public String GetAddress(){
    return MonetaAddress;
}
public String GetTipologia(){
    return Tipo;
}

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
            Monete[0].Rete = Rete;
            Monete[1].Moneta = v[11];
            Monete[1].Tipo = v[12];
            Monete[1].Qta = v[13];
            Monete[1].Rete = Rete;
            
            return Monete;
    }
    
}


