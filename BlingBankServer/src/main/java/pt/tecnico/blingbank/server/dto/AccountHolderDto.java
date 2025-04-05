package pt.tecnico.blingbank.server.dto;

import pt.tecnico.blingbank.server.domain.AccountHolder;

public class AccountHolderDto {
    private Integer id;
    private String username;

    public AccountHolderDto() {
    }

    public AccountHolderDto(AccountHolder accountHolder) {
        this.id = accountHolder.getId();
        this.username = accountHolder.getUsername();
    }

    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

}
