package br.dev.ctrls.api.application.service.submission;

import br.dev.ctrls.api.application.service.document.PdfService;
import br.dev.ctrls.api.client.feegow.FeegowClient;
import br.dev.ctrls.api.client.feegow.dto.FeegowPatientRequest;
import br.dev.ctrls.api.client.feegow.dto.FeegowPatientResponse;
import br.dev.ctrls.api.client.feegow.dto.UploadFileRequest;
import br.dev.ctrls.api.domain.form.FormTemplate;
import br.dev.ctrls.api.domain.form.repository.FormTemplateRepository;
import br.dev.ctrls.api.domain.submission.Submission;
import br.dev.ctrls.api.domain.submission.SubmissionStatus;
import br.dev.ctrls.api.domain.submission.repository.SubmissionRepository;
import br.dev.ctrls.api.web.dto.SubmissionRequest;
import br.dev.ctrls.api.web.dto.SubmissionResponse;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final FormTemplateRepository templateRepository;
    private final SubmissionRepository submissionRepository;
    private final FeegowClient feegowClient;
    private final PdfService pdfService;

    @Transactional
    public SubmissionResponse processSubmission(UUID formUuid, SubmissionRequest request) {
        FormTemplate template = templateRepository.findByPublicUuid(formUuid)
                .orElseThrow(() -> new EntityNotFoundException("Formulário não encontrado"));

        if (!template.isActive()) {
            throw new IllegalStateException("Formulário inativo");
        }

        Submission submission = Submission.builder()
                .template(template)
                .patientCpf(request.patient().cpf())
                .patientName(request.patient().name())
                .answersJson(request.answersJson())
                .status(SubmissionStatus.PENDING)
                .build();

        String token = template.getClinic().getFeegowApiToken();

        try {
            Long patientId = resolveFeegowPatient(token, request);

            byte[] pdfBytes = pdfService.generateAnamnesisPdf(submission, template);
            String base64 = Base64.getEncoder().encodeToString(pdfBytes);
            String filename = "anamnese-" + Instant.now().toEpochMilli() + ".pdf";

            UploadFileRequest uploadRequest = new UploadFileRequest(patientId, base64, filename);
            feegowClient.uploadPatientFile(token, uploadRequest);

            submission.setStatus(SubmissionStatus.PROCESSED);
            submission.setFeegowPatientId(String.valueOf(patientId));
        } catch (Exception ex) {
            log.error("Erro na integração Feegow", ex);
            submission.setStatus(SubmissionStatus.ERROR);
        }

        submissionRepository.save(submission);
        return new SubmissionResponse(submission.getId(), submission.getStatus());
    }

    private Long resolveFeegowPatient(String token, SubmissionRequest request) {
        FeegowPatientResponse response = feegowClient.listPatients(token, request.patient().cpf());
        Long existingId = response.firstId();

        if (existingId != null) {
            return existingId;
        }

        FeegowPatientRequest createRequest = request.toCreatePatientRequest();
        return feegowClient.createPatient(token, createRequest);
    }
}