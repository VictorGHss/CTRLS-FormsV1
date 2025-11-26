package br.dev.ctrls.api.application.service.submission;

import br.dev.ctrls.api.application.event.SubmissionCreatedEvent;
import br.dev.ctrls.api.application.service.document.PdfGenerationException;
import br.dev.ctrls.api.application.service.document.PdfService;
import br.dev.ctrls.api.client.feegow.dto.UploadFileRequest;
import br.dev.ctrls.api.domain.submission.Submission;
import br.dev.ctrls.api.domain.submission.SubmissionStatus;
import br.dev.ctrls.api.domain.submission.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Handler assíncrono para processar submissões após serem criadas.
 * Executa a integração com Feegow e geração de PDF em background.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionEventHandler {

    private final SubmissionRepository submissionRepository;
    private final FeegowIntegrationService feegowService;
    private final PdfService pdfService;

    /**
     * Processa submissão de forma assíncrona após evento de criação.
     * Usa thread pool dedicado configurado em AsyncConfig.
     */
    @Async("submissionTaskExecutor")
    @EventListener
    public void handleSubmissionCreated(SubmissionCreatedEvent event) {
        log.info("Processando submissão assíncrona: {}", event.getSubmissionId());

        try {
            processSubmissionIntegration(event.getSubmissionId());
        } catch (Exception ex) {
            log.error("Erro fatal no processamento da submissão: {}", event.getSubmissionId(), ex);
            markAsError(event.getSubmissionId(), ex.getMessage());
        }
    }

    /**
     * Processa integração com Feegow em uma nova transação.
     *
     * PROPAGATION.REQUIRES_NEW garante que mesmo que a transação pai falhe,
     * esta transação pode ser commitada independentemente.
     *
     * IMPORTANTE: Usa findByIdWithGraph() para carregar relacionamentos eagerly
     * e evitar LazyInitializationException ao acessar submission.getTemplate().getClinic()
     * em contexto assíncrono.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void processSubmissionIntegration(UUID submissionId) {
        // ✅ FIX: Usa findByIdWithGraph para carregar template + clinic eagerly
        Submission submission = submissionRepository.findByIdWithGraph(submissionId)
                .orElseThrow(() -> new IllegalStateException("Submissão não encontrada: " + submissionId));

        try {
            // ✅ Agora submission.getTemplate().getClinic() está carregado (sem LazyInitializationException)
            String token = submission.getTemplate().getClinic().getFeegowApiToken();

            // 1. Resolver/Criar Paciente no Feegow
            log.debug("Resolvendo paciente no Feegow para submissão: {}", submissionId);
            Long patientId = feegowService.resolvePatient(
                token,
                submission.getPatientCpf(),
                submission.getPatientName()
            );

            submission.setFeegowPatientId(String.valueOf(patientId));
            submissionRepository.save(submission);

            // 2. Gerar PDF
            log.debug("Gerando PDF para submissão: {}", submissionId);
            byte[] pdfBytes = pdfService.generateAnamnesisPdf(submission, submission.getTemplate());

            // 3. Upload para Feegow
            log.debug("Fazendo upload de PDF para Feegow");
            String base64 = Base64.getEncoder().encodeToString(pdfBytes);
            String filename = "anamnese-" + Instant.now().toEpochMilli() + ".pdf";
            UploadFileRequest uploadRequest = new UploadFileRequest(patientId, base64, filename);

            feegowService.uploadFile(token, uploadRequest);

            // 4. Marcar como processado
            submission.setStatus(SubmissionStatus.PROCESSED);
            submissionRepository.save(submission);

            log.info("Submissão processada com sucesso: {}", submissionId);

        } catch (FeegowIntegrationException ex) {
            log.error("Erro de integração com Feegow na submissão {}: {}", submissionId, ex.getMessage());
            submission.setStatus(SubmissionStatus.ERROR);
            submissionRepository.save(submission);

        } catch (PdfGenerationException ex) {
            log.error("Erro ao gerar PDF na submissão {}: {}", submissionId, ex.getMessage());
            submission.setStatus(SubmissionStatus.ERROR);
            submissionRepository.save(submission);

        } catch (Exception ex) {
            log.error("Erro inesperado ao processar submissão {}", submissionId, ex);
            submission.setStatus(SubmissionStatus.ERROR);
            submissionRepository.save(submission);
            throw ex; // Re-throw para ser capturado pelo handler principal
        }
    }

    /**
     * Marca submissão como erro em uma transação separada.
     * Usado quando há erro fatal antes mesmo de iniciar o processamento.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void markAsError(UUID submissionId, String errorMessage) {
        // Usa findByIdWithGraph para consistência (mesmo que não precise acessar relacionamentos lazy aqui)
        submissionRepository.findByIdWithGraph(submissionId).ifPresent(submission -> {
            submission.setStatus(SubmissionStatus.ERROR);
            submissionRepository.save(submission);
            log.error("Submissão marcada como ERROR: {} - Motivo: {}", submissionId, errorMessage);
        });
    }
}

