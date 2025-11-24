package br.dev.ctrls.api.web.rest;

import br.dev.ctrls.api.application.service.submission.SubmissionService;
import br.dev.ctrls.api.domain.form.FormTemplate;
import br.dev.ctrls.api.domain.form.repository.FormTemplateRepository;
import br.dev.ctrls.api.web.dto.FormPublicViewDTO;
import br.dev.ctrls.api.web.dto.SubmissionRequest;
import br.dev.ctrls.api.web.dto.SubmissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Formulários Públicos", description = "APIs públicas para acesso e submissão de formulários de anamnese")
@RestController
@RequestMapping("/api/public/forms")
@RequiredArgsConstructor
public class PublicFormController {

    private final FormTemplateRepository formTemplateRepository;
    private final SubmissionService submissionService;

    @GetMapping("/{uuid}")
    @Operation(
        summary = "Obter template de formulário público",
        description = "Retorna o template do formulário com informações de branding da clínica e médico. " +
                      "Este endpoint é público e não requer autenticação."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Formulário encontrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FormPublicViewDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Formulário não encontrado - UUID inválido ou formulário não existe",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "UUID em formato inválido",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public ResponseEntity<FormPublicViewDTO> getForm(@PathVariable UUID uuid) {
        // Busca no banco
        FormTemplate template = formTemplateRepository.findByPublicUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Formulário não encontrado"));

        // Converte para DTO preenchendo o Branding
        return ResponseEntity.ok(FormPublicViewDTO.fromEntity(template));
    }

    @PostMapping("/{uuid}/submit")
    @Operation(
        summary = "Enviar resposta do formulário (processamento assíncrono)",
        description = "Recebe a submissão do formulário preenchido pelo paciente. " +
                      "O processamento (integração com Feegow e geração de PDF) é feito de forma assíncrona. " +
                      "Retorna imediatamente com status PENDING e um ID para acompanhamento."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "202",
            description = "Submissão aceita para processamento assíncrono",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SubmissionResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos - verifique validações de CPF, data, etc",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Formulário não encontrado",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Formulário inativo - não aceita submissões",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public ResponseEntity<SubmissionResponse> submit(
            @PathVariable UUID uuid,
            @Valid @RequestBody SubmissionRequest request) {

        SubmissionResponse response = submissionService.submitForm(uuid, request);

        // Retorna 202 Accepted indicando que o processamento será feito de forma assíncrona
        return ResponseEntity.accepted().body(response);
    }
}