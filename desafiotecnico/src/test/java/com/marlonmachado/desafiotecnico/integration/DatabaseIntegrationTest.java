package com.marlonmachado.desafiotecnico.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@SpringBootTest
public abstract class DatabaseIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("desafiotecnico")
                    .withUsername("desafiotecnico")
                    .withPassword("desafiotecnico");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );

        registry.add(
                "spring.datasource.driver-class-name",
                postgres::getDriverClassName
        );

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );

        registry.add(
                "spring.flyway.enabled",
                () -> true
        );
    }
}