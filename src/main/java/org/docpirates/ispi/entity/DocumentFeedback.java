package org.docpirates.ispi.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_feedback")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Document document;

    @ManyToOne(optional = false)
    private User user;

    @Column(nullable = false)
    private int stars;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
