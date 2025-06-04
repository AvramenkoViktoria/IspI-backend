package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {}