package br.dev.ctrls.api.web.rest;

import br.dev.ctrls.api.application.service.submission.SubmissionService;
import br.dev.ctrls.api.domain.form.FormTemplate;
import br.dev.ctrls.api.domain.form.repository.FormTemplateRepository;
import br.dev.ctrls.api.web.dto.FormPublicViewDTO;
import br.dev.ctrls.api.web.dto.SubmissionRequest;
import br.dev.ctrls.api.web.dto.SubmissionResponse;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos para renderização e submissão de formulários.
 */
@RestController
@RequestMapping("/api/public/forms")
public class PublicFormController {

    private final FormTemplateRepository formTemplateRepository;
    private final SubmissionService submissionService;

    public PublicFormController(FormTemplateRepository formTemplateRepository,
                                SubmissionService submissionService) {
        this.formTemplateRepository = formTemplateRepository;
        this.submissionService = submissionService;
    }

    @GetMapping("/{uuid}")
    @Cacheable(value = "forms", key = "#uuid")
    public ResponseEntity<FormPublicViewDTO> getForm(@PathVariable UUID uuid) {
        FormTemplate template = formTemplateRepository.findByPublicUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Formulário não encontrado"));
        return ResponseEntity.ok(FormPublicViewDTO.from(template));
    }

    @PostMapping("/{uuid}/submit")
    public ResponseEntity<SubmissionResponse> submit(@PathVariable UUID uuid,
                                                     @RequestBody SubmissionRequest request) {
        SubmissionResponse response = submissionService.processSubmission(uuid, request);
        return ResponseEntity.ok(response);
    }
}
