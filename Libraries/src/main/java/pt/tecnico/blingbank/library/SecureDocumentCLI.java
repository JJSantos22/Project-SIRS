package pt.tecnico.blingbank.library;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import pt.tecnico.blingbank.library.exceptions.InvalidSignatureException;
import pt.tecnico.blingbank.library.exceptions.OutdatedSignatureException;

public class SecureDocumentCLI {

    private static final String HELP = "help";
    private static final String PROTECT = "protect";
    private static final String CHECK = "check";
    private static final String UNPROTECT = "unprotect";
    private static final String PROTECTMAC = "protectWithMac";
    private static final String CHECKMAC = "checkMac";
    private static final String UNPROTECTMAC = "unprotectWithMac";

    public static void parse(String[] args) {
        if (args.length < 1) {
            help();
            return;
        }

        try {
            switch (args[0]) {
                case HELP:
                    help();
                    break;
                case PROTECT:
                    if (args.length != 5) {
                        System.out.println("Error: protect requires 4 arguments.");
                        return;
                    }
                    protect(args[1], args[2], args[3], args[4]);
                    break;
                case CHECK:
                    if (args.length != 4) {
                        System.out.println("Error: check requires 3 arguments.");
                        return;
                    }
                    check(args[1], args[2], args[3]);
                    break;
                case UNPROTECT:
                    if (args.length != 5) {
                        System.out.println("Error: unprotect requires 4 arguments.");
                        return;
                    }
                    unprotect(args[1], args[2], args[3], args[4]);
                    break;
                case PROTECTMAC:
                    if (args.length != 4) {
                        System.out.println("Error: protect requires 3 arguments.");
                        return;
                    }
                    protectWithMac(args[1], args[2], args[3]);
                    break;
                case CHECKMAC:
                    if (args.length != 3) {
                        System.out.println("Error: check requires 2 arguments.");
                        return;
                    }
                    checkWithMac(args[1], args[2]);
                    break;
                case UNPROTECTMAC:
                    if (args.length != 4) {
                        System.out.println("Error: unprotect requires 3 arguments.");
                        return;
                    }
                    unprotectWithMac(args[1], args[2], args[3]);
                    break;
                default:
                    System.out.println("Error: unknown command '" + args[0] + "'.");
                    break;
            }
        } catch (IOException | GeneralSecurityException | InvalidSignatureException | OutdatedSignatureException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void help() {
        System.out.println("Usage: BlingBank <command>\n" +
                "Commands:\n" +
                "help                                                             - Show this message. \n \n" +
                "protect <input-file> <output-file> <private-key> <secret-key>    - Protects the specified file with the\n"
                +
                "                                                                   specified keys into an output file. \n\n"
                +
                "check <input-file> <public-key> <secret-key>                     - Checks if the specified file is \n"
                +
                "                                                                   protected with the specified keys. \n\n"
                +
                "unprotect <input-file> <output-file> <public-key> <secret-key>  - Unprotects the specified file with the \n"
                +
                "                                                                   specified keys into an output file. \n\n"
                +
                "protectWithMac <input-file> <output-file> <secret-key>         - Protects the specified file with the\n"
                +
                "                                                                   key into an output file. \n\n"
                +
                "checkMac <input-file> <secret-key>                             - Checks if the specified file is \n"
                +
                "                                                                   protected with the specified keys. \n\n"
                +
                "unprotectWithMac <input-file> <output-file> <secret-key>       - Unprotects the specified file with the \n"
                +
                "                                                                   keys into an output file. \n");
    }

    private static void protect(String inputFile, String outputFile, String privateKeyFile, String secretKeyFile)
            throws FileNotFoundException, IOException, GeneralSecurityException {
        byte[] data = SecureDocumentUtils.readFile(inputFile);

        // Write JSON object to file
        try (FileWriter fileWriter = new FileWriter(outputFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            SecretKey secretKey = SecureDocumentUtils.readSecretKey(secretKeyFile);
            PrivateKey privateKey = SecureDocumentUtils.readPrivateKey(privateKeyFile);
            gson.toJson(SecureDocument.protect(data, privateKey, secretKey), fileWriter);
        }
    }

    private static void check(String inputFile, String publicKeyFile, String secretKeyFile)
            throws InvalidKeyException, FileNotFoundException, InvalidKeySpecException, IllegalBlockSizeException,
            BadPaddingException, IOException {
        try {
            unprotect(inputFile, "", publicKeyFile, secretKeyFile);
            System.out.println("The file is authentic and fresh");
        } catch (InvalidSignatureException e) {
            System.out.println("The file not authentic and fresh");
        } catch (OutdatedSignatureException e) {
            System.out.println("The file is authentic but not fresh");
        }
    }

    private static void unprotect(String inputFile, String outputFile, String publicKeyFile, String secretKeyFile)
            throws FileNotFoundException, IOException, InvalidKeySpecException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, InvalidSignatureException, OutdatedSignatureException {
        // Read JSON object from file
        try (FileReader fileReader = new FileReader(inputFile)) {
            Gson gson = new Gson();
            JsonObject rootJson = gson.fromJson(fileReader, JsonObject.class);

            if (!outputFile.equals("")) {
                FileOutputStream document = new FileOutputStream(outputFile);
                SecretKey secretKey = SecureDocumentUtils.readSecretKey(secretKeyFile);
                PublicKey publicKey = SecureDocumentUtils.readPublicKey(publicKeyFile);
                document.write(SecureDocument.unprotect(rootJson, publicKey, secretKey, 60000));
                document.close();
            }
        }
    }

    private static void protectWithMac(String inputFile, String outputFile, String secretKeyFile)
            throws FileNotFoundException, IOException, GeneralSecurityException {
        byte[] data = SecureDocumentUtils.readFile(inputFile);
        SecretKey secretKey = SecureDocumentUtils.readSecretKey(secretKeyFile);

        // Write JSON object to file
        try (FileWriter fileWriter = new FileWriter(outputFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(SecureDocument.protect(data, secretKey), fileWriter);
        }
    }
    
    private static void checkWithMac(String inputFile, String secretKeyFile)
            throws InvalidKeyException, FileNotFoundException, InvalidKeySpecException, IllegalBlockSizeException,
            BadPaddingException, IOException {
        try {
            unprotectWithMac(inputFile, "", secretKeyFile);
            System.out.println("The file is authentic and fresh");
        } catch (InvalidSignatureException e) {
            System.out.println("The file not authentic and fresh");
        } catch (OutdatedSignatureException e) {
            System.out.println("The file is authentic but not fresh");
        }
    }
    
    private static void unprotectWithMac(String inputFile, String outputFile, String secretKeyFile)
            throws FileNotFoundException, IOException, InvalidKeySpecException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, InvalidSignatureException, OutdatedSignatureException {
        // Read JSON object from file
        try (FileReader fileReader = new FileReader(inputFile)) {
            Gson gson = new Gson();
            JsonObject rootJson = gson.fromJson(fileReader, JsonObject.class);

            if (!outputFile.equals("")) {
                FileOutputStream document = new FileOutputStream(outputFile);
                SecretKey secretKey = SecureDocumentUtils.readSecretKey(secretKeyFile);
                document.write(SecureDocument.unprotect(rootJson, secretKey, 60000));
                document.close();
            }
        }
    }

}
