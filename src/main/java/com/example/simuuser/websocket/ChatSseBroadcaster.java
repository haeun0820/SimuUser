package com.example.simuuser.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ChatSseBroadcaster {

    private final ConcurrentMap<Long, Set<SseEmitter>> emittersByRoomId = new ConcurrentHashMap<>();

    public SseEmitter register(Long roomId) {
        SseEmitter emitter = new SseEmitter(0L);
        emittersByRoomId.computeIfAbsent(roomId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);

        emitter.onCompletion(() -> unregister(roomId, emitter));
        emitter.onTimeout(() -> unregister(roomId, emitter));
        emitter.onError(error -> unregister(roomId, emitter));

        return emitter;
    }

    public void broadcast(Long roomId, Object payload) {
        Set<SseEmitter> emitters = emittersByRoomId.get(roomId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("message").data(payload));
            } catch (IOException | IllegalStateException e) {
                unregister(roomId, emitter);
            }
        }
    }

    private void unregister(Long roomId, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersByRoomId.get(roomId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByRoomId.remove(roomId, emitters);
        }
    }
}
