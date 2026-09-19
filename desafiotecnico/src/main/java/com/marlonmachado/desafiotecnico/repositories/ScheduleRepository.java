package com.marlonmachado.desafiotecnico.repositories;

import com.marlonmachado.desafiotecnico.entities.Schedule;
import com.marlonmachado.desafiotecnico.entities.enums.ScheduleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "SELECT * FROM scheduled " +
            "WHERE state = :estado AND execute_at <= :agora " +
            "ORDER BY execute_at ASC " +
            "LIMIT :tamanhoLote " +
            "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<Schedule> buscarVencidosParaProcessar(@Param("estado") String estado,
                                               @Param("agora") Instant agora,
                                               @Param("tamanhoLote") int tamanhoLote);

    long countByState(ScheduleStatus state);
}
