package com.example.simuuser.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ChatWebSocketBroadcaster {

    public static final String ROOM_ID_ATTRIBUTE = "chatRoomId";

    private final ObjectMapper objectMapper;
    private final ConcurrentMap<Long, Set<WebSocketSession>> sessionsByRoomId = new ConcurrentHashMap<>();

    public ChatWebSocketBroadcaster(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(Long roomId, WebSocketSession session) {
        session.getAttributes().put(ROOM_ID_ATTRIBUTE, roomId);
        sessionsByRoomId.computeIfAbsent(roomId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(WebSocketSession session) {
        Object roomIdValue = session.getAttributes().get(ROOM_ID_ATTRIBUTE);
        if (!(roomIdValue instanceof Long roomId)) {
            return;
        }

        Set<WebSocketSession> sessions = sessionsByRoomId.get(roomId);
        if (sessions == null) {
            return;
        }

        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByRoomId.remove(roomId, sessions);
        }
    }

    public void broadcast(Long roomId, Object payload) throws IOException {
        Set<WebSocketSession> sessions = sessionsByRoomId.get(roomId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        TextMessage message = new TextMessage(toJson(payload));
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                unregister(session);
                continue;
            }

            synchronized (session) {
                session.sendMessage(message);
            }
        }
    }

    private String toJson(Object payload) throws JsonProcessingException {
        return objectMapper.writeValueAsString(payload);
    }
}
