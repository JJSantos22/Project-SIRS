package pt.tecnico.blingbank.user;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import pt.tecnico.blingbank.library.KeystoreService;

public class UserMain {
    
    public static void main(String[] args) throws IOException {


        boolean debug=false;
        if (args.length > 0) {
            if (args[0].equals("-v")) {
                debug=true;
            }
        }
        

        Properties config = loadConfig();

        String truststoreType = config.getProperty("truststore.type");
        String truststorePath = config.getProperty("truststore.path");
        String truststorePassword = config.getProperty("truststore.password");

        String serverHostname = config.getProperty("server.hostname");
        String serverPort = config.getProperty("server.port");
        String fullServerHostname = serverHostname + ":" + serverPort;

        String keystoreType = config.getProperty("keystore.type");
        String keystorePath = config.getProperty("keystore.path");
        String keystorePassword = config.getProperty("keystore.password");

        String initial_key_path = config.getProperty("initialKey.path");
        String keyPassword = config.getProperty("initialKey.password");
        String alias = config.getProperty("initialKey.alias");

        /* Setup http client */
        SSLContext sslContext;
        try {
            sslContext = customSSLContext(truststoreType, truststorePath, truststorePassword);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();

                
        KeystoreService keystoreService = new KeystoreService(keystoreType, keystorePath, keystorePassword);
        SystemService systemService = new SystemService(httpClient, fullServerHostname, keystoreService, debug);
        UserService userService = new UserService(httpClient, fullServerHostname, -1, keystoreService, debug);
        
        /* Setup initial secret key */
        try {
            keystoreService.storeSecretKeyInKeystore(initial_key_path, alias, keyPassword);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
                
        /* LOGIN */
        System.out.println("\t***Welcome to BlingBank!***\n");
        SystemCommandParser systemParser = new SystemCommandParser(systemService, userService);
        try {
            systemParser.parseInput();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        httpClient.close();
    }

    private static Properties loadConfig() {
        // Load the configuration from the properties file
        Properties config = new Properties();
        try (InputStream input = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("application.properties")) {
            config.load(input);
        } catch (Exception e) {
            e.printStackTrace(); // Handle the exception appropriately in a real-world scenario
        }
        return config;
    }

    private static SSLContext customSSLContext(String truststoreType, String truststorePath, String truststorePassword)
            throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException,
            KeyManagementException, UnrecoverableKeyException {
        // Load the JKS truststore
        KeyStore truststore = KeyStore.getInstance(truststoreType);
        try (InputStream truststoreInputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(truststorePath)) {
            truststore.load(truststoreInputStream, truststorePassword.toCharArray());
        }

        // Create an SSL context with the loaded truststore using SSLContextBuilder
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(truststore, null)
                .build();

        return sslContext;
    }

}
