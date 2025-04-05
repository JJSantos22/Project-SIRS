package pt.tecnico.blingbank.server.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.Key;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Configuration;
import org.springframework.util.SerializationUtils;


import jakarta.persistence.AttributeConverter;

@Configuration
public class AesEncryptor implements AttributeConverter<Object, String> {
    private final String encryptionKey = "this-is-stat-key";
    private final String encryptionMethod = "AES";

    private static Key key;
    private static Cipher cipher;

    private Key getKey() {
        if (key == null) {
            key = new SecretKeySpec(encryptionKey.getBytes(), encryptionMethod);
        }
        return key;
    }

    private Cipher getCipher() {
        if (cipher == null) {
            try {
                cipher = Cipher.getInstance(encryptionMethod);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return cipher;
    }

    private void initCipher(int mode) {
        try {
            getCipher().init(mode, getKey());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String convertToDatabaseColumn(Object attribute) {
        if (attribute == null) {
            return null;
        }
        initCipher(Cipher.ENCRYPT_MODE);
        byte[] bytes = SerializationUtils.serialize(attribute);
        try {
            return Base64.getEncoder().encodeToString(getCipher().doFinal(bytes));
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        initCipher(Cipher.DECRYPT_MODE);
        try {
            byte[] bytes = getCipher().doFinal(Base64.getDecoder().decode(dbData));
            return deserialize(bytes);
        } catch (IllegalBlockSizeException | BadPaddingException | ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return ois.readObject();
        }
    }
    
}
