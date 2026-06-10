package edu.rutmiit.demo.grpcmatchanalytics.service;

import edu.rutmiit.demo.matchmakinggrpc.CalculateRankChangesRequest;
import edu.rutmiit.demo.matchmakinggrpc.MatchPlayer;
import edu.rutmiit.demo.matchmakinggrpc.MatchRankEnrichmentGrpc;
import edu.rutmiit.demo.matchmakinggrpc.PlayerRankChange;
import edu.rutmiit.demo.matchmakinggrpc.RankChangesResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC-сервер расчета рейтинга после матча.
 * Победителю +25, проигравшим -10.
 */
public class MatchAnalyticsServiceImpl extends MatchRankEnrichmentGrpc.MatchRankEnrichmentImplBase {

    private static final Logger log = LoggerFactory.getLogger(MatchAnalyticsServiceImpl.class);

    @Override
    public void calculateRankChanges(CalculateRankChangesRequest request,
                                     StreamObserver<RankChangesResponse> responseObserver) {

        log.info("gRPC-запрос расчета рейтинга: matchId={} mode={} region={} winner={}",
                request.getMatchId(), request.getMode(), request.getRegion(), request.getWinnerNickname());

        RankChangesResponse.Builder response = RankChangesResponse.newBuilder()
                .setMatchId(request.getMatchId())
                .setWinnerNickname(request.getWinnerNickname());

        StringBuilder summary = new StringBuilder("Изменения рейтинга после матча: ");

        for (int i = 0; i < request.getPlayersCount(); i++) {
            MatchPlayer player = request.getPlayers(i);
            int delta = player.getWinner() ? 25 : -10;
            int newRating = Math.max(0, player.getRating() + delta);
            String newRank = rankByRating(newRating);

            PlayerRankChange change = PlayerRankChange.newBuilder()
                    .setPlayerId(player.getPlayerId())
                    .setNickname(player.getNickname())
                    .setOldRating(player.getRating())
                    .setDelta(delta)
                    .setNewRating(newRating)
                    .setOldRank(player.getRank())
                    .setNewRank(newRank)
                    .build();

            response.addChanges(change);

            if (i > 0) {
                summary.append("; ");
            }
            summary.append(player.getNickname())
                    .append(" ")
                    .append(delta > 0 ? "+" : "")
                    .append(delta)
                    .append(" до ")
                    .append(newRating)
                    .append(" (")
                    .append(newRank)
                    .append(")");
        }

        RankChangesResponse result = response.setSummary(summary.toString()).build();

        log.info("gRPC-ответ: matchId={}, {}", result.getMatchId(), result.getSummary());

        responseObserver.onNext(result);
        responseObserver.onCompleted();
    }

    private String rankByRating(int rating) {
        if (rating < 1000) {
            return "BRONZE";
        }
        if (rating < 1500) {
            return "SILVER";
        }
        if (rating < 2000) {
            return "GOLD";
        }
        if (rating < 2500) {
            return "PLATINUM";
        }
        return "DIAMOND";
    }
}
