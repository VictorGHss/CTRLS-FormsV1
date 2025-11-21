package br.dev.ctrls.api.client.feegow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

/**
 * Estrutura enviada ao Feegow para criação de paciente.
 */
@Getter
@Builder
public class FeegowPatientRequest {

    @NotBlank
    private final String nome;

    @NotBlank
    private final String cpf;

    @NotBlank
    private final String sexo;

    @NotBlank
    private final String nascimento;
}
