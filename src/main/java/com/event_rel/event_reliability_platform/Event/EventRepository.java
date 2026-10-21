package com.event_rel.event_reliability_platform.Event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByStatus(EventStatus status);

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    List<Event> findByStatusAndNextAttemptAtLessThanEqual(
            EventStatus status,
            Instant nextAttemptAt
    );

    @Query("""
    SELECT e FROM Event e
    WHERE e.status = :status
      AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= :now)
    """)
    List<Event> findEligibleEvents(
            @Param("status") EventStatus status,
            @Param("now") Instant now
    );
}
