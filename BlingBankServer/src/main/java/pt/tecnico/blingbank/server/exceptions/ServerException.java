package pt.tecnico.blingbank.server.exceptions;

public class ServerException extends RuntimeException {
    private final ErrorMessage errorMessage;

    public ServerException(ErrorMessage errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }
}