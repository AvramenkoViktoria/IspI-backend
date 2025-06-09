package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.Institution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {
    Optional<Institution> findByName(String name);
    Optional<Institution> findByNameIgnoreCase(String name);
}
