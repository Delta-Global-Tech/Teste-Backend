package com.financial.transfer.repository;

import com.financial.transfer.entity.Conta;
import com.financial.transfer.enums.EstadoConta;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ContaRepository extends JpaRepository<Conta, Long> {

    Optional<Conta> findByNumero(String numero);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Conta c WHERE c.numero = :numero")
    Optional<Conta> findByNumeroWithLock(@Param("numero") String numero);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Conta c WHERE c.id = :id")
    Optional<Conta> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Conta c WHERE c.id IN :ids ORDER BY c.id ASC")
    List<Conta> findAllByIdWithLock(@Param("ids") List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Conta c WHERE c.numero IN :numeros ORDER BY c.id ASC")
    List<Conta> findAllByNumeroWithLock(@Param("numeros") List<String> numeros);

    long countByEstado(EstadoConta estado);
}
