package com.example.simuuser.controller;

import com.example.simuuser.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chat/window")
    public String chatWindow() {
        return "chat/chat_window";
    }

    @GetMapping("/chat/room/{roomId}")
    public String chatRoom() {
        return "chat/chat_room";
    }

    @ResponseBody
    @GetMapping("/api/chat/rooms")
    public ResponseEntity<?> rooms(Authentication authentication) {
        return ResponseEntity.ok(chatService.findMyRooms(authentication));
    }

    @ResponseBody
    @GetMapping("/api/chat/requests")
    public ResponseEntity<?> requests(Authentication authentication) {
        return ResponseEntity.ok(chatService.findPendingRequests(authentication));
    }

    @ResponseBody
    @PostMapping("/api/chat/private-requests")
    public ResponseEntity<?> requestPrivateChat(@RequestBody Map<String, String> body, Authentication authentication) {
        try {
            return ResponseEntity.ok(chatService.requestPrivateChat(body.get("email"), authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @PostMapping("/api/chat/requests/{roomId}/accept")
    public ResponseEntity<?> acceptRequest(@PathVariable Long roomId, Authentication authentication) {
        try {
            return ResponseEntity.ok(chatService.acceptRequest(roomId, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @PostMapping("/api/chat/requests/{roomId}/decline")
    public ResponseEntity<?> declineRequest(@PathVariable Long roomId, Authentication authentication) {
        try {
            chatService.declineRequest(roomId, authentication);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @PostMapping("/api/chat/rooms/{projectId}")
    public ResponseEntity<?> createOrGetProjectRoom(@PathVariable Long projectId, Authentication authentication) {
        try {
            return ResponseEntity.ok(chatService.createOrGetProjectRoom(projectId, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @GetMapping("/api/chat/rooms/{roomId}")
    public ResponseEntity<?> roomDetail(@PathVariable Long roomId, Authentication authentication) {
        try {
            return ResponseEntity.ok(chatService.findRoomDetail(roomId, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @GetMapping("/api/chat/rooms/{roomId}/messages")
    public ResponseEntity<?> messages(@PathVariable Long roomId, Authentication authentication) {
        try {
            return ResponseEntity.ok(chatService.findMessages(roomId, authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @PostMapping("/api/chat/rooms/{roomId}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable Long roomId, @RequestBody Map<String, String> body, Authentication authentication) {
        try {
            return ResponseEntity.ok(chatService.sendMessage(roomId, body.get("content"), authentication));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
