package com.mapbank.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração global de observabilidade e métricas de microsserviço (Micrometer).
 *
 * @author Matheus Araújo Pereira
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
                "application", "map-bank",
                "environment", "production-ready",
                "institution", "MAP-BANK-NTT"
        );
    }
}
