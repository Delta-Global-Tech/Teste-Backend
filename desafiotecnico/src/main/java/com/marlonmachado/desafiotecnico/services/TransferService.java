package com.marlonmachado.desafiotecnico.services;

import com.marlonmachado.desafiotecnico.dto.TransferReceiptDTO;
import com.marlonmachado.desafiotecnico.dto.TransferRequestDTO;
import com.marlonmachado.desafiotecnico.entities.Account;
import com.marlonmachado.desafiotecnico.entities.IdempotencyRecord;
import com.marlonmachado.desafiotecnico.entities.Movement;
import com.marlonmachado.desafiotecnico.entities.Transfer;
import com.marlonmachado.desafiotecnico.entities.enums.AccountStatus;
import com.marlonmachado.desafiotecnico.entities.enums.TransferStatus;
import com.marlonmachado.desafiotecnico.exceptions.BusinessException;
import com.marlonmachado.desafiotecnico.repositories.AccountRepository;
import com.marlonmachado.desafiotecnico.repositories.MovementRepository;
import com.marlonmachado.desafiotecnico.repositories.TransferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TransferService {

    private static final String ENDPOINT_DEPOSITO = "POST /contas/{numero}/depositos";
    private static final String ENDPOINT_TRANSFERENCIA = "POST /transferencias";
    private static final String ENDPOINT_ESTORNO = "POST /transferencias/{id}/estorno";
    private static final String SYSTEM_ENTRY_ACCOUNT_NUMBER = "SISTEMA-ENTRADA";
    private static final String SYSTEM_FEES_ACCOUNT_NUMBER = "SISTEMA-TAXAS";
    private static final long MAX_DEPOSIT_CENTS = 1_000_000L;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private FeeCalculator feeCalculator;


    @Transactional
    public TransferReceiptDTO deposit(String accountNumber, BigDecimal valorEmReais, String idempotencyKey) {

        Optional<IdempotencyRecord> registroExistente = idempotencyService.buscarRegistroExistente(
                ENDPOINT_DEPOSITO, idempotencyKey, valorEmReais);

        if (registroExistente.isPresent()) {
            return idempotencyService.lerResposta(registroExistente.get(), TransferReceiptDTO.class);
        }

        long amountCents = reaisParaCentavos(valorEmReais);

        if (amountCents <= 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "VALOR_INVALIDO",
                    "Valor do depósito deve ser maior que zero");
        }
        if (amountCents > MAX_DEPOSIT_CENTS) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "VALOR_INVALIDO",
                    "Valor do depósito não pode ultrapassar R$ 10.000,00");
        }

        Account systemEntry = accountRepository.findByNumber(SYSTEM_ENTRY_ACCOUNT_NUMBER)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "Conta SISTEMA-ENTRADA não encontrada"));

        Account target = accountRepository.findByNumber(accountNumber)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "Conta não encontrada: " + accountNumber));

        Account lockedTarget = accountRepository.findByIdForUpdate(target.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "Conta não encontrada: " + accountNumber));

        if (lockedTarget.getAccountStatus() == AccountStatus.CLOSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONTA_ENCERRADA",
                    "Conta encerrada não pode receber depósito");
        }

        Instant now = Instant.now();

        Transfer transfer = new Transfer(null, systemEntry, lockedTarget, amountCents, 0L,
                TransferStatus.CREATED, null, now, null);
        transfer.confirm(now);
        transfer = transferRepository.save(transfer);

        long saldoAposCredito = lockedTarget.getBalanceCents() + amountCents;
        lockedTarget.setBalanceCents(saldoAposCredito);
        accountRepository.save(lockedTarget);

        long proximaSequencia = proximaSequenciaPara(lockedTarget.getId());
        Movement movimentoCredito = Movement.credit(lockedTarget, transfer, proximaSequencia, amountCents, saldoAposCredito, now);
        movementRepository.save(movimentoCredito);

        TransferReceiptDTO comprovante = new TransferReceiptDTO(
                transfer.getId(),
                SYSTEM_ENTRY_ACCOUNT_NUMBER,
                accountNumber,
                valorEmReais,
                BigDecimal.ZERO,
                valorEmReais,
                centavosParaReais(saldoAposCredito),
                now.toString()
        );

        idempotencyService.salvarRegistro(ENDPOINT_DEPOSITO, idempotencyKey, valorEmReais, comprovante, 201);

        return comprovante;
    }


    @Transactional
    public TransferReceiptDTO transfer(String contaOrigemNumero, String contaDestinoNumero, BigDecimal valorEmReais, String idempotencyKey) {

        Optional<IdempotencyRecord> registroExistente = idempotencyService.buscarRegistroExistente(
                ENDPOINT_TRANSFERENCIA, idempotencyKey, new TransferRequestDTO(contaOrigemNumero, contaDestinoNumero, valorEmReais));

        if (registroExistente.isPresent()) {
            return idempotencyService.lerResposta(registroExistente.get(), TransferReceiptDTO.class);
        }

        if (contaOrigemNumero.equals(contaDestinoNumero)) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONTAS_IGUAIS",
                    "Conta de origem não pode ser igual à conta de destino");
        }

        long valorCentavos = reaisParaCentavos(valorEmReais);

        if (valorCentavos <= 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "VALOR_INVALIDO",
                    "Valor da transferência deve ser maior que zero");
        }

        Long origemId = accountRepository
                .findIdByAccountNumber(contaOrigemNumero)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "RECURSO_NAO_ENCONTRADO",
                        "Conta de origem não encontrada: " + contaOrigemNumero
                ));

        Long destinoId = accountRepository
                .findIdByAccountNumber(contaDestinoNumero)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "RECURSO_NAO_ENCONTRADO",
                        "Conta de destino não encontrada: " + contaDestinoNumero
                ));


        long taxaCentavos = feeCalculator.calculate(valorCentavos);

        Account sistemaTaxasSemLock = null;

        if (taxaCentavos > 0) {
            sistemaTaxasSemLock = accountRepository
                    .findByNumber(SYSTEM_FEES_ACCOUNT_NUMBER)
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "RECURSO_NAO_ENCONTRADO",
                            "Conta SISTEMA-TAXAS não encontrada"
                    ));
        }

        List<Long> idsParaBloquear = new ArrayList<>();
        idsParaBloquear.add(origemId);
        idsParaBloquear.add(destinoId);

        if (sistemaTaxasSemLock != null) {
            idsParaBloquear.add(sistemaTaxasSemLock.getId());
        }

        List<Account> contasBloqueadas =
                bloquearContasEmOrdem(idsParaBloquear);

        Map<Long, Account> contasPorId = contasBloqueadas.stream()
                .collect(Collectors.toMap(
                        Account::getId,
                        account -> account
                ));


        Account contaOrigem = contasPorId.get(origemId);
        Account contaDestino = contasPorId.get(destinoId);

        Account sistemaTaxas = sistemaTaxasSemLock == null
                ? null
                : contasPorId.get(sistemaTaxasSemLock.getId());

        if (!contaOrigem.getAccountStatus().canSend()) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONTA_NAO_PODE_ENVIAR",
                    "Conta de origem bloqueada ou encerrada não pode enviar transferência");
        }
        if (contaDestino.getAccountStatus() == AccountStatus.CLOSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONTA_ENCERRADA",
                    "Conta de destino encerrada não pode receber transferência");
        }

        long totalDebitado = valorCentavos + taxaCentavos;

        if (contaOrigem.getBalanceCents() < totalDebitado) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "SALDO_INSUFICIENTE",
                    "Saldo insuficiente para transferir");
        }

        long usadoHoje = valorTransferidoHoje(contaOrigem.getId());

        if (usadoHoje + valorCentavos > contaOrigem.getDailyLimitCents()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "LIMITE_DIARIO_EXCEDIDO",
                    "Limite diário de transferência excedido");
        }

        Instant agora = Instant.now();

        Transfer transferencia = new Transfer(null, contaOrigem, contaDestino, valorCentavos,
                taxaCentavos, TransferStatus.CREATED, null, agora, null);

        transferencia.confirm(agora);
        transferencia = transferRepository.save(transferencia);

        long saldoOrigemApos = contaOrigem.getBalanceCents() - totalDebitado;

        long saldoDestinoApos = contaDestino.getBalanceCents() + valorCentavos;

        contaOrigem.setBalanceCents(saldoOrigemApos);
        contaDestino.setBalanceCents(saldoDestinoApos);

        accountRepository.save(contaOrigem);
        accountRepository.save(contaDestino);

        long saldoOrigemAposTransferencia =
                contaOrigem.getBalanceCents() + taxaCentavos;


        movementRepository.save(
                Movement.debit(
                        contaOrigem,
                        transferencia,
                        proximaSequenciaPara(contaOrigem.getId()),
                        valorCentavos,
                        saldoOrigemAposTransferencia,
                        agora
                )
        );

        movementRepository.save(
                Movement.credit(contaDestino,
                        transferencia,
                        proximaSequenciaPara(contaDestino.getId()),
                        valorCentavos,
                        saldoDestinoApos,
                        agora
                )
        );

        if (taxaCentavos > 0) {
            sistemaTaxas.setBalanceCents(
                    sistemaTaxas.getBalanceCents() + taxaCentavos
            );
            accountRepository.save(sistemaTaxas);

            movementRepository.save(
                    Movement.debit(
                            contaOrigem,
                            transferencia,
                            proximaSequenciaPara(contaOrigem.getId()),
                            taxaCentavos,
                            saldoOrigemApos,
                            agora
                    )
            );

            movementRepository.save(
                    Movement.credit(
                            sistemaTaxas,
                            transferencia,
                            proximaSequenciaPara(sistemaTaxas.getId()),
                            taxaCentavos,
                            sistemaTaxas.getBalanceCents(),
                            agora
                    )
            );
        }

        TransferReceiptDTO comprovante = new TransferReceiptDTO(
                transferencia.getId(),
                contaOrigemNumero,
                contaDestinoNumero,
                valorEmReais,
                centavosParaReais(taxaCentavos),
                centavosParaReais(totalDebitado),
                centavosParaReais(saldoOrigemApos),
                agora.toString());

        idempotencyService.salvarRegistro(
                ENDPOINT_TRANSFERENCIA,
                idempotencyKey,
                new TransferRequestDTO(contaOrigemNumero, contaDestinoNumero, valorEmReais), comprovante, 201);

        return comprovante;
    }

    private List<Account> bloquearContasEmOrdem(
            List<Long> ids
    ) {
        return ids.stream()
                .distinct()
                .sorted()
                .map(id -> accountRepository.findByIdForUpdate(id)
                        .orElseThrow(() -> new BusinessException(
                                HttpStatus.NOT_FOUND,
                                "RECURSO_NAO_ENCONTRADO",
                                "Conta não encontrada: " + id
                        )))
                .toList();
    }

    private long valorTransferidoHoje(Long contaId) {
        ZoneId fusoBrasil = ZoneId.of("America/Sao_Paulo");
        Instant inicioDia = LocalDate.now(fusoBrasil).atStartOfDay(fusoBrasil).toInstant();
        Instant fimDia = LocalDate.now(fusoBrasil).plusDays(1).atStartOfDay(fusoBrasil).toInstant();
        return transferRepository.sumDebitedToday(contaId, inicioDia, fimDia);
    }

    private long proximaSequenciaPara(Long accountId) {
        return movementRepository.findLastMovementByAccountId(accountId)
                .map(m -> m.getSequence() + 1L)
                .orElse(1L);
    }


    @Transactional
    public TransferReceiptDTO reverse(Long transferenciaId, String idempotencyKey) {

        Optional<IdempotencyRecord> registroExistente = idempotencyService.buscarRegistroExistente(
                ENDPOINT_ESTORNO, idempotencyKey, transferenciaId);

        if (registroExistente.isPresent()) {
            return idempotencyService.lerResposta(registroExistente.get(), TransferReceiptDTO.class);
        }

        Transfer original = transferRepository.findById(transferenciaId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "Transferência não encontrada: " + transferenciaId));

        if (original.getStatus() != TransferStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.CONFLICT, "TRANSFERENCIA_NAO_CONFIRMADA",
                    "Só transferências confirmadas podem ser estornadas");
        }

        Account contaOrigemOriginal = original.getSourceAccount();
        Account contaDestinoOriginal = original.getTargetAccount();

        Long primeiroId = Math.min(contaOrigemOriginal.getId(), contaDestinoOriginal.getId());
        Long segundoId = Math.max(contaOrigemOriginal.getId(), contaDestinoOriginal.getId());

        Account primeiro = accountRepository
                .findByIdForUpdate(primeiroId)
                .orElseThrow();

        Account segundo = accountRepository
                .findByIdForUpdate(segundoId)
                .orElseThrow();

        Account contaQueRecebeuOriginal =
                primeiro.getId().equals(contaDestinoOriginal.getId())
                        ? primeiro
                        : segundo;

        Account contaQueEnviouOriginal =
                primeiro.getId().equals(contaOrigemOriginal.getId())
                        ? primeiro
                        : segundo;

        long valorDevolvido = original.getAmountCents() + original.getFeeCents();

        if (contaQueRecebeuOriginal.getBalanceCents() < valorDevolvido) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "SALDO_INSUFICIENTE_PARA_ESTORNO",
                    "Conta de destino não tem saldo suficiente para o estorno");
        }

        original.reverse();
        transferRepository.save(original);

        Instant agora = Instant.now();

        Transfer estorno = new Transfer(null, contaQueRecebeuOriginal, contaQueEnviouOriginal,
                original.getAmountCents(), original.getFeeCents(), TransferStatus.CREATED, original, agora, null);
        estorno.confirm(agora);
        estorno = transferRepository.save(estorno);

        long saldoQuemRecebeuApos = contaQueRecebeuOriginal.getBalanceCents() - valorDevolvido;
        contaQueRecebeuOriginal.setBalanceCents(saldoQuemRecebeuApos);
        accountRepository.save(contaQueRecebeuOriginal);

        long saldoQuemEnviouApos = contaQueEnviouOriginal.getBalanceCents() + valorDevolvido;
        contaQueEnviouOriginal.setBalanceCents(saldoQuemEnviouApos);
        accountRepository.save(contaQueEnviouOriginal);

        movementRepository.save(Movement.debit(contaQueRecebeuOriginal, estorno,
                proximaSequenciaPara(contaQueRecebeuOriginal.getId()), valorDevolvido, saldoQuemRecebeuApos, agora));

        movementRepository.save(Movement.credit(contaQueEnviouOriginal, estorno,
                proximaSequenciaPara(contaQueEnviouOriginal.getId()), valorDevolvido, saldoQuemEnviouApos, agora));

        TransferReceiptDTO comprovante = new TransferReceiptDTO(
                estorno.getId(),
                contaDestinoOriginal.getNumber(),
                contaOrigemOriginal.getNumber(),
                centavosParaReais(original.getAmountCents()),
                centavosParaReais(original.getFeeCents()),
                centavosParaReais(valorDevolvido),
                centavosParaReais(saldoQuemRecebeuApos),
                agora.toString());

        idempotencyService.salvarRegistro(ENDPOINT_ESTORNO, idempotencyKey, transferenciaId, comprovante, 201);

        return comprovante;
    }

    private long reaisParaCentavos(BigDecimal valorEmReais) {
        return valorEmReais
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
    }

    private BigDecimal centavosParaReais(long valorCentavos) {
        return BigDecimal.valueOf(valorCentavos, 2);
    }
}