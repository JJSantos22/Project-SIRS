package pt.tecnico.blingbank.user.dto;

import com.google.gson.Gson;

public class AccountHolderDto {
    private Integer id;
    private String username;

    public AccountHolderDto() {
    }

    public static AccountHolderDto fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, AccountHolderDto.class);
    }

    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

}
