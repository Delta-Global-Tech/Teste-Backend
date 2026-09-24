package teste.delta.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import teste.delta.model.Idempotencia;
import teste.delta.model.IdempotenciaId;

/**
 * IdempotenciaRepository
 */
public interface IdempotenciaRepository extends JpaRepository<Idempotencia, IdempotenciaId>{

    /**
     * Reserva a chave. Se outra transação já reservou a mesma chave e ainda não terminou,
     * o Postgres segura este INSERT até ela fazer commit/rollback. Retorna 0 se a chave já existia.
     */
    @Modifying
    @Query(value = """
        INSERT INTO IDEMPOTENCIA (CHAVE, ENDPOINT, HASH_REQUISICAO, RESPOSTA_JSON, STATUS_HTTP, CRIADO_EM)
        VALUES (:chave, :endpoint, :hashRequisicao, '', 0, now())
        ON CONFLICT (CHAVE, ENDPOINT) DO NOTHING
        """, nativeQuery = true)
    int reservar(
        @Param("chave") String chave,
        @Param("endpoint") String endpoint,
        @Param("hashRequisicao") String hashRequisicao
    );

    @Modifying
    @Query("""
        UPDATE Idempotencia i SET i.respostaJson = :respostaJson, i.statusHttp = :statusHttp
        WHERE i.chave = :chave AND i.endpoint = :endpoint
        """)
    int registrarResposta(
        @Param("chave") String chave,
        @Param("endpoint") String endpoint,
        @Param("respostaJson") String respostaJson,
        @Param("statusHttp") int statusHttp
    );

    Optional<Idempotencia> findByChaveAndEndpoint(String chave, String endpoint);
}
