package edu.rutmiit.demo.matchmakingevents;

import java.time.OffsetDateTime;
import java.util.UUID;

/** События, связанные с лобби. */
public sealed interface LobbyEvent {

    record Created(
            UUID lobbyId,
            String mode,
            String region,
            String rank,
            int minPlayers,
            OffsetDateTime deadlineAt
    ) implements LobbyEvent {}

    record PlayerJoined(
            UUID lobbyId,
            UUID playerId,
            String nickname,
            int playerCount,
            int minPlayers
    ) implements LobbyEvent {}

    record Formed(
            UUID lobbyId,
            UUID matchId,
            int playerCount,
            String playersSummary
    ) implements LobbyEvent {}

    record Disbanded(
            UUID lobbyId,
            String reason,
            int playerCount
    ) implements LobbyEvent {}
}
