package edu.rutmiit.demo.notificationservice.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("WebSocket подключен: {} (всего={})", session.getId(), sessions.size());

        String welcome = """
                {"type":"CONNECTED","message":"Подключение к сервису уведомлений Matchmaking выполнено","activeConnections":%d} """
                .formatted(sessions.size()).trim();
        session.sendMessage(new TextMessage(welcome));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket отключен: {} (статус={}, всего={})", session.getId(), status, sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload.trim())) {
            session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Ошибка транспорта WebSocket: сессия={}, ошибка={}", session.getId(), exception.getMessage());
        sessions.remove(session);
    }

    public void broadcast(String json) {
        if (sessions.isEmpty()) {
            return;
        }

        TextMessage message = new TextMessage(json);
        int sent = 0;
        int failed = 0;

        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessions.remove(session);
                failed++;
                continue;
            }

            try {
                session.sendMessage(message);
                sent++;
            } catch (IOException e) {
                sessions.remove(session);
                failed++;
                log.warn("Не удалось отправить сообщение WebSocket: сессия={}, ошибка={}", session.getId(), e.getMessage());
            }
        }

        log.info("Рассылка WebSocket завершена: отправлено={}, ошибок={}, активных={}", sent, failed, sessions.size());
    }

    public int getActiveConnectionCount() {
        return sessions.size();
    }
}
