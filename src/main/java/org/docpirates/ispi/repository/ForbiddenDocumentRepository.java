package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.ForbiddenDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForbiddenDocumentRepository extends JpaRepository<ForbiddenDocument, Long> {
     boolean existsByName(String name);
}
