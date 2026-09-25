package com.financial.transfer.concurrency;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class ConcurrencyTestBase {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("financial_transfer_concurrency_test")
        .withUsername("test_user")
        .withPassword("test_pass");
       

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    protected <T> List<TaskResult<T>> executarConcorrentemente(
            int nThreads, ConcurrentTask<T> task) throws InterruptedException {

        CyclicBarrier barrier  = new CyclicBarrier(nThreads);
        CountDownLatch done    = new CountDownLatch(nThreads);
        List<TaskResult<T>> results = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);

        for (int i = 0; i < nThreads; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    barrier.await(10, TimeUnit.SECONDS); // sincroniza as threads
                    T result = task.execute(idx);
                    results.add(new TaskResult<>(idx, result, null));
                } catch (Exception e) {
                    results.add(new TaskResult<>(idx, null, e));
                } finally {
                    done.countDown();
                }
            });
        }

        done.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();
        return results;
    }

    @FunctionalInterface
    public interface ConcurrentTask<T> {
        T execute(int threadIndex) throws Exception;
    }

    public record TaskResult<T>(int threadIndex, T result, Exception error) {
        public boolean sucesso() { return error == null; }
    }
}
