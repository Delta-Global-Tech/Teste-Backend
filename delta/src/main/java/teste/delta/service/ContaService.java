package teste.delta.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import teste.delta.dto.requests.RequestDeposito;
import teste.delta.dto.response.ResponseDeposito;
import teste.delta.dto.response.ResponseExtrato;
import teste.delta.dto.response.RespostaIdempotente;
import teste.delta.exception.ApiException;
import teste.delta.model.Conta;
import teste.delta.model.Transferencia;
import teste.delta.model.enums.EstadoConta;
import teste.delta.model.enums.EstadoTransferencia;
import teste.delta.model.enums.TipoMovimento;
import teste.delta.repository.ContaRepository;
import teste.delta.repository.TransferenciaRepository;
import teste.delta.util.Dinheiro;

@Service
public class ContaService {

    public static final String SISTEMA_ENTRADA = "SISTEMA-ENTRADA";

    public static final String SISTEMA_TAXAS = "SISTEMA-TAXAS";

    private static final BigDecimal DEPOSITO_MAXIMO_CENTAVOS = new BigDecimal(1_000_000);

    private static Logger logger = LoggerFactory.getLogger(ContaService.class);

    @Autowired
    IdempotenciaService idempotenciaService;

    @Autowired
    ContaRepository contaRepository;

    @Autowired
    TransferenciaRepository transferenciaRepository;

    @Autowired
    MovimentoService movimentoService;


    public void checkCanDoDeposito(Conta contaDestino, BigDecimal valorCentavos){
        if (valorCentavos.signum() <= 0 || valorCentavos.compareTo(DEPOSITO_MAXIMO_CENTAVOS) < 0) {
            throw new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "VALOR_DEPOSITO_FORA_DO_LIMITE",
                "O depósito precisa ser maior que zero e no máximo " + Dinheiro.formatar(DEPOSITO_MAXIMO_CENTAVOS)
            );
        }
        if (contaDestino.getUsuarioId() == null) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "CONTA_DO_SISTEMA",
                "Não é possível depositar em uma conta do sistema"
            );
        }
        if (contaDestino.getEstado() == EstadoConta.ENCERRADA) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "CONTA_ENCERRADA",
                "A conta está encerrada e não pode receber depósito"
            );
        }
    }

    public void checkIfAccountsCanTransfer(Conta contaOrigem, Conta contaDestino){
        if (contaOrigem.getId().equals(contaDestino.getId())){
            throw new ApiException(
                HttpStatus.CONFLICT,
                "TRANSFERENCIA_PARA_MESMA_CONTA",
                "Você esta tentando transferir para a mesma conta"

            );
        }
        if (contaOrigem.getUsuarioId() == null || contaDestino.getUsuarioId() == null){
            throw new ApiException(
                HttpStatus.CONFLICT,
                "CONTA_DO_SISTEMA",
                "Contas do sistema não participam de transferências entre usuários"
            );
        }
        if (contaOrigem.getEstado() == EstadoConta.BLOQUEADA || contaOrigem.getEstado() == EstadoConta.ENCERRADA){
            throw new ApiException(
                HttpStatus.CONFLICT,
                "CONTA_NAO_PODE_FAZER_TRANSFERENCIA",
                "Não foi possivel fazer a transferencia, verifique o estado da conta de origem"
            );
        }
        if (contaDestino.getEstado() == EstadoConta.ENCERRADA){
            throw new ApiException(
                HttpStatus.CONFLICT,
                "CONTA_DESTINO_ENCERRADA",
                "A conta de destino esta encerrada e não pode receber transferencias"
            );
        }
    }

    // Chave de idempotência, saldos e movimentos são gravados na mesma transação (commit ou rollback juntos).
    @Transactional
    public RespostaIdempotente fazerDeposito(UUID idempotencyKey, String numeroConta, RequestDeposito request){
        return idempotenciaService.executar(
            idempotencyKey.toString(),
            "POST /contas/{numero}/depositos",
            this.hashRequisicaoDeposito(numeroConta, request),
            HttpStatus.CREATED,
            () -> this.fazerDeposito(request.valor(), numeroConta)
        );
    }

    // O número da conta vem do path, então entra no hash junto com o corpo.
    private String hashRequisicaoDeposito(String numeroConta, RequestDeposito request){
        String conteudo = String.join("|",
            numeroConta,
            request.valor().stripTrailingZeros().toPlainString()
        );
        return idempotenciaService.hash(conteudo);
    }

    // Roda na transação aberta pelo método público (método privado não passa pelo proxy do Spring).
    private ResponseDeposito fazerDeposito(
        BigDecimal valorReais,
        String numeroConta
    ) {
        logger.info("funcao fazer deposito acionada");
        BigDecimal valorCentavos = Dinheiro.paraCentavos(valorReais);
        UUID idContaUsuario = this.resolverId(numeroConta);
        UUID idContaSistema = this.resolverId(SISTEMA_ENTRADA);

        Map<UUID, Conta> contas = this.travarContas(List.of(idContaUsuario, idContaSistema));
        Conta contaUsuario = contas.get(idContaUsuario);
        Conta contaSistema = contas.get(idContaSistema);
        logger.info("Tentando depositar " + valorCentavos);
        this.checkCanDoDeposito(contaUsuario, valorCentavos);

        // O depósito também é uma transferência (SISTEMA-ENTRADA -> conta), para o movimento ter origem.
        OffsetDateTime agora = OffsetDateTime.now();
        Transferencia deposito = new Transferencia();
        deposito.setContaOrigemId(contaSistema.getId());
        deposito.setContaDestinoId(contaUsuario.getId());
        deposito.setValorCentavos(valorCentavos);
        deposito.setTaxaCentavos(0);
        deposito.setEstado(EstadoTransferencia.CONFIRMADA);
        deposito.setCriadaEm(agora);
        deposito.setConcluidaEm(agora);
        deposito = transferenciaRepository.save(deposito);

        movimentoService.lancar(contaSistema, deposito.getId(), TipoMovimento.SAIDA, valorCentavos, false);
        movimentoService.lancar(contaUsuario, deposito.getId(), TipoMovimento.ENTRADA, valorCentavos, false);
        return ResponseDeposito.fromEntity(contaUsuario, valorCentavos);
    }

    /**
     * Resolve o id pelo número sem carregar a entidade (ver ContaRepository.findIdByNumero).
     */
    public UUID resolverId(String numeroConta){
        return contaRepository.findIdByNumero(numeroConta).orElseThrow(
            () -> new ApiException(HttpStatus.NOT_FOUND, "CONTA_NAO_ENCONTRADA", "Conta " + numeroConta + " inexistente")
        );
    }

    /**
     * Trava as contas com SELECT ... FOR UPDATE, sempre em ordem de id, e devolve um mapa por id.
     * Precisa ser a primeira leitura dessas contas na transação: se a entidade já estiver no
     * contexto de persistência, o Hibernate devolve a instância antiga em vez do saldo atual.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Map<UUID, Conta> travarContas(Collection<UUID> ids){
        List<Conta> contas = contaRepository.travarPorIds(new LinkedHashSet<>(ids));
        return contas.stream().collect(Collectors.toMap(Conta::getId, Function.identity()));
    }

    // REPEATABLE_READ: saldo do topo e movimentos da página vêm do mesmo snapshot, sem travar as escritas (MVCC).
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ResponseExtrato consultarExtrato(String numeroConta, LocalDate de, LocalDate ate, int page, int size){
        Conta conta = contaRepository.findByNumero(numeroConta);
        if (conta == null){
            throw new ApiException(HttpStatus.NOT_FOUND, "CONTA_NAO_ENCONTRADA", "Conta " + numeroConta + " inexistente");
        }
        return movimentoService.consultarExtrato(conta, de, ate, page, size);
    }

    public Conta getContaByNumero(String nomeConta){
        return contaRepository.findByNumero(nomeConta);
    }

    public Conta getContaById(UUID contaId){
        return contaRepository.findById(contaId).orElseThrow(
            () -> new ApiException(
                HttpStatus.NOT_FOUND,
                "NAO_ENCONTRADO",
                "Conta inexistente"
            )
        );
    }
}
