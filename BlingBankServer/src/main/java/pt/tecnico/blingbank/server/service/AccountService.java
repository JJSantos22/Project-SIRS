package pt.tecnico.blingbank.server.service;

import static pt.tecnico.blingbank.server.exceptions.ErrorMessage.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;

import pt.tecnico.blingbank.library.KeystoreService;
import pt.tecnico.blingbank.library.SecureDocument;
import pt.tecnico.blingbank.server.domain.Account;
import pt.tecnico.blingbank.server.domain.AccountHolder;
import pt.tecnico.blingbank.server.dto.AccountDto;
import pt.tecnico.blingbank.server.dto.TransferDto;
import pt.tecnico.blingbank.server.exceptions.ServerException;
import pt.tecnico.blingbank.server.repository.AccountHolderRepository;
import pt.tecnico.blingbank.server.repository.AccountRepository;

@Service
public class AccountService {

    private static final String keystoreType = "PKCS12";
    private static final String keystorePath = "src/main/resources/server.p12";
    private static final String keystorePassword = "server";

    private KeystoreService keystoreService = new KeystoreService(keystoreType, keystorePath, keystorePassword);

    @Autowired
    private final AccountRepository accountRepository;

    @Autowired
    private final AccountHolderRepository accountHolderRepository;

    public AccountService(AccountRepository accountRepository, AccountHolderRepository accountHolderRepository) {
        this.accountRepository = accountRepository;
        this.accountHolderRepository = accountHolderRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JsonObject registerAccount(Integer userId, List<String> holdersUsernames, String currency) {
        List<AccountHolder> accountHolders = new ArrayList<>();
        
        AccountHolder currentUser = accountHolderRepository.findById(userId).orElseThrow(() -> new ServerException(USER_NOT_FOUND));
        accountHolders.add(currentUser);

        for (String accountHolderUsername : holdersUsernames) {
            AccountHolder accountHolder = accountHolderRepository.findByUsername(accountHolderUsername);
            if (accountHolder == null)
                throw new ServerException(USER_NOT_FOUND);
            if (accountHolders.contains(accountHolder))
                throw new ServerException(DUPLICATE_ACCOUNT_HOLDER);
            accountHolders.add(accountHolder);
        }

        Account account = new Account(accountHolders, 100, currency);

        accountRepository.save(account);

        AccountDto accountDto = new AccountDto(account);

        Gson gson = new Gson();

        try {
            return SecureDocument.protect(gson.toJsonTree(accountDto).toString().getBytes(), keystoreService.loadSecretKeyFromKeystore(Integer.toString(userId)+"_Session_Key", currentUser.getPassword()));
        } catch (IOException | GeneralSecurityException e) {
            throw new ServerException(PROBLEM_SERVER_ERROR);
        }


    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<AccountDto> getAccounts() {
        return accountRepository.findAll().stream().map(AccountDto::new).toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JsonObject getAccountOverview(Integer userId, Integer accountId) {
        if (userId == null)
            throw new ServerException(INVALID_USER);
        if (accountId == null)
            throw new ServerException(INVALID_ACCOUNT);
        AccountHolder user = accountHolderRepository.findById(userId).orElseThrow(() -> new ServerException(USER_NOT_FOUND));
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new ServerException(ACCOUNT_NOT_FOUND));
        if (!account.getAccountHolders().contains(user))
            throw new ServerException(PERMISSION_DENIED);
        AccountDto accountDto = new AccountDto(account);
        Gson gson = new Gson();
        try {
            return SecureDocument.protect(gson.toJsonTree(accountDto).toString().getBytes(), keystoreService.loadSecretKeyFromKeystore(Integer.toString(userId)+"_Session_Key", user.getPassword()));
        } catch (IOException | GeneralSecurityException e) {
            throw new ServerException(PROBLEM_SERVER_ERROR);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JsonObject getPendingTransfers(Integer userId, Integer accountId) {
        if (userId == null)
            throw new ServerException(INVALID_USER);
        if (accountId == null)
            throw new ServerException(INVALID_ACCOUNT);
        AccountHolder user = accountHolderRepository.findById(userId).orElseThrow(() -> new ServerException(USER_NOT_FOUND));
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new ServerException(ACCOUNT_NOT_FOUND));
        if (!account.getAccountHolders().contains(user))
            throw new ServerException(PERMISSION_DENIED);
        List<TransferDto> list = account.getPendingTransfers().stream().map(TransferDto::new).toList();
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = gson.toJsonTree(list).getAsJsonArray();
        jsonObject.add("pendingTransfers", jsonArray);
        try {
            return SecureDocument.protect(jsonObject.toString().getBytes(), keystoreService.loadSecretKeyFromKeystore(Integer.toString(userId)+"_Session_Key", user.getPassword()));
        } catch (IOException | GeneralSecurityException e) {
            throw new ServerException(PROBLEM_SERVER_ERROR);
        }
    }

}
