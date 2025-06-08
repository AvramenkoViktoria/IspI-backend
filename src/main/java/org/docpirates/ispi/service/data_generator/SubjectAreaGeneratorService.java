package org.docpirates.ispi.service.data_generator;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.entity.SubjectArea;
import org.docpirates.ispi.repository.SubjectAreaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectAreaGeneratorService {

    private final SubjectAreaRepository subjectAreaRepository;

    @PostConstruct
    public void generateSubjectAreas() {
        if (subjectAreaRepository.count() > 0) return;

        List<String> areas = List.of(
                "Математика",
                "Фізика",
                "Хімія",
                "Біологія",
                "Інформатика",
                "Економіка",
                "Право",
                "Історія",
                "Література",
                "Філософія",
                "Психологія",
                "Соціологія",
                "Педагогіка",
                "Медицина",
                "Іноземні мови",
                "Журналістика",
                "Мистецтво",
                "Музика",
                "Архітектура",
                "Інженерія",
                "Астрономія",
                "Географія",
                "Фізична культура",
                "Політологія",
                "Менеджмент",
                "Маркетинг",
                "Кібербезпека",
                "Штучний інтелект",
                "Програмування",
                "Освітні технології"
        );

        List<SubjectArea> subjectAreas = areas.stream()
                .map(name -> SubjectArea.builder().name(name).build())
                .toList();

        subjectAreaRepository.saveAll(subjectAreas);
    }
}
