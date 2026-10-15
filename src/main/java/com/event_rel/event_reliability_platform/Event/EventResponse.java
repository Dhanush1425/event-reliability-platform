package com.event_rel.event_reliability_platform.Event;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String eventType,
        JsonNode payload,
        Instant createdAt
) {}

