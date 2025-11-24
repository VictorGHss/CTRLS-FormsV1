# Análise de Persistência - Domain Package
**Role:** Senior Java Persistence Architect  
**Sistema:** Multi-tenant SaaS - Spring Boot 3 + PostgreSQL 16  
**Data da Análise:** 2025-11-24

---

## 1. ⚠️ PROBLEMAS CRÍTICOS DE PERFORMANCE (N+1 Queries)

### 1.1. Clinic.doctors - Risco Alto de N+1
**Arquivo:** `Clinic.java`
```java
@ManyToMany(mappedBy = "clinics", fetch = FetchType.LAZY)
private Set<Doctor> doctors = new HashSet<>();
```
**Problema:** Se houver endpoints que listam clínicas e acessam os médicos (ex: `/clinics/{id}/doctors`), cada acesso a `clinic.getDoctors()` dispara uma query adicional.

**Solução Recomendada:**
- Adicionar `@EntityGraph` nos métodos do `ClinicRepository` que precisam carregar médicos
- Ou criar uma query JPQL com `JOIN FETCH`
```java
@EntityGraph(attributePaths = {"doctors"})
Optional<Clinic> findWithDoctorsById(UUID id);
```

---

### 1.2. Doctor.clinics e Doctor.secretaries - Risco Alto de N+1
**Arquivo:** `Doctor.java`
```java
@ManyToMany(fetch = FetchType.LAZY)
private Set<Clinic> clinics = new HashSet<>();

@ManyToMany(fetch = FetchType.LAZY)
private Set<Secretary> secretaries = new HashSet<>();
```
**Problema:** Ao listar médicos e iterar sobre suas clínicas/secretárias, cada iteração dispara queries extras.

**Solução Recomendada:**
- Criar métodos no `DoctorRepository` com `@EntityGraph` para cenários específicos:
```java
@EntityGraph(attributePaths = {"clinics", "secretaries"})
List<Doctor> findAllWithRelations();

@EntityGraph(attributePaths = {"clinics"})
Optional<Doctor> findWithClinicsById(UUID id);
```

---

### 1.3. Secretary.doctors - Risco Médio de N+1
**Arquivo:** `Secretary.java`
```java
@ManyToMany(mappedBy = "secretaries", fetch = FetchType.LAZY)
private Set<Doctor> doctors = new HashSet<>();
```
**Problema:** Similar ao caso anterior.

**Solução Recomendada:**
- Adicionar `@EntityGraph` no `SecretaryRepository` quando necessário
- Considerar criar uma query personalizada para listar secretárias com médicos

---

### 1.4. FormTemplate.clinic e FormTemplate.doctor - ✅ BEM IMPLEMENTADO
**Arquivo:** `FormTemplate.java` + `FormTemplateRepository.java`
```java
@EntityGraph(attributePaths = {"clinic", "doctor"})
Optional<FormTemplate> findByPublicUuid(UUID publicUuid);
```
**Status:** ✅ Correto! O `@EntityGraph` já está sendo usado adequadamente para evitar N+1.

---

### 1.5. Submission.template - ✅ BEM IMPLEMENTADO
**Arquivo:** `SubmissionRepository.java`
```java
@EntityGraph(attributePaths = {"template", "template.clinic", "template.doctor"})
Page<Submission> searchWithFilters(...);
```
**Status:** ✅ Correto! O grafo de entidades está carregando a hierarquia completa.

---

## 2. ⚠️ USO INCORRETO DE LOMBOK

### 2.1. Clinic - Falta @EqualsAndHashCode Explícito
**Arquivo:** `Clinic.java`
```java
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Clinic extends BaseEntity {
    // ... campos mutáveis como Set<Doctor> doctors
}
```
**Problema:** 
- Herda `@EqualsAndHashCode(of = "id")` do `BaseEntity`, mas tem uma coleção mutável (`doctors`)
- Se essa coleção for acessada no `hashCode()` (caso a herança mude), pode causar problemas

**Solução Recomendada:**
- Adicionar explicitamente `@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)` e marcar apenas `id`:
```java
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode.Include
private UUID id; // se for sobrescrever, ou apenas confiar no BaseEntity
```
**OU** manter como está, mas documentar claramente que o `BaseEntity` já trata isso.

---

### 2.2. User - Falta @EqualsAndHashCode Explícito
**Arquivo:** `User.java`
```java
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class User extends BaseEntity {
```
**Problema:** Mesma situação da `Clinic` - herda do `BaseEntity`, mas não documenta claramente.

**Solução Recomendada:**
- Adicionar `@EqualsAndHashCode(callSuper = true)` para deixar explícito que usa o `id` do pai.

---

### 2.3. Doctor e Secretary - Herdam de User
**Arquivos:** `Doctor.java`, `Secretary.java`
**Status:** Herdam o comportamento de `User`, que herda de `BaseEntity`.

**Solução Recomendada:**
- Garantir que `Doctor` e `Secretary` também tenham `@EqualsAndHashCode(callSuper = true)` para manter a consistência.

---

### 2.4. ⚠️ CRÍTICO: Uso de @AllArgsConstructor em User
**Arquivo:** `User.java`
```java
@AllArgsConstructor
```
**Problema:** 
- Lombok gera um construtor com TODOS os campos, incluindo os herdados de `BaseEntity` (id, createdAt, updatedAt)
- Isso pode levar a inconsistências, pois esses campos devem ser gerenciados pelo JPA

**Solução Recomendada:**
- **REMOVER** `@AllArgsConstructor`
- Se necessário, criar construtores específicos manualmente ou usar `@SuperBuilder`

---

### 2.5. FormTemplate e Submission - Uso de @AllArgsConstructor
**Arquivos:** `FormTemplate.java`, `Submission.java`
**Problema:** Mesma situação do `User`.

**Solução Recomendada:**
- **REMOVER** `@AllArgsConstructor` de entidades JPA
- Usar apenas `@SuperBuilder` e `@NoArgsConstructor`

---

## 3. 🗄️ ÍNDICES DE BANCO DE DADOS

### 3.1. Índices Ausentes - CRÍTICO

#### 3.1.1. Submissions: Foreign Keys sem Índice
**Tabela:** `submissions`
**Colunas sem índice:**
- `form_template_id` (usada em JOINs frequentes)
- `patient_cpf` (usada em buscas - ver `FeegowClient.listPatients`)
- `status` (usada em filtros - ver `SubmissionRepository.searchWithFilters`)

**SQL para adicionar:**
```sql
CREATE INDEX idx_submissions_form_template_id ON submissions(form_template_id);
CREATE INDEX idx_submissions_patient_cpf ON submissions(patient_cpf);
CREATE INDEX idx_submissions_status ON submissions(status);
CREATE INDEX idx_submissions_created_at ON submissions(created_at); -- para ordenação
```

---

#### 3.1.2. Form Templates: Foreign Keys sem Índice
**Tabela:** `form_templates`
**Colunas sem índice:**
- `clinic_id` (FK, usada em filtros multi-tenant)
- `doctor_id` (FK, usada em queries)
- `active` (usada em filtros)

**SQL para adicionar:**
```sql
CREATE INDEX idx_form_templates_clinic_id ON form_templates(clinic_id);
CREATE INDEX idx_form_templates_doctor_id ON form_templates(doctor_id);
CREATE INDEX idx_form_templates_active ON form_templates(active);
```

---

#### 3.1.3. Users: Email já tem UNIQUE (índice implícito) ✅
**Tabela:** `users`
**Status:** A constraint `UNIQUE` em `email` já cria um índice automaticamente.

---

#### 3.1.4. Doctor_Clinic e Doctor_Secretary: Índices Invertidos
**Tabelas:** `doctor_clinic`, `doctor_secretary`
**Problema:** PKs compostas existem, mas queries podem filtrar pela coluna da direita.

**SQL para adicionar:**
```sql
CREATE INDEX idx_doctor_clinic_clinic_id ON doctor_clinic(clinic_id);
CREATE INDEX idx_doctor_secretary_secretary_id ON doctor_secretary(secretary_id);
```

---

#### 3.1.5. Audit Logs: Falta Índice em Campos de Busca
**Tabela:** `audit_logs`
**Colunas sem índice:**
- `actor_email` (usada em queries de auditoria por usuário)
- `scope` (usada em filtros)
- `created_at` (usada em ordenação e range queries)

**SQL para adicionar:**
```sql
CREATE INDEX idx_audit_logs_actor_email ON audit_logs(actor_email);
CREATE INDEX idx_audit_logs_scope ON audit_logs(scope);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
```

---

### 3.2. Índice Composto para Multi-Tenancy
**Problema:** Queries multi-tenant frequentemente filtram por `clinic_id` + outro campo.

**Solução Recomendada:**
```sql
-- Índice composto para queries tipo: WHERE clinic_id = ? AND status = ?
CREATE INDEX idx_submissions_clinic_status ON submissions(
    (SELECT clinic_id FROM form_templates WHERE id = form_template_id), 
    status
);

-- Alternativa: desnormalizar clinic_id em submissions
ALTER TABLE submissions ADD COLUMN clinic_id UUID;
CREATE INDEX idx_submissions_clinic_id ON submissions(clinic_id);
```

---

## 4. 📊 MAPEAMENTO JSONB - POSTGRESQL

### 4.1. FormTemplate.schemaJson - ✅ CORRETO
**Arquivo:** `FormTemplate.java`
```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "schema_json", nullable = false, columnDefinition = "jsonb")
private String schemaJson;
```
**Status:** ✅ Correto! Usa `@JdbcTypeCode(SqlTypes.JSON)` do Hibernate 6+ e `columnDefinition = "jsonb"`.

---

### 4.2. Submission.answersJson - ✅ CORRETO
**Arquivo:** `Submission.java`
```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "answers_json", nullable = false, columnDefinition = "jsonb")
private String answersJson;
```
**Status:** ✅ Correto!

---

### 4.3. Sugestão de Melhoria: Queries JSONB
**Problema:** Atualmente os campos JSONB são tratados como `String`, o que limita queries avançadas.

**Solução Recomendada (OPCIONAL):**
- Criar índices GIN para buscas dentro do JSONB:
```sql
CREATE INDEX idx_form_templates_schema_json ON form_templates USING GIN (schema_json);
CREATE INDEX idx_submissions_answers_json ON submissions USING GIN (answers_json);
```
- Adicionar métodos no repositório para queries JSONB:
```java
@Query(value = "SELECT * FROM submissions WHERE answers_json->>'symptom' = :symptom", 
       nativeQuery = true)
List<Submission> findByJsonField(@Param("symptom") String symptom);
```

---

## 5. 🔒 SEGURANÇA E ENCRIPTAÇÃO

### 5.1. EncryptedStringConverter - ✅ BEM IMPLEMENTADO
**Arquivo:** `Clinic.java`
```java
@Convert(converter = EncryptedStringConverter.class)
@Column(name = "feegow_api_token", nullable = false, length = 512)
private String feegowApiToken;
```
**Status:** ✅ Correto! Token sensível está sendo encriptado.

**Sugestão Adicional:**
- Verificar se o `EncryptedStringConverter` usa chaves rotacionáveis (não verificado, pois está em `infrastructure.persistence.converter`)
- Considerar usar AWS Secrets Manager ou Vault para tokens de API

---

## 6. 📋 VALIDAÇÕES E CONSTRAINTS

### 6.1. CPF em Submission - Falta Validação de Formato
**Arquivo:** `Submission.java`
```java
@NotBlank
@Column(name = "patient_cpf", nullable = false, length = 11)
private String patientCpf;
```
**Problema:** Aceita qualquer string com até 11 caracteres.

**Solução Recomendada:**
- Adicionar validação de formato:
```java
@Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 dígitos")
@Column(name = "patient_cpf", nullable = false, length = 11)
private String patientCpf;
```
- Criar índice para buscas por CPF (já mencionado na seção 3.1.1)

---

### 6.2. CNPJ em Clinic - ✅ VALIDAÇÃO CORRETA
**Arquivo:** `Clinic.java`
```java
@Pattern(regexp = "\\d{14}")
@Column(nullable = false, unique = true, length = 14)
private String cnpj;
```
**Status:** ✅ Correto!

---

## 7. 🚀 OTIMIZAÇÕES ADICIONAIS

### 7.1. Adicionar @Cacheable em Queries Frequentes
**Repositórios:** `UserRepository`, `ClinicRepository`
**Solução:**
```java
@Cacheable("users")
Optional<User> findByEmail(String email);

@Cacheable("clinics")
Optional<Clinic> findByLinkUuid(UUID linkUuid);
```
**Configuração:** Adicionar Spring Cache (Redis recomendado para multi-tenant).

---

### 7.2. Paginação em Relacionamentos ManyToMany
**Problema:** `Doctor.clinics` e `Doctor.secretaries` carregam TODOS os registros.

**Solução (se houver muitos registros):**
- Criar queries paginadas:
```java
@Query("SELECT c FROM Clinic c JOIN c.doctors d WHERE d.id = :doctorId")
Page<Clinic> findClinicsByDoctorId(@Param("doctorId") UUID doctorId, Pageable pageable);
```

---

### 7.3. Read-Only Transactions para Queries
**Arquivo:** `SubmissionService.java` (e outros serviços de leitura)
**Solução:**
```java
@Transactional(readOnly = true)
public SubmissionResponse getSubmission(UUID id) {
    // ...
}
```
**Benefício:** Hibernate otimiza a sessão para leitura (não faz dirty checking).

---

## 8. 📝 RESUMO DE AÇÕES PRIORITÁRIAS

### CRÍTICO (Implementar AGORA):
1. ✅ **Remover `@AllArgsConstructor`** de `User`, `FormTemplate` e `Submission`
2. ✅ **Adicionar índices nas FKs:** `submissions.form_template_id`, `form_templates.clinic_id`, `form_templates.doctor_id`
3. ✅ **Adicionar índice em `submissions.patient_cpf`** (busca frequente)
4. ✅ **Adicionar validação `@Pattern` no CPF de `Submission`**
5. ✅ **Adicionar `@EntityGraph` nos métodos de `DoctorRepository` e `ClinicRepository`**

### ALTO (Implementar em Sprint Atual):
6. ⚠️ **Adicionar índices em `submissions.status` e `audit_logs.actor_email`**
7. ⚠️ **Adicionar `@EqualsAndHashCode(callSuper = true)` em `User`, `Doctor`, `Secretary`**
8. ⚠️ **Criar índices GIN nos campos JSONB** (se houver queries complexas)

### MÉDIO (Backlog):
9. 🔄 **Implementar cache com Redis** para `findByEmail` e `findByLinkUuid`
10. 🔄 **Adicionar `@Transactional(readOnly = true)` em métodos de leitura**
11. 🔄 **Considerar desnormalização de `clinic_id` em `submissions`** (para índice composto multi-tenant)

---

## 9. 🛠️ MIGRATION FLYWAY SUGERIDA

Criar arquivo: `V2__Add_Performance_Indexes.sql`

```sql
-- Índices para Foreign Keys
CREATE INDEX idx_submissions_form_template_id ON submissions(form_template_id);
CREATE INDEX idx_form_templates_clinic_id ON form_templates(clinic_id);
CREATE INDEX idx_form_templates_doctor_id ON form_templates(doctor_id);

-- Índices para Campos de Busca
CREATE INDEX idx_submissions_patient_cpf ON submissions(patient_cpf);
CREATE INDEX idx_submissions_status ON submissions(status);
CREATE INDEX idx_submissions_created_at ON submissions(created_at);

-- Índices para Form Templates
CREATE INDEX idx_form_templates_active ON form_templates(active);

-- Índices Invertidos para Tabelas de Junção
CREATE INDEX idx_doctor_clinic_clinic_id ON doctor_clinic(clinic_id);
CREATE INDEX idx_doctor_secretary_secretary_id ON doctor_secretary(secretary_id);

-- Índices para Audit Logs
CREATE INDEX idx_audit_logs_actor_email ON audit_logs(actor_email);
CREATE INDEX idx_audit_logs_scope ON audit_logs(scope);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- Índices GIN para JSONB (OPCIONAL)
CREATE INDEX idx_form_templates_schema_json ON form_templates USING GIN (schema_json);
CREATE INDEX idx_submissions_answers_json ON submissions USING GIN (answers_json);
```

---

**Fim da Análise**  
*Gerado por: Senior Java Persistence Architect*  
*Framework: Spring Boot 3 + Hibernate 6 + PostgreSQL 16*

