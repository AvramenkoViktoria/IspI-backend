package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByNameIgnoreCase(String name);
}
