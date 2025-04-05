package pt.tecnico.blingbank.server.dto;

import java.util.List;
import java.util.stream.Collectors;

import pt.tecnico.blingbank.server.domain.Account;

public class AccountDto {
    private Integer id;
    private Integer balance;
    private String currency;
    private List<AccountHolderDto> accountholders;
    private List<TransferDto> transfers;

    public AccountDto() {
    }

    public AccountDto(Account account) {
        this.id = account.getId();
        this.balance = account.getBalance();
        this.currency = account.getCurrency();
        this.accountholders = account.getAccountHolders().stream()
                .map(AccountHolderDto::new)
                .collect(Collectors.toList());
        this.transfers = (account.getTransfers().stream()
                .sorted((o2, o1)->o1.getDate().compareTo(o2.getDate()))
                .collect(Collectors.toList())).stream()
                .map((transfer) -> new TransferDto(transfer, account))
                .collect(Collectors.toList());
    }

    public Integer getId() {
        return id;
    }

    public Integer getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public List<AccountHolderDto> getAccountholders() {
        return accountholders;
    }

    public List<TransferDto> getTransfers() {
        return transfers;
    }
}
