package com.example.starter.adapter.persistence.repository;

import com.example.starter.adapter.persistence.entity.PositionEntity;
import com.example.starter.domain.model.PositionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Kept apart from {@link PositionJpaRepository} because it exists only for the batch module: an
 * application generated without the batch drops this file whole, rather than having a query
 * removed from a shared interface.
 */
public interface OpenPositionJpaRepository extends Repository<PositionEntity, UUID> {

    List<PositionEntity> findAllByStatus(PositionStatus status);
}
