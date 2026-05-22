package com.discoverdev.nms.child.dto;

import com.discoverdev.nms.child.AuthorizedPickup;
import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.EmergencyContact;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ParentChildDetailResponse(
    UUID id,
    String fullName,
    LocalDate dob,
    String roomName,
    String photoUrl,
    String allergies,
    String medicalNotes,
    String immunizations,
    List<PickupResponse> pickups,
    List<EmergencyContactResponse> emergencyContacts
) {
    public static ParentChildDetailResponse of(
        Child c, List<AuthorizedPickup> pickups, List<EmergencyContact> contacts
    ) {
        return new ParentChildDetailResponse(
            c.getId(),
            c.getFullName(),
            c.getDob(),
            c.getRoom() != null ? c.getRoom().getName() : null,
            c.getPhotoKey() != null ? "/api/files/children/" + c.getId() + "/photo" : null,
            c.getAllergies(),
            c.getMedicalNotes(),
            c.getImmunizations(),
            pickups.stream().map(PickupResponse::of).toList(),
            contacts.stream().map(EmergencyContactResponse::of).toList()
        );
    }
}
