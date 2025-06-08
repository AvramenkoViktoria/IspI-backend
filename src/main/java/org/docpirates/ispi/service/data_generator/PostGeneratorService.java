package org.docpirates.ispi.service.data_generator;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.entity.Institution;
import org.docpirates.ispi.entity.Post;
import org.docpirates.ispi.entity.Student;
import org.docpirates.ispi.entity.SubjectArea;
import org.docpirates.ispi.entity.WorkType;
import org.docpirates.ispi.enums.PostStatus;
import org.docpirates.ispi.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PostGeneratorService {

    private final PostRepository postRepository;
    private final StudentRepository studentRepository;
    private final InstitutionRepository institutionRepository;
    private final SubjectAreaRepository subjectAreaRepository;
    private final WorkTypeRepository workTypeRepository;

    private final Random random = new Random();

    public void generatePosts(int count) {
        if (postRepository.count() > 0) return;

        List<Student> students = studentRepository.findAll();
        List<Institution> institutions = institutionRepository.findAll();
        List<SubjectArea> subjectAreas = subjectAreaRepository.findAll();
        List<WorkType> workTypes = workTypeRepository.findAll();
        List<PostStatus> statuses = List.of(PostStatus.values());

        if (students.isEmpty() || institutions.isEmpty()
            || subjectAreas.isEmpty() || workTypes.isEmpty()) return;

        for (int i = 0; i < count; i++) {
            Post post = Post.builder()
                    .workType(getRandom(workTypes))
                    .subjectArea(getRandom(subjectAreas))
                    .description("Опис оголошення №" + (i + 1))
                    .initialPrice(BigDecimal.valueOf(200 + random.nextInt(800)))
                    .creationDate(LocalDateTime.now().minusDays(random.nextInt(30)))
                    .status(getRandom(statuses))
                    .student(getRandom(students))
                    .institution(getRandom(institutions))
                    .build();
            postRepository.save(post);
        }
    }

    private <T> T getRandom(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }
}
