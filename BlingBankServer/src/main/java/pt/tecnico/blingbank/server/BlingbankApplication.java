package pt.tecnico.blingbank.server;

import java.io.FileNotFoundException;
import java.io.IOException;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import pt.tecnico.blingbank.library.KeystoreService;

@SpringBootApplication
public class BlingbankApplication {

	private static final String keystorePath = "src/main/resources/server.p12";
	private static final String keystorePassword = "server";
	private static final String alias = "0";
	private static final String keyPassword = "server";
	private static final String initial_key_path = "src/main/resources/certificates/initial.key";
    private static final String keystoreType = "PKCS12";

	public static void main(String[] args) throws FileNotFoundException, IOException {
        KeystoreService keystoreService = new KeystoreService(keystoreType, keystorePath, keystorePassword);
        try {
			keystoreService.storeSecretKeyInKeystore(initial_key_path, alias, keyPassword);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		SpringApplication.run(BlingbankApplication.class, args);
	}

}
