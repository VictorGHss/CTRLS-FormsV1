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

/**
 * Controller para APIs públicas de formulários (sem autenticação).
 *
 * RESPONSABILIDADES:
 * 1. Fornecer formulário público para preenchimento pelo paciente
 * 2. Receber submissão do formulário preenchido
 * 3. Retornar 202 Accepted para processamento assíncrono
 */
@Tag(
    name = "Formulários Públicos",
    description = "APIs públicas para acesso e submissão de formulários de anamnese. " +
                  "Não requer autenticação."
)
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
                      "Usado pelo frontend para renderizar o formulário de anamnese. " +
                      "**Endpoint público - não requer autenticação.**"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Formulário encontrado e retornado com sucesso",
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
        FormTemplate template = formTemplateRepository.findByPublicUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Formulário não encontrado"));

        return ResponseEntity.ok(FormPublicViewDTO.fromEntity(template));
    }

    @PostMapping("/{uuid}/submit")
    @Operation(
        summary = "Enviar resposta do formulário (processamento assíncrono)",
        description = "Recebe a submissão do formulário preenchido pelo paciente. " +
                      "**IMPORTANTE:** O processamento é assíncrono - integração com Feegow e geração de PDF " +
                      "ocorrem em background. Retorna imediatamente com status PENDING e um ID para acompanhamento. " +
                      "\n\n**Endpoint público - não requer autenticação.**"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "202",
            description = "Submissão aceita para processamento assíncrono. " +
                         "Status inicial: PENDING. Processamento em background incluirá: " +
                         "validação de paciente no Feegow, geração de PDF e upload de arquivo.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SubmissionResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos - verifique validações de CPF (11 dígitos), " +
                         "data (dd/MM/yyyy), sexo (M/F/Outro), etc.",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Formulário não encontrado com o UUID fornecido",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Formulário inativo - não aceita submissões no momento",
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

        // ✅ Retorna 202 Accepted (processamento assíncrono)
        return ResponseEntity.accepted().body(response);
    }
}