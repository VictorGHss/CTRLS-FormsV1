package br.dev.ctrls.api.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuração de execução assíncrona para processamento enterprise-grade.
 *
 * Thread Pool Strategy:
 * - Core Pool: 5 threads sempre ativas para processar submissões
 * - Max Pool: até 10 threads em picos de demanda
 * - Queue: até 100 tarefas aguardando processamento
 *
 * Quando a fila está cheia, o RejectedExecutionHandler registra erro crítico
 * e a tarefa é descartada (considere implementar DLQ em produção).
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Thread pool dedicado para processamento assíncrono de submissões.
     *
     * Configuração otimizada para ambiente de produção:
     * - Core Pool Size: 5 threads (sempre ativas)
     * - Max Pool Size: 10 threads (expandir sob demanda)
     * - Queue Capacity: 100 tarefas pendentes
     * - Keep Alive: threads extras sobrevivem 60s após ociosidade
     * - Shutdown Gracioso: aguarda até 60s para finalizar tarefas
     */
    @Bean(name = "submissionTaskExecutor")
    public Executor submissionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Pool configuration
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);

        // Thread naming for debugging
        executor.setThreadNamePrefix("submission-async-");

        // Graceful shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        // Handler para tarefas rejeitadas (quando queue está cheia)
        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            log.error("❌ [AsyncConfig] Tarefa de submissão REJEITADA! " +
                     "Queue cheia ({}/{}). Considere aumentar capacidade ou implementar DLQ.",
                     threadPoolExecutor.getQueue().size(),
                     threadPoolExecutor.getQueue().remainingCapacity());

            // Em produção: enviar para Dead Letter Queue (DLQ) aqui
            // Exemplo: dlqService.send(runnable);
        });

        executor.initialize();

        log.info("✅ [AsyncConfig] Thread pool configurado: core={}, max={}, queue={}",
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getQueueCapacity());

        return executor;
    }

    /**
     * Handler global para exceções não tratadas em métodos @Async.
     * Evita que exceções sejam silenciosamente engolidas.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("❌ [AsyncConfig] Exceção não tratada em método async: {}.{}",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(),
                    throwable);

            // Em produção: enviar alerta para monitoramento (Sentry, DataDog, etc)
            // Exemplo: alertService.sendCriticalAlert("Async execution failed", throwable);
        };
    }
}

