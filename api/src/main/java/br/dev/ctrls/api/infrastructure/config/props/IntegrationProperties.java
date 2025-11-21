package br.dev.ctrls.api.infrastructure.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações de integrações internas (Feegow etc.).
 */
@ConfigurationProperties(prefix = "integration")
public class IntegrationProperties {

    private final Feegow feegow = new Feegow();

    public Feegow getFeegow() {
        return feegow;
    }

    public static class Feegow {
        private String baseUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}

