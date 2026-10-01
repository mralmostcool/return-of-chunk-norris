package mralmostcool.rocn.config.exception;

import org.springframework.http.HttpStatus;

public class DocumentNotFoundException extends RagException {
    public DocumentNotFoundException(String id) {
        super(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Document not found: " + id);
    }
}
