package com.event_rel.event_reliability_platform.Event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository eventRepository;
    private final WebhookDeliveryService webhookDeliveryService;

    public EventController(EventRepository eventRepository, WebhookDeliveryService webhookDeliveryService) {
        this.eventRepository = eventRepository;
        this.webhookDeliveryService = webhookDeliveryService;
    }


    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(@Valid  @RequestBody CreateEventRequest request){
        Event event = new Event(
                request.eventType(),
                request.payload()
        );
      Event savedEvent = eventRepository.save(event);
      return new EventResponse(
              savedEvent.getId(),
              savedEvent.getEventType(),
              savedEvent.getPayload(),
              savedEvent.getCreatedAt(),
              savedEvent.getStatus(),
              savedEvent.getAttemptCount(),
              savedEvent.getNextAttemptAt(),
              savedEvent.getLastError(),
              savedEvent.getFailedAt()
      );
    }

    public record CreateEventRequest(
            @NotBlank String eventType, // not missing, empty, whitespace
            @NonNull JsonNode payload   // no missing and null, allows empty
    ){}


    @GetMapping("/{id}")
    public EventResponse getEventById(@PathVariable UUID id){
        Event event = eventRepository.findById(id).orElseThrow(()->
                new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND,"Event not fount"));
        return new EventResponse(
                event.getId(),
                event.getEventType(),
                event.getPayload(),
                event.getCreatedAt(),
                event.getStatus(),
                event.getAttemptCount(),
                event.getNextAttemptAt(),
                event.getLastError(),
                event.getFailedAt()
        );
    }

    @GetMapping("/getAll")
    public List<EventResponse> getAllEvents(){
        return eventRepository.findAll().
                stream().map(event -> new EventResponse(
                        event.getId(),
                        event.getEventType(),
                        event.getPayload(),
                        event.getCreatedAt(),
                        event.getStatus(),
                        event.getAttemptCount(),
                        event.getNextAttemptAt(),
                        event.getLastError(),
                        event.getFailedAt()
                ))
                .toList();
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<Void> deliverEvent(@PathVariable UUID id){
        Event event = eventRepository.findById(id).
                orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event Not Found"));

        webhookDeliveryService.deliverEvent(
                event.getId(),
                event.getEventType(),
                event.getPayload()
        );
        event.setStatus(EventStatus.DELIVERED);
        eventRepository.save(event);

        return ResponseEntity.noContent().build();
    }


    @GetMapping("/dead-letter")
    public List<EventResponse> getDeadLetterEvents() {
        return eventRepository.findByStatus(EventStatus.DEAD_LETTER)
                .stream()
                .map(event -> new EventResponse(
                        event.getId(),
                        event.getEventType(),
                        event.getPayload(),
                        event.getCreatedAt(),
                        event.getStatus(),
                        event.getAttemptCount(),
                        event.getNextAttemptAt(),
                        event.getLastError(),
                        event.getFailedAt()
                ))
                .toList();
    }

    @PostMapping("/{id}/replay")
    public ResponseEntity<Void> replayEvent(@PathVariable UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getStatus() != EventStatus.DEAD_LETTER) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only dead-letter events can be replayed");
        }

        event.setStatus(EventStatus.PENDING);
        event.setAttemptCount(0);
        event.setNextAttemptAt(null);
        event.setLastError(null);
        event.setFailedAt(null);

        eventRepository.save(event);

        return ResponseEntity.accepted().build();
    }

    @GetMapping("/filterByStatus")
    public List<EventResponse> getEvents(
            @RequestParam(required = false) EventStatus status) {

        List<Event> events;

        if (status != null) {
            events = eventRepository.findByStatus(status);
        } else {
            events = eventRepository.findAll();
        }

        return events.stream()
                .map(event -> new EventResponse(
                        event.getId(),
                        event.getEventType(),
                        event.getPayload(),
                        event.getCreatedAt(),
                        event.getStatus(),
                        event.getAttemptCount(),
                        event.getNextAttemptAt(),
                        event.getLastError(),
                        event.getFailedAt()
                ))
                .toList();
    }

    @GetMapping("/page")
    public Page<EventResponse> getEvents(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be >= 0 and size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Event> events = status == null
                ? eventRepository.findAll(pageable)
                : eventRepository.findByStatus(status, pageable);

        return events.map(event -> new EventResponse(
                event.getId(),
                event.getEventType(),
                event.getPayload(),
                event.getCreatedAt(),
                event.getStatus(),
                event.getAttemptCount(),
                event.getNextAttemptAt(),
                event.getLastError(),
                event.getFailedAt()
        ));
    }

}
