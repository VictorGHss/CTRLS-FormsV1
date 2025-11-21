package br.dev.ctrls.api.infrastructure.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    // Mapeia "rate-limiter.public-api"
    private final PublicApi publicApi = new PublicApi();

    @Data
    public static class PublicApi {
        private Integer bucketCapacity;
        private Integer refillTokens;
        private Integer refillSeconds;
    }
}