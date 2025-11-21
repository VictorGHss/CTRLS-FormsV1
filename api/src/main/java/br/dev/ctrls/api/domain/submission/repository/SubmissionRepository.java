package br.dev.ctrls.api.domain.submission.repository;

import br.dev.ctrls.api.domain.submission.Submission;
import br.dev.ctrls.api.domain.submission.SubmissionStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório de submissões para consultas e sincronizações.
 */
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    @EntityGraph(attributePaths = {"template", "template.clinic", "template.doctor"})
    @Query("SELECT s FROM Submission s WHERE (:clinicId IS NULL OR s.template.clinic.id = :clinicId) " +
            "AND (:status IS NULL OR s.status = :status) " +
            "AND (:patientName IS NULL OR LOWER(s.patientName) LIKE LOWER(CONCAT('%', :patientName, '%')))")
    Page<Submission> searchWithFilters(@Param("clinicId") UUID clinicId,
                                       @Param("status") SubmissionStatus status,
                                       @Param("patientName") String patientName,
                                       Pageable pageable);
}
