package com.event_rel.event_reliability_platform.Event;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventDeliveryWorker {

    private final EventRepository eventRepository;
    private final WebhookDeliveryService webhookDeliveryService;

    public EventDeliveryWorker(EventRepository eventRepository, WebhookDeliveryService webhookDeliveryService) {
        this.eventRepository = eventRepository;
        this.webhookDeliveryService = webhookDeliveryService;
    }


    @Scheduled(fixedDelay = 5000)
    public void processPendingEvents() {
        System.out.println("Background Worker is running...");

        var pendingEvents = eventRepository.findEligibleEvents(EventStatus.PENDING, java.time.Instant.now());

        for (Event event : pendingEvents) {
            try {
                event.setAttemptCount(event.getAttemptCount() + 1);
                event.setStatus(EventStatus.PROCESSING);
                eventRepository.save(event);

                webhookDeliveryService.deliverEvent(
                        event.getId(),
                        event.getEventType(),
                        event.getPayload()
                );
                event.setStatus(EventStatus.DELIVERED);
                eventRepository.save(event);

                System.out.println("Event Delivered successfully: " + event.getId());

            } catch (Exception exception) {
                if (event.getAttemptCount() < 3) {
                    long delaySeconds = event.getAttemptCount() == 1 ? 5 : 15;
                    event.setStatus(EventStatus.PENDING);
                    event.setNextAttemptAt(java.time.Instant.now().plusSeconds(delaySeconds));
                    event.setLastError(exception.getMessage());

                    System.out.println("Event retry scheduled: "+ event.getId() + " in " + delaySeconds + " seconds");
                } else {
                    System.out.println("event entered");
                    event.setStatus(EventStatus.DEAD_LETTER);
                    event.setLastError(exception.getMessage());
                    event.setFailedAt(java.time.Instant.now());
                    event.setNextAttemptAt(null);

                    System.err.println("Event moved to DLQ after "
                                    + event.getAttemptCount()
                                    + " attempts: " + event.getId());
                }

                eventRepository.save(event);
            }
        }
    }
}


//
//        if App B is unavailable, App A marks the event FAILED immediately. Instead, we want:
//        - First failure: return the event to PENDING and retry after 5 seconds.
//                - Second failure: return it to PENDING and retry after 15 seconds.
//                - Third failure: mark it FAILED permanently for now. We’ll use this as the basis for a dead-letter queue later.
//
//    }

