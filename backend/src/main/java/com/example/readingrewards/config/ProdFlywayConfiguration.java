package com.example.readingrewards.config;

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Neon prod was baselined at V1; V2–V7 were added later. Flyway 11 validate-on-migrate
 * blocks startup before pending scripts run unless configured explicitly.
 */
@Configuration
@Profile("prod")
public class ProdFlywayConfiguration {

    @Bean
    FlywayConfigurationCustomizer prodFlywayCustomizer() {
        return configuration -> configuration
                .outOfOrder(true)
                .ignoreMigrationPatterns(
                        "versioned:pending",
                        "*:pending",
                        "*:ignored")
                .validateOnMigrate(false);
    }
}
