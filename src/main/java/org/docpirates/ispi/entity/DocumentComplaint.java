package org.docpirates.ispi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentComplaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "plaintiff_id", nullable = false)
    private User plaintiff;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
}
