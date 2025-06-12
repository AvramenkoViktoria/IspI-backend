package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.Post;
import org.docpirates.ispi.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    List<Post> findByStudent(Student student);
    List<Post> findAllByStudentId(Long studentId);
}