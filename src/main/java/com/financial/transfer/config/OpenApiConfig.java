package com.financial.transfer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Financial Transfer API")
                .description("Sistema de transferências financeiras com concorrência, idempotência e extrato imutável.")
                .version("1.0.0"));
    }
}
