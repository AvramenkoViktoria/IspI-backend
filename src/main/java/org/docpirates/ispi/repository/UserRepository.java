package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}