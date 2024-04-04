/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author luca.passelli
 */
public class DatabaseH2 {

    static String jdbcUrl = "jdbc:h2:./database";
    static String jdbcUrl2 = "jdbc:h2:./personale";
    static String usernameH2 = "sa";
    static String passwordH2 = "";
    static Connection connection;
    static Connection connectionPersonale;

    //per compattare database comando -> SHUTDOWN COMPACT //da valutare quando farlo
    public static boolean CreaoCollegaDatabase() {
        boolean successo=false;
        try {
            connection = DriverManager.getConnection(jdbcUrl, usernameH2, passwordH2);
            connectionPersonale = DriverManager.getConnection(jdbcUrl2, usernameH2, passwordH2);
            // Creazione delle tabelle se non esistono
        /*    String createTableSQL = "CREATE TABLE IF NOT EXISTS Address_Senza_Prezzo  (address_chain VARCHAR(255) PRIMARY KEY, data VARCHAR(255))";
            PreparedStatement preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute();*/

            String createTableSQL = "CREATE TABLE IF NOT EXISTS Prezzo_ora_Address_Chain  (ora_address_chain VARCHAR(255) PRIMARY KEY, prezzo VARCHAR(255))";
            PreparedStatement preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute();

            createTableSQL = "CREATE TABLE IF NOT EXISTS USDTEUR  (data VARCHAR(255) PRIMARY KEY, prezzo VARCHAR(255))";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute();
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS XXXEUR  (dataSimbolo VARCHAR(255) PRIMARY KEY, prezzo VARCHAR(255))";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute(); 
            
             
            createTableSQL = "CREATE TABLE IF NOT EXISTS GESTITIBINANCE  (Coppia VARCHAR(255) PRIMARY KEY)";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute(); 
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS GESTITICOINGECKO  (Address_Chain VARCHAR(255) PRIMARY KEY, Simbolo VARCHAR(255), Nome VARCHAR (255))";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute(); 
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS GESTITICRYPTOHISTORY  (Symbol VARCHAR(255) PRIMARY KEY, Nome VARCHAR(255))";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute();
                       
            createTableSQL = "CREATE TABLE IF NOT EXISTS OPZIONI (Opzione VARCHAR(255) PRIMARY KEY, Valore VARCHAR(255))";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute();  
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS GIACENZEBLOCKCHAIN (Wallet_Blocco VARCHAR(255) PRIMARY KEY, Valore VARCHAR(255))";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute();
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS RINOMINATOKEN (address_chain VARCHAR(255) PRIMARY KEY, VecchioNome VARCHAR(255), NuovoNome VARCHAR(255))";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute(); 
            
            //Tabella che associa i Wallet ad un Gruppo per poter poi gestire correttamente i quadri RW
            createTableSQL = "CREATE TABLE IF NOT EXISTS WALLETGRUPPO  (Wallet VARCHAR(255) PRIMARY KEY, Gruppo VARCHAR(255))";
            preparedStatement = connectionPersonale.prepareStatement(createTableSQL);
            preparedStatement.execute();
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS EMONEY  (Moneta VARCHAR(255) PRIMARY KEY, Data VARCHAR(255))";
            preparedStatement = connectionPersonale.prepareStatement(createTableSQL);
            preparedStatement.execute();
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS OPZIONI (Opzione VARCHAR(255) PRIMARY KEY, Valore VARCHAR(255))";
            preparedStatement = connectionPersonale.prepareStatement(createTableSQL);
            preparedStatement.execute(); 
            
            successo=true;
            

            //DROP TABLE IF EXISTS " + tableName;
            /*  String insertSQL = "INSERT INTO AddressSenzaPrezzo (address_chain, data) VALUES (?, ?)";
            preparedStatement = connection.prepareStatement(insertSQL);
                preparedStatement.setString(1, "Mario");
                preparedStatement.setString(2, "Pippo");
                preparedStatement.executeUpdate();*/
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
            
        }
        return successo;
    }
    
    
    //Devo predisporre la cancellazione dei record dei prezzi nulli all'apertura del gestionale
    //DELETE FROM XXXEUR WHERE PREZZO='ND'
    //DELETE FROM XXXEUR WHERE PREZZO='null'
    public static void CancellaPrezziVuoti() {
        try {
            String SQL = "DELETE FROM XXXEUR WHERE PREZZO='ND'";
            PreparedStatement checkStatement = connection.prepareStatement(SQL);
            checkStatement.executeUpdate();
            SQL = "DELETE FROM XXXEUR WHERE PREZZO='null'";
            checkStatement = connection.prepareStatement(SQL);
            checkStatement.executeUpdate();

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        //Con questa query ritorno sia il vecchio che il nuovo nome
    }
    

        public static void Pers_Emoney_Scrivi(String Moneta, String Data) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT COUNT(*) FROM EMONEY WHERE Moneta = '" + Moneta + "'";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                String updateSQL = "UPDATE EMONEY SET Data = '" + Data + "' WHERE Moneta = '" + Moneta + "'";
                PreparedStatement updateStatement = connectionPersonale.prepareStatement(updateSQL);
                updateStatement.executeUpdate();               

            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = 
                    "INSERT INTO EMONEY (Moneta, Data ) VALUES ('" + Moneta + "','" + Data + "')";
                PreparedStatement insertStatement = connectionPersonale.prepareStatement(insertSQL);
                insertStatement.executeUpdate();

            }
            //Se aggiungo una riga al DB la aggiungo anche alla mappa di riferimento
            //Lavorare con le mappe risulta infatti + veloce del DB e uso quella come base per le ricerche
            CDC_Grafica.Mappa_EMoney.put(Moneta, Data);
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        
        public static void Pers_Emoney_Cancella(String Moneta) {
               //completamente da gestire
        try {
            String checkIfExistsSQL = "DELETE FROM EMONEY WHERE Moneta='"+Moneta+"'";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            checkStatement.executeUpdate();
            CDC_Grafica.Mappa_EMoney.remove(Moneta);

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        //Con questa query ritorno sia il vecchio che il nuovo nome
    }
        
        public static String Pers_Emoney_Leggi(String Moneta) {
        String Risultato = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Moneta,Data FROM EMONEY WHERE Moneta = '" + Moneta + "'";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("Data");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }

        return Risultato;
        //Con questa query ritorno sia il vecchio che il nuovo nome
    }
        
        public static void Pers_Emoney_PopolaMappaEmoney() {

        CDC_Grafica.Mappa_EMoney.clear();
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT * FROM EMONEY";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            //System.out.println(resultSet.getFetchSize());
            while (resultSet.next()) {
                String Moneta = resultSet.getString("Moneta"); 
                String Data = resultSet.getString("Data");
                CDC_Grafica.Mappa_EMoney.put(Moneta, Data);
                //System.out.println(Moneta);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }


        //Con questa query ritorno sia il vecchio che il nuovo nome
    }        
    
        public static void Pers_GruppoWallet_Scrivi(String Wallet, String Gruppo) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT COUNT(*) FROM WALLETGRUPPO WHERE Wallet = '" + Wallet + "'";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                String updateSQL = "UPDATE WALLETGRUPPO SET Gruppo = '" + Gruppo + "' WHERE Wallet = '" + Wallet + "'";
                PreparedStatement updateStatement = connectionPersonale.prepareStatement(updateSQL);
                updateStatement.executeUpdate();

            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = 
                    "INSERT INTO WALLETGRUPPO (Wallet, Gruppo ) VALUES ('" + Wallet + "','" + Gruppo + "')";
                PreparedStatement insertStatement = connectionPersonale.prepareStatement(insertSQL);
                insertStatement.executeUpdate();

            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        public static String Pers_GruppoWallet_Leggi(String Wallet) {
                String Risultato = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Wallet,Gruppo FROM WALLETGRUPPO WHERE Wallet = '" + Wallet + "'";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("Gruppo");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (Risultato==null){
            Pers_GruppoWallet_Scrivi(Wallet, "Wallet 01");
            Risultato="Wallet 01";
            }
        return Risultato;
        //Con questa query ritorno sia il vecchio che il nuovo nome
    }
        
    
        public static void RinominaToken_Scrivi(String address_chain, String VecchioNome,String NuovoNome) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT COUNT(*) FROM RINOMINATOKEN WHERE address_chain = '" + address_chain + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                String updateSQL = "UPDATE RINOMINATOKEN SET NuovoNome = '" + NuovoNome + "' WHERE address_chain = '" + address_chain + "'";
                PreparedStatement updateStatement = connection.prepareStatement(updateSQL);
                updateStatement.executeUpdate();

            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = 
                    "INSERT INTO RINOMINATOKEN (address_chain, VecchioNome,NuovoNome ) VALUES ('" + address_chain + "','" + VecchioNome + "','" +NuovoNome+ "')";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                insertStatement.executeUpdate();

            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        
        public static String[] RinominaToken_Leggi(String address_chain) {
                String Risultato[] = new String[2];
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT address_chain,VecchioNome,NuovoNome FROM RINOMINATOKEN WHERE address_chain = '" + address_chain + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato[0] = resultSet.getString("VecchioNome");
                Risultato[1] = resultSet.getString("NuovoNome");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
        //Con questa query ritorno sia il vecchio che il nuovo nome
    }
        
        public static String GiacenzeWalletMonetaBlockchain_Leggi(String wallet_blocco) {
                String Valore = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Valore FROM GIACENZEBLOCKCHAIN WHERE Wallet_Blocco = '" + wallet_blocco + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Valore = resultSet.getString("Valore");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Valore;
        //Con questa query ritorno sia il vecchio che il nuovo nome
    }    
        
        public static void GiacenzeWalletMonetaBlockchain_Scrivi(String wallet_blocco, String Valore) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT COUNT(*) FROM GIACENZEBLOCKCHAIN WHERE Wallet_Blocco = '" + wallet_blocco + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                String updateSQL = "UPDATE GIACENZEBLOCKCHAIN SET Valore = '" + Valore + "' WHERE Wallet_Blocco = '" + wallet_blocco + "'";
                PreparedStatement updateStatement = connection.prepareStatement(updateSQL);
                updateStatement.executeUpdate();
            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO GIACENZEBLOCKCHAIN (Wallet_Blocco, Valore) VALUES ('" + wallet_blocco + "','" + Valore + "')";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    } 
        
        public static Map<String, String> RinominaToken_LeggiTabella() {
        Map<String, String> Mappa_NomiToken = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT * FROM RINOMINATOKEN";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            while (resultSet.next()) {
                String Nome = resultSet.getString("NuovoNome");
                String ID = resultSet.getString("address_chain");
                Mappa_NomiToken.put(ID, Nome);
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Mappa_NomiToken;
        //Con questa query ritorno sia il vecchio che il nuovo nome
    }
        
        public static void RinominaToken_CancellaRiga(String address_chain) {
               //completamente da gestire
        try {
            String checkIfExistsSQL = "DELETE FROM RINOMINATOKEN WHERE address_chain='"+address_chain+"'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.executeUpdate();

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        //Con questa query ritorno sia il vecchio che il nuovo nome
    }
    
 /*   public static void AddressSenzaPrezzo_Scrivi(String address_chain, String data) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT COUNT(*) FROM Address_Senza_Prezzo WHERE address_chain = '" + address_chain + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                String updateSQL = "UPDATE Address_Senza_Prezzo SET data = '" + data + "' WHERE address_chain = '" + address_chain + "'";
                PreparedStatement updateStatement = connection.prepareStatement(updateSQL);
                // updateStatement.setString(1, data);
                //updateStatement.setString(2, address_chain);
                updateStatement.executeUpdate();

            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO Address_Senza_Prezzo (address_chain, data) VALUES ('" + address_chain + "','" + data + "')";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                //insertStatement.setString(1, address_chain);
                //insertStatement.setString(2, data);
                insertStatement.executeUpdate();

            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }*/

 /*   public static String AddressSenzaPrezzo_Leggi(String address_chain) {
        String Risultato = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT address_chain,data FROM Address_Senza_Prezzo WHERE address_chain = '" + address_chain + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("data");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    }*/

    public static String PrezzoAddressChain_Leggi(String ora_address_chain) {
        String Risultato = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT ora_address_chain,prezzo FROM Prezzo_ora_Address_Chain WHERE ora_address_chain = '" + ora_address_chain + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("prezzo");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    }

    public static void PrezzoAddressChain_Scrivi(String ora_address_chain, String prezzo) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT COUNT(*) FROM Prezzo_ora_Address_Chain WHERE ora_address_chain = '" + ora_address_chain + "'";
            //System.out.println(checkIfExistsSQL);
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            //checkStatement.setString(1, address_chain);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                String updateSQL = "UPDATE Prezzo_ora_Address_Chain SET prezzo = '" + prezzo + "' WHERE ora_address_chain = '" + ora_address_chain + "'";
                PreparedStatement updateStatement = connection.prepareStatement(updateSQL);
                // updateStatement.setString(1, data);
                //updateStatement.setString(2, address_chain);
                updateStatement.executeUpdate();
                // System.out.println("Riga aggiornata con successo.");

            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO Prezzo_ora_Address_Chain (ora_address_chain, prezzo) VALUES ('" + ora_address_chain + "','" + prezzo + "')";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                //insertStatement.setString(1, address_chain);
                //insertStatement.setString(2, data);
                insertStatement.executeUpdate();
                // System.out.println("Nuova riga inserita con successo.");

            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String USDTEUR_Leggi(String data) {
        String Risultato = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT data,prezzo FROM USDTEUR WHERE data = '" + data + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("prezzo");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    }

    public static void USDTEUR_Scrivi(String data, String prezzo) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT COUNT(*) FROM USDTEUR WHERE data = '" + data + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                String updateSQL = "UPDATE USDTEUR SET prezzo = '" + prezzo + "' WHERE data = '" + data + "'";
                PreparedStatement updateStatement = connection.prepareStatement(updateSQL);
                updateStatement.executeUpdate();
            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO USDTEUR (data, prezzo) VALUES ('" + data + "','" + prezzo + "')";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
       public static String XXXEUR_Leggi(String dataSimbolo) {
        String Risultato = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT dataSimbolo,prezzo FROM XXXEUR WHERE dataSimbolo = '" + dataSimbolo + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("prezzo");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    }

    public static void XXXEUR_Scrivi(String dataSimbolo, String prezzo) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT COUNT(*) FROM XXXEUR WHERE dataSimbolo = '" + dataSimbolo + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                String updateSQL = "UPDATE XXXEUR SET prezzo = '" + prezzo + "' WHERE dataSimbolo = '" + dataSimbolo + "'";
                PreparedStatement updateStatement = connection.prepareStatement(updateSQL);
                updateStatement.executeUpdate();
            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO XXXEUR (dataSimbolo, prezzo) VALUES ('" + dataSimbolo + "','" + prezzo + "')";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    } 
    
    
        public static String CoppieBinance_Leggi(String Coppia) {
        String Risultato = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Coppia FROM GESTITIBINANCE WHERE Coppia = '" + Coppia + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("Coppia");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    }
        
            public static String GestitiCoingecko_Leggi(String Gestito) {
        String Risultato = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Address_Chain FROM GESTITICOINGECKO WHERE Address_Chain = '" + Gestito.toUpperCase() + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("Address_Chain");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    }  
            
        public static String[] GestitiCoingecko_LeggiInteraRiga(String Gestito) {
        String Risultato[]=new String[3];
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Address_Chain,Simbolo,Nome FROM GESTITICOINGECKO WHERE Address_Chain = '" + Gestito.toUpperCase() + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato[0] = resultSet.getString("Address_Chain");
                Risultato[1] = resultSet.getString("Simbolo");
                Risultato[2] = resultSet.getString("Nome");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    } 
                        
        public static String GestitiCryptohistory_Leggi(String Gestito) {
        String Risultato = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Symbol FROM GESTITICRYPTOHISTORY WHERE Symbol = '" + Gestito + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("Symbol");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    } 
    public static String[] GestitiCryptohistory_LeggiInteraRiga(String Gestito) {
        String Risultato[] = new String[2];
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Symbol,Nome FROM GESTITICRYPTOHISTORY WHERE Symbol = '" + Gestito + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato[0] = resultSet.getString("Symbol");
                Risultato[1] = resultSet.getString("Nome");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    } 
        public static void CoppieBinance_ScriviNuovaTabella(List<String> Coppie) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "DROP TABLE IF EXISTS GESTITIBINANCE";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.execute();
            checkIfExistsSQL = "CREATE TABLE IF NOT EXISTS GESTITIBINANCE  (Coppia VARCHAR(255) PRIMARY KEY)";
            checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.execute(); 
            for (String coppia:Coppie){
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO GESTITIBINANCE (Coppia) VALUES ('" + coppia + "')";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    } 
        public static void GestitiCoingecko_ScriviNuovaTabella(List<String[]> Gestiti) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "DROP TABLE IF EXISTS GESTITICOINGECKO";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.execute();
            checkIfExistsSQL = "CREATE TABLE IF NOT EXISTS GESTITICOINGECKO  (Address_Chain VARCHAR(255) PRIMARY KEY, Simbolo VARCHAR(255), Nome VARCHAR (255))";
            checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.execute(); 
            for (String[] gestito:Gestiti){
                // La riga non esiste, esegui l'inserimento
                gestito[1]=gestito[1].replace("'", "");
                gestito[2]=gestito[2].replace("'", "");
                gestito[2]=gestito[2].split("\\(")[0].trim();//questo serve per eliminare i nomi delle chain tra parentesi nel nome qualora vi fosse
                String insertSQL = "INSERT INTO GESTITICOINGECKO (Address_Chain,Simbolo,Nome) VALUES ('" + gestito[0] + "','"+ gestito[1] + "','" + gestito[2] + "')";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        
        public static void GestitiCryptohistory_ScriviNuovaTabella(List<String[]> Gestiti) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "DROP TABLE IF EXISTS GESTITICRYPTOHISTORY";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.execute();
            checkIfExistsSQL = "CREATE TABLE IF NOT EXISTS GESTITICRYPTOHISTORY  (Symbol VARCHAR(255) PRIMARY KEY, Nome VARCHAR(255))";
            checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.execute(); 
            for (String gestito[]:Gestiti){
                // La riga non esiste, esegui l'inserimento
                gestito[1]=gestito[1].replace("'", "").replace("*", "");
                String insertSQL = "INSERT INTO GESTITICRYPTOHISTORY (Symbol,Nome) VALUES ('" + gestito[0] + "','"+ gestito[1] + "')";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        
        public static String Opzioni_Leggi(String Opzione) {
        String Risultato = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Opzione,valore FROM OPZIONI WHERE Opzione = '" + Opzione + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("valore");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    }
    
    public static String Pers_Opzioni_Leggi(String Opzione) {
        String Risultato = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Opzione,valore FROM OPZIONI WHERE Opzione = '" + Opzione + "'";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("valore");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    }
        
        public static void Opzioni_Scrivi(String Opzione, String Valore) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT COUNT(*) FROM OPZIONI WHERE Opzione = '" + Opzione + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                String updateSQL = "UPDATE OPZIONI SET Valore = '" + Valore + "' WHERE Opzione = '" + Opzione + "'";
                PreparedStatement updateStatement = connection.prepareStatement(updateSQL);
                updateStatement.executeUpdate();
            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO OPZIONI (Opzione, Valore) VALUES ('" + Opzione + "','" + Valore + "')";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    } 
    
                public static void Pers_Opzioni_Scrivi(String Opzione, String Valore) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT COUNT(*) FROM OPZIONI WHERE Opzione = '" + Opzione + "'";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                String updateSQL = "UPDATE OPZIONI SET Valore = '" + Valore + "' WHERE Opzione = '" + Opzione + "'";
                PreparedStatement updateStatement = connectionPersonale.prepareStatement(updateSQL);
                updateStatement.executeUpdate();
            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO OPZIONI (Opzione, Valore) VALUES ('" + Opzione + "','" + Valore + "')";
                PreparedStatement insertStatement = connectionPersonale.prepareStatement(insertSQL);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
