package com.financial.transfer.service;

import com.financial.transfer.repository.AgendamentoRepository;
import com.financial.transfer.repository.ContaRepository;
import com.financial.transfer.repository.IdempotenciaRepository;
import com.financial.transfer.repository.TransferenciaRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DadosService {

    private static final Logger log = LoggerFactory.getLogger(DadosService.class);

    private final EntityManager em;

    public DadosService(EntityManager em) {
        this.em = em;
    }
    
    @Transactional
    public void resetar() {
        log.warn("RESET DE DADOS SOLICITADO — apagando todos os dados transacionais");

        em.createNativeQuery("SET session_replication_role = 'replica'").executeUpdate();

        try {
            em.createNativeQuery("DELETE FROM movimento").executeUpdate();
            em.createNativeQuery("DELETE FROM agendamento").executeUpdate();
            em.createNativeQuery("DELETE FROM idempotencia").executeUpdate();
            em.createNativeQuery("DELETE FROM transferencia").executeUpdate();

            restaurarSaldos();

            log.info("Reset concluído com sucesso");
        } finally {
            em.createNativeQuery("SET session_replication_role = 'origin'").executeUpdate();
        }
    }

    private void restaurarSaldos() {
    	
        // Contas ATIVA: R$ 1.000,00
        em.createNativeQuery("""
            UPDATE conta SET saldo_centavos = 100000, versao = 0
            WHERE numero IN ('CONTA-001','CONTA-002','CONTA-003','CONTA-004','CONTA-005')
            """).executeUpdate();

        // Conta BLOQUEADA: R$ 300,00
        em.createNativeQuery("""
            UPDATE conta SET saldo_centavos = 30000, versao = 0
            WHERE numero = 'CONTA-006'
            """).executeUpdate();

        // Conta ENCERRADA: R$ 0,00
        em.createNativeQuery("""
            UPDATE conta SET saldo_centavos = 0, versao = 0
            WHERE numero = 'CONTA-007'
            """).executeUpdate();

   
        // 5 × 100000 + 30000 = 530000 centavos negativos
        em.createNativeQuery("""
            UPDATE conta SET saldo_centavos = -530000, versao = 0
            WHERE numero = 'SISTEMA-ENTRADA'
            """).executeUpdate();

        // SISTEMA-TAXAS: zero
        em.createNativeQuery("""
            UPDATE conta SET saldo_centavos = 0, versao = 0
            WHERE numero = 'SISTEMA-TAXAS'
            """).executeUpdate();

        reinserirMovimentosIniciais();
    }

    private void reinserirMovimentosIniciais() {
       
        em.createNativeQuery("""
            DO $$
            DECLARE
                v_se_id  BIGINT;
                v_conta  RECORD;
                v_seq    BIGINT := 1;
                v_saldo  BIGINT := 0;
                v_tid    BIGINT;
            BEGIN
                SELECT id INTO v_se_id FROM conta WHERE numero = 'SISTEMA-ENTRADA';

                FOR v_conta IN
                    SELECT c.id AS conta_id, c.numero,
                           CASE c.numero WHEN 'CONTA-006' THEN 30000 ELSE 100000 END AS valor
                    FROM conta c
                    WHERE c.numero IN ('CONTA-001','CONTA-002','CONTA-003','CONTA-004','CONTA-005','CONTA-006')
                    ORDER BY c.id
                LOOP
                    INSERT INTO transferencia (conta_origem_id, conta_destino_id, valor_centavos,
                                               taxa_centavos, estado, concluida_em)
                    VALUES (v_se_id, v_conta.conta_id, v_conta.valor, 0, 'CONFIRMADA', NOW())
                    RETURNING id INTO v_tid;

                    v_saldo := v_saldo - v_conta.valor;
                    INSERT INTO movimento (conta_id, transferencia_id, sequencia, tipo, valor_centavos, saldo_apos_centavos)
                    VALUES (v_se_id, v_tid, v_seq, 'SAIDA', v_conta.valor, v_saldo);
                    v_seq := v_seq + 1;

                    INSERT INTO movimento (conta_id, transferencia_id, sequencia, tipo, valor_centavos, saldo_apos_centavos)
                    VALUES (v_conta.conta_id, v_tid, 1, 'ENTRADA', v_conta.valor, v_conta.valor);
                END LOOP;
            END $$;
            """).executeUpdate();
    }
}
