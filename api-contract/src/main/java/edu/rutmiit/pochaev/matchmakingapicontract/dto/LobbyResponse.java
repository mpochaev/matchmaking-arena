package edu.rutmiit.pochaev.matchmakingapicontract.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.LobbyStatus;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchMode;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Relation(collectionRelation = "lobbies", itemRelation = "lobby")
public class LobbyResponse extends RepresentationModel<LobbyResponse> {

    private final UUID id;
    private final MatchMode mode;
    private final Region region;
    private final Rank rank;
    private final LobbyStatus status;
    private final int minPlayers;
    private final int playerCount;
    private final List<PlayerResponse> players;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime deadlineAt;
    private final OffsetDateTime startedAt;
    private final UUID matchId;

    public LobbyResponse(UUID id,
                         MatchMode mode,
                         Region region,
                         Rank rank,
                         LobbyStatus status,
                         int minPlayers,
                         int playerCount,
                         List<PlayerResponse> players,
                         OffsetDateTime createdAt,
                         OffsetDateTime deadlineAt,
                         OffsetDateTime startedAt,
                         UUID matchId) {
        this.id = id;
        this.mode = mode;
        this.region = region;
        this.rank = rank;
        this.status = status;
        this.minPlayers = minPlayers;
        this.playerCount = playerCount;
        this.players = players == null ? List.of() : List.copyOf(players);
        this.createdAt = createdAt;
        this.deadlineAt = deadlineAt;
        this.startedAt = startedAt;
        this.matchId = matchId;
    }

    public UUID getId() { return id; }
    public MatchMode getMode() { return mode; }
    public Region getRegion() { return region; }
    public Rank getRank() { return rank; }
    public LobbyStatus getStatus() { return status; }
    public int getMinPlayers() { return minPlayers; }
    public int getPlayerCount() { return playerCount; }
    public List<PlayerResponse> getPlayers() { return players; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getDeadlineAt() { return deadlineAt; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public UUID getMatchId() { return matchId; }

    public UUID id() { return id; }
    public MatchMode mode() { return mode; }
    public Region region() { return region; }
    public Rank rank() { return rank; }
    public LobbyStatus status() { return status; }
    public int minPlayers() { return minPlayers; }
    public int playerCount() { return playerCount; }
    public List<PlayerResponse> players() { return players; }
    public OffsetDateTime createdAt() { return createdAt; }
    public OffsetDateTime deadlineAt() { return deadlineAt; }
    public OffsetDateTime startedAt() { return startedAt; }
    public UUID matchId() { return matchId; }
}
