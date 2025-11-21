package br.dev.ctrls.api.web.dto;

import br.dev.ctrls.api.domain.form.FormTemplate;
import java.util.UUID;

/**
 * DTO simplificado para exposição pública de templates.
 */
public record FormPublicViewDTO(UUID id, String title, String description, String schemaJson) {

    public static FormPublicViewDTO from(FormTemplate template) {
        return new FormPublicViewDTO(template.getPublicUuid(), template.getTitle(), template.getDescription(), template.getSchemaJson());
    }
}

