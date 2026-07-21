package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.RoomResponse;
import com.discoverdev.nms.nursery.RoomImageService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/rooms/{roomId}/image")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomImageController {

    private final RoomImageService service;

    public AdminRoomImageController(RoomImageService service) {
        this.service = service;
    }

    @PostMapping
    public RoomResponse upload(@PathVariable UUID roomId, @RequestParam("file") MultipartFile file) {
        return service.upload(roomId, file);
    }
}
