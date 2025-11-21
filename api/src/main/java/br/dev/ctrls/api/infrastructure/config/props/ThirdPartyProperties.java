package br.dev.ctrls.api.infrastructure.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de integrações externas.
 */
@ConfigurationProperties(prefix = "third-party")
public class ThirdPartyProperties {

    private final Brevo brevo = new Brevo();
    private final Cloudinary cloudinary = new Cloudinary();
    private final Integration integration = new Integration();

    public Brevo getBrevo() {
        return brevo;
    }

    public Cloudinary getCloudinary() {
        return cloudinary;
    }

    public Integration getIntegration() {
        return integration;
    }

    public static class Brevo {
        private String apiKey;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class Cloudinary {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class Integration {
        private Feegow feegow = new Feegow();

        public Feegow getFeegow() {
            return feegow;
        }

        public void setFeegow(Feegow feegow) {
            this.feegow = feegow;
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
}

