package com.example.starter.adapter.persistence.repository;

import com.example.starter.adapter.persistence.entity.PositionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionJpaRepository extends JpaRepository<PositionEntity, UUID> {}
