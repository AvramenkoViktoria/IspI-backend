package org.docpirates.ispi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.docpirates.ispi.entity.Response;
import org.docpirates.ispi.enums.RespondentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseChainDto {
    private Long responseId;
    private BigDecimal price;
    private LocalDateTime creationDate;
    private Long respondent;
    private RespondentType respondentType;
    private Long prevResponseId;

    public static ResponseChainDto fromEntity(Response response) {
        return ResponseChainDto.builder()
                .responseId(response.getId())
                .price(response.getPrice())
                .creationDate(response.getCreationDate())
                .respondent(response.getRespondent().getId())
                .respondentType(response.getRespondentType())
                .prevResponseId(response.getPrevResponseId())
                .build();
    }
}
