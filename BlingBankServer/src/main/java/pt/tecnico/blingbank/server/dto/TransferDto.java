package pt.tecnico.blingbank.server.dto;

import pt.tecnico.blingbank.server.domain.Account;
import pt.tecnico.blingbank.server.domain.Transfer;

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

    public TransferDto(Transfer transfer, Account account) {
        this(transfer);

        if (transfer.getOrigin() == account) {
            this.amount *= -1;
        }
    }

    public TransferDto(Transfer transfer) {
        this.id = transfer.getId();
        this.date = transfer.getDate().toString();
        this.originId = transfer.getOrigin().getId();
        this.destinationId = transfer.getDestination().getId();
        this.amount = transfer.getValue();
        this.description = transfer.getDescription();
        this.status = transfer.getStatus().name();
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
