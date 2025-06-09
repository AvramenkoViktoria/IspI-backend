package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.Document;
import org.docpirates.ispi.entity.User;
import org.docpirates.ispi.entity.UserFavorites;
import org.docpirates.ispi.entity.UserFavoritesId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFavoritesRepository extends JpaRepository<UserFavorites, UserFavoritesId> {
    List<UserFavorites> findByUserId(Long userId);
    Optional<UserFavorites> findByUserIdAndDocumentId(Long userId, Long documentId);
    void deleteByUserIdAndDocumentId(Long userId, Long documentId);
    boolean existsByUserAndDocument(User user, Document document);
}
