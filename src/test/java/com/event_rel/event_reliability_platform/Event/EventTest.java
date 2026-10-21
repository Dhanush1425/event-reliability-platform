package com.event_rel.event_reliability_platform.Event;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventTest {

    @Test
    void newEventShouldHavePendingStatus() {
        Event event = new Event(
                "order.created",
                null
        );

        assertEquals(EventStatus.PENDING, event.getStatus());
    }

    @Test
    void eventStatusCanBeUpdated() {
        Event event = new Event("order.created", null);

        event.setStatus(EventStatus.PROCESSING);

        assertEquals(EventStatus.PROCESSING, event.getStatus());
    }

    @Test
    void attemptCountCanBeUpdated() {
        Event event = new Event("order.created", null);

        event.setAttemptCount(1);

        assertEquals(1, event.getAttemptCount());
    }

    @Test
    void newEventShouldHaveZeroAttempts() {
        Event event = new Event("order.created", null);

        assertEquals(0, event.getAttemptCount());
    }

    @Test
    void eventShouldStoreLastError() {
        Event event = new Event("order.created", null);

        event.setLastError("Receiver unavailable");

        assertEquals("Receiver unavailable", event.getLastError());
    }

    @Test
    void eventShouldStoreFailedAtTimestamp() {
        Event event = new Event("order.created", null);
        Instant failureTime = Instant.now();

        event.setFailedAt(failureTime);

        assertEquals(failureTime, event.getFailedAt());
    }
}