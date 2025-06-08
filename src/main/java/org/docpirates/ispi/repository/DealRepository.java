package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.Deal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DealRepository extends JpaRepository<Deal, Long> {}