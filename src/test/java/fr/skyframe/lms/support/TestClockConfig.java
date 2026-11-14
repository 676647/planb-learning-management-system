package fr.skyframe.lms.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;

/**
 * Replaces the real clock (from ClockConfig) with a MutableClock in tests that import this.
 * @Primary makes Spring inject this one wherever a Clock is needed.
 */
@TestConfiguration
public class TestClockConfig {

    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock(Instant.parse("2026-10-19T08:00:00Z"));
    }
}
