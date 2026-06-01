package com.meetingsum.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetingsum.model.dto.TaskStatusResponse;
import com.meetingsum.model.enums.TaskStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理每个 taskId 的 WebSocket 会话集合，负责向所有订阅者广播进度消息。
 * 线程安全：使用 ConcurrentHashMap + ConcurrentHashMap.newKeySet()。
 */
@Component
public class WebSocketSessionManager {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionManager.class);

    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public WebSocketSessionManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(String taskId, WebSocketSession session) {
        sessions.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(String taskId, WebSocketSession session) {
        Set<WebSocketSession> taskSessions = sessions.get(taskId);
        if (taskSessions != null) {
            taskSessions.remove(session);
            if (taskSessions.isEmpty()) {
                sessions.remove(taskId);
            }
        }
    }

    public void sendProgress(String taskId, TaskStage stage, int progress) {
        sendMessage(taskId, Map.of(
                "type", "progress",
                "stage", stage != null ? stage.getValue() : "",
                "progress", progress
        ));
    }

    public void sendCompleted(String taskId, TaskStatusResponse task) {
        sendMessage(taskId, Map.of(
                "type", "completed",
                "stage", "completed",
                "progress", 100,
                "task", task != null ? task : Collections.emptyMap()
        ));
    }

    public void sendFailed(String taskId, String error, String errorCode) {
        sendMessage(taskId, Map.of(
                "type", "failed",
                "error", error != null ? error : "Unknown error",
                "error_code", errorCode != null ? errorCode : "UNKNOWN_ERROR"
        ));
    }

    private void sendMessage(String taskId, Map<String, Object> payload) {
        Set<WebSocketSession> taskSessions = sessions.getOrDefault(taskId, Collections.emptySet());
        if (taskSessions.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : taskSessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        log.debug("Failed to send WebSocket message to session {}: {}", session.getId(), e.getMessage());
                        // Session might be broken — it will be cleaned up on close handler
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to serialize WebSocket message for task {}: {}", taskId, e.getMessage());
        }
    }
}
