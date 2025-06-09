package org.docpirates.ispi.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePostRequest {
    private String workType;
    private String university;
    private String subjectArea;
    private BigDecimal initialPrice;
    private String description;
}

