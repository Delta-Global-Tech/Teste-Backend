package com.marlonmachado.desafiotecnico.repositories;

import com.marlonmachado.desafiotecnico.entities.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByEndpointAndKey(String endpoint, String idempotencyKey);
}
