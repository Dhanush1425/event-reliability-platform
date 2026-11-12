package com.event_rel.event_reliability_platform.config;

import com.event_rel.event_reliability_platform.Event.WebhookMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public WebhookMetrics webhookMetrics(MeterRegistry registry) {
        return new WebhookMetrics(registry);
    }
}
