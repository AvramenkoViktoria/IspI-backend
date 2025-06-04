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
public class Response {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime creationDate;
    private BigDecimal price;

    @ManyToOne
    private User respondent;

    @Enumerated(EnumType.STRING)
    private RespondentType respondentType;

    @ManyToOne
    private Post post;
}
