package edu.rutmiit.pochaev.matchmakingapicontract.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchMode;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchStatus;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Relation(collectionRelation = "matches", itemRelation = "match")
public class MatchResponse extends RepresentationModel<MatchResponse> {

    private final UUID id;
    private final UUID lobbyId;
    private final MatchMode mode;
    private final Region region;
    private final Rank rank;
    private final MatchStatus status;
    private final List<PlayerResponse> players;
    private final List<MatchEventResponse> events;
    private final PlayerResponse winner;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime finishedAt;

    public MatchResponse(UUID id,
                         UUID lobbyId,
                         MatchMode mode,
                         Region region,
                         Rank rank,
                         MatchStatus status,
                         List<PlayerResponse> players,
                         List<MatchEventResponse> events,
                         PlayerResponse winner,
                         OffsetDateTime createdAt,
                         OffsetDateTime finishedAt) {
        this.id = id;
        this.lobbyId = lobbyId;
        this.mode = mode;
        this.region = region;
        this.rank = rank;
        this.status = status;
        this.players = players == null ? List.of() : List.copyOf(players);
        this.events = events == null ? List.of() : List.copyOf(events);
        this.winner = winner;
        this.createdAt = createdAt;
        this.finishedAt = finishedAt;
    }

    public UUID getId() { return id; }
    public UUID getLobbyId() { return lobbyId; }
    public MatchMode getMode() { return mode; }
    public Region getRegion() { return region; }
    public Rank getRank() { return rank; }
    public MatchStatus getStatus() { return status; }
    public List<PlayerResponse> getPlayers() { return players; }
    public List<MatchEventResponse> getEvents() { return events; }
    public PlayerResponse getWinner() { return winner; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }

    public UUID id() { return id; }
    public UUID lobbyId() { return lobbyId; }
    public MatchMode mode() { return mode; }
    public Region region() { return region; }
    public Rank rank() { return rank; }
    public MatchStatus status() { return status; }
    public List<PlayerResponse> players() { return players; }
    public List<MatchEventResponse> events() { return events; }
    public PlayerResponse winner() { return winner; }
    public OffsetDateTime createdAt() { return createdAt; }
    public OffsetDateTime finishedAt() { return finishedAt; }
}
