package com.mapbank.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import java.time.Instant;

/**
 * HealthIndicator customizado do Spring Boot Actuator para verificação da saúde das integrações bancárias
 * (Conectividade com BACEN / SPI / DICT).
 *
 * <p>Exposto em {@code /actuator/health} para orquestradores (Kubernetes, AWS ECS) e ferramentas de monitoramento.</p>
 *
 * @author Matheus Araujo Pereira
 */
@Component
public class BankingHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // Simulação de verificação de liveness dos gateways de pagamentos instantâneos (SPI / DICT)
        boolean spiConnected = true;
        boolean dictConnected = true;

        if (spiConnected && dictConnected) {
            return Health.up()
                    .withDetail("spiGateway", "ONLINE - Liquidação em tempo real ativa")
                    .withDetail("bacenDict", "ONLINE - Diretório de Identificadores sincronizado")
                    .withDetail("lastSync", Instant.now().toString())
                    .build();
        }

        return Health.down()
                .withDetail("spiGateway", "OFFLINE - Falha de comunicação com o BACEN")
                .build();
    }
}
