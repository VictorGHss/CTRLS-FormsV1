package br.dev.ctrls.api.application.service.submission;

import br.dev.ctrls.api.application.event.SubmissionCreatedEvent;
import br.dev.ctrls.api.domain.form.FormTemplate;
import br.dev.ctrls.api.domain.form.repository.FormTemplateRepository;
import br.dev.ctrls.api.domain.submission.Submission;
import br.dev.ctrls.api.domain.submission.SubmissionStatus;
import br.dev.ctrls.api.domain.submission.repository.SubmissionRepository;
import br.dev.ctrls.api.web.dto.SubmissionRequest;
import br.dev.ctrls.api.web.dto.SubmissionResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço para gerenciar submissões de formulários.
 *
 * ARQUITETURA:
 * - Método síncrono (submitForm): valida e salva submissão com status PENDING
 * - Retorna imediatamente ao cliente (resposta rápida)
 * - Publica evento para processamento assíncrono
 * - SubmissionEventHandler processa integração Feegow em background
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final FormTemplateRepository templateRepository;
    private final SubmissionRepository submissionRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Recebe submissão do formulário e agenda processamento assíncrono.
     *
     * IMPORTANTE:
     * - Esta transação é RÁPIDA (apenas validação + insert)
     * - NÃO faz chamadas HTTP nem geração de PDF
     * - Libera conexão do pool imediatamente
     *
     * @param formUuid UUID público do formulário
     * @param request Dados da submissão
     * @return Resposta com ID e status PENDING
     */
    @Transactional
    public SubmissionResponse submitForm(UUID formUuid, SubmissionRequest request) {
        log.info("Recebendo submissão do formulário: {}", formUuid);

        // 1. Validar formulário (query rápida com @EntityGraph)
        FormTemplate template = templateRepository.findByPublicUuid(formUuid)
                .orElseThrow(() -> new EntityNotFoundException("Formulário não encontrado"));

        if (!template.isActive()) {
            throw new IllegalStateException("Formulário inativo");
        }

        // 2. Criar submissão com status PENDING
        Submission submission = Submission.builder()
                .template(template)
                .patientCpf(request.patient().cpf())
                .patientName(request.patient().name())
                .answersJson(request.answersJson())
                .status(SubmissionStatus.PENDING)
                .build();

        submission = submissionRepository.save(submission);

        // 3. Publicar evento para processamento assíncrono
        // O SubmissionEventHandler vai processar em background
        eventPublisher.publishEvent(new SubmissionCreatedEvent(this, submission.getId()));

        log.info("Submissão criada com sucesso. ID: {} - Status: PENDING", submission.getId());

        // 4. Retornar resposta imediata ao cliente
        return new SubmissionResponse(submission.getId(), submission.getStatus());
    }

    /**
     * MÉTODO DEPRECADO - Mantido apenas para compatibilidade temporária.
     * Use submitForm() para o novo fluxo assíncrono.
     *
     * @deprecated Usar {@link #submitForm(UUID, SubmissionRequest)} com processamento assíncrono
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public SubmissionResponse processSubmission(UUID formUuid, SubmissionRequest request) {
        log.warn("Método processSubmission() está deprecated. Use submitForm() para processamento assíncrono.");
        return submitForm(formUuid, request);
    }
}