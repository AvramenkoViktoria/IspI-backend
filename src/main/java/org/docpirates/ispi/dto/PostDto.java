package org.docpirates.ispi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.docpirates.ispi.entity.Post;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDto {
    private Long postId;
    private Long studentId;
    private String studentName;
    private String workType;
    private String subjectArea;
    private String institution;
    private BigDecimal initialPrice;
    private LocalDateTime creationDate;
    private String status;

    public static PostDto fromEntity(Post post) {
        return PostDto.builder()
                .postId(post.getId())
                .studentId(post.getStudent().getId())
                .studentName(post.getStudent().getPib())
                .workType(post.getWorkType().getName())
                .subjectArea(post.getSubjectArea().getName())
                .institution(post.getInstitution().getName())
                .initialPrice(post.getInitialPrice())
                .creationDate(post.getCreationDate())
                .status(post.getStatus().name())
                .build();
    }
}
