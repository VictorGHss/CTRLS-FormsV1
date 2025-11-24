package br.dev.ctrls.api.application.service.submission;

import br.dev.ctrls.api.client.feegow.FeegowClient;
import br.dev.ctrls.api.client.feegow.dto.FeegowPatientRequest;
import br.dev.ctrls.api.client.feegow.dto.FeegowPatientResponse;
import br.dev.ctrls.api.client.feegow.dto.UploadFileRequest;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Serviço de integração com Feegow com tratamento específico de erros e retry automático.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeegowIntegrationService {

    private final FeegowClient feegowClient;

    /**
     * Busca ou cria paciente no Feegow.
     * Retry automático para erros temporários (503, timeout).
     */
    @Retryable(
        retryFor = {RetryableException.class, FeignException.ServiceUnavailable.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public Long resolvePatient(String token, String cpf, String name) {
        try {
            log.debug("Buscando paciente por CPF no Feegow");
            FeegowPatientResponse response = feegowClient.listPatients(token, cpf);
            Long existingId = response.firstId();

            if (existingId != null) {
                log.info("Paciente já existe no Feegow");
                return existingId;
            }

            log.info("Criando novo paciente no Feegow");
            FeegowPatientRequest createRequest = buildPatientRequest(cpf, name);
            Long createdId = feegowClient.createPatient(token, createRequest);
            log.info("Paciente criado com sucesso no Feegow");
            return createdId;

        } catch (FeignException.BadRequest ex) {
            log.error("Requisição inválida ao Feegow (400): {}", ex.contentUTF8());
            throw new FeegowIntegrationException("Dados inválidos para criação de paciente", ex);

        } catch (FeignException.Unauthorized ex) {
            log.error("Token de autenticação inválido (401)");
            throw new FeegowIntegrationException("Token Feegow inválido ou expirado", ex);

        } catch (FeignException.Forbidden ex) {
            log.error("Acesso negado pelo Feegow (403)");
            throw new FeegowIntegrationException("Sem permissão para acessar Feegow", ex);

        } catch (FeignException.ServiceUnavailable ex) {
            log.warn("Feegow temporariamente indisponível (503) - tentando retry");
            throw ex; // Será capturado pelo @Retryable

        } catch (FeignException ex) {
            log.error("Erro HTTP {} ao comunicar com Feegow: {}", ex.status(), ex.contentUTF8());
            throw new FeegowIntegrationException("Erro na integração com Feegow: HTTP " + ex.status(), ex);
        }
    }

    /**
     * Faz upload de arquivo para o Feegow.
     * Retry automático para erros temporários.
     */
    @Retryable(
        retryFor = {RetryableException.class, FeignException.ServiceUnavailable.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void uploadFile(String token, UploadFileRequest request) {
        try {
            log.debug("Enviando arquivo para Feegow");
            feegowClient.uploadPatientFile(token, request);
            log.info("Arquivo enviado com sucesso ao Feegow");

        } catch (FeignException.ServiceUnavailable ex) {
            log.warn("Feegow temporariamente indisponível (503) - tentando retry");
            throw ex; // Retry automático

        } catch (FeignException ex) {
            // Verifica se é erro 413 (Payload Too Large)
            if (ex.status() == 413) {
                log.error("Arquivo muito grande para upload (413)");
                throw new FeegowIntegrationException("PDF muito grande para upload", ex);
            }
            log.error("Erro HTTP {} ao fazer upload: {}", ex.status(), ex.contentUTF8());
            throw new FeegowIntegrationException("Erro ao enviar arquivo para Feegow: HTTP " + ex.status(), ex);
        }
    }

    private FeegowPatientRequest buildPatientRequest(String cpf, String name) {
        // TODO: Implementar mapeamento completo com dados adicionais do paciente
        // Por enquanto, usa valores padrão
        return FeegowPatientRequest.builder()
                .cpf(cpf)
                .nome(name)
                .sexo("Não informado") // TODO: adicionar ao formulário
                .nascimento("01/01/1990") // TODO: adicionar ao formulário
                .build();
    }
}

