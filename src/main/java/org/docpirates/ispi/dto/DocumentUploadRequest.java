package org.docpirates.ispi.dto;

import lombok.Data;

@Data
public class DocumentUploadRequest {
    private String name;
    private String workType;
    private String subjectArea;
    private Long authorId;
}
