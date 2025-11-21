package br.dev.ctrls.api.domain.form.repository;

import br.dev.ctrls.api.domain.form.FormTemplate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório para templates de formulários.
 */
public interface FormTemplateRepository extends JpaRepository<FormTemplate, UUID> {

    Optional<FormTemplate> findByPublicUuid(UUID publicUuid);
}
