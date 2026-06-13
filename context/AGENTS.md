# AGENTS.md – Matchmaking Arena

This file is written for future AI coding agents working on **Matchmaking Arena**, especially Claude Opus-style agents. It explains the project, the architecture, the domain rules, the intended course scenario, the important simplifications, and the safest way to modify the code without overengineering it.

The user expects this project to stay close to the reference course examples. The most important rule is: **keep everything simple, student-level, predictable, and easy to explain**.

---

## 1. Project identity

**Application name:** `Matchmaking Arena`

**Subject area:** game matchmaking.

The system demonstrates service-oriented programming patterns using a small training domain:

1. Players are created.
2. Players join lobbies.
3. A lobby waits 10 seconds after the 3rd player, restarts the timer after the 4th player, and starts immediately at 5 players.
4. The match runs for a short random demo duration.
5. Match progress events are published.
6. The match finishes with one winner.
7. gRPC calculates post-match rating/rank changes.
8. RabbitMQ distributes domain events to independent services.
9. A match log service stores a simple in-memory history.
10. A notification service pushes live events to a browser through WebSocket.

This is not a production game server. It is a **course demonstration** of REST, GraphQL, RabbitMQ, gRPC, and WebSocket in one understandable distributed system.

---

## 2. Non-negotiable development principles

### Keep the project simple

Do not add production-grade complexity unless explicitly requested. Avoid:

- databases;
- authentication / JWT;
- real user sessions;
- complex game simulation;
- Docker Compose unless explicitly requested;
- separate API Gateway;
- frontend frameworks;
- backend persistence for notifications;
- background schedulers beyond the existing simple demo logic;
- complicated event sourcing, CQRS, sagas, or distributed transactions.

The project intentionally uses in-memory storage and simple Java collections to stay readable.

### Stay close to the reference project

The course reference uses simple services, simple contracts, simple queues, and simple browser UI. Matchmaking Arena should follow the same spirit.

Mapping from reference project to this project:

| Reference concept | Matchmaking Arena concept |
|---|---|
| `demo-rest` | `matchmaking` |
| `books-api-contract` | `matchmaking-api-contract` / module `api-contract` |
| `books-events-contract` | `matchmaking-events-contract` |
| `books-grpc-contract` | `matchmaking-grpc-contract` |
| `audit-service` | `match-log-service` |
| `grpc-analytics-server` | `grpc-match-analytics-server` |
| `grpc-enrichment-client` | `grpc-match-enrichment-client` |
| `notification-service` | `notification-service` |

### Runtime logs should be Russian

Runtime Java logs and simple code comments are written in Russian to make the demo easier to read and explain during defense. Keep log messages short, clear and minimal. Avoid noisy long messages and avoid adding unnecessary technical detail.

README, AGENTS and explanatory docs should stay aligned with the current event names and Russian console output.

### Use `OffsetDateTime`, not `LocalDateTime`

A prior requirement was to replace `LocalDateTime` with `OffsetDateTime`. Do not reintroduce `LocalDateTime` in DTOs, events, or service state unless the user explicitly asks.

---

## 3. Root layout

Root project is a Maven aggregator POM:

```text
Matchmaking Arena/
├── api-contract/
├── matchmaking-events-contract/
├── matchmaking-grpc-contract/
├── matchmaking/
├── match-log-service/
├── grpc-match-analytics-server/
├── grpc-match-enrichment-client/
├── notification-service/
├── context/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

Root `pom.xml` has packaging `pom` and lists modules in dependency order:

```text
api-contract
matchmaking-events-contract
matchmaking-grpc-contract
matchmaking
match-log-service
grpc-match-analytics-server
grpc-match-enrichment-client
notification-service
```

Important: root POM is only an aggregator. Individual modules inherit from `spring-boot-starter-parent` where needed.

---

## 4. Module responsibilities

### 4.1 `api-contract`

**Purpose:** REST and GraphQL-adjacent API contract module.

Contains:

```text
edu.rutmiit.pochaev.matchmakingapicontract.dto
edu.rutmiit.pochaev.matchmakingapicontract.endpoints
edu.rutmiit.pochaev.matchmakingapicontract.exception
src/main/resources/graphql/schema.graphqls
```

Key DTOs:

- `PlayerRequest`
- `PlayerResponse`
- `JoinLobbyRequest`
- `LobbyResponse`
- `MatchResponse`
- `MatchEventResponse`
- `PagedResponse`
- `PageInfoResponse`
- `ErrorResponse`

REST endpoint interfaces:

- `PlayerApi`
- `LobbyApi`
- `MatchApi`

Important REST endpoints:

```text
GET  /api/players
POST /api/players
GET  /api/players/{id}

GET  /api/lobbies
POST /api/lobbies/join
GET  /api/lobbies/{id}
POST /api/lobbies/{id}/disband
POST /api/lobbies/process-timeouts

GET  /api/matches
GET  /api/matches/{id}
```

This module is a library and should not run as a Spring Boot application. Its `spring-boot-maven-plugin` should remain skipped.

### 4.2 `matchmaking-events-contract`

**Purpose:** shared RabbitMQ event contract.

Contains:

- `EventEnvelope<T>`
- `EventMetadata`
- `RoutingKeys`
- `PlayerEvent`
- `LobbyEvent`
- `MatchEvent`

All RabbitMQ messages are wrapped as:

```java
EventEnvelope<T>(metadata, payload)
```

Metadata contains:

```text
eventId
timestamp
source
eventType
```

Important: `eventId` is not `playerId`, `lobbyId`, or `matchId`. It identifies one RabbitMQ event/message and is used for deduplication and traceability.

Routing keys:

```text
player.created
lobby.created
lobby.player-joined
lobby.formed
lobby.disbanded
match.progress
match.finished
match.enriched
```

Exchange:

```text
matchmaking.events
```

Wildcard constants:

```text
player.*
lobby.*
match.*
#
```

### 4.3 `matchmaking-grpc-contract`

**Purpose:** contract-first gRPC module.

Main file:

```text
matchmaking-grpc-contract/src/main/proto/match_analytics.proto
```

Defines service:

```proto
service MatchRankEnrichment {
    rpc CalculateRankChanges (CalculateRankChangesRequest) returns (RankChangesResponse);
}
```

Generated Java classes are used by:

- `grpc-match-analytics-server`
- `grpc-match-enrichment-client`

Do not manually edit generated classes under `target/generated-sources`.

### 4.4 `matchmaking`

**Purpose:** main service / core domain service.

Ports:

```text
HTTP: 8080
Swagger:  http://localhost:8080/swagger-ui.html
GraphiQL: http://localhost:8080/graphiql
GraphQL:  http://localhost:8080/graphql
```

Contains:

- REST controllers implementing `PlayerApi`, `LobbyApi`, `MatchApi`.
- GraphQL DGS fetchers.
- in-memory business logic in `MatchmakingService`.
- RabbitMQ event publishers.

This is the only service where players, lobbies and matches are created. It also listens to `match.enriched` and updates the in-memory ratings of players after gRPC calculation.

### 4.5 `match-log-service`

**Purpose:** domain log service, equivalent to the reference audit service but renamed to match the domain.

Port:

```text
HTTP: 8081
GET http://localhost:8081/api/match-log
```

Rabbit queue:

```text
q.matchmaking.match-log.events
q.matchmaking.match-log.events.dlq
```

Binding:

```text
#
```

It receives all events and stores simple in-memory `MatchLogEntry` records. It does not calculate ratings. It does not own the domain. It is only a listener and log viewer.

### 4.6 `grpc-match-analytics-server`

**Purpose:** gRPC server that calculates post-match rating/rank changes.

Ports:

```text
HTTP actuator: 8083
gRPC: 9090
```

gRPC method:

```text
MatchRankEnrichment.CalculateRankChanges
```

Logic is intentionally simple:

```text
winner: +25 rating
losers: -10 rating
rating cannot go below 0
rank is recalculated by rating thresholds
```

It does not listen to RabbitMQ. It only responds to gRPC calls.

### 4.7 `grpc-match-enrichment-client`

**Purpose:** bridge from RabbitMQ to gRPC and back to RabbitMQ.

Port:

```text
HTTP actuator: 8082
```

Rabbit queue:

```text
q.matchmaking.enrichment.match-finished
q.matchmaking.enrichment.match-finished.dlq
```

Binding:

```text
match.finished
```

Flow:

```text
RabbitMQ match.finished
→ MatchFinishedListener
→ gRPC CalculateRankChanges
→ MatchEnrichmentEventPublisher
→ RabbitMQ match.enriched
```

It publishes the enriched event. The main `matchmaking` service then listens to `match.enriched` and updates player ratings in its own in-memory `players` map.

### 4.8 `notification-service`

**Purpose:** WebSocket notification service.

Ports:

```text
HTTP UI: 8084
WebSocket: ws://localhost:8084/ws/notifications
```

Rabbit queue:

```text
q.matchmaking.notifications.all
q.matchmaking.notifications.all.dlq
```

Binding:

```text
#
```

It listens to all domain events and pushes browser notifications. The browser UI also keeps the latest 50 notification cards in `localStorage`, so a simple page refresh does not immediately clear the visible list. This is still only lightweight browser-side storage. `match-log-service` remains the proper in-memory event history for the project.

Static UI:

```text
notification-service/src/main/resources/static/index.html
```

The UI uses native WebSocket API. No React/Vue/Angular.

---

## 5. Domain model and rules

### Players

A player has:

```text
id
nickname
rating
region
rank
createdAt
```

Seed demo players are created in `MatchmakingService` constructor without publishing events:

```text
Artem   1210 EU SILVER
Misha   1180 EU SILVER
Sasha   1270 EU SILVER
Dima    1600 EU GOLD
```

### Rank rules

Rank is determined by rating:

```text
BRONZE:   rating < 1000
SILVER:   1000–1499
GOLD:     1500–1999
PLATINUM: 2000–2499
DIAMOND:  2500+
```

On player creation, the server should calculate rank by rating. Do not trust the incoming `rank` if it contradicts `rating`.

The gRPC analytics server uses the same rule after rating changes.

### Modes

Allowed modes:

```text
BATTLE_ROYALE
SURVIVAL
DEATHMATCH
```

These modes intentionally have the same logic. They exist to demonstrate that matchmaking queues can be separated by mode.

Do not reintroduce `DUO`; it conflicts with the 3-player rule.

Do not reintroduce `ARENA` unless explicitly asked; current replacement is `DEATHMATCH`.

### Lobby grouping

Players are grouped into lobbies by exact normalized triple:

```text
mode + region + rank
```

A player asking for `BATTLE_ROYALE/EU/SILVER` joins only a waiting lobby with the same values.

A player asking for `BATTLE_ROYALE/EU/GOLD` goes to a different lobby.

A player asking for `SURVIVAL/EU/SILVER` goes to a different lobby.

### Lobby lifecycle

Lobby statuses:

```text
WAITING
FORMED
DISBANDED
```

Rules:

- A lobby starts as `WAITING`.
- If it reaches 3 players, the service schedules match start after 10 seconds.
- If a fourth player joins before the timer ends, the 10-second timer restarts.
- If the lobby reaches 5 players, it immediately becomes `FORMED` and starts a match.
- A sixth player cannot join this lobby because `MAX_PLAYERS_IN_LOBBY = 5`.
- Waiting lobbies may time out and become `DISBANDED` if they do not reach 3 players.
- Manual disband is allowed only for empty `WAITING` lobbies.

### Player queue restrictions

A player must not be allowed to:

- join a second `WAITING` lobby;
- join a new lobby while already in an `IN_PROGRESS` match.

Expected check in `MatchmakingService`:

```text
ensurePlayerCanJoinQueue(playerId)
```

It should check both waiting lobbies and active matches. After a match becomes `FINISHED`, the player may join a new lobby again.

### Match lifecycle

Match statuses:

```text
IN_PROGRESS
FINISHED
```

Match starts when a lobby has at least 3 players and the 10-second waiting timer finishes, or immediately when the lobby reaches 5 players.

Current demo simulation:

```text
match duration: random 60–120 seconds
random progress events: 4–8
internal match history events: MATCH_STARTED, PLAYER_ELIMINATED events, MATCH_FINISHED
winner: random among match players
```

Random progress event types:

```text
PLAYER_HIT
PLAYER_DODGED
PLAYER_FOUND_LOOT
ZONE_DAMAGE
PLAYER_HEALED
```

Finish logic:

- choose random winner;
- publish `PLAYER_ELIMINATED` progress events for losers;
- keep `MATCH_FINISHED` only inside `match.events`;
- publish final `match.finished` event as a completed match snapshot.

Important naming nuance: `match.finished` currently means “final match snapshot is ready” and is intentionally used by the gRPC enrichment client. Do not rename it casually unless all consumers are updated.

### gRPC enrichment rules

After `match.finished`:

```text
grpc-match-enrichment-client receives match.finished
→ calls grpc-match-analytics-server
→ server calculates rank changes
→ client publishes match.enriched
```

Rating calculation:

```text
winner: +25
losers: -10
```

Important intentional simplification:

`match.enriched` is now also consumed by `matchmaking`. The service reads rating changes from the event and updates the in-memory `players` map. After enrichment is processed, `GET /api/players` can show changed ratings and ranks. Keep this implementation simple and local to `MatchmakingService`.

---

## 6. Transport architecture

### REST

REST is implemented in `matchmaking` through controller classes implementing interfaces from `api-contract`.

The contract contains URL paths, HTTP methods, validation, and Swagger annotations. Controllers should not duplicate endpoint annotations unless necessary.

REST is tested through Swagger:

```text
http://localhost:8080/swagger-ui.html
```

### GraphQL

GraphQL is implemented with Netflix DGS in `matchmaking`.

Schema:

```text
api-contract/src/main/resources/graphql/schema.graphqls
```

Main DGS pieces:

```text
PlayerDataFetcher
LobbyDataFetcher
MatchDataFetcher
LobbyMatchDataFetcher
MatchLobbyDataFetcher
LobbyAverageRatingDataFetcher
GraphQLExceptionHandler
GraphQLSecurityConfig
DateTimeScalarRegistration
```

Features:

- Queries for players, lobbies, matches.
- Mutations for `createPlayer`, `joinLobby`, `disbandLobby`, `processLobbyTimeouts`.
- Nested fields: `Lobby.match`, `Match.lobby`.
- Computed field: `Lobby.averageRating`.
- DateTime scalar for `OffsetDateTime`.
- GraphQL errors are returned in the `errors` array.
- Max query depth and complexity instrumentation are present.

Do not put business logic into GraphQL fetchers. Fetchers should map inputs and call `MatchmakingService`.

### RabbitMQ

RabbitMQ is the asynchronous event bus.

Docker command:

```powershell
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management
```

Management UI:

```text
http://localhost:15672
login: guest
password: guest
```

Main exchange:

```text
matchmaking.events    type=topic
```

Dead-letter exchange:

```text
matchmaking.events.dlx    type=direct
```

Queues:

```text
q.matchmaking.match-log.events
q.matchmaking.match-log.events.dlq
q.matchmaking.enrichment.match-finished
q.matchmaking.enrichment.match-finished.dlq
q.matchmaking.notifications.all
q.matchmaking.notifications.all.dlq
```

Bindings:

```text
q.matchmaking.match-log.events           -> #
q.matchmaking.enrichment.match-finished   -> match.finished
q.matchmaking.notifications.all          -> #
```

If RabbitMQ shows `Ready = 0`, this is usually good: consumers processed messages quickly.

### gRPC

gRPC is a synchronous internal service-to-service call.

The actual gRPC call happens only here:

```text
grpc-match-enrichment-client → grpc-match-analytics-server
```

It is triggered by RabbitMQ but not performed through RabbitMQ.

The gRPC server listens on:

```text
localhost:9090
```

It also has a normal Spring Boot HTTP actuator server on:

```text
localhost:8083
```

Do not try to open `localhost:9090` in a browser.

### WebSocket

WebSocket is used for push notifications to the browser.

Service:

```text
notification-service
```

UI:

```text
http://localhost:8084/
```

WebSocket endpoint:

```text
ws://localhost:8084/ws/notifications
```

Behavior:

- Browser connects and keeps a persistent connection.
- `notification-service` listens to RabbitMQ events.
- It broadcasts JSON notification payloads to all connected browser sessions.
- `Ping` button sends `ping`; server replies `PONG`.
- UI reconnects with exponential backoff.
- The browser keeps up to 50 latest notifications in `localStorage`.
- Refreshing the page restores these latest browser-side notifications.
- Backend history still belongs to `match-log-service`, not to `notification-service`.

Notification UI should show important domain events:

```text
Игрок создан
Лобби создано
Игрок вошел в лобби
Лобби сформировано
Событие матча
Матч завершен
Рейтинг пересчитан
```

`MATCH_STARTED` and `MATCH_FINISHED` are kept only inside `match.events` as match history entries. They are not published as RabbitMQ `match.progress` events.

---

## 7. Standard run commands

Use Java 21.

### Build

From project root:

```powershell
.\mvnw.cmd clean install -DskipTests
```

If using Linux/macOS:

```bash
./mvnw clean install -DskipTests
```

### Start RabbitMQ

If container already exists:

```powershell
docker start rabbitmq
```

If not:

```powershell
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management
```

### Start services

Use separate terminals:

```powershell
.\mvnw.cmd spring-boot:run -pl matchmaking -am -DskipTests
```

```powershell
.\mvnw.cmd spring-boot:run -pl match-log-service -am -DskipTests
```

```powershell
.\mvnw.cmd spring-boot:run -pl grpc-match-analytics-server -am -DskipTests
```

```powershell
.\mvnw.cmd spring-boot:run -pl grpc-match-enrichment-client -am -DskipTests
```

```powershell
.\mvnw.cmd spring-boot:run -pl notification-service -am -DskipTests
```

If port `8084` is occupied, run notification-service on 8085:

```powershell
.\mvnw.cmd spring-boot:run -pl notification-service -am -DskipTests -Dspring-boot.run.arguments="--server.port=8085"
```

Then open:

```text
http://localhost:8085/
```

### Useful URLs

```text
RabbitMQ UI:          http://localhost:15672
Swagger:              http://localhost:8080/swagger-ui.html
GraphiQL:             http://localhost:8080/graphiql
GraphQL endpoint:     http://localhost:8080/graphql
Match log:            http://localhost:8081/api/match-log
Notification center:  http://localhost:8084/
```

---

## 8. Basic manual test scenario

### 8.1 Test with Swagger

1. Open Swagger:

```text
http://localhost:8080/swagger-ui.html
```

2. Create or use three players with the same `region` and rank-compatible `rating`.

Example `POST /api/players`:

```json
{
  "nickname": "TestA",
  "rating": 1210,
  "region": "EU",
  "rank": "SILVER"
}
```

The service should calculate rank by rating. If `rating=1510`, response rank should be `GOLD` even if request says `BRONZE`.

3. Add three players to one lobby with `POST /api/lobbies/join`:

```json
{
  "playerId": "PUT_PLAYER_UUID_HERE",
  "mode": "BATTLE_ROYALE",
  "region": "EU",
  "rank": "SILVER",
  "timeoutSeconds": 60
}
```

First join:

```text
lobby.status = WAITING
playerCount = 1
```

Second join:

```text
lobby.status = WAITING
playerCount = 2
```

Third join:

```text
lobby.status = WAITING
playerCount = 3
matchId is absent until the 10-second timer finishes
```

4. Open notification center before or during the test:

```text
http://localhost:8084/
```

Expected notification cards:

```text
Игрок создан
Лобби создано
Игрок вошел в лобби
Игрок вошел в лобби
Игрок вошел в лобби
Лобби сформировано
Событие матча
...
Матч завершен
Рейтинг пересчитан
```

5. Open match log:

```text
http://localhost:8081/api/match-log
```

It should contain full event history.

6. Open RabbitMQ:

```text
http://localhost:15672
```

Check queues. `Ready = 0` is normal if consumers are running.

### 8.2 Test GraphQL

Open:

```text
http://localhost:8080/graphiql
```

Query players:

```graphql
{
  players(page: 0, size: 10) {
    content {
      id
      nickname
      rating
      region
      rank
    }
    totalElements
    pageInfo {
      pageNumber
      totalPages
      last
    }
  }
}
```

Join lobby:

```graphql
mutation {
  joinLobby(input: {
    playerId: "PUT_PLAYER_UUID_HERE"
    mode: "BATTLE_ROYALE"
    region: "EU"
    rank: "SILVER"
    timeoutSeconds: 60
  }) {
    id
    status
    playerCount
    players { nickname }
    match {
      id
      status
      winner { nickname }
      events { type message }
    }
  }
}
```

Query lobbies with match:

```graphql
{
  lobbies(page: 0, size: 10) {
    content {
      id
      mode
      region
      rank
      status
      playerCount
      averageRating
      players { nickname rating rank }
      match {
        id
        status
        winner { nickname }
        events { type message }
      }
    }
  }
}
```

---

## 9. Common troubleshooting

### `Port 8084 was already in use`

Often caused by `plasticd.exe` / Plastic SCM / Unity Version Control using port 8084.

Check:

```powershell
netstat -ano | findstr :8084
```

If killing the process is denied, either run PowerShell as administrator or start notification-service on port 8085:

```powershell
.\mvnw.cmd spring-boot:run -pl notification-service -am -DskipTests -Dspring-boot.run.arguments="--server.port=8085"
```

### Old RabbitMQ queues/exchanges remain after code changes

RabbitMQ state lives inside the container. To reset for a clean demo:

```powershell
docker stop rabbitmq
docker rm rabbitmq
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management
```

### Maven uses old contract classes

If module contracts changed and compile errors mention old constructors or missing records, rebuild all modules:

```powershell
.\mvnw.cmd clean install -DskipTests
```

If necessary, remove old local Maven cache entries:

```powershell
Remove-Item -Recurse -Force "$env:USERPROFILE\.m2\repository\edu\rutmiit\demo\matchmaking-events-contract"
Remove-Item -Recurse -Force "$env:USERPROFILE\.m2\repository\edu\rutmiit\demo\matchmaking-grpc-contract"
Remove-Item -Recurse -Force "$env:USERPROFILE\.m2\repository\edu\rutmiit\demo\matchmaking-api-contract"
```

Then rebuild.

### Protobuf plugin cannot delete `target/protoc-dependencies`

This may happen on Windows, especially inside Dropbox or while IDEA indexes files.

Recommended fix:

1. Stop all Java services.
2. Close IDEA.
3. Pause Dropbox sync.
4. Delete:

```powershell
Remove-Item -Recurse -Force .\matchmaking-grpc-contract\target
```

or:

```powershell
cmd /c rmdir /s /q matchmaking-grpc-contract\target
```

Then:

```powershell
.\mvnw.cmd clean install -DskipTests
```

Best practice: work outside Dropbox, for example `C:\SOP\Matchmaking_Arena`.

### Cyrillic logs look broken in Windows terminal

The project now uses Russian runtime logs. If a Windows terminal shows broken Cyrillic, switch the terminal/IDE encoding to UTF-8 or run services in an IntelliJ terminal configured for UTF-8. Do not rewrite logs back to English just to hide encoding problems.

### Notification page after refresh

The UI stores up to 50 latest notification cards in browser `localStorage`. A simple refresh should restore them. If a full runtime history is needed, use `match-log-service`.

---

## 10. How to modify the project safely

### If changing REST API

1. Change contract first in `api-contract`.
2. Update corresponding controller in `matchmaking`.
3. Update service method if needed.
4. Update Swagger/examples in README if needed.
5. Rebuild whole project.

Do not put endpoint annotations only in controllers if the endpoint belongs in the contract interface.

### If adding/changing RabbitMQ events

Update in this order:

1. `matchmaking-events-contract`:
   - event record in `PlayerEvent`, `LobbyEvent`, or `MatchEvent`;
   - routing key in `RoutingKeys`.
2. Publisher in `matchmaking` or relevant service.
3. Consumer parsing logic:
   - `match-log-service`;
   - `notification-service`;
   - `grpc-match-enrichment-client` if it needs the event.
4. Rabbit binding if this event needs a new queue or different routing.
5. Rebuild all modules.

Do not hardcode routing key strings in multiple services. Prefer `RoutingKeys` constants.

### If changing gRPC contract

1. Edit `matchmaking-grpc-contract/src/main/proto/match_analytics.proto`.
2. Rebuild:

```powershell
.\mvnw.cmd clean install -DskipTests
```

3. Update server implementation.
4. Update client request/response mapping.

Do not edit generated Java files.

### If changing notification UI

Keep it a single static `index.html` unless explicitly asked otherwise.

Avoid adding frameworks. Native HTML/CSS/JS is intentional and close to the reference.

If adding a new notification event:

- handle title;
- handle description;
- handle icon;
- handle level;
- ensure no duplicate noisy cards.

### If changing matchmaking rules

Keep rules explicit and near the constants in `MatchmakingService`.

Current constants:

```java
MIN_PLAYERS_TO_START = 3
MAX_PLAYERS_IN_LOBBY = 5
LOBBY_START_DELAY_SECONDS = 10
DEFAULT_TIMEOUT_SECONDS = 30
ALLOWED_MODES = BATTLE_ROYALE, SURVIVAL, DEATHMATCH
MATCH_MIN_DURATION_SECONDS = 60
MATCH_MAX_DURATION_SECONDS = 120
MATCH_MIN_PROGRESS_EVENTS = 4
MATCH_MAX_PROGRESS_EVENTS = 8
```

If you change the number of players or match duration, update README and test scenario.

---

## 11. Intended event flow

Full happy-path flow:

```text
User / Swagger / GraphQL
→ matchmaking
→ RabbitMQ exchange matchmaking.events
→ match-log-service receives all events
→ notification-service receives all events and pushes WebSocket notifications
→ grpc-match-enrichment-client receives match.finished only
→ grpc-match-analytics-server calculates rank changes
→ grpc-match-enrichment-client publishes match.enriched
→ match-log-service and notification-service receive match.enriched
```

Detailed event sequence for one match:

```text
player.created                optional, when user creates a player
lobby.created                 first player creates a waiting lobby
lobby.player-joined           first player joins
lobby.player-joined           second player joins
lobby.player-joined           third player joins, 10-second start timer begins
lobby.player-joined           optional fourth player joins, timer restarts
lobby.formed                 lobby became FORMED and match id exists
match.progress ...            random live events
match.progress PLAYER_ELIMINATED
match.progress PLAYER_ELIMINATED
match.finished                 final match snapshot, triggers gRPC enrichment
match.enriched                rank changes calculated through gRPC
```

Notification UI receives `lobby.formed`, live `match.progress` events, `match.finished` and `match.enriched`. It no longer needs a special duplicate filter for `MATCH_STARTED` or `MATCH_FINISHED`, because those two names are not published to RabbitMQ as progress events.

---

## 12. What is intentionally simplified

This project intentionally does not include:

- persistent database;
- real matchmaking algorithm;
- real player availability/session tracking;
- database-backed ranking persistence;
- true battle simulation;
- frontend authentication;
- service discovery;
- gateway;
- Docker Compose;
- cloud deployment;
- unit/integration test suite beyond default context test;
- transactional outbox.

Do not treat these as bugs unless the user explicitly asks to implement them.

---

## 13. Definition of done for future changes

A future change is acceptable if:

1. It preserves the simple course architecture.
2. It does not add unnecessary infrastructure.
3. It keeps contracts and implementations aligned.
4. It compiles with Java 21.
5. It keeps runtime logs in Russian and keeps them short and readable.
6. It does not break the standard demo flow.
7. It documents any changed commands or behavior in README/AGENTS if relevant.
8. It clearly tells the user which services must be restarted.

For most changes:

- code-only change in `matchmaking` → restart only `matchmaking`;
- event contract change → rebuild all, restart all event consumers/producers;
- gRPC proto change → rebuild all, restart gRPC server and enrichment client;
- notification UI/listener change → restart `notification-service`, refresh browser;
- match-log listener change → restart `match-log-service`.

---

## 14. Best explanation of the project for humans

Use this short explanation when needed:

> Matchmaking Arena is a training service-oriented application. The main `matchmaking` service accepts REST and GraphQL requests, creates players, groups them into lobbies by mode, region and rank, and starts a match after 3 players are ready and the short waiting timer finishes. It publishes domain events to RabbitMQ. `match-log-service` listens to all events and keeps a simple match history. `grpc-match-enrichment-client` listens to the finished match event and synchronously calls `grpc-match-analytics-server` to calculate rating and rank changes. It then publishes `match.enriched`. `notification-service` listens to all events and pushes short live notifications to the browser through WebSocket.

Short transport distinction:

```text
REST/GraphQL = external API to control/query matchmaking
RabbitMQ     = asynchronous domain events
gRPC         = synchronous internal calculation call
WebSocket    = push notifications to browser
```

---

## 15. Files worth checking first

For most tasks, inspect these first:

```text
README.md
pom.xml
api-contract/src/main/resources/graphql/schema.graphqls
api-contract/src/main/java/.../endpoints/*.java
matchmaking/src/main/java/com/example/demo/service/MatchmakingService.java
matchmaking-events-contract/src/main/java/.../RoutingKeys.java
matchmaking-events-contract/src/main/java/.../MatchEvent.java
matchmaking/src/main/java/com/example/demo/event/*.java
match-log-service/src/main/java/.../listener/MatchLogEventListener.java
grpc-match-enrichment-client/src/main/java/.../listener/MatchFinishedListener.java
grpc-match-analytics-server/src/main/java/.../service/MatchAnalyticsServiceImpl.java
notification-service/src/main/java/.../listener/EventNotificationListener.java
notification-service/src/main/resources/static/index.html
```

Ignore `target/` directories unless debugging generated protobuf output.

