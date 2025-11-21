package br.dev.ctrls.api.web.dto;

import br.dev.ctrls.api.client.feegow.dto.FeegowPatientRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmissionRequest(
        @NotNull Patient patient,
        @NotBlank String answersJson
) {
    public FeegowPatientRequest toCreatePatientRequest() {
        return FeegowPatientRequest.builder()
                .nome(patient.name())
                .cpf(patient.cpf())
                .sexo(patient.sexo())
                .nascimento(patient.nascimento())
                .build();
    }

    public record Patient(
            @NotBlank String name,
            @NotBlank String cpf,
            @NotBlank String sexo,
            @NotBlank String nascimento
    ) {}
}
