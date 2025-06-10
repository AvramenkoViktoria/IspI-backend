package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ResponseRepository extends JpaRepository<Response, Long> {
    @Query("""
        select r from Response r
        where r.post.id = :postId
          and r.respondentType = org.docpirates.ispi.entity.RespondentType.STUDENT
          and r.prevResponseId in (
              select t.id from Response t where t.respondent.id = :teacherId
          )
        order by r.creationDate desc
        """)
    Optional<Response> findNewestStudentResponseByPostIdAndTeacherResponse(
            @Param("postId") Long postId,
            @Param("teacherId") Long teacherId
    );

    Optional<Response> findTopByPostIdAndRespondentIdOrderByCreationDateDesc(Long postId, Long respondentId);
    List<Response> findAllByPostId(Long postId);
    List<Response> findAllByRespondentId(Long respondentId);
    List<Response> findAllByPostIdIn(Set<Long> postIds);
    List<Response> findAllByPostIdAndRespondentId(Long postId, Long respondentId);
}