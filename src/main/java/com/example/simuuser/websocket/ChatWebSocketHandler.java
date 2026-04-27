package com.example.simuuser.websocket;

import com.example.simuuser.dto.ChatMessageResponse;
import com.example.simuuser.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ChatWebSocketBroadcaster broadcaster;
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(ChatService chatService, ChatWebSocketBroadcaster broadcaster, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.broadcaster = broadcaster;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Authentication authentication = requireAuthentication(session.getPrincipal());
        Long roomId = extractRoomId(session.getUri());

        chatService.findRoomDetail(roomId, authentication);
        broadcaster.register(roomId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Authentication authentication = requireAuthentication(session.getPrincipal());
            Long roomId = requireRoomId(session);
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String content = payload.path("content").asText(null);

            ChatMessageResponse response = chatService.sendMessage(roomId, content, authentication);
            broadcaster.broadcast(roomId, response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendError(session, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        broadcaster.unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        broadcaster.unregister(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private Authentication requireAuthentication(Principal principal) {
        if (principal instanceof Authentication authentication && authentication.isAuthenticated()) {
            return authentication;
        }
        throw new IllegalStateException("Unauthorized websocket session");
    }

    private Long requireRoomId(WebSocketSession session) {
        Object roomId = session.getAttributes().get(ChatWebSocketBroadcaster.ROOM_ID_ATTRIBUTE);
        if (roomId instanceof Long value) {
            return value;
        }
        throw new IllegalStateException("Missing room id");
    }

    private Long extractRoomId(URI uri) {
        if (uri == null || uri.getQuery() == null || uri.getQuery().isBlank()) {
            throw new IllegalArgumentException("Missing room id");
        }

        for (String pair : uri.getQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && "roomId".equals(parts[0])) {
                try {
                    return Long.parseLong(parts[1]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid room id");
                }
            }
        }

        throw new IllegalArgumentException("Missing room id");
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        if (!session.isOpen()) {
            return;
        }

        synchronized (session) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new SocketErrorResponse(message))));
        }
    }

    private record SocketErrorResponse(String error) {
    }
}
