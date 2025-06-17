package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.Complaint;
import org.docpirates.ispi.entity.Deal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByDeal(Deal deal);
    void deleteAllByDealId(Long dealId);
}