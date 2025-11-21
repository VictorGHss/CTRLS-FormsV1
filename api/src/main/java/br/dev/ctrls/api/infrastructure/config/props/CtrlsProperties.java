package br.dev.ctrls.api.infrastructure.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades específicas da aplicação CTRLS.
 */
@ConfigurationProperties(prefix = "ctrls")
public class CtrlsProperties {

    private final Security security = new Security();
    private final App app = new App();

    public Security getSecurity() {
        return security;
    }

    public App getApp() {
        return app;
    }

    public static class Security {
        private final Jwt jwt = new Jwt();

        public Jwt getJwt() {
            return jwt;
        }

        public static class Jwt {
            private String secret;
            private long expirationMs;

            public String getSecret() {
                return secret;
            }

            public void setSecret(String secret) {
                this.secret = secret;
            }

            public long getExpirationMs() {
                return expirationMs;
            }

            public void setExpirationMs(long expirationMs) {
                this.expirationMs = expirationMs;
            }
        }
    }

    public static class App {
        private String clientUrl;

        public String getClientUrl() {
            return clientUrl;
        }

        public void setClientUrl(String clientUrl) {
            this.clientUrl = clientUrl;
        }
    }
}

