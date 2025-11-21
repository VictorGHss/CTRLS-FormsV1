package br.dev.ctrls.api.config;

import br.dev.ctrls.api.tenant.TenantContextHolder;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração base para Feign.
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor tenantHeaderInterceptor() {
        return template -> template.header("X-Clinic-ID", TenantContextHolder.getCurrentTenantId().orElse("public"));
    }
}
