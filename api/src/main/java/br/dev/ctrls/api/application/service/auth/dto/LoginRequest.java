package br.dev.ctrls.api.application.service.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload de autenticação.
 */
public record LoginRequest(
        @Email String email,
        @NotBlank String password,
        @NotBlank String clinicId
) {
}

