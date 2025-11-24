# CTRLS-Forms Frontend

Multi-tenant medical SaaS for form submissions with patient interface and doctor dashboard.

## 🚀 Tech Stack

- **Framework**: React 18 + Vite + TypeScript
- **Styling**: Tailwind CSS + Shadcn/UI
- **State Management**: TanStack Query (React Query) v5
- **Routing**: React Router DOM v6
- **HTTP Client**: Axios
- **Icons**: Lucide React

## 📁 Project Structure

```
src/
├── components/
│   └── ui/              # Reusable UI components (Shadcn-style)
├── features/
│   ├── auth/            # Login page
│   ├── dashboard/       # Doctor dashboard (protected)
│   └── public-form/     # Patient form submission (public)
├── lib/
│   ├── axios.ts         # API client with interceptors
│   └── utils.ts         # Utility functions
├── services/
│   └── api.service.ts   # API endpoints
├── types/
│   └── api.ts           # TypeScript interfaces
├── App.tsx              # Main app with routing
├── main.tsx             # Entry point
└── style.css            # Tailwind imports & CSS variables
```

## 🔧 Installation & Setup

### 1. Install Dependencies

```bash
npm install
```

### 2. Start Development Server

```bash
npm run dev
```

The app will run on **http://localhost:3000**

### 3. Build for Production

```bash
npm run build
```

## 🌐 API Integration

### Backend URL
- **Base URL**: `http://localhost:8080`
- Make sure the backend is running before starting the frontend

### Critical Headers

All protected requests MUST include:
- `Authorization: Bearer <token>`
- `X-Clinic-ID: <clinic_uuid>`

These headers are automatically added by the Axios interceptor in `src/lib/axios.ts`.

## 📋 Routes

### Public Routes
- `/forms/:uuid` - Public patient form (Mobile-first)
- `/login` - Login page

### Protected Routes (Admin)
- `/admin/dashboard` - Doctor dashboard with submissions list (Desktop-first)

## 🎨 Features

### Patient Form (`/forms/:uuid`)
- ✅ Mobile-first responsive design
- ✅ Dynamic theming based on clinic branding
- ✅ Renders form from JSON schema
- ✅ Patient information collection
- ✅ Success feedback after submission

### Doctor Dashboard (`/admin/dashboard`)
- ✅ Desktop-optimized table layout
- ✅ Pagination controls
- ✅ Status badges (Pending, Processed, Error, Sync Error)
- ✅ Responsive design (hides columns on smaller screens)

### Authentication
- ✅ Login with email, password, and clinic ID
- ✅ Token storage in localStorage
- ✅ Protected route wrapper
- ✅ Auto-redirect on 401

## 🔐 Authentication Flow

1. User logs in with email, password, and **Clinic ID** (temporary for MVP)
2. Backend returns JWT token
3. Frontend stores token + clinic ID in localStorage
4. Axios interceptor adds headers to all private requests
5. On 401, user is redirected to login

## 📊 API Endpoints Used

| Scope   | Method | Endpoint                          | Description                  |
|---------|--------|-----------------------------------|------------------------------|
| Public  | GET    | `/api/public/forms/{uuid}`        | Fetch form template          |
| Public  | POST   | `/api/public/forms/{uuid}/submit` | Submit patient form          |
| Private | POST   | `/api/auth/login`                 | Login                        |
| Private | GET    | `/api/submissions`                | Get submissions (paginated)  |

## 🎨 Dynamic Theming

The public form applies the clinic's primary color dynamically:

```typescript
// In PublicFormPage.tsx
useEffect(() => {
  if (template?.clinicBranding?.primaryColor) {
    const hsl = hexToHSL(template.clinicBranding.primaryColor);
    document.documentElement.style.setProperty('--primary', hsl);
  }
}, [template]);
```

This changes the header background and submit button to match the clinic's brand.

## 🧪 Development Tips

### Test Public Form
1. Get a form UUID from the backend
2. Navigate to: `http://localhost:3000/forms/{uuid}`

### Test Dashboard
1. Login with credentials: `http://localhost:3000/login`
2. You'll need:
   - Email
   - Password
   - Clinic ID (UUID)

## 📦 Key Dependencies

```json
{
  "react": "^18.x",
  "react-dom": "^18.x",
  "react-router-dom": "^6.x",
  "@tanstack/react-query": "^5.x",
  "axios": "^1.x",
  "tailwindcss": "^3.x",
  "lucide-react": "latest",
  "clsx": "latest",
  "tailwind-merge": "latest"
}
```

## 🚨 Important Notes

### Multi-Tenancy
- **X-Clinic-ID** header is CRITICAL for the backend
- Without it, requests will fail
- The clinic ID is stored in localStorage after login

### Form Schema Parsing
- The `schemaJson` field is a **stringified JSON**
- Must be parsed with `JSON.parse()` before rendering
- Example structure:
  ```json
  [
    {
      "id": "q1",
      "type": "text",
      "label": "Question 1",
      "required": true
    }
  ]
  ```

### Submission Flow
1. User fills patient info (name, CPF, birth date, etc.)
2. User fills dynamic form fields
3. Answers are stringified: `JSON.stringify(formAnswers)`
4. Sent to backend as `SubmissionRequest`

## 🎯 Next Steps (Phase 2+)

- [ ] Add form builder for doctors
- [ ] Implement submission detail view
- [ ] Add filtering and search in dashboard
- [ ] Export submissions to PDF/Excel
- [ ] Add real-time notifications
- [ ] Implement doctor profile management
- [ ] Add analytics dashboard

## 📝 License

Internal project for CTRLS-Forms medical SaaS.

