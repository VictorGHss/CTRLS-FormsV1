# Análise de Segurança - Infrastructure Package
**Role:** Application Security Engineer (AppSec)  
**Sistema:** Multi-tenant SaaS - Spring Boot 3 + Spring Security 6  
**Data da Análise:** 2025-11-24

---

## 🔴 VULNERABILIDADES CRÍTICAS ENCONTRADAS

### 1. ❌ VAZAMENTO DE INFORMAÇÕES SENSÍVEIS EM LOGS (CRÍTICO)
**Arquivos:** `JwtAuthenticationFilter.java`, `TenantContextFilter.java`

**Problema 1 - UUID de Usuário em Console:**
```java
System.out.println("🔑 [JwtFilter] Usuário autenticado via Token: " + userId);
System.out.println("🔍 [TenantFilter] Verificando Acesso: User=" + userId + " -> Clinic=" + clinicId);
```
**Risco:** 
- UUIDs de usuários e clínicas sendo expostos em logs de produção
- Atacantes podem mapear IDs reais do sistema
- Viola LGPD (Lei Geral de Proteção de Dados)

**Solução Recomendada:**
- **REMOVER** todos os `System.out.println` de produção
- Usar `logger.debug()` com dados sanitizados
- Implementar log masking para dados sensíveis

```java
// ✅ CORRETO:
private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

log.debug("Usuário autenticado via token"); // SEM ID
// Ou com masking:
log.debug("Usuário autenticado: {}", maskUuid(userId));

private String maskUuid(String uuid) {
    return uuid.substring(0, 8) + "****-****-****";
}
```

---

**Problema 2 - Mensagens de Erro Verbosas:**
```java
System.out.println("⚠️ [JwtFilter] Erro ao validar token: " + e.getMessage());
System.out.println("❌ [TenantFilter] UUID Inválido: " + e.getMessage());
```
**Risco:** 
- Atacantes obtêm informações sobre a estrutura interna (stack traces, detalhes do JWT)
- Facilita ataques de enumeração

**Solução:**
- Logar internamente com `logger.warn()`, mas NÃO retornar detalhes ao cliente
- Usar mensagens genéricas em responses

---

### 2. ❌ AUSÊNCIA DE TRATAMENTO DE EXCEÇÕES EM FILTROS (CRÍTICO)

**Arquivo:** `JwtAuthenticationFilter.java`
```java
@Override
protected void doFilterInternal(...) throws ServletException, IOException {
    // ...
    try {
        userId = jwtService.extractUsername(jwt);
        // ... lógica
    } catch (Exception e) {
        System.out.println("⚠️ [JwtFilter] Erro ao validar token: " + e.getMessage());
        // ❌ PROBLEMA: Não lança exceção, apenas segue!
    }
    filterChain.doFilter(request, response); // Continua mesmo com erro
}
```

**Problema:**
- Exceções são silenciosamente engolidas
- Requisições com tokens inválidos/corrompidos passam sem autenticação
- Comportamento imprevisível (pode retornar 403 ou 500 dependendo do próximo filtro)

**Solução Recomendada:**
- Criar um `FilterExceptionHandler` dedicado
- Retornar respostas JSON padronizadas (RFC 7807 - Problem Details)

---

**Arquivo:** `TenantContextFilter.java`
```java
try {
    UUID clinicId = UUID.fromString(clinicHeader);
    // ...
} catch (IllegalArgumentException e) {
    System.out.println("❌ [TenantFilter] UUID Inválido: " + e.getMessage());
    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID inválido");
    return; // ❌ PROBLEMA: Mensagem muito genérica, mas pelo menos retorna erro
}
```

**Problema:**
- `response.sendError()` retorna HTML por padrão, não JSON
- Inconsistente com o resto da API (que usa ProblemDetail)
- Não há log estruturado para auditoria

---

### 3. ⚠️ MEMORY LEAK DE ThreadLocal (ALTO RISCO)

**Arquivo:** `TenantContextFilter.java`
```java
@Override
protected void doFilterInternal(...) throws ServletException, IOException {
    // ... lógica que define o tenant
    TenantContextHolder.setTenantId(clinicId.toString());
    
    try {
        filterChain.doFilter(request, response);
    } finally {
        TenantContextHolder.clear(); // ✅ BOM! Mas há um problema...
    }
}
```

**Problema Parcial:**
- O `finally` está presente, mas **APENAS** se o fluxo chegar até o `filterChain.doFilter()`
- Se houver um `return` antes (ex: erro de validação), o ThreadLocal **NÃO É LIMPO**

**Exemplo de Cenário Problemático:**
```java
if (!clinicRepository.existsById(clinicId)) {
    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Clínica inválida");
    return; // ❌ SAIR AQUI = ThreadLocal não é limpo!
}
```

**Solução:**
- Envolver **TODA** a lógica em `try-finally`, incluindo validações:

```java
try {
    String clinicHeader = request.getHeader("X-Clinic-ID");
    
    if (clinicHeader == null || clinicHeader.isBlank()) {
        filterChain.doFilter(request, response);
        return;
    }
    
    // ... validações e setTenantId aqui
    
    filterChain.doFilter(request, response);
    
} finally {
    TenantContextHolder.clear(); // SEMPRE executa
}
```

---

### 4. ❌ ORDEM DE FILTROS INCORRETA (CRÍTICO)

**Arquivo:** `SecurityConfig.java`
```java
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
.addFilterAfter(tenantContextFilter, JwtAuthenticationFilter.class);
```

**Análise:**
- **Ordem Atual:** JWT → Tenant
- **Ordem Correta:** ✅ JWT deve vir ANTES do Tenant

**Status:** ✅ **CORRETO!** A ordem está adequada.

**Justificativa:**
1. **Primeiro (JWT):** Valida o token e autentica o usuário
2. **Segundo (Tenant):** Valida se o usuário tem acesso àquela clínica (tenant)

**⚠️ ATENÇÃO:** O problema está na **execução lógica** dentro do `TenantContextFilter`:
```java
if (!clinicRepository.existsById(clinicId)) {
    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Clínica inválida");
    return; // ❌ ThreadLocal não é limpo aqui!
}
```

---

### 5. ⚠️ FALTA DE RATE LIMITING NOS FILTROS (MÉDIO)

**Problema:**
- Não há proteção contra força bruta em tokens JWT
- Atacante pode tentar milhares de tokens inválidos sem bloqueio

**Solução Recomendada:**
- Implementar rate limiting no `JwtAuthenticationFilter` usando Redis ou Bucket4j
- Bloquear IPs com mais de X tentativas falhas em Y minutos

---

### 6. ✅ SECRETS MANAGEMENT - BEM IMPLEMENTADO (COM RESSALVAS)

**Arquivo:** `application.properties`
```properties
ctrls.security.jwt.secret=${JWT_SECRET:c2VncmVkby1tdWl0by1zZWd1cm8tcGFyYS10ZXN0ZXMtbG9jYWlzLWRvLXByb2pldG8=}
jasypt.encryptor.password=${JASYPT_ENCRYPTOR_PASSWORD:changeit}
```

**Análise:**
- ✅ Usa variáveis de ambiente `${JWT_SECRET}` e `${JASYPT_ENCRYPTOR_PASSWORD}`
- ✅ Não há hardcoded secrets no código Java
- ✅ `CtrlsProperties` carrega via `@ConfigurationProperties`

**⚠️ PROBLEMA:**
- Valores padrão (`:changeit`) são **EXTREMAMENTE FRACOS**
- Se alguém rodar em produção sem configurar as variáveis, o sistema fica vulnerável

**Solução:**
- Remover valores padrão ou usar placeholders que causem falha na inicialização:
```properties
ctrls.security.jwt.secret=${JWT_SECRET}
jasypt.encryptor.password=${JASYPT_ENCRYPTOR_PASSWORD}
```
- Adicionar validação no startup:
```java
@PostConstruct
public void validateSecrets() {
    if ("changeit".equals(ctrlsProperties.getSecurity().getJwt().getSecret())) {
        throw new IllegalStateException("JWT_SECRET não configurado em produção!");
    }
}
```

---

### 7. ⚠️ ENCRIPTAÇÃO DE DADOS - RISCO DE CAMPO ESTÁTICO

**Arquivo:** `EncryptedStringConverter.java`
```java
@Component
@Converter(autoApply = false)
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static StringEncryptor encryptor; // ❌ CAMPO ESTÁTICO

    @Autowired
    public void setEncryptor(StringEncryptor encryptor) {
        EncryptedStringConverter.encryptor = encryptor; // ❌ SETTER ESTÁTICO
    }
```

**Problema:**
- Uso de campo estático em um componente Spring
- Pode causar problemas em ambientes de teste com múltiplos contextos
- Não é thread-safe durante inicialização

**Solução Recomendada:**
- Remover `static` e usar injeção de dependência normal:
```java
@Component
@Converter(autoApply = false)
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final StringEncryptor encryptor;

    public EncryptedStringConverter(StringEncryptor encryptor) {
        this.encryptor = encryptor;
    }
    
    // ... resto do código
}
```

---

### 8. ❌ FALTA DE AUDITORIA DE SEGURANÇA

**Problema:**
- Não há logs de tentativas de acesso negado
- Não há registro de quem acessou qual tenant
- Impossível rastrear ataques ou abusos

**Solução:**
- Integrar com `AuditLog` existente:
```java
// No TenantContextFilter, após negar acesso:
auditLogRepository.save(AuditLog.builder()
    .actorEmail(extractEmailFromToken())
    .action("ACCESS_DENIED_TENANT")
    .scope(AuditScope.SECURITY)
    .resourceId(clinicId.toString())
    .details("Usuário sem vínculo com a clínica")
    .build());
```

---

## 🛡️ IMPLEMENTAÇÃO DE SOLUÇÕES

### Solução 1: FilterExceptionHandler

Criar novo arquivo: `FilterExceptionHandler.java`

```java
package br.dev.ctrls.api.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Captura exceções lançadas dentro de filtros e retorna respostas JSON padronizadas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FilterExceptionHandler extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.error("Erro capturado no filtro: {}", ex.getMessage(), ex);
            handleException(response, ex);
        }
    }

    private void handleException(HttpServletResponse response, Exception ex) throws IOException {
        ProblemDetail problem;
        
        if (ex instanceof SecurityException) {
            problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Acesso negado");
        } else if (ex instanceof IllegalArgumentException) {
            problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Requisição inválida");
        } else {
            problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno");
        }
        
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
```

**Registrar no SecurityConfig:**
```java
.addFilterBefore(filterExceptionHandler, JwtAuthenticationFilter.class)
```

---

### Solução 2: Refatorar JwtAuthenticationFilter

```java
package br.dev.ctrls.api.infrastructure.security;

import br.dev.ctrls.api.application.service.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            String userId = jwtService.extractUsername(jwt);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                if (jwtService.isTokenValid(jwt)) {
                    List<String> roles = jwtService.extractRoles(jwt);
                    var authorities = roles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .toList();

                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("Token JWT validado com sucesso"); // SEM IDs
                } else {
                    log.warn("Token JWT inválido ou expirado"); // Log genérico
                }
            }
        } catch (Exception e) {
            log.error("Erro ao processar token JWT", e); // Log interno
            // NÃO continua! Deixa sem autenticação para retornar 401/403
        }

        filterChain.doFilter(request, response);
    }
}
```

---

### Solução 3: Refatorar TenantContextFilter (Thread-Safe)

```java
package br.dev.ctrls.api.infrastructure.security;

import br.dev.ctrls.api.domain.clinic.repository.ClinicRepository;
import br.dev.ctrls.api.domain.user.repository.DoctorRepository;
import br.dev.ctrls.api.tenant.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String clinicHeader = request.getHeader("X-Clinic-ID");

            if (clinicHeader == null || clinicHeader.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                validateTenantAccess(request, response, clinicHeader, authentication);
            }

            filterChain.doFilter(request, response);
            
        } finally {
            TenantContextHolder.clear(); // ✅ SEMPRE limpa, mesmo com erro
        }
    }

    private void validateTenantAccess(HttpServletRequest request, 
                                      HttpServletResponse response,
                                      String clinicHeader, 
                                      Authentication authentication) throws IOException {
        try {
            UUID clinicId = UUID.fromString(clinicHeader);
            String userIdStr = authentication.getName();
            UUID userId = UUID.fromString(userIdStr);

            log.debug("Validando acesso ao tenant"); // SEM IDs

            if (!clinicRepository.existsById(clinicId)) {
                log.warn("Tentativa de acesso a clínica inexistente");
                sendErrorResponse(response, HttpStatus.BAD_REQUEST, "Clínica inválida");
                return;
            }

            boolean isDoctorLinked = doctorRepository.existsByIdAndClinicsId(userId, clinicId);
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().contains("ADMIN"));

            if (!isDoctorLinked && !isAdmin) {
                log.warn("Acesso negado ao tenant");
                sendErrorResponse(response, HttpStatus.FORBIDDEN, "Acesso negado a este ambiente");
                return;
            }

            TenantContextHolder.setTenantId(clinicId.toString());
            log.debug("Acesso ao tenant validado com sucesso");

        } catch (IllegalArgumentException e) {
            log.warn("UUID inválido no header X-Clinic-ID");
            sendErrorResponse(response, HttpStatus.BAD_REQUEST, "ID inválido");
        }
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) 
            throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
```

---

### Solução 4: Validação de Secrets no Startup

Criar arquivo: `SecurityPropertiesValidator.java`

```java
package br.dev.ctrls.api.infrastructure.config;

import br.dev.ctrls.api.infrastructure.config.props.CtrlsProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Base64;

@Slf4j
@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class SecurityPropertiesValidator {

    private final CtrlsProperties ctrlsProperties;

    @PostConstruct
    public void validateSecurityProperties() {
        String jwtSecret = ctrlsProperties.getSecurity().getJwt().getSecret();
        
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET não está configurado!");
        }
        
        // Valida se é o valor padrão de desenvolvimento
        String defaultSecret = "c2VncmVkby1tdWl0by1zZWd1cm8tcGFyYS10ZXN0ZXMtbG9jYWlzLWRvLXByb2pldG8=";
        if (jwtSecret.equals(defaultSecret)) {
            log.warn("⚠️ JWT_SECRET usando valor padrão de desenvolvimento! NÃO USE EM PRODUÇÃO!");
        }
        
        // Valida tamanho mínimo (256 bits = 32 bytes)
        try {
            byte[] decodedKey = Base64.getDecoder().decode(jwtSecret);
            if (decodedKey.length < 32) {
                throw new IllegalStateException("JWT_SECRET muito curto! Mínimo: 256 bits (32 bytes)");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT_SECRET não é uma string Base64 válida!");
        }
        
        log.info("✅ Configurações de segurança validadas com sucesso");
    }
}
```

---

## 📋 RESUMO DE AÇÕES PRIORITÁRIAS

### 🔴 CRÍTICO (Implementar IMEDIATAMENTE):

1. ✅ **Remover todos os `System.out.println`** e substituir por `logger.debug()`
2. ✅ **Implementar `FilterExceptionHandler`** para capturar exceções em filtros
3. ✅ **Corrigir limpeza de ThreadLocal** no `TenantContextFilter` (mover `clear()` para `finally` abrangente)
4. ✅ **Remover valores padrão fracos** de `JWT_SECRET` e `JASYPT_ENCRYPTOR_PASSWORD`
5. ✅ **Implementar validação de secrets no startup** (`SecurityPropertiesValidator`)

### ⚠️ ALTO (Implementar em Sprint Atual):

6. ⚠️ **Adicionar auditoria de segurança** (logs estruturados de acesso negado)
7. ⚠️ **Refatorar `EncryptedStringConverter`** para remover campo estático
8. ⚠️ **Implementar rate limiting** no `JwtAuthenticationFilter` (Bucket4j + Redis)
9. ⚠️ **Adicionar testes de segurança** para validar ordem de filtros

### 🔄 MÉDIO (Backlog):

10. 🔄 **Implementar JWT refresh tokens** (evitar tokens de longa duração)
11. 🔄 **Adicionar JWT blacklist** (invalidar tokens ao fazer logout)
12. 🔄 **Implementar IP whitelisting** para endpoints de admin
13. 🔄 **Adicionar headers de segurança** (X-Frame-Options, CSP, etc.)

---

## 🔒 CHECKLIST DE SEGURANÇA

### Autenticação e Autorização:
- ✅ JWT implementado corretamente
- ✅ Ordem de filtros correta (JWT → Tenant)
- ❌ Falta rate limiting
- ❌ Falta auditoria de tentativas falhas
- ❌ Tokens de longa duração (sem refresh)

### Gerenciamento de Secrets:
- ✅ Secrets em variáveis de ambiente
- ⚠️ Valores padrão fracos presentes
- ❌ Falta validação no startup
- ✅ Encriptação de dados sensíveis (Jasypt)

### Tratamento de Erros:
- ❌ Exceções em filtros não tratadas adequadamente
- ❌ Mensagens de erro muito verbosas
- ✅ GlobalExceptionHandler para controllers
- ❌ Falta FilterExceptionHandler

### Thread Safety:
- ⚠️ ThreadLocal com limpeza parcial (risco de leak)
- ⚠️ Campo estático em `EncryptedStringConverter`

### Logging e Auditoria:
- ❌ Logs expostos no console (System.out)
- ❌ IDs sensíveis em logs
- ❌ Falta auditoria de acessos negados
- ✅ Entidade `AuditLog` existe (precisa ser integrada)

---

## 🛠️ ARQUIVOS PARA CRIAR/MODIFICAR

### Criar:
1. `FilterExceptionHandler.java`
2. `SecurityPropertiesValidator.java`

### Modificar:
1. `JwtAuthenticationFilter.java` - Remover System.out, melhorar tratamento de erros
2. `TenantContextFilter.java` - Corrigir finally, remover System.out, adicionar auditoria
3. `SecurityConfig.java` - Adicionar FilterExceptionHandler
4. `application.properties` - Remover valores padrão de secrets
5. `EncryptedStringConverter.java` - Remover campo estático

---

**Fim da Análise de Segurança**  
*Gerado por: Application Security Engineer (AppSec)*  
*Framework: Spring Security 6 + JWT + Multi-Tenancy*

