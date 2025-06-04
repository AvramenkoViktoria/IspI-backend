package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {}
