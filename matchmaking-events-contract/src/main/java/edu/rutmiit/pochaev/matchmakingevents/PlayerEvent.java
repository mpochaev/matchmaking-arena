package edu.rutmiit.pochaev.matchmakingevents;

import java.util.UUID;

/** События, связанные с игроками. */
public sealed interface PlayerEvent {

    record Created(
            UUID playerId,
            String nickname,
            int rating,
            String region,
            String rank
    ) implements PlayerEvent {}
}
