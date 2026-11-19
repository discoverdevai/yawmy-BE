package com.discoverdev.nms.chat;

import com.discoverdev.nms.chat.dto.SendMessageRequest;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.nursery.RoomRepository;
import com.discoverdev.nms.tenant.TenantContext;
import com.discoverdev.nms.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Send Broadcast (Jira YC-39): the nursery owner posts one message to a room's group chat, or
 * to every active room's group chat ("Whole nursery"). Each recipient gets it in the room chat
 * they already use, with the usual notifications.
 */
@Service
public class ChatBroadcastService {

    public enum Scope { NURSERY, ROOM }

    public record BroadcastRequest(String name, String description, Scope scope, UUID roomId) {}

    public record BroadcastResponse(int roomsReached, List<UUID> threadIds) {}

    private final RoomRepository roomRepository;
    private final ChatAccessService chatAccess;
    private final ChatService chatService;

    public ChatBroadcastService(RoomRepository roomRepository, ChatAccessService chatAccess, ChatService chatService) {
        this.roomRepository = roomRepository;
        this.chatAccess = chatAccess;
        this.chatService = chatService;
    }

    @Transactional
    public BroadcastResponse send(BroadcastRequest req, User admin) {
        String name = req.name() == null ? "" : req.name().trim();
        String description = req.description() == null ? "" : req.description().trim();
        if (name.isEmpty() || description.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A broadcast needs a name and a description");
        }
        if (name.length() > 100 || description.length() > 3800) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The broadcast is too long");
        }
        UUID nurseryId = TenantContext.currentNurseryId();
        List<Room> rooms;
        if (req.scope() == Scope.ROOM) {
            if (req.roomId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a room for the broadcast");
            }
            Room room = roomRepository.findByIdAndNurseryId(req.roomId(), nurseryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
            if (room.getArchivedAt() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This room is archived");
            }
            rooms = List.of(room);
        } else if (req.scope() == Scope.NURSERY) {
            rooms = roomRepository.findByNurseryIdAndArchivedAtIsNullOrderByNameAsc(nurseryId);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scope must be NURSERY or ROOM");
        }
        String body = name + "\n" + description;
        List<UUID> threads = new java.util.ArrayList<>();
        for (Room room : rooms) {
            ChatThread thread = chatAccess.ensureRoomGroup(room, admin);   // the owner is in every room group
            chatService.send(thread.getId(), new SendMessageRequest(body, null, null), admin);
            threads.add(thread.getId());
        }
        return new BroadcastResponse(rooms.size(), threads);
    }
}
