package org.docpirates.ispi.dto;

import java.time.LocalDateTime;

public record DocumentComplaintDto(
        Long complaint_id,
        Long document_id,
        String document_name,
        String description,
        LocalDateTime creation_date
) {}
