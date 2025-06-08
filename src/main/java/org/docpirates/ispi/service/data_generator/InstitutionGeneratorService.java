package org.docpirates.ispi.service.data_generator;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.entity.Institution;
import org.docpirates.ispi.repository.InstitutionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InstitutionGeneratorService {

    private final InstitutionRepository institutionRepository;

    @PostConstruct
    public void generateInstitutions() {
        if (institutionRepository.count() > 0) return;

        List<String> names = List.of(
                "НаУКМА", "КНУ ім. Шевченка", "ЛНУ ім. Франка", "ХНУ ім. Каразіна", "КПІ ім. Сікорського",
                "Острозька академія", "ЧНУ ім. Федьковича", "Прикарпатський НУ", "ТНПУ ім. Гнатюка", "СумДУ",
                "ДНУ ім. Гончара", "КНУКіМ", "КНЕУ", "КНЛУ", "НУБіП", "НПУ ім. Драгоманова", "НУ «Львівська політехніка»",
                "УжНУ", "Житомирський Державний Університет", "Вінницький НТУ", "Запорізький НУ",
                "Comenius University in Bratislava", "Slovak University of Technology", "Technical University of Košice",
                "University of Žilina", "University of Prešov", "Pavol Jozef Šafárik University", "Matej Bel University",
                "Школа", "Самонавчання"
        );

        List<Institution> institutions = names.stream()
                .map(name -> Institution.builder().name(name).build())
                .toList();

        institutionRepository.saveAll(institutions);
    }
}
