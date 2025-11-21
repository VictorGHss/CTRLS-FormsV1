package br.dev.ctrls.api;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base de testes que inicializa Postgres e Redis via Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withReuse(true);

    @Container
    @ServiceConnection
    protected static final RedisContainer REDIS_CONTAINER = new RedisContainer(DockerImageName.parse("redis:7-alpine"))
            .withReuse(true);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.ssl.enabled", () -> Boolean.FALSE);
        registry.add("spring.cloud.openfeign.enabled", () -> Boolean.FALSE);
    }
}
