package br.dev.ctrls.api.web.dto;

import br.dev.ctrls.api.domain.clinic.Clinic;
import br.dev.ctrls.api.domain.form.FormTemplate;
import br.dev.ctrls.api.domain.user.Doctor;

import java.util.UUID;

public record FormPublicViewDTO(
        UUID id,
        String title,
        String description,
        String schemaJson,
        BrandingInfo clinicBranding,
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

    public record BrandingInfo(String name, String logoUrl, String primaryColor, String address) {
    }

    public record DoctorBranding(String name, String profilePhotoUrl, String bannerUrl, String bio) {
    }
}