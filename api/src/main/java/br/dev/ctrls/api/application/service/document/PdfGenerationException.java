package br.dev.ctrls.api.application.service.document;

/**
 * Exceção lançada quando há erro na geração de PDF.
 */
public class PdfGenerationException extends RuntimeException {

    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PdfGenerationException(String message) {
        super(message);
    }
}

