package com.example.app.intake.dto;

import com.example.app.intake.entity.SampleCodeEvent;

import java.time.Instant;

public record SampleCodeEventResponse(Long id, String eventType, String oldCode, String newCode, String reason,
        String actor, Instant createdAt) {
    public static SampleCodeEventResponse from(SampleCodeEvent event) {
        return new SampleCodeEventResponse(event.getId(), event.getEventType().name(), event.getOldCode(),
                event.getNewCode(), event.getReason(), event.getActor().getDisplayName(), event.getCreatedAt());
    }
}
