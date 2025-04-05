package pt.tecnico.blingbank.server.controller;

import static pt.tecnico.blingbank.server.exceptions.ErrorMessage.*;

import java.io.IOException;
import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
import pt.tecnico.blingbank.server.exceptions.ServerException;
import pt.tecnico.blingbank.server.service.AccountHolderService;

@RestController
public class AccountHolderController {

    @Autowired
    private AccountHolderService accountHolderService;
    
    private static final String keystoreType = "PKCS12";
    private static final String keystorePath = "src/main/resources/server.p12";
    private static final String keystorePassword = "server";

    private KeystoreService keystoreService = new KeystoreService(keystoreType, keystorePath, keystorePassword);

    private static final String initial_key_alias = "0";
    private static final String initial_key_password = "server";

    public AccountHolderController(AccountHolderService accountHolderService) {
        this.accountHolderService = accountHolderService;
    }

    @PutMapping("users/register")
    String registerAccountHolder(@RequestBody String json) {
        Gson gson = new Gson();
        JsonObject rootJson = gson.fromJson(json, JsonObject.class);
        
        try {
            rootJson = gson.fromJson(new String(SecureDocument.unprotect(rootJson, keystoreService.loadSecretKeyFromKeystore(initial_key_alias, initial_key_password), SecureDocumentUtils.DEFAULT_TIMESTAMP_VALIDITY)), JsonObject.class);
        } catch (JsonSyntaxException | InvalidKeyException | IllegalBlockSizeException
                | BadPaddingException | IOException | InvalidSignatureException | OutdatedSignatureException e) {
            throw new ServerException(INVALID_REQUEST);
        }

        if (!rootJson.has("username")
                || !rootJson.get("username").isJsonPrimitive()
                || !rootJson.get("username").getAsJsonPrimitive().isString()) {
            throw new ServerException(INVALID_ID);
        }
        if (!rootJson.has("publicKey")
                || !rootJson.get("publicKey").isJsonPrimitive()
                || !rootJson.get("publicKey").getAsJsonPrimitive().isString()) {
            throw new ServerException(INVALID_KEY);
        }

        return accountHolderService.registerAccountHolder(
                rootJson.get("username").getAsString(),
                rootJson.get("publicKey").getAsString()).toString();
    }

    @PostMapping("users/login")
    String login(@RequestBody String json) {
        Gson gson = new Gson();
        JsonObject rootJson = gson.fromJson(json, JsonObject.class);

        try {
            rootJson = gson.fromJson(new String(SecureDocument.unprotect(rootJson, keystoreService.loadSecretKeyFromKeystore(initial_key_alias, initial_key_password), SecureDocumentUtils.DEFAULT_TIMESTAMP_VALIDITY)), JsonObject.class);
        } catch (JsonSyntaxException | InvalidKeyException | IllegalBlockSizeException
                | BadPaddingException | IOException | InvalidSignatureException | OutdatedSignatureException e) {
            throw new ServerException(INVALID_REQUEST);
        }

        if (!rootJson.has("username")
                || !rootJson.get("username").isJsonPrimitive()
                || !rootJson.get("username").getAsJsonPrimitive().isString()) {
            throw new ServerException(INVALID_USERNAME);
        }
        return accountHolderService.login(rootJson.get("username").getAsString()).toString();
    }

    @GetMapping("/users")
    String getAllUsers() {
        Gson gson = new Gson();
        String json = gson.toJson(accountHolderService.getAccountHolders());

        return json;
    }

    @GetMapping("users/{userId}/accounts")
    String getUserAccounts(@PathVariable Integer userId) {
        if (userId == null)
            throw new ServerException(INVALID_REQUEST);

        return accountHolderService.getUserAccounts(userId).toString();
    }

    @GetMapping("users/{userId}/pending_transfers")
    String getUserPendingTransfers(@PathVariable Integer userId) {
        if (userId == null)
            throw new ServerException(INVALID_REQUEST);

        return accountHolderService.getPendingTransfers(userId).toString();
    }

}
