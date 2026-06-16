package edu.rutmiit.pochaev.matchmakingapicontract.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchEventType;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchEventResponse {

    private final UUID id;
    private final MatchEventType type;
    private final String message;
    private final UUID actorPlayerId;
    private final UUID targetPlayerId;
    private final OffsetDateTime occurredAt;

    public MatchEventResponse(UUID id,
                              MatchEventType type,
                              String message,
                              UUID actorPlayerId,
                              UUID targetPlayerId,
                              OffsetDateTime occurredAt) {
        this.id = id;
        this.type = type;
        this.message = message;
        this.actorPlayerId = actorPlayerId;
        this.targetPlayerId = targetPlayerId;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public MatchEventType getType() { return type; }
    public String getMessage() { return message; }
    public UUID getActorPlayerId() { return actorPlayerId; }
    public UUID getTargetPlayerId() { return targetPlayerId; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }

    public UUID id() { return id; }
    public MatchEventType type() { return type; }
    public String message() { return message; }
    public UUID actorPlayerId() { return actorPlayerId; }
    public UUID targetPlayerId() { return targetPlayerId; }
    public OffsetDateTime occurredAt() { return occurredAt; }
}
