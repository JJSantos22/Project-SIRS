package pt.tecnico.blingbank.library;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class KeystoreService {

    private String keystoreType;
    private String keystorePath;
    private String keystorePassword;

    public KeystoreService(String keystoreType, String keystorePath, String keystorePassword) {
        this.keystoreType = keystoreType;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
    }

    public void storeKeyPairInKeystore(String alias, KeyPair keyPair, String keyPassword)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, OperatorCreationException {
        KeyStore keyStore;

        try (InputStream fis = new FileInputStream(keystorePath)) {
            keyStore = KeyStore.getInstance(keystoreType);
            keyStore.load(fis, keystorePassword.toCharArray());
        } catch (FileNotFoundException e) {
            // If the keystore file does not exist, create a new one
            keyStore = KeyStore.getInstance(keystoreType);
            keyStore.load(null, null);
        }

        Certificate[] certificate;
        certificate = new Certificate[] { generateX509Certificate(keyPair, alias) };

        PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(keyPair.getPrivate(), certificate);
        keyStore.setEntry(alias, privateKeyEntry, new KeyStore.PasswordProtection(keyPassword.toCharArray()));

        File yourFile = new File(keystorePath);
        yourFile.createNewFile();

        try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
            keyStore.store(fos, keystorePassword.toCharArray());
        }
    }

    public void storeSecretKeyInKeystore(String path, String alias, String keyPassword) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
             {
        SecretKey key = SecureDocumentUtils.readSecretKey(path);
        KeyStore keystore;

        try (InputStream fis = new FileInputStream(keystorePath)) {
            keystore = KeyStore.getInstance(keystoreType);
            keystore.load(fis, keystorePassword.toCharArray());
        } catch (FileNotFoundException e) {
            // If the keystore file does not exist, create a new one
            keystore = KeyStore.getInstance(keystoreType);
            keystore.load(null, null);
        }

        keystore.setEntry(alias, new SecretKeyEntry(key), new PasswordProtection(keyPassword.toCharArray()));

        File yourFile = new File(keystorePath);
        yourFile.createNewFile();

        try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
            keystore.store(fos, keystorePassword.toCharArray());
        }
    }

    public KeyPair loadKeyPairFromKeystore(String alias, String keyPassword)
            throws FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException,
            KeyStoreException, UnrecoverableEntryException {
        KeyStore keyStore;

        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore = KeyStore.getInstance(keystoreType);
            keyStore.load(fis, keystorePassword.toCharArray());
        }

        PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry) keyStore.getEntry(alias,
                new KeyStore.PasswordProtection(keyPassword.toCharArray()));
        PublicKey publicKey = privateKeyEntry.getCertificate().getPublicKey();
        PrivateKey privateKey = privateKeyEntry.getPrivateKey();

        return new KeyPair(publicKey, privateKey);
    }

    public SecretKey loadSecretKeyFromKeystore(String alias, String keyPassword)
            throws FileNotFoundException, IOException {
        SecretKey key = null;
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            KeyStore keystore = KeyStore.getInstance(keystoreType);
            keystore.load(fis, keystorePassword.toCharArray());
            key = (SecretKey) keystore.getKey(alias, keyPassword.toCharArray());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return key;
    }

    public X509Certificate generateX509Certificate(KeyPair keyPair, String domain)
            throws OperatorCreationException, CertificateException {
        // Generate X.509 certificate
        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                new X500Name("CN=" + domain),
                BigInteger.valueOf(System.currentTimeMillis()),
                Date.from(Instant.now()),
                Date.from(Instant.now().plus(365, ChronoUnit.DAYS)),
                new X500Name("CN=" + domain), SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));

        // Set the signature algorithm 
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA")
                .build(keyPair.getPrivate());

        // Build the X.509 certificate
        X509CertificateHolder certificateHolder = certBuilder.build(contentSigner);

        // Convert the Bouncy Castle X509CertificateHolder to a standard Java X.509
        // certificate
        return new JcaX509CertificateConverter()
                .getCertificate(certificateHolder);
    }

}