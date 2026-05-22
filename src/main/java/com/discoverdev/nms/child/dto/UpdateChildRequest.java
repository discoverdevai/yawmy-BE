package com.discoverdev.nms.child.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateChildRequest(
    @Size(min = 1, max = 200) String fullName,
    @Past LocalDate dob,
    @Size(max = 2000) String allergies,
    @Size(max = 2000) String medicalNotes,
    @Size(max = 2000) String immunizations
) {}
