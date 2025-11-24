# CTRLS Forms API

## Overview
CTRLS Forms API e uma aplicacao Spring Boot voltada para gestao de formularios clinicos, submetimentos de pacientes e integracoes com terceiros. Este documento descreve a estrutura de pacotes e classes principais para facilitar a navegacao e manutencao do codigo.

## Estrutura do codigo-fonte

### `src/main/java/br/dev/ctrls/api`
- **ApiApplication**: classe principal que inicializa o Spring Boot.

#### application
- **service.auth**: `AuthService`, `JwtService` e DTOs (`LoginRequest`, `LoginResponse`) para autenticacao.
- **service.document**: `PdfService` para geracao de documentos.
- **service.submission**: `SubmissionService` com regras de negocio das submisses.

#### client
- **feegow**: cliente HTTP (`FeegowClient`) e DTOs (`FeegowPatientRequest`, `FeegowPatientResponse`, `UploadFileRequest`).

#### config
- **FeignConfig**: configuracoes globais de clientes Feign.

#### domain
- **audit**: `AuditLog`, `AuditScope` para registro de auditoria.
- **clinic**: `Clinic`, `ClinicTheme` e `ClinicRepository` para entidades de clinica.
- **common**: `BaseEntity` com comportamentos comuns a entidades.
- **form**: `FormTemplate` e `FormTemplateRepository` para templates de formulario.
- **secretary**: `Secretary` e `SecretaryRepository`.
- **submission**: `Submission`, `SubmissionStatus`, `SubmissionRepository`.
- **user**: `User`, `UserRole`, `Doctor`, `DoctorRepository`, `UserRepository`.

#### infrastructure
- **bootstrap**: `DevDataSeeder` popula dados de desenvolvimento.
- **config**: `RedisCacheConfig` e propriedades (`BrevoProperties`, `CloudinaryProperties`, `CtrlsProperties`, `IntegrationProperties`, `RateLimiterProperties`, `ThirdPartyProperties`).
- **persistence.converter**: `EncryptedStringConverter` para encriptar campos.
- **security**: `JwtAuthenticationFilter`, `SecurityConfig`, `TenantContextFilter`.

#### tenant
- `TenantContextHolder` define o contexto do tenant.

#### web
- **config**: `OpenApiConfig` com a documentacao Swagger.
- **dto**: `FormPublicViewDTO`, `SubmissionRequest`, `SubmissionResponse`, `SubmissionSummaryDTO`.
- **rest**: `AuthController`, `GlobalExceptionHandler`, `PublicFormController`, `SubmissionController`.

### `src/test/java/br/dev/ctrls/api`
- **ApiApplicationTests**: smoke tests da aplicacao.
- **BaseIntegrationTest**: base para testes de integracao com contexto Spring.
- **application.service.submission**: `SubmissionServiceTest` cobre regras de submissao.

## Estrutura de recursos
- **`src/main/resources/application.properties`**: configuracoes padrao.
- **`src/main/resources/db/migration/V1__Initial_Schema.sql`**: migracao Flyway inicial.
- **`src/test/resources/application.properties`**: propriedades de teste.

## Execucao
```bash
./mvnw spring-boot:run
```
Use perfis (`-Dspring.profiles.active=`) conforme necessario.
