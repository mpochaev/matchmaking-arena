package edu.rutmiit.pochaev.service;

import edu.rutmiit.pochaev.event.LobbyEventPublisher;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.JoinLobbyRequest;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.LobbyResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PagedResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PlayerResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.LobbyStatus;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchMode;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchStatus;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;
import edu.rutmiit.pochaev.matchmakingapicontract.exception.InvalidLobbyOperationException;
import edu.rutmiit.pochaev.matchmakingapicontract.exception.LobbyHasPlayersException;
import edu.rutmiit.pochaev.matchmakingapicontract.exception.PlayerAlreadyInLobbyException;
import edu.rutmiit.pochaev.matchmakingapicontract.exception.ResourceNotFoundException;
import edu.rutmiit.pochaev.model.LobbyState;
import edu.rutmiit.pochaev.model.MatchState;
import edu.rutmiit.pochaev.model.PlayerState;
import edu.rutmiit.pochaev.storage.MatchmakingStorage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class LobbyService {

    private static final int MIN_PLAYERS_TO_START = 3;
    private static final int MAX_PLAYERS_IN_LOBBY = 5;
    private static final int LOBBY_START_DELAY_SECONDS = 10;
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final MatchmakingStorage storage;
    private final PlayerService playerService;
    private final MatchService matchService;
    private final LobbyEventPublisher lobbyEventPublisher;

    public LobbyService(MatchmakingStorage storage,
                        PlayerService playerService,
                        MatchService matchService,
                        LobbyEventPublisher lobbyEventPublisher) {
        this.storage = storage;
        this.playerService = playerService;
        this.matchService = matchService;
        this.lobbyEventPublisher = lobbyEventPublisher;
    }

    public PagedResponse<LobbyResponse> findLobbies(LobbyStatus status,
                                                    Region region,
                                                    Rank rank,
                                                    MatchMode mode,
                                                    int page,
                                                    int size) {
        processExpiredLobbiesInternal();
        var all = storage.lobbies.values().stream()
                .filter(lobby -> status == null || lobby.getStatus() == status)
                .filter(lobby -> region == null || lobby.getRegion() == region)
                .filter(lobby -> rank == null || lobby.getRank() == rank)
                .filter(lobby -> mode == null || lobby.getMode() == mode)
                .sorted(Comparator.comparing(LobbyState::getCreatedAt).reversed())
                .map(this::toLobbyResponse)
                .toList();
        return PagedResponse.of(all, page, size);
    }

    public PagedResponse<LobbyResponse> findLobbiesByRegion(Region region, int page, int size) {
        return findLobbies(null, region, null, null, page, size);
    }

    public LobbyResponse findLobbyById(UUID id) {
        processExpiredLobbiesInternal();
        return toLobbyResponse(lobbyState(id));
    }

    public double calculateLobbyAverageRating(UUID lobbyId) {
        LobbyState lobby = lobbyState(lobbyId);
        if (lobby.getPlayerIds().isEmpty()) {
            return 0.0;
        }
        return lobby.getPlayerIds().stream()
                .map(playerService::playerState)
                .mapToInt(PlayerState::rating)
                .average()
                .orElse(0.0);
    }

    public LobbyResponse joinLobby(JoinLobbyRequest request) {
        processExpiredLobbiesInternal();

        PlayerState player = playerService.playerState(request.playerId());
        MatchMode mode = request.mode();
        Region region = request.region();
        Rank rank = player.rank();
        int timeoutSeconds = request.timeoutSeconds() == null ? DEFAULT_TIMEOUT_SECONDS : request.timeoutSeconds();

        ensurePlayerCanJoinQueue(player.id());

        boolean[] created = {false};
        LobbyState lobby = storage.lobbies.values().stream()
                .filter(candidate -> candidate.getStatus() == LobbyStatus.WAITING)
                .filter(candidate -> candidate.getPlayerIds().size() < MAX_PLAYERS_IN_LOBBY)
                .filter(candidate -> candidate.getMode() == mode)
                .filter(candidate -> candidate.getRegion() == region)
                .filter(candidate -> candidate.getRank() == rank)
                .min(Comparator.comparing(LobbyState::getCreatedAt))
                .orElseGet(() -> {
                    created[0] = true;
                    return createLobby(mode, region, rank, timeoutSeconds);
                });

        if (created[0]) {
            lobbyEventPublisher.publishCreated(toLobbyResponse(lobby));
        }

        lobby.getPlayerIds().add(player.id());
        LobbyResponse afterJoin = toLobbyResponse(lobby);
        lobbyEventPublisher.publishPlayerJoined(afterJoin, playerService.toPlayerResponse(player));

        if (lobby.getPlayerIds().size() >= MAX_PLAYERS_IN_LOBBY) {
            cancelLobbyStartTask(lobby.getId());
            return formLobbyAndStartMatch(lobby.getId());
        }

        if (lobby.getPlayerIds().size() >= MIN_PLAYERS_TO_START) {
            scheduleLobbyStart(lobby.getId());
        }

        return afterJoin;
    }

    public LobbyResponse leaveLobby(UUID lobbyId, UUID playerId) {
        LobbyState lobby = lobbyState(lobbyId);
        if (lobby.getStatus() != LobbyStatus.WAITING) {
            throw new InvalidLobbyOperationException("Выйти можно только из WAITING-лобби");
        }
        if (!lobby.getPlayerIds().remove(playerId)) {
            throw new InvalidLobbyOperationException("Игрок не находится в этом лобби: " + playerId);
        }

        if (lobby.getPlayerIds().size() < MIN_PLAYERS_TO_START) {
            cancelLobbyStartTask(lobby.getId());
        }
        if (lobby.getPlayerIds().isEmpty()) {
            lobby.setStatus(LobbyStatus.DISBANDED);
            lobbyEventPublisher.publishDisbanded(toLobbyResponse(lobby), "empty");
        }

        return toLobbyResponse(lobby);
    }

    public LobbyResponse disbandLobby(UUID id) {
        LobbyState lobby = lobbyState(id);
        if (lobby.getStatus() != LobbyStatus.WAITING) {
            throw new InvalidLobbyOperationException("Можно распустить только WAITING-лобби");
        }
        if (!lobby.getPlayerIds().isEmpty()) {
            throw new LobbyHasPlayersException(id);
        }
        cancelLobbyStartTask(lobby.getId());
        lobby.setStatus(LobbyStatus.DISBANDED);
        LobbyResponse response = toLobbyResponse(lobby);
        lobbyEventPublisher.publishDisbanded(response, "manual");
        return response;
    }

    public List<LobbyResponse> processExpiredLobbies() {
        return processExpiredLobbiesInternal().stream()
                .map(this::toLobbyResponse)
                .toList();
    }

    @Scheduled(fixedDelay = 5000)
    public void scheduledLobbyTimeoutCheck() {
        processExpiredLobbiesInternal();
    }

    public LobbyResponse findLobbyByMatchId(UUID matchId) {
        MatchState match = matchService.matchState(matchId);
        return toLobbyResponse(lobbyState(match.lobbyId()));
    }

    public LobbyState lobbyState(UUID id) {
        LobbyState lobby = storage.lobbies.get(id);
        if (lobby == null) {
            throw new ResourceNotFoundException("Лобби не найдено: " + id);
        }
        return lobby;
    }

    public LobbyResponse toLobbyResponse(LobbyState lobby) {
        List<PlayerResponse> lobbyPlayers = lobby.getPlayerIds().stream()
                .map(storage.players::get)
                .filter(player -> player != null)
                .map(playerService::toPlayerResponse)
                .toList();

        return new LobbyResponse(
                lobby.getId(),
                lobby.getMode(),
                lobby.getRegion(),
                lobby.getRank(),
                lobby.getStatus(),
                lobby.getMinPlayers(),
                lobbyPlayers.size(),
                lobbyPlayers,
                lobby.getCreatedAt(),
                lobby.getDeadlineAt(),
                lobby.getStartedAt(),
                lobby.getMatchId()
        );
    }

    private LobbyState createLobby(MatchMode mode, Region region, Rank rank, int timeoutSeconds) {
        OffsetDateTime now = OffsetDateTime.now();
        LobbyState lobby = new LobbyState(
                UUID.randomUUID(),
                mode,
                region,
                rank,
                LobbyStatus.WAITING,
                MIN_PLAYERS_TO_START,
                new CopyOnWriteArrayList<>(),
                now,
                now.plusSeconds(timeoutSeconds),
                null,
                null
        );
        storage.lobbies.put(lobby.getId(), lobby);
        return lobby;
    }

    private List<LobbyState> processExpiredLobbiesInternal() {
        OffsetDateTime now = OffsetDateTime.now();
        var changed = new java.util.ArrayList<LobbyState>();

        for (LobbyState lobby : storage.lobbies.values()) {
            if (lobby.getStatus() == LobbyStatus.WAITING
                    && lobby.getDeadlineAt().isBefore(now)
                    && lobby.getPlayerIds().size() < lobby.getMinPlayers()) {
                cancelLobbyStartTask(lobby.getId());
                lobby.setStatus(LobbyStatus.DISBANDED);
                changed.add(lobby);
                lobbyEventPublisher.publishDisbanded(toLobbyResponse(lobby), "timeout");
            }
        }

        return changed;
    }

    private void ensurePlayerCanJoinQueue(UUID playerId) {
        boolean alreadyWaiting = storage.lobbies.values().stream()
                .anyMatch(lobby -> lobby.getStatus() == LobbyStatus.WAITING && lobby.getPlayerIds().contains(playerId));

        if (alreadyWaiting) {
            throw new PlayerAlreadyInLobbyException("Игрок уже ожидает в лобби: " + playerId);
        }

        boolean alreadyPlaying = storage.matches.values().stream()
                .anyMatch(match -> match.status() == MatchStatus.IN_PROGRESS && match.playerIds().contains(playerId));

        if (alreadyPlaying) {
            throw new InvalidLobbyOperationException("Игрок уже находится в активном матче: " + playerId);
        }
    }

    private void scheduleLobbyStart(UUID lobbyId) {
        cancelLobbyStartTask(lobbyId);
        ScheduledFuture<?> task = storage.matchExecutor.schedule(
                () -> formLobbyAndStartMatch(lobbyId),
                LOBBY_START_DELAY_SECONDS,
                TimeUnit.SECONDS
        );
        storage.lobbyStartTasks.put(lobbyId, task);
    }

    private void cancelLobbyStartTask(UUID lobbyId) {
        ScheduledFuture<?> task = storage.lobbyStartTasks.remove(lobbyId);
        if (task != null) {
            task.cancel(false);
        }
    }

    private LobbyResponse formLobbyAndStartMatch(UUID lobbyId) {
        LobbyState lobby = storage.lobbies.get(lobbyId);
        if (lobby == null || lobby.getStatus() != LobbyStatus.WAITING || lobby.getPlayerIds().size() < MIN_PLAYERS_TO_START) {
            return lobby == null ? null : toLobbyResponse(lobby);
        }

        cancelLobbyStartTask(lobbyId);
        MatchState match = matchService.startMatch(lobby);
        LobbyResponse formedLobby = toLobbyResponse(lobby);
        lobbyEventPublisher.publishFormed(formedLobby, match.id());
        matchService.scheduleMatchSimulation(match.id());
        return formedLobby;
    }
}
