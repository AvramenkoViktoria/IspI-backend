package org.docpirates.ispi.service.data_generator;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.entity.Post;
import org.docpirates.ispi.entity.RespondentType;
import org.docpirates.ispi.entity.Response;
import org.docpirates.ispi.entity.Teacher;
import org.docpirates.ispi.repository.PostRepository;
import org.docpirates.ispi.repository.ResponseRepository;
import org.docpirates.ispi.repository.TeacherRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ResponseGeneratorService {

    private final ResponseRepository responseRepository;
    private final TeacherRepository teacherRepository;
    private final PostRepository postRepository;

    public void generateResponses(int postCount) {
        List<Post> posts = postRepository.findAll();
        List<Teacher> teachers = teacherRepository.findAll();
        Random random = new Random();
        List<Long> usedPostIds = new ArrayList<>();
        for (int i = 0; i < postCount; i++) {
            Post randomPost;
            do {
                randomPost = posts.get(random.nextInt(posts.size()));
            } while (usedPostIds.contains(randomPost.getId()));

            usedPostIds.add(randomPost.getId());
            int chainsNumber = random.nextInt(10);
            List<Long> usedTeachersIds = new ArrayList<>();
            for (int j = 0; j < chainsNumber; j++) {
                Teacher randomTeacher;
                do {
                    randomTeacher = teachers.get(random.nextInt(teachers.size()));
                } while (usedTeachersIds.contains(randomTeacher.getId()));

                usedTeachersIds.add(randomTeacher.getId());

                int chainLength = random.nextInt(5);
                LocalDateTime firstResponse = LocalDateTime.now().minusDays(random.nextInt(5, 30));
                BigDecimal price = BigDecimal.valueOf(random.nextInt(6000));
                Long previousResponseId = null;
                for (int k = 0; k < chainLength; k++) {
                    int newPrice;
                    if (k % 2 == 0) {
                        newPrice = random.nextInt(1000);
                        price = BigDecimal.valueOf(newPrice);
                    } else {
                        do {
                            newPrice = random.nextInt(1000);
                        } while (price.compareTo(BigDecimal.valueOf(newPrice)) <= 0);
                        price = price.subtract(BigDecimal.valueOf(newPrice));
                    }
                    Response response = Response.builder()
                            .creationDate(firstResponse.plusDays(k + 1))
                            .price(price)
                            .post(randomPost)
                            .respondent(k % 2 == 0
                                    ? randomTeacher
                                    : randomPost.getStudent())
                            .respondentType(k % 2 == 0
                                    ? RespondentType.TEACHER
                                    : RespondentType.STUDENT)
                            .prevResponseId(previousResponseId)
                            .build();
                    responseRepository.save(response);
                    previousResponseId = response.getId();
                }
            }
        }
    }
}
