package pt.tecnico.blingbank.server.controller;

import static pt.tecnico.blingbank.server.exceptions.ErrorMessage.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import pt.tecnico.blingbank.library.KeystoreService;
import pt.tecnico.blingbank.library.SecureDocument;
import pt.tecnico.blingbank.library.SecureDocumentUtils;
import pt.tecnico.blingbank.library.exceptions.InvalidSignatureException;
import pt.tecnico.blingbank.library.exceptions.OutdatedSignatureException;
import pt.tecnico.blingbank.server.domain.AccountHolder;
import pt.tecnico.blingbank.server.exceptions.ServerException;
import pt.tecnico.blingbank.server.repository.AccountHolderRepository;
import pt.tecnico.blingbank.server.service.AccountService;

@RestController
public class AccountController {

    private static final String keystoreType = "PKCS12";
    private static final String keystorePath = "src/main/resources/server.p12";
    private static final String keystorePassword = "server";

    private KeystoreService keystoreService = new KeystoreService(keystoreType, keystorePath, keystorePassword);

    @Autowired
    private AccountService accountService;

    @Autowired
    AccountHolderRepository accountHolderRepository;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PutMapping("users/{userId}/accounts/register")
    String registerAccount(@PathVariable Integer userId, @RequestBody String json) {
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

        if (!rootJson.has("accountHolders")
                || !rootJson.get("accountHolders").isJsonArray()) {
            throw new ServerException(INVALID_REQUEST);
        }
        if (!rootJson.has("currency")
                || !rootJson.get("currency").isJsonPrimitive()
                || !rootJson.get("currency").getAsJsonPrimitive().isString()) {
            throw new ServerException(INVALID_REQUEST);
        }

        try {
            List<String> accountHolders = gson.fromJson(
                rootJson.getAsJsonArray("accountHolders"),
                new TypeToken<List<String>>(){}.getType()
            );

            String currency = rootJson.getAsJsonPrimitive("currency").getAsString();

            String jsonResponse = gson.toJson(accountService.registerAccount(userId, accountHolders, currency));

            return jsonResponse;
        } catch(JsonSyntaxException e) {
            throw new ServerException(INVALID_REQUEST);
        }
    }

    @GetMapping("/accounts")
    String getAccounts() {
        Gson gson = new Gson();
        String json = gson.toJson(accountService.getAccounts());

        return json;
    }

    @GetMapping("users/{userId}/accounts/{accountId}/overview")
    String getAccountOverview(@PathVariable Integer userId, @PathVariable Integer accountId) {
        if (userId == null)
            throw new ServerException(INVALID_REQUEST);
        if (accountId == null)
            throw new ServerException(INVALID_REQUEST);

        Gson gson = new Gson();
        String json = gson.toJson(accountService.getAccountOverview(userId, accountId));

        return json;
    }

    @GetMapping("users/{userId}/accounts/{accountId}/pending_transfers")
    String getAccountPendingTransfers(@PathVariable Integer userId, @PathVariable Integer accountId) {
        if (userId == null)
            throw new ServerException(INVALID_REQUEST);
        if (accountId == null)
            throw new ServerException(INVALID_REQUEST);

        Gson gson = new Gson();
        String json = gson.toJson(accountService.getPendingTransfers(userId, accountId));

        return json;
    }
}
