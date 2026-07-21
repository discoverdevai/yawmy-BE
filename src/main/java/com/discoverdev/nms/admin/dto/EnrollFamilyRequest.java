package com.discoverdev.nms.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * One-shot "invite parent" payload from the 3-step flow: the parent + one or
 * more children (each with optional additional guardians and pickup contacts).
 * Child images are uploaded separately after this call using the returned childIds.
 */
public record EnrollFamilyRequest(
    @NotNull @Valid Parent parent,
    @NotEmpty @Valid List<Child> children
) {
    /** relationship = the parent's role: "Mother" or "Father". */
    public record Parent(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Size(max = 50) String relationship
    ) {}

    public record Child(
        @NotBlank @Size(max = 200) String fullName,
        @NotNull @Past LocalDate dob,
        UUID roomId,
        @Valid List<Guardian> guardians,
        @Valid List<Pickup> pickups
    ) {}

    /** An additional guardian — contact info only (saved as an emergency contact). */
    public record Guardian(
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Size(max = 50) String relationship,
        @NotBlank @Size(max = 32) String phone
    ) {}

    /** A pickup contact — saved info only, no account/email. */
    public record Pickup(
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Size(max = 50) String relationship,
        @NotBlank @Size(max = 32) String phone
    ) {}
}
