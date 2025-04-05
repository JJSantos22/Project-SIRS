package pt.tecnico.blingbank.server.exceptions;

public enum ErrorMessage {
        PROBLEM_SERVER_ERROR("Error: A problem occurred in the server"),
        PERMISSION_DENIED("Error: Permission denied"),

        INVALID_ACCOUNT("Error: Invalid account"),
        INVALID_ACCOUNT_HOLDER("Error: Invalid account holder"),
        INVALID_AMOUNT("Error: Invalid ammount"),
        INVALID_DESCRIPTION("Error: Invalid description"),
        INVALID_DESTINATION_ACCOUNT("Error: invalid destination account"),
        INVALID_ID("Error: Invalid id"),
        INVALID_KEY("Error: Invalid key"),
        INVALID_ORIGIN_ACCOUNT("Error: invalid origin account"),
        INVALID_REQUEST("Error: Invalid request"),
        INVALID_SIGNATURE("Error: Invalid signature"),
        INVALID_TRANSFER_ID("Error: Invalid transfer id"),
        INVALID_USERNAME("Invalid username"),
        INVALID_USER("Error: Invalid user"),
        
        ACCOUNT_NOT_FOUND("Error: Account not found"),
        DESTINATION_NOT_FOUND("Error: Destination account not found"),
        ORIGIN_NOT_FOUND("Error: Origin account not found"),
        PENDING_TRANSFER_NOT_FOUND("Error: Pending transfer not found"),
        USER_ALREADY_EXISTS("Error: User already exists"),
        USER_NOT_FOUND("Error: User not found"),

        NOT_ENOUGH_FUNDS("Error: Not enough funds to transfer"),
        INCOMPATIBLE_CURRENCIES("Error: Origin and destination accounts have different currencies"),
        ORIGIN_EQUALS_DESTINATION("Error: Origin and destination accounts are the same"),
        CONFIRMATION_ALREADY_GIVEN("Error: Confirmation already given"),
        
        UNKNOWN_ACCOUNT_HOLDER("Error: Unknown account holder"),
        UNKNOWN_TRANSFER("Error: Unknown transfer"),

        DUPLICATE_ACCOUNT_HOLDER("Error: Duplicate account holder");


        public final String label;

        ErrorMessage(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
}