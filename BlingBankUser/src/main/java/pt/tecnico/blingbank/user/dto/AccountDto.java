package pt.tecnico.blingbank.user.dto;

import java.util.List;

public class AccountDto {
    private Integer id;
    private Integer balance;
    private String currency;
    private List<AccountHolderDto> accountholders;
    private List<TransferDto> transfers;

    public AccountDto() {
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
