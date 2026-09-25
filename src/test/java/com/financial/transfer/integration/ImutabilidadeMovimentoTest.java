package com.financial.transfer.integration;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DisplayName("Imutabilidade do movimento — trigger no banco")
class ImutabilidadeMovimentoTest extends BaseIntegrationTest {

    @Autowired
    EntityManager em;

    @Test
    @Transactional
    @DisplayName("UPDATE em movimento deve ser rejeitado pelo banco (trigger)")
    void updateEmMovimentoDeveSerRejeitado() {
        Long count = (Long) em.createQuery("SELECT COUNT(m) FROM Movimento m").getSingleResult();
        if (count == 0) return; // sem movimentos ainda, teste não aplicável

        assertThatThrownBy(() -> {
            em.createNativeQuery("UPDATE movimento SET valor_centavos = 1 WHERE id = (SELECT MIN(id) FROM movimento)")
              .executeUpdate();
            em.flush();
        }).isInstanceOf(Exception.class)
          .hasMessageContaining("MOVIMENTO_IMUTAVEL");
    }

    @Test
    @Transactional
    @DisplayName("DELETE em movimento deve ser rejeitado pelo banco (trigger)")
    void deleteEmMovimentoDeveSerRejeitado() {
        Long count = (Long) em.createQuery("SELECT COUNT(m) FROM Movimento m").getSingleResult();
        if (count == 0) return;

        assertThatThrownBy(() -> {
            em.createNativeQuery("DELETE FROM movimento WHERE id = (SELECT MIN(id) FROM movimento)")
              .executeUpdate();
            em.flush();
        }).isInstanceOf(Exception.class)
          .hasMessageContaining("MOVIMENTO_IMUTAVEL");
    }
}
