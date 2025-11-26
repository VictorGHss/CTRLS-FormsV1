package br.dev.ctrls.api.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter para capturar exceções dentro da Security Filter Chain.
 *
 * PROBLEMA: Por padrão, exceções lançadas dentro de filtros retornam erro 500
 * genérico em HTML, sem detalhes úteis para o cliente.
 *
 * SOLUÇÃO: Este filtro intercepta exceções e retorna respostas JSON padronizadas
 * seguindo RFC 7807 (ProblemDetail).
 *
 * ORDEM: Executado PRIMEIRO na cadeia (@Order(Ordered.HIGHEST_PRECEDENCE))
 * para garantir que envolve todos os outros filtros em try-catch.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
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

        } catch (AuthenticationException ex) {
            log.warn("🔒 [FilterExceptionHandler] Falha de autenticação: {} - URI: {}",
                    ex.getMessage(), request.getRequestURI());
            handleAuthenticationException(response, ex);

        } catch (AccessDeniedException ex) {
            log.warn("🚫 [FilterExceptionHandler] Acesso negado: {} - URI: {}",
                    ex.getMessage(), request.getRequestURI());
            handleAccessDeniedException(response, ex);

        } catch (IllegalArgumentException ex) {
            log.warn("⚠️ [FilterExceptionHandler] Argumento inválido: {} - URI: {}",
                    ex.getMessage(), request.getRequestURI());
            handleBadRequestException(response, ex);

        } catch (Exception ex) {
            log.error("❌ [FilterExceptionHandler] Erro não tratado na cadeia de filtros - URI: {}",
                    request.getRequestURI(), ex);
            handleGenericException(response, ex);
        }
    }

    /**
     * Trata erros de autenticação (token inválido, expirado, etc).
     * Retorna 401 Unauthorized com detalhes em JSON.
     */
    private void handleAuthenticationException(HttpServletResponse response,
                                               AuthenticationException ex) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Autenticação falhou. Token JWT inválido, expirado ou ausente."
        );
        problem.setTitle("Não Autenticado");

        writeErrorResponse(response, HttpStatus.UNAUTHORIZED, problem);
    }

    /**
     * Trata erros de autorização (usuário não tem permissão).
     * Retorna 403 Forbidden com detalhes em JSON.
     */
    private void handleAccessDeniedException(HttpServletResponse response,
                                            AccessDeniedException ex) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "Acesso negado. Você não tem permissão para acessar este recurso."
        );
        problem.setTitle("Acesso Negado");

        writeErrorResponse(response, HttpStatus.FORBIDDEN, problem);
    }

    /**
     * Trata erros de validação (UUID inválido, etc).
     * Retorna 400 Bad Request com detalhes em JSON.
     */
    private void handleBadRequestException(HttpServletResponse response,
                                          IllegalArgumentException ex) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problem.setTitle("Requisição Inválida");

        writeErrorResponse(response, HttpStatus.BAD_REQUEST, problem);
    }

    /**
     * Trata erros genéricos não esperados.
     * Retorna 500 Internal Server Error SEM detalhes sensíveis.
     */
    private void handleGenericException(HttpServletResponse response,
                                       Exception ex) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Ocorreu um erro inesperado. Por favor, tente novamente mais tarde."
        );
        problem.setTitle("Erro Interno");

        // NÃO expor stack trace ou detalhes sensíveis em produção
        writeErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, problem);
    }

    /**
     * Escreve a resposta de erro no formato JSON (RFC 7807).
     */
    private void writeErrorResponse(HttpServletResponse response,
                                   HttpStatus status,
                                   ProblemDetail problem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), problem);
    }
}

