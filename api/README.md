# CTRLS Forms API

API backend para o sistema CTRLS Forms - Plataforma de formulários clínicos com multi-tenancy.

## 🚀 Quick Start

### Desenvolvimento Local

```bash
# 1. Iniciar PostgreSQL com Docker
docker-compose up -d

# 2. Rodar aplicação
./mvnw spring-boot:run
```

A aplicação estará disponível em: http://localhost:8080

**Dados de Teste (criados automaticamente):**
- Médico: `victor@ctrls.dev` / `password`
- Admin: `admin@ctrls.dev` / `password`
- Formulário público: verificar logs para obter UUID

### Deploy para GCP Cloud Run

```powershell
# Deploy completo
.\deploy-backend.ps1

# Atualização rápida (apenas código)
.\deploy-update.ps1
```

---

## 📋 Pré-requisitos

### Desenvolvimento
- Java 21
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 15+

### Deploy GCP
- [Google Cloud SDK](https://cloud.google.com/sdk)
- Docker Desktop
- Projeto GCP com billing habilitado
- Autenticação: `gcloud auth login`

---

## 📂 Estrutura do Projeto

```
api/
├── src/
│   ├── main/
│   │   ├── java/br/dev/ctrls/api/
│   │   │   ├── ApiApplication.java
│   │   │   ├── application/      # Use Cases & Services
│   │   │   ├── client/           # Integrações externas (Feegow, Cloudinary)
│   │   │   ├── config/           # Configurações (Redis, Async, etc)
│   │   │   ├── domain/           # Entidades & Repositórios
│   │   │   ├── infrastructure/   # Segurança, Persistence, Bootstrap
│   │   │   ├── tenant/           # Multi-tenancy (Context, Filter)
│   │   │   └── web/              # Controllers REST & DTOs
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-prod.properties
│   │       └── db/migration/
│   │           ├── V1__Initial_Schema.sql
│   │           └── V2__Performance_Indexes.sql
│   └── test/
├── deploy-backend.ps1            # Deploy completo GCP
├── deploy-update.ps1             # Update rápido
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── README.md
└── TROUBLESHOOTING.md
```

---

## 🛠️ Scripts Disponíveis

### Deploy
- **`deploy-backend.ps1`** - Deploy completo (infraestrutura + código)
- **`deploy-update.ps1`** - Atualização rápida (apenas código)

**Nota:** Outros scripts foram removidos para simplificar. Veja `TROUBLESHOOTING.md` para comandos manuais.

---

## 🗄️ Database

### Migrations Flyway

O schema é criado automaticamente via Flyway:

- **V1__Initial_Schema.sql** - 9 tabelas:
  - `clinics`, `users`, `doctors`, `secretaries`
  - `doctor_clinic`, `doctor_secretary`
  - `form_templates`, `submissions`, `audit_logs`

- **V2__Performance_Indexes.sql** - Índices de performance

### Resetar Banco Local

```bash
psql -U docker
DROP DATABASE clinical_forms;
CREATE DATABASE clinical_forms;
\q

# Reiniciar Spring Boot (Flyway recria tudo)
```

### Resetar Banco Cloud SQL

```bash
# 1. Conectar
gcloud sql connect ctrls-postgres --user=docker --database=postgres

# 2. Recriar
DROP DATABASE IF EXISTS clinical_forms;
CREATE DATABASE clinical_forms;
\q

# 3. Deploy (Flyway recria schema)
.\deploy-backend.ps1
```

---

## 🌐 API Endpoints

### Autenticação

```bash
POST /api/auth/login
{
  "email": "victor@ctrls.dev",
  "password": "password",
  "clinicId": "uuid-da-clinica"
}
```

### Formulário Público (SEM autenticação)

```bash
# Obter formulário
GET /api/public/forms/{public_uuid}

# Submeter respostas
POST /api/public/forms/{public_uuid}/submit
{
  "patient": {
    "name": "João Silva",
    "cpf": "12345678901",
    "sexo": "M",
    "nascimento": "15/03/1990"
  },
  "answersJson": "{\"q1\": \"Resposta 1\"}"
}
```

**Obter `public_uuid`:**
- Verificar logs do Spring Boot: `Generated Form Link: http://localhost:3000/forms/{uuid}`
- Ou consultar: `SELECT public_uuid FROM form_templates;`

⚠️ **Use `public_uuid`, NÃO o `id` da tabela!**

---

## 🔐 Segurança

### JWT Authentication

O sistema usa JWT com claims de multi-tenancy:
- `userId` - ID do usuário
- `tenantId` - ID da clínica (isolamento de dados)
- `role` - Role do usuário (DOCTOR, SECRETARY, SUPER_ADMIN, etc.)

### Discriminators JPA

As entidades de herança usam discriminadores explícitos:
- `User` → `@DiscriminatorValue("USER")`
- `Doctor` → `@DiscriminatorValue("DOCTOR")`
- `Secretary` → `@DiscriminatorValue("SECRETARY")`

### Jasypt Encryption

Campos sensíveis (`feegowApiToken`) são criptografados com Jasypt.

**Configurar em produção:**
```bash
gcloud run services update ctrls-forms-api \
  --region=southamerica-east1 \
  --update-env-vars="JASYPT_ENCRYPTOR_PASSWORD=sua-senha-secreta"
```

---

## 🧪 Testes

```bash
# Executar todos os testes
./mvnw test

# Testes específicos
./mvnw test -Dtest=SubmissionServiceTest

# Com cobertura
./mvnw test jacoco:report
```

---

## 🏗️ Arquitetura

### Clean Architecture

```
┌─────────────────────────────────────┐
│         Web Layer (REST)            │
│  - Controllers                      │
│  - DTOs Request/Response            │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│      Application Layer              │
│  - Use Cases / Services             │
│  - Business Logic                   │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│         Domain Layer                │
│  - Entities                         │
│  - Repository Interfaces            │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│      Infrastructure Layer           │
│  - JPA Implementations              │
│  - Security (JWT)                   │
│  - Multi-tenancy                    │
│  - External Integrations            │
└─────────────────────────────────────┘
```

### Multi-Tenancy

- **Tenant Isolation**: Filtro por `clinic_id` em queries
- **Context Propagation**: JWT contém `tenantId`
- **Security**: Validação de vínculo User-Clinic no banco
- **Audit**: Registro por tenant em `audit_logs`

### Stack Tecnológica

- **Framework**: Spring Boot 3.2+
- **Security**: Spring Security + JWT
- **Database**: PostgreSQL 15 + Flyway
- **Cache**: Redis (Memorystore GCP)
- **File Storage**: Cloudinary
- **Cloud**: Google Cloud Platform
  - Cloud Run (Serverless)
  - Cloud SQL (PostgreSQL)
  - Memorystore (Redis)
  - Artifact Registry

---

## 🐛 Troubleshooting

Para problemas comuns e soluções detalhadas, consulte:

**[TROUBLESHOOTING.md](TROUBLESHOOTING.md)**

Inclui:
- Reset de banco local e Cloud SQL
- Endpoints do formulário público
- Exemplos de requisições
- Erros comuns e soluções

---

## 📝 Licença

Este projeto é privado e proprietário.

---

## 🔄 Changelog

### v0.0.1-SNAPSHOT (Atual)
- ✅ Multi-tenancy implementado
- ✅ Autenticação JWT
- ✅ CRUD de Formulários e Submissions
- ✅ Deploy GCP Cloud Run
- ✅ Integrações: Cloudinary, Feegow
- ✅ Cache Redis
- ✅ Async Events
- ✅ Flyway Migrations (V1 + V2)
- ✅ JPA Discriminators corrigidos
- ✅ Jasypt encryption com fallback

---

**Desenvolvido com ❤️ para CTRLS**

