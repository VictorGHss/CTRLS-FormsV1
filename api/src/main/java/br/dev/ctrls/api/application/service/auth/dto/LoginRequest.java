package br.dev.ctrls.api.application.service.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload de autenticação com validações completas.
 */
@Schema(description = "Requisição de login no sistema")
public record LoginRequest(
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        @Schema(description = "Email do usuário", example = "medico@clinica.com")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        @Schema(description = "Senha do usuário", example = "senha123", minLength = 6)
        String password,

        @NotBlank(message = "ID da clínica é obrigatório")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                 message = "ID da clínica deve ser um UUID válido")
        @Schema(description = "UUID da clínica", example = "123e4567-e89b-12d3-a456-426614174000")
        String clinicId
) {
}

