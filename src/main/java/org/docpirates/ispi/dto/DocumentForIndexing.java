package org.docpirates.ispi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentForIndexing {
    private String path;
    private String filename;
    private String content;
}
