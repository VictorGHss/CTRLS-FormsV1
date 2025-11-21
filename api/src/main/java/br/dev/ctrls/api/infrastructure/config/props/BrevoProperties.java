package br.dev.ctrls.api.infrastructure.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações da integração com a Brevo.
 */
@ConfigurationProperties(prefix = "brevo")
public class BrevoProperties {

    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}

