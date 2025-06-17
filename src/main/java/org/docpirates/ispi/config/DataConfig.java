package org.docpirates.ispi.config;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.enums.UserType;
import org.docpirates.ispi.service.DocumentIndexService;
import org.docpirates.ispi.service.data_generator.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataConfig {

    private final UserGeneratorService userGeneratorService;
    private final InstitutionGeneratorService institutionGeneratorService;
    private final WorkTypeGeneratorService workTypeGeneratorService;
    private final SubjectAreaGeneratorService subjectAreaGeneratorService;
    private final SubscriptionGeneratorService subscriptionGeneratorService;
    private final PostGeneratorService postGeneratorService;
    private final ResponseGeneratorService responseGeneratorService;
    private final DealGeneratorService dealGeneratorService;
    private final DocumentGeneratorService documentGeneratorService;
    private final ComplaintGeneratorService complaintGeneratorService;
    private final DocumentIndexService documentIndexService;

    @Bean
    public ApplicationRunner runGeneratorAfterStartup() {
        return args -> {
//            subscriptionGeneratorService.generateSubscriptions();
//            institutionGeneratorService.generateInstitutions();
//            workTypeGeneratorService.generateWorkTypes();
//            subjectAreaGeneratorService.generateSubjectAreas();
//            userGeneratorService.generateUsers(50, UserType.STUDENT);
//            userGeneratorService.generateUsers(30, UserType.TEACHER);
//            userGeneratorService.generateUsers(5, UserType.MODERATOR);
//            postGeneratorService.generatePosts(70);
//            responseGeneratorService.generateResponses(50);
//            dealGeneratorService.generateDeals(20);
//            documentGeneratorService.generateDocuments(2);
//            complaintGeneratorService.generateComplaints(20);
            //documentIndexService.indexDocuments(documentIndexService.readDocumentsFromDirectory(Path.of("src/main/java/org/docpirates/ispi/service/user_data/test_files")));
            System.out.println("Running after startup...");
        };
    }
}
