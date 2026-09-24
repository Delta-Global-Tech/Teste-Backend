package teste.delta.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import teste.delta.model.Transferencia;

/**
 * Transferencia
 */
public interface TransferenciaRepository extends JpaRepository<Transferencia, UUID> {

    // Dois estornos simultâneos da mesma transferência: o segundo espera aqui e depois vê ESTORNADA.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transferencia t WHERE t.id = :id")
    Optional<Transferencia> travarPorId(@Param("id") UUID id);
}
