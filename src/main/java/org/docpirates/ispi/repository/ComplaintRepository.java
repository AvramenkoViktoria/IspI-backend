package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.Complaint;
import org.docpirates.ispi.entity.Deal;
import org.docpirates.ispi.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> ,
        JpaSpecificationExecutor<Complaint> {
    List<Complaint> findByDeal(Deal deal);
    void deleteAllByDealId(Long dealId);
}