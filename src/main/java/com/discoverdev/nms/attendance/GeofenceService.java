package com.discoverdev.nms.attendance;

import com.discoverdev.nms.nursery.Nursery;
import org.springframework.stereotype.Service;

@Service
public class GeofenceService {

    private static final double EARTH_RADIUS_METERS = 6_371_000d;
    private static final double DRIFT_BUFFER_METERS = 10d;

    public boolean isConfigured(Nursery nursery) {
        return nursery.getGeofenceLat() != null
            && nursery.getGeofenceLng() != null
            && nursery.getGeofenceRadiusM() != null;
    }

    public boolean isWithin(Nursery nursery, double lat, double lng) {
        if (!isConfigured(nursery)) return false;
        double dist = haversineMeters(lat, lng, nursery.getGeofenceLat(), nursery.getGeofenceLng());
        double allowed = nursery.getGeofenceRadiusM() + DRIFT_BUFFER_METERS;
        return dist <= allowed;
    }

    public double distanceMeters(Nursery nursery, double lat, double lng) {
        if (!isConfigured(nursery)) return Double.POSITIVE_INFINITY;
        return haversineMeters(lat, lng, nursery.getGeofenceLat(), nursery.getGeofenceLng());
    }

    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
