package org.docpirates.ispi.service.data_generator;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.entity.Document;
import org.docpirates.ispi.entity.SubjectArea;
import org.docpirates.ispi.entity.User;
import org.docpirates.ispi.entity.WorkType;
import org.docpirates.ispi.repository.DocumentRepository;
import org.docpirates.ispi.repository.SubjectAreaRepository;
import org.docpirates.ispi.repository.UserRepository;
import org.docpirates.ispi.repository.WorkTypeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentGeneratorService {

    private final DocumentRepository documentRepository;
    private final WorkTypeRepository workTypeRepository;
    private final SubjectAreaRepository subjectAreaRepository;
    private final UserRepository userRepository;

    private final Random random = new Random();

    public void generateDocuments(int count) {
        List<WorkType> workTypes = workTypeRepository.findAll();
        List<SubjectArea> subjectAreas = subjectAreaRepository.findAll();
        List<User> users = userRepository.findAll();

        if (workTypes.isEmpty() || subjectAreas.isEmpty() || users.isEmpty())
            throw new IllegalStateException("Cannot load from tables: workTypes, subjectAreas or users");

        for (int i = 0; i < count; i++) {
            User author = users.get(random.nextInt(users.size()));
            WorkType workType = workTypes.get(random.nextInt(workTypes.size()));
            SubjectArea subjectArea = subjectAreas.get(random.nextInt(subjectAreas.size()));

            Document document = Document.builder()
                    .name("Document_" + UUID.randomUUID().toString().substring(0, 8))
                    .extension("pdf")
                    .workType(workType.getName())
                    .subjectArea(subjectArea.getName())
                    .diskPath("src/main/java/org/docpirates/ispi/service/user_data/IspI.pdf")
                    .uploadedAt(generateRandomDateWithinLastMonth())
                    .author(author)
                    .build();
            documentRepository.save(document);
        }
    }

    private LocalDateTime generateRandomDateWithinLastMonth() {
        int daysAgo = random.nextInt(30);
        int hour = random.nextInt(24);
        int minute = random.nextInt(60);
        int second = random.nextInt(60);
        return LocalDateTime.now()
                .minusDays(daysAgo)
                .withHour(hour)
                .withMinute(minute)
                .withSecond(second)
                .withNano(0);
    }
}
