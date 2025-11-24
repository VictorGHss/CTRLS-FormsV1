package br.dev.ctrls.api.application.service.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta de autenticação com o JWT.
 */
@Schema(description = "Resposta do login contendo o token JWT")
public record LoginResponse(
        @Schema(description = "Token JWT para autenticação nas APIs protegidas. " +
                              "Deve ser enviado no header Authorization como 'Bearer {token}'. " +
                              "Válido por 1 hora.",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token
) {
}

