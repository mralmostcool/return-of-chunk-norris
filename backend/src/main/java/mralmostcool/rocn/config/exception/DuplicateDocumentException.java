package mralmostcool.rocn.config.exception;

import org.springframework.http.HttpStatus;

public class DuplicateDocumentException extends RagException {
    public DuplicateDocumentException(String existingId) {
        super(HttpStatus.CONFLICT, "DUPLICATE_DOCUMENT",
                "A document with identical content already exists: " + existingId);
    }
}
