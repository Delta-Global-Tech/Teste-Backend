package com.example.demo.repository;

import com.example.demo.enums.EstadoTransferencia;
import com.example.demo.model.Transferencia;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {

    @Query("""
        SELECT COALESCE(SUM(t.valorCentavos), 0)
        FROM Transferencia t
        WHERE t.contaOrigem.id = :contaId
          AND t.estado = :estado
          AND t.concluidaEm >= :inicio
          AND t.concluidaEm < :fim
    """)
    long somarTransferenciasDoDia(
            @Param("contaId") Long contaId,
            @Param("estado") EstadoTransferencia estado,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE) // lock pessimista
    @Query("""
        SELECT t
        FROM Transferencia t
        WHERE t.id = :id
    """)
    Optional<Transferencia> findByIdForUpdate(@Param("id") Long id);

}
