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
  
}


