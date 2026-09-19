package com.marlonmachado.desafiotecnico.repositories;

import com.marlonmachado.desafiotecnico.entities.Movement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface MovementRepository extends JpaRepository<Movement, Long> {

    @Query(value = """
            SELECT * FROM movements
            WHERE account_id = :accountId
            ORDER BY sequence_number DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Movement> findLastMovementByAccountId(@Param("accountId") Long accountId);

    Page<Movement> findByAccountId(Long accountId, Pageable pageable);

    Page<Movement> findByAccountIdAndCreatedAtBetween(Long accountId, Instant de, Instant ate, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(
                CASE WHEN m.type = 'CREDIT' THEN m.amountCents ELSE -m.amountCents END
            ), 0)
            FROM Movement m
            """)
    Long sumAllMovements();
}
