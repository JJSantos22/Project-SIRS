package pt.tecnico.blingbank.library;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class SecureDocumentUtils {
    private static final String MAC_ALGO = "HmacSHA256";
    private static final String SIGNATURE_ALGO = "SHA256withRSA";
    private static final String SYM_KEY_ALGO = "AES";
    private static final String SYM_CIPHER_ALGO = SYM_KEY_ALGO + "/CBC/PKCS5Padding";
    private static final String ASYM_KEY_ALGO = "RSA";
    private static final String ASYM_CIPHER_ALGO = ASYM_KEY_ALGO;
    
    /* Default milisseconds until a timestamp is invalid */
    public static final long DEFAULT_TIMESTAMP_VALIDITY = 30000;

    public static SecretKey generateSecretKey(int size) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(SYM_KEY_ALGO);
            keyGen.init(size);
            SecretKey key = keyGen.generateKey();
            return key;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hardcoded algorithm should be valid!");
        }
    }

    public static SecretKey readSecretKey(String secretKeyPath) throws FileNotFoundException, IOException {
        byte[] encoded = SecureDocumentUtils.readFile(secretKeyPath);
        SecretKeySpec keySpec = new SecretKeySpec(encoded, SYM_KEY_ALGO);
        return keySpec;
    }

    public static KeyPair generateKeyPair(int size) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ASYM_KEY_ALGO);
            keyGen.initialize(size);
            KeyPair keyPair = keyGen.generateKeyPair();
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hardcoded algorithm should be valid!");
        }
    }

    public static PrivateKey readPrivateKey(String privateKeyPath)
            throws FileNotFoundException, IOException, InvalidKeySpecException {
        try {
            byte[] privEncoded = SecureDocumentUtils.readFile(privateKeyPath);
            PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privEncoded);
            KeyFactory keyFacPriv = KeyFactory.getInstance(ASYM_KEY_ALGO);
            PrivateKey priv = keyFacPriv.generatePrivate(privSpec);
            return priv;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hardcoded algorithm should be valid!");
        }
    }

    public static PublicKey readPublicKey(String publicKeyPath)
            throws FileNotFoundException, IOException, InvalidKeySpecException {
        try {
            byte[] pubEncoded = SecureDocumentUtils.readFile(publicKeyPath);
            X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubEncoded);
            KeyFactory keyFacPub = KeyFactory.getInstance(ASYM_KEY_ALGO);
            PublicKey pub = keyFacPub.generatePublic(pubSpec);
            return pub;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hardcoded algorithm should be valid!");
        }
    }

    public static String encryptDocument(byte[] inputData, SecretKey secretKey)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        try {
            // cipher data
            Cipher cipher = Cipher.getInstance(SYM_CIPHER_ALGO);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(Arrays.copyOf(new String("Secure").getBytes(), 16));
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            byte[] cipherBytes = cipher.doFinal(inputData);

            // return value
            return Base64.getEncoder().encodeToString(cipherBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            throw new IllegalStateException("Hardcoded algorithm should be valid!");
        }
    }

    public static byte[] unencryptDocument(String value, SecretKey key)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] byteValue = Base64.getDecoder().decode(value);

        try {
            Cipher cipher = Cipher.getInstance(SYM_CIPHER_ALGO);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(Arrays.copyOf(new String("Secure").getBytes(), 16));
            cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);
            byte[] decipheredBytes = cipher.doFinal(byteValue);

            return decipheredBytes;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            throw new IllegalStateException("Hardcoded algorithm should be valid!");
        }
    }

    public static String sign(byte[][] dataArray, PrivateKey privateKey)
            throws InvalidKeyException, SignatureException {
        try {
            Signature sig = Signature.getInstance(SIGNATURE_ALGO);

            sig.initSign(privateKey);
            for (byte[] data : dataArray) {
                sig.update(data);
            }
            byte[] signature = sig.sign();

            return Base64.getEncoder().encodeToString(signature);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hardcoded algorithm should be valid!");
        }
    }

    public static String createMAC(byte[][] dataArray, SecretKey secretKey) throws InvalidKeyException, SignatureException {
        try {
            Mac mac = Mac.getInstance(MAC_ALGO);

            mac.init(secretKey);
            for (byte[] data : dataArray) {
                mac.update(data);
            }
            byte[] macBytes = mac.doFinal();

            return Base64.getEncoder().encodeToString(macBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hardcoded algorithm should be valid!");
        }
    }

    public static boolean verifySignature(byte[][] dataArray, PublicKey publicKey, String signature)
            throws InvalidKeyException {
        byte[] encryptedSignatureBytes = Base64.getDecoder().decode(signature);

        try {
            Signature sig = Signature.getInstance(SIGNATURE_ALGO);

            sig.initVerify(publicKey);
            for (byte[] data : dataArray) {
                sig.update(data);
            }
            boolean valid = sig.verify(encryptedSignatureBytes);

            return valid;
        } catch (NoSuchAlgorithmException | SignatureException e) {
            throw new IllegalStateException("Hardcoded algorithm should be valid!");
        }
    }

    public static boolean verifyMAC(byte[][] dataArray, SecretKey secretKey, String signature)
            throws InvalidKeyException {
        byte[] encryptedSignatureBytes = Base64.getDecoder().decode(signature);

        try {
            Mac mac = Mac.getInstance(MAC_ALGO);

            mac.init(secretKey);
            for (byte[] data : dataArray) {
                mac.update(data);
            }
            byte[] macBytes = mac.doFinal();

            return MessageDigest.isEqual(macBytes, encryptedSignatureBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hardcoded algorithm should be valid!");
        }
    }

    public static String encryptKey(Key key, PublicKey publicKey)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        try {
            Cipher cipher = Cipher.getInstance(ASYM_CIPHER_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] cipherKey = cipher.doFinal(key.getEncoded());

            return Base64.getEncoder().encodeToString(cipherKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalStateException("Hardcoded algorithm should be valid!");
        }
    }

    public static String encryptKey(Key key, SecretKey secretKey)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        try {
            Cipher cipher = Cipher.getInstance(SYM_CIPHER_ALGO);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(Arrays.copyOf(new String("Secure").getBytes(), 16));
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            byte[] cipherKey = cipher.doFinal(key.getEncoded());

            return Base64.getEncoder().encodeToString(cipherKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
            throw new IllegalStateException("Hardcoded algorithm should be valid!");
        }
    }

    public static SecretKey unencryptKey(String encryptedKey, PrivateKey privateKey)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] encryptedKeyBytes = Base64.getDecoder().decode(encryptedKey);

        try {
            Cipher cipher = Cipher.getInstance(ASYM_CIPHER_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decipheredBytes = cipher.doFinal(encryptedKeyBytes);

            SecretKey keySpec = new SecretKeySpec(decipheredBytes, SYM_KEY_ALGO);

            return keySpec;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalStateException("Hardcoded algorithm should be valid!");
        }
    }

    public static SecretKey unencryptKey(String encryptedKey, SecretKey secretKey)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] encryptedKeyBytes = Base64.getDecoder().decode(encryptedKey);

        try {
            Cipher cipher = Cipher.getInstance(SYM_CIPHER_ALGO);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(Arrays.copyOf(new String("Secure").getBytes(), 16));
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            byte[] decipheredBytes = cipher.doFinal(encryptedKeyBytes);

            SecretKey keySpec = new SecretKeySpec(decipheredBytes, SYM_KEY_ALGO);

            return keySpec;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            throw new IllegalStateException("Hardcoded algorithm should be valid!");
        }
    }

    public static byte[] readFile(String path) throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(path);
        byte[] content = new byte[fis.available()];
        fis.read(content);
        fis.close();
        return content;
    }

}
