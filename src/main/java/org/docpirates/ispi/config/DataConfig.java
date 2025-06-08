package org.docpirates.ispi.config;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.enums.UserType;
import org.docpirates.ispi.service.data_generator.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public ApplicationRunner runGeneratorAfterStartup() {
        return args -> {
            subscriptionGeneratorService.generateSubscriptions();
            institutionGeneratorService.generateInstitutions();
            workTypeGeneratorService.generateWorkTypes();
            subjectAreaGeneratorService.generateSubjectAreas();
            userGeneratorService.generateUsers(50, UserType.STUDENT);
            userGeneratorService.generateUsers(30, UserType.TEACHER);
            userGeneratorService.generateUsers(10, UserType.MODERATOR);
            postGeneratorService.generatePosts(50);
            responseGeneratorService.generateResponses(50);
            dealGeneratorService.generateDeals(10);
        };
    }
}
