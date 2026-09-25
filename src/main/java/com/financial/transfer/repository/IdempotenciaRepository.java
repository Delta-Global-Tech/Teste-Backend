package com.financial.transfer.repository;

import com.financial.transfer.entity.Idempotencia;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotenciaRepository extends JpaRepository<Idempotencia, Long> {

    Optional<Idempotencia> findByEndpointAndChave(String endpoint, String chave);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Idempotencia i WHERE i.endpoint = :endpoint AND i.chave = :chave")
    Optional<Idempotencia> findByEndpointAndChaveWithLock(
        @Param("endpoint") String endpoint,
        @Param("chave") String chave
    );

    @Query(value = """
        SELECT pg_advisory_xact_lock(
            hashtextextended(CAST(:chave AS text), 0)
        )
        """, nativeQuery = true)
    void adquirirLockTransacional(@Param("chave") String chave);

    boolean existsByEndpointAndChave(String endpoint, String chave);
}