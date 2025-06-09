package org.docpirates.ispi.repository;

import java.util.List;
import org.docpirates.ispi.entity.Deal;
import org.docpirates.ispi.entity.Post;
import org.docpirates.ispi.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DealRepository extends JpaRepository<Deal, Long> {
    Optional<Deal> findByTeacher(Teacher teacher);
    Optional<Deal> findByPost(Post post);
    List<Deal> findByPostIn(List<Post> posts);
    Optional<Deal> findByPostId(Long postId);
}