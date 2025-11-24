# Análise de API Design - Web Package
**Role:** API Design Specialist  
**Sistema:** Multi-tenant SaaS - Spring Boot 3 + OpenAPI/Swagger  
**Data da Análise:** 2025-11-24

---

## 🔴 PROBLEMAS CRÍTICOS IDENTIFICADOS

### 1. ❌ HTTP STATUS CODES INCORRETOS (CRÍTICO)

#### 1.1. AuthController - Login sem status code específico
**Arquivo:** `AuthController.java`
```java
@PostMapping("/login")
public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request)); // ❌ 200 OK - Deveria ser específico
}
```

**Problema:**
- Login retorna `200 OK`, mas não há diferenciação clara
- Não há `@ApiResponse` para documentar possíveis erros (401, 400)

**Correção:**
- Manter `200 OK` para login bem-sucedido (padrão REST)
- Adicionar `@ApiResponse` para 401 Unauthorized, 400 Bad Request

---

#### 1.2. PublicFormController - Submit retorna 202 ✅ (CORRETO)
**Arquivo:** `PublicFormController.java`
```java
@PostMapping("/{uuid}/submit")
public ResponseEntity<SubmissionResponse> submit(...) {
    return ResponseEntity.accepted().body(response); // ✅ 202 Accepted
}
```

**Status:** ✅ **CORRETO!** 202 Accepted é apropriado para processamento assíncrono.

---

#### 1.3. SubmissionController - Falta 404 específico
**Arquivo:** `SubmissionController.java`
```java
@GetMapping
public ResponseEntity<Page<SubmissionSummaryDTO>> findAll(...) {
    return ResponseEntity.ok(dtoPage); // ✅ 200 OK correto
}
```

**Problema:**
- Não há endpoint para GET individual (ex: `GET /api/submissions/{id}`)
- Se houver no futuro, precisa retornar `404 Not Found` quando não existir

---

### 2. ❌ VALIDAÇÕES DE INPUT INSUFICIENTES (ALTO)

#### 2.1. SubmissionRequest - Falta validação de CPF
**Arquivo:** `SubmissionRequest.java`
```java
public record Patient(
        @NotBlank String name,
        @NotBlank String cpf,  // ❌ Só verifica se não é vazio
        @NotBlank String sexo,
        @NotBlank String nascimento
) {}
```

**Problemas:**
1. **CPF:** Não valida formato (11 dígitos)
2. **sexo:** Não valida valores aceitos (M/F/Outro)
3. **nascimento:** Não valida formato de data (dd/MM/yyyy)
4. **answersJson:** Não valida se é JSON válido

**Correção:**
```java
@Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 dígitos")
String cpf,

@Pattern(regexp = "^(M|F|Outro)$", message = "Sexo deve ser M, F ou Outro")
String sexo,

@Pattern(regexp = "^\\d{2}/\\d{2}/\\d{4}$", message = "Data deve estar no formato dd/MM/yyyy")
String nascimento
```

---

#### 2.2. LoginRequest - Email validado ✅, mas falta mensagem customizada
**Arquivo:** `LoginRequest.java`
```java
public record LoginRequest(
        @Email String email,  // ✅ Valida email
        @NotBlank String password,  // ❌ Sem validação de tamanho mínimo
        @NotBlank String clinicId  // ❌ Deveria validar UUID
) {}
```

**Problemas:**
1. **password:** Sem validação de tamanho mínimo (segurança)
2. **clinicId:** Deveria validar se é UUID válido

**Correção:**
```java
@NotBlank(message = "Email é obrigatório")
@Email(message = "Email inválido")
String email,

@NotBlank(message = "Senha é obrigatória")
@Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
String password,

@NotBlank(message = "ID da clínica é obrigatório")
@Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", 
         message = "ID da clínica deve ser um UUID válido")
String clinicId
```

---

#### 2.3. PublicFormController - Falta @Valid no submit
**Arquivo:** `PublicFormController.java`
```java
@PostMapping("/{uuid}/submit")
public ResponseEntity<SubmissionResponse> submit(
        @PathVariable UUID uuid,
        @RequestBody SubmissionRequest request) {  // ❌ FALTA @Valid!
```

**Problema:** Sem `@Valid`, as validações do `SubmissionRequest` não são executadas!

**Correção:**
```java
@RequestBody @Valid SubmissionRequest request
```

---

### 3. ⚠️ DOCUMENTAÇÃO OPENAPI INSUFICIENTE (ALTO)

#### 3.1. Falta @ApiResponse em todos os endpoints
**Problema:** Nenhum controller documenta possíveis respostas de erro.

**Exemplo atual:**
```java
@GetMapping("/{uuid}")
@Operation(summary = "Obter template público de formulário")
public ResponseEntity<FormPublicViewDTO> getForm(@PathVariable UUID uuid) {
```

**O que está faltando:**
- Documentar 200 OK com exemplo
- Documentar 404 Not Found
- Documentar 400 Bad Request (UUID inválido)

**Correção:**
```java
@Operation(
    summary = "Obter template público de formulário",
    description = "Retorna o template de formulário com branding da clínica e médico"
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Formulário encontrado com sucesso",
        content = @Content(schema = @Schema(implementation = FormPublicViewDTO.class))
    ),
    @ApiResponse(
        responseCode = "404",
        description = "Formulário não encontrado",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    ),
    @ApiResponse(
        responseCode = "400",
        description = "UUID inválido",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
})
```

---

#### 3.2. Falta @Tag para agrupar endpoints
**Problema:** Swagger não agrupa endpoints por contexto.

**Correção:** Adicionar `@Tag` nos controllers:
```java
@Tag(name = "Formulários Públicos", description = "APIs públicas para acesso e submissão de formulários")
@RestController
@RequestMapping("/api/public/forms")
public class PublicFormController {
```

---

#### 3.3. Falta @Schema nas DTOs
**Problema:** Swagger não documenta os campos dos DTOs.

**Exemplo atual:**
```java
public record SubmissionResponse(UUID submissionId, SubmissionStatus status) {
}
```

**Correção:**
```java
@Schema(description = "Resposta da submissão de formulário")
public record SubmissionResponse(
    @Schema(description = "ID único da submissão", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID submissionId,
    
    @Schema(description = "Status atual do processamento", example = "PENDING")
    SubmissionStatus status
) {}
```

---

### 4. ⚠️ VAZAMENTO DE ENTIDADES (MÉDIO)

#### 4.1. SubmissionStatus exposto diretamente ✅ (ACEITÁVEL)
**Arquivo:** `SubmissionResponse.java`
```java
public record SubmissionResponse(UUID submissionId, SubmissionStatus status) {
```

**Análise:**
- `SubmissionStatus` é um **Enum de domínio** exposto na API
- ✅ **ACEITÁVEL** para enums simples (PENDING, PROCESSED, ERROR)
- ⚠️ **ATENÇÃO:** Se o enum mudar internamente, quebra o contrato da API

**Recomendação:**
- Manter por enquanto (simplicidade)
- **OU** criar um DTO enum específico:
```java
public enum SubmissionStatusDTO {
    PENDING, PROCESSING, COMPLETED, FAILED;
    
    public static SubmissionStatusDTO fromDomain(SubmissionStatus status) {
        return switch(status) {
            case PENDING -> PENDING;
            case PROCESSED -> COMPLETED;
            case ERROR -> FAILED;
        };
    }
}
```

---

#### 4.2. Todos os DTOs usam padrão correto ✅
**Análise:**
- ✅ `FormPublicViewDTO` - Converte de Entity para DTO
- ✅ `SubmissionSummaryDTO` - Usa método `fromEntity()`
- ✅ `SubmissionRequest` - Record puro (não é Entity)

**Status:** ✅ **CORRETO!** Nenhum vazamento de entidade detectado.

---

### 5. ❌ FALTA TRATAMENTO DE VALIDATION EXCEPTIONS (CRÍTICO)

**Arquivo:** `GlobalExceptionHandler.java`

**Problema:**
- Não há `@ExceptionHandler` para `MethodArgumentNotValidException`
- Quando `@Valid` falha, retorna erro genérico 400 sem detalhes
- Frontend não recebe informação sobre qual campo está inválido

**Correção:**
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setTitle("Erro de validação");
    
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(error -> 
        errors.put(error.getField(), error.getDefaultMessage())
    );
    
    problem.setProperty("errors", errors);
    return problem;
}
```

---

### 6. ⚠️ FALTA PAGINAÇÃO DOCUMENTADA

**Arquivo:** `SubmissionController.java`
```java
@GetMapping
public ResponseEntity<Page<SubmissionSummaryDTO>> findAll(
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
```

**Problema:**
- Swagger não documenta parâmetros de paginação (`page`, `size`, `sort`)
- Frontend precisa adivinhar como paginar

**Correção:**
```java
@Operation(
    summary = "Listar submissões com paginação",
    parameters = {
        @Parameter(name = "page", description = "Número da página (0-indexed)", example = "0"),
        @Parameter(name = "size", description = "Tamanho da página", example = "20"),
        @Parameter(name = "sort", description = "Ordenação (campo,direção)", example = "createdAt,desc")
    }
)
```

---

## ✅ PONTOS POSITIVOS ENCONTRADOS

1. ✅ **DTOs bem estruturados** - Uso correto de records
2. ✅ **202 Accepted** para processamento assíncrono
3. ✅ **ProblemDetail (RFC 7807)** para erros
4. ✅ **Validação básica** com `@Valid` no AuthController
5. ✅ **OpenAPI configurado** com JWT
6. ✅ **Nenhum vazamento de entidade** detectado
7. ✅ **GlobalExceptionHandler** centralizado

---

## 🛠️ IMPLEMENTAÇÕES NECESSÁRIAS

### Arquivos para Criar:
1. ✅ Validadores customizados (CPF, Data)
2. ✅ DTOs com @Schema completo
3. ✅ Controllers com @ApiResponse completo

### Arquivos para Modificar:
1. ✅ `SubmissionRequest.java` - Adicionar validações
2. ✅ `LoginRequest.java` - Adicionar validações e mensagens
3. ✅ `PublicFormController.java` - Adicionar @Valid, @ApiResponse, @Tag
4. ✅ `SubmissionController.java` - Adicionar @ApiResponse, @Tag, documentar paginação
5. ✅ `AuthController.java` - Adicionar @ApiResponse, @Tag
6. ✅ `GlobalExceptionHandler.java` - Adicionar handler de validação
7. ✅ Todos os DTOs - Adicionar @Schema

---

## 📋 CHECKLIST DE MELHORIAS

### 🔴 CRÍTICO (Implementar AGORA):
1. ✅ Adicionar `@Valid` no `PublicFormController.submit()`
2. ✅ Adicionar validações de formato em `SubmissionRequest.Patient`
3. ✅ Adicionar `@ExceptionHandler` para `MethodArgumentNotValidException`
4. ✅ Adicionar `@ApiResponse` em todos os endpoints

### ⚠️ ALTO (Sprint Atual):
5. ✅ Adicionar `@Tag` em todos os controllers
6. ✅ Adicionar `@Schema` em todos os DTOs
7. ✅ Documentar parâmetros de paginação
8. ✅ Adicionar validação de senha mínima em LoginRequest

### 🔄 MÉDIO (Backlog):
9. 🔄 Criar validador customizado de CPF (algoritmo de validação)
10. 🔄 Adicionar exemplos de request/response no Swagger
11. 🔄 Criar DTO para SubmissionStatus (desacoplar do domínio)
12. 🔄 Adicionar versionamento da API (v1, v2)

---

**Fim da Análise de API Design**  
*Gerado por: API Design Specialist*  
*Padrões: REST, OpenAPI 3.0, RFC 7807, Bean Validation*

