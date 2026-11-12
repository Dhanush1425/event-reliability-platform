package com.event_rel.event_reliability_platform.Event;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Service
public class WebhookDeliveryService {

    private final RestClient webhookRestClient;
    private final WebhookMetrics webhookMetrics;

    public WebhookDeliveryService(RestClient webhookRestClient, WebhookMetrics webhookMetrics) {
        this.webhookRestClient = webhookRestClient;
        this.webhookMetrics = webhookMetrics;
    }

    @CircuitBreaker(name = "webhookDelivery")
    public void deliverEvent(UUID eventId, String eventType, Object payload) {
        try {
            webhookRestClient.post()
                    .uri("/receiver/events")
                    .body(Map.of(
                            "eventId", eventId.toString(),
                            "eventType", eventType,
                            "payload", payload
                    ))
                    .retrieve()
                    .toBodilessEntity();

            webhookMetrics.recordSuccess();

        } catch (Exception exception) {
            webhookMetrics.recordFailure();
            throw exception;
        }
    }
}
