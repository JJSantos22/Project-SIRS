package pt.tecnico.blingbank.user;

import java.io.IOException;
import java.util.Scanner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import pt.tecnico.blingbank.user.exceptions.UserException;
import static pt.tecnico.blingbank.user.exceptions.UserException.ErrorMessages.*;

public class UserCommandParser {

    private static final String GET_ACCOUNTS = "accounts";
    private static final String GET_BALANCE = "balance";
    private static final String GET_ACCOUNT_TRANSFERS = "accountTransfers";
    private static final String TRANSFER_TO = "transferTo";
    private static final String GET_PENDING_TRANSFERS = "pendingTransfers";
    private static final String AUTHORIZE_TRANSFER = "authorizeTransfer";
    private static final String CANCEL_TRANSFER = "cancelTransfer";
    private static final String CREATE_ACCOUNT = "createAccount";
    private static final String LOGOUT = "logout";
    private static final String SPACE = " ";

    private Scanner scanner = new Scanner(System.in);

    private UserService userService;

    public UserCommandParser(UserService userService) {
        this.userService = userService;
    }

    void parseInput() throws IOException {

        boolean logout = false;

        while (true) {
            System.out.println();
            help();
            System.out.print(userService.getCurrentUsername() + "> ");
            String line = scanner.nextLine().trim();
            String cmd = line.split(SPACE)[0];

            try {
                switch (cmd) {
                    case GET_ACCOUNTS:
                    case "1":
                        getAccounts();
                        break;

                    case GET_BALANCE:
                    case "2":
                        getBalance();
                        break;

                    case GET_ACCOUNT_TRANSFERS:
                    case "3":
                        getAccountTransfers();
                        break;

                    case TRANSFER_TO:
                    case "4":
                        transferTo();
                        break;

                    case GET_PENDING_TRANSFERS:
                    case "5":
                        getPendingTransfers();
                        break;

                    case AUTHORIZE_TRANSFER:
                    case "6":
                        authorizeTransfer();
                        break;

                    case CANCEL_TRANSFER:
                    case "7":
                        cancelTransfer();
                        break;

                    case CREATE_ACCOUNT:
                    case "8":
                        createAccount();
                        break;

                    case LOGOUT:
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

            System.out.print("[Press ENTER to continue]");
            scanner.nextLine();
        }
    }

    private void getAccounts() throws UserException {
        JsonObject json = userService.getAccounts();
        JsonArray jsonArray = json.get("userAccounts").getAsJsonArray();
        if (jsonArray.size() == 0) {
            System.out.println("No accounts");
            return;
        }
        System.out.println("Your Accounts:");
        for (JsonElement jsonElement : jsonArray) {
            JsonObject obj = jsonElement.getAsJsonObject();
            String str = "Account id:" + obj.get("id").getAsInt() + "   Account holders:";
            for (JsonElement holder : obj.get("accountholders").getAsJsonArray()) {
                str += " " + holder.getAsJsonObject().get("username").getAsString();
            }
            System.out.println(str) ;
        } 
        

        
    }
    

    private void getBalance() throws UserException {
        try {

            System.out.print("Enter the account id: ");
            int accountId = Integer.parseInt(scanner.nextLine());

            JsonObject json = userService.getBalance(accountId);
            System.out.println("Balance: " + json.get("balance").getAsInt() + " " + json.get("currency").getAsString());
        } catch (NumberFormatException e) {
            throw new UserException(INVALID_ARG_FORMAT);
        }
    }

    private void getAccountTransfers() throws UserException {
        try {
            System.out.print("Enter the account id: ");
            int accountId = Integer.parseInt(scanner.nextLine());
            JsonObject json = userService.getAccountTransfers(accountId);
            JsonArray jsonArray = json.get("transfers").getAsJsonArray();
            if (jsonArray.size() == 0) {
                System.out.println("No transfers");
                return;
            }
            System.out.println("Account number " +accountId + " transfers:" +"\n"
            +"-------------------------------------------------");
            for (JsonElement jsonElement : jsonArray) {
                JsonObject obj = jsonElement.getAsJsonObject();
                String str = "Transfer id:" + obj.get("id").getAsInt() + "\n"
                    + "Source account id:" + obj.get("originId").getAsInt()
                    + "   Destination account id:" + obj.get("destinationId").getAsInt() + "\n"
                    + "Amount:" + obj.get("amount").getAsInt() + " " 
                    + json.get("currency").getAsString() + "\n"
                    + "Description:" + obj.get("description").getAsString() + "\n"
                    + "Status:" + obj.get("status").getAsString() + "\n"
                    + "-------------------------------------------------";
                System.out.println(str) ;
            }
        } catch (NumberFormatException e) {
            throw new UserException(INVALID_ARG_FORMAT);
        }
    }

    /**
     * Transfers money to another account
     * 
     * @throws UserException
     */
    private void transferTo() throws UserException {
        try {
            System.out.print("Enter the source account id: ");
            int sourceAccountId = Integer.parseInt(scanner.nextLine());
            System.out.print("Enter the destination account id: ");
            int destinationAccountId = Integer.parseInt(scanner.nextLine());
            System.out.print("Enter the amount to transfer: ");
            int amount = Integer.parseInt(scanner.nextLine());
            System.out.print("Enter a description: ");
            String description = scanner.nextLine();

            Integer transferId = userService.transferTo(sourceAccountId, destinationAccountId, amount, description);
            System.out.println("Transfer created");
            userService.authorizeTransfer(transferId);
            
        } catch (NumberFormatException e) {
            throw new UserException(INVALID_ARG_FORMAT);
        }
        
    }

    private void getPendingTransfers() throws UserException {
        JsonObject json = userService.getPendingTransfers();
        JsonArray jsonArray = json.get("pendingTransfers").getAsJsonArray();
        if (jsonArray.size() == 0) {
            System.out.println("No pending transfers");
            return;
        }
        System.out.println("Pending transfers:" +"\n"+"-------------------------------------------------");
        for (JsonElement jsonElement : jsonArray) {
            JsonObject obj = jsonElement.getAsJsonObject();
            String str = "Transfer id:" + obj.get("id").getAsInt() + "\n"
                + "Source account id:" + obj.get("originId").getAsInt()
                + "   Destination account id:" + obj.get("destinationId").getAsInt() + "\n"
                + "Amount:" + obj.get("amount").getAsInt() + "\n"
                + "Description:" + obj.get("description").getAsString() + "\n"
                + "Status:" + obj.get("status").getAsString() + "\n"
                + "-------------------------------------------------";
            System.out.println(str) ;
        }
    }

    private void authorizeTransfer() throws UserException {
        try {
            System.out.print("Enter the transfer id: ");
            int transferId = Integer.parseInt(scanner.nextLine());

            JsonObject json = userService.authorizeTransfer(transferId);
            System.out.println("Transfer authorized:");
            JsonObject obj = json.get("transfer").getAsJsonObject();
            String str = "-------------------------------------------------"+ "\n" +
                "Transfer id:" + obj.get("id").getAsInt() + "\n"
                + "Source account id:" + obj.get("originId").getAsInt()
                + "   Destination account id:" + obj.get("destinationId").getAsInt() + "\n"
                + "Amount:" + obj.get("amount").getAsInt() + "\n"
                + "Description:" + obj.get("description").getAsString() + "\n"
                + "Status:" + obj.get("status").getAsString() + "\n"
                + "-------------------------------------------------";
            System.out.println(str) ;
        } catch (NumberFormatException e) {
            throw new UserException(INVALID_ARG_FORMAT);
        }
    }

    private void cancelTransfer() throws UserException {
        try {
            System.out.print("Enter the transfer id: ");
            int transferId = Integer.parseInt(scanner.nextLine());

            JsonObject json = userService.cancelTransfer(transferId);
            System.out.println("Transfer canceled:");
            JsonObject obj = json.get("transfer").getAsJsonObject();
            String str = "-------------------------------------------------"+ "\n" +
                "Transfer id:" + obj.get("id").getAsInt() + "\n"
                + "Source account id:" + obj.get("originId").getAsInt()
                + "   Destination account id:" + obj.get("destinationId").getAsInt() + "\n"
                + "Amount:" + obj.get("amount").getAsInt() + "\n"
                + "Description:" + obj.get("description").getAsString() + "\n"
                + "Status:" + obj.get("status").getAsString() + "\n"
                + "-------------------------------------------------";
            System.out.println(str) ;
        } catch (NumberFormatException e) {
            throw new UserException(INVALID_ARG_FORMAT);
        }
    }

    private void createAccount() throws UserException {
        try { 
            System.out.println("Enter the usernames of other account holders separated by enter (leave blank to finish):");
            List<String> accountHoldersArray = new ArrayList<>();
            String line = scanner.nextLine();
            while (!line.isBlank()) {
                accountHoldersArray.add(line);
                line = scanner.nextLine();
            }
            System.out.println("Enter the currency: ");
            String currency = scanner.nextLine();

            JsonObject json = userService.createAccount(accountHoldersArray, currency);
            String str = "\n" + "Account created with id " + json.get("id").getAsInt() 
                + "  -  Balance: " + json.get("balance").getAsInt()
                + " " + json.get("currency").getAsString() + "";
            System.out.println(str) ;
        } catch (NumberFormatException e) {
            throw new UserException(INVALID_ARG_FORMAT);
        }

    }

    /**
     * Prints the usage of the user commands
     */
    private void help() {
        System.out.println("1 - accounts");
        System.out.println("2 - balance");
        System.out.println("3 - accountTransfers");
        System.out.println("4 - transferTo");
        System.out.println("5 - pendingTransfers");
        System.out.println("6 - authorizeTransfer");
        System.out.println("7 - cancelTransfer");
        System.out.println("8 - createAccount");
        System.out.println("0 - logout");
    }
}