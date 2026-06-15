package edu.rutmiit.pochaev.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import edu.rutmiit.pochaev.config.RabbitMQConfig;
import edu.rutmiit.pochaev.event.LobbyEventPublisher;
import edu.rutmiit.pochaev.event.MatchEventPublisher;
import edu.rutmiit.pochaev.event.PlayerEventPublisher;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.JoinLobbyRequest;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.LobbyResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.MatchEventResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.MatchResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PagedResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PlayerRequest;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PlayerResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.exception.InvalidLobbyOperationException;
import edu.rutmiit.pochaev.matchmakingapicontract.exception.LobbyHasPlayersException;
import edu.rutmiit.pochaev.matchmakingapicontract.exception.PlayerAlreadyInLobbyException;
import edu.rutmiit.pochaev.matchmakingapicontract.exception.ResourceNotFoundException;
import edu.rutmiit.pochaev.matchmakingevents.EventMetadata;
import edu.rutmiit.pochaev.matchmakingevents.MatchEvent;
import jakarta.annotation.PreDestroy;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class MatchmakingService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);

    private static final int MIN_PLAYERS_TO_START = 3;
    private static final int MAX_PLAYERS_IN_LOBBY = 5;
    private static final int LOBBY_START_DELAY_SECONDS = 10;
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final Set<String> ALLOWED_MODES = Set.of("BATTLE_ROYALE", "SURVIVAL", "DEATHMATCH");

    // Демо-матч длится от 1 до 2 минут.
    // События матча отправляются случайно до его завершения.
    private static final int MATCH_MIN_DURATION_SECONDS = 60;
    private static final int MATCH_MAX_DURATION_SECONDS = 120;
    private static final int MATCH_MIN_PROGRESS_EVENTS = 4;
    private static final int MATCH_MAX_PROGRESS_EVENTS = 8;


    private final Map<UUID, PlayerState> players = new LinkedHashMap<>();
    private final Map<UUID, LobbyState> lobbies = new LinkedHashMap<>();
    private final Map<UUID, MatchState> matches = new LinkedHashMap<>();
    private final Map<UUID, ScheduledFuture<?>> lobbyStartTasks = new LinkedHashMap<>();
    private final ScheduledExecutorService matchExecutor = Executors.newSingleThreadScheduledExecutor();

    private final PlayerEventPublisher playerEventPublisher;
    private final LobbyEventPublisher lobbyEventPublisher;
    private final MatchEventPublisher matchEventPublisher;
    private final JsonMapper jsonMapper;

    public MatchmakingService(PlayerEventPublisher playerEventPublisher,
                              LobbyEventPublisher lobbyEventPublisher,
                              MatchEventPublisher matchEventPublisher,
                              JsonMapper jsonMapper) {
        this.playerEventPublisher = playerEventPublisher;
        this.lobbyEventPublisher = lobbyEventPublisher;
        this.matchEventPublisher = matchEventPublisher;
        this.jsonMapper = jsonMapper;

        // Стартовые данные, чтобы проект можно было сразу показать.
        createPlayerInternal(new PlayerRequest("Artem", 1210, "EU", "SILVER"), false);
        createPlayerInternal(new PlayerRequest("Misha", 1180, "EU", "SILVER"), false);
        createPlayerInternal(new PlayerRequest("Sasha", 1270, "EU", "SILVER"), false);
        createPlayerInternal(new PlayerRequest("Dima", 1600, "EU", "GOLD"), false);
    }

    @PreDestroy
    public void shutdownMatchExecutor() {
        matchExecutor.shutdownNow();
    }

    @RabbitListener(queues = RabbitMQConfig.MATCH_ENRICHED_QUEUE, messageConverter = "")
    @SuppressWarnings("UseSpecificCatch")
    public synchronized void handleMatchEnriched(Message message) {
        try {
            JsonNode root = jsonMapper.readTree(message.getBody());
            EventMetadata metadata = jsonMapper.treeToValue(root.get("metadata"), EventMetadata.class);
            MatchEvent.Enriched enriched = jsonMapper.treeToValue(root.get("payload"), MatchEvent.Enriched.class);

            for (MatchEvent.PlayerRankChange change : enriched.changes()) {
                PlayerState oldPlayer = players.get(change.playerId());
                if (oldPlayer == null) {
                    continue;
                }

                PlayerState updatedPlayer = new PlayerState(
                        oldPlayer.id,
                        oldPlayer.nickname,
                        change.newRating(),
                        oldPlayer.region,
                        change.newRank(),
                        oldPlayer.createdAt
                );
                players.put(updatedPlayer.id, updatedPlayer);
            }

            log.info("Рейтинги игроков обновлены после match.enriched: matchId={} [eventId={}]",
                    enriched.matchId(), metadata.eventId());
        } catch (Exception e) {
            log.error("Не удалось обработать match.enriched: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось обработать match.enriched", e);
        }
    }

    public synchronized PagedResponse<PlayerResponse> findPlayers(String region, String rank, int page, int size) {
        List<PlayerResponse> all = players.values().stream()
                .filter(player -> region == null || player.region.equalsIgnoreCase(region))
                .filter(player -> rank == null || player.rank.equalsIgnoreCase(rank))
                .sorted(Comparator.comparing((PlayerState player) -> player.createdAt).reversed())
                .map(this::toPlayerResponse)
                .toList();
        return PagedResponse.of(all, page, size);
    }

    public synchronized PlayerResponse findPlayerById(UUID id) {
        return toPlayerResponse(playerState(id));
    }

    public synchronized PlayerResponse createPlayer(PlayerRequest request) {
        return createPlayerInternal(request, true);
    }

    private PlayerResponse createPlayerInternal(PlayerRequest request, boolean publishEvent) {
        String region = normalize(request.region() == null || request.region().isBlank() ? "EU" : request.region());
        Integer requestedRating = request.rating();
        int rating = requestedRating == null ? 1000 : requestedRating;
        String rank = rankByRating(rating);

        PlayerState player = new PlayerState(
                UUID.randomUUID(),
                request.nickname(),
                rating,
                region,
                rank,
                OffsetDateTime.now()
        );
        players.put(player.id, player);

        PlayerResponse response = toPlayerResponse(player);
        if (publishEvent) {
            playerEventPublisher.publishCreated(response);
        }
        return response;
    }

    public synchronized PagedResponse<LobbyResponse> findLobbies(String status,
                                                                 String region,
                                                                 String rank,
                                                                 String mode,
                                                                 int page,
                                                                 int size) {
        processExpiredLobbiesInternal();
        List<LobbyResponse> all = lobbies.values().stream()
                .filter(lobby -> status == null || lobby.status.equalsIgnoreCase(status))
                .filter(lobby -> region == null || lobby.region.equalsIgnoreCase(region))
                .filter(lobby -> rank == null || lobby.rank.equalsIgnoreCase(rank))
                .filter(lobby -> mode == null || lobby.mode.equalsIgnoreCase(mode))
                .sorted(Comparator.comparing((LobbyState lobby) -> lobby.createdAt).reversed())
                .map(this::toLobbyResponse)
                .toList();
        return PagedResponse.of(all, page, size);
    }

    public synchronized PagedResponse<LobbyResponse> findLobbiesByRegion(String region, int page, int size) {
        return findLobbies(null, region, null, null, page, size);
    }

    public synchronized LobbyResponse findLobbyById(UUID id) {
        processExpiredLobbiesInternal();
        return toLobbyResponse(lobbyState(id));
    }

    public synchronized double calculateLobbyAverageRating(UUID lobbyId) {
        LobbyState lobby = lobbyState(lobbyId);
        if (lobby.playerIds.isEmpty()) {
            return 0.0;
        }
        return lobby.playerIds.stream()
                .map(this::playerState)
                .mapToInt(player -> player.rating)
                .average()
                .orElse(0.0);
    }

    public synchronized LobbyResponse joinLobby(JoinLobbyRequest request) {
        processExpiredLobbiesInternal();

        PlayerState player = playerState(request.playerId());
        String mode = normalize(request.mode());
        String region = normalize(request.region());
        String rank = normalize(request.rank());
        Integer requestedTimeoutSeconds = request.timeoutSeconds();
        int timeoutSeconds = requestedTimeoutSeconds != null ? requestedTimeoutSeconds : DEFAULT_TIMEOUT_SECONDS;

        ensureAllowedMode(mode);
        ensurePlayerCanJoinQueue(player.id);

        boolean[] created = {false};
        LobbyState lobby = lobbies.values().stream()
                .filter(candidate -> "WAITING".equals(candidate.status))
                .filter(candidate -> candidate.playerIds.size() < MAX_PLAYERS_IN_LOBBY)
                .filter(candidate -> candidate.mode.equals(mode))
                .filter(candidate -> candidate.region.equals(region))
                .filter(candidate -> candidate.rank.equals(rank))
                .min(Comparator.comparing(candidate -> candidate.createdAt))
                .orElseGet(() -> {
                    created[0] = true;
                    return createLobby(mode, region, rank, timeoutSeconds);
                });

        if (created[0]) {
            lobbyEventPublisher.publishCreated(toLobbyResponse(lobby));
        }

        lobby.playerIds.add(player.id);
        LobbyResponse afterJoin = toLobbyResponse(lobby);
        lobbyEventPublisher.publishPlayerJoined(afterJoin, toPlayerResponse(player));

        if (lobby.playerIds.size() >= MAX_PLAYERS_IN_LOBBY) {
            cancelLobbyStartTask(lobby.id);
            return formLobbyAndStartMatch(lobby.id);
        }

        if (lobby.playerIds.size() >= MIN_PLAYERS_TO_START) {
            scheduleLobbyStart(lobby.id);
        }

        return afterJoin;
    }

    public synchronized LobbyResponse disbandLobby(UUID id) {
        LobbyState lobby = lobbyState(id);
        if (!"WAITING".equals(lobby.status)) {
            throw new InvalidLobbyOperationException("Можно распустить только WAITING-лобби");
        }
        if (!lobby.playerIds.isEmpty()) {
            throw new LobbyHasPlayersException(id);
        }
        cancelLobbyStartTask(lobby.id);
        lobby.status = "DISBANDED";
        LobbyResponse response = toLobbyResponse(lobby);
        lobbyEventPublisher.publishDisbanded(response, "manual");
        return response;
    }

    public synchronized List<LobbyResponse> processExpiredLobbies() {
        return processExpiredLobbiesInternal().stream()
                .map(this::toLobbyResponse)
                .toList();
    }

    @Scheduled(fixedDelay = 5000)
    public synchronized void scheduledLobbyTimeoutCheck() {
        processExpiredLobbiesInternal();
    }

    public synchronized PagedResponse<MatchResponse> findMatches(String status,
                                                                 String region,
                                                                 String rank,
                                                                 String mode,
                                                                 int page,
                                                                 int size) {
        List<MatchResponse> all = matches.values().stream()
                .filter(match -> status == null || match.status.equalsIgnoreCase(status))
                .filter(match -> region == null || match.region.equalsIgnoreCase(region))
                .filter(match -> rank == null || match.rank.equalsIgnoreCase(rank))
                .filter(match -> mode == null || match.mode.equalsIgnoreCase(mode))
                .sorted(Comparator.comparing((MatchState match) -> match.createdAt).reversed())
                .map(this::toMatchResponse)
                .toList();
        return PagedResponse.of(all, page, size);
    }

    public synchronized MatchResponse findMatchById(UUID id) {
        return toMatchResponse(matchState(id));
    }

    public synchronized MatchResponse findMatchByLobbyId(UUID lobbyId) {
        LobbyState lobby = lobbyState(lobbyId);
        if (lobby.matchId == null) {
            return null;
        }
        return toMatchResponse(matchState(lobby.matchId));
    }

    public synchronized LobbyResponse findLobbyByMatchId(UUID matchId) {
        MatchState match = matchState(matchId);
        return toLobbyResponse(lobbyState(match.lobbyId));
    }

    private LobbyState createLobby(String mode, String region, String rank, int timeoutSeconds) {
        OffsetDateTime now = OffsetDateTime.now();
        LobbyState lobby = new LobbyState(
                UUID.randomUUID(),
                mode,
                region,
                rank,
                "WAITING",
                MIN_PLAYERS_TO_START,
                new ArrayList<>(),
                now,
                now.plusSeconds(timeoutSeconds),
                null,
                null
        );
        lobbies.put(lobby.id, lobby);
        return lobby;
    }

    private List<LobbyState> processExpiredLobbiesInternal() {
        OffsetDateTime now = OffsetDateTime.now();
        List<LobbyState> changed = new ArrayList<>();

        for (LobbyState lobby : lobbies.values()) {
            if ("WAITING".equals(lobby.status)
                    && lobby.deadlineAt.isBefore(now)
                    && lobby.playerIds.size() < lobby.minPlayers) {
                cancelLobbyStartTask(lobby.id);
                lobby.status = "DISBANDED";
                changed.add(lobby);
                lobbyEventPublisher.publishDisbanded(toLobbyResponse(lobby), "timeout");
            }
        }

        return changed;
    }

    private void ensureAllowedMode(String mode) {
        if (!ALLOWED_MODES.contains(mode)) {
            throw new InvalidLobbyOperationException(
                    "Неизвестный режим: " + mode + ". Доступные режимы: BATTLE_ROYALE, SURVIVAL, DEATHMATCH"
            );
        }
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

    private void ensurePlayerCanJoinQueue(UUID playerId) {
        boolean alreadyWaiting = lobbies.values().stream()
                .anyMatch(lobby -> "WAITING".equals(lobby.status) && lobby.playerIds.contains(playerId));

        if (alreadyWaiting) {
            throw new PlayerAlreadyInLobbyException("Игрок уже ожидает в лобби: " + playerId);
        }

        boolean alreadyPlaying = matches.values().stream()
                .anyMatch(match -> "IN_PROGRESS".equals(match.status())
                        && match.playerIds().contains(playerId));

        if (alreadyPlaying) {
            throw new InvalidLobbyOperationException("Игрок уже находится в активном матче: " + playerId);
        }
    }

    private void scheduleLobbyStart(UUID lobbyId) {
        cancelLobbyStartTask(lobbyId);
        ScheduledFuture<?> task = matchExecutor.schedule(
                () -> formLobbyAndStartMatch(lobbyId),
                LOBBY_START_DELAY_SECONDS,
                TimeUnit.SECONDS
        );
        lobbyStartTasks.put(lobbyId, task);
    }

    private void cancelLobbyStartTask(UUID lobbyId) {
        ScheduledFuture<?> task = lobbyStartTasks.remove(lobbyId);
        if (task != null) {
            task.cancel(false);
        }
    }

    private synchronized LobbyResponse formLobbyAndStartMatch(UUID lobbyId) {
        LobbyState lobby = lobbies.get(lobbyId);
        if (lobby == null || !"WAITING".equals(lobby.status) || lobby.playerIds.size() < MIN_PLAYERS_TO_START) {
            return lobby == null ? null : toLobbyResponse(lobby);
        }

        cancelLobbyStartTask(lobbyId);
        MatchState match = startMatch(lobby);
        LobbyResponse formedLobby = toLobbyResponse(lobby);
        lobbyEventPublisher.publishFormed(formedLobby, match.id());
        scheduleMatchSimulation(match.id());
        return formedLobby;
    }

    private MatchState startMatch(LobbyState lobby) {
        OffsetDateTime startedAt = OffsetDateTime.now();
        lobby.status = "FORMED";
        lobby.startedAt = startedAt;

        UUID matchId = UUID.randomUUID();
        List<UUID> matchPlayerIds = new ArrayList<>();
        matchPlayerIds.addAll(lobby.playerIds);
        List<MatchEventResponse> events = new ArrayList<>();

        MatchEventResponse startEvent = event(
                "MATCH_STARTED",
                "Матч начался. Игроков в лобби: " + matchPlayerIds.size(),
                null,
                null,
                startedAt
        );
        events.add(startEvent);

        MatchState match = new MatchState(
                matchId,
                lobby.id,
                lobby.mode,
                lobby.region,
                lobby.rank,
                "IN_PROGRESS",
                matchPlayerIds,
                events,
                null,
                startedAt,
                null
        );

        matches.put(match.id(), match);
        lobby.matchId = match.id();
        return match;
    }

    private void scheduleMatchSimulation(UUID matchId) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int durationSeconds = random.nextInt(MATCH_MIN_DURATION_SECONDS, MATCH_MAX_DURATION_SECONDS + 1);
        int progressEvents = random.nextInt(MATCH_MIN_PROGRESS_EVENTS, MATCH_MAX_PROGRESS_EVENTS + 1);

        for (Integer delaySeconds : randomProgressDelays(durationSeconds, progressEvents)) {
            matchExecutor.schedule(() -> addRandomMatchProgressEvent(matchId),
                    delaySeconds, TimeUnit.SECONDS);
        }

        matchExecutor.schedule(() -> finishMatch(matchId),
                durationSeconds, TimeUnit.SECONDS);
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

    private synchronized void addRandomMatchProgressEvent(UUID matchId) {
        MatchState match = matches.get(matchId);
        if (match == null || !"IN_PROGRESS".equals(match.status())) {
            return;
        }

        OffsetDateTime occurredAt = OffsetDateTime.now();
        List<UUID> matchPlayerIds = match.playerIds();
        PlayerState actor = randomPlayer(matchPlayerIds);
        PlayerState target = randomTarget(matchPlayerIds, actor.id);

        int eventType = ThreadLocalRandom.current().nextInt(5);
        MatchEventResponse progressEvent;

        progressEvent = switch (eventType) {
            case 0 -> event("PLAYER_HIT", actor.nickname + " попал по " + target.nickname, actor.id, target.id, occurredAt);
            case 1 -> event("PLAYER_DODGED", actor.nickname + " увернулся от атаки", actor.id, null, occurredAt);
            case 2 -> event("PLAYER_FOUND_LOOT", actor.nickname + " нашел полезный предмет", actor.id, null, occurredAt);
            case 3 -> event("ZONE_DAMAGE", actor.nickname + " получил урон от зоны", actor.id, null, occurredAt);
            default -> event("PLAYER_HEALED", actor.nickname + " использовал аптечку", actor.id, null, occurredAt);
        };

        match.events().add(progressEvent);
        matchEventPublisher.publishProgress(toMatchResponse(match), progressEvent);
    }

    private PlayerState randomPlayer(List<UUID> playerIds) {
        int index = ThreadLocalRandom.current().nextInt(playerIds.size());
        return playerState(playerIds.get(index));
    }

    private PlayerState randomTarget(List<UUID> playerIds, UUID actorId) {
        PlayerState target;
        do {
            target = randomPlayer(playerIds);
        } while (target.id.equals(actorId));
        return target;
    }

    private synchronized void finishMatch(UUID matchId) {
        MatchState match = matches.get(matchId);
        if (match == null || !"IN_PROGRESS".equals(match.status())) {
            return;
        }

        OffsetDateTime finishedAt = OffsetDateTime.now();
        List<UUID> matchPlayerIds = match.playerIds();
        PlayerState winner = randomPlayer(matchPlayerIds);

        for (UUID playerId : matchPlayerIds) {
            if (playerId.equals(winner.id)) {
                continue;
            }
            PlayerState eliminated = playerState(playerId);
            MatchEventResponse eliminationEvent = event(
                    "PLAYER_ELIMINATED",
                    winner.nickname + " выбил игрока " + eliminated.nickname,
                    winner.id,
                    eliminated.id,
                    finishedAt
            );
            match.events().add(eliminationEvent);
            matchEventPublisher.publishProgress(toMatchResponse(match), eliminationEvent);
        }

        MatchEventResponse finishEvent = event(
                "MATCH_FINISHED",
                "Матч завершен. Победитель: " + winner.nickname,
                winner.id,
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
                "FINISHED",
                match.playerIds(),
                match.events(),
                winner.id,
                match.createdAt(),
                finishedAt
        );

        matches.put(matchId, finishedMatch);
        // gRPC-цепочка остается простой. match.finished означает, что итог матча готов.
        // grpc-match-enrichment-client слушает это событие и считает изменения рейтинга.
        matchEventPublisher.publishFinished(toMatchResponse(finishedMatch));
    }

    private MatchEventResponse event(String type,
                                     String message,
                                     UUID actorPlayerId,
                                     UUID targetPlayerId,
                                     OffsetDateTime occurredAt) {
        return new MatchEventResponse(UUID.randomUUID(), type, message, actorPlayerId, targetPlayerId, occurredAt);
    }

    private PlayerState playerState(UUID id) {
        PlayerState player = players.get(id);
        if (player == null) {
            throw new ResourceNotFoundException("Игрок не найден: " + id);
        }
        return player;
    }

    private LobbyState lobbyState(UUID id) {
        LobbyState lobby = lobbies.get(id);
        if (lobby == null) {
            throw new ResourceNotFoundException("Лобби не найдено: " + id);
        }
        return lobby;
    }

    private MatchState matchState(UUID id) {
        MatchState match = matches.get(id);
        if (match == null) {
            throw new ResourceNotFoundException("Матч не найден: " + id);
        }
        return match;
    }

    private PlayerResponse toPlayerResponse(PlayerState player) {
        return new PlayerResponse(player.id, player.nickname, player.rating, player.region, player.rank, player.createdAt);
    }

    private LobbyResponse toLobbyResponse(LobbyState lobby) {
        List<PlayerResponse> lobbyPlayers = lobby.playerIds.stream()
                .map(players::get)
                .filter(player -> player != null)
                .map(this::toPlayerResponse)
                .toList();

        return new LobbyResponse(
                lobby.id,
                lobby.mode,
                lobby.region,
                lobby.rank,
                lobby.status,
                lobby.minPlayers,
                lobbyPlayers.size(),
                lobbyPlayers,
                lobby.createdAt,
                lobby.deadlineAt,
                lobby.startedAt,
                lobby.matchId
        );
    }

    private MatchResponse toMatchResponse(MatchState match) {
        List<PlayerResponse> matchPlayers = match.playerIds.stream()
                .map(players::get)
                .filter(player -> player != null)
                .map(this::toPlayerResponse)
                .toList();

        PlayerResponse winner = match.winnerPlayerId == null ? null : toPlayerResponse(playerState(match.winnerPlayerId));

        return new MatchResponse(
                match.id,
                match.lobbyId,
                match.mode,
                match.region,
                match.rank,
                match.status,
                matchPlayers,
                match.events,
                winner,
                match.createdAt,
                match.finishedAt
        );
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private record PlayerState(
            UUID id,
            String nickname,
            int rating,
            String region,
            String rank,
            OffsetDateTime createdAt
    ) {}

    private static class LobbyState {
        private final UUID id;
        private final String mode;
        private final String region;
        private final String rank;
        private String status;
        private final int minPlayers;
        private final List<UUID> playerIds;
        private final OffsetDateTime createdAt;
        private final OffsetDateTime deadlineAt;
        private OffsetDateTime startedAt;
        private UUID matchId;

        private LobbyState(UUID id,
                           String mode,
                           String region,
                           String rank,
                           String status,
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
    }

    private record MatchState(
            UUID id,
            UUID lobbyId,
            String mode,
            String region,
            String rank,
            String status,
            @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
            List<UUID> playerIds,
            List<MatchEventResponse> events,
            UUID winnerPlayerId,
            OffsetDateTime createdAt,
            OffsetDateTime finishedAt
    ) {}
}
