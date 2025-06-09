package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.Post;
import org.docpirates.ispi.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByStudent(Student student);
}