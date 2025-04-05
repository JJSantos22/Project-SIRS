package pt.tecnico.blingbank.user;

import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Scanner;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.JsonObject;

import pt.tecnico.blingbank.library.exceptions.InvalidSignatureException;
import pt.tecnico.blingbank.user.exceptions.UserException;
import static pt.tecnico.blingbank.user.exceptions.UserException.ErrorMessages.*;

public class SystemCommandParser {

    private static final String SYM_KEY_ALGO = "AES";

    private static final String REGISTER = "register";
    private static final String LOGIN = "login";
    private static final String EXIT = "exit";
    private static final String SPACE = " ";

    Scanner scanner = new Scanner(System.in);

    private SystemService systemService;
    private UserService userService;

    public SystemCommandParser(SystemService systemService, UserService userService) {
        this.systemService = systemService;
        this.userService = userService;
    }

    void parseInput() throws Exception {

        boolean logout = false;        
        while (true) {
            help();
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            System.out.println();
            String cmd = line.split(SPACE)[0];

            try {
                switch (cmd) {
                    case REGISTER:
                    case "1":
                        register();
                        break;

                    case LOGIN:
                    case "2":
                        login();
                        break;

                    case EXIT:
                    case "0":
                        logout = true;
                        break;

                    default:
                        throw new UserException(UNKNOWN_COMMAND);
                }
            } catch (UserException e) {
                System.err.println(e.getErrorMessage());
            }

            if (logout) {
                break;
            }
        }
    }

    /**
     * Register a new account
     * 
     * @throws UserException
     */
    private void register() throws UserException {
        System.out.println("Please enter a username");
        String username = scanner.nextLine();

        JsonObject json = systemService.register(username);
        if (!json.has("key")
                || !json.get("key").isJsonPrimitive()
                || !json.get("key").getAsJsonPrimitive().isString()) {
            throw new UserException("Invalid response");
        }
        if (!json.has("accountHolder")
                || !json.get("accountHolder").isJsonObject()) {
            throw new UserException("Invalid response");
        }

        JsonObject accountHolderObject = json.getAsJsonObject("accountHolder");

        if (!accountHolderObject.has("id")
                || !accountHolderObject.get("id").isJsonPrimitive() 
                || !accountHolderObject.get("id").getAsJsonPrimitive().isNumber()) {
            throw new UserException("Invalid response");
        }
        if (!accountHolderObject.has("username")
                || !accountHolderObject.get("username").isJsonPrimitive() 
                || !accountHolderObject.get("username").getAsJsonPrimitive().isString()) {
            throw new UserException("Invalid response");
        }

        byte[] keyBytes = Base64.getDecoder().decode(json.get("key").getAsString());
        SecretKey key = new SecretKeySpec(keyBytes, SYM_KEY_ALGO);
        int userId = accountHolderObject.get("id").getAsInt();

        lauchUser(username, userId, key, userService);
    }

    /**
     * Login to an existing account
     * @throws UserException
     * @throws InvalidSignatureException
     * @throws InvalidKeySpecException
     */
    private void login() throws UserException, InvalidKeySpecException, InvalidSignatureException {
        System.out.println("Please enter your username");
        String username = scanner.nextLine();

        JsonObject json = systemService.login(username);

        if (!json.has("key")
                || !json.get("key").isJsonPrimitive()
                || !json.get("key").getAsJsonPrimitive().isString()) {
            throw new UserException("Invalid response");
        }
        if (!json.has("accountHolder")
                || !json.get("accountHolder").isJsonObject()) {
            throw new UserException("Invalid response");
        }

        JsonObject accountHolderObject = json.getAsJsonObject("accountHolder");

        if (!accountHolderObject.has("id")
                || !accountHolderObject.get("id").isJsonPrimitive() 
                || !accountHolderObject.get("id").getAsJsonPrimitive().isNumber()) {
            throw new UserException("Invalid response");
        }
        if (!accountHolderObject.has("username")
                || !accountHolderObject.get("username").isJsonPrimitive() 
                || !accountHolderObject.get("username").getAsJsonPrimitive().isString()) {
            throw new UserException("Invalid response");
        }

        byte[] keyBytes = Base64.getDecoder().decode(json.get("key").getAsString());
        SecretKey key = new SecretKeySpec(keyBytes, SYM_KEY_ALGO);
        int userId = accountHolderObject.get("id").getAsInt();

        lauchUser(username, userId, key, userService);
    }

    private void lauchUser(String username, int userId, SecretKey key, UserService userService) {
        System.out.println("\n\t***Hello " + username + "!***");
        UserCommandParser parser = new UserCommandParser(userService);
        userService.setCurrentUser(userId);
        userService.setSessionKey(key);
        userService.setUsername(username);
        try {
            parser.parseInput();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        userService.setCurrentUser(-1);
        System.out.println("\t***Goodbye " + username + "!***\n");
    }

    /**
     * Prints the help menu
     */
    private void help() {
        System.out.println("1 - Register");
        System.out.println("2 - Login");
        System.out.println("0 - Exit");
    }
}
