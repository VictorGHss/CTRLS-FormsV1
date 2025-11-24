package br.dev.ctrls.api.web.dto;

import br.dev.ctrls.api.domain.submission.SubmissionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Resposta da submissão de formulário (processamento assíncrono)")
public record SubmissionResponse(
        @Schema(description = "ID único da submissão gerado pelo sistema",
                example = "123e4567-e89b-12d3-a456-426614174000")
        UUID submissionId,

        @Schema(description = "Status atual do processamento da submissão",
                example = "PENDING",
                allowableValues = {"PENDING", "PROCESSED", "ERROR"})
        SubmissionStatus status
) {
}

