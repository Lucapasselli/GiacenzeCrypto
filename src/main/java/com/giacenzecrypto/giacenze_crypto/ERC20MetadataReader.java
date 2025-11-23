/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

  /*      <dependency>
            <groupId>org.web3j</groupId>
            <artifactId>core</artifactId>
            <version>4.14.0</version>
            <type>jar</type>
        </dependency>*/

//MOMENTANEAMENTE CLASSE DISABILITATA LA RIATTIVERO IN UN SECONDO MOMENTO
//LA CLASSE INTERROGAVA LA BLOCKCHAIN PER TROVARE NOME TOKEN, SYMBOL E DECIMALS DA UN DETERMINATO TOKEN ADDRESS SU UNA CERTA RETE EVM


/**
 *
 * @author lucap
 */
/*import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.abi.datatypes.Function;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.http.HttpService;

import java.util.Collections;
import java.util.List;
import java.math.BigInteger;*/

public class ERC20MetadataReader {

 /*   private final Web3j web3j;

    public ERC20MetadataReader(String rpcEndpoint) {
        web3j = Web3j.build(new HttpService(rpcEndpoint));
    }

    public String callStringFunction(String contractAddress, String functionName) throws Exception {
        Function function = new Function(functionName,
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<Utf8String>() {}));

        String encodedFunction = org.web3j.abi.FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST).send();

        List<Type> someTypes = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());

        if (someTypes.isEmpty()) return null;
        return someTypes.get(0).getValue().toString();
    }

    public BigInteger callUint8Function(String contractAddress, String functionName) throws Exception {
        Function function = new Function(functionName,
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<Uint8>() {}));

        String encodedFunction = org.web3j.abi.FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST).send();

        List<Type> someTypes = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());

        if (someTypes.isEmpty()) return null;
        return (BigInteger) someTypes.get(0).getValue();
    }

    public void printTokenMetadata(String contractAddress) throws Exception {
        String name = callStringFunction(contractAddress, "name");
        String symbol = callStringFunction(contractAddress, "symbol");
        BigInteger decimalsBI = callUint8Function(contractAddress, "decimals");
        Integer decimals = decimalsBI == null ? null : decimalsBI.intValue();

        System.out.println("Token address: " + contractAddress);
        System.out.println("Name: " + (name != null ? name : "N/A"));
        System.out.println("Symbol: " + (symbol != null ? symbol : "N/A"));
        System.out.println("Decimals: " + (decimals != null ? decimals : "N/A"));
    }

    public static void main(String[] args) throws Exception {
        String rpc = "https://bsc-dataseed.binance.org/"; // Endpoint RPC della rete BSC
        String tokenAddress = "0xe9e7cea3dedca5984780bafc599bd69add087d56"; // Esempio: BUSD token

        ERC20MetadataReader reader = new ERC20MetadataReader(rpc);
        reader.printTokenMetadata(tokenAddress);
    }*/
}

