package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgreementRepository extends JpaRepository<Agreement, Long> {}