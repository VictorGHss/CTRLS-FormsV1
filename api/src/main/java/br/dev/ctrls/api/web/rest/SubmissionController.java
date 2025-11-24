package br.dev.ctrls.api.web.rest;

import br.dev.ctrls.api.domain.submission.Submission;
import br.dev.ctrls.api.domain.submission.SubmissionStatus;
import br.dev.ctrls.api.domain.submission.repository.SubmissionRepository;
import br.dev.ctrls.api.web.dto.SubmissionSummaryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoints privados para visualização de submissões no dashboard.
 */
@RestController
@RequestMapping("/api/submissions")
@SecurityRequirement(name = "bearerAuth")
public class SubmissionController {

    private final SubmissionRepository submissionRepository;

    public SubmissionController(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    @GetMapping
    @Operation(summary = "Listar submissões do dashboard")
    // @PreAuthorize("hasAnyRole('DOCTOR','SECRETARY','CLINIC_ADMIN')") // Descomente se tiver @EnableMethodSecurity
    public ResponseEntity<Page<SubmissionSummaryDTO>> findAll(
            @RequestHeader("X-Clinic-ID") UUID clinicId,
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) String patientName,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        // O TenantContextFilter já validou se este usuário pode acessar este clinicId via Header.

        Page<Submission> page = submissionRepository.searchWithFilters(clinicId, status, patientName, pageable);

        Page<SubmissionSummaryDTO> dtoPage = page.map(SubmissionSummaryDTO::fromEntity);

        return ResponseEntity.ok(dtoPage);
    }
}