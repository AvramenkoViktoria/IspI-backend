package org.docpirates.ispi.dto;

import org.docpirates.ispi.entity.Document;

import java.time.LocalDateTime;

public record DocumentDto(
        Long document_id,
        String name,
        LocalDateTime upload_date,
        String work_type,
        String content_type,
        String download_url
) {
    public static DocumentDto from(Document doc) {
        return new DocumentDto(
                doc.getId(),
                doc.getName(),
                doc.getUploadedAt(),
                doc.getWorkType(),
                getMimeType(doc.getExtension()),
                "/api/users/me/documents/" + doc.getId()
        );
    }

    private static String getMimeType(String ext) {
        return switch (ext.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "doc", "docx" -> "application/msword";
            case "txt" -> "text/plain";
            default -> "application/octet-stream";
        };
    }
}
