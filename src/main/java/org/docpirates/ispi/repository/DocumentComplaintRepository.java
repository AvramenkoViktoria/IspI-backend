package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.DocumentComplaint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentComplaintRepository extends JpaRepository<DocumentComplaint, Long> {
}
