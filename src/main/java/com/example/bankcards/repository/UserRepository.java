package com.example.bankcards.repository;

import com.example.bankcards.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository
        extends JpaRepository<UserEntity, UUID>,
        JpaSpecificationExecutor<UserEntity> {  // расширить

    boolean existsByUsername(String username);
    Optional<UserEntity> findByUsername(String username);
    boolean existsByEmail(String email);
}