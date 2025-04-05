package pt.tecnico.blingbank.server.domain;

import static pt.tecnico.blingbank.server.domain.Transfer.Status.PENDING;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import pt.tecnico.blingbank.server.config.AesEncryptor;

@Entity
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @Convert(converter = AesEncryptor.class)
    private Account origin;

    @ManyToOne
    @Convert(converter = AesEncryptor.class)
    private Account destination;

    @Convert(converter = AesEncryptor.class)
    private LocalDateTime date;

    @Convert(converter = AesEncryptor.class)
    private Integer value;

    @Convert(converter = AesEncryptor.class)
    private String description;

    public enum Status {
        PENDING, ACCEPTED, CANCELED;
    } 

    @Convert(converter = AesEncryptor.class)
    private Status status;

    @OneToMany(mappedBy = "transfer")
    private List<TransferAuthorization> authorized = new ArrayList<>();


    public Transfer() {}

    public Transfer(Account origin, Account destination, LocalDateTime date, Integer value, String description) {
        setOrigin(origin);
        setDestination(destination);
        setDate(date);
        setValue(value);
        setDescription(description);
        setStatus(PENDING);
    }

    public Integer getId() {
        return id;
    }

    public Account getOrigin() {
        return origin;
    }

    public Account getDestination() {
        return destination;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Integer getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
    
    public List<TransferAuthorization> getAuthorized() {
        return authorized;
    }

    public Status getStatus() {
        return status;
    }

    private void setOrigin(Account origin) {
        this.origin = origin;
        origin.addTransferSent(this);
    }
    
    private void setDestination(Account destination) {
        this.destination = destination;
        destination.addTransferReceived(this);
    }

    private void setDate(LocalDateTime date) {
        this.date = date;
    }

    private void setValue(Integer value) {
        this.value = value;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void addAuthorized(TransferAuthorization authorization) {
        this.authorized.add(authorization);
    }

    @Override
    public String toString() {
        return "Transfer{" +
                "id=" + id +
                ", origin=" + origin +
                ", destination=" + destination +
                ", date=" + date +
                ", value=" + value +
                ", description='" + description +
                ", authorized=" + authorized +
                ", status=" + status.toString() +
                '}';
    }
}
