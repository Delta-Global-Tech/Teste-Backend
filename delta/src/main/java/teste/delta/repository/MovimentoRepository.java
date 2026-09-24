package teste.delta.repository;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import teste.delta.model.Movimento;
import teste.delta.model.enums.TipoMovimento;

/**
 * Movimento
 */
public interface MovimentoRepository extends JpaRepository<Movimento, UUID> {

    @Query("""
        SELECT m FROM Movimento m
        WHERE m.contaId = :contaId AND m.criadoEm >= :inicio AND m.criadoEm < :fim
        ORDER BY m.sequencia DESC
        """)
    Page<Movimento> buscarExtrato(
        @Param("contaId") UUID contaId,
        @Param("inicio") OffsetDateTime inicio,
        @Param("fim") OffsetDateTime fim,
        Pageable pageable
    );

    // Só é seguro chamar com a linha da conta travada (FOR UPDATE).
    @Query("SELECT MAX(m.sequencia) FROM Movimento m WHERE m.contaId = :contaId")
    Long ultimaSequencia(@Param("contaId") UUID contaId);

    /**
     * Valor transferido pela conta no período: saídas principais de transferências normais.
     * Fica de fora: taxa, saída de estorno e depósito (conta do sistema).
     */
    @Query("""
        SELECT SUM(m.valorCentavos) FROM Movimento m, Transferencia t
        WHERE t.id = m.transferenciaId
          AND m.contaId = :contaId
          AND m.tipo = :tipo
          AND m.taxa = false
          AND t.transferenciaOriginal IS NULL
          AND m.criadoEm >= :inicio AND m.criadoEm < :fim
        """)
    BigDecimal somarTransferidoNoPeriodo(
        @Param("contaId") UUID contaId,
        @Param("tipo") TipoMovimento tipo,
        @Param("inicio") OffsetDateTime inicio,
        @Param("fim") OffsetDateTime fim
    );
}
