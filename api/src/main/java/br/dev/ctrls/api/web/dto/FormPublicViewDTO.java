package br.dev.ctrls.api.web.dto;

import br.dev.ctrls.api.domain.clinic.Clinic;
import br.dev.ctrls.api.domain.form.FormTemplate;
import br.dev.ctrls.api.domain.user.Doctor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Template de formulário público com informações de branding")
public record FormPublicViewDTO(
        @Schema(description = "UUID público do formulário para acesso",
                example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Título do formulário", example = "Anamnese Cardiológica")
        String title,

        @Schema(description = "Descrição do formulário", example = "Formulário de avaliação cardíaca pré-consulta")
        String description,

        @Schema(description = "Schema JSON do formulário (estrutura dos campos)")
        String schemaJson,

        @Schema(description = "Informações de branding da clínica")
        BrandingInfo clinicBranding,

        @Schema(description = "Informações do médico (opcional)", nullable = true)
        DoctorBranding doctorBranding
) {
    // Método estático para converter a Entidade em DTO
    public static FormPublicViewDTO fromEntity(FormTemplate template) {
        Clinic clinic = template.getClinic();
        Doctor doctor = template.getDoctor();

        return new FormPublicViewDTO(
                template.getPublicUuid(),
                template.getTitle(),
                template.getDescription(),
                template.getSchemaJson(),
                new BrandingInfo(
                        clinic.getName(),
                        clinic.getLogoUrl(),
                        clinic.getPrimaryColor(),
                        clinic.getAddress()
                ),
                doctor != null ? new DoctorBranding(
                        doctor.getName(),
                        doctor.getProfilePhotoUrl(),
                        doctor.getBannerUrl(),
                        doctor.getBio()
                ) : null
        );
    }

    @Schema(description = "Informações de marca/identidade visual da clínica")
    public record BrandingInfo(
            @Schema(description = "Nome da clínica", example = "Clínica Cardiológica São Paulo")
            String name,

            @Schema(description = "URL da logo da clínica",
                    example = "https://cdn.clinica.com/logo.png",
                    nullable = true)
            String logoUrl,

            @Schema(description = "Cor primária em hexadecimal",
                    example = "#0066CC",
                    nullable = true)
            String primaryColor,

            @Schema(description = "Endereço da clínica",
                    example = "Av. Paulista, 1000 - São Paulo/SP")
            String address
    ) {}

    @Schema(description = "Informações do médico responsável pelo formulário")
    public record DoctorBranding(
            @Schema(description = "Nome completo do médico", example = "Dr. João Silva")
            String name,

            @Schema(description = "URL da foto de perfil do médico",
                    example = "https://cdn.clinica.com/medico.jpg",
                    nullable = true)
            String profilePhotoUrl,

            @Schema(description = "URL da imagem de banner/capa",
                    example = "https://cdn.clinica.com/banner.jpg",
                    nullable = true)
            String bannerUrl,

            @Schema(description = "Biografia/descrição do médico",
                    example = "Cardiologista com 15 anos de experiência",
                    nullable = true)
            String bio
    ) {}
}