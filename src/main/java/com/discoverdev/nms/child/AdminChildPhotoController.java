package com.discoverdev.nms.child;

import com.discoverdev.nms.child.dto.ChildResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/children/{childId}/photo")
@PreAuthorize("hasRole('ADMIN')")
public class AdminChildPhotoController {

    private final ChildPhotoService service;

    public AdminChildPhotoController(ChildPhotoService service) {
        this.service = service;
    }

    @PostMapping
    public ChildResponse upload(@PathVariable UUID childId, @RequestParam("file") MultipartFile file) {
        return service.upload(childId, file);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID childId) {
        service.delete(childId);
    }
}
