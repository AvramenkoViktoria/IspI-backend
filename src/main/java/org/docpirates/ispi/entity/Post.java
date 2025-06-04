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
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String workType;
    private String university;
    private String subjectArea;
    private String description;
    private BigDecimal initialPrice;

    private LocalDateTime creationDate;
    private String status;

    @ManyToOne
    private Student student;
}
