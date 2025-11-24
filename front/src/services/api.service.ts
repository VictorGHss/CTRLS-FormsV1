import api from '../lib/axios';
import type {
  LoginRequest,
  LoginResponse,
  FormTemplateDTO,
  SubmissionRequest,
  SubmissionSummaryDTO,
  Page
} from '../types/api';

// ============================================
// Authentication API
// ============================================
export const authApi = {
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await api.post<LoginResponse>('/api/auth/login', data);
    return response.data;
  },
};

// ============================================
// Public Forms API
// ============================================
export const publicFormsApi = {
  getFormTemplate: async (uuid: string): Promise<FormTemplateDTO> => {
    const response = await api.get<FormTemplateDTO>(`/api/public/forms/${uuid}`);
    return response.data;
  },

  submitForm: async (uuid: string, data: SubmissionRequest): Promise<void> => {
    await api.post(`/api/public/forms/${uuid}/submit`, data);
  },
};

// ============================================
// Private Submissions API (Dashboard)
// ============================================
export interface SubmissionsParams {
  page?: number; // 0-based
  size?: number;
  sort?: string; // e.g., "createdAt,desc"
}

export const submissionsApi = {
  getSubmissions: async (params: SubmissionsParams = {}): Promise<Page<SubmissionSummaryDTO>> => {
    const response = await api.get<Page<SubmissionSummaryDTO>>('/api/submissions', {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 10,
        sort: params.sort ?? 'createdAt,desc',
      },
    });
    return response.data;
  },
};

