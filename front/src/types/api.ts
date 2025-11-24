// TypeScript Interfaces for API Communication

// ============================================
// Auth Types
// ============================================
export interface LoginRequest {
  email: string;
  password: string;
  clinicId: string;
}

export interface LoginResponse {
  token: string;
}

// ============================================
// Public Form Types
// ============================================
export interface BrandingInfo {
  name: string;
  logoUrl?: string;
  primaryColor?: string; // Hex code (e.g., #007bff)
  address?: string;
}

export interface DoctorBranding {
  name: string;
  profilePhotoUrl?: string;
  bannerUrl?: string;
}

export interface FormTemplateDTO {
  id: string; // Public UUID
  title: string;
  description?: string;
  schemaJson: string; // RAW JSON String -> Needs JSON.parse() to render inputs
  clinicBranding: BrandingInfo;
  doctorBranding?: DoctorBranding;
}

// ============================================
// Form Schema Types (Parsed from schemaJson)
// ============================================
export interface FormField {
  id: string;
  type: 'text' | 'textarea' | 'number' | 'date' | 'select' | 'radio' | 'checkbox';
  label: string;
  placeholder?: string;
  required?: boolean;
  options?: string[]; // For select, radio, checkbox
}

// ============================================
// Submission Types
// ============================================
export interface PatientInfo {
  name: string;
  cpf: string;
  sexo: 'M' | 'F';
  nascimento: string; // Format: 'YYYY-MM-DD'
  email?: string;
  celular?: string;
}

export interface SubmissionRequest {
  patient: PatientInfo;
  answersJson: string; // Stringified JSON of the form answers
}

// ============================================
// Dashboard Types
// ============================================
export type SubmissionStatus = 'PENDING' | 'PROCESSED' | 'ERROR' | 'SYNC_ERROR';

export interface SubmissionSummaryDTO {
  id: string;
  patientName: string;
  status: SubmissionStatus;
  formTitle: string;
  createdAt: string; // ISO String
}

// ============================================
// Pagination Wrapper
// ============================================
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // Current page index (0-based)
}

