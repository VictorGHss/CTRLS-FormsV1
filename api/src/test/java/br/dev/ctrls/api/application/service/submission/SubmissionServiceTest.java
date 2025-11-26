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
import br.dev.ctrls.api.domain.submission.Submission;
import br.dev.ctrls.api.domain.submission.SubmissionStatus;
import br.dev.ctrls.api.domain.submission.repository.SubmissionRepository;
import br.dev.ctrls.api.web.dto.SubmissionRequest;
import br.dev.ctrls.api.web.dto.SubmissionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes de integração para SubmissionService com arquitetura Event-Driven.
 *
 * ARQUITETURA TESTADA:
 * 1. submitForm() → Salva PENDING → Publica evento → Retorna 202
 * 2. SubmissionEventHandler (@Async) → Processa em background → Atualiza PROCESSED
 *
 * ESTRATÉGIA DE TESTE:
 * - Usa @SpringBootTest para ativar TaskExecutor e EventListener
 * - Mocka FeegowClient e PdfService para evitar chamadas HTTP reais
 * - Usa Awaitility para aguardar processamento assíncrono
 * - Valida status PENDING → PROCESSED com timeout de 10 segundos
 * - @DirtiesContext garante contexto limpo entre testes
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SubmissionServiceTest extends BaseIntegrationTest {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private ClinicRepository clinicRepository;

    @Autowired
    private FormTemplateRepository formTemplateRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @MockitoBean
    private FeegowClient feegowClient;

    @MockitoBean
    private br.dev.ctrls.api.application.service.document.PdfService pdfService;

    private Clinic clinic;
    private FormTemplate template;

    @BeforeEach
    void setUp() {
        // Limpar dados de testes anteriores na ordem correta (FK constraints)
        // Submissions referencia FormTemplates, FormTemplates referencia Clinics
        submissionRepository.deleteAll();
        formTemplateRepository.deleteAll();
        // Não deletar clinics - o BaseIntegrationTest pode ter dados seed

        // Criar clínica de teste
        clinic = Clinic.builder()
                .name("Clínica Teste Integração")
                .cnpj("12345678901234")
                .address("Rua Teste, 100")
                .feegowApiToken("MOCK_TOKEN_123")
                .build();
        clinic = clinicRepository.save(clinic);

        // Criar template de formulário ativo
        template = FormTemplate.builder()
                .clinic(clinic)
                .title("Anamnese Teste")
                .description("Formulário para testes de integração")
                .schemaJson("{\"fields\": [\"sintomas\", \"alergias\"]}")
                .active(true)
                .build();
        template = formTemplateRepository.save(template);
    }

    /**
     * Testa o fluxo completo de submissão assíncrona com sucesso.
     *
     * CENÁRIO:
     * 1. Cliente envia submissão via submitForm()
     * 2. Sistema retorna 202 Accepted com status PENDING
     * 3. Worker assíncrono processa: busca/cria paciente → gera PDF → upload
     * 4. Status atualizado para PROCESSED
     *
     * VALIDAÇÕES:
     * - Resposta inicial tem status PENDING
     * - Após processamento assíncrono, status é PROCESSED
     * - FeegowClient foi chamado corretamente (listPatients, createPatient, uploadPatientFile)
     * - PdfService foi chamado para gerar PDF
     * - Submission persistida com feegowPatientId correto
     */
    @Test
    void shouldProcessSubmissionAsynchronouslyWithSuccess() throws Exception {
        // ARRANGE: Configurar mocks do Feegow e PdfService

        // Mock: Busca de paciente retorna vazio (paciente não existe)
        when(feegowClient.listPatients(eq("MOCK_TOKEN_123"), eq("11122233344")))
                .thenReturn(new FeegowPatientResponse(List.of()));

        // Mock: Criação de paciente retorna ID 999
        when(feegowClient.createPatient(eq("MOCK_TOKEN_123"), any(FeegowPatientRequest.class)))
                .thenReturn(999L);

        // Mock: Upload de arquivo retorna ID 321
        when(feegowClient.uploadPatientFile(eq("MOCK_TOKEN_123"), any(UploadFileRequest.class)))
                .thenReturn(321L);

        // Mock: Geração de PDF retorna byte array
        when(pdfService.generateAnamnesisPdf(any(), any()))
                .thenReturn(new byte[]{1, 2, 3, 4, 5});

        // Criar request de submissão
        SubmissionRequest request = new SubmissionRequest(
                new SubmissionRequest.Patient(
                        "Fulano de Tal",
                        "11122233344",
                        "M",
                        "01/01/1990"
                ),
                "{\"sintomas\": \"dor de cabeça\", \"alergias\": \"nenhuma\"}"
        );

        // ACT: Enviar submissão (método assíncrono)
        SubmissionResponse response = submissionService.submitForm(template.getPublicUuid(), request);

        // ASSERT 1: Resposta imediata deve ter status PENDING
        assertThat(response).isNotNull();
        assertThat(response.submissionId()).isNotNull();
        assertThat(response.status()).isEqualTo(SubmissionStatus.PENDING);

        // ASSERT 2: Submissão persistida no banco com status PENDING
        UUID submissionId = response.submissionId();
        Submission submissionInitial = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new AssertionError("Submissão não foi salva no banco"));
        assertThat(submissionInitial.getStatus()).isEqualTo(SubmissionStatus.PENDING);
        assertThat(submissionInitial.getPatientName()).isEqualTo("Fulano de Tal");
        assertThat(submissionInitial.getPatientCpf()).isEqualTo("11122233344");

        // AWAIT: Aguardar processamento assíncrono (Worker thread)
        // Timeout de 10 segundos para garantir que o evento foi processado
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Submission updated = submissionRepository.findById(submissionId)
                            .orElseThrow(() -> new AssertionError("Submissão desapareceu do banco"));

                    assertThat(updated.getStatus())
                            .as("Status deve ser PROCESSED após processamento assíncrono")
                            .isEqualTo(SubmissionStatus.PROCESSED);
                });

        // ASSERT 3: Validar dados finais da submissão processada
        Submission submissionFinal = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new AssertionError("Submissão não encontrada"));

        assertThat(submissionFinal.getStatus()).isEqualTo(SubmissionStatus.PROCESSED);
        assertThat(submissionFinal.getFeegowPatientId()).isEqualTo("999");

        // ASSERT 4: Verificar interações com mocks (foram chamados?)
        verify(feegowClient).listPatients(eq("MOCK_TOKEN_123"), eq("11122233344"));
        verify(feegowClient).createPatient(eq("MOCK_TOKEN_123"), any(FeegowPatientRequest.class));
        verify(pdfService).generateAnamnesisPdf(any(), any());

        ArgumentCaptor<UploadFileRequest> uploadCaptor = ArgumentCaptor.forClass(UploadFileRequest.class);
        verify(feegowClient).uploadPatientFile(eq("MOCK_TOKEN_123"), uploadCaptor.capture());

        UploadFileRequest uploadRequest = uploadCaptor.getValue();
        assertThat(uploadRequest.patient_id()).isEqualTo(999L);
        assertThat(uploadRequest.base64_file()).isNotBlank();
        assertThat(uploadRequest.filename()).startsWith("anamnese-");
    }

    /**
     * Testa cenário onde paciente JÁ EXISTE no Feegow.
     *
     * CENÁRIO:
     * 1. listPatients retorna paciente existente (ID 888)
     * 2. createPatient NÃO deve ser chamado
     * 3. Upload feito para paciente existente
     * 4. Status PROCESSED
     */
    @Test
    void shouldUseExistingPatientWhenFoundInFeegow() throws Exception {
        // ARRANGE: Paciente já existe no Feegow
        when(feegowClient.listPatients(eq("MOCK_TOKEN_123"), eq("99988877766")))
                .thenReturn(new FeegowPatientResponse(List.of(
                        new FeegowPatientResponse.PatientSummary(888L, "João Existente", "99988877766")
                )));

        when(feegowClient.uploadPatientFile(eq("MOCK_TOKEN_123"), any(UploadFileRequest.class)))
                .thenReturn(555L);

        when(pdfService.generateAnamnesisPdf(any(), any()))
                .thenReturn(new byte[]{9, 8, 7});

        SubmissionRequest request = new SubmissionRequest(
                new SubmissionRequest.Patient(
                        "João Existente",
                        "99988877766",
                        "M",
                        "15/05/1985"
                ),
                "{\"sintomas\": \"febre\"}"
        );

        // ACT
        SubmissionResponse response = submissionService.submitForm(template.getPublicUuid(), request);

        // AWAIT: Aguardar processamento
        UUID submissionId = response.submissionId();
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Submission updated = submissionRepository.findById(submissionId)
                            .orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(SubmissionStatus.PROCESSED);
                });

        // ASSERT: Paciente existente foi usado (ID 888)
        Submission submissionFinal = submissionRepository.findById(submissionId).orElseThrow();
        assertThat(submissionFinal.getFeegowPatientId()).isEqualTo("888");

        // Verificar que createPatient NÃO foi chamado
        verify(feegowClient).listPatients(eq("MOCK_TOKEN_123"), eq("99988877766"));
        verify(feegowClient).uploadPatientFile(eq("MOCK_TOKEN_123"), any(UploadFileRequest.class));

        // ✅ createPatient nunca deve ser chamado quando paciente existe
        verify(feegowClient, org.mockito.Mockito.never())
                .createPatient(any(), any());
    }

    /**
     * @deprecated Método mantido apenas para compatibilidade com código legado.
     * Use submitForm() para processamento assíncrono.
     */
    @Test
    @Deprecated
    void processSubmissionPersistsAndUploadsPdf_Legacy() throws Exception {
        // Este teste valida o método deprecated processSubmission()
        // que internamente chama submitForm()

        when(pdfService.generateAnamnesisPdf(any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(feegowClient.listPatients(eq("MOCK_TOKEN_123"), eq("11122233344")))
                .thenReturn(new FeegowPatientResponse(List.of()));
        when(feegowClient.createPatient(eq("MOCK_TOKEN_123"), any(FeegowPatientRequest.class)))
                .thenReturn(999L);
        when(feegowClient.uploadPatientFile(eq("MOCK_TOKEN_123"), any(UploadFileRequest.class)))
                .thenReturn(321L);

        SubmissionRequest request = new SubmissionRequest(
                new SubmissionRequest.Patient("Fulano", "11122233344", "M", "01/01/1990"),
                "{\"q1\":\"ok\"}"
        );

        @SuppressWarnings("deprecation")
        SubmissionResponse response = submissionService.processSubmission(template.getPublicUuid(), request);

        // Aguardar processamento assíncrono
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var submission = submissionRepository.findById(response.submissionId()).orElseThrow();
                    assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.PROCESSED);
                });

        assertThat(submissionRepository.count()).isEqualTo(1);
        var submission = submissionRepository.findAll().get(0);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.PROCESSED);
        assertThat(submission.getFeegowPatientId()).isEqualTo("999");
    }
}
