package com.financial.transfer.repository;

import com.financial.transfer.entity.Agendamento;
import com.financial.transfer.enums.EstadoAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.OffsetDateTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    @Query(value = """
        SELECT * FROM agendamento
        WHERE estado = 'PENDENTE'
          AND executar_em <= :agora
          AND (processando_em IS NULL OR processando_em < :timeout)
        ORDER BY executar_em ASC
        LIMIT :limite
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<Agendamento> buscarPendentesParaProcessar(
        @Param("agora") OffsetDateTime agora,
        @Param("timeout") OffsetDateTime timeout,
        @Param("limite") int limite
    );

   
    @Modifying
    @Query("""
        UPDATE Agendamento a
        SET a.processandoEm = :agora
        WHERE a.id IN :ids AND a.estado = 'PENDENTE'
        """)
    int marcarProcessando(@Param("ids") List<Long> ids, @Param("agora") OffsetDateTime agora);

    long countByEstado(EstadoAgendamento estado);


    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.estado = 'PENDENTE'")
    Long countPendentes();
}
