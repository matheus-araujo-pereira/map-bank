package com.mapbank.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

/**
 * Configuração da documentação OpenAPI 3 / Swagger UI do MAP-Bank.
 *
 * @author Matheus Araujo Pereira
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MAP-Bank API - Sistema Bancário Corporativo")
                        .version("1.0.0")
                        .description("API REST corporativa do MAP-Bank (Matheus Araujo Pereira Bank). " +
                                "Construída com Java 21, Spring Boot 3.4, Virtual Threads, Observabilidade com Prometheus e PostgreSQL. " +
                                "Desenvolvida como projeto de demonstração sênior/pleno para a NTT DATA.")
                        .contact(new Contact()
                                .name("Matheus Araujo Pereira")
                                .email("matheus.araujo@mapbank.com")
                                .url("https://github.com/matheus-araujo-pereira"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Ambiente de Desenvolvimento Local")
                ));
    }
}
