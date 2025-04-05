package pt.tecnico.blingbank.user;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.operator.OperatorCreationException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import pt.tecnico.blingbank.library.KeystoreService;
import pt.tecnico.blingbank.library.SecureDocument;
import pt.tecnico.blingbank.library.SecureDocumentUtils;
import pt.tecnico.blingbank.library.exceptions.InvalidSignatureException;
import pt.tecnico.blingbank.library.exceptions.OutdatedSignatureException;
import pt.tecnico.blingbank.user.exceptions.UserException;

public class SystemService {

    private void debug(String debugMessage) {
        if (debug)
            System.out.println("\nServer encrypted response:\n\t" + debugMessage + "\n");
    }

    private boolean debug = false;

    private final static String URI_SCHEME = "https";

    String hostname;

    private HttpClient httpClient;

    private KeystoreService keystoreService;

    public SystemService(HttpClient httpClient, String hostname, KeystoreService keystoreService, boolean debug) {
        this.httpClient = httpClient;
        this.hostname = hostname;
        this.keystoreService = keystoreService;
        this.debug = debug;
    }

    public JsonObject login(String username) throws UserException {
        try {
            URI uri = new URIBuilder()
                    .setScheme(URI_SCHEME)
                    .setHost(hostname)
                    .setPath("users/login")
                    .build();

            JsonObject json = new JsonObject();
            json.addProperty("username", username);

            json = SecureDocument.protect(json.toString().getBytes(),
                    keystoreService.loadSecretKeyFromKeystore("0", "user"));

            StringEntity requestEntity = new StringEntity(
                    json.toString(),
                    ContentType.APPLICATION_JSON);

            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(requestEntity);
            HttpResponse response = httpClient.execute(httpPost);

            if (response.getStatusLine().getStatusCode() == 200) {
                String responseBody = EntityUtils.toString(response.getEntity());

                // Use Gson to parse JSON response
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                try {
                    debug(jsonObject.toString());
                    jsonObject = gson.fromJson(
                            new String(SecureDocument.unprotect(
                                    jsonObject,
                                    keystoreService.loadSecretKeyFromKeystore("0", "user"),
                                    SecureDocumentUtils.DEFAULT_TIMESTAMP_VALIDITY)),
                            JsonObject.class);
                    return jsonObject;
                } catch (JsonSyntaxException | InvalidKeyException | IllegalBlockSizeException
                        | BadPaddingException e) {
                    throw new UserException("An error occured on the server");
                } catch (InvalidSignatureException e) {
                    throw new UserException("Response from the server is not authentic");
                } catch (OutdatedSignatureException e) {
                    throw new UserException("Outdated response from the server may not be authentic");
                }

            } else if (response.getStatusLine().getStatusCode() == 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                throw new UserException(responseBody);
            } else {
                throw new UserException("An error occured on the server");
            }
        } catch (URISyntaxException | IOException | GeneralSecurityException e) {
            throw new UserException("Failed to connect to the server" + e.getLocalizedMessage());
        }
    }

    public JsonObject register(String username) throws UserException {
        /* Generate key pair */
        KeyPair keyPair = SecureDocumentUtils.generateKeyPair(2048);

        try {
            keystoreService.storeKeyPairInKeystore(username, keyPair, "pass");
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException
                | OperatorCreationException e) {
            throw new UserException("Failed to generate keys");
        }

        try {
            URI uri = new URIBuilder()
                    .setScheme(URI_SCHEME)
                    .setHost(hostname)
                    .setPath("/users/register")
                    .build();

            JsonObject json = new JsonObject();
            json.addProperty("username", username);
            json.addProperty("publicKey", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));

            json = SecureDocument.protect(json.toString().getBytes(),
                    keystoreService.loadSecretKeyFromKeystore("0", "user"));

            StringEntity requestEntity = new StringEntity(
                    json.toString(),
                    ContentType.APPLICATION_JSON);

            HttpPut httpPut = new HttpPut(uri);
            httpPut.setEntity(requestEntity);
            HttpResponse response = httpClient.execute(httpPut);

            if (response.getStatusLine().getStatusCode() == 200) {
                String responseBody = EntityUtils.toString(response.getEntity());
                // Use Gson to parse JSON response
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                try {
                    debug(jsonObject.toString());
                    jsonObject = gson.fromJson(
                            new String(SecureDocument.unprotect(
                                    jsonObject,
                                    keystoreService.loadSecretKeyFromKeystore("0", "user"),
                                    SecureDocumentUtils.DEFAULT_TIMESTAMP_VALIDITY)),
                            JsonObject.class);
                    return jsonObject;
                } catch (JsonSyntaxException | InvalidKeyException | IllegalBlockSizeException
                        | BadPaddingException e) {
                    throw new UserException("An error occured on the server");
                } catch (InvalidSignatureException e) {
                    throw new UserException("Response from the server is not authentic");
                } catch (OutdatedSignatureException e) {
                    throw new UserException("Outdated response from the server may not be authentic");
                }
            } else if (response.getStatusLine().getStatusCode() == 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                throw new UserException(responseBody);
            } else {
                throw new UserException("An error occured on the server");
            }
        } catch (URISyntaxException | IOException | GeneralSecurityException e) {
            throw new UserException("Failed to connect to the server");
        }
    }
}
