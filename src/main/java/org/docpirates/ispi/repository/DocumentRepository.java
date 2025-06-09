package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.Document;
import org.docpirates.ispi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    long countByAuthor(User author);
    List<Document> findByAuthor(User user);
    Optional<Document> findById(long id);
    List<Document> findByAuthorOrderByUploadedAtDesc(User user);
    long countByAuthorAndUploadedAtAfter(User author, LocalDateTime date);
}