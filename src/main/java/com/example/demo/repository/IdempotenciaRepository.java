package com.example.demo.repository;

import com.example.demo.model.Idempotencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IdempotenciaRepository extends JpaRepository<Idempotencia, String> {

    @Modifying // vai modificar
    @Query(value = """
        INSERT INTO idempotencia (
            chave,
            endpoint,
            hash_requisicao,
            criado_em
        )
        VALUES (
            :chave,
            :endpoint,
            :hash,
            CURRENT_TIMESTAMP
        )
        ON CONFLICT (endpoint, chave) DO NOTHING
        """, nativeQuery = true)
    int criarSeNaoExistir(
            @Param("chave") String chave,
            @Param("endpoint") String endpoint,
            @Param("hash") String hash
    );

    @Query("""
        SELECT i
        FROM Idempotencia i
        WHERE i.id.endpoint = :endpoint
          AND i.id.chave = :chave
    """)
    Optional<Idempotencia> findByEndpointAndChave(
            @Param("endpoint") String endpoint,
            @Param("chave") String chave
    );
}