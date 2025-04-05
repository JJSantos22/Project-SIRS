package pt.tecnico.blingbank.server.service;

import static pt.tecnico.blingbank.server.exceptions.ErrorMessage.*;

import pt.tecnico.blingbank.server.exceptions.ServerException;
import pt.tecnico.blingbank.library.KeystoreService;
import pt.tecnico.blingbank.library.SecureDocument;
import pt.tecnico.blingbank.library.SecureDocumentUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.SecretKeyEntry;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;

import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import pt.tecnico.blingbank.server.domain.AccountHolder;
import pt.tecnico.blingbank.server.dto.AccountHolderDto;
import pt.tecnico.blingbank.server.dto.TransferDto;
import pt.tecnico.blingbank.server.dto.AccountDto;
import pt.tecnico.blingbank.server.repository.AccountHolderRepository;

@Service
public class AccountHolderService {


    private static final String keystoreType = "PKCS12";
    private static final String keystorePath = "src/main/resources/server.p12";
    private static final String keystorePassword = "server";

    private static final String initial_key_alias = "0";
    private static final String initial_key_password = "server";

    private KeystoreService keystoreService = new KeystoreService(keystoreType, keystorePath, keystorePassword);

    @Autowired
    private final AccountHolderRepository accountHolderRepository;

    public AccountHolderService(AccountHolderRepository accountHolderRepository) {
        this.accountHolderRepository = accountHolderRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JsonObject registerAccountHolder(String username, String publicKey) {
        if (username == null)
            throw new ServerException(INVALID_USER);

        if (accountHolderRepository.findByUsername(username) != null)
            throw new ServerException(USER_ALREADY_EXISTS);

        AccountHolder accountHolder = new AccountHolder(username, publicKey);

        accountHolderRepository.save(accountHolder);

        return login(username);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JsonObject getUserAccounts(Integer userId) {
        if (userId == null)
            throw new ServerException(INVALID_USER);
        AccountHolder accountHolder = accountHolderRepository.findById(userId)
                .orElseThrow(() -> new ServerException(USER_NOT_FOUND));
        List<AccountDto> list = accountHolder.getAccounts().stream()
                .map(AccountDto::new)
                .collect(Collectors.toList());

        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = gson.toJsonTree(list).getAsJsonArray();
        jsonObject.add("userAccounts", jsonArray);
        try {
            return SecureDocument.protect(jsonObject.toString().getBytes(), keystoreService.loadSecretKeyFromKeystore(Integer.toString(userId)+"_Session_Key", accountHolder.getPassword()));
        } catch (IOException | GeneralSecurityException e) {
            throw new ServerException(PROBLEM_SERVER_ERROR);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JsonObject getAccountHolders() {
        List<AccountHolderDto> list = accountHolderRepository.findAll().stream()
                .map(AccountHolderDto::new)
                .collect(Collectors.toList());
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("accounts", gson.toJson(list));
        return jsonObject;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JsonObject getPendingTransfers(Integer userId) {
        if (userId == null)
            throw new ServerException(INVALID_USER);
        AccountHolder accountHolder = accountHolderRepository.findById(userId)
                .orElseThrow(() -> new ServerException(USER_NOT_FOUND));
        List<TransferDto> list = accountHolder.getPendingTransfers().stream()
                .map(TransferDto::new)
                .collect(Collectors.toList());

        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = gson.toJsonTree(list).getAsJsonArray();
        jsonObject.add("pendingTransfers", jsonArray);
        
        try {
            return SecureDocument.protect(jsonObject.toString().getBytes(), keystoreService.loadSecretKeyFromKeystore(Integer.toString(userId)+"_Session_Key", accountHolder.getPassword()));
        } catch (IOException | GeneralSecurityException e) {
            throw new ServerException(PROBLEM_SERVER_ERROR);
        }
    }


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JsonObject login(String username) {
        if (username == null)
            throw new ServerException(INVALID_USER);
        AccountHolder accountHolder = accountHolderRepository.findByUsername(username);
        if (accountHolder == null)
            throw new ServerException(USER_NOT_FOUND);

        KeyStore keystore;
        SecretKey key;
        String alias = accountHolder.getId().toString()+"_Session_Key";
        String keyPassword = accountHolder.getPassword();
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keystore = KeyStore.getInstance(keystoreType);
            keystore.load(fis, keystorePassword.toCharArray());
            if (keystore.containsAlias(alias))
                keystore.deleteEntry(alias);
            key = SecureDocumentUtils.generateSecretKey(128);
            keystore.setEntry(alias, new SecretKeyEntry(key), new PasswordProtection(keyPassword.toCharArray()));
            FileOutputStream fos = new FileOutputStream(keystorePath);
            keystore.store(fos, keystorePassword.toCharArray());
        }
        catch(Exception e){
            throw new ServerException(PROBLEM_SERVER_ERROR);
        }

        Gson gson = new Gson();
        String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("key", encodedKey);
        jsonObject.add("accountHolder", gson.toJsonTree(new AccountHolderDto(accountHolder)));

        try {
            SecretKey initial_key = keystoreService.loadSecretKeyFromKeystore(initial_key_alias, initial_key_password);
            return SecureDocument.protect(jsonObject.toString().getBytes(StandardCharsets.UTF_8), initial_key);
        }
        catch(Exception e){
            throw new ServerException(PROBLEM_SERVER_ERROR);
        }
    }

}
