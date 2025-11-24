import axios from 'axios';

// Base API Configuration
const BASE_URL = 'http://localhost:8080';

// Create axios instance
export const api = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ============================================
// Request Interceptor - Add Auth & Clinic ID
// ============================================
api.interceptors.request.use(
  (config) => {
    // Get token from localStorage
    const token = localStorage.getItem('auth_token');
    const clinicId = localStorage.getItem('clinic_id');

    // Add Authorization header if token exists
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Add X-Clinic-ID header if clinicId exists (CRITICAL for multi-tenancy)
    if (clinicId) {
      config.headers['X-Clinic-ID'] = clinicId;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// ============================================
// Response Interceptor - Handle 401 errors
// ============================================
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // If 401 Unauthorized, clear auth and redirect to login
    if (error.response?.status === 401) {
      localStorage.removeItem('auth_token');
      localStorage.removeItem('clinic_id');

      // Only redirect if not already on login page
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);

// ============================================
// Auth Helper Functions
// ============================================
export const authStorage = {
  setAuth: (token: string, clinicId: string) => {
    localStorage.setItem('auth_token', token);
    localStorage.setItem('clinic_id', clinicId);
  },

  getAuth: () => {
    return {
      token: localStorage.getItem('auth_token'),
      clinicId: localStorage.getItem('clinic_id'),
    };
  },

  clearAuth: () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('clinic_id');
  },

  isAuthenticated: () => {
    return !!localStorage.getItem('auth_token');
  },
};

export default api;

