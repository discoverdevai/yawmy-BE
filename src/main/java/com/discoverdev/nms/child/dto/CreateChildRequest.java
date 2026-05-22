package com.discoverdev.nms.child.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateChildRequest(
    @NotBlank @Size(max = 200) String fullName,
    @NotNull @Past LocalDate dob,
    UUID roomId,
    @Size(max = 2000) String allergies,
    @Size(max = 2000) String medicalNotes,
    @Size(max = 2000) String immunizations
) {}
