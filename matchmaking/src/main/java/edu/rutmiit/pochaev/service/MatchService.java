package edu.rutmiit.pochaev.service;

import edu.rutmiit.pochaev.event.MatchEventPublisher;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.MatchEventResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.MatchResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PagedResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PlayerResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.LobbyStatus;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchEventType;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchMode;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchStatus;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;
import edu.rutmiit.pochaev.matchmakingapicontract.exception.ResourceNotFoundException;
import edu.rutmiit.pochaev.model.LobbyState;
import edu.rutmiit.pochaev.model.MatchState;
import edu.rutmiit.pochaev.model.PlayerState;
import edu.rutmiit.pochaev.storage.MatchmakingStorage;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class MatchService {

    private static final int MATCH_MIN_DURATION_SECONDS = 60;
    private static final int MATCH_MAX_DURATION_SECONDS = 120;
    private static final int MATCH_MIN_PROGRESS_EVENTS = 4;
    private static final int MATCH_MAX_PROGRESS_EVENTS = 8;

    private static final int PLAYER_HIT_SCORE = 10;
    private static final int PLAYER_FOUND_LOOT_SCORE = 5;
    private static final int PLAYER_DODGED_SCORE = 3;
    private static final int PLAYER_HEALED_SCORE = 2;
    private static final int ZONE_DAMAGE_SCORE = -5;

    private final MatchmakingStorage storage;
    private final PlayerService playerService;
    private final MatchEventPublisher matchEventPublisher;

    public MatchService(MatchmakingStorage storage,
                        PlayerService playerService,
                        MatchEventPublisher matchEventPublisher) {
        this.storage = storage;
        this.playerService = playerService;
        this.matchEventPublisher = matchEventPublisher;
    }

    @PreDestroy
    public void shutdownMatchExecutor() {
        storage.matchExecutor.shutdownNow();
    }

    public PagedResponse<MatchResponse> findMatches(MatchStatus status,
                                                    Region region,
                                                    Rank rank,
                                                    MatchMode mode,
                                                    int page,
                                                    int size) {
        var all = storage.matches.values().stream()
                .filter(match -> status == null || match.status() == status)
                .filter(match -> region == null || match.region() == region)
                .filter(match -> rank == null || match.rank() == rank)
                .filter(match -> mode == null || match.mode() == mode)
                .sorted(Comparator.comparing(MatchState::createdAt).reversed())
                .map(this::toMatchResponse)
                .toList();
        return PagedResponse.of(all, page, size);
    }

    public MatchResponse findMatchById(UUID id) {
        return toMatchResponse(matchState(id));
    }

    public MatchResponse findMatchByLobbyId(UUID lobbyId) {
        LobbyState lobby = storage.lobbies.get(lobbyId);
        if (lobby == null || lobby.getMatchId() == null) {
            return null;
        }
        return toMatchResponse(matchState(lobby.getMatchId()));
    }

    public MatchState matchState(UUID id) {
        MatchState match = storage.matches.get(id);
        if (match == null) {
            throw new ResourceNotFoundException("Матч не найден: " + id);
        }
        return match;
    }

    public MatchState startMatch(LobbyState lobby) {
        OffsetDateTime startedAt = OffsetDateTime.now();
        lobby.setStatus(LobbyStatus.FORMED);
        lobby.setStartedAt(startedAt);

        UUID matchId = UUID.randomUUID();
        List<UUID> matchPlayerIds = new CopyOnWriteArrayList<>(lobby.getPlayerIds());
        List<MatchEventResponse> events = new CopyOnWriteArrayList<>();

        events.add(event(
                MatchEventType.MATCH_STARTED,
                "Матч начался. Игроков в лобби: " + matchPlayerIds.size(),
                null,
                null,
                startedAt
        ));

        MatchState match = new MatchState(
                matchId,
                lobby.getId(),
                lobby.getMode(),
                lobby.getRegion(),
                lobby.getRank(),
                MatchStatus.IN_PROGRESS,
                matchPlayerIds,
                events,
                null,
                startedAt,
                null
        );

        storage.matches.put(match.id(), match);
        lobby.setMatchId(match.id());
        return match;
    }

    public void scheduleMatchSimulation(UUID matchId) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int durationSeconds = random.nextInt(MATCH_MIN_DURATION_SECONDS, MATCH_MAX_DURATION_SECONDS + 1);
        int progressEvents = random.nextInt(MATCH_MIN_PROGRESS_EVENTS, MATCH_MAX_PROGRESS_EVENTS + 1);

        for (Integer delaySeconds : randomProgressDelays(durationSeconds, progressEvents)) {
            storage.matchExecutor.schedule(() -> addRandomMatchProgressEvent(matchId),
                    delaySeconds, TimeUnit.SECONDS);
        }

        storage.matchExecutor.schedule(() -> finishMatch(matchId),
                durationSeconds, TimeUnit.SECONDS);
    }

    public MatchResponse toMatchResponse(MatchState match) {
        List<PlayerResponse> matchPlayers = match.playerIds().stream()
                .map(storage.players::get)
                .filter(player -> player != null)
                .map(playerService::toPlayerResponse)
                .toList();

        PlayerResponse winner = match.winnerPlayerId() == null ? null : playerService.toPlayerResponse(playerService.playerState(match.winnerPlayerId()));

        return new MatchResponse(
                match.id(),
                match.lobbyId(),
                match.mode(),
                match.region(),
                match.rank(),
                match.status(),
                matchPlayers,
                match.events(),
                winner,
                match.createdAt(),
                match.finishedAt()
        );
    }

    private List<Integer> randomProgressDelays(int durationSeconds, int progressEvents) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Integer> delays = new ArrayList<>();

        int minDelay = 8;
        int maxDelay = Math.max(minDelay + 1, durationSeconds - 5);

        while (delays.size() < progressEvents) {
            int delay = random.nextInt(minDelay, maxDelay);
            if (!delays.contains(delay)) {
                delays.add(delay);
            }
        }

        delays.sort(Integer::compareTo);
        return delays;
    }

    private void addRandomMatchProgressEvent(UUID matchId) {
        MatchState match = storage.matches.get(matchId);
        if (match == null || match.status() != MatchStatus.IN_PROGRESS) {
            return;
        }

        OffsetDateTime occurredAt = OffsetDateTime.now();
        List<UUID> matchPlayerIds = match.playerIds();
        PlayerState actor = randomPlayer(matchPlayerIds);
        PlayerState target = randomTarget(matchPlayerIds, actor.id());

        int eventType = ThreadLocalRandom.current().nextInt(5);
        MatchEventResponse progressEvent = switch (eventType) {
            case 0 -> event(MatchEventType.PLAYER_HIT,
                    actor.nickname() + " попал по " + target.nickname() + " (+" + PLAYER_HIT_SCORE + " очков)",
                    actor.id(), target.id(), occurredAt);
            case 1 -> event(MatchEventType.PLAYER_DODGED,
                    actor.nickname() + " увернулся от атаки (+" + PLAYER_DODGED_SCORE + " очка)",
                    actor.id(), null, occurredAt);
            case 2 -> event(MatchEventType.PLAYER_FOUND_LOOT,
                    actor.nickname() + " нашел полезный предмет (+" + PLAYER_FOUND_LOOT_SCORE + " очков)",
                    actor.id(), null, occurredAt);
            case 3 -> event(MatchEventType.ZONE_DAMAGE,
                    actor.nickname() + " получил урон от зоны (" + ZONE_DAMAGE_SCORE + " очков)",
                    actor.id(), null, occurredAt);
            default -> event(MatchEventType.PLAYER_HEALED,
                    actor.nickname() + " использовал аптечку (+" + PLAYER_HEALED_SCORE + " очка)",
                    actor.id(), null, occurredAt);
        };

        match.events().add(progressEvent);
        matchEventPublisher.publishProgress(toMatchResponse(match), progressEvent);
    }

    private void finishMatch(UUID matchId) {
        MatchState match = storage.matches.get(matchId);
        if (match == null || match.status() != MatchStatus.IN_PROGRESS) {
            return;
        }

        OffsetDateTime finishedAt = OffsetDateTime.now();
        Map<UUID, Integer> scores = calculateScores(match);
        UUID winnerId = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(match.playerIds().get(0));
        PlayerState winner = playerService.playerState(winnerId);

        MatchEventResponse finishEvent = event(
                MatchEventType.MATCH_FINISHED,
                "Матч завершен. Победитель: " + winner.nickname() + ". Счет: " + formatScores(scores),
                winner.id(),
                null,
                finishedAt
        );
        match.events().add(finishEvent);

        MatchState finishedMatch = new MatchState(
                match.id(),
                match.lobbyId(),
                match.mode(),
                match.region(),
                match.rank(),
                MatchStatus.FINISHED,
                match.playerIds(),
                match.events(),
                winner.id(),
                match.createdAt(),
                finishedAt
        );

        storage.matches.put(matchId, finishedMatch);
        matchEventPublisher.publishFinished(toMatchResponse(finishedMatch));
    }


    private Map<UUID, Integer> calculateScores(MatchState match) {
        Map<UUID, Integer> scores = new LinkedHashMap<>();
        for (UUID playerId : match.playerIds()) {
            scores.put(playerId, 0);
        }

        for (MatchEventResponse matchEvent : match.events()) {
            UUID actorPlayerId = matchEvent.actorPlayerId();
            if (actorPlayerId != null && scores.containsKey(actorPlayerId)) {
                scores.compute(actorPlayerId, (id, currentScore) -> currentScore + scoreDelta(matchEvent.type()));
            }
        }

        return scores;
    }

    private int scoreDelta(MatchEventType type) {
        return switch (type) {
            case PLAYER_HIT -> PLAYER_HIT_SCORE;
            case PLAYER_FOUND_LOOT -> PLAYER_FOUND_LOOT_SCORE;
            case PLAYER_DODGED -> PLAYER_DODGED_SCORE;
            case PLAYER_HEALED -> PLAYER_HEALED_SCORE;
            case ZONE_DAMAGE -> ZONE_DAMAGE_SCORE;
            default -> 0;
        };
    }

    private String formatScores(Map<UUID, Integer> scores) {
        return scores.entrySet().stream()
                .map(entry -> playerService.playerState(entry.getKey()).nickname() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    private MatchEventResponse event(MatchEventType type,
                                     String message,
                                     UUID actorPlayerId,
                                     UUID targetPlayerId,
                                     OffsetDateTime occurredAt) {
        return new MatchEventResponse(UUID.randomUUID(), type, message, actorPlayerId, targetPlayerId, occurredAt);
    }

    private PlayerState randomPlayer(List<UUID> playerIds) {
        int index = ThreadLocalRandom.current().nextInt(playerIds.size());
        return playerService.playerState(playerIds.get(index));
    }

    private PlayerState randomTarget(List<UUID> playerIds, UUID actorId) {
        PlayerState target;
        do {
            target = randomPlayer(playerIds);
        } while (target.id().equals(actorId));
        return target;
    }
}
