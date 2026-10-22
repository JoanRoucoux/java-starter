package com.example.starter.adapter.persistence.repository;

import com.example.starter.adapter.persistence.entity.PositionEntity;
import com.example.starter.domain.model.PositionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface OpenPositionJpaRepository extends Repository<PositionEntity, UUID> {
    List<PositionEntity> findAllByStatus(PositionStatus status);
}
