package com.event_rel.event_reliability_platform.Event;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class WebhookMetrics {

    private final MeterRegistry registry;

    public WebhookMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordSuccess() {
        registry.counter("webhook.delivery.success").increment();
    }

    public void recordFailure() {
        registry.counter("webhook.delivery.failure").increment();
    }

    public void recordRetry() {
        registry.counter("webhook.delivery.retry").increment();
    }

    public void recordDeadLetter() {
        registry.counter("webhook.delivery.dlq").increment();
    }
}
