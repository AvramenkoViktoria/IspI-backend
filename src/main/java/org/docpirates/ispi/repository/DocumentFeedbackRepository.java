package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.Document;
import org.docpirates.ispi.entity.DocumentFeedback;
import org.docpirates.ispi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentFeedbackRepository extends JpaRepository<DocumentFeedback, Long> {
    boolean existsByUserAndDocument(User user, Document document);
}