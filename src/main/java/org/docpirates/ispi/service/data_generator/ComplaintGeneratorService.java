package org.docpirates.ispi.service.data_generator;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.entity.Complaint;
import org.docpirates.ispi.entity.Deal;
import org.docpirates.ispi.entity.Moderator;
import org.docpirates.ispi.entity.User;
import org.docpirates.ispi.enums.ComplaintStatus;
import org.docpirates.ispi.repository.ComplaintRepository;
import org.docpirates.ispi.repository.DealRepository;
import org.docpirates.ispi.repository.ModeratorRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ComplaintGeneratorService {

    private final ComplaintRepository complaintRepository;
    private final DealRepository dealRepository;
    private final ModeratorRepository moderatorRepository;

    private static final String[] SAMPLE_DESCRIPTIONS = {
            "The teacher was unresponsive.",
            "The work was not delivered on time.",
            "The deal was canceled without notice.",
            "Payment issues occurred.",
            "Unprofessional behavior.",
            "The provided material was plagiarized.",
            "The teacher did not follow instructions.",
            "Communication was poor.",
            "Work quality was unsatisfactory.",
            "The student changed requirements mid-way."
    };

    public void generateComplaints(int count) {
        List<Deal> deals = dealRepository.findAll();
        List<Moderator> moderators = moderatorRepository.findAll();
        Random random = new Random();

        if (deals.isEmpty())
            throw new IllegalStateException("Deals not found in database");

        int created = 0;
        int attempts = 0;
        int maxAttempts = count * 10;

        while (created < count && attempts < maxAttempts) {
            Deal deal = deals.get(random.nextInt(deals.size()));
            Moderator moderator = moderators.isEmpty() ? null : moderators.get(random.nextInt(moderators.size()));
            User student = deal.getPost().getStudent();
            User teacher = deal.getTeacher();
            User plaintiff = random.nextBoolean() ? student : teacher;

            Complaint complaint = Complaint.builder()
                    .deal(deal)
                    .plaintiff(plaintiff)
                    .moderator(moderator)
                    .creationDate(LocalDateTime.now().minusDays(random.nextInt(30)))
                    .description(SAMPLE_DESCRIPTIONS[random.nextInt(SAMPLE_DESCRIPTIONS.length)])
                    .status(String.valueOf(ComplaintStatus.values()[random.nextInt(ComplaintStatus.values().length)]))
                    .build();

            complaintRepository.save(complaint);
            created++;
            attempts++;
        }

        if (created < count)
            System.out.println("Only " + created + " complaints generated (limited by existing uniqueness).");
    }
}
