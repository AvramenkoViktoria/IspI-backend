package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {}