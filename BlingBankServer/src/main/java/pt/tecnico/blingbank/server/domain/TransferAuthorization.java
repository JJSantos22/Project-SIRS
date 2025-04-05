package pt.tecnico.blingbank.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import pt.tecnico.blingbank.server.config.AesEncryptor;

@Entity
public class TransferAuthorization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @Convert(converter = AesEncryptor.class)
    private Transfer transfer;

    @ManyToOne
    @Convert(converter = AesEncryptor.class)
    private AccountHolder accountHolder;
    
    @Convert(converter = AesEncryptor.class)
    private Long timestamp;

    @Column(length = 345)
    private String signature;

    public TransferAuthorization() {}

    public TransferAuthorization(Transfer transfer, AccountHolder accountHolder, long timestamp, String signature) {
        setTransfer(transfer);
        setAccountHolder(accountHolder);
        this.timestamp = timestamp;
        this.signature = signature;
    }

    public Transfer getTransfer() {
        return transfer;
    }

    public AccountHolder getAccountHolder() {
        return accountHolder;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getSignature() {
        return signature;
    }

    private void setTransfer(Transfer transfer) {
        this.transfer = transfer;
        transfer.addAuthorized(this);
    }

    private void setAccountHolder(AccountHolder accountHolder) {
        this.accountHolder = accountHolder;
        accountHolder.addAuthorizedTransfer(this);
    }

    @Override
    public String toString() {
        return "TransferConfirmation{" +
                "transfer=" + transfer +
                ", accountHolder=" + accountHolder +
                ", signature= '" + signature + '\'' +
                '}';
    }
}
