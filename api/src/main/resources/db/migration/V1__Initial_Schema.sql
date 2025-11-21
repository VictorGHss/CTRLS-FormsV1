CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE clinics (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    cnpj CHAR(14) NOT NULL UNIQUE,
    address VARCHAR(255) NOT NULL,
    feegow_api_token VARCHAR(512) NOT NULL,
    primary_color VARCHAR(7),
    logo_url VARCHAR(255),
    link_uuid UUID NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    user_type VARCHAR(31) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE doctors (
    id UUID PRIMARY KEY REFERENCES users(id),
    crm VARCHAR(30) NOT NULL,
    uf CHAR(2) NOT NULL,
    bio VARCHAR(1000),
    profile_photo_url VARCHAR(255),
    banner_url VARCHAR(255)
);

CREATE TABLE secretaries (
    id UUID PRIMARY KEY REFERENCES users(id)
);

CREATE TABLE doctor_clinic (
    doctor_id UUID NOT NULL REFERENCES doctors(id),
    clinic_id UUID NOT NULL REFERENCES clinics(id),
    PRIMARY KEY (doctor_id, clinic_id)
);

CREATE TABLE doctor_secretary (
    doctor_id UUID NOT NULL REFERENCES doctors(id),
    secretary_id UUID NOT NULL REFERENCES secretaries(id),
    PRIMARY KEY (doctor_id, secretary_id)
);

CREATE TABLE form_templates (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(512),
    public_uuid UUID NOT NULL UNIQUE,
    schema_json JSONB NOT NULL,
    active BOOLEAN NOT NULL,
    clinic_id UUID NOT NULL REFERENCES clinics(id),
    doctor_id UUID REFERENCES doctors(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE submissions (
    id UUID PRIMARY KEY,
    form_template_id UUID NOT NULL REFERENCES form_templates(id),
    patient_name VARCHAR(255) NOT NULL,
    patient_cpf CHAR(11) NOT NULL,
    answers_json JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    feegow_request_id VARCHAR(255),
    feegow_patient_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_email VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    scope VARCHAR(50) NOT NULL,
    resource_id VARCHAR(255),
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

