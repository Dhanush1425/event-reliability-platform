package com.event_rel.event_reliability_platform.Event;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
public class WebhookDeliveryService {

    private final RestClient webhookRestClient;

    public WebhookDeliveryService(RestClient webhookRestClient) {
        this.webhookRestClient = webhookRestClient;
    }

    public void deliverEvent(UUID eventId, String eventType, Object payload) {
        webhookRestClient.post()
                .uri("/receiver/events")
                .body(java.util.Map.of(
                        "eventId", eventId.toString(),
                        "eventType", eventType,
                        "payload", payload
                ))
                .retrieve()
                .toBodilessEntity();
    }
}
