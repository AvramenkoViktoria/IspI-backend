package org.docpirates.ispi.service.data_generator;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.entity.*;
import org.docpirates.ispi.enums.UserType;
import org.docpirates.ispi.repository.ModeratorRepository;
import org.docpirates.ispi.repository.StudentRepository;
import org.docpirates.ispi.repository.SubscriptionRepository;
import org.docpirates.ispi.repository.TeacherRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class UserGeneratorService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ModeratorRepository moderatorRepository;
    private final SubscriptionRepository subscriptionRepository;

    private final Random random = new Random();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void generateUsers(int count, UserType type) {
        String fileName = switch (type) {
            case STUDENT -> "src/main/java/org/docpirates/ispi/service/data_generator/user_data/student_data.txt";
            case TEACHER -> "src/main/java/org/docpirates/ispi/service/data_generator/user_data/teacher_data.txt";
            case MODERATOR -> "src/main/java/org/docpirates/ispi/service/data_generator/user_data/moderator_data.txt";
        };

        try {
            File file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            Files.writeString(Path.of(fileName), "");
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare file " + fileName, e);
        }

        List<Subscription> allSubscriptions = subscriptionRepository.findAll();
        if (allSubscriptions.isEmpty()) {
            throw new IllegalStateException("No subscriptions found in the database.");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            IntStream.range(0, count).forEach(i -> {
                Subscription randomSubscription = allSubscriptions.get(
                        ThreadLocalRandom.current().nextInt(allSubscriptions.size())
                );

                LocalDateTime activationDate = LocalDateTime.now().minusDays(random.nextInt(30));

                String rawPassword = "password" + i;
                String encodedPassword = passwordEncoder.encode(rawPassword);
                String name = type.name() + " " + i;
                String email = type.name().toLowerCase() + i + "@example.com";
                String phoneNumber = "+380" + (100000000 + random.nextInt(899999999));
                String rawBankCardNumber = "5375" + String.format("%012d", random.nextInt(1_000_000_000));

                try {
                    switch (type) {
                        case STUDENT -> {
                            Student student = Student.builder()
                                    .pib(name)
                                    .email(email)
                                    .password(encodedPassword)
                                    .phoneNumber(phoneNumber)
                                    .subscription(randomSubscription)
                                    .lastActivationDate(activationDate)
                                    .bankCardNumber(rawBankCardNumber)
                                    .build();
                            studentRepository.save(student);
                        }
                        case MODERATOR -> {
                            Moderator moderator = Moderator.builder()
                                    .pib(name)
                                    .email(email)
                                    .password(encodedPassword)
                                    .phoneNumber(phoneNumber)
                                    .subscription(randomSubscription)
                                    .lastActivationDate(activationDate)
                                    .bankCardNumber(rawBankCardNumber)
                                    .build();
                            moderatorRepository.save(moderator);
                        }
                        case TEACHER -> {
                            Teacher teacher = Teacher.builder()
                                    .pib(name)
                                    .email(email)
                                    .password(encodedPassword)
                                    .phoneNumber(phoneNumber)
                                    .subscription(randomSubscription)
                                    .lastActivationDate(activationDate)
                                    .bankCardNumber(rawBankCardNumber)
                                    .description("Expert in " + (i % 5))
                                    .rating(0f)
                                    .build();
                            teacherRepository.save(teacher);
                        }
                    }

                    String line = String.format("%s | %s | %s%n", name, email, rawPassword);
                    writer.write(line);
                    writer.flush();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write user data to file", e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to open file " + fileName, e);
        }
    }
}
