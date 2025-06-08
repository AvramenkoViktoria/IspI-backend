package org.docpirates.ispi.service.data_generator;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.entity.*;
import org.docpirates.ispi.enums.DealStatus;
import org.docpirates.ispi.enums.PostStatus;
import org.docpirates.ispi.repository.DealRepository;
import org.docpirates.ispi.repository.PostRepository;
import org.docpirates.ispi.repository.ResponseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class DealGeneratorService {

    private final PostRepository postRepository;
    private final ResponseRepository responseRepository;
    private final DealRepository dealRepository;

    public void generateDeals(int count) {
        List<Response> responses = responseRepository.findAll();
        List<Post> posts = postRepository.findAll();
        Random random = new Random();
        if (count > posts.size())
            throw new IllegalArgumentException("Deal count exceeds number of posts");
        for (int i = 0; i < count; i++) {
            Response randomResponse;
            int a = 0;
            do {
                randomResponse = responses.get(random.nextInt(responses.size()));
                if (a > 1000000)
                    throw new RuntimeException("Too many closed posts");
                a++;
            } while (randomResponse.getRespondentType().equals(RespondentType.STUDENT)
                     || randomResponse.getPost().getStatus().equals(PostStatus.CLOSED));

            randomResponse.getPost().setStatus(PostStatus.CLOSED);
            System.out.println(randomResponse.getId() + " | " + randomResponse.getRespondentType());

            boolean open = random.nextBoolean();
            Deal deal = Deal.builder()
                    .price(randomResponse.getPrice())
                    .post(randomResponse.getPost())
                    .teacher((Teacher) randomResponse.getRespondent())
                    .status(open
                            ? DealStatus.OPEN
                            : DealStatus.CLOSED)
                    .studentFeedback(!open
                            ? random.nextInt(1, 6)
                            : 0)
                    .build();
            dealRepository.save(deal);
        }
    }
}
