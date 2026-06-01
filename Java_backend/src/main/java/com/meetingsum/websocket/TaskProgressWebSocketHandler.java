package com.meetingsum.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class TaskProgressWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TaskProgressWebSocketHandler.class);

    private final WebSocketSessionManager sessionManager;

    public TaskProgressWebSocketHandler(WebSocketSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String taskId = extractTaskId(session);
        if (taskId != null) {
            sessionManager.register(taskId, session);
            log.debug("WebSocket connected: taskId={}, sessionId={}", taskId, session.getId());
        } else {
            log.warn("WebSocket connection without taskId in path, closing");
            try {
                session.close(CloseStatus.BAD_DATA);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String taskId = extractTaskId(session);
        if (taskId != null) {
            sessionManager.unregister(taskId, session);
            log.debug("WebSocket disconnected: taskId={}, sessionId={}, status={}", taskId, session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error: sessionId={}, error={}", session.getId(), exception.getMessage());
        String taskId = extractTaskId(session);
        if (taskId != null) {
            sessionManager.unregister(taskId, session);
        }
    }

    private String extractTaskId(WebSocketSession session) {
        if (session.getUri() == null) return null;
        String path = session.getUri().getPath();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            return path.substring(lastSlash + 1);
        }
        return null;
    }
}
