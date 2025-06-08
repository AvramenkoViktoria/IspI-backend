package org.docpirates.ispi.service.data_generator;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.entity.WorkType;
import org.docpirates.ispi.repository.WorkTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkTypeGeneratorService {

    private final WorkTypeRepository workTypeRepository;

    @PostConstruct
    public void generateWorkTypes() {
        if (workTypeRepository.count() > 0) return;

        List<String> types = List.of(
                "Курсова робота",
                "Дипломна робота",
                "Кваліфікаційна робота",
                "Презентація",
                "Тест",
                "Домашня робота",
                "Контрольна робота",
                "Екзамен",
                "Реферат",
                "Звіт",
                "Есе",
                "Лабораторна робота",
                "Проект",
                "Тези доповіді",
                "Аналіз джерел"
        );

        List<WorkType> workTypes = types.stream()
                .map(name -> WorkType.builder().name(name).build())
                .toList();

        workTypeRepository.saveAll(workTypes);
    }
}
