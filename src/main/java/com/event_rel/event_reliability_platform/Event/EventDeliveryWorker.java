package com.event_rel.event_reliability_platform.Event;

import com.event_rel.event_reliability_platform.Event.WebhookMetrics;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventDeliveryWorker {

    private final EventRepository eventRepository;
    private final WebhookDeliveryService webhookDeliveryService;
    private final WebhookMetrics webhookMetrics;

    public EventDeliveryWorker(
            EventRepository eventRepository,
            WebhookDeliveryService webhookDeliveryService, WebhookMetrics webhookMetrics) {

        this.eventRepository = eventRepository;
        this.webhookDeliveryService = webhookDeliveryService;
        this.webhookMetrics = webhookMetrics;
    }

    @Scheduled(fixedDelay = 5000)
    public void processPendingEvents() {

        System.out.println("Background Worker is running...");

        var pendingEvents = eventRepository.findEligibleEvents(
                EventStatus.PENDING,
                java.time.Instant.now()
        );

        for (Event event : pendingEvents) {

            try {
                event.setStatus(EventStatus.PROCESSING);
                eventRepository.save(event);

                webhookDeliveryService.deliverEvent(
                        event.getId(),
                        event.getEventType(),
                        event.getPayload()
                );

                event.setAttemptCount(event.getAttemptCount() + 1);
                event.setStatus(EventStatus.DELIVERED);
                eventRepository.save(event);

                System.out.println(
                        "Event Delivered successfully: " + event.getId()
                );

            } catch (CallNotPermittedException exception) {

                event.setStatus(EventStatus.PENDING);
                event.setNextAttemptAt(
                        java.time.Instant.now().plusSeconds(10)
                );

                eventRepository.save(event);

                System.out.println(
                        "Circuit breaker is OPEN. Delivery postponed: "
                                + event.getId()
                );

            } catch (Exception exception) {

                event.setAttemptCount(event.getAttemptCount() + 1);

                if (event.getAttemptCount() < 3) {

                    long delaySeconds =
                            event.getAttemptCount() == 1 ? 5 : 15;

                    event.setStatus(EventStatus.PENDING);
                    event.setNextAttemptAt(
                            java.time.Instant.now().plusSeconds(delaySeconds)
                    );
                    event.setLastError(exception.getMessage());

                    webhookMetrics.recordRetry();

                    System.out.println(
                            "Event retry scheduled: "
                                    + event.getId()
                                    + " in "
                                    + delaySeconds
                                    + " seconds"
                    );

                } else {

                    event.setStatus(EventStatus.DEAD_LETTER);
                    event.setLastError(exception.getMessage());
                    event.setFailedAt(java.time.Instant.now());
                    event.setNextAttemptAt(null);

                    webhookMetrics.recordDeadLetter();

                    System.err.println(
                            "Event moved to DLQ after "
                                    + event.getAttemptCount()
                                    + " attempts: "
                                    + event.getId()
                    );
                }

                eventRepository.save(event);
            }
        }
    }
}