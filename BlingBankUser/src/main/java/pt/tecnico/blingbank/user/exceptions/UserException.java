package pt.tecnico.blingbank.user.exceptions;

public class UserException extends Exception {
    public final class ErrorMessages {
        private ErrorMessages() {}
        
        public static final String IO_ERROR = "Error: Couldn't connect to the server";

        public static final String CLOSED_APPLICATION = "Application closed";

        public static final String INVALID_COMMAND = "Error: Invalid Command";
        public static final String INVALID_NUM_ARGS = "Error: Wrong number of arguments for command";
        public static final String UNKNOWN_COMMAND = "Error: Unknown command";
        public static final String INVALID_ARG_FORMAT = "Error: Invalid argument type";
        public static final String PASSWORD_MISMATCH = "Error: Passwords don't match";
        public static final String INVALID_PASSWORD = "Error: Invalid password";
        public static final String INVALID_USERNAME = "Error: Invalid username";
        public static final String INVALID_RESPONSE = "Error: Invalid response from server";
    }
    private final String errorMessage;

    public UserException(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}