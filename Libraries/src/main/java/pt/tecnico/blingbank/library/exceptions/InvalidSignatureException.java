package pt.tecnico.blingbank.library.exceptions;

public class InvalidSignatureException extends Exception {

    public InvalidSignatureException() {
        super("The signature does not match the file.");
    }

}
