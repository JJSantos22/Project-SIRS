package pt.tecnico.blingbank.user.dto;

import com.google.gson.Gson;

public class TransferDto {
    private Integer id;
    private String date;
    private Integer originId;
    private Integer destinationId;
    private Integer amount;
    private String description;
    private String status;

    public TransferDto() {
    }

    public static TransferDto fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, TransferDto.class);
    }

    public Integer getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public Integer getOriginId() {
        return originId;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public Integer getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }
}
