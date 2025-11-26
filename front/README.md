# CTRLS-Forms Frontend

Multi-tenant medical SaaS platform for form submissions with patient interface and doctor dashboard.

## 🚀 Tech Stack

- **Framework**: React + Vite + TypeScript
- **Styling**: Tailwind CSS + Shadcn/UI
- **State**: TanStack Query (React Query) v5
- **Routing**: React Router DOM v6
- **HTTP**: Axios
- **Icons**: Lucide React
- **Deploy**: Docker + Nginx + GCP Cloud Run

---

## 📁 Project Structure

```
src/
├── components/ui/       # Reusable UI components (Button, Card, Input, Badge)
├── features/
│   ├── auth/           # Login page
│   ├── dashboard/      # Doctor dashboard (protected)
│   └── public-form/    # Patient form submission (public)
├── lib/
│   ├── axios.ts        # API client with auth interceptors
│   └── utils.ts        # Utility functions (cn, hexToHSL, formatters)
├── services/
│   └── api.service.ts  # API endpoints
└── types/
    └── api.ts          # TypeScript interfaces
```

---

## 🔧 Quick Start

### Prerequisites

- Node.js 18+
- Backend API running on `http://localhost:8080`

### Installation

```bash
# Install dependencies
npm install

# (Optional) Configure environment
cp .env.example .env.local

# Start development server
npm run dev
```

The app runs on **http://localhost:5173**

### Build

```bash
npm run build
```

---

## 🌐 API Integration

### Backend Configuration

- **Base URL**: `http://localhost:8080` (development)
- **Production**: Set via `VITE_API_BASE_URL` environment variable

### Authentication Headers

All protected requests include:
- `Authorization: Bearer <token>`
- `X-Clinic-ID: <clinic_uuid>` ⚠️ **Critical for multi-tenancy**

Configured automatically in `src/lib/axios.ts`

### Endpoints

| Scope | Method | Endpoint | Description |
|-------|--------|----------|-------------|
| Public | GET | `/api/public/forms/{uuid}` | Get form template |
| Public | POST | `/api/public/forms/{uuid}/submit` | Submit form |
| Private | POST | `/api/auth/login` | Login |
| Private | GET | `/api/submissions` | List submissions (paginated) |

---

## 📋 Application Routes

### Public
- `/forms/:uuid` - Patient form (mobile-first, dynamic theming)
- `/login` - Login page

### Protected (Admin)
- `/admin/dashboard` - Submissions list (desktop-first)

---

## 🎨 Features

### Patient Form (`/forms/:uuid`)
- ✅ Mobile-first responsive design
- ✅ Dynamic theming (clinic branding colors)
- ✅ JSON schema-based form rendering
- ✅ Patient data collection (CPF, birth date, etc.)
- ✅ Success feedback

### Doctor Dashboard (`/admin/dashboard`)
- ✅ Desktop-optimized table
- ✅ Pagination
- ✅ Status badges (Pending, Processed, Error, Sync Error)
- ✅ Responsive (hides columns on mobile)

---

## 🚀 Deployment (GCP Cloud Run)

### Quick Deploy

```powershell
.\deploy-frontend.ps1 -BackendUrl "https://your-api.a.run.app"
```

### Prerequisites

- Docker Desktop running
- Google Cloud SDK (`gcloud`) installed
- Authenticated: `gcloud auth login`
- Backend API already deployed

### Local Docker Test

```powershell
# Build
docker build --build-arg VITE_API_BASE_URL=http://localhost:8080 -t ctrls-web:test .

# Run
docker run -p 8080:8080 ctrls-web:test

# Access: http://localhost:8080
```

### Deployment Documentation

See [`docs/DEPLOY.md`](docs/DEPLOY.md) for complete deployment guide.

---

## 🧪 Development Tips

### Test Public Form
1. Get a form UUID from the backend
2. Navigate to: `http://localhost:5173/forms/{uuid}`

### Test Dashboard
1. Login at: `http://localhost:5173/login`
2. Required credentials:
   - Email
   - Password
   - Clinic ID (UUID) - Temporary for MVP

### Dynamic Theming

The public form applies clinic branding dynamically:

```typescript
// Applies primaryColor from API to CSS variables
useEffect(() => {
  if (template?.clinicBranding?.primaryColor) {
    const hsl = hexToHSL(template.clinicBranding.primaryColor);
    document.documentElement.style.setProperty('--primary', hsl);
  }
}, [template]);
```

---

## 🔐 Multi-Tenancy

⚠️ **Critical**: The `X-Clinic-ID` header is **required** for all private requests.

- Stored in `localStorage` after login
- Automatically added by Axios interceptor
- Without it, backend requests will fail

---

## 📦 Key Dependencies

```json
{
  "react": "^19.2.0",
  "react-router-dom": "^7.9.6",
  "@tanstack/react-query": "^5.90.10",
  "axios": "^1.13.2",
  "tailwindcss": "^3.4.18",
  "lucide-react": "^0.554.0"
}
```

---

## 🐛 Troubleshooting

### TypeScript Error: `Property 'env' does not exist`

**Solution**: Restart TypeScript Server
- VSCode: `Ctrl+Shift+P` → "TypeScript: Restart TS Server"
- Or restart VSCode

### Docker Build Fails

**Check**:
- Docker Desktop is running
- `.dockerignore` exists
- `npm run build` works locally

### CORS Errors

**Backend must allow** frontend origin:
```java
@CrossOrigin(origins = {"http://localhost:5173", "https://your-frontend.a.run.app"})
```

---

## 📚 Documentation

- [`docs/DEPLOY.md`](docs/DEPLOY.md) - Complete deployment guide
- [`docs/COMMANDS.md`](docs/COMMANDS.md) - Useful commands reference

---

## 📝 License

Internal project for CTRLS-Forms medical SaaS platform.

---

**Made with ❤️ using React + Vite + TypeScript + Tailwind CSS**

