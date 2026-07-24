package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.LinkChildrenRequest;
import com.discoverdev.nms.admin.dto.LinkableChildResponse;
import com.discoverdev.nms.admin.dto.ParentDetailResponse;
import com.discoverdev.nms.admin.dto.ParentResponse;
import com.discoverdev.nms.admin.dto.UpdateParentRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Parents (guardians) list for the enrollment module. */
@RestController
@RequestMapping("/api/admin/parents")
@PreAuthorize("hasRole('ADMIN')")
public class AdminParentController {

    private final AdminParentService service;

    public AdminParentController(AdminParentService service) {
        this.service = service;
    }

    @GetMapping
    public List<ParentResponse> list() {
        return service.listParents();
    }

    @GetMapping("/{id}")
    public ParentDetailResponse get(@PathVariable UUID id) {
        return service.getParent(id);
    }

    @PatchMapping("/{id}")
    public ParentDetailResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateParentRequest req) {
        return service.update(id, req);
    }

    /** Children that can be linked to this parent (none of the parent's role yet). */
    @GetMapping("/{id}/linkable-children")
    public List<LinkableChildResponse> linkable(@PathVariable UUID id) {
        return service.getLinkableChildren(id);
    }

    /** Link the selected children to this parent. */
    @PostMapping("/{id}/children")
    public ParentDetailResponse link(@PathVariable UUID id, @RequestBody LinkChildrenRequest req) {
        return service.linkChildren(id, req);
    }

    /** Remove the parent from the nursery (soft-delete; children are kept). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
