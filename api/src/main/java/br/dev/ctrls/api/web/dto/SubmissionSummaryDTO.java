package br.dev.ctrls.api.web.dto;

import br.dev.ctrls.api.domain.submission.Submission;
import br.dev.ctrls.api.domain.submission.SubmissionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO enxuto para listagens do dashboard médico.
 */
@Schema(description = "Resumo de uma submissão para exibição em listas")
public record SubmissionSummaryDTO(
        @Schema(description = "ID único da submissão", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Nome completo do paciente", example = "João Silva")
        String patientName,

        @Schema(description = "CPF do paciente (11 dígitos)", example = "12345678901")
        String patientCpf,

        @Schema(description = "Status do processamento", example = "PROCESSED")
        SubmissionStatus status,

        @Schema(description = "Título do formulário respondido", example = "Anamnese Cardiológica")
        String formTitle,

        @Schema(description = "Data e hora da submissão", example = "2024-11-24T10:30:00Z")
        Instant createdAt
) {
    public static SubmissionSummaryDTO fromEntity(Submission submission) {
        return new SubmissionSummaryDTO(
                submission.getId(),
                submission.getPatientName(),
                submission.getPatientCpf(),
                submission.getStatus(),
                submission.getTemplate().getTitle(),
                submission.getCreatedAt()
        );
    }
}

