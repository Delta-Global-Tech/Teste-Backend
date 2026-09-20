package com.marlonmachado.desafiotecnico.repositories;

import com.marlonmachado.desafiotecnico.dto.SummaryDTO;
import com.marlonmachado.desafiotecnico.entities.Transfer;
import com.marlonmachado.desafiotecnico.projections.TransferWindowSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByOriginalTransferId(Long originalTransferId);

    @Query(value = """
            SELECT COALESCE(SUM(t.amount_cents), 0)
            FROM transfers t
            WHERE t.source_account_id = :accountId
              AND t.status = 'CONFIRMED'
              AND t.created_at >= :startOfDay
              AND t.created_at < :endOfDay
            """, nativeQuery = true)
    long sumDebitedToday(@Param("accountId") Long accountId,
                         @Param("startOfDay") Instant startOfDay,
                         @Param("endOfDay") Instant endOfDay);

    @Query("""
            SELECT
                COUNT(t) as totalTransferencias,
                COALESCE(SUM(t.amountCents), 0) as valorTransferido,
                COALESCE(SUM(t.feeCents), 0) as taxas,
                COALESCE(AVG(t.amountCents), 0) as ticketMedio,
                COUNT(CASE WHEN t.originalTransfer IS NOT NULL THEN 1 END) as totalEstornos
            FROM Transfer t
            WHERE t.createdAt >= :inicioJanela
            """)
    TransferWindowSummary summarizeWindow(@Param("inicioJanela") Instant inicioJanela);
}