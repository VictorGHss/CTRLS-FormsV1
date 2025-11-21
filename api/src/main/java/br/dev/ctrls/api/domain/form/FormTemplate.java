package br.dev.ctrls.api.domain.form;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import br.dev.ctrls.api.domain.clinic.Clinic;
import br.dev.ctrls.api.domain.common.BaseEntity;
import br.dev.ctrls.api.domain.user.Doctor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "form_templates")
public class FormTemplate extends BaseEntity {

    @Builder.Default
    @Column(name = "public_uuid", nullable = false, unique = true)
    private UUID publicUuid = UUID.randomUUID();

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(length = 512)
    private String description;

    @NotBlank
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_json", nullable = false, columnDefinition = "jsonb")
    private String schemaJson;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;
}