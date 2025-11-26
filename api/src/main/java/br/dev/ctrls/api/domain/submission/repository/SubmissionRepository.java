package br.dev.ctrls.api.domain.submission.repository;

import br.dev.ctrls.api.domain.submission.Submission;
import br.dev.ctrls.api.domain.submission.SubmissionStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório de submissões com suporte a EntityGraph para evitar LazyInitializationException.
 */
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    /**
     * Busca submissão por ID com relacionamentos carregados eagerly.
     *
     * IMPORTANTE: Use este método em processamento assíncrono (@Async) para evitar
     * LazyInitializationException quando acessar submission.getTemplate().getClinic().
     *
     * A anotação @Query é necessária porque "WithGraph" não é uma propriedade reconhecida
     * pelo Spring Data JPA no método derivado. O @EntityGraph carrega os relacionamentos
     * especificados em uma única query (LEFT JOIN).
     *
     * @param id ID da submissão
     * @return Submissão com template e clinic carregados
     */
    @EntityGraph(attributePaths = {"template", "template.clinic", "template.doctor"})
    @Query("SELECT s FROM Submission s WHERE s.id = :id")
    Optional<Submission> findByIdWithGraph(@Param("id") UUID id);

    /**
     * Busca submissões com filtros e relacionamentos carregados.
     * EntityGraph carrega template+clínica+médico para evitar N+1 queries.
     */
    @EntityGraph(attributePaths = {"template", "template.clinic", "template.doctor"})
    @Query("SELECT s FROM Submission s WHERE (:clinicId IS NULL OR s.template.clinic.id = :clinicId) " +
            "AND (:status IS NULL OR s.status = :status)")
    Page<Submission> searchWithFilters(@Param("clinicId") UUID clinicId,
                                       @Param("status") SubmissionStatus status,
                                       @Param("patientName") String patientName,
                                       Pageable pageable);
}