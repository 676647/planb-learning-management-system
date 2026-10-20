package fr.skyframe.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides the current time as a bean instead of calling Instant.now() directly.
 * Tests can then replace it with a fixed clock to check time-dependent rules
 * (visibility windows, deadlines) deterministically.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
