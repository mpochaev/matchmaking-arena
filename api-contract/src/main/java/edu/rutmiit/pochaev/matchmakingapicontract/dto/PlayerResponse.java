package edu.rutmiit.pochaev.matchmakingapicontract.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Relation(collectionRelation = "players", itemRelation = "player")
public class PlayerResponse extends RepresentationModel<PlayerResponse> {

    private final UUID id;
    private final String nickname;
    private final int rating;
    private final Region region;
    private final Rank rank;
    private final OffsetDateTime createdAt;

    public PlayerResponse(UUID id, String nickname, int rating, Region region, Rank rank, OffsetDateTime createdAt) {
        this.id = id;
        this.nickname = nickname;
        this.rating = rating;
        this.region = region;
        this.rank = rank;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getNickname() { return nickname; }
    public int getRating() { return rating; }
    public Region getRegion() { return region; }
    public Rank getRank() { return rank; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public UUID id() { return id; }
    public String nickname() { return nickname; }
    public int rating() { return rating; }
    public Region region() { return region; }
    public Rank rank() { return rank; }
    public OffsetDateTime createdAt() { return createdAt; }
}
