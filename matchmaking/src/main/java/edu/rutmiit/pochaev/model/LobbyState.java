package edu.rutmiit.pochaev.model;

import edu.rutmiit.pochaev.matchmakingapicontract.enums.LobbyStatus;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchMode;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class LobbyState {

    private final UUID id;
    private final MatchMode mode;
    private final Region region;
    private final Rank rank;
    private LobbyStatus status;
    private final int minPlayers;
    private final List<UUID> playerIds;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime deadlineAt;
    private OffsetDateTime startedAt;
    private UUID matchId;

    public LobbyState(UUID id,
                      MatchMode mode,
                      Region region,
                      Rank rank,
                      LobbyStatus status,
                      int minPlayers,
                      List<UUID> playerIds,
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
        this.playerIds = playerIds;
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
    public void setStatus(LobbyStatus status) { this.status = status; }
    public int getMinPlayers() { return minPlayers; }
    public List<UUID> getPlayerIds() { return playerIds; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getDeadlineAt() { return deadlineAt; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }
}
