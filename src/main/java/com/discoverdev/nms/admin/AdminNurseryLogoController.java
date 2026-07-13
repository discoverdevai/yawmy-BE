package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.NurseryResponse;
import com.discoverdev.nms.nursery.NurseryLogoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/nursery/logo")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNurseryLogoController {

    private final NurseryLogoService service;

    public AdminNurseryLogoController(NurseryLogoService service) {
        this.service = service;
    }

    @PostMapping
    public NurseryResponse upload(@RequestParam("file") MultipartFile file) {
        return service.upload(file);
    }
}