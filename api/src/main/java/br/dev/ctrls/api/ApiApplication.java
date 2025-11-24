package br.dev.ctrls.api;

import br.dev.ctrls.api.infrastructure.config.props.BrevoProperties;
import br.dev.ctrls.api.infrastructure.config.props.CloudinaryProperties;
import br.dev.ctrls.api.infrastructure.config.props.CtrlsProperties;
import br.dev.ctrls.api.infrastructure.config.props.IntegrationProperties;
import br.dev.ctrls.api.infrastructure.config.props.RateLimiterProperties;
import br.dev.ctrls.api.infrastructure.config.props.ThirdPartyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableFeignClients(basePackages = "br.dev.ctrls.api.client")
@EnableConfigurationProperties({
        CtrlsProperties.class,
        BrevoProperties.class,
        CloudinaryProperties.class,
        IntegrationProperties.class,
        ThirdPartyProperties.class,
        RateLimiterProperties.class
})
@EnableCaching
@EnableRetry
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

}