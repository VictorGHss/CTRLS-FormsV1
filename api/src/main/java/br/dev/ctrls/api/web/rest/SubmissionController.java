package br.dev.ctrls.api.web.rest;

import br.dev.ctrls.api.domain.submission.Submission;
import br.dev.ctrls.api.domain.submission.SubmissionStatus;
import br.dev.ctrls.api.domain.submission.repository.SubmissionRepository;
import br.dev.ctrls.api.web.dto.SubmissionSummaryDTO;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints privados para visualização de submissões no dashboard.
 */
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionRepository submissionRepository;

    public SubmissionController(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR','SECRETARY','CLINIC_ADMIN')")
    public ResponseEntity<Page<SubmissionSummaryDTO>> findAll(
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) String patientName,
            @RequestParam UUID clinicId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        // Nota: validação de tenant foi realizada no TenantContextFilter.
        Page<Submission> page = submissionRepository.searchWithFilters(clinicId, status, patientName, pageable);
        Page<SubmissionSummaryDTO> dtoPage = page.map(SubmissionSummaryDTO::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }
}
