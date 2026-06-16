package edu.rutmiit.pochaev.storage;

import edu.rutmiit.pochaev.model.LobbyState;
import edu.rutmiit.pochaev.model.MatchState;
import edu.rutmiit.pochaev.model.PlayerState;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

@Component
public class MatchmakingStorage {

    public final ConcurrentMap<UUID, PlayerState> players = new ConcurrentHashMap<>();
    public final ConcurrentMap<UUID, LobbyState> lobbies = new ConcurrentHashMap<>();
    public final ConcurrentMap<UUID, MatchState> matches = new ConcurrentHashMap<>();
    public final ConcurrentMap<UUID, ScheduledFuture<?>> lobbyStartTasks = new ConcurrentHashMap<>();
    public final ScheduledExecutorService matchExecutor = Executors.newSingleThreadScheduledExecutor();
}
