package br.dev.ctrls.api.application.service.submission;

import br.dev.ctrls.api.BaseIntegrationTest;
import br.dev.ctrls.api.client.feegow.FeegowClient;
import br.dev.ctrls.api.client.feegow.dto.FeegowPatientRequest;
import br.dev.ctrls.api.client.feegow.dto.FeegowPatientResponse;
import br.dev.ctrls.api.client.feegow.dto.UploadFileRequest;
import br.dev.ctrls.api.domain.clinic.Clinic;
import br.dev.ctrls.api.domain.clinic.repository.ClinicRepository;
import br.dev.ctrls.api.domain.form.FormTemplate;
import br.dev.ctrls.api.domain.form.repository.FormTemplateRepository;
import br.dev.ctrls.api.domain.submission.repository.SubmissionRepository;
import br.dev.ctrls.api.web.dto.SubmissionRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica o fluxo principal de submissão com Feegow mockado.
 */
public class SubmissionServiceTest extends BaseIntegrationTest {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private ClinicRepository clinicRepository;

    @Autowired
    private FormTemplateRepository formTemplateRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @MockBean
    private FeegowClient feegowClient;

    @MockBean
    private br.dev.ctrls.api.application.service.document.PdfService pdfService;

    @Test
    @Transactional
    void processSubmissionPersistsAndUploadsPdf() throws Exception {
        Clinic clinic = Clinic.builder()
                .name("Clínica Teste")
                .cnpj("12345678901234")
                .address("Rua X, 100")
                .feegowApiToken("TOKEN")
                .build();
        clinicRepository.save(clinic);

        FormTemplate template = FormTemplate.builder()
                .clinic(clinic)
                .title("Ficha")
                .schemaJson("{}")
                .active(true)
                .build();
        formTemplateRepository.save(template);

        SubmissionRequest request = new SubmissionRequest(
                new SubmissionRequest.Patient("Fulano", "11122233344", "M", "1990-01-01"),
                "{\"q1\":\"ok\"}"
        );

        when(pdfService.generateAnamnesisPdf(any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(feegowClient.listPatients(eq("TOKEN"), eq("11122233344")))
                .thenReturn(new FeegowPatientResponse(List.of()));
        when(feegowClient.createPatient(eq("TOKEN"), any(FeegowPatientRequest.class))).thenReturn(999L);
        when(feegowClient.uploadPatientFile(eq("TOKEN"), any(UploadFileRequest.class))).thenReturn(321L);

        submissionService.processSubmission(template.getPublicUuid(), request);

        assertThat(submissionRepository.count()).isEqualTo(1);
        var submission = submissionRepository.findAll().get(0);
        assertThat(submission.getStatus()).isEqualTo(br.dev.ctrls.api.domain.submission.SubmissionStatus.PROCESSED);
        assertThat(submission.getFeegowPatientId()).isEqualTo("999");

        ArgumentCaptor<UploadFileRequest> uploadCaptor = ArgumentCaptor.forClass(UploadFileRequest.class);
        verify(feegowClient).uploadPatientFile(eq("TOKEN"), uploadCaptor.capture());
        assertThat(uploadCaptor.getValue().patient_id()).isEqualTo(999L);
    }
}
