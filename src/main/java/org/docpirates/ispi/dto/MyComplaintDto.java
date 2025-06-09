package org.docpirates.ispi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class MyComplaintDto {
    private Long complaintId;
    private String message;
}
