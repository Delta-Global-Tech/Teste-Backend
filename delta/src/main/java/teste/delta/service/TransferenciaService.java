package teste.delta.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import teste.delta.dto.requests.RequestEstorno;
import teste.delta.dto.requests.RequestTransferencia;
import teste.delta.dto.response.ResponseComprovanteTransferencia;
import teste.delta.dto.response.RespostaIdempotente;
import teste.delta.exception.ApiException;
import teste.delta.model.Conta;
import teste.delta.model.Transferencia;
import teste.delta.model.enums.EstadoConta;
import teste.delta.model.enums.EstadoTransferencia;
import teste.delta.model.enums.TipoMovimento;
import teste.delta.repository.TransferenciaRepository;
import teste.delta.util.Dinheiro;

@Service
public class TransferenciaService {

    private static Logger logger = LoggerFactory.getLogger(TransferenciaService.class);

    private static final int VALOR_ISENTO_CENTAVOS = 10_000;

    private static final int TAXA_MINIMA_CENTAVOS = 100;

    private static final int TAXA_MAXIMA_CENTAVOS = 2_000;

    @Autowired
    TransferenciaRepository transferenciaRepository;

    @Autowired
    MovimentoService movimentoService;

    @Autowired
    ContaService contaService;

    @Autowired
    IdempotenciaService idempotenciaService;


    /**
     * Até R$ 100,00 é isento; acima, 1% com mínimo de R$ 1,00 e teto de R$ 20,00.
     * 1% em centavos = valor / 100, arredondando meio centavo para cima: (valor + 50) / 100. Só inteiros.
     */
    private int calculateRate(int valorEmCentavos){
        if (valorEmCentavos <= VALOR_ISENTO_CENTAVOS) {
            return 0;
        }
        int taxa = (valorEmCentavos + 50) / 100;
        return Math.min(Math.max(taxa, TAXA_MINIMA_CENTAVOS), TAXA_MAXIMA_CENTAVOS);
    }

    private String hashRequisicao(RequestTransferencia request){
        String conteudo = String.join("|",
            request.contaOrigem(),
            request.contaDestino(),
            request.valor().stripTrailingZeros().toPlainString()
        );
        return idempotenciaService.hash(conteudo);
    }

    private String hashRequisicaoEstorno(UUID transferId, RequestEstorno request){
        String conteudo = String.join("|",
            transferId.toString(),
            Objects.toString(request.motivo(), "")
        );
        return idempotenciaService.hash(conteudo);
    }

    // Fazer transferencia
    // Chave de idempotência, transferência e movimentos são gravados na mesma transação.
    @Transactional
    public RespostaIdempotente fazerTransferenciaEntreContas(UUID idempotencyKey, RequestTransferencia requestTransferencia){
        return idempotenciaService.executar(
            idempotencyKey.toString(),
            "POST /transferencias",
            this.hashRequisicao(requestTransferencia),
            HttpStatus.CREATED,
            () -> this.transferir(
                requestTransferencia.contaOrigem(),
                requestTransferencia.contaDestino(),
                Dinheiro.paraCentavos(requestTransferencia.valor())
            )
        );
    }

    /**
     * Executa a transferência. Público para ser reaproveitado pelo job de agendamento.
     *
     * Concorrência: as três contas envolvidas (origem, destino e SISTEMA-TAXAS) são travadas com
     * SELECT ... FOR UPDATE em ordem de id antes de qualquer leitura de saldo. Assim:
     * - duas transferências da mesma origem são serializadas; a segunda lê o saldo e a soma do
     *   limite diário já com a primeira commitada;
     * - A->B e B->A travam as contas na mesma ordem, então uma espera a outra em vez de deadlock.
     */
    @Transactional
    public ResponseComprovanteTransferencia transferir(String numeroOrigem, String numeroDestino, BigDecimal valorCentavos){
        if (valorCentavos.signum() <= 0) {
            throw new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "VALOR_INVALIDO",
                "O valor da transferência precisa ser maior que zero"
            );
        }
        UUID idOrigem = contaService.resolverId(numeroOrigem);
        UUID idDestino = contaService.resolverId(numeroDestino);
        UUID idTaxas = contaService.resolverId(ContaService.SISTEMA_TAXAS);

        Map<UUID, Conta> contas = contaService.travarContas(List.of(idOrigem, idDestino, idTaxas));
        Conta contaOrigem = contas.get(idOrigem);
        Conta contaDestino = contas.get(idDestino);
        Conta contaTaxas = contas.get(idTaxas);

        contaService.checkIfAccountsCanTransfer(contaOrigem, contaDestino);

        // Acima de R$ 2.000,00 a taxa já está no teto, então limitar o valor ao máximo de int não muda o resultado.
        int valorParaTaxa = valorCentavos.min(BigDecimal.valueOf(Integer.MAX_VALUE - 50)).intValue();
        BigDecimal taxa = BigDecimal.valueOf(this.calculateRate(valorParaTaxa));
        BigDecimal totalDebitado = valorCentavos.add(taxa);
        if (contaOrigem.getSaldoCentavos().compareTo(totalDebitado) < 0) {
            throw new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "SALDO_INSUFICIENTE",
                "Saldo insuficiente para transferir " + Dinheiro.formatar(valorCentavos)
                    + " mais taxa de " + Dinheiro.formatar(taxa)
            );
        }

        BigDecimal usadoHoje = movimentoService.totalTransferidoHoje(contaOrigem.getId());
        BigDecimal limite = BigDecimal.valueOf(contaOrigem.getLimiteDiarioCentavos());
        if (usadoHoje.add(valorCentavos).compareTo(limite) > 0) {
            throw new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "LIMITE_DIARIO_EXCEDIDO",
                "Limite diário excedido: já transferido hoje " + Dinheiro.formatar(usadoHoje)
                    + " de " + Dinheiro.formatar(limite)
            );
        }

        Transferencia transferencia = new Transferencia();
        transferencia.setContaOrigemId(contaOrigem.getId());
        transferencia.setContaDestinoId(contaDestino.getId());
        transferencia.setValorCentavos(valorCentavos);
        transferencia.setTaxaCentavos(taxa.intValueExact());
        transferencia.setEstado(EstadoTransferencia.CRIADA);
        transferencia.setCriadaEm(OffsetDateTime.now());
        transferencia = transferenciaRepository.save(transferencia);

        movimentoService.lancar(contaOrigem, transferencia.getId(), TipoMovimento.SAIDA, valorCentavos, false);
        movimentoService.lancar(contaDestino, transferencia.getId(), TipoMovimento.ENTRADA, valorCentavos, false);
        if (taxa.signum() > 0) {
            movimentoService.lancar(contaOrigem, transferencia.getId(), TipoMovimento.SAIDA, taxa, true);
            movimentoService.lancar(contaTaxas, transferencia.getId(), TipoMovimento.ENTRADA, taxa, true);
        }

        transferencia.setEstado(EstadoTransferencia.CONFIRMADA);
        transferencia.setConcluidaEm(OffsetDateTime.now());
        logger.info("Transferencia {} confirmada", transferencia.getId());
        return ResponseComprovanteTransferencia.fromEntity(transferencia, contaOrigem, contaDestino);
    }

    @Transactional
    public RespostaIdempotente fazerEstorno(UUID idempotencyKey, UUID transferId, RequestEstorno requestEstorno){
        return idempotenciaService.executar(
            idempotencyKey.toString(),
            "POST /transferencias/{id}/estorno",
            this.hashRequisicaoEstorno(transferId, requestEstorno),
            HttpStatus.CREATED,
            () -> this.fazerEstorno(transferId)
        );
    }

    /**
     * Cria uma transferência nova, no sentido contrário, devolvendo valor e taxa.
     * A original só muda de estado para ESTORNADA; valor e movimentos dela não são tocados.
     */
    @Transactional
    public ResponseComprovanteTransferencia fazerEstorno(UUID transferId){
        // Lock na transferência original: um segundo estorno simultâneo espera aqui e depois vê ESTORNADA.
        Transferencia original = transferenciaRepository.travarPorId(transferId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "TRANSFERENCIA_INEXISTENTE",
                "Transferencia inexistente"
            ));

        if (original.getTransferenciaOriginalId() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "ESTORNO_NAO_ESTORNAVEL", "Um estorno não pode ser estornado");
        }
        if (original.getEstado() == EstadoTransferencia.ESTORNADA) {
            throw new ApiException(HttpStatus.CONFLICT, "TRANSFERENCIA_JA_ESTORNADA", "A transferência já foi estornada");
        }
        if (original.getEstado() != EstadoTransferencia.CONFIRMADA) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "TRANSFERENCIA_NAO_CONFIRMADA",
                "Só transferências confirmadas podem ser estornadas"
            );
        }

        BigDecimal valor = original.getValorCentavos();
        BigDecimal taxa = BigDecimal.valueOf(original.getTaxaCentavos());

        List<UUID> ids = new ArrayList<>(List.of(original.getContaOrigemId(), original.getContaDestinoId()));
        UUID idTaxas = null;
        if (taxa.signum() > 0) {
            idTaxas = contaService.resolverId(ContaService.SISTEMA_TAXAS);
            ids.add(idTaxas);
        }
        Map<UUID, Conta> contas = contaService.travarContas(ids);
        // No estorno quem paga é quem recebeu, e quem recebe de volta é quem pagou.
        Conta pagador = contas.get(original.getContaDestinoId());
        Conta recebedor = contas.get(original.getContaOrigemId());

        if (recebedor.getUsuarioId() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "DEPOSITO_NAO_ESTORNAVEL", "Depósitos não podem ser estornados");
        }
        if (recebedor.getEstado() == EstadoConta.ENCERRADA) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "CONTA_DESTINO_ENCERRADA",
                "A conta que receberia o estorno está encerrada"
            );
        }
        // Decisão em aberto nº 1: sem saldo para devolver, o estorno é recusado (a conta de usuário nunca fica negativa).
        if (pagador.getSaldoCentavos().compareTo(valor) < 0) {
            throw new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "SALDO_INSUFICIENTE_PARA_ESTORNO",
                "A conta " + pagador.getNumero() + " não tem saldo para devolver " + Dinheiro.formatar(valor)
            );
        }

        OffsetDateTime agora = OffsetDateTime.now();
        Transferencia estorno = new Transferencia();
        estorno.setContaOrigemId(pagador.getId());
        estorno.setContaDestinoId(recebedor.getId());
        estorno.setValorCentavos(valor);
        estorno.setTaxaCentavos(0);
        estorno.setEstado(EstadoTransferencia.CONFIRMADA);
        estorno.setTransferenciaOriginalId(original.getId());
        estorno.setCriadaEm(agora);
        estorno.setConcluidaEm(agora);
        estorno = transferenciaRepository.save(estorno);

        movimentoService.lancar(pagador, estorno.getId(), TipoMovimento.SAIDA, valor, false);
        movimentoService.lancar(recebedor, estorno.getId(), TipoMovimento.ENTRADA, valor, false);
        if (taxa.signum() > 0) {
            movimentoService.lancar(contas.get(idTaxas), estorno.getId(), TipoMovimento.SAIDA, taxa, true);
            movimentoService.lancar(recebedor, estorno.getId(), TipoMovimento.ENTRADA, taxa, true);
        }

        original.setEstado(EstadoTransferencia.ESTORNADA);
        logger.info("Transferencia {} estornada pela {}", original.getId(), estorno.getId());
        return ResponseComprovanteTransferencia.fromEntity(estorno, pagador, recebedor);
    }
}
