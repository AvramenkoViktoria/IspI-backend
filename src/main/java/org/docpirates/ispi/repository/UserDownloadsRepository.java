package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.User;
import org.docpirates.ispi.entity.UserDownloads;
import org.docpirates.ispi.entity.UserDownloadsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface UserDownloadsRepository extends JpaRepository<UserDownloads, UserDownloadsId> {

    @Query("""
        SELECT COUNT(ud)
        FROM UserDownloads ud
        WHERE ud.user = :user
          AND ud.document.uploadedAt > :fromDate
    """)
    int countDownloadsSince(
            @Param("user") User user,
            @Param("fromDate") LocalDateTime fromDate
    );
}
