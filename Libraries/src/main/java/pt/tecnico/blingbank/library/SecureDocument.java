package pt.tecnico.blingbank.library;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

import com.google.gson.*;

import pt.tecnico.blingbank.library.exceptions.InvalidSignatureException;
import pt.tecnico.blingbank.library.exceptions.OutdatedSignatureException;

/**
 * Encryption
 */
public class SecureDocument {

    public static void main(String[] args) {
        SecureDocumentCLI.parse(args);
    }

    /**
     * Protects the document ensuring confidenciality and authenticity (used in
     * server)
     * 
     * @param input
     * @param privateKeyFile of the sender
     * @param secretKeyFile  of both
     * @return a JSON object with the protected document
     * @throws FileNotFoundException
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static JsonObject protect(byte[] input, PrivateKey privateKey, SecretKey secretKey)
            throws FileNotFoundException, IOException, GeneralSecurityException {

        String value = SecureDocumentUtils.encryptDocument(input, secretKey);

        long timestamp = System.currentTimeMillis();

        byte[][] toSign = { input, Long.toString(timestamp).getBytes() };
        String signature = SecureDocumentUtils.sign(toSign, privateKey);

        JsonObject json = new JsonObject();

        json.addProperty("value", value);
        json.addProperty("timestamp", timestamp);
        json.addProperty("signature", signature);

        return json;
    }

    public static JsonObject protect(byte[] input, SecretKey secretKey)
            throws FileNotFoundException, IOException, GeneralSecurityException {

        String value = SecureDocumentUtils.encryptDocument(input, secretKey);

        long timestamp = System.currentTimeMillis();

        byte[][] toMac = { input, Long.toString(timestamp).getBytes() };
        String mac = SecureDocumentUtils.createMAC(toMac, secretKey);

        JsonObject json = new JsonObject();

        json.addProperty("value", value);
        json.addProperty("timestamp", timestamp);
        json.addProperty("mac", mac);

        return json;
    }

    /**
     * Unprotects the document (used in server)
     * 
     * @param input
     * @param publicKeyFile of the sender
     * @param secretKeyFile of both
     * @return the unprotected document
     * @throws FileNotFoundException
     * @throws IOException
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidSignatureException  if the file was not authentic
     * @throws OutdatedSignatureException
     */
    public static byte[] unprotect(JsonObject input, PublicKey publicKey, SecretKey secretKey, long timestampLimit)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidSignatureException,
            OutdatedSignatureException {

        String value = input.get("value").getAsString();
        long timestamp = input.get("timestamp").getAsLong();
        String signature = input.get("signature").getAsString();

        byte[] data = SecureDocumentUtils.unencryptDocument(value, secretKey);

        byte[][] toVerify = { data, Long.toString(timestamp).getBytes() };
        if (!SecureDocumentUtils.verifySignature(toVerify, publicKey, signature)) {
            throw new InvalidSignatureException();
        }

        if (timestampLimit > 0 && System.currentTimeMillis() - timestamp > timestampLimit) {
            throw new OutdatedSignatureException(System.currentTimeMillis() - timestamp);
        }

        return data;
    }

    public static byte[] unprotect(JsonObject input, SecretKey secretKey, long timestampLimit)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidSignatureException,
            OutdatedSignatureException {

        String value = input.get("value").getAsString();
        long timestamp = input.get("timestamp").getAsLong();
        String mac = input.get("mac").getAsString();

        byte[] data = SecureDocumentUtils.unencryptDocument(value, secretKey);

        byte[][] toVerify = { data, Long.toString(timestamp).getBytes() };
        if (!SecureDocumentUtils.verifyMAC(toVerify, secretKey, mac)) {
            throw new InvalidSignatureException();
        }

        if (timestampLimit > 0 && System.currentTimeMillis() - timestamp > timestampLimit) {
            throw new OutdatedSignatureException(System.currentTimeMillis() - timestamp);
        }

        return data;
    }

}
