package br.dev.ctrls.api.web.rest;

import br.dev.ctrls.api.application.service.submission.SubmissionService;
import br.dev.ctrls.api.domain.form.FormTemplate;
import br.dev.ctrls.api.domain.form.repository.FormTemplateRepository;
import br.dev.ctrls.api.web.dto.FormPublicViewDTO;
import br.dev.ctrls.api.web.dto.SubmissionRequest;
import br.dev.ctrls.api.web.dto.SubmissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/forms")
@RequiredArgsConstructor
public class PublicFormController {

    private final FormTemplateRepository formTemplateRepository;
    private final SubmissionService submissionService;

    @GetMapping("/{uuid}")
    @Operation(summary = "Obter template público de formulário")
    public ResponseEntity<FormPublicViewDTO> getForm(@PathVariable UUID uuid) {
        // Busca no banco
        FormTemplate template = formTemplateRepository.findByPublicUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Formulário não encontrado"));

        // Converte para DTO preenchendo o Branding
        return ResponseEntity.ok(FormPublicViewDTO.fromEntity(template));
    }

    @PostMapping("/{uuid}/submit")
    @Operation(summary = "Enviar resposta do formulário")
    public ResponseEntity<SubmissionResponse> submit(
            @PathVariable UUID uuid,
            @RequestBody SubmissionRequest request) {

        return ResponseEntity.ok(submissionService.processSubmission(uuid, request));
    }
}