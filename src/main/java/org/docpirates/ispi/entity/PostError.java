package org.docpirates.ispi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostError {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private LocalDateTime creationDate;
    private String workType;
    private String university;
    private String subjectArea;
    private String postDescription;
    private BigDecimal initialPrice;
    private String status;
    private boolean existingPost;

    @ManyToOne
    private Student student;
}
