package teste.delta.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import teste.delta.model.Conta;


/**
 * ContaRepository
 */
public interface ContaRepository extends JpaRepository<Conta, UUID> {
    Conta findByNumero(String numeroConta);

    // Projeção escalar: descobre o id sem colocar a entidade no contexto de persistência,
    // assim o SELECT ... FOR UPDATE seguinte sempre carrega o saldo mais recente.
    @Query("SELECT c.id FROM Conta c WHERE c.numero = :numero")
    Optional<UUID> findIdByNumero(@Param("numero") String numero);

    /**
     * SELECT ... FOR UPDATE ordenado por id. Todas as operações travam as contas na mesma ordem,
     * então duas transferências cruzadas (A->B e B->A) nunca ficam esperando uma pela outra.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Conta c WHERE c.id IN :ids ORDER BY c.id")
    List<Conta> travarPorIds(@Param("ids") Collection<UUID> ids);
}
