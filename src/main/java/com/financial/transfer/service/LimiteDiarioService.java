package com.financial.transfer.service;

import com.financial.transfer.entity.Conta;
import com.financial.transfer.exception.ApiException;
import com.financial.transfer.repository.TransferenciaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Verifica e valida o limite diário de transferências.
 * - O limite é contado das 00:00 até 23:59:59 no fuso America/Sao_Paulo.
 */
@Service
public class LimiteDiarioService {

    private static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");

    private final TransferenciaRepository transferenciaRepository;

    public LimiteDiarioService(TransferenciaRepository transferenciaRepository) {
        this.transferenciaRepository = transferenciaRepository;
    }

    public void validar(Conta conta, long valorCentavos) {
        long jaUsado = calcularUsadoHoje(conta.getId());
        long disponivel = conta.getLimiteDiarioCentavos() - jaUsado;

        if (valorCentavos > disponivel) {
            throw ApiException.limiteDiarioExcedido();
        }
    }

   
    public long calcularUsadoHoje(Long contaId) {
        OffsetDateTime[] janela = janelaHoje();
        Long soma = transferenciaRepository.somaTransferidaHoje(contaId, janela[0], janela[1]);
        return soma != null ? soma : 0L;
    }

    private OffsetDateTime[] janelaHoje() {
        LocalDate hoje = LocalDate.now(ZONE_SP);
        OffsetDateTime inicio = hoje.atStartOfDay(ZONE_SP).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime fim    = hoje.plusDays(1).atStartOfDay(ZONE_SP).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
        return new OffsetDateTime[]{ inicio, fim };
    }
}
