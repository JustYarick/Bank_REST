package com.example.bankcards.repository;

import com.example.bankcards.entity.CardEntity;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<CardEntity, UUID>, JpaSpecificationExecutor<CardEntity> {

    boolean existsByCardNumberEncrypted(String cardNumberEncrypted);

    Optional<CardEntity> findByCardNumberEncrypted(String cardNumberEncrypted);
}
