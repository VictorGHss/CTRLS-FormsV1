package br.dev.ctrls.api.client.feegow.dto;

import java.util.List;

/**
 * DTO básico para deserializar resposta de listagem de pacientes.
 */
public record FeegowPatientResponse(List<PatientSummary> content) {

    public Long firstId() {
        if (content != null && !content.isEmpty()) {
            return content.get(0).id();
        }
        return null;
    }

    public record PatientSummary(Long id, String nome, String cpf) {
    }
}
