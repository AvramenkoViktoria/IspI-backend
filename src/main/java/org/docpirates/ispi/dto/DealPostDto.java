package org.docpirates.ispi.dto;

import org.docpirates.ispi.entity.Deal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DealPostDto(
        Long student_id,
        String student_name,
        Long teacher_id,
        String teacher_name,
        Long deal_id,
        LocalDateTime creation_date,
        BigDecimal price,
        Long post_id,
        String work_type,
        String university,
        String post_description,
        Integer student_feedback,
        String deal_status
) {
    public static DealPostDto from(Deal deal) {
        var post = deal.getPost();
        var student = post.getStudent();
        var teacher = deal.getTeacher();

        return new DealPostDto(
                student.getId(),
                student.getPib(),
                teacher.getId(),
                teacher.getPib(),
                deal.getId(),
                post.getCreationDate(),
                deal.getPrice(),
                post.getId(),
                post.getWorkType().getName(),
                post.getInstitution().getName(),
                post.getDescription(),
                deal.getStudentFeedback(),
                deal.getStatus().name()
        );
    }
}
