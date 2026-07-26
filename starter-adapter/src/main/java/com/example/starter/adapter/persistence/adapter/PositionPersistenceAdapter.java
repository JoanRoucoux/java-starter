package com.example.starter.adapter.persistence.adapter;

import com.example.starter.adapter.persistence.entity.PositionEntity;
import com.example.starter.adapter.persistence.repository.PositionJpaRepository;
import com.example.starter.domain.model.Position;
import com.example.starter.domain.port.out.LoadPositionPort;
import com.example.starter.domain.port.out.SavePositionPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Outbound adapter: implements the domain's read and write ports with Spring Data JPA. */
@Component
class PositionPersistenceAdapter implements LoadPositionPort, SavePositionPort {

    private final PositionJpaRepository jpaRepository;

    PositionPersistenceAdapter(PositionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Position save(Position position) {
        return jpaRepository.save(PositionEntity.fromDomain(position)).toDomain();
    }

    @Override
    public Optional<Position> findById(UUID id) {
        return jpaRepository.findById(id).map(PositionEntity::toDomain);
    }
}
