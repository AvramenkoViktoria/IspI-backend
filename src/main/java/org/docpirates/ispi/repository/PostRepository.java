package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {}