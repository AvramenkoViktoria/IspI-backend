package org.docpirates.ispi.dto;

import lombok.Builder;
import lombok.Data;
import org.docpirates.ispi.entity.*;

import java.time.LocalDateTime;

@Data
@Builder
public class ComplaintDto {
    private Long complaintId;
    private String description;
    private LocalDateTime creationDate;
    private String status;
    private String moderator;
    private Long dealId;
    private Long studentId;
    private Long teacherId;
    private String studentName;
    private String teacherName;
    private String studentPhone;
    private String teacherPhone;
    private String studentEmail;
    private String teacherEmail;
    private Long plantiffId;

    public static ComplaintDto toDto(Complaint complaint) {
        Deal deal = complaint.getDeal();
        Student student = deal.getPost().getStudent();
        Teacher teacher = deal.getTeacher();
        User plaintiff = complaint.getPlaintiff();

        return ComplaintDto.builder()
                .complaintId(complaint.getId())
                .description(complaint.getDescription())
                .creationDate(complaint.getCreationDate())
                .status(complaint.getStatus())
                .moderator(complaint.getModerator() != null ? complaint.getModerator().getPib() : null)
                .dealId(deal.getId())
                .studentId(student.getId())
                .teacherId(teacher.getId())
                .studentName(student.getPib())
                .teacherName(teacher.getPib())
                .studentPhone(student.getPhoneNumber())
                .teacherPhone(teacher.getPhoneNumber())
                .studentEmail(student.getEmail())
                .teacherEmail(teacher.getEmail())
                .plantiffId(plaintiff.getId())
                .build();
    }
}

