package com.discoverdev.nms.media;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.media.dto.BulkUploadResponse;
import com.discoverdev.nms.media.dto.MediaItemResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherMediaController {

    private final TeacherMediaService service;

    public TeacherMediaController(TeacherMediaService service) {
        this.service = service;
    }

    @PostMapping("/children/{childId}/media")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkUploadResponse uploadForChild(
        @PathVariable UUID childId,
        @RequestParam("files") List<MultipartFile> files,
        @RequestParam(value = "caption", required = false) String caption,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.uploadForChild(childId, files, caption, principal.getUser());
    }

    @PostMapping("/rooms/{roomId}/media")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkUploadResponse uploadForRoom(
        @PathVariable UUID roomId,
        @RequestParam("files") List<MultipartFile> files,
        @RequestParam(value = "caption", required = false) String caption,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.uploadForRoom(roomId, files, caption, principal.getUser());
    }

    @DeleteMapping("/media/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID mediaId, @AuthenticationPrincipal AppUserDetails principal) {
        service.delete(mediaId, principal.getUser());
    }

    @GetMapping("/children/{childId}/media")
    public List<MediaItemResponse> listForChild(
        @PathVariable UUID childId,
        @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.listForChild(childId, from, to, principal.getUser());
    }

    @GetMapping("/rooms/{roomId}/media")
    public List<MediaItemResponse> listForRoom(
        @PathVariable UUID roomId,
        @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.listForRoom(roomId, date, principal.getUser());
    }
}
