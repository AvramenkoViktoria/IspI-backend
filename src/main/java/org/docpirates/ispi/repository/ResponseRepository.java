package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.Response;
import org.docpirates.ispi.enums.RespondentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ResponseRepository extends JpaRepository<Response, Long> {
    @Query("""
    SELECT r FROM Response r
    WHERE r.post.id = :postId
      AND r.respondentType = :respondentType
      AND r.prevResponseId IN (
          SELECT t.id FROM Response t WHERE t.respondent.id = :teacherId
      )
    ORDER BY r.creationDate DESC
    """)
    Optional<Response> findNewestStudentResponseByPostIdAndTeacherResponse(
            @Param("postId") Long postId,
            @Param("teacherId") Long teacherId,
            @Param("respondentType") RespondentType respondentType
    );

    Optional<Response> findTopByPostIdAndRespondentIdOrderByCreationDateDesc(Long postId, Long respondentId);
    List<Response> findAllByPostId(Long postId);
    List<Response> findAllByRespondentId(Long respondentId);
    List<Response> findAllByPostIdIn(Set<Long> postIds);
    List<Response> findAllByPostIdAndRespondentId(Long postId, Long respondentId);
}