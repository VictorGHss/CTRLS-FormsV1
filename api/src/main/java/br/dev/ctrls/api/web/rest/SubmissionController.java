package br.dev.ctrls.api.web.rest;

import br.dev.ctrls.api.domain.submission.Submission;
import br.dev.ctrls.api.domain.submission.SubmissionStatus;
import br.dev.ctrls.api.domain.submission.repository.SubmissionRepository;
import br.dev.ctrls.api.web.dto.SubmissionSummaryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoints privados para visualização de submissões no dashboard.
 */
@Tag(name = "Submissões", description = "APIs para gerenciar submissões de formulários (requer autenticação)")
@RestController
@RequestMapping("/api/submissions")
@SecurityRequirement(name = "bearerAuth")
public class SubmissionController {

    private final SubmissionRepository submissionRepository;

    public SubmissionController(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    @GetMapping
    @Operation(
        summary = "Listar submissões com paginação e filtros",
        description = "Lista todas as submissões da clínica autenticada com suporte a paginação, " +
                      "ordenação e filtros por status e nome de paciente.",
        parameters = {
            @Parameter(
                name = "X-Clinic-ID",
                description = "UUID da clínica (header obrigatório para multi-tenancy)",
                required = true,
                example = "123e4567-e89b-12d3-a456-426614174000"
            ),
            @Parameter(
                name = "status",
                description = "Filtrar por status da submissão (opcional)",
                schema = @Schema(allowableValues = {"PENDING", "PROCESSED", "ERROR"}),
                example = "PROCESSED"
            ),
            @Parameter(
                name = "patientName",
                description = "Filtrar por nome do paciente (busca parcial, opcional)",
                example = "João"
            ),
            @Parameter(
                name = "page",
                description = "Número da página (0-indexed)",
                example = "0"
            ),
            @Parameter(
                name = "size",
                description = "Tamanho da página (número de itens por página)",
                example = "20"
            ),
            @Parameter(
                name = "sort",
                description = "Campo e direção de ordenação (formato: campo,direção)",
                example = "createdAt,desc"
            )
        }
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de submissões retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parâmetros inválidos (UUID, status, etc)",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado - token JWT ausente ou inválido",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Acesso negado - usuário não pertence à clínica especificada",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
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