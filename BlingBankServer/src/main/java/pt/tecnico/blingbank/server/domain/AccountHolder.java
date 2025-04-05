package pt.tecnico.blingbank.server.domain;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import pt.tecnico.blingbank.server.config.AesEncryptor;

@Entity
public class AccountHolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Convert(converter = AesEncryptor.class)
    @Column(unique = true)
    private String username;

    @Convert(converter = AesEncryptor.class)
    private String password;

    /* É suposto apagar uma conta se um dos users for removido? */
    @ManyToMany(cascade = CascadeType.ALL, mappedBy = "accountHolders")
    private List<Account> accounts;

    @Column(length = 2732)
    private String publicKey;

    @OneToMany(mappedBy = "accountHolder")
    private List<TransferAuthorization> authorizedTransfers;

    public AccountHolder() {
    }

    public AccountHolder(String username, String publicKey) {
        setUsername(username);
        setPassword("password");
        setPublicKey(publicKey);
    }

    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public List<Transfer> getPendingTransfers() {
        return this.getAccounts()
                .stream()
                .map(account -> account.getPendingTransfers()
                        .stream()
                        .filter(transfer -> !transfer.getAuthorized()
                                .stream()
                                .anyMatch(authorization -> authorization.getAccountHolder() == this))
                        .collect(Collectors.toList()))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public PublicKey getPublicKey() throws InvalidKeySpecException {

        byte[] keyBytes = Base64.getDecoder().decode(publicKey);

        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hardcoded algorithm should be valid");
        }
    }

    public List<TransferAuthorization> getAuthorizedTransfers() {
        return authorizedTransfers;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private void setPassword(String password) {
        this.password = password;
    }

    private void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void addAccount(Account account) {
        accounts.add(account);
    }

    public void addAuthorizedTransfer(TransferAuthorization transferAuthorization) {
        authorizedTransfers.add(transferAuthorization);
    }

    @Override
    public String toString() {
        try {
            return "AccountHolder: " +
                    "Id=" + getId() +
                    ", Username=" + getUsername() +
                    ", Accounts=" + getAccounts() +
                    ", PublicKey=" + getPublicKey();
        } catch (InvalidKeySpecException e) {
            return "AccountHolder: " +
                    "Id=" + getId() +
                    ", Username=" + getUsername() +
                    ", Accounts=" + getAccounts();
        }
    }
}
