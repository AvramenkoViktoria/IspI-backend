package org.docpirates.ispi.service;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.dto.DocumentDto;
import org.docpirates.ispi.entity.User;
import org.docpirates.ispi.enums.Subscription;
import org.docpirates.ispi.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final DocumentRepository docRepo;

    public int getNumberOfUploadedDocuments(User user) {
        return (int) docRepo.countByAuthor(user);
    }

    public List<DocumentDto> getDocumentsByAuthor(User user) {
        return docRepo.findByAuthorOrderByUploadedAtDesc(user).stream()
                .map(DocumentDto::from)
                .toList();
    }

    public int getNumberOfUploadedDocuments(LocalDateTime from, User user) {
        return (int) docRepo.countByAuthorAndUploadedAtAfter(user, from);
    }

    public static LocalDateTime getNextPaymentDate(long subscriptionId, LocalDateTime lastActivationDate) {
        if (subscriptionId == 0)
            return null;
        Subscription subscription = Subscription.values()[(int) (subscriptionId - 1)];
        switch (subscription) {
            case LIBRARIAN, DEFENSE_PLUS -> {
                return lastActivationDate.plusDays(30);
            }
            case PATRON -> {
                return lastActivationDate.plusDays(7);
            }
            default -> {
                return null;
            }
        }
    }
}
