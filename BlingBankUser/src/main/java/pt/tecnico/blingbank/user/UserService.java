package pt.tecnico.blingbank.user;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;

import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import pt.tecnico.blingbank.library.KeystoreService;
import pt.tecnico.blingbank.library.SecureDocument;
import pt.tecnico.blingbank.library.SecureDocumentUtils;
import pt.tecnico.blingbank.library.exceptions.InvalidSignatureException;
import pt.tecnico.blingbank.library.exceptions.OutdatedSignatureException;
import pt.tecnico.blingbank.user.exceptions.UserException;
import static pt.tecnico.blingbank.user.exceptions.UserException.ErrorMessages.*;


public class UserService {
    
    private void debug(String debugMessage) {
        if (debug)
            System.out.println("\nServer encrypted response:\n\t" + debugMessage + "\n");
    }

    private final static String URI_SCHEME = "https";

    String hostname;

    int currentUser;

    String username;

    private HttpClient httpClient;

    private SecretKey sessionKey;

    private KeystoreService keystoreService;

    private boolean debug=false;

    public UserService(HttpClient httpClient, String hostname, int currentUser, KeystoreService keystoreService, boolean debug) {
        this.httpClient = httpClient;
        this.hostname = hostname;
        this.currentUser = currentUser;
        this.keystoreService = keystoreService;
        this.debug = debug;
    }

    public void setSessionKey(SecretKey sessionKey) {
        this.sessionKey = sessionKey;
    }

    public void setCurrentUser(int userId) {
        this.currentUser = userId;
    }

    public String getCurrentUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public JsonObject getAccounts() throws UserException {
        try {
            URI uri = new URIBuilder()
                    .setScheme(URI_SCHEME)
                    .setHost(hostname)
                    .setPath("/users/" + currentUser + "/accounts")
                    .build();
            HttpGet httpGet = new HttpGet(uri);

            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                String responseBody = EntityUtils.toString(response.getEntity());

                // Use Gson to parse JSON response
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);

                try {
                    debug(jsonObject.toString());
                    return gson.fromJson(new String(SecureDocument.unprotect(jsonObject, sessionKey, SecureDocumentUtils.DEFAULT_TIMESTAMP_VALIDITY)), JsonObject.class);
                } catch (JsonSyntaxException | InvalidKeyException | IllegalBlockSizeException
                        | BadPaddingException | InvalidSignatureException | OutdatedSignatureException e) {
                    throw new UserException(INVALID_RESPONSE);
                }
            } else if (response.getStatusLine().getStatusCode() == 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                throw new UserException(responseBody);
            } else {
                throw new UserException("An error occured on the server");
            }
        } catch (URISyntaxException | IOException e) {
            throw new UserException("Failed to connect to the server");
        }
    }

    public JsonObject getBalance(int accountId) throws UserException {
        try {
            URI uri = new URIBuilder()
                    .setScheme(URI_SCHEME)
                    .setHost(hostname)
                    .setPath("/users/" + currentUser + "/accounts/" + accountId + "/overview")
                    .build();
            HttpGet httpGet = new HttpGet(uri);

            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                String responseBody = EntityUtils.toString(response.getEntity());

                // Use Gson to parse JSON response
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                try {
                    debug(jsonObject.toString());
                    return gson.fromJson(new String(SecureDocument.unprotect(jsonObject, sessionKey, SecureDocumentUtils.DEFAULT_TIMESTAMP_VALIDITY)), JsonObject.class);
                } catch (JsonSyntaxException | InvalidKeyException | IllegalBlockSizeException
                        | BadPaddingException | InvalidSignatureException | OutdatedSignatureException e) {
                    throw new UserException(INVALID_RESPONSE);
                }
            } else if (response.getStatusLine().getStatusCode() == 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                throw new UserException(responseBody);
            } else {
                throw new UserException("An error occured on the server");
            }
        } catch (URISyntaxException | IOException e) {
            throw new UserException("Failed to connect to the server");
        }
    }

    public JsonObject getPendingTransfers() throws UserException {
        try {
            URI uri = new URIBuilder()
                    .setScheme(URI_SCHEME)
                    .setHost(hostname)
                    .setPath("/users/" + currentUser + "/pending_transfers")
                    .build();
            HttpGet httpGet = new HttpGet(uri);

            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                String responseBody = EntityUtils.toString(response.getEntity());

                // Use Gson to parse JSON response
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);

                try {
                    debug(jsonObject.toString());
                    return gson.fromJson(new String(SecureDocument.unprotect(jsonObject, sessionKey, SecureDocumentUtils.DEFAULT_TIMESTAMP_VALIDITY)), JsonObject.class);
                } catch (JsonSyntaxException | InvalidKeyException | IllegalBlockSizeException
                        | BadPaddingException | InvalidSignatureException | OutdatedSignatureException e) {
                    throw new UserException(INVALID_RESPONSE);
                }
            } else if (response.getStatusLine().getStatusCode() == 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                throw new UserException(responseBody);
            } else {
                throw new UserException("An error occured on the server");
            }
        } catch (URISyntaxException | IOException e) {
            throw new UserException("Failed to connect to the server");
        }
    }

    public JsonObject createAccount(List<String> accountHolders, String currency) throws UserException {
        try {
            URI uri = new URIBuilder()
                    .setScheme(URI_SCHEME)
                    .setHost(hostname)
                    .setPath("users/" + currentUser + "/accounts/register")
                    .build();

            JsonObject json = new JsonObject();
            JsonArray accountHoldersJson = new JsonArray();
            for (String holder : accountHolders) {
                accountHoldersJson.add(holder);
            }
            json.add("accountHolders", accountHoldersJson);
            json.addProperty("currency", currency);

            try {
                json = SecureDocument.protect(json.toString().getBytes(), keystoreService.loadKeyPairFromKeystore(username,"pass").getPrivate(), sessionKey);
            } catch (GeneralSecurityException e) {
                throw new UserException(e.getMessage());
            }

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
                    return gson.fromJson(new String(SecureDocument.unprotect(jsonObject, sessionKey, SecureDocumentUtils.DEFAULT_TIMESTAMP_VALIDITY)), JsonObject.class);
                } catch (JsonSyntaxException | InvalidKeyException | IllegalBlockSizeException
                        | BadPaddingException | InvalidSignatureException | OutdatedSignatureException e) {
                    throw new UserException(INVALID_RESPONSE);
                }
            } else if (response.getStatusLine().getStatusCode() == 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                throw new UserException(responseBody);
            } else {
                throw new UserException("An error occured on the server");
            }

        } catch (URISyntaxException | IOException e) {
            throw new UserException("Failed to connect to the server");
        }
    }

    public JsonObject getAccountTransfers(int accountId) throws UserException {
        try {
            URI uri = new URIBuilder()
                    .setScheme(URI_SCHEME)
                    .setHost(hostname)
                    .setPath("/users/" + currentUser + "/accounts/" + accountId + "/overview")
                    .build();
            HttpGet httpGet = new HttpGet(uri);

            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                String responseBody = EntityUtils.toString(response.getEntity());

                // Use Gson to parse JSON response
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);

                try {
                    debug(jsonObject.toString());
                    return gson.fromJson(new String(SecureDocument.unprotect(jsonObject, sessionKey, SecureDocumentUtils.DEFAULT_TIMESTAMP_VALIDITY)), JsonObject.class);
                } catch (JsonSyntaxException | InvalidKeyException | IllegalBlockSizeException
                        | BadPaddingException | InvalidSignatureException | OutdatedSignatureException e) {
                    throw new UserException(INVALID_RESPONSE);
                }
            } else if (response.getStatusLine().getStatusCode() == 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                throw new UserException(responseBody);
            } else {
                throw new UserException("An error occured on the server");
            }
        } catch (URISyntaxException | IOException e) {
            throw new UserException("Failed to connect to the server");
        }
    }

    public Integer transferTo(int accountId, int destinationAccountId, float amount, String description)
            throws UserException {
        try {
            URI uri = new URIBuilder()
                    .setScheme(URI_SCHEME)
                    .setHost(hostname)
                    .setPath("users/" + currentUser + "/transfers/register")
                    .build();

            JsonObject json = new JsonObject();
            json.addProperty("origin", accountId);
            json.addProperty("destination", destinationAccountId);
            json.addProperty("amount", amount);
            json.addProperty("description", description);

            try {
                json = SecureDocument.protect(json.toString().getBytes(), keystoreService.loadKeyPairFromKeystore(username,"pass").getPrivate(), sessionKey);
            } catch (GeneralSecurityException e) {
                throw new UserException(e.getMessage());
            }

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
                    return gson.fromJson(new String(SecureDocument.unprotect(jsonObject, sessionKey, SecureDocumentUtils.DEFAULT_TIMESTAMP_VALIDITY)), JsonObject.class).get("id").getAsInt();
                } catch (JsonSyntaxException | InvalidKeyException | IllegalBlockSizeException
                        | BadPaddingException | InvalidSignatureException | OutdatedSignatureException e) {
                    throw new UserException(INVALID_RESPONSE);
                }
            } else if (response.getStatusLine().getStatusCode() == 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                throw new UserException(responseBody);
            } else {
                throw new UserException("An error occured on the server");
            }

        } catch (URISyntaxException | IOException e) {
            throw new UserException("Failed to connect to the server");
        }
    }

    public JsonObject authorizeTransfer(int transferId) throws UserException {
        try {
            URI uri = new URIBuilder()
                    .setScheme(URI_SCHEME)
                    .setHost(hostname)
                    .setPath("users/" + currentUser + "/transfers/confirm")
                    .build();

            JsonObject json = new JsonObject();
            json.addProperty("transfer", transferId);

            try {
                json = SecureDocument.protect(json.toString().getBytes(), keystoreService.loadKeyPairFromKeystore(username,"pass").getPrivate(), sessionKey);
            } catch (GeneralSecurityException e) {
                throw new UserException(e.getMessage());
            }
            
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
                    return gson.fromJson(new String(SecureDocument.unprotect(jsonObject, sessionKey, SecureDocumentUtils.DEFAULT_TIMESTAMP_VALIDITY)), JsonObject.class);
                } catch (JsonSyntaxException | InvalidKeyException | IllegalBlockSizeException
                        | BadPaddingException | InvalidSignatureException | OutdatedSignatureException e) {
                    throw new UserException(INVALID_RESPONSE);
                }
            } else if (response.getStatusLine().getStatusCode() == 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                throw new UserException(responseBody);
            } else {
                throw new UserException("An error occured on the server");
            }

        } catch (URISyntaxException | IOException e) {
            throw new UserException("Failed to connect to the server");
        }
    }

    public JsonObject cancelTransfer(int transferId) throws UserException {
        try {
            URI uri = new URIBuilder()
                    .setScheme(URI_SCHEME)
                    .setHost(hostname)
                    .setPath("users/" + currentUser + "/transfers/cancel")
                    .build();

            JsonObject json = new JsonObject();
            json.addProperty("transfer", transferId);

            try {
                json = SecureDocument.protect(json.toString().getBytes(), keystoreService.loadKeyPairFromKeystore(username,"pass").getPrivate(), sessionKey);
            } catch (GeneralSecurityException e) {
                throw new UserException(e.getMessage());
            }
            
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
                    return gson.fromJson(new String(SecureDocument.unprotect(jsonObject, sessionKey, SecureDocumentUtils.DEFAULT_TIMESTAMP_VALIDITY)), JsonObject.class);
                } catch (JsonSyntaxException | InvalidKeyException | IllegalBlockSizeException
                        | BadPaddingException | InvalidSignatureException | OutdatedSignatureException e) {
                    throw new UserException(INVALID_RESPONSE);
                }
            } else if (response.getStatusLine().getStatusCode() == 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                throw new UserException(responseBody);
            } else {
                throw new UserException("An error occured on the server");
            }

        } catch (URISyntaxException | IOException e) {
            throw new UserException("Failed to connect to the server");
        }
    }
}
