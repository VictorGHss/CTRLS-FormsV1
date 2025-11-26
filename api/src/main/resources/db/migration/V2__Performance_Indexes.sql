-- Performance indexes for common queries

-- Users table
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_user_type ON users(user_type);

-- Doctors table
CREATE INDEX idx_doctors_crm_uf ON doctors(crm, uf);

-- Doctor-Clinic relationships
CREATE INDEX idx_doctor_clinic_clinic_id ON doctor_clinic(clinic_id);
CREATE INDEX idx_doctor_clinic_doctor_id ON doctor_clinic(doctor_id);

-- Doctor-Secretary relationships
CREATE INDEX idx_doctor_secretary_doctor_id ON doctor_secretary(doctor_id);
CREATE INDEX idx_doctor_secretary_secretary_id ON doctor_secretary(secretary_id);

-- Clinics
CREATE INDEX idx_clinics_cnpj ON clinics(cnpj);
CREATE INDEX idx_clinics_link_uuid ON clinics(link_uuid);

-- Form Templates
CREATE INDEX idx_form_templates_public_uuid ON form_templates(public_uuid);
CREATE INDEX idx_form_templates_clinic_id ON form_templates(clinic_id);
CREATE INDEX idx_form_templates_doctor_id ON form_templates(doctor_id);
CREATE INDEX idx_form_templates_active ON form_templates(active);

-- Submissions
CREATE INDEX idx_submissions_form_template_id ON submissions(form_template_id);
CREATE INDEX idx_submissions_patient_cpf ON submissions(patient_cpf);
CREATE INDEX idx_submissions_status ON submissions(status);
CREATE INDEX idx_submissions_created_at ON submissions(created_at);

-- Audit Logs
CREATE INDEX idx_audit_logs_actor_email ON audit_logs(actor_email);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);


