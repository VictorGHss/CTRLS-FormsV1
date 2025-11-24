package br.dev.ctrls.api.web.dto;

import br.dev.ctrls.api.client.feegow.dto.FeegowPatientRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Requisição de submissão de formulário de anamnese")
public record SubmissionRequest(
        @NotNull(message = "Dados do paciente são obrigatórios")
        @Valid
        @Schema(description = "Informações do paciente")
        Patient patient,

        @NotBlank(message = "Respostas do formulário são obrigatórias")
        @Schema(description = "JSON contendo as respostas do formulário",
                example = "{\"sintomas\": \"dor de cabeça\", \"duracao\": \"2 dias\"}")
        String answersJson
) {
    public FeegowPatientRequest toCreatePatientRequest() {
        return FeegowPatientRequest.builder()
                .nome(patient.name())
                .cpf(patient.cpf())
                .sexo(patient.sexo())
                .nascimento(patient.nascimento())
                .build();
    }

    @Schema(description = "Dados pessoais do paciente")
    public record Patient(
            @NotBlank(message = "Nome do paciente é obrigatório")
            @Schema(description = "Nome completo do paciente", example = "João Silva")
            String name,

            @NotBlank(message = "CPF é obrigatório")
            @Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 dígitos numéricos")
            @Schema(description = "CPF do paciente (apenas números)", example = "12345678901")
            String cpf,

            @NotBlank(message = "Sexo é obrigatório")
            @Pattern(regexp = "^(M|F|Outro)$", message = "Sexo deve ser 'M', 'F' ou 'Outro'")
            @Schema(description = "Sexo do paciente", example = "M", allowableValues = {"M", "F", "Outro"})
            String sexo,

            @NotBlank(message = "Data de nascimento é obrigatória")
            @Pattern(regexp = "^\\d{2}/\\d{2}/\\d{4}$",
                     message = "Data de nascimento deve estar no formato dd/MM/yyyy")
            @Schema(description = "Data de nascimento no formato dd/MM/yyyy", example = "15/03/1990")
            String nascimento
    ) {}
}
