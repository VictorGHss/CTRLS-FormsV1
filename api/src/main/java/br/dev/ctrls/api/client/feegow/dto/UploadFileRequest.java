package br.dev.ctrls.api.client.feegow.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Solicitação para envio de arquivo em base64.
 */
public record UploadFileRequest(
        Long patient_id,
        @NotBlank String base64_file,
        @NotBlank String filename
) {
}

