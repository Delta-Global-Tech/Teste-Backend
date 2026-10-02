package com.example.demo.repository;

import com.example.demo.model.Movimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimentoRepository extends JpaRepository<Movimento, Long> {

    @Query("select coalesce(max(m.sequencia), 0) from Movimento m where m.conta.id = :contaId")
    long findMaiorSequencia(@Param("contaId") Long contaId);
}