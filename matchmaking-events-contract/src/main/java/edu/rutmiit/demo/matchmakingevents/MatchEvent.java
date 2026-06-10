package edu.rutmiit.demo.matchmakingevents;

import java.util.List;
import java.util.UUID;

/** События, связанные с матчем. */
public sealed interface MatchEvent {

    /** Игрок внутри события match.finished. */
    record MatchPlayer(
            UUID playerId,
            String nickname,
            int rating,
            String rank
    ) {}

    /** Изменение рейтинга игрока после gRPC-обогащения. */
    record PlayerRankChange(
            UUID playerId,
            String nickname,
            int oldRating,
            int delta,
            int newRating,
            String oldRank,
            String newRank
    ) {}


    /** Одно live-событие во время матча. */
    record Progress(
            UUID matchId,
            UUID lobbyId,
            String eventType,
            String message,
            int eventNumber,
            String status
    ) implements MatchEvent {}

    record Finished(
            UUID matchId,
            UUID lobbyId,
            String mode,
            String region,
            String rank,
            int playerCount,
            List<MatchPlayer> players,
            UUID winnerPlayerId,
            String winnerNickname,
            String resultSummary
    ) implements MatchEvent {}

    /**
     * Результат gRPC-обогащения матча.
     * Его публикует grpc-match-enrichment-client после вызова gRPC-сервера.
     * Здесь нет сложной БД. Мы просто показываем расчет изменений рейтинга после матча.
     */
    record Enriched(
            UUID matchId,
            String winnerNickname,
            List<PlayerRankChange> changes,
            String summary
    ) implements MatchEvent {}
}
