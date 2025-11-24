package br.dev.ctrls.api.domain.form.repository;

import br.dev.ctrls.api.domain.form.FormTemplate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormTemplateRepository extends JpaRepository<FormTemplate, UUID> {

    // O @EntityGraph diz: "Quando buscar por UUID, traga também a clinic e o doctor"
    // Isso evita o erro de LazyInitializationException no Controller
    @EntityGraph(attributePaths = {"clinic", "doctor"})
    Optional<FormTemplate> findByPublicUuid(UUID publicUuid);
}