package org.docpirates.ispi.dto;

import lombok.Data;

@Data
public class PostEditDto {
    private String newUniversity;
    private String newDescription;
    private boolean moderatorFlag;
}
