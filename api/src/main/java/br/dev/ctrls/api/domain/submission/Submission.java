package br.dev.ctrls.api.domain.submission;

import br.dev.ctrls.api.domain.common.BaseEntity;
import br.dev.ctrls.api.domain.form.FormTemplate;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "submissions")
public class Submission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_template_id", nullable = false)
    private FormTemplate template;

    @NotBlank
    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @NotBlank
    @Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 dígitos numéricos")
    @Column(name = "patient_cpf", nullable = false, length = 11)
    private String patientCpf;

    @NotBlank
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers_json", nullable = false, columnDefinition = "jsonb")
    private String answersJson;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status = SubmissionStatus.PENDING;

    @Column(name = "feegow_request_id")
    private String feegowRequestId;

    @Column(name = "feegow_patient_id")
    private String feegowPatientId;
}