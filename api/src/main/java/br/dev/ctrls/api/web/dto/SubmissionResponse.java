package br.dev.ctrls.api.web.dto;

import br.dev.ctrls.api.domain.submission.SubmissionStatus;
import java.util.UUID;

public record SubmissionResponse(UUID submissionId, SubmissionStatus status) {
}

