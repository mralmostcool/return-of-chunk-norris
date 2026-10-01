package mralmostcool.rocn.config.exception;

import org.springframework.http.HttpStatus;

public class InvalidFilenameException extends RagException {
    public InvalidFilenameException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_FILENAME", message);
    }
}
