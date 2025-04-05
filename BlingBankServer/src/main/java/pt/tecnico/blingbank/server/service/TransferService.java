package pt.tecnico.blingbank.server.service;

import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.stereotype.Service;

import static pt.tecnico.blingbank.server.exceptions.ErrorMessage.*;
import static pt.tecnico.blingbank.server.domain.Transfer.Status.*;

import java.time.LocalDateTime;

import pt.tecnico.blingbank.server.domain.Account;
import pt.tecnico.blingbank.server.domain.AccountHolder;
import pt.tecnico.blingbank.server.domain.Transfer;
import pt.tecnico.blingbank.server.domain.TransferAuthorization;
import pt.tecnico.blingbank.server.dto.TransferDto;
import pt.tecnico.blingbank.server.repository.AccountHolderRepository;
import pt.tecnico.blingbank.server.repository.AccountRepository;
import pt.tecnico.blingbank.server.repository.TransferAuthorizationRepository;
import pt.tecnico.blingbank.server.repository.TransferRepository;
import pt.tecnico.blingbank.server.exceptions.ServerException;

@Service
public class TransferService {

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private AccountHolderRepository accountHolderRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferAuthorizationRepository transferAuthorizationRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JsonObject registerTransfer(Integer userId, Integer originId, Integer destinationId, Integer amount,
            String description) {

        AccountHolder user = accountHolderRepository.findById(userId)
                .orElseThrow(() -> new ServerException(UNKNOWN_ACCOUNT_HOLDER));
        Account origin = accountRepository.findById(originId)
                .orElseThrow(() -> new ServerException(ORIGIN_NOT_FOUND));
        Account destination = accountRepository.findById(destinationId)
                .orElseThrow(() -> new ServerException(DESTINATION_NOT_FOUND));

        if (originId.equals(destinationId)) {
            throw new ServerException(ORIGIN_EQUALS_DESTINATION);
        }

        if (!origin.getAccountHolders().contains(user)) {
            throw new ServerException(PERMISSION_DENIED);
        }

        if (amount <= 0) {
            throw new ServerException(INVALID_AMOUNT);
        }

        if (origin.getBalance() < amount) {
            throw new ServerException(NOT_ENOUGH_FUNDS);
        }

        if (!origin.getCurrency().equals(destination.getCurrency())) {
            throw new ServerException(INCOMPATIBLE_CURRENCIES);
        }

        LocalDateTime date = LocalDateTime.now();

        Transfer transfer = new Transfer(origin, destination, date, amount, description);

        origin.addBalance(-amount);

        accountRepository.save(origin);
        transfer = transferRepository.save(transfer);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", transfer.getId());
        return jsonObject;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JsonObject confirmTransfer(Integer id, Integer accountHolderId, long timestamp, String signature) {
        AccountHolder accountHolder = accountHolderRepository.findById(accountHolderId)
                .orElseThrow(() -> new ServerException(UNKNOWN_ACCOUNT_HOLDER));
        Transfer transfer = transferRepository.findById(id).orElseThrow(() -> new ServerException(UNKNOWN_TRANSFER));

        if (!transfer.getOrigin().getAccountHolders().contains(accountHolder)) {
            throw new ServerException(PERMISSION_DENIED);
        }

        if (!transfer.getStatus().equals(PENDING)) {
            throw new ServerException(PENDING_TRANSFER_NOT_FOUND);
        }

        if (transfer.getAuthorized()
                .stream()
                .anyMatch(authorization -> authorization.getAccountHolder() == accountHolder)) {
            throw new ServerException(CONFIRMATION_ALREADY_GIVEN);
        }

        return executeTransfer(transfer, accountHolder, timestamp, signature);

    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JsonObject cancelTransfer(Integer transferId, Integer accountHolderId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ServerException(UNKNOWN_TRANSFER));
        AccountHolder accountHolder = accountHolderRepository.findById(accountHolderId)
                .orElseThrow(() -> new ServerException(UNKNOWN_ACCOUNT_HOLDER));

        if (!transfer.getOrigin().getAccountHolders().contains(accountHolder)) {
            throw new ServerException(PERMISSION_DENIED);
        }

        if (!transfer.getStatus().equals(PENDING)) {
            throw new ServerException(PENDING_TRANSFER_NOT_FOUND);
        }

        Account origAccount = transfer.getOrigin();
        transfer.setStatus(CANCELED);
        origAccount.addBalance(transfer.getValue());
        transferRepository.save(transfer);
        accountRepository.save(origAccount);

        Gson gson = new Gson();

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("transfer", gson.toJsonTree(new TransferDto(transfer)));

        return jsonObject;
    }

    public JsonObject executeTransfer(Transfer transfer, AccountHolder accountHolder, long timestamp, String signature) {
        TransferAuthorization authorization = new TransferAuthorization(transfer, accountHolder, timestamp, signature);
        transferAuthorizationRepository.save(authorization);

        if (transfer.getAuthorized().size() == transfer.getOrigin().getAccountHolders().size()) {
            Account destAccount = transfer.getDestination();
            transfer.setStatus(ACCEPTED);
            destAccount.addBalance(transfer.getValue());
            accountRepository.save(destAccount);
        }

        Gson gson = new Gson();

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("transfer", gson.toJsonTree(new TransferDto(transfer)));

        return jsonObject;
    }
}