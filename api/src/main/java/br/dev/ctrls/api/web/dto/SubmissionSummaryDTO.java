package br.dev.ctrls.api.web.dto;

import br.dev.ctrls.api.domain.submission.Submission;
import br.dev.ctrls.api.domain.submission.SubmissionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO enxuto para listagens do dashboard médico.
 */
public record SubmissionSummaryDTO(
        UUID id,
        String patientName,
        String patientCpf,
        SubmissionStatus status,
        String formTitle,
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

