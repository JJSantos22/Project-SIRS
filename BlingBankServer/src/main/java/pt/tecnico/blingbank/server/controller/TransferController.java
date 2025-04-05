package pt.tecnico.blingbank.server.controller;

import static pt.tecnico.blingbank.server.exceptions.ErrorMessage.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import pt.tecnico.blingbank.library.KeystoreService;
import pt.tecnico.blingbank.library.SecureDocument;
import pt.tecnico.blingbank.library.SecureDocumentUtils;
import pt.tecnico.blingbank.library.exceptions.InvalidSignatureException;
import pt.tecnico.blingbank.library.exceptions.OutdatedSignatureException;
import pt.tecnico.blingbank.server.domain.AccountHolder;
import pt.tecnico.blingbank.server.exceptions.ServerException;
import pt.tecnico.blingbank.server.repository.AccountHolderRepository;
import pt.tecnico.blingbank.server.service.TransferService;

@RestController
public class TransferController {

    @Autowired
    private TransferService transferService;

    private static final String keystoreType = "PKCS12";
    private static final String keystorePath = "src/main/resources/server.p12";
    private static final String keystorePassword = "server";

    private KeystoreService keystoreService = new KeystoreService(keystoreType, keystorePath, keystorePassword);

    @Autowired
    AccountHolderRepository accountHolderRepository;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PutMapping("users/{userId}/transfers/register")
    String registerTransfer(@PathVariable Integer userId, @RequestBody String json) {
        if (userId == null)
            throw new ServerException(INVALID_REQUEST);
        AccountHolder accountHolder = accountHolderRepository.findById(userId).orElseThrow(() -> new ServerException(UNKNOWN_ACCOUNT_HOLDER));

        Gson gson = new Gson();
        JsonObject rootJson = gson.fromJson(json, JsonObject.class);
        
        try {
            rootJson = gson.fromJson(new String(SecureDocument.unprotect(rootJson, accountHolder.getPublicKey(), keystoreService.loadSecretKeyFromKeystore(Integer.toString(userId)+"_Session_Key", accountHolder.getPassword()), SecureDocumentUtils.DEFAULT_TIMESTAMP_VALIDITY)), JsonObject.class);
        } catch (JsonSyntaxException | InvalidKeyException | InvalidKeySpecException | IllegalBlockSizeException
                | BadPaddingException | IOException | InvalidSignatureException | OutdatedSignatureException e) {
            throw new ServerException(INVALID_REQUEST);
        }

        if (!rootJson.has("origin")
                || !rootJson.get("origin").isJsonPrimitive()
                || !rootJson.get("origin").getAsJsonPrimitive().isNumber()) {
            throw new ServerException(INVALID_ORIGIN_ACCOUNT);
        }
        if (!rootJson.has("destination")
                || !rootJson.get("destination").isJsonPrimitive()
                || !rootJson.get("destination").getAsJsonPrimitive().isNumber()) {
            throw new ServerException(INVALID_DESTINATION_ACCOUNT);
        }
        if (!rootJson.has("amount")
                || !rootJson.get("amount").isJsonPrimitive()
                || !rootJson.get("amount").getAsJsonPrimitive().isNumber()) {
            throw new ServerException(INVALID_AMOUNT);
        }
        if (!rootJson.has("description")
                || !rootJson.get("description").isJsonPrimitive()
                || !rootJson.get("description").getAsJsonPrimitive().isString()) {
            throw new ServerException(INVALID_DESCRIPTION);
        }

        rootJson = transferService.registerTransfer(
                userId,
                rootJson.get("origin").getAsInt(),
                rootJson.get("destination").getAsInt(),
                rootJson.get("amount").getAsInt(),
                rootJson.get("description").getAsString());
        

        try {
            return SecureDocument.protect(rootJson.toString().getBytes(), keystoreService.loadSecretKeyFromKeystore(Integer.toString(userId)+"_Session_Key", accountHolder.getPassword())).toString();
        } catch (IOException | GeneralSecurityException e) {
            throw new ServerException(PROBLEM_SERVER_ERROR);
        }
    }

    @PostMapping("users/{userId}/transfers/confirm")
    String confirmTransfer(@PathVariable Integer userId, @RequestBody String json) {
        if (userId == null)
            throw new ServerException(INVALID_REQUEST);
        
        AccountHolder accountHolder = accountHolderRepository.findById(userId).orElseThrow(() -> new ServerException(UNKNOWN_ACCOUNT_HOLDER));

        Gson gson = new Gson();
        JsonObject rootJson = gson.fromJson(json, JsonObject.class);

        if (!rootJson.has("signature")
                || !rootJson.get("signature").isJsonPrimitive()
                || !rootJson.get("signature").getAsJsonPrimitive().isString()) {
            throw new ServerException(INVALID_SIGNATURE);
        }
        String signature = rootJson.get("signature").getAsString();

        if (!rootJson.has("timestamp")
                || !rootJson.get("timestamp").isJsonPrimitive()
                || !rootJson.get("timestamp").getAsJsonPrimitive().isNumber()) {
            throw new ServerException(INVALID_SIGNATURE);
        }
        long timestamp = rootJson.get("timestamp").getAsLong();

        try {
            rootJson = gson.fromJson(new String(SecureDocument.unprotect(rootJson, accountHolder.getPublicKey(), keystoreService.loadSecretKeyFromKeystore(Integer.toString(userId)+"_Session_Key", accountHolder.getPassword()), SecureDocumentUtils.DEFAULT_TIMESTAMP_VALIDITY)), JsonObject.class);
        } catch (JsonSyntaxException | InvalidKeyException | InvalidKeySpecException | IllegalBlockSizeException
                | BadPaddingException | IOException | InvalidSignatureException | OutdatedSignatureException e) {
            throw new ServerException(INVALID_REQUEST);
        }

        if (!rootJson.has("transfer")
                || !rootJson.get("transfer").isJsonPrimitive()
                || !rootJson.get("transfer").getAsJsonPrimitive().isNumber()) {
            throw new ServerException(INVALID_TRANSFER_ID);
        }
        rootJson = transferService.confirmTransfer(rootJson.get("transfer").getAsInt(), userId, timestamp, signature);

        try {
            return SecureDocument.protect(rootJson.toString().getBytes(), keystoreService.loadSecretKeyFromKeystore(Integer.toString(userId)+"_Session_Key", accountHolder.getPassword())).toString();
        } catch (IOException | GeneralSecurityException e) {
            throw new ServerException(PROBLEM_SERVER_ERROR);
        }
    }

    @PostMapping("users/{userId}/transfers/cancel")
    String cancelTransfer(@PathVariable Integer userId, @RequestBody String json) {
        if (userId == null)
            throw new ServerException(INVALID_REQUEST);

        AccountHolder accountHolder = accountHolderRepository.findById(userId).orElseThrow(() -> new ServerException(UNKNOWN_ACCOUNT_HOLDER));

        Gson gson = new Gson();
        JsonObject rootJson = gson.fromJson(json, JsonObject.class);

        try {
            rootJson = gson.fromJson(new String(SecureDocument.unprotect(rootJson, accountHolder.getPublicKey(), keystoreService.loadSecretKeyFromKeystore(Integer.toString(userId)+"_Session_Key", accountHolder.getPassword()), SecureDocumentUtils.DEFAULT_TIMESTAMP_VALIDITY)), JsonObject.class);
        } catch (JsonSyntaxException | InvalidKeyException | InvalidKeySpecException | IllegalBlockSizeException
                | BadPaddingException | IOException | InvalidSignatureException | OutdatedSignatureException e) {
            throw new ServerException(INVALID_REQUEST);
        }

        if (!rootJson.has("transfer")
                || !rootJson.get("transfer").isJsonPrimitive()
                || !rootJson.get("transfer").getAsJsonPrimitive().isNumber()) {
            throw new ServerException(INVALID_TRANSFER_ID);
        }
        rootJson = transferService.cancelTransfer(rootJson.get("transfer").getAsInt(), userId);

        try {
            return SecureDocument.protect(rootJson.toString().getBytes(), keystoreService.loadSecretKeyFromKeystore(Integer.toString(userId)+"_Session_Key", accountHolder.getPassword())).toString();
        } catch (IOException | GeneralSecurityException e) {
            throw new ServerException(PROBLEM_SERVER_ERROR);
        }
    }
}
