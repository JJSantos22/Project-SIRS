package pt.tecnico.blingbank.library.exceptions;

public class OutdatedSignatureException extends Exception {

    public OutdatedSignatureException(long tsDifference) {
        super("The encrypted file is not fresh - " + tsDifference + "ms outdated.");
    }
}
