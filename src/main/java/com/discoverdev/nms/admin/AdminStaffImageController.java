package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.ImageUrlResponse;
import com.discoverdev.nms.user.UserImageService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Upload a staff member's profile image (after inviting them). */
@RestController
@RequestMapping("/api/admin/staff/{userId}/image")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffImageController {

    private final UserImageService service;

    public AdminStaffImageController(UserImageService service) {
        this.service = service;
    }

    @PostMapping
    public ImageUrlResponse upload(@PathVariable UUID userId, @RequestParam("file") MultipartFile file) {
        return service.upload(userId, file);
    }
}
