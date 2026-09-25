package com.financial.transfer.repository;

import com.financial.transfer.entity.Movimento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface MovimentoRepository extends JpaRepository<Movimento, Long> {

    @Query("""
        SELECT COALESCE(MAX(m.sequencia), 0) + 1
        FROM Movimento m
        WHERE m.conta.id = :contaId
        """)
    Long proximaSequencia(@Param("contaId") Long contaId);

    @Query("""
        SELECT m.saldoAposCentavos
        FROM Movimento m
        WHERE m.conta.id = :contaId
          AND m.sequencia = (
              SELECT MAX(m2.sequencia)
              FROM Movimento m2
              WHERE m2.conta.id = :contaId
          )
        """)
    Optional<Long> ultimoSaldo(@Param("contaId") Long contaId);

    @Query("""
        SELECT m
        FROM Movimento m
        WHERE m.conta.id = :contaId
        ORDER BY m.sequencia DESC
        """)
    Page<Movimento> findExtrato(
        @Param("contaId") Long contaId,
        Pageable pageable
    );

    @Query("""
        SELECT m
        FROM Movimento m
        WHERE m.conta.id = :contaId
          AND m.criadoEm >= :de
        ORDER BY m.sequencia DESC
        """)
    Page<Movimento> findExtratoDesde(
        @Param("contaId") Long contaId,
        @Param("de") OffsetDateTime de,
        Pageable pageable
    );

    @Query("""
        SELECT m
        FROM Movimento m
        WHERE m.conta.id = :contaId
          AND m.criadoEm <= :ate
        ORDER BY m.sequencia DESC
        """)
    Page<Movimento> findExtratoAte(
        @Param("contaId") Long contaId,
        @Param("ate") OffsetDateTime ate,
        Pageable pageable
    );

    @Query("""
        SELECT m
        FROM Movimento m
        WHERE m.conta.id = :contaId
          AND m.criadoEm >= :de
          AND m.criadoEm <= :ate
        ORDER BY m.sequencia DESC
        """)
    Page<Movimento> findExtratoEntre(
        @Param("contaId") Long contaId,
        @Param("de") OffsetDateTime de,
        @Param("ate") OffsetDateTime ate,
        Pageable pageable
    );

    @Query("""
        SELECT COUNT(DISTINCT m.sequencia)
        FROM Movimento m
        WHERE m.conta.id = :contaId
        """)
    Long contarSequencias(@Param("contaId") Long contaId);

    @Query("""
        SELECT COALESCE(MAX(m.sequencia), 0)
        FROM Movimento m
        WHERE m.conta.id = :contaId
        """)
    Long maxSequencia(@Param("contaId") Long contaId);

    @Query("""
        SELECT COALESCE(SUM(
            CASE m.tipo
                WHEN 'ENTRADA' THEN m.valorCentavos
                WHEN 'SAIDA' THEN -m.valorCentavos
            END
        ), 0)
        FROM Movimento m
        """)
    Long somaGlobal();

    @Query("""
        SELECT m.saldoAposCentavos
        FROM Movimento m
        WHERE m.conta.id = :contaId
          AND m.sequencia = (
              SELECT MAX(m2.sequencia)
              FROM Movimento m2
              WHERE m2.conta.id = :contaId
          )
        """)
    Optional<Long> saldoAposUltimoMovimento(
        @Param("contaId") Long contaId
    );
}