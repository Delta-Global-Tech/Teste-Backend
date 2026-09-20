package com.marlonmachado.desafiotecnico.entities;


import com.marlonmachado.desafiotecnico.entities.enums.TransferStatus;
import com.marlonmachado.desafiotecnico.exceptions.IllegalStateTransitionException;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;


@Entity
@Table(
        name = "transfers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_transfers_original_reversal",
                        columnNames = "original_transfer_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_transfers_created_at",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_transfers_status_created_at",
                        columnList = "status, created_at"
                )
        }
)
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_account_id", nullable = false)
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_account_id", nullable = false)
    private Account targetAccount;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "fee_cents", nullable = false)
    private Long feeCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_transfer_id")
    private Transfer originalTransfer;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    public Transfer() {
    }


    public Transfer(Long id, Account sourceAccount, Account targetAccount, Long amountCents, Long feeCents, TransferStatus status, Transfer originalTransfer, Instant createdAt, Instant completedAt) {
        this.id = id;
        this.sourceAccount = sourceAccount;
        this.targetAccount = targetAccount;
        this.amountCents = amountCents;
        this.feeCents = feeCents;
        this.status = status;
        this.originalTransfer = originalTransfer;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }


    public void confirm(Instant now) {
        if (!this.status.canTransitionTo(TransferStatus.CONFIRMED)) {
            throw new IllegalStateTransitionException(this.status ,TransferStatus.CONFIRMED);
        }
        this.status = TransferStatus.CONFIRMED;
        this.completedAt = now;
    }

    public void reverse() {
        if (!this.status.canTransitionTo(TransferStatus.REVERSED)) {
            throw new IllegalStateTransitionException(this.status,TransferStatus.REVERSED);
        }
        this.status = TransferStatus.REVERSED;
    }

    public void fail() {
        if (!this.status.canTransitionTo(TransferStatus.FAILED)) {
            throw new IllegalStateTransitionException(this.status, TransferStatus.FAILED);
        }
        this.status = TransferStatus.FAILED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Account getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(Account sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public Account getTargetAccount() {
        return targetAccount;
    }

    public void setTargetAccount(Account targetAccount) {
        this.targetAccount = targetAccount;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Long amountCents) {
        this.amountCents = amountCents;
    }

    public Long getFeeCents() {
        return feeCents;
    }

    public void setFeeCents(Long feeCents) {
        this.feeCents = feeCents;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public Transfer getOriginalTransfer() {
        return originalTransfer;
    }

    public void setOriginalTransfer(Transfer originalTransfer) {
        this.originalTransfer = originalTransfer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Transfer transfer = (Transfer) o;
        return Objects.equals(getId(), transfer.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
