package pt.tecnico.blingbank.server.domain;

import static pt.tecnico.blingbank.server.domain.Transfer.Status.ACCEPTED;
import static pt.tecnico.blingbank.server.domain.Transfer.Status.PENDING;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import pt.tecnico.blingbank.server.config.AesEncryptor;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToMany
    private List<AccountHolder> accountHolders = new ArrayList<>();
    
    @Convert(converter = AesEncryptor.class)
    private Integer balance;

    @Convert(converter = AesEncryptor.class)
    private String currency;

    @OneToMany(mappedBy = "origin")
    private List<Transfer> transfersSent = new ArrayList<>();

    @OneToMany(mappedBy = "destination")
    private List<Transfer> transfersReceived = new ArrayList<>();

    public Account() {
    }

    public Account(List<AccountHolder> accountHolders, int balance, String currency) {
        setAccountHolders(accountHolders);
        setBalance(balance);
        setCurrency(currency);
    }

    public Integer getId() {
        return id;
    }

    public List<AccountHolder> getAccountHolders() {
        return accountHolders;
    }

    public Integer getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public List<Transfer> getTransfers() {
        List<Transfer> transfers = new ArrayList<>();
        transfers.addAll(transfersSent);
        transfers.addAll(transfersReceived.stream()
                .filter(t -> t.getStatus().equals(ACCEPTED))
                .collect(Collectors.toList()));

        return transfers;
    }

    public List<Transfer> getPendingTransfers() {
        return transfersSent.stream().filter(t -> t.getStatus().equals(PENDING)).collect(Collectors.toList());
    }

    private void setAccountHolders(List<AccountHolder> accountHolders) {
        this.accountHolders.addAll(accountHolders);
        accountHolders.forEach(accountHolder -> accountHolder.addAccount(this));
    }

    private void setCurrency(String currency) {
        this.currency = currency;
    }

    private void setBalance(Integer value) {
        this.balance = value;
    }

    public void addTransferSent(Transfer transfer) {
        this.transfersSent.add(transfer);
    }

    public void addTransferReceived(Transfer transfer) {
        this.transfersReceived.add(transfer);
    }

    public void addBalance(Integer value) {
        this.balance += value;
    }

    public String toString() {
        return "Account: " + id + ", Balance: " + balance + ", Currency: " + currency;
    }
}
