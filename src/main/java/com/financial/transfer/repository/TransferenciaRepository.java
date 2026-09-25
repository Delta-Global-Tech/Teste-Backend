package com.financial.transfer.repository;

import com.financial.transfer.entity.Transferencia;
import com.financial.transfer.enums.EstadoTransferencia;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {

    @Query("""
        SELECT COALESCE(SUM(t.valorCentavos), 0)
        FROM Transferencia t
        WHERE t.contaOrigem.id = :contaId
          AND t.estado = 'CONFIRMADA'
          AND t.criadaEm >= :inicioDia
          AND t.criadaEm <  :fimDia
        """)
    Long somaTransferidaHoje(
            @Param("contaId") Long contaId,
            @Param("inicioDia") OffsetDateTime inicioDia,
            @Param("fimDia") OffsetDateTime fimDia
    );

    @Query("""
        SELECT COUNT(t)
        FROM Transferencia t
        WHERE t.estado = 'CONFIRMADA'
          AND t.criadaEm >= :desde
        """)
    Long contarTransferenciasNaJanela(@Param("desde") OffsetDateTime desde);

    @Query("""
        SELECT COALESCE(SUM(t.valorCentavos), 0)
        FROM Transferencia t
        WHERE t.estado = 'CONFIRMADA'
          AND t.criadaEm >= :desde
        """)
    Long somaValorNaJanela(@Param("desde") OffsetDateTime desde);

    @Query("""
        SELECT COALESCE(SUM(t.taxaCentavos), 0)
        FROM Transferencia t
        WHERE t.estado = 'CONFIRMADA'
          AND t.criadaEm >= :desde
        """)
    Long somaTaxasNaJanela(@Param("desde") OffsetDateTime desde);

    @Query("""
        SELECT COALESCE(AVG(CAST(t.valorCentavos AS double)), 0)
        FROM Transferencia t
        WHERE t.estado = 'CONFIRMADA'
          AND t.criadaEm >= :desde
        """)
    Double ticketMedioNaJanela(@Param("desde") OffsetDateTime desde);

    @Query("""
        SELECT COUNT(t)
        FROM Transferencia t
        WHERE t.estado = 'ESTORNADA'
          AND t.criadaEm >= :desde
        """)
    Long contarEstornosNaJanela(@Param("desde") OffsetDateTime desde);

    @Query("SELECT COALESCE(SUM(c.saldoCentavos), 0) FROM Conta c WHERE c.usuario IS NOT NULL")
    Long somasSaldoUsuarios();

    @Query("""
        SELECT COALESCE(SUM(
            CASE m.tipo
                WHEN 'ENTRADA' THEN  m.valorCentavos
                WHEN 'SAIDA'   THEN -m.valorCentavos
            END
        ), 0)
        FROM Movimento m
        """)
    Long somaMovimentos();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transferencia t WHERE t.id = :id")
    Optional<Transferencia> findByIdForUpdate(@Param("id") Long id);

    boolean existsByTransferenciaOriginalIdAndEstado(
            Long transferenciaOriginalId,
            EstadoTransferencia estado
    );
}
