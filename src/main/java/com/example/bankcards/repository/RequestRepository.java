package com.example.bankcards.repository;

import com.example.bankcards.entity.RequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RequestRepository extends JpaRepository<RequestEntity, UUID>{

}
