package br.dev.ctrls.api.tenant;

import java.util.Optional;

/**
 * Mantém o identificador do tenant durante o ciclo da requisição.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setTenantId(String tenantId) {
        TENANT.set(tenantId);
    }

    public static Optional<String> getCurrentTenantId() {
        return Optional.ofNullable(TENANT.get());
    }

    public static void clear() {
        TENANT.remove();
    }
}

