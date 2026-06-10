# Matchmaking SOP 7 – REST + GraphQL + RabbitMQ + gRPC + WebSocket

Учебный проект по предметной области **матчмейкинг**.

Главная идея проекта:

1. Игроки создаются в основном сервисе `matchmaking`.
2. Игроки заходят в лобби.
3. Когда в лобби набралось 3 игрока, запускается ожидание 10 секунд.
4. Если пришёл 4-й игрок, ожидание 10 секунд начинается заново. Если пришёл 5-й игрок, матч стартует сразу.
5. `matchmaking` публикует событие `lobby.formed`, когда лобби сформировано и матч создан.
6. После завершения матча `matchmaking` публикует событие `match.finished` в RabbitMQ.
7. `match-log-service` слушает события и пишет красивый журнал матчмейкинга.
8. `grpc-match-enrichment-client` слушает `match.finished`, вызывает gRPC-сервер и получает расчёт изменения рейтинга игроков.
9. `grpc-match-enrichment-client` публикует новое событие `match.enriched`.
10. `matchmaking` слушает `match.enriched` и обновляет рейтинг игроков в памяти.
11. `match-log-service` получает `match.enriched` и логирует итоговые изменения рейтинга.
12. **Новое в SOP 7:** `notification-service` слушает все события из RabbitMQ и сразу отправляет их в браузер через WebSocket.

Важно: `match-log-service` ничего не считает. Он только логирует. Расчёт изменения рейтинга делает gRPC-сервер. `matchmaking` только применяет уже готовые изменения из `match.enriched` к своим игрокам. WebSocket-сервис тоже ничего не считает, он только показывает уведомления в браузере.

> Runtime logs in Java code are written in Russian, коротко и без лишнего шума, чтобы проект было проще объяснять на защите.

---

## Правила режимов и рангов

В проекте используются три простых демонстрационных режима:

```text
BATTLE_ROYALE
SURVIVAL
DEATHMATCH
```

Все режимы работают одинаково, чтобы не усложнять учебный код: игроки попадают в отдельные очереди по `mode + region + rank`, и после 3-го игрока лобби ждёт ещё 10 секунд, после 4-го игрока таймер начинается заново, а на 5-м игроке матч стартует сразу. Названия `SURVIVAL` и `DEATHMATCH` выбраны вместо `DUO` и `ARENA`, потому что они логично подходят для матча на 3+ игроков.

Ранг игрока определяется по рейтингу:

```text
BRONZE:   rating < 1000
SILVER:   1000–1499
GOLD:     1500–1999
PLATINUM: 2000–2499
DIAMOND:  2500+
```

При создании игрока сервис берёт `rating` и сам выставляет корректный `rank`. Это сделано специально, чтобы не было ситуации `rating=1200`, но `rank=GOLD`. После матча gRPC-сервис считает изменение рейтинга по тому же правилу: победитель получает `+25`, проигравшие получают `-10`. Затем `matchmaking` получает `match.enriched` и обновляет рейтинг игроков в своей памяти.

---

## 1. Структура проекта простыми словами

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

### `matchmaking`

Главный сервис. В нём находятся игроки, лобби и матчи.

Он даёт:

```text
REST / Swagger на 8080
GraphQL / GraphiQL на 8080
RabbitMQ publisher событий
```

Именно через него мы создаём игроков, добавляем их в лобби и видим обновлённый рейтинг после `match.enriched`.

### `api-contract`

Библиотека с DTO и REST-контрактами:

```text
PlayerRequest
PlayerResponse
LobbyResponse
MatchResponse
JoinLobbyRequest
```

Сама не запускается.

### `matchmaking-events-contract`

Библиотека с событиями RabbitMQ:

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

Сама не запускается.

### `match-log-service`

Сервис журнала. Слушает RabbitMQ и пишет список событий матчмейкинга.

Адрес:

```text
http://localhost:8081/api/match-log
```

### `matchmaking-grpc-contract`

Библиотека с `.proto` контрактом gRPC. Из неё Maven генерирует Java-классы для gRPC-сервера и gRPC-клиента.

Сама не запускается.

### `grpc-match-analytics-server`

gRPC-сервер. Он считает изменения рейтинга после матча:

```text
победитель +25
проигравшие -10
```

HTTP actuator порт:

```text
8083
```

gRPC порт:

```text
9090
```

В браузере gRPC-порт не открывается. Его вызывает только `grpc-match-enrichment-client`.

### `grpc-match-enrichment-client`

Мост:

```text
RabbitMQ -> gRPC -> RabbitMQ
```

Он слушает `match.finished`, вызывает gRPC-сервер, получает изменения рейтинга и публикует `match.enriched`.

HTTP actuator порт:

```text
8082
```

### `notification-service`

Новое в SOP 7. Это WebSocket notifier.

Он слушает все события из RabbitMQ и отправляет уведомления всем открытым браузерам. Страница также хранит последние 50 уведомлений в `localStorage`.

Адрес страницы:

```text
http://localhost:8084/
```

WebSocket endpoint:

```text
ws://localhost:8084/ws/notifications
```

---

## 2. Полная цепочка работы

```text
1. Ты создаёшь игроков через Swagger.
2. Ты добавляешь 3 игроков в одно лобби.
3. matchmaking ждёт 10 секунд, чтобы мог присоединиться 4-й игрок.
4. Если появляется 4-й игрок, таймер начинается заново. Если появляется 5-й игрок, матч стартует сразу.
5. matchmaking формирует лобби и отправляет lobby.formed в RabbitMQ.
6. после завершения матча matchmaking отправляет match.finished в RabbitMQ.
7. match-log-service получает события и пишет лог.
8. notification-service получает события и показывает уведомления в браузере.
9. grpc-match-enrichment-client получает match.finished.
10. grpc-match-enrichment-client вызывает grpc-match-analytics-server по gRPC.
11. grpc-match-analytics-server считает изменения рейтинга.
12. grpc-match-enrichment-client публикует match.enriched в RabbitMQ.
13. matchmaking получает match.enriched и обновляет рейтинг игроков.
14. match-log-service логирует match.enriched.
15. notification-service показывает уведомление про match.enriched в браузере.
```

Коротко:

```text
Swagger -> matchmaking -> RabbitMQ -> match-log-service
                              |
                              -> notification-service -> WebSocket browser
                              |
                              -> grpc-match-enrichment-client -> gRPC server -> RabbitMQ -> notifications/log
```

---

## 3. Быстрый запуск в IntelliJ IDEA

Открой проект как Maven-проект из папки:

```powershell
cd "C:\Users\colonialrift\Dropbox\SOP\Matchmaking Arena"
```

Все команды можно писать в терминале IDEA. Для каждого сервиса открой отдельную вкладку терминала.

---

### Консоль 1 – RabbitMQ

Сначала открой Docker Desktop.

Если контейнер уже есть:

```powershell
cd "C:\Users\colonialrift\Dropbox\SOP\Matchmaking Arena"
docker start rabbitmq
```

Если контейнера ещё нет:

```powershell
cd "C:\Users\colonialrift\Dropbox\SOP\Matchmaking Arena"
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management
```

Проверить:

```powershell
docker ps
```

RabbitMQ UI:

```text
http://localhost:15672
```

Логин / пароль:

```text
guest / guest
```

---

### Консоль 2 – сборка проекта

```powershell
cd "C:\Users\colonialrift\Dropbox\SOP\Matchmaking Arena"
.\mvnw.cmd clean install -DskipTests
```

Если Maven на Windows падает на удалении `target`, останови все сервисы, закрой IDEA, поставь Dropbox Sync на паузу и собери заново. Лучше всего для сборки скопировать проект из Dropbox в обычную папку, например `C:\Users\colonialrift\Dropbox\SOP\Matchmaking Arena`.

---

### Консоль 3 – основной сервис `matchmaking`

```powershell
cd "C:\Users\colonialrift\Dropbox\SOP\Matchmaking Arena"
.\mvnw.cmd spring-boot:run -pl matchmaking -am -DskipTests
```

Адреса:

```text
Swagger REST: http://localhost:8080/swagger-ui.html
GraphQL:      http://localhost:8080/graphql
GraphiQL:     http://localhost:8080/graphiql?path=/graphql
```

---

### Консоль 4 – `match-log-service`

```powershell
cd "C:\Users\colonialrift\Dropbox\SOP\Matchmaking Arena"
.\mvnw.cmd spring-boot:run -pl match-log-service -am -DskipTests
```

Адрес:

```text
http://localhost:8081/api/match-log
```

---

### Консоль 5 – `grpc-match-analytics-server`

```powershell
cd "C:\Users\colonialrift\Dropbox\SOP\Matchmaking Arena"
.\mvnw.cmd spring-boot:run -pl grpc-match-analytics-server -am -DskipTests
```

Он запускает gRPC-сервер на порту:

```text
9090
```

---

### Консоль 6 – `grpc-match-enrichment-client`

```powershell
cd "C:\Users\colonialrift\Dropbox\SOP\Matchmaking Arena"
.\mvnw.cmd spring-boot:run -pl grpc-match-enrichment-client -am -DskipTests
```

---

### Консоль 7 – `notification-service`

```powershell
cd "C:\Users\colonialrift\Dropbox\SOP\Matchmaking Arena"
.\mvnw.cmd spring-boot:run -pl notification-service -am -DskipTests
```

Открой в браузере:

```text
http://localhost:8084/
```

Если статус зелёный `Connected`, браузер подключился к WebSocket.

---

## 4. Что появилось в SOP 7

Добавлен новый сервис:

```text
notification-service
```

Он делает только одну простую вещь:

```text
RabbitMQ event -> WebSocket notification in browser
```

То есть раньше событие можно было увидеть только в RabbitMQ, логах или через `GET /api/match-log`. Теперь событие само прилетает в браузер.

Внутри `notification-service` есть:

```text
RabbitMQConfig.java
WebSocketConfig.java
NotificationWebSocketHandler.java
EventNotificationListener.java
static/index.html
```

### `RabbitMQConfig.java`

Создаёт очередь:

```text
q.matchmaking.notifications.all
```

Она привязана к exchange:

```text
matchmaking.events
```

через binding:

```text
#
```

`#` значит: слушать все события.

### `WebSocketConfig.java`

Регистрирует WebSocket endpoint:

```text
/ws/notifications
```

### `NotificationWebSocketHandler.java`

Хранит список открытых браузерных подключений и умеет делать broadcast.

Broadcast значит: одно событие отправить всем открытым вкладкам браузера.

### `EventNotificationListener.java`

Слушает RabbitMQ, делает понятное уведомление и отправляет его в WebSocket.

### `static/index.html`

Простая страница центра уведомлений. Никакого React/Vue нет. Только обычный HTML + CSS + JavaScript.

---

## 5. Как протестировать всю цепочку через Swagger

Открой:

```text
http://localhost:8080/swagger-ui.html
```

Также открой рядом:

```text
http://localhost:8084/
```

---

### Шаг 1. Создай 3 игроков

Важно: `rank` в ответе считается по `rating`. В примерах ниже рейтинг 1210/1180/1270 даёт `SILVER`.

В Swagger открой:

```text
Players -> POST /api/players
```

Создай первого игрока:

```json
{
  "nickname": "Artem",
  "rating": 1210,
  "region": "EU",
  "rank": "SILVER"
}
```

Второго:

```json
{
  "nickname": "Misha",
  "rating": 1180,
  "region": "EU",
  "rank": "SILVER"
}
```

Третьего:

```json
{
  "nickname": "Sasha",
  "rating": 1270,
  "region": "EU",
  "rank": "SILVER"
}
```

После каждого `POST /api/players` в браузере `http://localhost:8084/` должно появиться уведомление `Игрок создан`.

Скопируй `id` каждого игрока.

---

### Шаг 2. Добавь игроков в лобби

Для `mode` используй одно из значений: `BATTLE_ROYALE`, `SURVIVAL`, `DEATHMATCH`. Для базового теста ниже используем `BATTLE_ROYALE`.

В Swagger открой:

```text
Lobbies -> POST /api/lobbies/join
```

Для Artem:

```json
{
  "playerId": "ID_ARTEM",
  "mode": "BATTLE_ROYALE",
  "region": "EU",
  "rank": "SILVER",
  "timeoutSeconds": 60
}
```

Для Misha:

```json
{
  "playerId": "ID_MISHA",
  "mode": "BATTLE_ROYALE",
  "region": "EU",
  "rank": "SILVER",
  "timeoutSeconds": 60
}
```

Для Sasha:

```json
{
  "playerId": "ID_SASHA",
  "mode": "BATTLE_ROYALE",
  "region": "EU",
  "rank": "SILVER",
  "timeoutSeconds": 60
}
```

После первого игрока будет `WAITING` и `playerCount = 1`.
После второго будет `WAITING` и `playerCount = 2`.
После третьего будет `WAITING`, `playerCount = 3`, и начнётся ожидание 10 секунд. Если 4-й игрок присоединится до старта, таймер начнётся заново. Когда таймер истечёт, лобби станет `FORMED`, появится `matchId`, и матч создастся. Если лобби соберёт 5 игроков, матч стартует сразу.

---

### Шаг 3. Что должно появиться в уведомлениях

На странице:

```text
http://localhost:8084/
```

ты увидишь примерно такие карточки:

```text
Игрок создан
Лобби создано
Игрок вошел в лобби
Лобби сформировано
Событие матча
Матч завершен
Рейтинг обновлен
```

Самое важное для SOP 7:

```text
Матч завершен
Рейтинг обновлен
```

Это показывает, что уведомления приходят не только от основного сервиса, но и от gRPC-enrichment цепочки. После `match.enriched` можно отдельно проверить `GET /api/players`: рейтинги игроков должны измениться.

---

### Шаг 4. Проверь match-log

Открой:

```text
http://localhost:8081/api/match-log
```

Там должен быть тот же смысл, но в виде журнала событий.

---

### Шаг 5. Проверь RabbitMQ

Открой:

```text
http://localhost:15672
```

В `Queues and Streams` должны быть очереди:

```text
q.matchmaking.match-log.events
q.matchmaking.enrichment.match-finished
q.matchmaking.notifications.all
```

У `q.matchmaking.notifications.all` должно быть:

```text
Consumers = 1
```

Если страница уведомлений открыта, это не consumer RabbitMQ. Consumer RabbitMQ – это сам `notification-service`. Браузер подключается уже к `notification-service` через WebSocket.

---

## 6. Как увидеть сообщение в RabbitMQ

Если все сервисы работают, сообщения быстро обрабатываются и `Ready = 0`. Это нормально.

Чтобы увидеть сообщение в очереди:

1. Останови `notification-service`.
2. Создай игрока или матч через Swagger.
3. Зайди в RabbitMQ -> `Queues and Streams` -> `q.matchmaking.notifications.all`.
4. Там будет `Ready = 1` или больше.
5. Запусти `notification-service` обратно.
6. `Ready` станет 0, а уведомление появится в браузере.

---

## 7. Как проверить WebSocket отдельно

Открой страницу:

```text
http://localhost:8084/
```

Нажми кнопку:

```text
Ping
```

В `Connection log` появится:

```text
Client: ping
Server: PONG
```

Это значит, что браузер реально держит WebSocket-соединение с сервером.

---

## 8. Как проверить переподключение

1. Открой `http://localhost:8084/`.
2. Останови `notification-service`.
3. На странице статус станет `Disconnected`, потом `Reconnect in ...`.
4. Запусти `notification-service` снова.
5. Страница сама подключится обратно.

Это показывает простую логику reconnect, как в референсном проекте.

---

## 9. Короткая формулировка для защиты

Можно сказать так:

> В SOP 7 мы добавили `notification-service`. Он слушает все доменные события из RabbitMQ и отправляет их подключённым браузерам через WebSocket. WebSocket нужен, чтобы пользователь не делал постоянный GET-запрос вручную, а видел события сразу в реальном времени. В нашей предметной области это уведомления о создании игрока, входе в лобби, формировании лобби, завершении матча и расчёте изменений рейтинга после gRPC-обогащения.

Разница между транспортами:

```text
REST = пользователь вручную делает запрос
GraphQL = пользователь сам выбирает поля ответа
RabbitMQ = сервисы асинхронно обмениваются событиями
gRPC = один сервис синхронно вызывает другой сервис
WebSocket = сервер сам отправляет событие в браузер
```

---

## 10. Полная демонстрационная цепочка одной строкой

```text
POST /api/lobbies/join x3
-> wait 10 seconds
-> lobby.formed
-> match.finished
-> RabbitMQ
-> match-log-service
-> notification-service -> browser WebSocket card
-> grpc-match-enrichment-client
-> grpc-match-analytics-server
-> match.enriched
-> match-log-service
-> notification-service -> browser WebSocket card
```


## Дополнение: матч теперь стартует после короткого ожидания

В этой версии матч не стартует сразу после входа третьего игрока.
Когда в лобби набирается 3 игрока, `matchmaking` ждёт 10 секунд. Если за это время входит 4-й игрок, ожидание начинается заново. Если лобби собирает 5 игроков, матч стартует сразу. После старта лобби получает статус `FORMED`, создаётся матч со статусом `IN_PROGRESS`, а в RabbitMQ публикуется `lobby.formed`. `MATCH_STARTED` остаётся только внутри `match.events` для истории матча.
Дальше матч идёт случайное время от 60 до 120 секунд. За это время `matchmaking` случайно планирует от 4 до 8 live-событий матча: игрок кого-то ударил, увернулся, нашёл лут, получил урон от зоны или использовал аптечку. Промежутки между этими событиями тоже случайные.

Когда матч завершается, случайно выбирается победитель, публикуется финальное событие `match.finished`, после чего `grpc-match-enrichment-client` вызывает gRPC-сервер и публикует `match.enriched` с изменениями рейтинга. После этого `matchmaking` получает `match.enriched` и обновляет рейтинг игроков.

Для проверки открой `http://localhost:8084/`, добавь 3 игроков в одно лобби через Swagger и подожди 1-2 минуты. На странице уведомлений должны появляться несколько карточек `Событие матча`, затем `Матч завершен`, затем `Рейтинг пересчитан`.


### Notification-service: what should appear

The notification center listens to all domain events from RabbitMQ. It shows short user-facing cards, not technical logs.
Expected cards: Игрок создан, Лобби создано, Игрок вошел в лобби, Лобби сформировано, Событие матча, Матч завершен, Рейтинг обновлен.
`Лобби сформировано` includes the match id and player nicknames, so it is clear who entered the match.
