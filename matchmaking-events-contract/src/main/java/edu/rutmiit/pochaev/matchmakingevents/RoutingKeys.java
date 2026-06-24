package edu.rutmiit.pochaev.matchmakingevents;

/**
 * Общие имена exchange и routing key.
 * Публикующий и читающий сервис используют одинаковые строки.
 */
public final class RoutingKeys {

    private RoutingKeys() {}

    public static final String EXCHANGE = "matchmaking.events";

    public static final String PLAYER_CREATED = "player.created";
    public static final String PLAYER_UPDATED = "player.updated";
    public static final String PLAYER_DELETED = "player.deleted";

    public static final String LOBBY_CREATED = "lobby.created";
    public static final String LOBBY_PLAYER_JOINED = "lobby.player-joined";
    public static final String LOBBY_FORMED = "lobby.formed";
    public static final String LOBBY_DISBANDED = "lobby.disbanded";

    public static final String MATCH_PROGRESS = "match.progress";
    public static final String MATCH_FINISHED = "match.finished";
    public static final String MATCH_ENRICHED = "match.enriched";

    public static final String ALL_PLAYER_EVENTS = "player.*";
    public static final String ALL_LOBBY_EVENTS = "lobby.*";
    public static final String ALL_MATCH_EVENTS = "match.*";
    public static final String ALL_EVENTS = "#";
}
