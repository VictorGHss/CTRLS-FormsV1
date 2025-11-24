package br.dev.ctrls.api.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuração de execução assíncrona para processamento em background.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool dedicado para processamento de submissões.
     * Core Pool: 5 threads sempre ativas
     * Max Pool: até 10 threads em picos de demanda
     * Queue: até 100 tarefas aguardando
     */
    @Bean(name = "submissionTaskExecutor")
    public Executor submissionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("submission-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        // Handler para tarefas rejeitadas (quando queue está cheia)
        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            log.error("Tarefa de submissão rejeitada! Queue cheia. Considere aumentar capacidade.");
            // Em produção, considere enviar para DLQ (Dead Letter Queue)
        });

        executor.initialize();
        log.info("Thread pool de submissões configurado: core={}, max={}, queue={}",
            executor.getCorePoolSize(),
            executor.getMaxPoolSize(),
            executor.getQueueCapacity());

        return executor;
    }
}

