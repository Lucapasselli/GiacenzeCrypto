/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package giacenze_crypto.com;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author luca.passelli
 */
public class DatabaseH2 {
    static String jdbcUrl = "jdbc:h2:./test";
    static String usernameH2 = "sa";
    static String passwordH2 = "";
    static Connection connection;
    //per compattare database comando -> SHUTDOWN COMPACT //da valutare quando farlo
    public static void CreaoCollegaDatabase(){
            try {
            connection = DriverManager.getConnection(jdbcUrl, usernameH2, passwordH2);
            // Creazione delle tabelle se non esistono
            String createTableSQL = "CREATE TABLE IF NOT EXISTS Address_Senza_Prezzo  (address_chain VARCHAR(255) PRIMARY KEY, data VARCHAR(255))";
            PreparedStatement preparedStatement = connection.prepareStatement(createTableSQL); 
            preparedStatement.execute();
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS Prezzo_ora_Address_Chain  (ora_address_chain VARCHAR(255) PRIMARY KEY, prezzo VARCHAR(255))";
            preparedStatement = connection.prepareStatement(createTableSQL); 
            preparedStatement.execute();
            
          //DROP TABLE IF EXISTS " + tableName;
                
          /*  String insertSQL = "INSERT INTO AddressSenzaPrezzo (address_chain, data) VALUES (?, ?)";
            preparedStatement = connection.prepareStatement(insertSQL);
                preparedStatement.setString(1, "Mario");
                preparedStatement.setString(2, "Pippo");
                preparedStatement.executeUpdate();*/
            
    }   catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void AddressSenzaPrezzo_Scrivi(String address_chain, String data) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT COUNT(*) FROM Address_Senza_Prezzo WHERE address_chain = '"+address_chain+"'";
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
                String updateSQL = "UPDATE Address_Senza_Prezzo SET data = '"+data+"' WHERE address_chain = '"+address_chain+"'";
                PreparedStatement updateStatement = connection.prepareStatement(updateSQL);
               // updateStatement.setString(1, data);
                //updateStatement.setString(2, address_chain);
                updateStatement.executeUpdate();
               // System.out.println("Riga aggiornata con successo.");
                
            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO Address_Senza_Prezzo (address_chain, data) VALUES ('"+address_chain+"','"+data+"')";
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
    
    public static String AddressSenzaPrezzo_Leggi(String address_chain) {
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
    }
    
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
            String checkIfExistsSQL = "SELECT COUNT(*) FROM Prezzo_ora_Address_Chain WHERE ora_address_chain = '"+ora_address_chain+"'";
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
                String updateSQL = "UPDATE Prezzo_ora_Address_Chain SET prezzo = '"+prezzo+"' WHERE ora_address_chain = '"+ora_address_chain+"'";
                PreparedStatement updateStatement = connection.prepareStatement(updateSQL);
               // updateStatement.setString(1, data);
                //updateStatement.setString(2, address_chain);
                updateStatement.executeUpdate();
               // System.out.println("Riga aggiornata con successo.");
                
            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO Prezzo_ora_Address_Chain (ora_address_chain, prezzo) VALUES ('"+ora_address_chain+"','"+prezzo+"')";
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
    
}
