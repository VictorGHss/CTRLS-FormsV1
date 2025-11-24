package br.dev.ctrls.api.application.service.submission;

/**
 * Exceção lançada quando há erro na integração com a API Feegow.
 */
public class FeegowIntegrationException extends RuntimeException {

    public FeegowIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FeegowIntegrationException(String message) {
        super(message);
    }
}

