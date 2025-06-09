package org.docpirates.ispi.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LastTeacherResponseDto {
    private Long response_id;
    private BigDecimal price;
    private LocalDateTime creation_date;
}
