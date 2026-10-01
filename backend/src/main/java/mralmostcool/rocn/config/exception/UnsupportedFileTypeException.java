package mralmostcool.rocn.config.exception;

import org.springframework.http.HttpStatus;

public class UnsupportedFileTypeException extends RagException {
    public UnsupportedFileTypeException(String detected) {
        super(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILE_TYPE", "Unsupported file type: " + detected);
    }
}
