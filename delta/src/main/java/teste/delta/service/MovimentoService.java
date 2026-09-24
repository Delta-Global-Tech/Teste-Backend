package teste.delta.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import teste.delta.dto.response.ResponseExtrato;
import teste.delta.exception.ApiException;
import teste.delta.model.Conta;
import teste.delta.model.Movimento;
import teste.delta.model.enums.TipoMovimento;
import teste.delta.repository.MovimentoRepository;

@Service
public class MovimentoService {

    @Autowired
    MovimentoRepository movimentoRepository;

    private static final ZoneId FUSO_BRASIL = ZoneId.of("America/Sao_Paulo");

    private static final int TAMANHO_MAXIMO_PAGINA = 100;

    @Transactional(readOnly = true)
    public ResponseExtrato consultarExtrato(Conta conta, LocalDate de, LocalDate ate, int page, int size){
        if (page < 0 || size < 1 || size > TAMANHO_MAXIMO_PAGINA) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "PAGINACAO_INVALIDA",
                "page precisa ser >= 0 e size entre 1 e " + TAMANHO_MAXIMO_PAGINA
            );
        }
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PERIODO_INVALIDO", "'de' não pode ser depois de 'ate'");
        }

        OffsetDateTime inicio = de != null
            ? de.atStartOfDay(FUSO_BRASIL).toOffsetDateTime()
            : OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime fim = ate != null
            ? ate.plusDays(1).atStartOfDay(FUSO_BRASIL).toOffsetDateTime()
            : OffsetDateTime.of(9999, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        Page<Movimento> movimentos = movimentoRepository.buscarExtrato(
            conta.getId(), inicio, fim, PageRequest.of(page, size)
        );
        return ResponseExtrato.fromEntity(conta, movimentos);
    }

    /**
     * Único ponto que altera saldo: atualiza conta.saldo_centavos e grava o movimento juntos.
     * A conta precisa ter sido carregada com lock (ContaService.travarContas), senão a
     * sequência e o saldo_apos podem ser calculados em cima de um valor desatualizado.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Movimento lancar(
        Conta conta,
        UUID transferenciaId,
        TipoMovimento tipo,
        BigDecimal valorCentavos,
        boolean taxa
    ){
        if (valorCentavos.signum() <= 0) {
            throw new IllegalArgumentException("Movimento precisa ter valor positivo");
        }
        BigDecimal novoSaldo = tipo == TipoMovimento.ENTRADA
            ? conta.getSaldoCentavos().add(valorCentavos)
            : conta.getSaldoCentavos().subtract(valorCentavos);
        if (conta.getUsuarioId() != null && novoSaldo.signum() < 0) {
            // As regras de negócio já checaram antes; chegar aqui é bug. O CHECK do banco também recusa.
            throw new IllegalStateException("Saldo da conta " + conta.getNumero() + " ficaria negativo");
        }
        conta.setSaldoCentavos(novoSaldo);

        Long ultimaSequencia = movimentoRepository.ultimaSequencia(conta.getId());

        Movimento movimento = new Movimento();
        movimento.setContaId(conta.getId());
        movimento.setTransferenciaId(transferenciaId);
        movimento.setSequencia(ultimaSequencia == null ? 1L : ultimaSequencia + 1);
        movimento.setTipo(tipo);
        movimento.setTaxa(taxa);
        movimento.setValorCentavos(valorCentavos);
        movimento.setSaldoAposCentavos(novoSaldo);
        movimento.setCriadoEm(OffsetDateTime.now(ZoneOffset.UTC));
        // Flush imediato: se a mesma conta recebe dois movimentos na transação (valor + taxa),
        // o segundo MAX(sequencia) já enxerga o primeiro.
        return movimentoRepository.saveAndFlush(movimento);
    }

    // Calculado a partir dos movimentos no banco; com a conta travada, o resultado não muda até o commit.
    @Transactional(propagation = Propagation.MANDATORY)
    public BigDecimal totalTransferidoHoje(UUID contaId){
        LocalDate hoje = LocalDate.now(FUSO_BRASIL);
        BigDecimal total = movimentoRepository.somarTransferidoNoPeriodo(
            contaId,
            TipoMovimento.SAIDA,
            hoje.atStartOfDay(FUSO_BRASIL).toOffsetDateTime(),
            hoje.plusDays(1).atStartOfDay(FUSO_BRASIL).toOffsetDateTime()
        );
        return total == null ? BigDecimal.ZERO : total;
    }
}
