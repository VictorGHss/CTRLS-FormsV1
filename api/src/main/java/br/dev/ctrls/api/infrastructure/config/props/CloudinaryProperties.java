package br.dev.ctrls.api.infrastructure.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações da integração com Cloudinary.
 */
@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryProperties {

    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

