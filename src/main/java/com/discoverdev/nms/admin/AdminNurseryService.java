package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.NurseryResponse;
import com.discoverdev.nms.admin.dto.UpdateGeofenceRequest;
import com.discoverdev.nms.admin.dto.UpdateNurseryRequest;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminNurseryService {

    private final NurseryRepository nurseryRepository;

    public AdminNurseryService(NurseryRepository nurseryRepository) {
        this.nurseryRepository = nurseryRepository;
    }

    @Transactional(readOnly = true)
    public NurseryResponse getCurrent() {
        return NurseryResponse.of(loadCurrent());
    }

    @Transactional
    public NurseryResponse update(UpdateNurseryRequest req) {
        Nursery nursery = loadCurrent();
        if (req.name() != null) nursery.setName(req.name());
        if (req.logoUrl() != null) nursery.setLogoUrl(req.logoUrl());
        if (req.timezone() != null) nursery.setTimezone(req.timezone());
        if (req.hoursStart() != null) nursery.setHoursStart(req.hoursStart());
        if (req.hoursEnd() != null) nursery.setHoursEnd(req.hoursEnd());
        if (req.workingDays() != null) nursery.setWorkingDays(req.workingDays());
        if (nursery.getHoursStart() != null
            && nursery.getHoursEnd() != null
            && !nursery.getHoursEnd().isAfter(nursery.getHoursStart())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hoursEnd must be after hoursStart");
        }
        return NurseryResponse.of(nurseryRepository.save(nursery));
    }

    @Transactional
    public NurseryResponse updateGeofence(UpdateGeofenceRequest req) {
        Nursery nursery = loadCurrent();
        nursery.setGeofenceLat(req.lat());
        nursery.setGeofenceLng(req.lng());
        nursery.setGeofenceRadiusM(req.radiusM());
        return NurseryResponse.of(nurseryRepository.save(nursery));
    }

    private Nursery loadCurrent() {
        return nurseryRepository.findById(TenantContext.currentNurseryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
    }
}
