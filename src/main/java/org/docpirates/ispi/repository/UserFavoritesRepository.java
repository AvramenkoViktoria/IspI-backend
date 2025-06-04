package org.docpirates.ispi.repository;

import org.docpirates.ispi.entity.UserFavorites;
import org.docpirates.ispi.entity.UserFavoritesId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFavoritesRepository extends JpaRepository<UserFavorites, UserFavoritesId> {}
