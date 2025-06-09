package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.SubjectArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubjectAreaRepository extends JpaRepository<SubjectArea, Long> {
    boolean existsByName(String name);
    Optional<SubjectArea> findByNameIgnoreCase(String name);
}
