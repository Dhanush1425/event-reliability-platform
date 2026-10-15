package com.event_rel.event_reliability_platform.Event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository eventRepository;

    public EventController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
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
              savedEvent.getCreatedAt()
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
                event.getCreatedAt()
        );
    }

    @GetMapping("/getAll")
    public List<EventResponse> getAllEvents(){
        return eventRepository.findAll().
                stream().map(event -> new EventResponse(
                        event.getId(),
                        event.getEventType(),
                        event.getPayload(),
                        event.getCreatedAt()
                ))
                .toList();
    }
}
