# Phase 1 Output - Core Files

## Shell Commands to Start the Project

```powershell
# Navigate to the project directory
cd C:\Projeto\CTRLS-Forms\front

# Install all dependencies
npm install

# Start development server (runs on http://localhost:3000)
npm run dev

# Build for production
npm run build
```

---

## File 1: `src/lib/axios.ts` - The API Client

```typescript
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
```

**Key Features:**
- ✅ Base URL configured to `http://localhost:8080`
- ✅ Request interceptor adds `Authorization: Bearer <token>`
- ✅ Request interceptor adds `X-Clinic-ID: <clinic_uuid>` (CRITICAL!)
- ✅ Response interceptor handles 401 errors (auto-logout)
- ✅ Helper functions for managing auth state in localStorage

---

## File 2: `src/App.tsx` - Routing Configuration

```typescript
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { authStorage } from './lib/axios';
import LoginPage from './features/auth/LoginPage';
import DashboardPage from './features/dashboard/DashboardPage';
import PublicFormPage from './features/public-form/PublicFormPage';

// Protected Route wrapper
function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const isAuthenticated = authStorage.isAuthenticated();
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  
  return <>{children}</>;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public Routes */}
        <Route path="/forms/:uuid" element={<PublicFormPage />} />
        <Route path="/login" element={<LoginPage />} />
        
        {/* Protected Admin Routes */}
        <Route 
          path="/admin/dashboard" 
          element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          } 
        />
        
        {/* Default Redirect */}
        <Route path="/" element={<Navigate to="/login" replace />} />
        
        {/* 404 Fallback */}
        <Route path="*" element={
          <div className="min-h-screen flex items-center justify-center">
            <div className="text-center">
              <h1 className="text-4xl font-bold mb-4">404</h1>
              <p className="text-muted-foreground">Página não encontrada</p>
            </div>
          </div>
        } />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
```

**Routes Configured:**
- ✅ `/login` - Login page (public)
- ✅ `/forms/:uuid` - Patient form submission (public)
- ✅ `/admin/dashboard` - Doctor dashboard (protected)
- ✅ `/` - Redirects to `/login`
- ✅ `*` - 404 page

**Protected Route Logic:**
- Checks if user is authenticated using `authStorage.isAuthenticated()`
- If not authenticated → Redirects to `/login`
- If authenticated → Renders the protected component

---

## Entry Point: `src/main.tsx`

```typescript
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import App from './App';
import './style.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

createRoot(document.getElementById('app')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </StrictMode>
);
```

**React Query Setup:**
- ✅ Global QueryClient with retry and refetch configuration
- ✅ Wraps the entire app with `QueryClientProvider`

---

## Complete Project Structure

```
C:\Projeto\CTRLS-Forms\front\
├── src/
│   ├── lib/
│   │   └── axios.ts              ⭐ API Client (File 1)
│   ├── App.tsx                   ⭐ Routing (File 2)
│   ├── main.tsx                  ⭐ Entry Point
│   ├── components/ui/            # Shadcn-style components
│   ├── features/
│   │   ├── auth/LoginPage.tsx
│   │   ├── dashboard/DashboardPage.tsx
│   │   └── public-form/PublicFormPage.tsx
│   ├── services/api.service.ts   # API endpoints
│   ├── types/api.ts              # TypeScript interfaces
│   └── style.css                 # Tailwind CSS
├── index.html
├── package.json
├── vite.config.ts
├── tailwind.config.js
└── tsconfig.json
```

---

## API Service Example: `src/services/api.service.ts`

```typescript
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
```

---

## Usage Example in Components

### Login Example (LoginPage.tsx)

```typescript
import { useMutation } from '@tanstack/react-query';
import { authApi } from '../../services/api.service';
import { authStorage } from '../../lib/axios';

const loginMutation = useMutation({
  mutationFn: authApi.login,
  onSuccess: (data) => {
    // Store token and clinic ID
    authStorage.setAuth(data.token, clinicId);
    // Redirect to dashboard
    navigate('/admin/dashboard');
  },
});

const handleSubmit = (e: React.FormEvent) => {
  e.preventDefault();
  loginMutation.mutate({ email, password, clinicId });
};
```

### Dashboard Example (DashboardPage.tsx)

```typescript
import { useQuery } from '@tanstack/react-query';
import { submissionsApi } from '../../services/api.service';

const { data, isLoading } = useQuery({
  queryKey: ['submissions', page],
  queryFn: () => submissionsApi.getSubmissions({ page, size: 10 }),
});

// data.content contains the submissions array
// data.totalPages, data.number for pagination
```

---

## Testing the Setup

### 1. Start the Dev Server
```powershell
npm run dev
```

### 2. Test Routes
- **Login**: http://localhost:3000/login
- **Dashboard**: http://localhost:3000/admin/dashboard (redirects to login if not authenticated)
- **Public Form**: http://localhost:3000/forms/{uuid}

### 3. Check Browser DevTools
- **Console**: Look for any errors
- **Network**: Verify API requests have correct headers
- **Application > Local Storage**: Check `auth_token` and `clinic_id` after login

---

## 🎉 Phase 1 Complete!

You now have:
✅ Fully configured React + Vite + TypeScript project
✅ Tailwind CSS with professional healthcare styling
✅ Axios client with Authorization + X-Clinic-ID interceptors
✅ React Router with protected routes
✅ React Query for data fetching
✅ Complete UI components (Button, Input, Card, Badge)
✅ Login page, Dashboard, and Public Form

**Next**: Connect to your Spring Boot backend and start testing the integration! 🚀

