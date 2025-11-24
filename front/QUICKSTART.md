# 🚀 QUICK START GUIDE - CTRLS-Forms Frontend

## Prerequisites
- Node.js 18+ installed
- Backend running on `http://localhost:8080`

## Installation Steps

### 1. Install Dependencies
```bash
npm install
```

### 2. Start Development Server
```bash
npm run dev
```

The app will be available at: **http://localhost:3000**

### 3. Build for Production
```bash
npm run build
```

## 🧪 Testing the Application

### Test Login Flow
1. Navigate to: http://localhost:3000/login
2. Enter credentials:
   - **Email**: doctor@example.com
   - **Password**: your_password
   - **Clinic ID**: your_clinic_uuid (get from backend)
3. Click "Entrar"
4. You'll be redirected to `/admin/dashboard`

### Test Public Form
1. Get a form UUID from the backend
2. Navigate to: http://localhost:3000/forms/{uuid}
3. Fill out the patient information
4. Fill out the dynamic form fields
5. Click "Enviar Formulário"

### Test Dashboard
1. After logging in, you should see the submissions table
2. Use pagination to navigate through submissions
3. Click "Sair" to logout

## 📁 Project Structure Overview

```
src/
├── components/ui/          # Reusable UI components (Button, Input, Card, Badge)
├── features/
│   ├── auth/              # LoginPage.tsx
│   ├── dashboard/         # DashboardPage.tsx (protected)
│   └── public-form/       # PublicFormPage.tsx (public)
├── lib/
│   ├── axios.ts           # ⚠️ API client with Auth + X-Clinic-ID interceptors
│   └── utils.ts           # Utility functions
├── services/
│   └── api.service.ts     # API endpoints
└── types/
    └── api.ts             # TypeScript interfaces
```

## 🔐 Critical Configuration

### API Base URL
Located in: `src/lib/axios.ts`
```typescript
const BASE_URL = 'http://localhost:8080';
```

### Request Headers (Auto-injected)
- `Authorization: Bearer <token>`
- `X-Clinic-ID: <clinic_uuid>` ⚠️ **CRITICAL for multi-tenancy**

## 🎨 Key Features Implemented

✅ **Mobile-First Patient Form**
- Dynamic theming from clinic branding
- Renders form from JSON schema
- Success feedback screen

✅ **Desktop-First Doctor Dashboard**
- Paginated submissions table
- Status badges (Pending, Processed, Error, Sync Error)
- Responsive design

✅ **Authentication System**
- Login with JWT
- Protected routes
- Auto-redirect on 401

## 🐛 Troubleshooting

### Backend Connection Issues
- Make sure backend is running on `http://localhost:8080`
- Check browser console for CORS errors
- Verify API endpoints match backend routes

### Login Issues
- Verify email, password, and clinic ID are correct
- Check if token is being stored in localStorage
- Open DevTools > Application > Local Storage

### Form Not Loading
- Verify the UUID is correct
- Check if the form exists in the backend
- Look for errors in the browser console

## 📊 API Endpoints Reference

| Method | Endpoint                          | Auth Required | Description           |
|--------|-----------------------------------|---------------|-----------------------|
| POST   | `/api/auth/login`                 | No            | Login                 |
| GET    | `/api/public/forms/{uuid}`        | No            | Get form template     |
| POST   | `/api/public/forms/{uuid}/submit` | No            | Submit form           |
| GET    | `/api/submissions`                | Yes           | Get submissions list  |

## 🎯 Next Development Tasks

- [ ] Add form builder UI
- [ ] Implement submission detail view
- [ ] Add search/filter to dashboard
- [ ] Export submissions (PDF/Excel)
- [ ] Real-time notifications
- [ ] Doctor profile management

## 📞 Support

For issues or questions, refer to the main README.md file.

