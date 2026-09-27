package com.mapbank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Ponto de entrada (Entrypoint) do ecossistema MAP-Bank.
 *
 * <p>Inicializado com <b>Java 21 LTS</b> e <b>Spring Boot 3.4.x</b>, com suporte nativo a
 * <b>Virtual Threads (Project Loom)</b> habilitado em {@code application.yml}
 * ({@code spring.threads.virtual.enabled=true}).</p>
 *
 * @author Matheus Araujo Pereira
 */
@SpringBootApplication
@EnableAsync
public class MapBankApplication {

    private static final Logger log = LoggerFactory.getLogger(MapBankApplication.class);

    public static void main(String[] args) {
        log.info("Inicializando o MAP-Bank (Matheus Araujo Pereira Bank) em Java 21 com Virtual Threads...");
        SpringApplication.run(MapBankApplication.class, args);
        log.info("MAP-Bank inicializado com sucesso. Documentação Swagger UI em: http://localhost:8080/swagger-ui.html");
    }
}
